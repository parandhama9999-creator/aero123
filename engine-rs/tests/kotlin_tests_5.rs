//! Ported from app/src/test: SelectionFilterTest, SoftAnnealTest, AerospaceScrewDrawingTest, ThreadRollingBlankTest,
//! HeaderRouteTest. The aerospace screw spec is built field by field instead of through the AI JSON parser, which
//! is not part of the engine.

#[path = "support/common.rs"]
mod common;
use aerofast_engine::calculation::{self, FastenerCalculationResult};
use aerofast_engine::catalog;
use aerofast_engine::drawing_scan::{self, ExtractedFastenerSpec};
use aerofast_engine::part_geometry::DrawingGeometry;
use aerofast_engine::poka_yoke::{CheckInput, PokaYokeIssue};
use aerofast_engine::tensile::{hardness_conversion, TensileRange};
use aerofast_engine::thread::thread_rolling_blank as trb;
use aerofast_engine::*;
use common::*;
use std::f64::consts::PI;
use CoatingType as C;
use FastenerType as FT;
use ProductStandard as PS;
use UnitSystem::{INCH, METRIC};

// ---------------- SelectionFilterTest
#[allow(clippy::too_many_arguments)]
fn gate(t: FT, unit: UnitSystem, std: Option<PS>, d: f64, len: f64, pc: Option<PropertyClass>, m: &str, coat: C, tc: Option<&str>) -> Vec<PokaYokeIssue> {
    let std = std.unwrap_or_else(|| poka_yoke::default_standard(t, unit));
    let tc = tc.unwrap_or_else(|| selection_filter::thread_classes(unit, t, std)[0]);
    let i = idx(unit, d);
    let pitch = if unit == METRIC { catalog::METRIC_SIZES[i].pitches[0] } else { catalog::INCH_SIZES[i].tpis[0] as f64 };
    let mi = mat(m);
    poka_yoke::check(&CheckInput {
        t,
        unit,
        standard: std,
        nominal_size_index: i,
        pitch_value: pitch,
        d: catalog::diameter_mm(unit, i),
        length_mm: len,
        thread_class: tc,
        property_class: pc,
        material: &mi,
        coating: coat,
        batch_qty: 100,
        fundamental_deviation_mm: 0.0,
        pitch_dia_min_mm: 0.0,
        pitch_dia_max_mm: 100.0,
        is_custom_scan: false,
        stress_area_mm2: 50.0,
        head_dims: None,
        thread_length_mm: None,
        tensile_range: None,
        thread_lock_patch: false,
        head_from_drawing: false,
    })
}
/// Kotlin gate() with its defaults: HEX_BOLT, METRIC, d 10, L 60, 10.9, 10B21, GEOMET.
struct G {
    t: FT,
    unit: UnitSystem,
    std: Option<PS>,
    d: f64,
    len: f64,
    pc: Option<PropertyClass>,
    m: &'static str,
    coat: C,
    tc: Option<&'static str>,
}
impl Default for G {
    fn default() -> Self {
        G { t: FT::HEX_BOLT, unit: METRIC, std: None, d: 10.0, len: 60.0, pc: Some(PropertyClass::CLASS_10_9), m: "10B21", coat: C::GEOMET, tc: None }
    }
}
impl G {
    fn run(&self) -> Vec<PokaYokeIssue> {
        gate(self.t, self.unit, self.std, self.d, self.len, self.pc, self.m, self.coat, self.tc)
    }
    fn critical(&self) -> Vec<String> {
        self.run().into_iter().filter(|i| i.severity == PokaYokeSeverity::CRITICAL).map(|i| i.title).collect()
    }
    fn titles(&self) -> Vec<String> {
        self.run().into_iter().map(|i| i.title).collect()
    }
}
const CANNOT: &str = "Material cannot reach the class strength";
fn has(v: &[String], s: &str) -> bool {
    v.iter().any(|x| x == s)
}

#[test]
fn every_class_default_material_satisfies_that_class() {
    for pc in PropertyClass::VALUES {
        let unit = if pc.name().starts_with("SAE") || pc.name().starts_with("ASTM") { INCH } else { METRIC };
        let t = if poka_yoke::NUT_ONLY_CLASSES.contains(&pc) {
            if pc == PropertyClass::NUT_04 || pc == PropertyClass::NUT_05 { FT::THIN_NUT } else { FT::HEX_NUT }
        } else if pc.name().starts_with("AERO") || pc == PropertyClass::MATERIAL_SPEC {
            FT::NAS_CLOSE_TOL_BOLT
        } else {
            FT::HEX_BOLT
        };
        let c = G { t, unit, d: if unit == METRIC { 10.0 } else { 9.525 }, pc: Some(pc), m: pc.default_mat_key(), coat: C::AERO_IVD, ..Default::default() }.critical();
        assert!(!c.iter().any(|x| x.contains("Material") || x.contains("needs") || x.contains("grade")), "{pc:?}: {c:?}");
    }
}

#[test]
fn aerospace_ranges_accept_every_alloy_in_the_labelled_range() {
    for (pc, alloys) in [
        (PropertyClass::AERO_AL, vec!["AL2024", "AL2219", "AL7075"]),
        (PropertyClass::AERO_1100, vec!["TI_GR5", "TI_6242"]),
        (PropertyClass::AERO_1750, vec!["300M", "MP35N", "H11_H13", "CUSTOM_455"]),
    ] {
        for m in alloys {
            let c = G { t: FT::NAS_CLOSE_TOL_BOLT, unit: INCH, d: 6.35, pc: Some(pc), m, coat: C::AERO_IVD, ..Default::default() }.critical();
            assert!(!has(&c, CANNOT), "{pc:?} {m}");
        }
    }
    let c = G { t: FT::NAS_CLOSE_TOL_BOLT, unit: INCH, d: 6.35, pc: Some(PropertyClass::AERO_AL), m: "AL6061", coat: C::ANODIZE_II, ..Default::default() }.critical();
    assert!(has(&c, CANNOT));
}

#[test]
fn inch_grade_uses_the_strength_of_its_size_band() {
    let g = |d: f64| G { unit: INCH, d, pc: Some(PropertyClass::SAE_GRADE_2), m: "1008", ..Default::default() }.critical();
    assert!(has(&g(12.7), CANNOT));
    assert!(!has(&g(25.4), CANNOT));
}

#[test]
fn wrong_material_for_the_class_is_blocked() {
    assert!(!G { pc: Some(PropertyClass::CLASS_12_9), m: "1008", ..Default::default() }.critical().is_empty());
    assert!(!G { pc: Some(PropertyClass::A2_70), m: "SS410", coat: C::PASSIVATION, ..Default::default() }.critical().is_empty());
    assert!(has(&G { pc: Some(PropertyClass::CLASS_9_8), d: 20.0, ..Default::default() }.critical(), "Class 9.8 is limited to M16"));
}

#[test]
fn phosphate_and_black_oxide_need_the_right_substrate() {
    for (m, pc) in [("SS304", PropertyClass::A2_70), ("TI_GR5", PropertyClass::AERO_1100), ("AL7075", PropertyClass::AERO_AL), ("INCONEL718", PropertyClass::AERO_1380)] {
        for c in [C::PHOSPHATE, C::PHOSPHATE_MN] {
            assert!(has(&G { pc: Some(pc), m, coat: c, ..Default::default() }.critical(), "Phosphating needs carbon or alloy steel"), "{m} {c:?}");
        }
    }
    let bo = "Black oxide needs a ferrous or copper alloy";
    assert!(has(&G { pc: Some(PropertyClass::AERO_AL), m: "AL7075", coat: C::BLACK_OXIDE, ..Default::default() }.critical(), bo));
    assert!(has(&G { pc: Some(PropertyClass::AERO_1100), m: "TI_GR5", coat: C::BLACK_OXIDE, ..Default::default() }.critical(), bo));
    assert!(G { coat: C::PHOSPHATE, ..Default::default() }.critical().is_empty());
    assert!(G { coat: C::BLACK_OXIDE, ..Default::default() }.critical().is_empty());
}

#[test]
fn wrong_standard_for_the_part_is_blocked() {
    assert!(has(&G { std: Some(PS::ISO_4032), ..Default::default() }.critical(), "Standard does not fit a bolt/screw"));
    assert!(has(&G { t: FT::HEX_NUT, std: Some(PS::ISO_4014), tc: Some("6H"), ..Default::default() }.critical(), "Standard does not fit a nut"));
    assert!(has(&G { unit: INCH, d: 9.525, std: Some(PS::ISO_4014), pc: Some(PropertyClass::SAE_GRADE_8), m: "4140", ..Default::default() }.critical(), "Metric standard with inch thread"));
    let a490 = G { t: FT::HEAVY_HEX_BOLT, std: Some(PS::ASTM_A490), d: 20.0, len: 100.0, ..Default::default() };
    assert!(a490.critical().is_empty());
    assert!(has(&a490.titles(), "Inch standard with metric thread"));
}

#[test]
fn length_rules_are_flagged_by_the_gate() {
    assert!(has(&G { d: 64.0, len: 6.0, ..Default::default() }.titles(), "Very short length for diameter"));
    let short = "Too short for a partially threaded bolt";
    assert!(has(&G { len: 30.0, ..Default::default() }.titles(), short));
    assert!(has(&G { unit: INCH, d: 9.525, len: 25.4, pc: Some(PropertyClass::SAE_GRADE_5), ..Default::default() }.titles(), short));
    assert!(!has(&G::default().titles(), short));
    let r = G { t: FT::SOLID_RIVET, unit: INCH, d: 4.826, len: 6.35, pc: Some(PropertyClass::AERO_AL), m: "AL2117", coat: C::ANODIZE_II, ..Default::default() };
    assert!(!has(&r.titles(), "Very short length for diameter"));
    assert!(!has(&G { t: FT::SOCKET_SET, len: 10.0, ..Default::default() }.titles(), "Very short length for diameter"));
}

#[test]
fn standards_own_thread_class_is_the_default() {
    assert_eq!("3A", selection_filter::thread_classes(INCH, FT::SHCS, PS::ASME_B18_3)[0]);
    assert_eq!("2A", selection_filter::thread_classes(INCH, FT::HEX_BOLT, PS::ASME_B18_2_1)[0]);
    assert_eq!("6g", selection_filter::thread_classes(METRIC, FT::HEX_BOLT, PS::ISO_4014)[0]);
    assert_eq!("6H", selection_filter::thread_classes(METRIC, FT::HEX_NUT, PS::ISO_4032)[0]);
}

#[test]
fn aerospace_standards_lead_with_the_unj_or_mj_thread() {
    assert_eq!("3A-UNJ", selection_filter::thread_classes(INCH, FT::NAS_CLOSE_TOL_BOLT, PS::NAS6203)[0]);
    assert_eq!("3B-UNJ", selection_filter::thread_classes(INCH, FT::AERO_LOCK_NUT, PS::MS21042)[0]);
    assert!(selection_filter::thread_classes(METRIC, FT::NAS_CLOSE_TOL_BOLT, PS::EN2882)[0].ends_with("-MJ"));
    assert_eq!("6g", selection_filter::thread_classes(METRIC, FT::THREAD_FORMING_SCREW, PS::DIN_7500)[0]);
}

#[test]
fn metric_aerospace_bolts_default_to_the_metric_standard() {
    assert_eq!(PS::EN2882, poka_yoke::default_standard(FT::TWELVE_POINT, METRIC));
    assert_eq!(PS::EN2882, poka_yoke::default_standard(FT::NAS_CLOSE_TOL_BOLT, METRIC));
    assert_eq!(PS::NAS624, poka_yoke::default_standard(FT::TWELVE_POINT, INCH));
    assert_eq!(PS::NAS6203, poka_yoke::default_standard(FT::NAS_CLOSE_TOL_BOLT, INCH));
}

#[test]
fn length_catalogues_are_the_standard_series() {
    assert_eq!(2.0, catalog::METRIC_LENGTHS[0]);
    assert_eq!(500.0, *catalog::METRIC_LENGTHS.last().unwrap());
    assert!(!catalog::METRIC_LENGTHS.contains(&75.0));
    close(3.175, catalog::INCH_LENGTHS[0], 1e-9);
    close(203.2, *catalog::INCH_LENGTHS.last().unwrap(), 1e-9);
}

// ---------------- SoftAnnealTest
fn sa(k: &str) -> soft_anneal::SoftAnnealSpec {
    soft_anneal::build(&mat(k))
}

#[test]
fn boron_steel_spheroidizes_and_reads_globular_target() {
    let s = sa("10B21");
    assert_eq!(AnnealRoute::SPHEROIDIZE, s.route);
    assert!(s.in_house_anneal_needed);
    assert!(s.structure_target.contains("globular"), "{}", s.structure_target);
    assert!(s.target_hardness_hb.starts_with("142 – 166"), "{}", s.target_hardness_hb);
    assert!(!s.target_hardness_hrb.trim().is_empty());
    close(0.5, s.softening_ratio.unwrap(), 0.02);
    assert!(s.standard_reference.contains("EN 10263"));
}

#[test]
fn austenitic_stainless_solution_anneals() {
    let s = sa("SS316");
    assert_eq!(AnnealRoute::SOLUTION_ANNEAL, s.route);
    assert!(!s.temperature_c.trim().is_empty());
    assert!(s.cooling_rate.to_lowercase().contains("rapid"));
    assert!(s.notes.iter().any(|n| n.contains("opposite")));
}

#[test]
fn aluminium_uses_o_temper_and_flags_missing_cycle() {
    // Kotlin takes the first "AL…" key of the catalogue map, in its insertion order
    let al = catalog::materials().iter().find(|m| m.key.to_uppercase().starts_with("AL")).unwrap();
    let s = soft_anneal::build(al);
    assert_eq!(AnnealRoute::O_TEMPER, s.route);
    assert_eq!("", s.temperature_c);
    assert!(s.unsourced.iter().any(|u| u.contains("temperature")));
}

#[test]
fn cold_drawn_wire_is_flagged_as_not_headable() {
    let drawn: Vec<_> = catalog::materials()
        .iter()
        .filter(|m| {
            let c = m.raw_material_sourcing_uts_mpa.to_uppercase();
            c.contains("COLD DRAWN") || c.contains("HALF-HARD")
        })
        .collect();
    assert!(!drawn.is_empty());
    for m in drawn {
        let s = soft_anneal::build(m);
        assert_eq!(AnnealRoute::ANNEAL_REQUIRED, s.route, "{}", m.key);
        assert!(s.notes.iter().any(|n| n.contains("not a headable condition")));
    }
}

#[test]
fn never_invents_a_value_it_cannot_source() {
    for m in catalog::materials() {
        let s = soft_anneal::build(m);
        assert!(!s.temperature_c.trim().is_empty() || !s.unsourced.is_empty() || s.route == AnnealRoute::MILL_ANNEALED, "{}", m.key);
    }
}

#[test]
fn soft_anneal_wired_into_calculation_result() {
    let r = calc(FT::HEX_BOLT, METRIC, PS::ISO_4014, m_idx(10.0), 1.5, 50.0, "6g", "10B21", C::GEOMET, 1000, Some(PropertyClass::CLASS_10_9));
    assert_eq!(AnnealRoute::SPHEROIDIZE, r.soft_anneal_spec.unwrap().route);
}

#[test]
fn titanium_mill_anneal_is_not_the_solution_treat_temperature() {
    let s = sa("TI_GR5");
    assert_eq!(AnnealRoute::MILL_ANNEALED, s.route);
    assert!(s.temperature_c.starts_with("704 – 788"));
    assert!(!s.temperature_c.contains("955"));
    assert!(s.atmosphere.contains("alpha-case"));
    assert!(s.cycle_source.contains("Rolled Alloys"));
    assert!(mat("TI_GR5").heat_treatment_spec.austenitizing_or_solution_temp_c.contains("955"));
}

#[test]
fn inconel625_uses_grade1_anneal_and_rene41_is_disputed() {
    assert!(sa("INCONEL625").temperature_c.starts_with("982"));
    let r = sa("RENE41");
    assert!(r.temperature_c.starts_with("1066"));
    assert!(r.cycle_confidence.contains("disagree"));
}

#[test]
fn every_mill_annealed_grade_has_a_cited_cycle_or_declares_the_gap() {
    let mill: Vec<_> = catalog::materials().iter().filter(|m| soft_anneal::build(m).route == AnnealRoute::MILL_ANNEALED).collect();
    assert!(mill.len() >= 14);
    for m in mill {
        let s = soft_anneal::build(m);
        let cited = !s.cycle_source.trim().is_empty() && !s.temperature_c.starts_with("N/A");
        assert!(cited || s.unsourced.iter().any(|u| u.contains("Mill-anneal")), "{}", m.key);
    }
}

#[test]
fn low_confidence_cycle_is_flagged_for_verification() {
    let s = sa("10KH11N23T3MR");
    assert!(s.cycle_confidence.starts_with("Low"));
    assert!(s.unsourced.iter().any(|u| u.contains("GOST")));
}

// ---------------- AerospaceScrewDrawingTest
/// #8-32 UNJC-3A hexalobular pan head screw, AISI 4340, Rm 1000–1240 MPa, cadmium QQ-P-416 Type II Class 2,
/// nylon patch, QTY 13000 — as the scanner reads it.
fn screw_spec() -> ExtractedFastenerSpec {
    let i = catalog::INCH_SIZES.iter().position(|s| s.d_inch == 0.164).unwrap();
    let mut s = spec(FT::PAN_HEXALOBULAR, INCH, i, 4.166, 0.79375, 6.35, PropertyClass::MATERIAL_SPEC, "4340", "3A-UNJ", C::AERO_CAD_CL2);
    s.head_dia_mm = Some(8.17);
    s.head_height_mm = Some(2.92);
    s.socket_size_mm = Some(3.94);
    s.head_dome_radius_mm = Some(6.20);
    s.head_edge_height_mm = Some(1.12);
    s.radius = Some(0.25);
    s.batch_qty = Some(13000);
    s.tensile_min_mpa = Some(1000.0);
    s.tensile_max_mpa = Some(1240.0);
    s.thread_lock_patch = true;
    s
}
fn screw() -> FastenerCalculationResult {
    drawing_scan::calculate(&screw_spec(), HeaderChoice::AUTO, 10000)
}

#[test]
fn domed_head_and_recess_give_the_real_weight() {
    let r = screw();
    let a = 8.17 / 2.0;
    let cap = 6.20 - (6.20f64 * 6.20 - a * a).sqrt();
    let head = PI * a * a * 1.12 + PI * cap * cap * (3.0 * 6.20 - cap) / 3.0;
    let part = r.finished_part.as_ref().unwrap();
    close(head, part.items.iter().filter(|i| i.feature.starts_with("Pan head")).map(|i| i.volume_mm3).sum(), 1e-6);
    assert!(part.items.iter().any(|i| i.feature == "Hexalobular recess" && i.volume_mm3 < 0.0));
    close(1.23, r.net_weight_grams, 0.03);
    assert!(part.cautions.iter().any(|c| c.contains("head height 2.66")));
}

#[test]
fn full_thread_screw_is_headed_from_blank_size_wire() {
    let r = screw();
    close((r.blank_target_dia * 100.0 - 1e-6).ceil() / 100.0, r.wire_dia_mm, 1e-9);
    close(15.1, r.cut_off_length_mm, 0.2);
    let plan = r.heading_plan.as_ref().unwrap();
    assert_eq!(HeaderRoute::TWO_BLOW, plan.route);
    assert_eq!("1st blow: cone preform", plan.stations[1].title);
    assert_eq!(3, plan.stations.len());
    assert!(plan.stations.last().unwrap().operation.contains("hexalobular recess"));
    assert_eq!("#8-32 3A-UNJ", r.thread_label);
    assert!(!r.recommended_machine.contains("warm"));
}

#[test]
fn tensile_range_drives_loads_and_hardness() {
    let r = screw();
    close(1000.0, r.post_forging_target_uts_mpa, 1e-9);
    close(r.stress_area_as_mm2 * 1000.0 / 1000.0, r.tensile_breaking_load_kn, 0.1);
    assert!(r.heat_treatment_spec.target_hardness_hrc.contains("aim 33–36 HRC"), "{}", r.heat_treatment_spec.target_hardness_hrc);
    assert!(r.heat_treatment_spec.tempering_or_aging_temp_c.starts_with("Trial temper 5"), "{}", r.heat_treatment_spec.tempering_or_aging_temp_c);
}

#[test]
fn poka_yoke_gives_the_right_controls_and_no_false_alarms() {
    let r = screw();
    let titles: Vec<&str> = r.poka_yoke_issues.iter().map(|i| i.title.as_str()).collect();
    assert!(r.critical_issues().is_empty(), "{titles:?}");
    for t in [
        "High-strength steel cadmium plated: hydrogen controls",
        "Bake before the chromate / passivate",
        "Locking patch after the bake",
        "Gauge the thread before the patch",
        "Control coating thickness on the thread",
    ] {
        assert!(titles.contains(&t), "{t} missing from {titles:?}");
    }
    for t in ["Electroplating ≥ 1200 MPa fasteners", "No dimension table for this type", "Very short length for diameter", "Aerospace alloy on a commercial class"] {
        assert!(!titles.contains(&t), "{t} should not fire");
    }
    assert!(!titles.iter().any(|t| t.starts_with("Thread class not per")));
    assert!(r.poka_yoke_issues.iter().find(|i| i.title == "Control coating thickness on the thread").unwrap().fix.contains("8–10 µm"));
}

#[test]
fn report_lists_the_process_in_order() {
    let r = screw();
    let seq = production_report::process_sequence(&r);
    let pos = |f: &dyn Fn(&String) -> bool| seq.iter().position(f).map(|x| x as i64).unwrap_or(-1);
    let plate = pos(&|s| s.starts_with("Plate"));
    let bake = pos(&|s| s.starts_with("Hydrogen"));
    let chromate = pos(&|s| s.starts_with("Chromate"));
    let patch = pos(&|s| s.contains("locking patch"));
    assert!(plate >= 0 && plate < bake && bake < chromate && chromate < patch, "{seq:?}");
    let text = production_report::build(&r, Some(&screw_spec()), "");
    assert!(text.contains("Rm 1000–1240 MPa (drawing)") && text.contains("13000 pcs"));
}

#[test]
fn conversions_and_parsing() {
    assert_eq!(32.0, hardness_conversion::hrc_for(1040.0));
    assert_eq!(39.0, hardness_conversion::hrc_for(1220.0));
    assert_eq!(Some(TensileRange { min_mpa: 1000.0, max_mpa: 1240.0 }), TensileRange::parse("TENSILE STRENGTH = 1000-1240MPa"));
    close(160.0 * 6.894757, TensileRange::parse("160-180 KSI").unwrap().min_mpa, 1e-6);
    assert!(TensileRange::parse("HEAD Ø8.17").is_none());
    assert_eq!(13.0, C::AERO_CAD.thickness_min_um());
    assert_eq!(8.0, C::AERO_CAD_CL2.thickness_min_um());
    assert_eq!(5.0, C::AERO_CAD_CL3.thickness_min_um());
}

#[test]
fn warm_heading_only_where_it_belongs() {
    assert!(!calculation::is_warm_headed(&mat("4340"), 4.2));
    assert!(calculation::is_warm_headed(&mat("4340"), 20.0));
    assert!(calculation::is_warm_headed(&mat("TI_GR5"), 6.0));
    assert!(!calculation::is_warm_headed(&mat("10B21"), 20.0));
}

#[test]
fn commercial_high_strength_zinc_is_still_blocked() {
    let c = calc(FT::HEX_BOLT, METRIC, PS::ISO_4014, idx(METRIC, 10.0), 1.5, 60.0, "6g", "4140", C::ZINC_BLUE, 100, Some(PropertyClass::CLASS_12_9));
    assert!(c.critical_issues().iter().any(|i| i.title == "Electroplating ≥ 1200 MPa fasteners"));
}

#[test]
fn full_coating_on_a_3a_thread_asks_for_thickness_control_not_rejection() {
    let i = catalog::INCH_SIZES.iter().position(|s| s.d_inch == 0.164).unwrap();
    let r = calc(FT::PAN_HEXALOBULAR, INCH, PS::ASME_B18_6_3, i, 32.0, 6.35, "3A-UNJ", "4340", C::ZN_NI, 100, Some(PropertyClass::CLASS_10_9));
    assert!(!r.poka_yoke_issues.iter().any(|x| x.title == "Coating consumes the whole thread tolerance"));
    assert!(r.poka_yoke_issues.iter().any(|x| x.title == "Control coating thickness on the thread" && x.fix.contains("8–10 µm")));
}

// ---------------- ThreadRollingBlankTest
fn blank_for(tc: &str) -> f64 {
    calc(FT::HEX_BOLT, METRIC, PS::ISO_4014, idx(METRIC, 10.0), 1.5, 60.0, tc, "10B21", C::ZN_NI, 100, Some(PropertyClass::CLASS_8_8)).blank_target_dia
}

#[test]
fn the_volume_balance_puts_the_plain_blank_just_above_d2() {
    close(0.01352, trb::controlled_root_delta(1.0, trb::ROOT_DEPTH_J_SHALLOW), 0.00005);
    close(0.00195, trb::controlled_root_delta(1.0, trb::ROOT_DEPTH_J_DEEP), 0.00005);
    close(0.00747, trb::controlled_root_delta_mid(1.0), 0.00005);
}

#[test]
fn the_correction_scales_with_pitch_and_is_always_positive() {
    for p in [0.5, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5] {
        let d = trb::controlled_root_delta_mid(p);
        close(trb::controlled_root_delta_mid(1.0) * p, d, 1e-9);
        assert!(d > 0.0);
    }
}

#[test]
fn the_correction_band_brackets_the_design_value() {
    let (lo, hi) = trb::controlled_root_delta_range(1.5);
    let mid = trb::controlled_root_delta_mid(1.5);
    assert!(lo < mid && hi > mid);
    assert!(trb::controlled_root_delta(1.5, trb::ROOT_DEPTH_J_SHALLOW) > trb::controlled_root_delta(1.5, trb::ROOT_DEPTH_J_DEEP));
}

#[test]
fn mj_blank_is_larger_than_the_same_class_without_the_j_root() {
    let plain = blank_for("4h6h");
    let mj = blank_for("4h6h-MJ");
    close(trb::controlled_root_delta_mid(1.5), mj - plain, 0.0015);
    assert!(mj > plain);
}

#[test]
fn the_correction_is_a_real_fraction_of_a_4h_band() {
    let r = calc(FT::HEX_BOLT, METRIC, PS::ISO_4014, idx(METRIC, 10.0), 1.5, 60.0, "4h6h-MJ", "10B21", C::ZN_NI, 100, Some(PropertyClass::CLASS_8_8));
    let band = r.pitch_dia_max - r.pitch_dia_min;
    assert!(trb::controlled_root_delta_mid(1.5) / band > 0.10);
    close(10.0 - 0.649519 * 1.5, r.pitch_dia_max, 0.0005);
}

#[test]
fn plain_metric_blanks_are_unchanged_by_the_fix() {
    let km = mat("10B21").km;
    for (d, p) in [(6.0, 1.0), (10.0, 1.5), (16.0, 2.0)] {
        let r = calc(FT::HEX_BOLT, METRIC, PS::ISO_4014, idx(METRIC, d), p, 60.0, "6g", "10B21", C::ZN_NI, 100, Some(PropertyClass::CLASS_8_8));
        let d2t = r.pitch_dia_min + 0.55 * (r.pitch_dia_max - r.pitch_dia_min);
        close(d2t - 0.014 * p + km * p, r.blank_target_dia, 0.0015);
    }
}

#[test]
fn unj_inch_blank_is_larger_than_unr() {
    let i = idx(INCH, 0.375 * 25.4);
    let b = |tc: &str| calc(FT::HEX_BOLT, INCH, PS::ASME_B18_2_1, i, 16.0, 50.0, tc, "10B21", C::ZN_NI, 100, Some(PropertyClass::SAE_GRADE_5)).blank_target_dia;
    let (unr, unj) = (b("3A"), b("3A-UNJ"));
    assert!(unj > unr);
    close(trb::controlled_root_delta_mid(25.4 / 16.0), unj - unr, 0.0015);
}

// ---------------- HeaderRouteTest
fn hr(t: FT, d: f64, len: f64, choice: HeaderChoice, g: DrawingGeometry) -> FastenerCalculationResult {
    let i = idx(METRIC, d);
    let mut c = input(t, METRIC, poka_yoke::default_standard(t, METRIC), i, catalog::METRIC_SIZES[i].pitches[0], len, if t.is_nut() { "6H" } else { "6g" }, "10B21", C::GEOMET, 100);
    c.property_class = Some(PropertyClass::CLASS_10_9);
    c.header_choice = choice;
    c.drawing = g;
    run(c)
}
fn hra(t: FT, d: f64, len: f64) -> FastenerCalculationResult {
    hr(t, d, len, HeaderChoice::AUTO, DrawingGeometry::default())
}

#[test]
fn choosing_one_die_designs_the_bolt_for_one_die() {
    let auto = hra(FT::HEX_BOLT, 10.0, 60.0);
    let r = hr(FT::HEX_BOLT, 10.0, 60.0, HeaderChoice::TWO_BLOW, DrawingGeometry::default());
    let plan = r.heading_plan.as_ref().unwrap();
    assert_eq!(HeaderRoute::TWO_BLOW, plan.route);
    assert_eq!(3, plan.stations.len());
    assert_eq!("1st blow: extrude & cone preform", plan.stations[1].title);
    assert!(!plan.stations.iter().any(|s| s.title.contains("Trim")));
    close(r.finished_part.as_ref().unwrap().volume_mm3(), plan.slug_volume_mm3, 1e-6);
    assert!(r.gross_weight_grams < auto.gross_weight_grams);
    close(plan.slug_dia_mm, r.wire_dia_mm, 1e-9);
    assert!(!r.critical_issues().iter().any(|i| i.title.starts_with("1-die 2-blow")));
    assert!(plan.max_volume_error() < 1e-9);
}

#[test]
fn choosing_multi_station_keeps_screws_on_the_progressive_former() {
    let auto = hra(FT::PAN_HEXALOBULAR, 4.0, 10.0).heading_plan.unwrap();
    let multi = hr(FT::PAN_HEXALOBULAR, 4.0, 10.0, HeaderChoice::MULTI_STATION, DrawingGeometry::default()).heading_plan.unwrap();
    assert_eq!(HeaderRoute::TWO_BLOW, auto.route);
    assert_eq!(HeaderRoute::MULTI_STATION, multi.route);
    assert!(multi.stations.len() > 3);
    assert!(multi.route_reason.contains("your choice"));
}

#[test]
fn one_die_enlarges_the_wire_when_two_blows_need_it() {
    let r = hr(FT::PAN_SLOT, 10.0, 30.0, HeaderChoice::TWO_BLOW, DrawingGeometry { head_dia_mm: Some(30.0), head_height_mm: Some(8.0), ..Default::default() });
    let plan = r.heading_plan.as_ref().unwrap();
    assert!(plan.slug_dia_mm > r.blank_target_dia + 0.1);
    assert_eq!("1st blow: extrude & cone preform", plan.stations[1].title);
    assert!(plan.feasibility_issue.is_none());
    assert!(r.upset_ld_ratio <= heading::TWO_BLOW_LD + 1e-9);
}

#[test]
fn one_die_that_cannot_work_is_blocked() {
    let g = DrawingGeometry { head_dia_mm: Some(14.0), head_height_mm: Some(4.0), ..Default::default() };
    let r = hr(FT::PAN_SLOT, 4.0, 10.0, HeaderChoice::TWO_BLOW, g.clone());
    assert!(r.heading_plan.as_ref().unwrap().feasibility_issue.is_some());
    assert!(r.critical_issues().iter().any(|i| i.title == "1-die 2-blow cannot make this part"));
    let m = hr(FT::PAN_SLOT, 4.0, 10.0, HeaderChoice::MULTI_STATION, g);
    assert!(!m.critical_issues().iter().any(|i| i.title == "1-die 2-blow cannot make this part"));
}

#[test]
fn nuts_stay_on_the_nut_former_whatever_is_chosen() {
    assert_eq!(HeaderRoute::NUT_FORMER, hr(FT::HEX_NUT, 10.0, 8.0, HeaderChoice::TWO_BLOW, DrawingGeometry::default()).heading_plan.unwrap().route);
}

#[test]
fn one_die_two_blow_only_where_it_is_enough() {
    let names: Vec<&str> = HeaderRoute::VALUES.iter().map(|r| r.name()).collect();
    assert_eq!(vec!["MULTI_STATION", "NUT_FORMER", "PRESS", "TWO_BLOW"], names);
    for t in FastenerType::VALUES {
        if t.is_washer() {
            continue;
        }
        let r = hra(t, 10.0, 50.0);
        let plan = r.heading_plan.as_ref().unwrap();
        if plan.route == HeaderRoute::TWO_BLOW {
            assert_eq!(3, plan.stations.len(), "{t:?}");
            assert!(plan.upset_blows <= 2, "{t:?}");
            assert!(!plan.stations.iter().any(|s| s.title.to_lowercase().contains("extrude") || s.title.contains("Trim")), "{t:?}");
            assert!(t.category() == "Machine Screws" || r.finished_part.as_ref().unwrap().style == HeadStyle::HEX_FORMED, "{t:?}");
            assert!(plan.max_volume_error() < 1e-9, "{t:?}");
        } else {
            assert!(plan.route == HeaderRoute::MULTI_STATION || plan.route == HeaderRoute::NUT_FORMER, "{t:?} ran on {:?}", plan.route);
        }
    }
    assert_eq!(HeaderRoute::TWO_BLOW, hra(FT::PAN_HEXALOBULAR, 4.0, 10.0).heading_plan.unwrap().route);
    assert_eq!(HeaderRoute::MULTI_STATION, hra(FT::HEX_BOLT, 10.0, 60.0).heading_plan.unwrap().route);
}

#[test]
fn a_fully_threaded_bolt_is_headed_on_a_progressive_former() {
    let plan = hra(FT::HEX_BOLT_FULL, 10.0, 40.0).heading_plan.unwrap();
    assert_eq!(HeaderRoute::MULTI_STATION, plan.route);
    assert!(plan.stations.iter().any(|s| s.title.contains("Trim")));
    assert!(plan.max_volume_error() < 1e-9);
}

#[test]
fn a_partial_thread_bolt_is_extruded_then_trimmed() {
    let r = hra(FT::HEX_BOLT, 10.0, 60.0);
    assert_eq!(HeaderRoute::MULTI_STATION, r.heading_plan.as_ref().unwrap().route);
    assert!(r.recommended_machine.contains("die progressive former"));
    assert!(r.heading_plan.as_ref().unwrap().stations.iter().any(|s| s.operation.contains("extrude")));
}

#[test]
fn nuts_run_on_a_nut_former_and_washers_are_not_headed() {
    assert_eq!(HeaderRoute::NUT_FORMER, hra(FT::HEX_NUT, 10.0, 10.0).heading_plan.unwrap().route);
    assert!(hra(FT::FLAT_WASHER, 10.0, 2.0).heading_plan.is_none());
}

#[test]
fn head_and_shank_volumes_add_up_to_the_total() {
    for t in FastenerType::VALUES {
        for d in [6.0, 10.0, 20.0] {
            let r = hra(t, d, 5.0 * d);
            close(r.part_volume_mm3(), r.head_volume_mm3() + r.shank_volume_mm3(), 1e-9);
            assert!(r.head_volume_mm3() >= -1e-9, "{t:?} d={d}");
            close(r.part_volume_mm3() / 1000.0 * r.material.density, r.head_weight_grams() + r.shank_weight_grams(), 1e-9);
            if t.is_nut() || t.is_washer() {
                close(r.part_volume_mm3(), r.head_volume_mm3(), 1e-9);
            }
        }
    }
}

#[test]
fn the_scanned_drawing_volume_also_splits_head_from_shank() {
    let s = spec(FT::HEX_BOLT, METRIC, idx(METRIC, 10.0), 10.0, 1.5, 60.0, PropertyClass::CLASS_10_9, "10B21", "6g", C::GEOMET);
    let r = drawing_scan::calculate(&s, HeaderChoice::AUTO, 10000);
    let v = drawing_scan::volume(&s, &r);
    assert!(v.total_mm3 > 0.0);
    close(v.total_mm3, v.head_mm3 + v.shank_mm3(), 1e-9);
    assert!(v.head_mm3 > 0.0);
}
