//! Helpers shared by the ported Kotlin unit tests.
#![allow(dead_code)]

use aerofast_engine::calculation::{self, CalcInput, FastenerCalculationResult};
use aerofast_engine::catalog::{self, MaterialInfo};
use aerofast_engine::*;

pub fn mat(k: &str) -> MaterialInfo {
    catalog::material(k).unwrap_or_else(|| panic!("material {k}")).clone()
}

/// `FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == d }`
pub fn m_idx(d: f64) -> usize {
    catalog::METRIC_SIZES.iter().position(|s| s.d == d).unwrap()
}

/// `FastenerCatalog.sizeIndexFor(unit, d)`
pub fn idx(unit: UnitSystem, d: f64) -> usize {
    catalog::size_index_for(unit, d)
}

#[allow(clippy::too_many_arguments)]
pub fn input(t: FastenerType, unit: UnitSystem, std: ProductStandard, size: usize, pitch: f64, len: f64, tc: &str, m: &str, coat: CoatingType, batch: i32) -> CalcInput {
    CalcInput::new(t, unit, std, size, pitch, len, tc, mat(m), coat, batch)
}

pub fn run(c: CalcInput) -> FastenerCalculationResult {
    calculation::calculate(&c)
}

/// CalcInput with a property class, calculated.
#[allow(clippy::too_many_arguments)]
pub fn calc(t: FastenerType, unit: UnitSystem, std: ProductStandard, size: usize, pitch: f64, len: f64, tc: &str, m: &str, coat: CoatingType, batch: i32, pc: Option<PropertyClass>) -> FastenerCalculationResult {
    let mut c = input(t, unit, std, size, pitch, len, tc, m, coat, batch);
    c.property_class = pc;
    run(c)
}

pub fn close(a: f64, b: f64, tol: f64) {
    assert!((a - b).abs() <= tol, "expected {a} ± {tol}, got {b}");
}

/// Two numbers separated by an en dash, e.g. "697 – 722 °C" → (697, 722).
pub fn range_of(s: &str) -> (f64, f64) {
    let (a, b) = s.split_once('–').unwrap();
    let num = |t: &str| t.trim().split(|c: char| !(c.is_ascii_digit() || c == '.')).find(|x| !x.is_empty()).unwrap().parse::<f64>().unwrap();
    (num(a.rsplit(' ').find(|x| !x.is_empty()).unwrap()), num(b))
}

/// An ExtractedFastenerSpec with the Kotlin constructor defaults for everything optional.
#[allow(clippy::too_many_arguments)]
pub fn spec(t: FastenerType, unit: UnitSystem, size: usize, d: f64, pitch: f64, len: f64, pc: PropertyClass, m: &str, tc: &str, coat: CoatingType) -> aerofast_engine::drawing_scan::ExtractedFastenerSpec {
    aerofast_engine::drawing_scan::ExtractedFastenerSpec {
        fastener_type: t,
        unit_system: unit,
        nominal_size_index: size,
        nominal_dia_mm: d,
        pitch_value: pitch,
        length_mm: len,
        property_class: pc,
        material_key: m.into(),
        thread_class: tc.into(),
        coating: coat,
        technical_summary: "test".into(),
        detected_dimensions: vec![],
        is_ai_vision_extracted: false,
        model_used: "gemini-3.5-flash".into(),
        radius: None,
        thread_length_mm: None,
        head_across_flats_mm: None,
        head_height_mm: None,
        flange_dia_mm: None,
        flange_thickness_mm: None,
        shank_dia_mm: None,
        head_dia_mm: None,
        socket_size_mm: None,
        socket_depth_mm: None,
        neck_height_mm: None,
        nut_height_mm: None,
        washer_outer_dia_mm: None,
        washer_inner_dia_mm: None,
        washer_thickness_mm: None,
        head_dome_radius_mm: None,
        head_edge_height_mm: None,
        tensile_min_mpa: None,
        tensile_max_mpa: None,
        thread_lock_patch: false,
        batch_qty: None,
        assumed: vec![],
        solids: vec![],
        cutouts: vec![],
    }
}
