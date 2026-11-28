//! Ported from app/src/test: AnnealCycleTest, CustomLengthTest, HeatTreatCycleTest, ThreadStandardsTest,
//! StainlessAnnealRouteTest, NewFastenerTypesTest.

#[path = "support/common.rs"]
mod common;
use aerofast_engine::calculation::FastenerCalculationResult;
use aerofast_engine::catalog::{self, MaterialInfo};
use aerofast_engine::dimensions;
use aerofast_engine::part_geometry::DrawingGeometry;
use aerofast_engine::poka_yoke::{CheckInput, PokaYokeIssue};
use aerofast_engine::soft_anneal::AnnealCycle;
use aerofast_engine::tensile::joint_grip;
use aerofast_engine::thread::IsoThreadClass;
use aerofast_engine::*;
use common::*;
use CoatingType as C;
use FastenerType as FT;
use ProductStandard as PS;
use UnitSystem::{INCH, METRIC};

fn ci(s: &str, x: &str) -> bool {
    s.to_lowercase().contains(&x.to_lowercase())
}
fn sph(k: &str, t: f64, wire: f64) -> AnnealCycle {
    soft_anneal::spheroidize(&mat(k), t, wire).unwrap_or_else(|| panic!("{k}"))
}
const T10: f64 = soft_anneal::DEFAULT_CHARGE_TONNES;

// ---------------- AnnealCycleTest
#[test]
fn every_stage_is_timed_and_the_total_is_the_sum() {
    let c = sph("10B21", T10, 0.0);
    assert_eq!(7, c.stages.len());
    for s in &c.stages {
        assert!(s.hours_low > 0.0 && s.hours_high >= s.hours_low, "{}", s.name);
        assert!(!s.detail.trim().is_empty());
        assert!(s.hours_text().contains('h'));
    }
    close(c.stages.iter().map(|s| s.hours_low).sum(), c.total_low(), 1e-9);
    close(c.stages.iter().map(|s| s.hours_high).sum(), c.total_high(), 1e-9);
}

#[test]
fn the_total_cycle_is_a_realistic_furnace_booking() {
    let c = sph("10B21", 10.0, 0.0);
    assert!((18.0..=30.0).contains(&c.total_low()), "{}", c.total_low());
    assert!((24.0..=40.0).contains(&c.total_high()), "{}", c.total_high());
    assert!(c.total_text().contains('h'));
    assert!(c.hours_per_tonne().ends_with("h/t"));
}

#[test]
fn a_heavier_charge_takes_longer_but_less_per_tonne() {
    let small = sph("10B21", 2.0, 0.0);
    let big = sph("10B21", 30.0, 0.0);
    assert!(big.total_low() > small.total_low());
    let pt = |c: &AnnealCycle| (c.total_low() + c.total_high()) / 2.0 / c.charge_tonnes;
    assert!(pt(&big) < pt(&small));
}

fn soak_stage(k: &str, wire: f64) -> soft_anneal::AnnealStage {
    sph(k, T10, wire).stages.into_iter().find(|s| s.name.contains("Soak")).unwrap()
}

#[test]
fn alloy_steel_soaks_longer_than_plain_carbon() {
    assert!(soak_stage("4140", 0.0).hours_high > soak_stage("10B21", 0.0).hours_high);
    assert!(soak_stage("10B21", 0.0).hours_high > soak_stage("1008", 0.0).hours_high);
}

#[test]
fn the_controlled_cool_is_in_the_cycle() {
    let c = sph("10B21", T10, 0.0);
    let slow = c.stages.iter().find(|s| s.detail.contains("20 °C/h")).unwrap();
    let soak_lo = steel::ac1_for(&mat("10B21")).unwrap() - 40.0;
    close((soak_lo - 550.0) / 20.0, slow.hours_low, 1e-9);
    assert!(c.stages.iter().any(|s| s.detail.contains("80 °C/h")));
}

#[test]
fn the_cycle_carries_its_sources_and_caveats() {
    let c = sph("10B21", T10, 0.0);
    assert!(c.sources.len() >= 4);
    assert!(c.cautions.iter().any(|x| x.contains("A1")));
    assert!(c.cautions.iter().any(|x| x.contains("coil mass")));
    assert!(c.basis.contains("10.0 t"));
}

#[test]
fn only_steel_gets_a_spheroidize_cycle() {
    for k in ["SS304", "TI_GR5", "AL7075"] {
        assert!(soft_anneal::spheroidize(&mat(k), T10, 0.0).is_none(), "{k}");
    }
    assert!(soft_anneal::spheroidize(&mat("4140"), T10, 0.0).is_some());
}

#[test]
fn thicker_wire_soaks_longer() {
    let hi = |d: f64| soak_stage("10B21", d).hours_high;
    assert!(hi(20.0) > hi(3.0));
    assert!(hi(10.0) > hi(3.0));
}

#[test]
fn diameter_and_grade_combine_by_taking_the_higher() {
    assert!(soak_stage("4135", 3.5).hours_low >= 8.0);
    assert!(soak_stage("1008", 20.0).hours_low >= 12.0);
}

#[test]
fn diameter_soak_ranges_match_published_table() {
    assert_eq!((6.0, 7.0), steel::soak_hours_for_diameter(3.0));
    assert_eq!((7.0, 9.0), steel::soak_hours_for_diameter(6.0));
    assert_eq!((9.0, 12.0), steel::soak_hours_for_diameter(12.0));
    assert_eq!((12.0, 16.0), steel::soak_hours_for_diameter(20.0));
}

#[test]
fn the_soak_is_no_longer_an_unsourced_blank() {
    let s = soft_anneal::build(&mat("10B21"));
    assert_eq!(AnnealRoute::SPHEROIDIZE, s.route);
    assert!(!s.unsourced.iter().any(|u| u.contains("Soak hours")));
    assert!(!s.soak_time.trim().is_empty());
}

// ---------------- CustomLengthTest
const SIXTEENTH: f64 = 25.4 / 16.0;

#[test]
fn threads_in_joint_when_grip_shorter_than_stack() {
    let g = joint_grip::check(INCH, 25.4, 12.7, 0.0, 0.6 * 25.4);
    assert_eq!(GripStatus::THREADS_IN_JOINT, g.status);
    close(0.1 * 25.4, g.threads_in_joint_mm(), 1e-9);
    close(10.0 * SIXTEENTH + 12.7, g.suggested_length_mm, 1e-9);
}

#[test]
fn runout_threads_count_as_thread_in_the_joint() {
    let two_p = 2.0 * 25.4 / 28.0;
    let g = joint_grip::check(INCH, 36.5125, 12.7, two_p, 22.86);
    assert_eq!(GripStatus::THREADS_IN_JOINT, g.status);
    close(36.5125 - 12.7 - two_p, g.grip_min_mm, 1e-9);
    close(15.0 * SIXTEENTH + 12.7 + two_p, g.suggested_length_mm, 1e-9);
}

#[test]
fn suggested_length_passes_the_check() {
    for stack in [3.0, 7.9, 12.7, 19.05, 31.0] {
        for unit in UnitSystem::VALUES {
            let first = joint_grip::check(unit, 20.0, 10.0, 2.5, stack);
            let again = joint_grip::check(unit, first.suggested_length_mm, 10.0, 2.5, stack);
            assert_eq!(GripStatus::OK, again.status, "{unit:?} stack {stack}");
            assert!(again.excess_mm() < joint_grip::step_mm(unit) + 1e-9);
        }
    }
}

#[test]
fn exact_fit_and_excess_grip() {
    assert_eq!(GripStatus::OK, joint_grip::check(METRIC, 40.0, 15.0, 0.0, 25.0).status);
    let g = joint_grip::check(METRIC, 40.0, 15.0, 0.0, 20.0);
    assert_eq!(GripStatus::EXCESS_GRIP, g.status);
    close(5.0, g.excess_mm(), 1e-9);
}

#[test]
fn grip_check_only_for_threaded_aerospace_bolts() {
    assert!(joint_grip::applies_to(FT::NAS_CLOSE_TOL_BOLT));
    assert!(joint_grip::applies_to(FT::TWELVE_POINT));
    assert!(!joint_grip::applies_to(FT::HEX_BOLT));
    assert!(!joint_grip::applies_to(FT::AERO_LOCK_NUT));
    assert!(!joint_grip::applies_to(FT::SOLID_RIVET));
}

fn gate(t: FT, unit: UnitSystem, len: f64, b: Option<f64>) -> Vec<PokaYokeIssue> {
    let i = idx(unit, if unit == INCH { 6.35 } else { 10.0 });
    let pitch = if unit == METRIC { catalog::METRIC_SIZES[i].pitches[0] } else { catalog::INCH_SIZES[i].tpis[0] as f64 };
    let std = poka_yoke::default_standard(t, unit);
    let m = mat("10B21");
    poka_yoke::check(&CheckInput {
        t,
        unit,
        standard: std,
        nominal_size_index: i,
        pitch_value: pitch,
        d: catalog::diameter_mm(unit, i),
        length_mm: len,
        thread_class: selection_filter::thread_classes(unit, t, std)[0],
        property_class: Some(PropertyClass::CLASS_10_9),
        material: &m,
        coating: C::GEOMET,
        batch_qty: 100,
        fundamental_deviation_mm: 0.0,
        pitch_dia_min_mm: 0.0,
        pitch_dia_max_mm: 100.0,
        is_custom_scan: false,
        stress_area_mm2: 50.0,
        head_dims: None,
        thread_length_mm: b,
        tensile_range: None,
        thread_lock_patch: false,
        head_from_drawing: false,
    })
}

#[test]
fn aerospace_bolt_asks_for_drawing_thread_length() {
    let title = "Aerospace thread length not from the drawing";
    assert!(gate(FT::NAS_CLOSE_TOL_BOLT, INCH, 25.4, None).iter().any(|i| i.title == title));
    assert!(!gate(FT::NAS_CLOSE_TOL_BOLT, INCH, 25.4, Some(12.0)).iter().any(|i| i.title == title));
    assert!(!gate(FT::HEX_BOLT, METRIC, 60.0, None).iter().any(|i| i.title == title));
}

#[test]
fn thread_longer_than_part_is_flagged() {
    let t = "Thread length longer than the part";
    assert!(gate(FT::HEX_BOLT, METRIC, 40.0, Some(45.0)).iter().any(|i| i.title == t));
    assert!(!gate(FT::HEX_BOLT, METRIC, 40.0, Some(20.0)).iter().any(|i| i.title == t));
}

#[test]
fn custom_length_and_thread_length_reach_the_calculation() {
    let go = |b: Option<f64>| {
        let mut c = input(FT::HEX_BOLT, METRIC, poka_yoke::default_standard(FT::HEX_BOLT, METRIC), idx(METRIC, 10.0), 1.5, 47.5, "6g", "10B21", C::GEOMET, 1000);
        c.property_class = Some(PropertyClass::CLASS_10_9);
        c.drawing = DrawingGeometry { thread_length_mm: b, ..Default::default() };
        run(c)
    };
    close(poka_yoke::thread_length_b(FT::HEX_BOLT, METRIC, 10.0, 47.5), go(None).thread_length_mm, 1e-9);
    close(18.0, go(Some(18.0)).thread_length_mm, 1e-9);
}

// ---------------- HeatTreatCycleTest
#[test]
fn temper_time_follows_the_spec_rule() {
    for (s, h) in [(6.0, 2.0), (25.4, 2.0), (30.0, 3.0), (50.8, 3.0), (60.0, 4.0)] {
        close(h, heat_treat_cycle::temper_hours(s), 1e-9);
    }
}

#[test]
fn soak_time_is_one_hour_per_inch_with_an_hour_floor() {
    close(1.0, heat_treat_cycle::soak_hours(5.0), 1e-9);
    close(1.0, heat_treat_cycle::soak_hours(25.4), 1e-9);
    assert!(heat_treat_cycle::soak_hours(50.8) >= 2.0);
    assert!(heat_treat_cycle::soak_hours(76.2) >= 3.0);
}

#[test]
fn a_quench_and_temper_steel_gets_a_fully_timed_cycle() {
    let c = heat_treat_cycle::quench_temper(&mat("10B21"), 10.0, None).unwrap();
    assert!(c.stages.len() >= 6);
    for s in &c.stages {
        assert!(s.hours_low > 0.0 && s.hours_high >= s.hours_low, "{}", s.name);
        assert!(!s.detail.trim().is_empty());
    }
    for n in ["Austenitize soak", "Temper soak", "Quench"] {
        assert!(c.stages.iter().any(|s| s.name.contains(n)), "{n}");
    }
    close(c.stages.iter().map(|s| s.hours_low).sum(), c.total_low(), 1e-9);
    assert!(c.sources.iter().any(|s| s.contains("AMS 2759/1")));
    assert!(c.cautions.iter().any(|s| s.contains("LOAD")));
}

#[test]
fn a_thicker_section_soaks_and_tempers_longer() {
    let thin = heat_treat_cycle::quench_temper(&mat("4140"), 8.0, None).unwrap();
    let thick = heat_treat_cycle::quench_temper(&mat("4140"), 60.0, None).unwrap();
    assert!(thick.total_low() > thin.total_low());
}

#[test]
fn a_high_strength_grade_shows_the_hydrogen_bake() {
    assert!(heat_treat_cycle::quench_temper(&mat("4140"), 10.0, None).unwrap().stages.iter().any(|s| s.name.contains("Hydrogen bake")));
}

#[test]
fn austenitic_and_non_ferrous_grades_get_no_quench_and_temper_cycle() {
    for k in ["SS304", "AL7075", "TI_GR5"] {
        assert!(heat_treat_cycle::quench_temper(&mat(k), 10.0, None).is_none(), "{k}");
    }
}

#[test]
fn aluminium_gets_the_four_stage_o_temper_cycle() {
    let c = heat_treat_cycle::soft_anneal(&mat("AL7075"), 10.0).unwrap();
    assert!(c.stages.iter().any(|s| s.detail.contains("413")));
    close((413.0 - 230.0) / 28.0, c.stages.iter().find(|s| s.name.contains("Controlled cool")).unwrap().hours_low, 0.01);
    assert!(c.cautions.iter().any(|s| s.contains("HARDER")));
    assert!(c.cautions.iter().any(|s| s.contains("10 °C/h")));
    let six = heat_treat_cycle::soft_anneal(&mat("AL6061"), 10.0).unwrap();
    close((413.0 - 260.0) / 28.0, six.stages.iter().find(|s| s.name.contains("Controlled cool")).unwrap().hours_low, 0.01);
}

#[test]
fn non_heat_treatable_aluminium_gets_the_recrystallisation_anneal() {
    let c = heat_treat_cycle::soft_anneal(&mat("AL5052"), 10.0).unwrap();
    assert!(c.stages.iter().any(|s| s.detail.contains("343")));
    assert!(!c.stages.iter().any(|s| s.name.contains("Controlled cool")));
}

#[test]
fn copper_gets_the_generic_envelope_and_says_so() {
    let c = heat_treat_cycle::soft_anneal(&mat("BRASS_C360"), 10.0).unwrap();
    assert!(c.basis.contains("Generic"));
    assert!(c.cautions.iter().any(|s| s.contains("ASTM B601")));
    assert!(heat_treat_cycle::soft_anneal(&mat("CUBE2"), 10.0).unwrap().stages.iter().any(|s| s.name.contains("Water quench")));
}

#[test]
fn carbon_steel_gets_no_soft_anneal_here() {
    assert!(heat_treat_cycle::soft_anneal(&mat("10B21"), 10.0).is_none());
    assert!(heat_treat_cycle::soft_anneal(&mat("4140"), 10.0).is_none());
}

#[test]
fn stainless_gets_a_timed_solution_anneal() {
    let c = heat_treat_cycle::soft_anneal(&mat("SS304"), 10.0).unwrap();
    assert!(c.title.contains("Solution"));
    assert!(c.stages.iter().any(|s| s.name.contains("Solution soak")));
    assert!(c.stages.iter().any(|s| s.name.contains("Rapid quench")));
    assert!(c.cautions.iter().any(|s| s.contains("sensitises")));
    assert!(heat_treat_cycle::soft_anneal(&mat("17_4PH"), 10.0).unwrap().stages.iter().any(|s| s.name.contains("AFTER forming")));
}

#[test]
fn bronzes_are_classified_as_copper_alloys() {
    for k in ["PHOS_BRONZE_C510", "SIL_BRONZE_C655", "CUNI_C715", "BRASS_C360"] {
        assert_eq!(MaterialFamily::COPPER_ALLOY, poka_yoke::material_family(&mat(k)), "{k}");
        assert!(soft_anneal::spheroidize(&mat(k), T10, 0.0).is_none(), "{k}");
    }
}

// ---------------- ThreadStandardsTest
fn ts(cls: &str, nut: bool, d: f64, p: f64, pc: Option<PropertyClass>, coat: C, m: &str) -> FastenerCalculationResult {
    calc(if nut { FT::HEX_NUT } else { FT::HEX_BOLT }, METRIC, if nut { PS::ISO_4032 } else { PS::ISO_4014 }, m_idx(d), p, 60.0, cls, m, coat, 1000, pc)
}
fn ts0(cls: &str, nut: bool) -> FastenerCalculationResult {
    ts(cls, nut, 10.0, 1.5, Some(PropertyClass::CLASS_10_9), C::GEOMET, "10B21")
}
fn itc(p: i32, c: i32, pos: &str, mj: bool) -> IsoThreadClass {
    IsoThreadClass { pitch_grade: p, crest_grade: c, position: pos.into(), is_mj: mj }
}

#[test]
fn parser_reads_pitch_and_crest_tolerances() {
    assert_eq!(itc(4, 6, "g", false), IsoThreadClass::parse("4g6g"));
    assert_eq!(itc(6, 6, "H", false), IsoThreadClass::parse("6H"));
    assert_eq!(itc(4, 5, "H", true), IsoThreadClass::parse("4H5H-MJ"));
    assert_eq!("AZ", IsoThreadClass::parse("6AZ").position);
}

#[test]
fn m10x1_5_6g_matches_iso965_table() {
    let r = ts0("6g", false);
    close(8.994, r.pitch_dia_max, 0.001);
    close(9.968, r.major_dia_max, 0.001);
}

#[test]
fn deviations_order_by_position_and_grades_scale_tolerance() {
    let es: Vec<f64> = ["6d", "6e", "6f", "6g", "6h"].iter().map(|c| ts0(c, false).fundamental_deviation_mm).collect();
    let mut sorted = es.clone();
    sorted.sort_by(|a, b| a.partial_cmp(b).unwrap());
    assert_eq!(sorted, es);
    let td = |c: &str| {
        let r = ts0(c, false);
        r.pitch_dia_max - r.pitch_dia_min
    };
    close(2.0, td("9g8g") / td("6g"), 0.01);
}

#[test]
fn every_catalogue_class_computes_a_valid_thread() {
    let ext = catalog::thread_class_catalog::get_options(METRIC, false).iter().map(|c| (*c, false));
    let int = catalog::thread_class_catalog::get_options(METRIC, true).iter().map(|c| (*c, true));
    for (c, nut) in ext.chain(int) {
        let r = ts0(c, nut);
        assert!(r.pitch_dia_min < r.pitch_dia_max, "{c} d2");
        assert!(r.minor_dia_min < r.minor_dia_max, "{c} minor");
        assert!(r.blank_target_dia.is_finite() && r.blank_target_dia > 0.0, "{c}");
    }
}

#[test]
fn galvanized_nut_classes_use_iso9655_deviation() {
    close(0.330, ts0("6AZ", true).fundamental_deviation_mm, 1e-6);
    close(0.310, ts0("6AX", true).fundamental_deviation_mm, 1e-6);
}

#[test]
fn mj_threads_have_the_controlled_root() {
    close(0.15011 * 1.5, ts0("4h6h-MJ", false).root_radius_min_mm.unwrap(), 1e-4);
}

#[test]
fn sizes_are_ordered_and_include_iso261_and_asme_b11() {
    let m: Vec<f64> = catalog::METRIC_SIZES.iter().map(|s| s.d).collect();
    assert!(m.windows(2).all(|w| w[0] <= w[1]));
    for d in [1.2, 1.4, 3.5, 18.0, 22.0, 27.0, 33.0, 39.0, 42.0, 45.0, 48.0, 52.0, 56.0, 60.0, 64.0] {
        assert!(m.contains(&d), "M{d}");
    }
    for s in catalog::METRIC_SIZES {
        let mx = s.pitches.iter().copied().fold(f64::MIN, f64::max);
        assert_eq!(mx, s.pitches[0], "{} coarse first", s.label);
    }
    let i: Vec<f64> = catalog::INCH_SIZES.iter().map(|s| s.d_inch).collect();
    assert!(i.windows(2).all(|w| w[0] <= w[1]));
    assert!(!catalog::INCH_SIZES[0].has_unc);
    for s in catalog::INCH_SIZES {
        assert!(s.tpis.iter().all(|&t| t >= 5), "{}", s.label);
    }
}

#[test]
fn defaults_and_legacy_saves_still_point_at_the_same_size() {
    assert_eq!(10.0, catalog::diameter_mm(METRIC, catalog::default_size_index(METRIC)));
    close(0.375 * 25.4, catalog::diameter_mm(INCH, catalog::default_size_index(INCH)), 1e-9);
    assert_eq!(10.0, catalog::diameter_mm(METRIC, catalog::migrate_legacy_size_index(METRIC, 11)));
    assert_eq!(36.0, catalog::diameter_mm(METRIC, catalog::migrate_legacy_size_index(METRIC, 18)));
    close(9.525, catalog::diameter_mm(INCH, catalog::migrate_legacy_size_index(INCH, 8)), 1e-9);
}

#[test]
fn every_property_class_has_strength_and_a_nut_match() {
    let m = mat("4140");
    for pc in PropertyClass::VALUES {
        let cs = poka_yoke::class_strength(pc, 10.0, &m);
        assert!(cs.rm_min_mpa > 0.0 && cs.yield_min_mpa <= cs.rm_min_mpa && cs.proof_stress_mpa <= cs.rm_min_mpa, "{pc:?}");
        assert!(!poka_yoke::nut_class_for(pc).trim().is_empty(), "{pc:?}");
    }
    assert_eq!(724.0, poka_yoke::class_strength(PropertyClass::SAE_GRADE_5, 30.0, &m).rm_min_mpa);
    assert_eq!("ASTM A194 Grade 2H", poka_yoke::nut_class_for(PropertyClass::ASTM_A193_B7));
}

#[test]
fn class9_8_is_blocked_above_m16() {
    let r = ts("6g", false, 20.0, 2.5, Some(PropertyClass::CLASS_9_8), C::GEOMET, "10B21");
    assert!(r.poka_yoke_issues.iter().any(|i| i.severity == PokaYokeSeverity::CRITICAL && i.title == "Class 9.8 is limited to M16"));
}

#[test]
fn anodize_on_steel_is_blocked_on_aluminium_is_not() {
    let blocked = |m: &str| ts("6g", false, 10.0, 1.5, None, C::ANODIZE_II, m).poka_yoke_issues.iter().any(|i| i.title.contains("is for aluminium only"));
    assert!(blocked("10B21"));
    assert!(!blocked("AL7075"));
}

// ---------------- StainlessAnnealRouteTest
fn cyc(k: &str) -> heat_treat_cycle::FurnaceCycle {
    heat_treat_cycle::soft_anneal(&mat(k), 10.0).unwrap_or_else(|| panic!("{k}"))
}

#[test]
fn martensitic_grades_are_not_quenched_soft() {
    for k in ["SS410", "20X13", "30X13"] {
        let c = cyc(k);
        assert!(c.title.contains("Martensitic"), "{k}");
        assert!(!c.stages.iter().any(|s| ci(&s.name, "quench")), "{k}");
        assert!(c.stages.iter().any(|s| s.name.contains("Furnace cool")), "{k}");
        assert!(c.cautions.iter().any(|s| s.contains("HARDENING")), "{k}");
    }
}

#[test]
fn nickel_bearing_431_type_gets_the_sub_critical_hold() {
    for k in ["SS431", "14KH17N2"] {
        let c = cyc(k);
        assert!(c.stages.iter().any(|s| s.detail.contains("620 – 660")), "{k}");
        assert!(c.stages.iter().any(|s| s.detail.contains("230 – 270 HB")), "{k}");
        assert!(!c.stages.iter().any(|s| s.name.contains("Furnace cool")), "{k}");
    }
}

#[test]
fn ferritic_430_recrystallises_and_misses_the_embrittlement_band() {
    let c = cyc("SS430");
    assert!(c.title.contains("Ferritic"));
    assert!(c.stages.iter().any(|s| s.detail.contains("705 – 790")));
    assert!(c.stages.iter().map(|s| &s.detail).chain(c.cautions.iter()).any(|s| s.contains("475 °C")));
    assert!(!c.stages.iter().any(|s| ci(&s.name, "quench")));
    assert!(!c.stages.iter().any(|s| s.name.contains("Furnace cool")));
}

#[test]
fn austenitic_duplex_and_ph_keep_the_true_solution_anneal() {
    for k in ["SS304", "SS316", "NITRONIC60", "DUPLEX_2205", "SUPER_DUPLEX_2507", "17_4PH"] {
        assert!(cyc(k).stages.iter().any(|s| s.name.contains("Rapid quench")), "{k}");
    }
    assert!(cyc("17_4PH").stages.iter().any(|s| s.name.contains("Age AFTER forming")));
}

#[test]
fn every_stainless_grade_gets_the_cycle_its_subfamily_needs() {
    let mut n = 0;
    for m in catalog::materials() {
        let Some(t) = poka_yoke::stainless_type(m) else { continue };
        let c = heat_treat_cycle::soft_anneal(m, 10.0).unwrap_or_else(|| panic!("{}", m.key));
        let quenched = c.stages.iter().any(|s| s.name.contains("Rapid quench"));
        let should = t != StainlessType::MARTENSITIC && t != StainlessType::FERRITIC;
        assert_eq!(should, quenched, "{} ({t:?})", m.key);
        n += 1;
    }
    assert!(n >= 20);
}

#[test]
fn the_card_no_longer_tells_a_martensitic_to_quench_itself_soft() {
    let b = |k: &str| soft_anneal::build(&mat(k));
    assert!(b("SS410").temperature_c.contains("845 – 885"));
    assert!(b("SS410").cooling_rate.contains("Furnace cool"));
    assert!(b("SS431").temperature_c.contains("620 – 660"));
    assert!(b("SS430").temperature_c.contains("705 – 790"));
    assert!(b("SS304").cooling_rate.contains("Rapid"));
}

#[test]
fn maraging_is_solution_treated_and_aged_never_quenched_and_tempered() {
    let c = heat_treat_cycle::quench_temper(&mat("MARAGING_250"), 10.0, None).unwrap();
    assert!(c.title.contains("Solution treat & age"));
    assert!(!c.stages.iter().any(|s| s.name.contains("Quench")));
    assert!(!c.stages.iter().any(|s| s.detail.contains("untempered martensite")));
    assert!(c.stages.iter().any(|s| s.name.contains("Air cool")));
    assert!(c.stages.iter().any(|s| s.name.contains("Age") && s.hours_high >= 3.0));
}

#[test]
fn maraging_wire_preparation_has_its_own_timed_cycle() {
    let c = cyc("MARAGING_250");
    assert!(c.title.contains("Solution anneal"));
    assert!(c.stages.iter().any(|s| s.detail.contains("815")));
    assert!(!c.stages.iter().any(|s| s.name.contains("Quench")));
    assert!(c.total_low() > 0.0);
}

#[test]
fn ordinary_steels_still_get_the_real_quench_and_temper() {
    let c = heat_treat_cycle::quench_temper(&mat("4140"), 10.0, None).unwrap();
    assert!(c.title.contains("Quench & temper"));
    assert!(c.stages.iter().any(|s| s.name.contains("Quench")));
}

// ---------------- NewFastenerTypesTest
const NEW_TYPES: [FT; 16] = [
    FT::TTY_HEAD_BOLT, FT::HEXALOBULAR_SCREW, FT::THREAD_FORMING_SCREW, FT::WHEEL_BOLT, FT::BANJO_BOLT, FT::WHEEL_NUT, FT::NAS_CLOSE_TOL_BOLT, FT::HI_LOK_PIN,
    FT::TAPER_LOK, FT::LOCKBOLT_PIN, FT::BLIND_BOLT, FT::AERO_LOCK_NUT, FT::HI_LOK_COLLAR, FT::NUTPLATE, FT::SOLID_RIVET, FT::BLIND_RIVET,
];

fn nt(t: FT, unit: UnitSystem) -> FastenerCalculationResult {
    let i = if unit == METRIC { m_idx(10.0) } else { catalog::INCH_SIZES.iter().position(|s| s.d_inch == 0.375).unwrap() };
    let tc = if unit == METRIC { if t.is_nut() { "6H" } else { "6g" } } else if t.is_nut() { "2B" } else { "2A" };
    calc(t, unit, poka_yoke::default_standard(t, unit), i, if unit == METRIC { 1.5 } else { 16.0 }, 50.0, tc, "10B21", C::GEOMET, 1000, Some(PropertyClass::CLASS_10_9))
}

#[test]
fn every_new_type_calculates_in_both_units_with_a_matching_standard() {
    for t in NEW_TYPES {
        for u in UnitSystem::VALUES {
            let r = nt(t, u);
            let misfit: Vec<_> = r
                .poka_yoke_issues
                .iter()
                .filter(|i| i.severity == PokaYokeSeverity::CRITICAL && (i.title.starts_with("Standard does not fit") || i.title.starts_with("Metric standard with inch")))
                .collect();
            assert!(misfit.is_empty(), "{t:?} {u:?}: {misfit:?}");
            assert!(r.net_weight_grams > 0.0);
            if let Some(p) = &r.heading_plan {
                assert!(p.max_volume_error() < 1e-9, "{t:?} {u:?}");
            }
        }
    }
}

#[test]
fn nut_types_get_nut_standards_and_nut_heading_plans() {
    for t in [FT::WHEEL_NUT, FT::AERO_LOCK_NUT, FT::HI_LOK_COLLAR, FT::NUTPLATE] {
        assert!(t.is_nut());
        assert_eq!("Pierce", nt(t, METRIC).heading_plan.unwrap().stations.last().unwrap().title);
    }
    assert_eq!(PS::DIN_74361_NUT, poka_yoke::default_standard(FT::WHEEL_NUT, METRIC));
    assert_eq!(PS::DIN_74361, poka_yoke::default_standard(FT::WHEEL_BOLT, METRIC));
}

#[test]
fn types_without_a_table_are_labelled_approximated_and_warned() {
    let d = dimensions::for_part(FT::WHEEL_BOLT, METRIC, 10.0);
    assert!(!d.is_table);
    assert!(d.source.starts_with("Approximated") && d.source.contains("DIN 74361"));
    assert!(nt(FT::WHEEL_BOLT, METRIC).poka_yoke_issues.iter().any(|i| i.title == "No dimension table for this type"));
}

#[test]
fn hexalobular_uses_iso4762_head_and_has_a_recess() {
    let d = dimensions::for_part(FT::HEXALOBULAR_SCREW, METRIC, 10.0);
    assert!(d.is_table);
    assert!(d.source.starts_with("ISO 14579"));
    assert!(dimensions::socket_for(FT::HEXALOBULAR_SCREW, METRIC, 10.0).is_some());
    let r = nt(FT::HEXALOBULAR_SCREW, METRIC);
    assert!(r.heading_plan.unwrap().stations.last().unwrap().operation.contains("hexalobular"));
}

#[test]
fn thread_forming_screw_is_fully_threaded() {
    assert_eq!(50.0, poka_yoke::thread_length_b(FT::THREAD_FORMING_SCREW, METRIC, 10.0, 50.0));
}

fn rivet(t: FT) -> FastenerCalculationResult {
    calc(t, METRIC, poka_yoke::default_standard(t, METRIC), m_idx(5.0), 0.8, 12.0, "6g", "AL2117", C::GEOMET, 1000, None)
}

#[test]
fn solid_rivet_has_no_thread_anywhere() {
    let r = rivet(FT::SOLID_RIVET);
    assert!(r.thread_label.contains("rivet (no thread)"));
    assert_eq!(0.0, r.thread_length_mm);
    assert_eq!(0.0, r.cost_cnc_turning);
    assert_eq!(1.0, r.stress_concentration_kt);
    close(std::f64::consts::PI / 4.0 * 25.0, r.stress_area_as_mm2, 0.01);
    assert_eq!(0.0, r.cost_thread_rolling);
    assert!(!r.poka_yoke_issues.iter().any(|i| i.title.starts_with("Mate with nut class")));
    assert!(r.poka_yoke_issues.iter().any(|i| i.title == "Rivet joints are designed in shear"));
    let plan = r.heading_plan.as_ref().unwrap();
    assert!(plan.off_header_ops.is_empty());
    assert_eq!("Head upset (universal head)", plan.stations.last().unwrap().title);
    assert_eq!(HeaderRoute::MULTI_STATION, plan.route);
    assert_eq!(SectionShape::DOME, plan.stations.last().unwrap().segments.last().unwrap().shape);
    assert!(plan.max_volume_error() < 1e-9);
    assert!(!r.forging_stages.iter().any(|s| s.title.contains("Thread rolling")));
}

#[test]
fn blind_rivet_says_what_is_not_modelled() {
    assert!(rivet(FT::BLIND_RIVET).heading_plan.unwrap().notes.iter().any(|n| n.contains("sleeve bore and the mandrel")));
    assert_eq!(PS::NAS1398, poka_yoke::default_standard(FT::BLIND_RIVET, INCH));
    assert_eq!(PS::MS20470, poka_yoke::default_standard(FT::SOLID_RIVET, METRIC));
}

#[allow(dead_code)]
fn _unused(_: &MaterialInfo) {}
