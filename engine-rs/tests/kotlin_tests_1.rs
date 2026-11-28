//! Ported from app/src/test: FlangeProductTest, DimensionAndNutTest, HeatTreatAuditTest, WireChoiceTest,
//! MillCertTest, BoltForgingTest. Assertions that need the AI / offline-knowledge modules (not part of the
//! engine) are left out and marked.

#[path = "support/common.rs"]
mod common;
use aerofast_engine::calculation::{self, FastenerCalculationResult};
use aerofast_engine::dimensions::{self, HeadDimensions};
use aerofast_engine::part_geometry::DrawingGeometry;
use aerofast_engine::tensile::MillCert;
use aerofast_engine::tooling::{self, MachineSetup};
use aerofast_engine::*;
use common::*;
use CoatingType as C;
use FastenerType as FT;
use ProductStandard as PS;
use UnitSystem::{INCH, METRIC};

// ---------------- FlangeProductTest
fn flange_calc(t: FT, std: PS, d: f64, len: f64, tc: &str) -> FastenerCalculationResult {
    calc(t, METRIC, std, idx(METRIC, d), if d == 10.0 { 1.5 } else { 1.25 }, len, tc, "10B21", C::ZN_NI, 10000, Some(PropertyClass::CLASS_8_8))
}

#[test]
fn flange_bolt_has_its_flange_without_a_drawing() {
    let r = flange_calc(FT::FLANGE_BOLT, PS::ISO_1665, 10.0, 40.0, "6g");
    let p = r.finished_part.as_ref().unwrap();
    let fl: Vec<_> = p.segments.iter().filter(|s| s.shape == SectionShape::ROUND && s.d_bottom_mm == 22.3).collect();
    assert_eq!(fl.len(), 1);
    close(1.5, fl[0].height_mm, 1e-9);
    let hex = p.segments.last().unwrap();
    assert_eq!(SectionShape::HEX, hex.shape);
    close(15.0, hex.d_bottom_mm, 1e-9);
    close(9.2 - 1.5, hex.height_mm, 1e-9);
    assert!(r.heading_plan.as_ref().unwrap().stations.last().unwrap().operation.contains("Coin flange Ø22.30"));
    assert!(!p.cautions.iter().any(|c| c.contains("not in the volume.") && c.starts_with("Flange Ø dc and thickness c are not both")));
}

#[test]
fn flange_nut_uses_din6923_not_the_plain_hex_nut() {
    let r = flange_calc(FT::FLANGE_NUT, PS::DIN_6923, 10.0, 10.0, "6H");
    assert_eq!("DIN 6923 (supplier tables)", r.head_dimensions.as_ref().unwrap().source);
    let segs = &r.finished_part.as_ref().unwrap().segments;
    close(21.8, segs[0].d_bottom_mm, 1e-9);
    close(1.5, segs[0].height_mm, 1e-9);
    close(15.0, segs.last().unwrap().d_bottom_mm, 1e-9);
    close(10.0, segs.iter().map(|s| s.height_mm).sum(), 1e-9);
    assert!(r.heading_plan.as_ref().unwrap().stations.iter().any(|s| s.operation.contains("flange Ø21.80")));
}

#[test]
fn sizes_outside_the_tables_say_they_are_approximated() {
    assert!(dimensions::for_part(FT::FLANGE_NUT, METRIC, 14.0).source.starts_with("Approximated"));
    assert_eq!(None, bolt_forging::din6921(20.0).unwrap().c);
}

// ---------------- DimensionAndNutTest
fn dn_calc(t: FT, std: PS, cls: &str) -> FastenerCalculationResult {
    calc(t, METRIC, std, m_idx(10.0), 1.5, 60.0, cls, "10B21", C::GEOMET, 1000, Some(PropertyClass::CLASS_10_9))
}

#[test]
fn tables_match_iso_and_asme() {
    let h = dimensions::for_part(FT::HEX_BOLT, METRIC, 10.0);
    close(16.0, h.across_flats_s, 1e-9);
    close(6.4, h.head_height_k, 1e-9);
    close(8.4, h.nut_height_m, 1e-9);
    close(16.0, dimensions::for_part(FT::SHCS, METRIC, 10.0).across_flats_s, 1e-9);
    close(0.5625 * 25.4, dimensions::for_part(FT::HEX_BOLT, INCH, 9.525).across_flats_s, 1e-6);
    assert!(!dimensions::for_part(FT::HEX_BOLT, METRIC, 0.5).is_table);
}

#[test]
fn weights_match_catalogue_values() {
    close(44.5, dn_calc(FT::HEX_BOLT, PS::ISO_4014, "6g").net_weight_grams, 2.5);
    close(10.9, dn_calc(FT::HEX_NUT, PS::ISO_4032, "6H").net_weight_grams, 1.0);
}

#[test]
fn bolt_requires_matching_nut_class() {
    let b = dn_calc(FT::HEX_BOLT, PS::ISO_4014, "6g");
    assert!(b.poka_yoke_issues.iter().any(|i| i.severity == PokaYokeSeverity::MANDATORY && i.title.contains("nut class 10")));
}

#[test]
fn nut_is_class10_with_bolt_breaking_proof_load() {
    let n = dn_calc(FT::HEX_NUT, PS::ISO_4032, "6H");
    assert!(n.poka_yoke_issues.iter().any(|i| i.title.contains("Nut class 10")));
    close(60.3, n.proof_load_kn, 0.2);
}

// ---------------- HeatTreatAuditTest
fn ht(k: &str, d: f64, pc: Option<PropertyClass>) -> catalog::HeatTreatmentSpec {
    calculation::calculate_dynamic_heat_treatment(&mat(k), d, false, pc)
}

#[test]
fn ti64_is_solution_treated_below_the_beta_transus_and_water_quenched() {
    let t = ht("TI_GR5", 10.0, None);
    assert!(t.austenitizing_or_solution_temp_c.starts_with("913°C – 954°C"));
    assert!(t.quench_medium.contains("Water"));
    assert!(t.tempering_or_aging_temp_c.contains("524°C – 552°C"));
    assert!(t.standard_reference.contains("AMS 4965"));
}

#[test]
fn beta_c_and_ti3al25v_keep_their_own_cycles() {
    for k in ["TI_BETA_C", "TI_3AL25V"] {
        assert_eq!(mat(k).heat_treatment_spec.austenitizing_or_solution_temp_c, ht(k, 10.0, None).austenitizing_or_solution_temp_c);
    }
}

#[test]
fn waspaloy_solution_is_four_hours() {
    assert!(ht("WASPALOY", 10.0, None).soaking_time_minutes.starts_with("240 mins"));
}

#[test]
fn spec_card_and_cycle_card_show_the_same_times_and_temper() {
    let spec = ht("4140", 10.0, Some(PropertyClass::CLASS_10_9));
    assert!(spec.tempering_or_aging_time_hours.starts_with("2.0 hrs"), "{}", spec.tempering_or_aging_time_hours);
    assert!(spec.soaking_time_minutes.starts_with("1.0 hrs"));
    let cycle = heat_treat_cycle::quench_temper(&mat("4140"), 10.0, Some(&spec)).unwrap();
    close(2.0, cycle.stages.iter().find(|s| s.name.contains("Temper soak")).unwrap().hours_low, 1e-9);
    assert!(cycle.stages.iter().any(|s| s.detail.contains("500°C – 560°C")));
}

#[test]
fn cold_formed_class_has_no_quench_and_temper_cycle() {
    let spec = ht("10B21", 10.0, Some(PropertyClass::CLASS_5_8));
    assert!(heat_treat_cycle::quench_temper(&mat("10B21"), 10.0, Some(&spec)).is_none());
}

#[test]
fn spheroidize_soak_keeps_a_fifteen_degree_margin_under_a1() {
    for k in ["10B21", "4135", "4140", "4340"] {
        let ac1 = steel::ac1_for(&mat(k)).unwrap();
        let top = range_of(&soft_anneal::spheroidize(&mat(k), soft_anneal::DEFAULT_CHARGE_TONNES, 0.0).unwrap().soak_temp_c).1;
        assert!(top <= ac1 - 14.5, "{k} soak top {top} vs A1 {ac1}");
    }
    assert!(!soft_anneal::build(&mat("4140")).cooling_rate.contains("transformation range"));
}

// ---------------- WireChoiceTest
fn shcs(wire: Option<f64>, choice: HeaderChoice) -> FastenerCalculationResult {
    let mut c = input(FT::SHCS, METRIC, PS::ISO_4762, idx(METRIC, 10.0), 1.5, 50.0, "6g", "4140", C::ZN_NI, 10000);
    c.property_class = Some(PropertyClass::CLASS_12_9);
    c.wire_dia_override_mm = wire;
    c.header_choice = choice;
    run(c)
}
fn cone_stations(r: &FastenerCalculationResult) -> usize {
    r.heading_plan.as_ref().unwrap().stations.iter().filter(|s| s.title.to_lowercase().contains("cone")).count()
}

#[test]
fn wire_auto_is_unchanged() {
    let r = shcs(None, HeaderChoice::AUTO);
    assert!(r.wire_dia_override_mm.is_none());
    close(9.85, r.wire_dia_mm, 1e-9);
    close(r.wire_dia_mm, r.auto_wire_dia_mm, 1e-9);
}

#[test]
fn thicker_wire_lowers_ld_and_drops_the_cone_blow() {
    let auto = shcs(None, HeaderChoice::AUTO);
    let thick = shcs(Some(11.0), HeaderChoice::AUTO);
    close(11.0, thick.wire_dia_mm, 1e-9);
    close(9.85, thick.auto_wire_dia_mm, 1e-9);
    assert!(thick.upset_ld_ratio < heading::SINGLE_BLOW_LD);
    assert_eq!(1, cone_stations(&auto));
    assert_eq!(0, cone_stations(&thick));
    assert!(thick.heading_plan.as_ref().unwrap().die_stations() < auto.heading_plan.as_ref().unwrap().die_stations());
    assert!(thick.cut_off_length_mm < auto.cut_off_length_mm);
    let sheet = tooling::build(&thick, &MachineSetup::default()).unwrap();
    assert!(sheet.stations.iter().all(|st| !st.items.iter().any(|i| i.name == "Cone punch type")));
}

#[test]
fn thinner_wire_raises_ld_adds_blows_and_is_flagged() {
    let thin = shcs(Some(7.5), HeaderChoice::AUTO);
    assert!(thin.upset_ld_ratio > heading::TWO_BLOW_LD);
    assert!(cone_stations(&thin) >= 2);
    assert!(thin.poka_yoke_issues.iter().any(|i| i.title == "Wire under the shank"));
    assert!(thin.poka_yoke_issues.iter().any(|i| i.title == "Wire under the rolling blank"));
}

#[test]
fn far_too_thin_is_critical() {
    let r = shcs(Some(6.0), HeaderChoice::AUTO);
    assert!(r.upset_ld_ratio > heading::THREE_BLOW_LD);
    let v: Vec<_> = r.poka_yoke_issues.iter().filter(|i| i.title == "Wire too thin to gather the head").collect();
    assert_eq!(1, v.len());
    assert_eq!(PokaYokeSeverity::CRITICAL, v[0].severity);
}

#[test]
fn two_blow_keeps_the_chosen_wire_and_says_when_it_cannot() {
    let r = shcs(Some(7.5), HeaderChoice::TWO_BLOW);
    close(7.5, r.heading_plan.as_ref().unwrap().slug_dia_mm, 1e-9);
    assert!(r.poka_yoke_issues.iter().any(|i| i.title == "1-die 2-blow cannot make this part"));
}
// whatIfOnAnotherSizeDropsTheChosenWire: uses OfflineKnowledge / QuestionParser (not in the engine)

// ---------------- MillCertTest
fn mc_calc(t: FT, std: PS, m: &str, cert: Option<MillCert>) -> FastenerCalculationResult {
    let mut c = input(t, METRIC, std, idx(METRIC, 10.0), 1.5, 50.0, "6g", m, C::ZN_NI, 10000);
    c.property_class = Some(PropertyClass::CLASS_12_9);
    c.mill_cert = cert;
    run(c)
}
const HARD: &str = "Wire too hard for the socket head";

#[test]
fn within_the_catalogue_range_the_socket_head_is_headable() {
    let r = mc_calc(FT::SHCS, PS::ISO_4762, "4140", MillCert::of(Some(700.0), None));
    let w = r.wire_check.as_ref().unwrap();
    assert_eq!(WireVerdict::HEADABLE, w.verdict);
    assert!(w.recess_head);
    assert!(!r.poka_yoke_issues.iter().any(|i| i.title == HARD));
}

#[test]
fn hard_wire_must_be_annealed_before_the_socket() {
    let r = mc_calc(FT::SHCS, PS::ISO_4762, "4140", MillCert::of(Some(850.0), None));
    assert_eq!(WireVerdict::TOO_HARD, r.wire_check.as_ref().unwrap().verdict);
    let v: Vec<_> = r.poka_yoke_issues.iter().filter(|i| i.title == HARD).collect();
    assert_eq!(1, v.len());
    assert_eq!(PokaYokeSeverity::MANDATORY, v[0].severity);
    assert!(v[0].detail.contains("650 – 730 MPa") && v[0].detail.contains("Rm 850 MPa"), "{}", v[0].detail);
    assert!(production_report::process_sequence(&r)[0].contains("mill cert above the headable range"));
    assert!(production_report::build(&r, None, "").contains("Mill cert (wire)"));
}

#[test]
fn a_hex_bolt_with_hard_wire_is_a_warning_not_the_socket_rule() {
    let r = mc_calc(FT::HEX_BOLT, PS::ISO_4014, "4140", MillCert::of(Some(850.0), None));
    assert!(!r.poka_yoke_issues.iter().any(|i| i.title == HARD));
    let v: Vec<_> = r.poka_yoke_issues.iter().filter(|i| i.title == "Wire harder than the headable range").collect();
    assert_eq!(1, v.len());
    assert_eq!(PokaYokeSeverity::WARNING, v[0].severity);
}

#[test]
fn hardness_is_compared_with_the_derived_hrb_band() {
    let v = |h: f64| mc_calc(FT::SHCS, PS::ISO_4762, "4140", MillCert::of(None, Some(h))).wire_check.unwrap().verdict;
    assert_eq!(WireVerdict::TOO_HARD, v(99.0));
    assert_eq!(WireVerdict::HEADABLE, v(94.0));
    assert_eq!(WireVerdict::SOFTER_THAN_RANGE, v(88.0));
}

#[test]
fn softer_wire_is_reported_not_blocked() {
    let r = mc_calc(FT::SHCS, PS::ISO_4762, "10B21", MillCert::of(Some(450.0), None));
    assert_eq!(WireVerdict::SOFTER_THAN_RANGE, r.wire_check.as_ref().unwrap().verdict);
    assert!(!r.poka_yoke_issues.iter().any(|i| i.title == HARD));
}

#[test]
fn nothing_entered_nothing_checked() {
    assert!(MillCert::of(None, None).is_none());
    assert!(MillCert::of(Some(0.0), Some(0.0)).is_none());
    assert!(mc_calc(FT::SHCS, PS::ISO_4762, "4140", None).wire_check.is_none());
}
// anotherGradeInAWhatIfDoesNotKeepTheCertificate: uses OfflineKnowledge / QuestionParser (not in the engine)

// ---------------- BoltForgingTest
#[test]
fn m10_hex_matches_hand_calc() {
    let head = HeadDimensions { across_flats_s: 16.0, head_height_k: 6.4, bearing_dia_dw: 14.63, nut_height_m: 8.4, source: "ISO 4014".into(), is_table: true, neck_height_mm: None };
    let f = bolt_forging::compute(FT::HEX_BOLT, METRIC, 10.0, 1.5, 60.0, 8.962, 9.85, Some(&head), 7.85, None, None, None, None, None, None);
    close(26.0, f.thread_length_mm, 1e-9);
    close(1.66, f.cone_length_mm, 0.01);
    close(23.04, f.stock_to_extrude_mm, 0.01);
    close(55.38, f.length_under_head_before_mm, 0.01);
    close(434.1, f.cheese_volume_mm3.unwrap(), 1.0);
    close(24.32, f.head_stock_mm.unwrap(), 0.01);
    close(79.70, f.cut_off_length_mm.unwrap(), 0.02);
    close(f.cut_off_length_mm.unwrap() * std::f64::consts::PI / 4.0 * 9.85 * 9.85, f.finished_volume_mm3.unwrap() + f.cheese_volume_mm3.unwrap(), 0.5);
}

#[test]
fn die_progression_uses_same_slug_as_card() {
    let m = mat("10B21");
    for d in [6.0, 10.0, 16.0] {
        for len in [40.0, 60.0, 150.0] {
            for scan in [false, true] {
                let i = idx(METRIC, d);
                let pitch = catalog::METRIC_SIZES[i].pitches[0];
                let mut c = input(FT::HEX_BOLT, METRIC, PS::ISO_4014, i, pitch, len, "6g", "10B21", C::ZN_NI, 100);
                c.property_class = Some(PropertyClass::CLASS_8_8);
                if scan {
                    c.drawing = DrawingGeometry { thread_length_mm: Some(2.0 * d + 10.0), shank_dia_mm: Some(d - 0.1), across_flats_mm: Some(d * 1.5), head_height_mm: Some(d * 0.65), ..Default::default() };
                }
                let r = run(c);
                let plan = r.heading_plan.as_ref().unwrap();
                let card = bolt_forging::compute(
                    FT::HEX_BOLT, METRIC, d, pitch, len, r.blank_target_dia, r.wire_dia_mm, r.head_dimensions.as_ref(), m.density,
                    Some(if scan { 2.0 * d + 10.0 } else { poka_yoke::thread_length_b(FT::HEX_BOLT, METRIC, d, len) }),
                    if scan { Some(d - 0.1) } else { None },
                    if scan { Some(d * 1.5) } else { None },
                    if scan { Some(d * 0.65) } else { None },
                    None,
                    None,
                );
                if plan.route == HeaderRoute::MULTI_STATION {
                    close(card.cut_off_length_mm.unwrap(), plan.slug_length_mm, 0.01);
                    assert_eq!("Trim to hex", plan.stations.last().unwrap().title);
                }
                assert!(plan.max_volume_error() < 0.005, "vol err {}", plan.max_volume_error());
            }
        }
    }
}

#[test]
fn flange_bolt_with_thickness_gets_formed_collar() {
    let i = idx(METRIC, 10.0);
    let mk = |dr: DrawingGeometry| {
        let mut c = input(FT::FLANGE_BOLT, METRIC, PS::ISO_1665, i, 1.5, 60.0, "6g", "10B21", C::ZN_NI, 100);
        c.property_class = Some(PropertyClass::CLASS_8_8);
        c.drawing = dr;
        run(c)
    };
    let r = mk(DrawingGeometry { flange_thickness_mm: Some(2.0), ..Default::default() });
    let plan = r.heading_plan.as_ref().unwrap();
    assert_eq!("Hex & flange form", plan.stations.last().unwrap().title);
    let f = r.bolt_forging.as_ref().unwrap();
    close(f.cut_off_length_mm.unwrap(), plan.slug_length_mm, 0.01);
    assert!(plan.max_volume_error() < 0.005);
    close(0.0, f.cheese_volume_mm3.unwrap(), 1e-9);
    close(0.0, plan.stations.iter().map(|s| s.scrap_mm3).sum(), 1e-9);
    assert!(!plan.stations.iter().any(|s| s.title.contains("Trim")));
    close(f.finished_volume_mm3.unwrap(), plan.slug_volume_mm3, 1e-9);
    let r2 = mk(DrawingGeometry::default());
    assert!(r2.bolt_forging.as_ref().unwrap().head_note.contains("c 1.5"));
    assert_eq!("Hex & flange form", r2.heading_plan.as_ref().unwrap().stations.last().unwrap().title);
}
