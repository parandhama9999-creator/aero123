//! Parity with the Kotlin engine: every golden case is re-run in Rust and compared field by field.
//! Numbers must agree to 1e-9 relative, strings and enums exactly.
//! The golden data comes from the Kotlin engine itself (GoldenExport.kt, removed with the desktop app; see git history).

use aerofast_engine::calculation::{self, CalcInput, FastenerCalculationResult};
use aerofast_engine::part_geometry::DrawingGeometry;
use aerofast_engine::tensile::{MillCert, TensileRange};
use aerofast_engine::*;
use flate2::read::GzDecoder;
use serde_json::{json, Map, Value};
use std::collections::BTreeMap;
use std::io::Read;

#[path = "support/derived.rs"]
mod derived;

fn load_cases() -> Vec<Value> {
    let path = concat!(env!("CARGO_MANIFEST_DIR"), "/golden/cases.json.gz");
    let mut s = String::new();
    GzDecoder::new(std::fs::File::open(path).expect("golden/cases.json.gz")).read_to_string(&mut s).unwrap();
    serde_json::from_str(&s).unwrap()
}

fn opt_f(v: &Value) -> Option<f64> {
    v.as_f64()
}

pub fn input_of(case: &Value) -> CalcInput {
    let i = &case["input"];
    let s = |k: &str| i[k].as_str().unwrap_or_else(|| panic!("input.{k}"));
    let material = catalog::material(s("materialKey")).unwrap_or_else(|| panic!("material {}", s("materialKey"))).clone();
    let mut c = CalcInput::new(
        FastenerType::value_of(s("type")).unwrap(),
        UnitSystem::value_of(s("unit")).unwrap(),
        ProductStandard::value_of(case["standard"].as_str().unwrap()).unwrap(),
        i["sizeIndex"].as_u64().unwrap() as usize,
        i["pitch"].as_f64().unwrap(),
        i["length"].as_f64().unwrap(),
        s("threadClass"),
        material,
        CoatingType::value_of(s("coating")).unwrap(),
        i["batch"].as_i64().unwrap() as i32,
    );
    c.currency_is_usd = i["currencyIsUsd"].as_bool().unwrap();
    c.cnc_hour_rate_inr = i["cncRate"].as_f64().unwrap();
    c.ht_rate_per_kg_inr = i["htRate"].as_f64().unwrap();
    c.margin_pct = i["margin"].as_f64().unwrap();
    c.custom_nominal_dia_mm = opt_f(&i["customDia"]);
    c.extracted_radius = opt_f(&i["radius"]);
    c.property_class = i["propertyClass"].as_str().map(|p| PropertyClass::value_of(p).unwrap());
    c.drawing = serde_json::from_value::<DrawingGeometry>(i["drawing"].clone()).unwrap();
    c.tensile_range = TensileRange::of(opt_f(&i["tensileMin"]), opt_f(&i["tensileMax"]));
    c.thread_lock_patch = i["patch"].as_bool().unwrap();
    c.mill_cert = MillCert::of(opt_f(&i["millRm"]), opt_f(&i["millHrb"]));
    c.wire_dia_override_mm = opt_f(&i["wireOverride"]);
    c.open_extrusion_limit_pct = i["openLimit"].as_f64().unwrap();
    c.header_choice = HeaderChoice::value_of(s("header")).unwrap();
    c
}

/// The golden JSON writes a MaterialInfo inside a case as its key.
pub fn result_json(r: &FastenerCalculationResult) -> Value {
    let mut v = serde_json::to_value(r).unwrap();
    v["material"] = json!(r.material.key);
    v
}

pub static LEAVES: std::sync::atomic::AtomicUsize = std::sync::atomic::AtomicUsize::new(0);

fn num_eq(a: f64, b: f64) -> bool {
    a == b || (a - b).abs() <= 1e-9 * 1f64.max(a.abs()).max(b.abs())
}

/// Compares golden (Kotlin) with Rust. Paths have array indices replaced by [] for grouping.
pub fn compare(path: &str, g: &Value, r: &Value, out: &mut Vec<(String, String)>) {
    if !matches!(g, Value::Object(_) | Value::Array(_)) {
        LEAVES.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
    }
    match (g, r) {
        (Value::Number(a), Value::Number(b)) => {
            let (a, b) = (a.as_f64().unwrap(), b.as_f64().unwrap());
            if !num_eq(a, b) {
                out.push((path.into(), format!("kotlin {a} vs rust {b}")));
            }
        }
        // Kotlin writes NaN / Infinity as strings; serde_json writes them as null
        (Value::String(s), Value::Null) if s == "NaN" || s.contains("Infinity") => {}
        // Kotlin Pair / Triple are objects {first, second[, third]}; Rust tuples are arrays
        (Value::Object(go), Value::Array(ra)) if go.contains_key("first") && go.len() == ra.len() && (2..=3).contains(&ra.len()) => {
            for (i, k) in ["first", "second", "third"].iter().take(ra.len()).enumerate() {
                match go.get(*k) {
                    Some(gv) => compare(&format!("{path}.{k}"), gv, &ra[i], out),
                    None => out.push((format!("{path}.{k}"), "missing in kotlin".into())),
                }
            }
        }
        (Value::Object(go), Value::Object(ro)) => {
            for (k, gv) in go {
                match ro.get(k) {
                    Some(rv) => compare(&format!("{path}.{k}"), gv, rv, out),
                    None => out.push((format!("{path}.{k}"), "missing in rust".into())),
                }
            }
            for k in ro.keys() {
                if !go.contains_key(k) {
                    out.push((format!("{path}.{k}"), "extra in rust".into()));
                }
            }
        }
        (Value::Array(ga), Value::Array(ra)) => {
            if ga.len() != ra.len() {
                out.push((format!("{path}[]"), format!("length kotlin {} vs rust {}", ga.len(), ra.len())));
            }
            for (x, y) in ga.iter().zip(ra) {
                compare(&format!("{path}[]"), x, y, out);
            }
        }
        _ => {
            if g != r {
                let show = |v: &Value| {
                    let s = v.to_string();
                    if s.chars().count() > 300 { s.chars().take(300).collect::<String>() + "…" } else { s }
                };
                out.push((path.into(), format!("kotlin {} vs rust {}", show(g), show(r))));
            }
        }
    }
}

#[test]
fn parity_with_kotlin_engine() {
    let cases = load_cases();
    let only = std::env::var("PARITY_CASE").ok().and_then(|s| s.parse::<u64>().ok());
    let mut by_path: BTreeMap<String, (usize, String, u64)> = BTreeMap::new();
    let mut failed_cases = 0;
    let mut not_ported: BTreeMap<String, usize> = BTreeMap::new();
    for case in &cases {
        let id = case["id"].as_u64().unwrap();
        if only.is_some_and(|o| o != id) {
            continue;
        }
        assert!(case["error"].is_null(), "case {id} errored in Kotlin");
        let inp = input_of(case);
        let r = calculation::calculate(&inp);
        let mut out = Vec::new();
        compare("result", &case["result"], &result_json(&r), &mut out);
        let golden_derived = case["derived"].as_object().unwrap();
        let rust_derived: Map<String, Value> = derived::derived(&r, golden_derived);
        for (k, gv) in golden_derived {
            match rust_derived.get(k) {
                Some(rv) => compare(&format!("derived.{k}"), gv, rv, &mut out),
                None => *not_ported.entry(k.clone()).or_default() += 1,
            }
        }
        if !out.is_empty() {
            failed_cases += 1;
        }
        for (p, msg) in out {
            let e = by_path.entry(p).or_insert((0, msg, id));
            e.0 += 1;
        }
    }
    for (p, (n, msg, id)) in &by_path {
        println!("{n:4} × {p}   [first: case {id}] {msg}");
    }
    if !not_ported.is_empty() {
        println!("not ported yet: {not_ported:?}");
    }
    println!(
        "{} cases, {} values compared, {} cases with differences, {} differing fields",
        cases.len(),
        LEAVES.load(std::sync::atomic::Ordering::Relaxed),
        failed_cases,
        by_path.len()
    );
    assert!(by_path.is_empty(), "{failed_cases} cases differ from the Kotlin engine");
    assert!(not_ported.is_empty() || std::env::var("PARITY_ALLOW_UNPORTED").is_ok(), "derived outputs not ported: {not_ported:?}");
}

#[test]
fn comparator_catches_differences() {
    let mut out = Vec::new();
    let g = json!({"a": 1.0, "b": "x", "c": [1, 2], "d": {"first": 1.0, "second": 2.0}, "e": null});
    compare("t", &g, &json!({"a": 1.0 + 1e-12, "b": "x", "c": [1, 2], "d": [1.0, 2.0], "e": null}), &mut out);
    assert!(out.is_empty(), "{out:?}");
    compare("t", &g, &json!({"a": 1.001, "b": "y", "c": [1], "d": [1.0, 3.0], "f": 1}), &mut out);
    let paths: Vec<&str> = out.iter().map(|x| x.0.as_str()).collect();
    assert_eq!(paths, ["t.a", "t.b", "t.c[]", "t.d.second", "t.e", "t.f"]);
}
