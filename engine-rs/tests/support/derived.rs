//! The Rust side of each golden `derived` entry (see GoldenExport.derived in the Kotlin exporter).

use aerofast_engine::calculation::{self, FastenerCalculationResult};
use aerofast_engine::tooling::{self, MachineSetup};
use aerofast_engine::*;
use serde::Serialize;
use serde_json::{Map, Value};

fn j<T: Serialize>(v: T) -> Value {
    serde_json::to_value(v).unwrap()
}

pub fn machine() -> MachineSetup {
    MachineSetup {
        die_case_od_mm: Some(40.0),
        die_case_length_mm: Some(60.0),
        punch_case_od_mm: Some(35.0),
        punch_case_length_mm: Some(50.0),
        ko_stroke_mm: Some(30.0),
        insert_od_mm: Some(20.0),
        open_extrusion_limit_pct: Some(25.0),
        die_angle_deg: Some(30.0),
        land_length_mm: Some(2.0),
    }
}

/// Builds the derived outputs the golden case has (full cases have more).
pub fn derived(r: &FastenerCalculationResult, golden: &Map<String, Value>) -> Map<String, Value> {
    let m = &r.material;
    let mut d = Map::new();
    let mut put = |k: &str, v: Value| {
        if golden.contains_key(k) {
            d.insert(k.to_string(), v);
        }
    };
    put("processSequence", j(production_report::process_sequence(r)));
    put("hexBoltTolerances", j(quality::hex_bolt_tolerances(r)));
    put("specialCharacteristics", j(quality::special_characteristics(r)));
    put("capabilityCharacteristics", j(quality::capability_characteristics(r)));
    put("coatingMethods", j(quality::coating_thickness_methods(r.coating)));
    put("quenchTemper", j(heat_treat_cycle::quench_temper(m, r.wire_dia_mm, Some(&r.heat_treatment_spec))));
    put("precipitationHarden", j(heat_treat_cycle::precipitation_harden(m, r.wire_dia_mm)));
    put("softAnnealCycle", j(heat_treat_cycle::soft_anneal(m, r.wire_dia_mm)));
    put("spheroidize", j(soft_anneal::spheroidize(m, soft_anneal::DEFAULT_CHARGE_TONNES, r.wire_dia_mm)));
    put("materialFamily", j(poka_yoke::material_family(m)));
    put("stainlessType", j(poka_yoke::stainless_type(m)));
    put("isWarmHeaded", j(calculation::is_warm_headed(m, r.nominal_dia_mm)));
    put("wireCheckIssues", j(tensile::mill_cert_check::issues(r.wire_check.as_ref())));
    if golden.contains_key("toolingSheet") {
        let none = MachineSetup::default();
        put("toolingSheet", j(tooling::build(r, &none)));
        put("toolingText", j(tooling::build(r, &none).map(|s| tooling::text(r, &s, &none))));
        put("toolingSheetMachine", j(tooling::build(r, &machine())));
        put("extrusions", j(r.heading_plan.as_ref().map(tooling::extrusions)));
        put("productionSummary", j(production_report::summary(r)));
        put("productionReport", j(production_report::build(r, None, "2026-01-01 00:00")));
        put("controlPlan", j(quality::control_plan(r)));
        put("apqp", j(quality::apqp(r)));
        put("ppap", j(quality::ppap_elements(r)));
        put("psw", j(quality::psw(r, "PN-1", "B", "OEM", 3, quality::PSW_REASONS[1], "Spec A\nSpec B")));
        put("ecn", j(quality::ecn(r, "ECN-1", "A", "B", "desc", "why", &[0, 4], "Spec A")));
        put(
            "rawMaterialPlan",
            j(raw_material::plan(
                r.gross_weight_grams / m.density * 1000.0,
                m.density,
                r.batch_qty,
                &raw_material::suggested_diameters(r.blank_target_dia, r.wire_dia_mm),
                r.head_volume_mm3(),
                Some(r.wire_dia_mm),
                Some(r.blank_target_dia),
            )),
        );
        put(
            "anim",
            j(r.heading_plan.as_ref().map(|pl| {
                let mut v: Vec<Value> = [0.0, 0.5, 1.7, 3.0, 5.5, 20.0]
                    .iter()
                    .map(|&t| {
                        let f = heading_animation::frame_at(pl, t);
                        serde_json::json!({ "frame": j(&f), "outline": j(heading_animation::outline(pl, &f, 16)) })
                    })
                    .collect();
                v.push(serde_json::json!({ "duration": heading_animation::duration_sec(pl), "blows": heading_animation::blow_count(pl) }));
                v
            })),
        );
    }
    d
}
