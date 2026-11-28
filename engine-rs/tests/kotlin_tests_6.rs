//! Ported from app/src/test: AnnealTemperatureTest, FastenerEngineeringTest, DrawingVolumeTest, DimensionTableAuditTest
//! (+ StationLabelTest geometry), RawMaterialPlannerTest (+ WireWindowAndStageClearanceTest). Tests that go
//! through the AI JSON parser or a UI label function build the spec directly or check only the engine part.

#[path = "support/common.rs"]
mod common;
use aerofast_engine::calculation::FastenerCalculationResult;
use aerofast_engine::catalog;
use aerofast_engine::dimensions;
use aerofast_engine::drawing_scan::{self, DrawnCutout, DrawnSolid};
use aerofast_engine::part_geometry::{self, DrawingGeometry};
use aerofast_engine::raw_material;
use aerofast_engine::steel::{self, SteelComposition};
use aerofast_engine::*;
use common::*;
use std::f64::consts::PI;
use CoatingType as C;
use FastenerType as FT;
use ProductStandard as PS;
use UnitSystem::{INCH, METRIC};

// ---------------- AnnealTemperatureTest
fn cyc(k: &str) -> Option<soft_anneal::AnnealCycle> {
    soft_anneal::spheroidize(&mat(k), soft_anneal::DEFAULT_CHARGE_TONNES, 0.0)
}
fn top(k: &str) -> f64 {
    range_of(&cyc(k).unwrap().soak_temp_c).1
}
fn comp(c: f64, mn: f64, si: f64, cr: f64, ni: f64) -> SteelComposition {
    SteelComposition { carbon: c, manganese: mn, silicon: si, chromium: cr, nickel: ni, molybdenum: 0.0 }
}

#[test]
fn andrews_is_refused_where_it_would_be_extrapolated() {
    let m = steel::for_material(&mat("MARAGING_250")).unwrap();
    assert!(steel::ac1(&m) < 500.0);
    assert!(!steel::andrews_applies(&m));
    assert!(steel::ac1_for(&mat("MARAGING_250")).is_none());
    assert!(steel::andrews_applies(&steel::for_material(&mat("H11_H13")).unwrap()));
}

#[test]
fn andrews_reproduces_the_textbook_values() {
    close(723.0, steel::ac1(&comp(0.2, 0.0, 0.0, 0.0, 0.0)), 1e-9);
    close(723.0 - 10.7, steel::ac1(&comp(0.2, 1.0, 0.0, 0.0, 0.0)), 1e-9);
    close(723.0 - 16.9, steel::ac1(&comp(0.2, 0.0, 0.0, 0.0, 1.0)), 1e-9);
    close(723.0 + 29.1, steel::ac1(&comp(0.2, 0.0, 1.0, 0.0, 0.0)), 1e-9);
    close(723.0 + 16.9, steel::ac1(&comp(0.2, 0.0, 0.0, 1.0, 0.0)), 1e-9);
}

#[test]
fn nickel_pulls_a1_down_and_silicon_pushes_it_up() {
    let ni = steel::ac1_for(&mat("35NCD16")).unwrap();
    let si = steel::ac1_for(&mat("30KHGSA")).unwrap();
    let plain = steel::ac1_for(&mat("1018")).unwrap();
    assert!(ni < plain - 20.0);
    assert!(si > plain + 25.0);
    close(691.0, ni, 1.5);
    close(759.0, si, 1.5);
}

#[test]
fn the_catalogue_spans_more_than_a_hundred_degrees_of_a1() {
    let all: Vec<f64> = catalog::materials().iter().filter_map(steel::ac1_for).collect();
    assert!(all.len() >= 30);
    let lo = all.iter().copied().fold(f64::MAX, f64::min);
    let hi = all.iter().copied().fold(f64::MIN, f64::max);
    assert!(hi - lo > 100.0);
    close(691.0, lo, 1.5);
    close(835.0, hi, 1.5);
}

#[test]
fn every_soak_band_sits_under_that_grades_own_a1() {
    let mut n = 0;
    for m in catalog::materials() {
        let (Some(c), Some(a)) = (soft_anneal::spheroidize(m, soft_anneal::DEFAULT_CHARGE_TONNES, 0.0), steel::ac1_for(m)) else { continue };
        n += 1;
        assert!(range_of(&c.soak_temp_c).1 < a, "{}", m.key);
    }
    assert!(n >= 30);
}

#[test]
fn the_nickel_steel_no_longer_gets_a_band_above_its_a1() {
    assert!(top("35NCD16") < 691.0);
    assert!(!cyc("35NCD16").unwrap().soak_temp_c.contains("718"));
}

#[test]
fn the_silicon_steels_run_hotter_than_plain_carbon() {
    assert!(top("30KHGSA") > top("1018") + 25.0);
    assert!(top("300M") > top("1018") + 10.0);
    assert!(top("H11_H13") > 800.0);
}

#[test]
fn duplex_is_not_offered_a_sigma_forming_hold() {
    for k in ["DUPLEX_2205", "SUPER_DUPLEX_2507"] {
        assert_eq!(MaterialFamily::STAINLESS, poka_yoke::material_family(&mat(k)));
        assert!(cyc(k).is_none());
    }
}

#[test]
fn nickel_alloys_are_not_offered_a_spheroidize_cycle() {
    for k in ["INCOLOY_825", "A453_660"] {
        assert_eq!(MaterialFamily::NICKEL_SUPERALLOY, poka_yoke::material_family(&mat(k)), "{k}");
        assert!(cyc(k).is_none());
    }
}

#[test]
fn maraging_is_solution_annealed_because_it_has_no_carbon_to_ball() {
    assert!(cyc("MARAGING_250").is_none());
    assert!(!steel::can_spheroidise(&mat("MARAGING_250")));
    assert_eq!(AnnealRoute::SOLUTION_ANNEAL, soft_anneal::build(&mat("MARAGING_250")).route);
}

#[test]
fn ordinary_carbon_steel_keeps_its_cycle() {
    for k in ["1008", "10B21", "4140"] {
        assert!(cyc(k).is_some(), "{k}");
    }
}

#[test]
fn soak_length_follows_carbide_formers_not_the_spelling() {
    let soak = |k: &str| cyc(k).unwrap().stages.into_iter().find(|s| s.name.contains("Soak")).unwrap().hours_high;
    for k in ["300M", "H11_H13", "30KHGSA", "16KHGSN"] {
        close(10.0, soak(k), 1e-9);
    }
    assert!(soak("1008") < soak("10B21"));
    assert!(soak("10B21") < soak("4140"));
}

#[test]
fn the_card_names_the_a1_it_used_and_where_it_came_from() {
    let s = soft_anneal::build(&mat("35NCD16"));
    assert!(s.notes.iter().any(|n| n.contains("A1 = ")));
    assert!(s.notes.iter().any(|n| n.contains("Andrews")));
    assert!(s.notes.iter().any(|n| n.contains("mill certificate")));
}

// ---------------- FastenerEngineeringTest
struct E {
    t: FT,
    unit: UnitSystem,
    std: PS,
    size: usize,
    pitch: f64,
    len: f64,
    tc: &'static str,
    m: &'static str,
    coat: C,
    pc: Option<PropertyClass>,
}
impl Default for E {
    fn default() -> Self {
        E { t: FT::HEX_BOLT, unit: METRIC, std: PS::ISO_4014, size: m_idx(10.0), pitch: 1.5, len: 60.0, tc: "6g", m: "10B21", coat: C::GEOMET, pc: Some(PropertyClass::CLASS_10_9) }
    }
}
impl E {
    fn go(&self) -> FastenerCalculationResult {
        calc(self.t, self.unit, self.std, self.size, self.pitch, self.len, self.tc, self.m, self.coat, 1000, self.pc)
    }
}
fn crit(r: &FastenerCalculationResult) -> Vec<String> {
    r.critical_issues().iter().map(|i| i.title.clone()).collect()
}

#[test]
fn m10_6g_external_limits_match_iso965() {
    let r = E::default().go();
    close(9.968, r.major_dia_max, 0.002);
    close(9.732, r.major_dia_min, 0.005);
    close(8.994, r.pitch_dia_max, 0.002);
    close(8.862, r.pitch_dia_min, 0.005);
    close(8.128, r.minor_dia_max, 0.003);
}

#[test]
fn m10_6h_nut_limits_match_iso965() {
    let r = E { t: FT::HEX_NUT, std: PS::ISO_4032, tc: "6H", ..Default::default() }.go();
    close(9.026, r.pitch_dia_min, 0.002);
    close(9.206, r.pitch_dia_max, 0.006);
    close(8.376, r.minor_dia_min, 0.002);
    close(8.676, r.minor_dia_max, 0.008);
}

#[test]
fn m10_stress_area_and_class_loads_match_iso898() {
    close(58.0, E::default().go().stress_area_as_mm2, 0.1);
    let r = E { pc: Some(PropertyClass::CLASS_8_8), ..Default::default() }.go();
    close(33.7, r.proof_load_kn, 0.15);
    close(46.4, r.tensile_breaking_load_kn, 0.15);
    let r = E::default().go();
    close(48.1, r.proof_load_kn, 0.15);
    close(60.3, r.tensile_breaking_load_kn, 0.15);
    let r = E { m: "4140", pc: Some(PropertyClass::CLASS_12_9), ..Default::default() }.go();
    close(56.3, r.proof_load_kn, 0.15);
    close(70.8, r.tensile_breaking_load_kn, 0.15);
}

#[test]
fn three_eighths_16_2a_matches_asme_b11() {
    let i = catalog::INCH_SIZES.iter().position(|s| (s.d_inch - 0.375).abs() < 1e-6).unwrap();
    let r = E { unit: INCH, std: PS::ASME_B18_2_1, size: i, pitch: 16.0, len: 50.8, tc: "2A", pc: Some(PropertyClass::CLASS_8_8), ..Default::default() }.go();
    close(0.3331 * 25.4, r.pitch_dia_max, 0.004);
    close(0.2970 * 25.4, r.minor_dia_max, 0.004);
    close(0.0775 * 645.16, r.stress_area_as_mm2, 0.3);
}

#[test]
fn thread_length_follows_product_standard() {
    close(26.0, E::default().go().thread_length_mm, 0.01);
    close(60.0, E { t: FT::HEX_BOLT_FULL, std: PS::ISO_4017, ..Default::default() }.go().thread_length_mm, 0.01);
}

#[test]
fn valid_spec_has_no_critical_issue() {
    let r = E::default().go();
    assert!(crit(&r).is_empty(), "{:?}", crit(&r));
    assert!(!r.is_release_blocked());
}

#[test]
fn poka_yoke_blocks_the_known_bad_specs() {
    assert!(E { m: "SS304", coat: C::PASSIVATION, ..Default::default() }.go().is_release_blocked());
    assert!(E { pc: Some(PropertyClass::CLASS_12_9), ..Default::default() }.go().is_release_blocked());
    assert!(crit(&E { m: "4140", pc: Some(PropertyClass::CLASS_12_9), coat: C::ZN_NI, ..Default::default() }.go()).iter().any(|t| t.contains("Electroplating")));
    assert!(crit(&E { coat: C::PASSIVATION, ..Default::default() }.go()).iter().any(|t| t.contains("Passivation")));
    assert!(crit(&E { t: FT::HEX_NUT, std: PS::ISO_4014, tc: "6H", ..Default::default() }.go()).iter().any(|t| t.contains("nut")));
    assert!(crit(&E { pitch: 2.0, ..Default::default() }.go()).iter().any(|t| t.contains("pitch")));
    assert!(crit(&E { t: FT::HEX_NUT, std: PS::ISO_4032, tc: "6g", ..Default::default() }.go()).iter().any(|t| t.contains("tolerance class")));
    assert!(crit(&E { len: 80.0, coat: C::HOT_DIP_GALV, pc: Some(PropertyClass::CLASS_8_8), ..Default::default() }.go()).iter().any(|t| t.contains("HDG")));
}

#[test]
fn electroplated_10_9_requires_bake_but_not_8_8() {
    let r = E { coat: C::ZINC_BLUE, ..Default::default() }.go();
    assert!(r.has_hydrogen_embrittlement_risk);
    assert!(r.poka_yoke_issues.iter().any(|i| i.severity == PokaYokeSeverity::MANDATORY && i.title.contains("bake")));
    assert!(!E { coat: C::ZINC_BLUE, pc: Some(PropertyClass::CLASS_8_8), ..Default::default() }.go().has_hydrogen_embrittlement_risk);
}

#[test]
fn thick_plating_on_6g_requires_undersize_pre_plate() {
    let r = E { coat: C::ZN_NI, ..Default::default() }.go();
    assert!(r.poka_yoke_issues.iter().any(|i| i.title.contains("undersize") || i.title.contains("whole thread tolerance")));
}

// ---------------- DrawingVolumeTest
fn dv(t: FT, unit: UnitSystem, d: f64, len: f64, g: DrawingGeometry) -> FastenerCalculationResult {
    let i = idx(unit, d);
    let std = poka_yoke::default_standard(t, unit);
    let mut c = input(
        t,
        unit,
        std,
        i,
        if unit == METRIC { catalog::METRIC_SIZES[i].pitches[0] } else { catalog::INCH_SIZES[i].tpis[0] as f64 },
        len,
        selection_filter::thread_classes(unit, t, std)[0],
        "10B21",
        C::GEOMET,
        100,
    );
    c.property_class = Some(PropertyClass::CLASS_10_9);
    c.drawing = g;
    run(c)
}
fn dv0(t: FT) -> FastenerCalculationResult {
    dv(t, METRIC, 10.0, 50.0, DrawingGeometry::default())
}

#[test]
fn every_type_breakdown_adds_up_and_progression_conserves_volume() {
    for t in FastenerType::VALUES {
        for unit in UnitSystem::VALUES {
            let sizes: &[f64] = if unit == METRIC { &[5.0, 10.0, 20.0] } else { &[6.35, 12.7] };
            for &d in sizes {
                let r = dv(t, unit, d, 6.0 * d, DrawingGeometry::default());
                let part = r.finished_part.as_ref().unwrap();
                assert!(part.volume_mm3() > 0.0, "{t:?} {unit:?} {d}");
                close(part.volume_mm3(), part.items.iter().map(|i| i.volume_mm3).sum(), 1e-6);
                close(part.volume_mm3() / 1000.0 * 7.85, r.net_weight_grams, 0.011);
                if let Some(f) = &r.bolt_forging {
                    close(f.finished_volume_mm3.unwrap(), part.volume_mm3(), 1e-6);
                }
                if t.is_washer() {
                    assert!(r.heading_plan.is_none());
                } else {
                    let plan = r.heading_plan.as_ref().unwrap();
                    assert!(plan.max_volume_error() < 1e-9, "{t:?} {unit:?} {d}");
                    close(part.height_mm(), plan.stations.last().unwrap().height_mm(), 1e-9);
                }
            }
        }
    }
}

#[test]
fn progression_follows_the_fastener_type() {
    let last = |t: FT| dv0(t).heading_plan.unwrap().stations.last().unwrap().title.clone();
    assert_eq!("Trim to hex", last(FT::HEX_BOLT));
    assert_eq!("Socket back-extrude", last(FT::SHCS));
    assert_eq!("Countersunk head & socket", last(FT::SOCKET_CSK));
    assert_eq!("Button head & socket", last(FT::SOCKET_BUTTON));
    assert_eq!("Mushroom head & square neck", last(FT::CARRIAGE_BOLT));
    assert_eq!("12-point form", last(FT::TWELVE_POINT));
    assert_eq!("T-head form", last(FT::T_BOLT));
    assert_eq!("Head upset (Hi-Lok head)", last(FT::HI_LOK_PIN));
    assert_eq!("Hex & flange form", dv0(FT::FLANGE_NUT).heading_plan.unwrap().stations[3].title);
    assert_eq!("Hex form", dv0(FT::HEX_NUT).heading_plan.unwrap().stations[3].title);
    let off = |t: FT| dv0(t).heading_plan.unwrap().off_header_ops.iter().map(|o| o.title.clone()).collect::<Vec<_>>();
    assert!(off(FT::U_BOLT).contains(&"Bending".to_string()));
    assert!(off(FT::BANJO_BOLT).contains(&"Drilling".to_string()));
    assert!(off(FT::CASTLE_NUT).contains(&"Slot milling".to_string()));
    assert!(off(FT::NYLOC_NUT).contains(&"Insert assembly".to_string()));
    assert!(off(FT::NAS_CLOSE_TOL_BOLT).contains(&"Fillet rolling".to_string()));
    assert_eq!(vec!["Pointing", "Thread rolling"], off(FT::HEX_BOLT));
}

#[test]
fn head_shapes_and_tables_are_type_specific() {
    let csk = dv(FT::SOCKET_CSK, METRIC, 10.0, 40.0, DrawingGeometry::default()).finished_part.unwrap();
    let cone = csk.segments.last().unwrap();
    close(22.4, cone.d_top_mm, 1e-9);
    close(6.2, cone.height_mm, 1e-9);
    close(40.0, csk.height_mm(), 1e-9);
    assert_eq!(1, csk.recesses.len());
    close(6.0, csk.recesses[0].size_mm, 1e-9);
    let dome = dv0(FT::SOCKET_BUTTON).finished_part.unwrap().segments.last().unwrap().clone();
    assert_eq!(SectionShape::DOME, dome.shape);
    close(PI * 5.5 / 6.0 * (3.0 * 8.75 * 8.75 + 5.5 * 5.5), dome.volume_mm3(), 1e-6);
    let car = dv0(FT::CARRIAGE_BOLT).finished_part.unwrap();
    close(10.0 * 10.0 * 4.5, car.segments.iter().find(|s| s.shape == SectionShape::SQUARE).unwrap().volume_mm3(), 1e-6);
    close(24.65, car.segments.last().unwrap().d_bottom_mm, 1e-9);
    assert!(car.not_on_drawing.iter().any(|n| n.contains("square neck height f") && n.contains("estimated")));
    let inch = dv(FT::SHCS, INCH, 9.525, 50.8, DrawingGeometry::default()).finished_part.unwrap();
    close(0.562 * 25.4, inch.segments.last().unwrap().d_bottom_mm, 1e-6);
    close(9.525, inch.segments.last().unwrap().height_mm, 1e-6);
    assert_eq!(1, inch.recesses.len());
    close(0.3125 * 25.4, inch.recesses[0].size_mm, 1e-6);
    close(0.92820 * 16.0 * 16.0 * 6.4, dv0(FT::TWELVE_POINT).finished_part.unwrap().segments.last().unwrap().volume_mm3(), 0.05);
    for t in [FT::TWELVE_POINT, FT::T_BOLT, FT::HEAVY_HEX_BOLT] {
        assert!(!dimensions::for_part(t, METRIC, 10.0).is_table, "{t:?}");
    }
    assert!(dimensions::for_part(FT::FLANGE_NUT, METRIC, 10.0).is_table);
    assert!(dimensions::for_part(FT::SOCKET_CSK, METRIC, 10.0).is_table);
}

#[test]
fn washer_is_a_ring_not_a_bolt() {
    let r = dv0(FT::FLAT_WASHER);
    close(PI / 4.0 * (400.0 - 10.5 * 10.5) * 2.0, r.part_volume_mm3(), 1e-6);
    close(3.57, r.net_weight_grams, 0.02);
    let drawn = dv(FT::FLAT_WASHER, METRIC, 10.0, 50.0, DrawingGeometry { washer_outer_dia_mm: Some(30.0), washer_inner_dia_mm: Some(11.0), washer_thickness_mm: Some(3.0), ..Default::default() })
        .finished_part
        .unwrap();
    close(PI / 4.0 * (900.0 - 121.0) * 3.0, drawn.volume_mm3(), 1e-6);
    assert!(drawn.is_complete());
}

#[test]
fn drawing_dimensions_drive_the_volume_and_are_reported() {
    let std = dv0(FT::SHCS).finished_part.unwrap();
    assert!(!std.is_complete());
    assert!(std.not_on_drawing.iter().any(|n| n.starts_with("head Ø dk 16.00 mm — ISO 4762")), "{:?}", std.not_on_drawing);
    let g = DrawingGeometry { thread_length_mm: Some(30.0), shank_dia_mm: Some(9.9), head_dia_mm: Some(15.0), head_height_mm: Some(9.0), socket_size_mm: Some(8.0), socket_depth_mm: Some(4.5), ..Default::default() };
    let drawn = dv(FT::SHCS, METRIC, 10.0, 50.0, g);
    let part = drawn.finished_part.as_ref().unwrap();
    assert!(part.is_complete(), "{:?}", part.not_on_drawing);
    let b = part.blank_dia_mm;
    let expected = PI / 4.0 * b * b * 30.0 + PI / 4.0 * 9.9 * 9.9 * 20.0 + PI / 4.0 * 15.0 * 15.0 * 9.0 - 3f64.sqrt() / 2.0 * 64.0 * 4.5;
    close(expected, part.volume_mm3(), 1e-6);
    let plan = drawn.heading_plan.as_ref().unwrap();
    close(15.0, plan.stations.last().unwrap().segments.last().unwrap().d_bottom_mm, 1e-9);
    assert!(plan.max_volume_error() < 1e-9);
}

fn solid(f: &str, shape: SolidShape, d1: f64, len: f64, threaded: bool) -> DrawnSolid {
    DrawnSolid { feature: f.into(), shape, d1_mm: d1, d2_mm: 0.0, length_mm: len, threaded }
}
fn bolt_spec_with_solids(solids: Vec<DrawnSolid>, cutouts: Vec<DrawnCutout>) -> drawing_scan::ExtractedFastenerSpec {
    let mut s = spec(FT::HEX_BOLT, METRIC, idx(METRIC, 10.0), 10.0, 1.5, 40.0, PropertyClass::CLASS_10_9, "10B21", "6g", C::GEOMET);
    s.head_across_flats_mm = Some(16.0);
    s.head_height_mm = Some(6.4);
    s.thread_length_mm = Some(20.0);
    s.shank_dia_mm = Some(10.0);
    s.solids = solids;
    s.cutouts = cutouts;
    s
}

#[test]
fn scan_solids_give_the_whole_volume_including_features_the_standard_shape_lacks() {
    let s = bolt_spec_with_solids(
        vec![
            solid("hex head", SolidShape::HEX, 16.0, 6.4, false),
            solid("collar", SolidShape::CYLINDER, 14.0, 3.0, false),
            solid("plain shank", SolidShape::CYLINDER, 10.0, 12.0, false),
            solid("thread", SolidShape::CYLINDER, 10.0, 20.0, true),
            solid("dog point", SolidShape::CYLINDER, 7.0, 5.0, false),
        ],
        vec![DrawnCutout { feature: "cross hole".into(), shape: SolidShape::CYLINDER, d1_mm: 2.0, d2_mm: 0.0, length_mm: 10.0, count: 1, from: CutoutFrom::CROSS, threaded: false }],
    );
    let r = drawing_scan::calculate(&s, HeaderChoice::AUTO, 10000);
    let v = drawing_scan::volume(&s, &r);
    assert_eq!(VolumeBasis::SOLIDS, v.basis);
    let b = r.blank_target_dia;
    let expected = 3f64.sqrt() / 2.0 * 256.0 * 6.4 + PI / 4.0 * 196.0 * 3.0 + PI / 4.0 * 100.0 * 12.0 + PI / 4.0 * b * b * 20.0 + PI / 4.0 * 49.0 * 5.0 - PI / 4.0 * 4.0 * 10.0;
    close(expected, v.total_mm3, 1e-6);
    close(v.total_mm3, v.items.iter().map(|i| i.volume_mm3).sum(), 1e-9);
    close(46.4, v.outline.as_ref().unwrap().height_mm(), 1e-9);
    assert!(v.cut_off_length_mm() > v.total_mm3 / (PI / 4.0 * r.wire_dia_mm * r.wire_dia_mm));
}

#[test]
fn scan_solids_that_do_not_add_up_are_not_used() {
    let s = bolt_spec_with_solids(vec![solid("hex head", SolidShape::HEX, 16.0, 6.4, false), solid("thread", SolidShape::CYLINDER, 10.0, 20.0, true)], vec![]);
    let r = drawing_scan::calculate(&s, HeaderChoice::AUTO, 10000);
    let v = drawing_scan::volume(&s, &r);
    assert_eq!(VolumeBasis::DIMENSIONS, v.basis);
    assert!(v.cautions.iter().any(|c| c.contains("add up to 26.40 mm")), "{:?}", v.cautions);
}

#[test]
fn scan_reports_what_the_drawing_did_not_show() {
    let mut s = spec(FT::SHCS, METRIC, idx(METRIC, 6.0), 6.0, 1.0, 24.0, PropertyClass::CLASS_10_9, "4135", "6g", C::ZN_NI);
    s.assumed = vec!["length 24 mm (4 × d)".into(), "pitch 1.0 mm (coarse)".into(), "material 4135 (shop default)".into()];
    let r = drawing_scan::calculate(&s, HeaderChoice::AUTO, 10000);
    let v = drawing_scan::volume(&s, &r);
    assert!(!v.is_complete());
    assert!(v.not_on_drawing.iter().any(|n| n.starts_with("length")));
    assert!(!v.from_drawing.iter().any(|n| n.starts_with("length L")));
}

// ---------------- DimensionTableAuditTest
const SIZES: [f64; 9] = [3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 16.0, 20.0];
fn tabled(t: FT, unit: UnitSystem, sizes: &[f64]) -> Vec<(f64, dimensions::HeadDimensions)> {
    sizes.iter().map(|&d| (d, dimensions::for_part(t, unit, d))).filter(|x| x.1.is_table).collect()
}

#[test]
fn countersunk_heads_satisfy_the_90_degree_cone() {
    let mut s = SIZES.to_vec();
    s.push(14.0);
    let rows = tabled(FT::SOCKET_CSK, METRIC, &s);
    assert_eq!(10, rows.len());
    for (d, h) in rows {
        close((h.across_flats_s - d) / 2.0, h.head_height_k, 0.01);
        assert!((2.0..=2.3).contains(&(h.across_flats_s / d)), "M{d}");
    }
}

#[test]
fn head_sizes_grow_with_the_diameter() {
    for t in [FT::SHCS, FT::SOCKET_CSK, FT::SOCKET_BUTTON, FT::CARRIAGE_BOLT, FT::HEX_BOLT] {
        let rows = tabled(t, METRIC, &SIZES);
        assert!(rows.len() >= 6, "{t:?}");
        for w in rows.windows(2) {
            assert!(w[1].1.across_flats_s > w[0].1.across_flats_s, "{t:?} M{}", w[1].0);
            assert!(w[1].1.head_height_k > w[0].1.head_height_k, "{t:?} M{}", w[1].0);
        }
    }
    let inch: Vec<f64> = [0.25, 0.3125, 0.375, 0.5, 0.625, 0.75, 1.0].iter().map(|x| x * 25.4).collect();
    let rows = tabled(FT::SHCS, INCH, &inch);
    assert_eq!(7, rows.len());
    assert!(rows.windows(2).all(|w| w[1].1.across_flats_s > w[0].1.across_flats_s));
}

#[test]
fn each_head_type_keeps_its_own_proportions() {
    for (t, dk, k) in [(FT::SHCS, (1.45, 1.90), (0.98, 1.02)), (FT::SOCKET_BUTTON, (1.70, 1.95), (0.52, 0.58)), (FT::CARRIAGE_BOLT, (2.30, 2.80), (0.50, 0.68)), (FT::HEX_BOLT, (1.45, 1.90), (0.60, 0.70))] {
        for (d, h) in tabled(t, METRIC, &SIZES) {
            let a = h.across_flats_s / d;
            let b = h.head_height_k / d;
            assert!(a >= dk.0 && a <= dk.1, "{t:?} M{d} dk/d {a}");
            assert!(b >= k.0 && b <= k.1, "{t:?} M{d} k/d {b}");
        }
    }
    for t in [FT::SHCS, FT::SOCKET_BUTTON] {
        for w in tabled(t, METRIC, &SIZES).windows(2) {
            assert!(w[1].1.across_flats_s / w[1].0 <= w[0].1.across_flats_s / w[0].0 + 1e-9, "{t:?}");
        }
    }
    for inch in [0.25, 0.375, 0.5, 0.75, 1.0] {
        let h = dimensions::for_part(FT::SHCS, INCH, inch * 25.4);
        assert!(h.is_table);
        close(inch * 25.4, h.head_height_k, 1e-6);
        assert!((1.40..=1.60).contains(&(h.across_flats_s / (inch * 25.4))));
    }
}

#[test]
fn every_drive_fits_inside_its_head() {
    for t in [FT::SHCS, FT::SOCKET_CSK, FT::SOCKET_BUTTON] {
        for (d, h) in tabled(t, METRIC, &SIZES) {
            let (key, depth) = dimensions::socket_for(t, METRIC, d).unwrap();
            assert!(key * 2.0 / 3f64.sqrt() < h.across_flats_s, "{t:?} M{d}");
            assert!(depth < h.head_height_k, "{t:?} M{d}");
            assert!(key >= 0.3 * d && key <= 0.9 * d, "{t:?} M{d}");
        }
    }
    for inch in [0.25, 0.375, 0.5, 0.75, 1.0] {
        let d = inch * 25.4;
        let (key, _) = dimensions::socket_for(FT::SHCS, INCH, d).unwrap();
        assert!(key * 2.0 / 3f64.sqrt() < dimensions::for_part(FT::SHCS, INCH, d).across_flats_s);
    }
}

#[test]
fn washer_table_is_a_ring_that_fits_its_bolt() {
    for d in SIZES.iter().copied().chain([24.0, 30.0, 36.0]) {
        let w = dimensions::washer_for(FT::FLAT_WASHER, METRIC, d);
        assert!(w.is_table, "M{d}");
        assert!(w.inner_dia_mm > d && w.inner_dia_mm < d + 1.5, "M{d}");
        assert!((1.8..=2.4).contains(&(w.outer_dia_mm / d)), "M{d}");
        assert!(w.thickness_mm >= 0.08 * d && w.thickness_mm <= 0.30 * d, "M{d}");
    }
}

#[test]
fn computed_weights_match_catalogue_weights() {
    let g = |t: FT, d: f64, len: f64, std: PS, tc: &str| {
        let i = idx(METRIC, d);
        calc(t, METRIC, std, i, catalog::METRIC_SIZES[i].pitches[0], len, tc, "10B21", C::GEOMET, 1, Some(PropertyClass::CLASS_10_9)).net_weight_grams
    };
    close(PI / 4.0 * (400.0 - 10.5 * 10.5) * 2.0 * 7.85 / 1000.0, g(FT::FLAT_WASHER, 10.0, 2.0, PS::ISO_7089, ""), 0.01);
    for (name, published, computed) in [
        ("ISO 4014 M10×60", 44.5, g(FT::HEX_BOLT, 10.0, 60.0, PS::ISO_4014, "6g")),
        ("ISO 4032 M10 nut", 10.9, g(FT::HEX_NUT, 10.0, 10.0, PS::ISO_4032, "6H")),
        ("ISO 4762 M10×40", 34.0, g(FT::SHCS, 10.0, 40.0, PS::ISO_4762, "6g")),
        ("ISO 4762 M6×20", 6.6, g(FT::SHCS, 6.0, 20.0, PS::ISO_4762, "6g")),
        ("ISO 10642 M10×40", 26.5, g(FT::SOCKET_CSK, 10.0, 40.0, PS::ISO_10642, "6g")),
        ("ISO 7089 M10 washer", 3.6, g(FT::FLAT_WASHER, 10.0, 2.0, PS::ISO_7089, "")),
    ] {
        assert!((computed - published).abs() / published <= 0.10, "{name}: catalogue {published} g, computed {computed:.1} g");
    }
}

#[test]
fn unsourced_values_are_labelled() {
    assert!(dimensions::for_part(FT::CARRIAGE_BOLT, METRIC, 10.0).neck_height_mm.is_none());
    let part = calc(FT::CARRIAGE_BOLT, METRIC, PS::DIN_603, idx(METRIC, 10.0), 1.5, 50.0, "6g", "10B21", C::GEOMET, 1, Some(PropertyClass::CLASS_8_8)).finished_part.unwrap();
    assert!(part.not_on_drawing.iter().any(|n| n.starts_with("square neck height f") && n.contains("estimated")));
    close(10.0 * 10.0 * 4.5, part.segments.iter().find(|s| s.shape == SectionShape::SQUARE).unwrap().volume_mm3(), 1e-6);
    let drawn = part_geometry::build(
        FT::CARRIAGE_BOLT,
        METRIC,
        10.0,
        50.0,
        26.0,
        8.96,
        &dimensions::for_part(FT::CARRIAGE_BOLT, METRIC, 10.0),
        None,
        &DrawingGeometry { neck_height_mm: Some(4.0), ..Default::default() },
        None,
        None,
    );
    close(10.0 * 10.0 * 4.0, drawn.segments.iter().find(|s| s.shape == SectionShape::SQUARE).unwrap().volume_mm3(), 1e-6);
}

#[test]
fn tapered_stations_gather_against_the_die_face() {
    let r = calc(FT::SHCS, METRIC, PS::ISO_4762, idx(METRIC, 4.0), 0.7, 25.0, "6g", "10B21", C::GEOMET, 1, Some(PropertyClass::CLASS_12_9));
    let st = r.heading_plan.as_ref().unwrap().stations.iter().find(|s| s.title.starts_with("Cone")).unwrap();
    let cone = st.segments.last().unwrap();
    assert!(cone.d_bottom_mm > cone.d_top_mm);
    close(r.wire_dia_mm, cone.d_top_mm, 1e-9);
}

// ---------------- RawMaterialPlannerTest
const STEEL: f64 = 7.85;
const VOL: f64 = 6200.0;
fn plan(v: f64, n: i32, w: &[f64], head: f64, shank: Option<f64>, blank: Option<f64>) -> raw_material::RawMaterialPlan {
    raw_material::plan(v, STEEL, n, w, head, shank, blank).unwrap()
}
fn opt(p: &raw_material::RawMaterialPlan, d: f64) -> &raw_material::FeedstockOption {
    p.options.iter().find(|o| o.wire_dia_mm == d).unwrap()
}

#[test]
fn kg_per_metre_matches_the_mill_figure() {
    let p = plan(VOL, 1000, &[6.0, 8.0], 0.0, None, None);
    close(0.2219, opt(&p, 6.0).kg_per_metre, 0.0005);
    close(0.3946, opt(&p, 8.0).kg_per_metre, 0.0005);
}

#[test]
fn batch_weight_does_not_depend_on_the_diameter() {
    let p = plan(VOL, 25_000, &[5.5, 6.0, 7.0, 8.0], 0.0, None, None);
    assert!(p.options.iter().all(|o| o.batch_weight_kg == p.options[0].batch_weight_kg));
    close(1216.75, p.batch_weight_kg, 0.1);
}

#[test]
fn length_times_kg_per_metre_comes_back_to_the_batch_weight() {
    let p = plan(VOL, 10_000, &[5.0, 6.5, 9.0], 0.0, None, None);
    for o in &p.options {
        close(p.batch_weight_kg, o.batch_length_m * o.kg_per_metre, 0.5);
    }
}

#[test]
fn cut_off_length_falls_with_the_square_of_diameter() {
    let p = plan(VOL, 100, &[5.0, 10.0], 0.0, None, None);
    let (s, l) = (opt(&p, 10.0).cut_off_length_mm, opt(&p, 5.0).cut_off_length_mm);
    close(4.0, l / s, 0.001);
    close(VOL / (PI / 4.0 * 25.0), l, 0.01);
}

#[test]
fn thin_wire_that_cannot_gather_the_head_is_flagged() {
    let p = plan(VOL, 1000, &[3.0, 10.0], 2500.0, None, None);
    assert!(!opt(&p, 3.0).feasible());
    assert!(opt(&p, 3.0).cautions.iter().any(|c| c.contains("3-blow limit")));
    assert!(opt(&p, 10.0).feasible());
}

#[test]
fn shank_larger_than_the_wire_is_rejected_and_deep_extrusion_flagged() {
    assert!(plan(VOL, 500, &[4.0], 0.0, Some(6.0), None).options[0].cautions.iter().any(|c| c.contains("cannot be formed")));
    let p = plan(VOL, 500, &[10.0], 0.0, Some(7.0), None);
    close(51.0, p.options[0].extrusion_reduction_pct.unwrap(), 0.5);
    assert!(p.options[0].cautions.iter().any(|c| c.contains("30 %")));
}

#[test]
fn recommends_the_shortest_workable_cut_off() {
    let p = plan(VOL, 1000, &[3.0, 6.0, 8.0], 2500.0, None, None);
    close(8.0, p.recommended().unwrap().wire_dia_mm, 0.001);
}

#[test]
fn pieces_per_tonne_and_coil_count() {
    let p = plan(VOL, 25_000, &[6.0], 0.0, None, None);
    close(20_546.0, p.pieces_per_tonne as f64, 50.0);
    assert_eq!(2, raw_material::coils_needed(p.batch_weight_kg, 1000.0));
}

#[test]
fn parses_the_diameter_field_however_it_is_typed() {
    assert_eq!(vec![5.5, 6.0, 6.5], raw_material::parse_diameters("6.5, 5.5 6"));
    assert_eq!(vec![5.5, 6.0], raw_material::parse_diameters("5.5; 6; 5.5; junk"));
    assert!(raw_material::parse_diameters("").is_empty());
}

#[test]
fn refuses_nonsense_input_rather_than_returning_zeroes() {
    assert!(raw_material::plan(0.0, STEEL, 100, &[6.0], 0.0, None, None).is_none());
    assert!(raw_material::plan(VOL, STEEL, 0, &[6.0], 0.0, None, None).is_none());
    assert!(raw_material::plan(VOL, STEEL, 100, &[], 0.0, None, None).is_none());
}

#[test]
fn the_wire_window_runs_from_rolling_blank_to_shank() {
    let w = raw_material::suggested_diameters(5.35, 5.90);
    assert_eq!(4, w.len());
    close(5.35, w[0], 0.001);
    close(5.90, *w.last().unwrap(), 0.001);
    assert!(w.iter().all(|x| (x / 0.05 - (x / 0.05).round()).abs() < 1e-6));
}

#[test]
fn a_diameter_below_the_rolling_blank_still_gives_a_cut_off_but_is_flagged() {
    let p = plan(1400.0, 50_000, &[5.20, 5.35, 5.90], 0.0, None, Some(5.35));
    let thin = opt(&p, 5.20);
    close(1400.0 / (PI / 4.0 * 5.20 * 5.20), thin.cut_off_length_mm, 0.01);
    assert!(thin.cutoffs_ok());
    assert!(thin.cautions.iter().any(|c| c.contains("rolling blank")));
    assert!(opt(&p, 5.35).feasible());
    assert_eq!(Some("thread rolling blank".to_string()), opt(&p, 5.35).role);
}

trait CutOk {
    fn cutoffs_ok(&self) -> bool;
}
impl CutOk for raw_material::FeedstockOption {
    fn cutoffs_ok(&self) -> bool {
        self.cut_off_length_mm > 0.0
    }
}

#[test]
fn the_shortest_cut_off_is_the_thickest_wire() {
    let p = plan(1400.0, 10_000, &[5.35, 5.90], 0.0, None, Some(5.35));
    let (b, s) = (opt(&p, 5.35), opt(&p, 5.90));
    assert!(b.cut_off_length_mm > s.cut_off_length_mm);
    close(b.batch_weight_kg, s.batch_weight_kg, 0.001);
    assert!(b.batch_length_m > s.batch_length_m);
}

#[test]
fn stage_clearance_holds_the_stated_floor_and_ceiling_and_steps() {
    for (d, c) in [(3.0, 0.02), (24.0, 0.09), (1.0, 0.02), (40.0, 0.09), (6.0, 0.03), (12.0, 0.05), (20.0, 0.08)] {
        close(c, heading::stage_clearance(d), 0.0005);
    }
    let mut last = 0.0;
    for d in 3..=24 {
        let c = heading::stage_clearance(d as f64);
        assert!(c >= last);
        assert!((c / 0.01 - (c / 0.01).round()).abs() < 1e-6);
        assert!((0.02 - 1e-9..=0.09 + 1e-9).contains(&c));
        last = c;
    }
}

#[test]
fn the_first_die_bore_is_the_wire_plus_its_clearance() {
    let p = plan(1400.0, 1000, &[6.0, 20.0], 0.0, None, None);
    close(6.03, opt(&p, 6.0).first_die_bore_mm, 0.0005);
    close(20.08, opt(&p, 20.0).first_die_bore_mm, 0.0005);
}
