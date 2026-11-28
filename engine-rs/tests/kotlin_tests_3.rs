//! Ported from app/src/test: HeadingProgressionTest, QualityDocsTest, ExtrusionTypeTest (+ ExtrusionDieTest),
//! ScreenAuditTest, CatalogueFilterTest, Inconel718CycleTest. UI helpers (maxCatalogueHrc), the custom-material
//! store and the Kotlin catalogue builder are not in the engine and are left out.

#[path = "support/common.rs"]
mod common;
use aerofast_engine::calculation::FastenerCalculationResult;
use aerofast_engine::catalog::MaterialInfo;
use aerofast_engine::dimensions::{self, HeadDimensions};
use aerofast_engine::poka_yoke::CheckInput;
use aerofast_engine::tooling::{self, extrusion_die, MachineSetup, ToolItem};
use aerofast_engine::*;
use common::*;
use std::f64::consts::PI;
use CoatingType as C;
use FastenerType as FT;
use ProductStandard as PS;
use UnitSystem::METRIC;

fn ci(s: &str, x: &str) -> bool {
    s.to_lowercase().contains(&x.to_lowercase())
}

// ---------------- HeadingProgressionTest
fn hp_calc(t: FT, std: PS, cls: &str, len: f64) -> FastenerCalculationResult {
    calc(t, METRIC, std, m_idx(10.0), 1.5, len, cls, "10B21", C::GEOMET, 1000, Some(PropertyClass::CLASS_10_9))
}

#[test]
fn every_station_conserves_slug_volume() {
    for (t, std, cls) in [
        (FT::HEX_BOLT, PS::ISO_4014, "6g"),
        (FT::HEX_BOLT_FULL, PS::ISO_4017, "6g"),
        (FT::SHCS, PS::ISO_4762, "6g"),
        (FT::SOCKET_SET, PS::ISO_4026, "6g"),
        (FT::FLANGE_BOLT, PS::ISO_1665, "6g"),
        (FT::STUD, PS::ASTM_A193, "6g"),
        (FT::HEX_NUT, PS::ISO_4032, "6H"),
    ] {
        for len in [12.0, 60.0, 200.0] {
            let plan = hp_calc(t, std, cls, len).heading_plan.unwrap_or_else(|| panic!("{t:?}"));
            assert!(plan.max_volume_error() < 1e-9, "{t:?} L={len} err {}", plan.max_volume_error());
        }
    }
}

#[test]
fn hex_bolt_progression_ends_at_iso_geometry() {
    let r = hp_calc(FT::HEX_BOLT, PS::ISO_4014, "6g", 60.0);
    let plan = r.heading_plan.as_ref().unwrap();
    assert_eq!(2, plan.upset_blows);
    let titles: Vec<&str> = plan.stations.iter().map(|s| s.title.as_str()).collect();
    assert_eq!(vec!["Shear cut-off", "Square & forward extrude", "Cone pre-upset", "Head upset (round, for trimming)", "Trim to hex"], titles);
    let last = plan.stations.last().unwrap();
    assert_eq!(SectionShape::HEX, last.segments.last().unwrap().shape);
    close(16.0, last.segments.last().unwrap().d_bottom_mm, 1e-9);
    close(66.4, last.height_mm(), 1e-9);
    close(r.cut_off_length_mm, plan.slug_length_mm, 0.05);
    assert!(r.recommended_machine.starts_with("4-die progressive former"), "{}", r.recommended_machine);
    assert!(r.forging_stages.last().unwrap().title.contains("Thread rolling"));
}

#[test]
fn socket_head_needs_cone_and_has_iso_socket() {
    let plan = hp_calc(FT::SHCS, PS::ISO_4762, "6g", 60.0).heading_plan.unwrap();
    assert_eq!(2, plan.upset_blows);
    assert!(plan.stations.iter().any(|s| s.title.starts_with("Cone")));
    let rc = &plan.stations.last().unwrap().recesses;
    assert_eq!(1, rc.len());
    close(8.0, rc[0].size_mm, 1e-9);
    close(5.0, rc[0].depth_mm, 1e-9);
    assert!(plan.stations.iter().all(|s| s.max_width_mm() <= 16.0 + 1e-9));
}

#[test]
fn nut_is_pierced_last_and_slug_includes_web() {
    let r = hp_calc(FT::HEX_NUT, PS::ISO_4032, "6H", 60.0);
    let plan = r.heading_plan.as_ref().unwrap();
    let titles: Vec<&str> = plan.stations.iter().map(|s| s.title.as_str()).collect();
    assert_eq!(vec!["Shear cut-off", "Square & pre-upset", "Pre-form & indent", "Hex form", "Pierce"], titles);
    let p = plan.stations.last().unwrap();
    assert!(p.through_hole_mm.is_some());
    assert!(p.scrap_mm3 > 0.0);
    close(8.4, p.height_mm(), 1e-9);
    close(plan.slug_volume_mm3 / (PI / 4.0 * r.wire_dia_mm * r.wire_dia_mm), r.cut_off_length_mm, 0.05);
    assert!(r.forging_stages.last().unwrap().title.contains("Tapping"));
}

#[test]
fn washer_has_no_heading_progression() {
    let r = hp_calc(FT::FLAT_WASHER, PS::ISO_7089, "", 60.0);
    assert!(r.heading_plan.is_none());
    assert!(r.recommended_machine.contains("press"));
}

// ---------------- QualityDocsTest
fn qd(t: FT, std: PS, len: f64, coat: C, pc: Option<PropertyClass>, m: &str) -> FastenerCalculationResult {
    calc(t, METRIC, std, m_idx(10.0), 1.5, len, if t.is_nut() { "6H" } else { "6g" }, m, coat, 1000, pc)
}
fn qd0() -> FastenerCalculationResult {
    qd(FT::HEX_BOLT, PS::ISO_4014, 60.0, C::ZINC_BLUE, Some(PropertyClass::CLASS_10_9), "10B21")
}

#[test]
fn hex_bolt_tolerances_follow_iso4759() {
    let tol = quality::hex_bolt_tolerances(&qd0());
    let get = |n: &str| tol.iter().find(|t| t.0 == n).unwrap().1.clone();
    assert!(get("Across flats s").starts_with("16.00 −0.27 / 0 (h13)"));
    assert!(get("Head height k").starts_with("6.40 ±0.18 (js14, grade A)"));
    assert!(get("Length l").starts_with("60.0 ±0.60 (js15, grade A)"));
    let long = qd(FT::HEX_BOLT, PS::ISO_4014, 160.0, C::ZINC_BLUE, Some(PropertyClass::CLASS_10_9), "10B21");
    assert!(quality::hex_bolt_tolerances(&long).iter().any(|t| t.1.contains("js15, grade B")));
    let s = qd(FT::SHCS, PS::ISO_4762, 60.0, C::ZINC_BLUE, Some(PropertyClass::CLASS_10_9), "10B21");
    assert!(quality::hex_bolt_tolerances(&s).is_empty());
}

#[test]
fn control_plan_covers_thread_hardness_proof_coating_and_bake() {
    let r = qd0();
    let rows = quality::control_plan(&r);
    let names: Vec<&str> = rows.iter().map(|x| x.characteristic.as_str()).collect();
    assert!(names.iter().any(|n| n.starts_with("Pitch diameter")));
    for n in ["Core hardness", "Proof load", "Coating thickness", "Hydrogen de-embrittlement bake"] {
        assert!(names.contains(&n), "{n}");
    }
    let proof = rows.iter().find(|x| x.characteristic == "Proof load").unwrap();
    assert!(proof.spec.starts_with(&format!("{} kN", jfmt::f(r.proof_load_kn, 1))));
    assert!(proof.special);
    assert!(rows.iter().find(|x| x.characteristic.starts_with("Pitch diameter")).unwrap().method.contains("ISO 1502"));
}

#[test]
fn control_plan_skips_what_does_not_apply() {
    let plain = quality::control_plan(&qd(FT::HEX_BOLT, PS::ISO_4014, 60.0, C::BLACK_OXIDE, Some(PropertyClass::CLASS_4_8), "1008"));
    assert!(!plain.iter().any(|x| x.characteristic.starts_with("Hydrogen")));
    let nut = quality::control_plan(&qd(FT::HEX_NUT, PS::ISO_4032, 60.0, C::ZINC_BLUE, Some(PropertyClass::CLASS_10_9), "10B21"));
    assert!(nut.iter().find(|x| x.characteristic.starts_with("Pitch diameter")).unwrap().method.contains("plug gauge"));
    assert!(!nut.iter().any(|x| x.characteristic.starts_with("Minimum tensile")));
}

#[test]
fn ppap_level_codes_match_aiag_table() {
    let e = quality::ppap_elements(&qd0());
    assert_eq!(19, e.len());
    let lv = |n: i32| e.iter().find(|x| x.no == n).unwrap().levels.clone();
    assert_eq!("S S S S R", lv(18));
    assert_eq!("S S S * R", lv(13));
    assert_eq!("R R R * R", lv(15));
    let l1: Vec<i32> = e.iter().filter(|x| x.levels.split(' ').next() == Some("S")).map(|x| x.no).collect();
    assert_eq!(vec![13, 18], l1);
    let studies = &e.iter().find(|x| x.no == 11).unwrap().for_this_part;
    assert!(studies.contains("head height") && studies.contains("core hardness"));
    assert!(!(studies.contains("proof load") || studies.contains("embrittlement")));
    let psw = quality::psw(&qd0(), "AB-123", "C", "OEM", 3, quality::PSW_REASONS[0], "Spec X\n\nSpec Y");
    assert!(psw.contains("Submission level : 3"));
    assert!(psw.contains(" - Spec X\n - Spec Y"));
}

#[test]
fn ecn_lists_selected_triggers() {
    let t = quality::ecn(&qd0(), "ECN-7", "B", "C", "New plater", "Capacity", &[4], "");
    assert!(t.contains("CUSTOMER NOTIFICATION REQUIRED"));
    assert!(t.contains("heat treatment, plating"));
    assert!(quality::ecn(&qd0(), "", "", "", "", "", &[], "").contains("No PPAP customer-notification case"));
}

#[test]
fn coating_methods_match_the_coating() {
    assert!(quality::coating_thickness_methods(C::ZN_NI)[0].1.contains("ISO 3497"));
    assert!(quality::coating_thickness_methods(C::PHOSPHATE)[0].1.contains("ISO 3892"));
    assert!(quality::coating_thickness_methods(C::PASSIVATION)[0].1.contains("ASTM A967"));
}

// ---------------- ExtrusionTypeTest / ExtrusionDieTest
fn ex_shcs(m: &str, wire: Option<f64>, limit: f64) -> FastenerCalculationResult {
    let mut c = input(FT::SHCS, METRIC, PS::ISO_4762, idx(METRIC, 10.0), 1.5, 50.0, "6g", m, C::ZN_NI, 10000);
    c.property_class = Some(PropertyClass::CLASS_12_9);
    c.wire_dia_override_mm = wire;
    c.open_extrusion_limit_pct = limit;
    run(c)
}
fn items(r: &FastenerCalculationResult, m: &MachineSetup) -> Vec<ToolItem> {
    tooling::build(r, m).unwrap().stations.into_iter().flat_map(|s| s.items).collect()
}
fn single<'a>(v: &'a [ToolItem], f: impl Fn(&ToolItem) -> bool) -> &'a ToolItem {
    let m: Vec<&ToolItem> = v.iter().filter(|i| f(i)).collect();
    assert_eq!(1, m.len(), "expected exactly one item");
    m[0]
}
fn extrusion_item(r: &FastenerCalculationResult, m: &MachineSetup) -> ToolItem {
    single(&items(r, m), |i| i.name == "Extrusion" || i.name.starts_with("Extrusion:")).clone()
}
const OPEN: f64 = heading::MAX_DIE_EXTRUSION_PCT;

#[test]
fn m10_socket_screw_is_an_open_extrusion() {
    let r = ex_shcs("4140", None, OPEN);
    let ex = tooling::extrusions(r.heading_plan.as_ref().unwrap());
    assert_eq!(1, ex.len());
    assert_eq!(1, ex[0].station_index);
    close((1.0 - (ex[0].to_mm / 9.85).powi(2)) * 100.0, ex[0].reduction_pct, 1e-9);
    assert!((17.0..=18.5).contains(&ex[0].reduction_pct));
    assert_eq!("Extrusion: open", extrusion_item(&r, &MachineSetup::default()).name);
    let sheet = tooling::build(&r, &MachineSetup::default()).unwrap();
    assert_eq!(1, sheet.stations.iter().filter(|st| st.items.iter().any(|i| i.name.starts_with("Extrusion entry"))).count());
}

#[test]
fn a_stricter_shop_limit_turns_it_trapped() {
    let r = ex_shcs("4140", None, 15.0);
    let item = extrusion_item(&r, &MachineSetup { open_extrusion_limit_pct: Some(15.0), ..Default::default() });
    assert_eq!("Extrusion: TRAPPED die required", item.name);
    assert!(item.value.contains("over your 15 % open-extrusion limit"));
    assert!(r.heading_plan.as_ref().unwrap().notes.iter().any(|n| n.contains("over your 15 % open-extrusion limit")));
}

#[test]
fn thick_wire_needs_a_trapped_die_and_is_warned() {
    let r = ex_shcs("4140", Some(13.0), OPEN);
    assert_eq!("Extrusion: TRAPPED die required", extrusion_item(&r, &MachineSetup::default()).name);
    assert!(r.poka_yoke_issues.iter().any(|i| i.title == "Large extrusion from this wire"));
}

#[test]
fn over_seventy_percent_is_critical() {
    let r = ex_shcs("4140", Some(20.0), OPEN);
    let ex = tooling::extrusions(r.heading_plan.as_ref().unwrap());
    assert_eq!(1, ex.len());
    assert!(ex[0].reduction_pct > 70.0);
    let v: Vec<_> = r.poka_yoke_issues.iter().filter(|i| i.title == "Extrusion over the trapped limit").collect();
    assert_eq!(1, v.len());
    assert_eq!(PokaYokeSeverity::CRITICAL, v[0].severity);
    assert!(tooling::build(&r, &MachineSetup::default()).unwrap().problems().iter().any(|p| p.contains("trapped limit")));
}

#[test]
fn cone_length_follows_the_angle() {
    close(3.732, extrusion_die::cone_length_mm(10.0, 8.0, 30.0), 1e-3);
}

#[test]
fn pressure_is_johnson_on_the_material_flow_curve() {
    let m = mat("4140");
    let ln_r = (10.0f64 / 8.0).powi(2).ln();
    let s = m.flow_stress_coeff_k * ln_r.powf(m.strain_hardening_n) / (m.strain_hardening_n + 1.0);
    let (lo, hi) = extrusion_die::pressure_mpa(&m, 10.0, 8.0);
    close(s * (0.8 + 1.2 * ln_r), lo, 1e-6);
    close(s * (0.8 + 1.5 * ln_r), hi, 1e-6);
    assert!(extrusion_die::pressure_mpa(&mat("10B21"), 10.0, 8.0).1 < hi);
}

#[test]
fn trapped_die_gets_container_trap_land_and_published_ranges() {
    let all = items(&ex_shcs("4140", Some(13.0), OPEN), &MachineSetup::default());
    assert!(all.iter().any(|i| i.name == "Container bore (trap)" && i.basis == ToolBasis::CALCULATED));
    assert!(all.iter().any(|i| i.name == "Trap length"));
    assert_eq!(ToolBasis::PUBLISHED, single(&all, |i| i.name == "Die opening angle 2α").basis);
    assert!(single(&all, |i| i.name == "Land length").value.starts_with("1.5 – 4.5 mm"));
    assert_eq!(ToolBasis::ESTIMATE, single(&all, |i| i.name == "Extrusion pressure (estimate)").basis);
}

#[test]
fn shop_settings_replace_the_ranges() {
    let r = ex_shcs("4140", Some(13.0), OPEN);
    let all = items(&r, &MachineSetup { die_angle_deg: Some(30.0), land_length_mm: Some(2.0), ..Default::default() });
    assert!(single(&all, |i| i.name == "Reduction cone length").value.ends_with("at 2α = 30°"));
    assert_eq!("2.0 mm (your setting)", single(&all, |i| i.name == "Land length").value);
    let odd = tooling::build(&r, &MachineSetup { die_angle_deg: Some(100.0), ..Default::default() }).unwrap();
    assert!(odd.problems().iter().any(|p| p.contains("outside the 24° – 70° guideline")));
}

#[test]
fn grade_limit_is_shown_where_one_is_published() {
    let lim = |m: &str| single(&items(&ex_shcs(m, Some(13.0), OPEN), &MachineSetup::default()), |i| i.name.starts_with("Reduction limit")).value.clone();
    assert!(lim("1018").contains("≈ 5.0 for 1018"));
    assert!(lim("4140").starts_with("No grade-specific limit"));
}

// ---------------- ScreenAuditTest
fn sa(m: &str, pc: Option<PropertyClass>) -> FastenerCalculationResult {
    calc(FT::HEX_BOLT, METRIC, PS::ISO_4014, m_idx(10.0), 1.5, 60.0, "6g", m, C::GEOMET, 1000, pc)
}

#[test]
fn fatigue_is_thread_endurance_not_half_uts() {
    let r = sa("10B21", Some(PropertyClass::CLASS_10_9));
    close(0.85 * (150.0 / 10.0 + 45.0), r.fatigue_endurance_limit_se_mpa, 0.2);
    close(51.0 * 58.0 / 1.2 / 1000.0, r.allowable_alternating_force_kn, 0.1);
}

#[test]
fn aluminium_never_gets_steel_quench_cycle() {
    let ht = sa("AL7075", Some(PropertyClass::AERO_AL)).heat_treatment_spec;
    assert!(!ht.austenitizing_or_solution_temp_c.contains("860"));
    assert!(ht.austenitizing_or_solution_temp_c.contains("470"));
}

#[test]
fn tempering_respects_iso898_minimum_and_class_hardness() {
    let h = sa("10B21", Some(PropertyClass::CLASS_10_9)).heat_treatment_spec;
    assert!(h.tempering_or_aging_temp_c.starts_with("425"));
    assert!(h.target_hardness_hrc.starts_with("32–39"));
    assert!(sa("10B21", Some(PropertyClass::CLASS_8_8)).heat_treatment_spec.target_hardness_hrc.starts_with("22–32"));
    assert!(sa("1008", Some(PropertyClass::CLASS_4_8)).heat_treatment_spec.austenitizing_or_solution_temp_c.contains("no hardening"));
    assert!(h.atmosphere.contains("0.20%–0.25%"));
}

#[test]
fn graded_classes_state_hardness_in_hrc_or_hrb() {
    use PropertyClass as PC;
    for (pc, hard) in [
        (PC::CLASS_4_6, "67–95 HRB"),
        (PC::CLASS_5_6, "79–95 HRB"),
        (PC::CLASS_5_8, "82–95 HRB"),
        (PC::CLASS_6_8, "89–99.5 HRB"),
        (PC::CLASS_9_8, "28–37 HRC"),
        (PC::CLASS_12_9, "39–44 HRC"),
        (PC::SAE_GRADE_5, "25–34 HRC"),
        (PC::SAE_GRADE_8, "33–39 HRC"),
        (PC::ASTM_A574, "39–45 HRC"),
    ] {
        let t = sa("10B21", Some(pc)).heat_treatment_spec.target_hardness_hrc;
        assert!(t.starts_with(hard), "{pc:?}: {t}");
    }
}

#[test]
fn non_steel_alloys_get_no_steel_furnace_values() {
    for (m, pc) in [("AL7075", PropertyClass::AERO_AL), ("AL2024", PropertyClass::AERO_AL), ("TI_GR1", PropertyClass::MATERIAL_SPEC), ("BRASS_C360", PropertyClass::MATERIAL_SPEC)] {
        let ht = sa(m, Some(pc)).heat_treatment_spec;
        assert!(!(ht.preheat_temp_c.contains("600") || ht.preheat_temp_c.contains("650")), "{m}");
        assert!(!ci(&ht.atmosphere, "Endothermic"), "{m}");
        assert!(!ci(&ht.quench_details, "Oil"), "{m}");
    }
    let s = sa("10B21", Some(PropertyClass::CLASS_10_9)).heat_treatment_spec;
    assert!(s.preheat_temp_c.starts_with("600"));
    assert!(s.atmosphere.contains("Endothermic"));
}

// ---------------- CatalogueFilterTest
fn all_mats() -> &'static [MaterialInfo] {
    catalog::materials()
}

#[test]
fn every_extended_grade_has_sources_strength_and_density() {
    let est: Vec<&MaterialInfo> = all_mats().iter().filter(|m| m.estimated_from.is_some()).collect();
    assert!(est.len() >= 30, "only {} estimated grades", est.len());
    for m in est {
        assert!(!m.data_sources.is_empty(), "{}", m.key);
        assert!(catalog::material(m.estimated_from.as_ref().unwrap()).is_some(), "{}", m.key);
        assert!(m.post_forging_target_uts_mpa > 0.0 && m.density > 0.5, "{}", m.key);
    }
}

#[test]
fn every_material_filter_group_has_grades() {
    for g in selection_filter::material_groups(all_mats()) {
        let list = selection_filter::materials(all_mats(), g, None, 10.0, false, "");
        assert!(!list.is_empty(), "{g:?}");
        if let Some(f) = g.family() {
            assert!(list.iter().all(|m| poka_yoke::material_family(m) == f), "{g:?}");
        }
    }
}

#[test]
fn material_filter_never_returns_an_empty_list() {
    for pc in PropertyClass::VALUES {
        for g in MaterialGroup::VALUES {
            assert!(!selection_filter::materials(all_mats(), g, Some(pc), 10.0, true, "").is_empty(), "{pc:?} / {g:?}");
        }
    }
}

#[test]
fn class_filter_offers_nut_classes_on_nuts_only() {
    for t in FastenerType::VALUES {
        let classes = selection_filter::property_classes(t, METRIC, ClassGroup::ALL, "");
        if t.is_washer() {
            assert!(classes.is_empty(), "{t:?}");
            continue;
        }
        assert!(!classes.is_empty(), "{t:?}");
        let nut: Vec<_> = classes.iter().filter(|c| poka_yoke::NUT_ONLY_CLASSES.contains(c)).collect();
        assert_eq!(t.is_nut(), !nut.is_empty(), "{t:?}");
    }
}

#[test]
fn every_class_group_chip_has_classes() {
    for t in [FT::HEX_BOLT, FT::HEX_NUT, FT::SHCS] {
        for g in selection_filter::class_groups(t) {
            assert!(!selection_filter::property_classes(t, METRIC, g, "").is_empty(), "{t:?} / {g:?}");
        }
    }
}

#[test]
fn class_filter_agrees_with_the_gate() {
    let i = idx(METRIC, 10.0);
    for pc in PropertyClass::VALUES {
        let t = if poka_yoke::NUT_ONLY_CLASSES.contains(&pc) { FT::HEX_NUT } else { FT::HEX_BOLT };
        let std = poka_yoke::default_standard(t, METRIC);
        let tc = selection_filter::thread_classes(METRIC, t, std)[0];
        for m in selection_filter::materials(all_mats(), MaterialGroup::ALL, Some(pc), 10.0, true, "") {
            let issues = poka_yoke::check(&CheckInput {
                t,
                unit: METRIC,
                standard: std,
                nominal_size_index: i,
                pitch_value: catalog::METRIC_SIZES[i].pitches[0],
                d: 10.0,
                length_mm: 60.0,
                thread_class: tc,
                property_class: Some(pc),
                material: m,
                coating: C::GEOMET,
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
            });
            let bad: Vec<_> = issues
                .iter()
                .filter(|x| x.severity == PokaYokeSeverity::CRITICAL)
                .filter(|x| x.title.contains("needs") || x.title.contains("cannot reach") || x.title.contains("not an austenitic"))
                .map(|x| x.title.clone())
                .collect();
            assert!(bad.is_empty(), "{pc:?} + {}: {bad:?}", m.key);
        }
    }
}

#[test]
fn every_type_has_a_standard_and_a_head_in_both_units() {
    for t in FastenerType::VALUES {
        for unit in UnitSystem::VALUES {
            let _std = poka_yoke::default_standard(t, unit);
            let h = dimensions::for_part(t, unit, 10.0);
            assert!(!h.source.trim().is_empty(), "{t:?} {unit:?}");
            assert!(h.head_height_k >= 0.0 && h.across_flats_s > 0.0, "{t:?} {unit:?}");
        }
    }
}

#[test]
fn hex_cheese_diameter_is_one_point_two_across_flats() {
    let head = HeadDimensions { across_flats_s: 16.0, head_height_k: 6.4, bearing_dia_dw: 14.63, nut_height_m: 8.4, source: "ISO 4014".into(), is_table: true, neck_height_mm: None };
    let f = bolt_forging::compute(FT::HEX_BOLT, METRIC, 10.0, 1.5, 60.0, 8.962, 9.85, Some(&head), 7.85, None, None, None, None, None, None);
    let forged = PI / 4.0 * (1.2 * 16.0) * (1.2 * 16.0) * 6.4;
    let finished = 3f64.sqrt() / 2.0 * 16.0 * 16.0 * 6.4;
    close(forged - finished, f.cheese_volume_mm3.unwrap(), 0.01);
    assert!(f.head_note.contains("1.20 × s"));
}

// ---------------- Inconel718CycleTest
fn aero(key: &str, pc: PropertyClass) -> FastenerCalculationResult {
    calc(FT::TWELVE_POINT, METRIC, poka_yoke::default_standard(FT::TWELVE_POINT, METRIC), m_idx(6.0), 1.0, 30.0, "6g", key, C::AERO_IVD, 1000, Some(pc))
}

#[test]
fn in718_ageing_is_ams5663_18h_total() {
    let c = heat_treat_cycle::precipitation_harden(&mat("INCONEL718"), 6.0).unwrap();
    let st = |p: &str| c.stages.iter().find(|s| s.name.starts_with(p)).unwrap().clone();
    assert!(st("4.").name.contains("718 °C"));
    close(97.0 / 55.0, st("5.").hours_low, 1e-9);
    close(18.0, 8.0 + st("5.").hours_low + st("6.").hours_low, 1e-9);
    assert!(st("3.").detail.contains("Water"));
    assert!(heat_treat_cycle::precipitation_harden(&mat("10B21"), 6.0).is_none());
}

#[test]
fn in718_anneal_cycle_shown_with_rapid_cool() {
    let a = heat_treat_cycle::soft_anneal(&mat("INCONEL718"), 6.0).unwrap();
    let cool = &a.stages.last().unwrap().detail;
    assert!(ci(cool, "water") && cool.contains("Never furnace cool"), "{cool}");
    assert!(a.stages[0].detail.contains("927 – 1010 °C"));
}

#[test]
fn in718_spec_cites_the_right_standards_and_no_preheat() {
    let ht = aero("INCONEL718", PropertyClass::AERO_1380).heat_treatment_spec;
    assert!(ht.standard_reference.contains("AMS 5663") && ht.standard_reference.contains("AMS 2774"));
    assert!(!(ht.standard_reference.contains("5664") || ht.standard_reference.contains("2759")));
    assert!(ht.preheat_temp_c.trim().is_empty());
    assert!(ht.tempering_or_aging_temp_c.contains("621 °C") && ht.tempering_or_aging_temp_c.contains("18 h"));
    assert!(ht.target_hardness_hrc.starts_with("36 – 43 HRC"));
    assert!(!mat("INCONEL718").heat_treatment_spec.standard_reference.contains("2759"));
}

#[test]
fn in718_forging_advice_and_sequence() {
    let r = aero("INCONEL718", PropertyClass::AERO_1380);
    assert!(!r.forging_recommendations.iter().any(|x| x.contains("650°C–720°C")));
    assert!(!r.forging_risks.iter().any(|x| x.contains("catastrophic")));
    assert!(r.forging_recommendations.iter().any(|x| x.contains("RAPID cool")));
    let seq = production_report::process_sequence(&r);
    assert!(seq.iter().any(|x| x.contains("AFTER heat treatment")));
    assert!(seq.iter().any(|x| x.contains("; age ")));
    assert!(!seq.iter().any(|x| x.contains("decarburisation")));
    assert!(!quality::control_plan(&r).iter().any(|x| x.characteristic.contains("decarburisation")));
}

#[test]
fn a286_cycle() {
    let ht = aero("A286", PropertyClass::MATERIAL_SPEC).heat_treatment_spec;
    assert!(ht.austenitizing_or_solution_temp_c.contains("982 °C") && ht.austenitizing_or_solution_temp_c.contains("899 °C"));
    assert!(ht.tempering_or_aging_temp_c.contains("718 °C") && ht.tempering_or_aging_temp_c.contains("16 h"));
    assert!(ht.preheat_temp_c.trim().is_empty());
    let c = heat_treat_cycle::precipitation_harden(&mat("A286"), 6.0).unwrap();
    assert!(c.stages.iter().any(|s| s.name.contains("718 °C") && s.detail.contains("16 h")));
    assert!(heat_treat_cycle::soft_anneal(&mat("A286"), 6.0).is_some());
}

#[test]
fn waspaloy_cycle() {
    let ht = aero("WASPALOY", PropertyClass::MATERIAL_SPEC).heat_treatment_spec;
    assert!(ht.preheat_temp_c.trim().is_empty());
    assert!(ht.tempering_or_aging_temp_c.contains("845 °C") && ht.tempering_or_aging_temp_c.contains("760 °C"));
    assert_eq!("34 – 44 HRC", ht.target_hardness_hrc);
    assert_eq!("34 – 44 HRC", mat("WASPALOY").heat_treatment_spec.target_hardness_hrc);
    assert_eq!(5, heat_treat_cycle::precipitation_harden(&mat("WASPALOY"), 6.0).unwrap().stages.len());
    assert!(heat_treat_cycle::soft_anneal(&mat("WASPALOY"), 6.0).unwrap().stages.last().unwrap().detail.contains("quench"));
}

#[test]
fn superalloys_explain_no_spheroidize() {
    for k in ["INCONEL718", "A286", "WASPALOY"] {
        let r = aero(k, PropertyClass::MATERIAL_SPEC);
        assert!(r.soft_anneal_spec.unwrap().notes.iter().any(|n| n.contains("No spheroidize anneal")), "{k}");
    }
}
