//! Ported from app/src/test: CycleTimeAuditTest, ExampleUnitTests (engine part), ResearchedMaterialsTest,
//! ProductionReportTest, QualityEngineTest, ToolingDesignTest. AI parsing, localisation and the Kotlin
//! catalogue builder (ResearchedMaterials.extend) are not in the engine and are left out.

#[path = "support/common.rs"]
mod common;
use aerofast_engine::calculation::FastenerCalculationResult;
use aerofast_engine::catalog::MaterialInfo;
use aerofast_engine::drawing_scan::{self, shop_defaults, ExtractedFastenerSpec};
use aerofast_engine::tooling::{self, MachineSetup};
use aerofast_engine::*;
use common::*;
use CoatingType as C;
use FastenerType as FT;
use ProductStandard as PS;
use UnitSystem::METRIC;

fn mats() -> Vec<&'static MaterialInfo> {
    let mut v: Vec<&MaterialInfo> = catalog::materials().iter().collect();
    v.sort_by(|a, b| a.key.cmp(&b.key));
    v
}
fn ci(s: &str, x: &str) -> bool {
    s.to_lowercase().contains(&x.to_lowercase())
}

// ---------------- CycleTimeAuditTest
#[test]
fn every_grade_reaches_a_timed_wire_preparation_cycle_or_says_why_not() {
    let mut uncovered = vec![];
    for m in mats() {
        let sph = soft_anneal::spheroidize(m, soft_anneal::DEFAULT_CHARGE_TONNES, 0.0).is_some();
        let nf = heat_treat_cycle::soft_anneal(m, 10.0).is_some();
        let spec = soft_anneal::build(m);
        let bought = spec.route == AnnealRoute::MILL_ANNEALED || !spec.in_house_anneal_needed;
        if sph || nf || bought {
            continue;
        }
        let explained = spec.notes.iter().any(|n| ci(n, "solution") || ci(n, "anneal")) && !spec.unsourced.is_empty();
        if !explained {
            uncovered.push(m.key.clone());
        }
    }
    assert!(uncovered.is_empty(), "no cycle and no explanation: {uncovered:?}");
}

#[test]
fn every_quench_and_temper_steel_gets_a_timed_cycle() {
    let mut missing = vec![];
    for m in mats() {
        if poka_yoke::material_family(m) != MaterialFamily::CARBON_ALLOY_STEEL {
            continue;
        }
        let a = &m.heat_treatment_spec.austenitizing_or_solution_temp_c;
        if a.trim().is_empty() || ci(a, "N/A") {
            continue;
        }
        match heat_treat_cycle::quench_temper(m, 10.0, None) {
            Some(c) if c.total_low() > 0.0 => {}
            _ => missing.push(m.key.clone()),
        }
    }
    assert!(missing.is_empty(), "{missing:?}");
}

#[test]
fn every_non_ferrous_grade_gets_its_anneal_cycle() {
    let mut missing = vec![];
    for m in mats() {
        let f = poka_yoke::material_family(m);
        if f != MaterialFamily::ALUMINUM && f != MaterialFamily::COPPER_ALLOY {
            continue;
        }
        match heat_treat_cycle::soft_anneal(m, 10.0) {
            Some(c) if !c.stages.is_empty() && c.total_low() > 0.0 => {}
            _ => missing.push(m.key.clone()),
        }
    }
    assert!(missing.is_empty(), "{missing:?}");
}

#[test]
fn no_cycle_anywhere_has_an_untimed_stage() {
    for m in mats() {
        let mut stages = vec![];
        if let Some(c) = soft_anneal::spheroidize(m, soft_anneal::DEFAULT_CHARGE_TONNES, 0.0) {
            stages.extend(c.stages);
        }
        if let Some(c) = heat_treat_cycle::quench_temper(m, 10.0, None) {
            stages.extend(c.stages);
        }
        if let Some(c) = heat_treat_cycle::soft_anneal(m, 10.0) {
            stages.extend(c.stages);
        }
        for st in stages {
            assert!(st.hours_low > 0.0, "{} / {} has no hours", m.key, st.name);
            assert!(st.hours_high >= st.hours_low, "{} / {} reversed", m.key, st.name);
            assert!(!st.detail.trim().is_empty(), "{} / {} no detail", m.key, st.name);
        }
    }
}

// ---------------- ExampleUnitTests (engine part)
#[test]
fn calculation_engine_turning_blank_for_m10() {
    let r = calc(FT::HEX_BOLT, METRIC, PS::ISO_4014, m_idx(10.0), 1.5, 60.0, "6g", "10B21", C::ZN_NI, 10000, Some(PropertyClass::CLASS_10_9));
    close(10.0, r.nominal_dia_mm, 0.001);
    close(1.5, r.pitch_mm, 0.001);
    close(8.936, r.blank_target_dia, 0.01);
    assert!(r.has_hydrogen_embrittlement_risk);
    close(1040.0, r.post_forging_target_uts_mpa, 0.1);
    close(940.0, r.yield_strength_mpa, 0.1);
    assert!(r.heat_treatment_spec.hydrogen_baking_required);
    assert_eq!("32–39 HRC (Class 10.9)", r.heat_treatment_spec.target_hardness_hrc);
}

// ---------------- ResearchedMaterialsTest
const ADDED: [&str; 16] = ["20MNB4", "23MNB4", "32CRB4", "36CRB4", "34CRMO4", "41CR4", "SCM435", "18CRMO4", "1022", "MP159", "INCONEL_X750", "TI_BETA_C", "TI_3AL25V", "AL2117", "AL7050", "AL5056"];

#[test]
fn researched_grades_are_in_the_catalogue_and_base_grades_untouched() {
    for k in ADDED {
        assert!(catalog::material(k).is_some(), "{k}");
    }
    assert!(mat("10B21").estimated_from.is_none());
    assert_eq!(1040.0, mat("10B21").uts);
}

#[test]
fn every_new_grade_names_its_base_grade_and_its_sources() {
    for k in ADDED {
        let g = mat(k);
        let base = g.estimated_from.clone().unwrap_or_else(|| panic!("{k} estimatedFrom"));
        assert!(catalog::material(&base).is_some(), "{k} base {base}");
        assert!(!g.data_sources.is_empty(), "{k} sources");
    }
}

#[test]
fn bolt_steels_carry_iso898_class_minima() {
    for k in ["23MNB4", "32CRB4", "34CRMO4", "41CR4", "SCM435"] {
        assert_eq!(1040.0, mat(k).post_forging_target_uts_mpa, "{k}");
        assert_eq!(940.0, mat(k).yield_strength_mpa, "{k}");
        assert!(mat(k).heat_treatment_spec.hydrogen_baking_required);
    }
    assert_eq!(800.0, mat("20MNB4").post_forging_target_uts_mpa);
    assert_eq!(52.0, mat("20MNB4").reduction_of_area_z_pct);
}

#[test]
fn sourced_heat_treat_cycles_are_the_published_ones() {
    assert!(mat("34CRMO4").heat_treatment_spec.austenitizing_or_solution_temp_c.starts_with("830°C – 870°C"));
    assert!(mat("23MNB4").heat_treatment_spec.austenitizing_or_solution_temp_c.starts_with("880°C"));
    assert!(mat("36CRB4").heat_treatment_spec.austenitizing_or_solution_temp_c.contains("estimated from 32CrB4"));
    assert_eq!("649°C – 677°C (1200 – 1250°F)", mat("MP159").heat_treatment_spec.tempering_or_aging_temp_c);
    assert_eq!(1793.0, mat("MP159").post_forging_target_uts_mpa);
    assert_eq!(2.83, mat("AL7050").density);
}

#[test]
fn families_route_correctly() {
    let f = |k: &str| poka_yoke::material_family(&mat(k));
    assert_eq!(MaterialFamily::NICKEL_SUPERALLOY, f("MP159"));
    assert_eq!(MaterialFamily::NICKEL_SUPERALLOY, f("INCONEL_X750"));
    assert_eq!(MaterialFamily::TITANIUM, f("TI_BETA_C"));
    assert_eq!(MaterialFamily::ALUMINUM, f("AL2117"));
    assert_eq!(MaterialFamily::CARBON_ALLOY_STEEL, f("34CRMO4"));
}

#[test]
fn rivet_alloys_are_driven_as_received_not_annealed() {
    for k in ["AL2117", "AL7050", "AL5056"] {
        let s = soft_anneal::build(&mat(k));
        assert_eq!(AnnealRoute::O_TEMPER, s.route, "{k}");
        assert!(!s.in_house_anneal_needed, "{k}");
    }
}

#[test]
fn yield_never_exceeds_tensile() {
    for k in ADDED {
        assert!(mat(k).yield_strength_mpa <= mat(k).post_forging_target_uts_mpa, "{k}");
    }
}

// ---------------- ProductionReportTest
/// What the AI scanner produces for "M10 × 60, pitch 1.5" with no material, class or coating on the drawing:
/// the shop defaults stand in and are listed as assumed.
fn scanned_m10x60() -> ExtractedFastenerSpec {
    ExtractedFastenerSpec {
        fastener_type: FT::HEX_BOLT,
        unit_system: METRIC,
        nominal_size_index: idx(METRIC, 10.0),
        nominal_dia_mm: 10.0,
        pitch_value: 1.5,
        length_mm: 60.0,
        property_class: shop_defaults::FACTORY_CLASS,
        material_key: shop_defaults::FACTORY_MATERIAL.into(),
        thread_class: "6g".into(),
        coating: shop_defaults::FACTORY_COATING,
        technical_summary: String::new(),
        detected_dimensions: vec![],
        is_ai_vision_extracted: true,
        model_used: "test".into(),
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
        assumed: vec!["material 4135 (shop default)".into(), "property class 10.9 (shop default)".into(), "coating Zn-Ni (shop default)".into()],
        solids: vec![],
        cutouts: vec![],
    }
}

#[test]
fn factory_defaults_are_the_shop_requirement() {
    assert_eq!("4135", drawing_scan::shop_material(shop_defaults::FACTORY_MATERIAL).key);
    assert_eq!("4135", drawing_scan::shop_material("NO_SUCH_GRADE").key);
    assert_eq!(PropertyClass::CLASS_10_9, shop_defaults::FACTORY_CLASS);
    assert_eq!(CoatingType::ZN_NI, shop_defaults::FACTORY_COATING);
    assert_eq!(10000, shop_defaults::FACTORY_BATCH);
}

#[test]
fn edited_batch_default_reaches_the_calculation() {
    let spec = scanned_m10x60();
    assert_eq!(25000, drawing_scan::calculate(&spec, HeaderChoice::AUTO, 25000).batch_qty);
    let mut with_qty = spec.clone();
    with_qty.batch_qty = Some(500);
    assert_eq!(500, drawing_scan::calculate(&with_qty, HeaderChoice::AUTO, 25000).batch_qty);
}

#[test]
fn report_carries_the_calculated_figures() {
    let spec = scanned_m10x60();
    let r = drawing_scan::calculate(&spec, HeaderChoice::AUTO, shop_defaults::FACTORY_BATCH);
    let s = production_report::summary(&r);
    assert_eq!(10000, s.batch_qty);
    close(r.cut_off_length_mm, s.cut_off_length_mm, 1e-9);
    close(r.net_weight_grams * 10000.0 / 1000.0, s.batch_finished_kg, 1e-9);
    assert!(s.slug_weight_g >= s.finished_weight_g);
    assert!(s.batch_raw_material_kg >= s.batch_finished_kg);
    let text = production_report::build(&r, Some(&spec), "2026-11-18 10:00");
    for h in [
        "1. REALITY CHECK AGAINST THE STANDARD",
        "2. CUT-OFF LENGTH & WEIGHT",
        "5. HEADING",
        "6. THREAD ROLLING",
        "7. HEAT TREATMENT",
        "8. COATING & HYDROGEN EMBRITTLEMENT",
        "9. MECHANICAL PROPERTIES REQUIRED",
        "10. PROCESS SEQUENCE",
        "11. RECOMMENDATIONS",
    ] {
        assert!(text.contains(h), "{h}");
    }
    assert!(text.contains(&format!("CUT-OFF LENGTH          : {} mm", jfmt::fg(r.cut_off_length_mm, 2))));
    assert!(text.contains("4135") && text.contains("10000 pcs"));
    assert!(r.has_hydrogen_embrittlement_risk);
    assert!(text.contains("De-embrittlement bake   : REQUIRED"));
    let rec = s.plan.as_ref().unwrap().options.iter().find(|o| (o.wire_dia_mm - r.wire_dia_mm).abs() < 1e-6).unwrap();
    close(r.cut_off_length_mm, rec.cut_off_length_mm, 0.06);
    close(r.heading_plan.as_ref().unwrap().slug_volume_mm3 * r.material.density / 1000.0, s.slug_weight_g, 0.01);
    assert!(text.contains("Confirm with the customer: material"));
}

// ---------------- QualityEngineTest
#[test]
fn torque_tension_matches_vdi2230_table_a1() {
    for (rp, fm, ma) in [(640.0, 29.6, 51.0), (940.0, 43.4, 75.0), (1100.0, 50.8, 87.0)] {
        let t = quality::torque_tension(10.0, 1.5, rp, 0.12, 0.12, 16.0, 11.0, 0.9).unwrap();
        close(fm, t.preload_kn, 0.25);
        close(ma, t.torque_nm, ma * 0.04);
    }
}

#[test]
fn torque_tension_shares_add_up_and_k_is_typical() {
    let t = quality::torque_tension(12.0, 1.75, 940.0, 0.12, 0.12, 16.63, 13.5, 0.9).unwrap();
    close(100.0, t.thread_friction_pct + t.head_friction_pct + t.pitch_pct, 1e-9);
    assert!((0.15..=0.22).contains(&t.nut_factor_k), "K {}", t.nut_factor_k);
    assert!(t.head_friction_pct > t.pitch_pct);
}

#[test]
fn torque_tension_rejects_bad_geometry() {
    assert!(quality::torque_tension(10.0, 1.5, 940.0, 0.12, 0.12, 10.0, 11.0, 0.9).is_none());
    assert!(quality::torque_tension(10.0, 1.5, 940.0, 0.12, 0.12, 16.0, 11.0, 1.2).is_none());
}

#[test]
fn capability_known_data() {
    let v = [9.8, 10.0, 10.2, 10.0, 9.9, 10.1];
    let c = quality::capability(&v, Some(9.4), Some(10.6)).unwrap();
    close(10.0, c.mean, 1e-9);
    close(0.141421, c.sigma_overall, 1e-5);
    close(0.18 / 1.128, c.sigma_within, 1e-9);
    close(1.2 / (6.0 * 0.18 / 1.128), c.cp.unwrap(), 1e-9);
    close(c.cp.unwrap(), c.cpk.unwrap(), 1e-9);
    close(1.2 / (6.0 * 0.141421), c.pp.unwrap(), 1e-4);
    assert_eq!(0, c.out_of_spec_count);
}

#[test]
fn capability_off_centre_and_one_sided() {
    let v = [10.3, 10.4, 10.35, 10.45, 10.4];
    let two = quality::capability(&v, Some(9.4), Some(10.6)).unwrap();
    assert!(two.cpk.unwrap() < two.cp.unwrap());
    let up = quality::capability(&v, None, Some(10.6)).unwrap();
    assert!(up.cp.is_none());
    close(two.cpk.unwrap(), up.cpk.unwrap(), 1e-9);
    assert!(quality::capability(&[1.0], Some(0.0), Some(2.0)).is_none());
}

#[test]
fn normal_and_sigma_level() {
    close(0.5, quality::normal_cdf(0.0), 1e-7);
    close(0.841345, quality::normal_cdf(1.0), 1e-6);
    close(1.959964, quality::normal_inv(0.975).unwrap(), 1e-5);
    close(6.0, quality::sigma_level(3.4).unwrap(), 0.01);
    close(4.0, quality::sigma_level(6210.0).unwrap(), 0.01);
    close(3.0, quality::sigma_level(66807.0).unwrap(), 0.01);
    close(500.0, quality::dpmo(5.0, 1000.0, 10.0).unwrap(), 1e-9);
    assert!(quality::normal_inv(0.0).is_none() && quality::normal_inv(1.0).is_none());
}

#[test]
fn oee_is_product_of_three_factors() {
    let o = quality::oee(480.0, 60.0, 1.0, 20000.0, 19600.0).unwrap();
    close(420.0 / 480.0, o.availability, 1e-9);
    close(20000.0 / 60.0 / 420.0, o.performance, 1e-9);
    close(0.98, o.quality, 1e-9);
    close(o.availability * o.performance * o.quality, o.oee(), 1e-12);
    assert!(quality::oee(480.0, 60.0, 1.0, 100.0, 120.0).is_none());
}

#[test]
fn iso_tables() {
    assert_eq!(Some(11.0), quality::clearance_hole_medium(10.0));
    assert_eq!(Some(13.5), quality::clearance_hole_medium(12.0));
    assert_eq!(Some(0.27), quality::it_grade(13, 16.0));
    assert_eq!(Some(0.36), quality::it_grade(14, 6.4));
    assert_eq!(Some(1.20), quality::it_grade(15, 60.0));
    assert_eq!(vec![9.98, 10.02, -0.5], quality::parse_values("9.98, 10.02\n−0.5 abc"));
}

// ---------------- ToolingDesignTest
fn shcs() -> FastenerCalculationResult {
    calc(FT::SHCS, METRIC, PS::ISO_4762, idx(METRIC, 10.0), 1.5, 50.0, "6g", "4140", C::ZN_NI, 10000, Some(PropertyClass::CLASS_12_9))
}
fn none() -> MachineSetup {
    MachineSetup::default()
}

#[test]
fn every_die_station_gets_bore_and_cavity_from_its_own_geometry() {
    let r = shcs();
    let sheet = tooling::build(&r, &none()).unwrap();
    assert_eq!(r.heading_plan.as_ref().unwrap().die_stations(), sheet.stations.len());
    for st in &sheet.stations {
        let h = |v: &[heading::ProfileSegment]| v.iter().map(|s| s.height_mm).sum::<f64>();
        let vol = |v: &[heading::ProfileSegment]| v.iter().map(|s| s.volume_mm3()).sum::<f64>();
        close(st.station.height_mm(), h(&st.in_die) + h(&st.above_die), 1e-6);
        close(vol(&st.station.segments), vol(&st.in_die) + vol(&st.above_die), 1e-3);
        if !st.in_die.is_empty() {
            let depth: Vec<_> = st.items.iter().filter(|i| i.name == "Die bore depth").collect();
            assert_eq!(1, depth.len());
            assert!(depth[0].value.starts_with(&jfmt::f(st.station.in_die_mm, 1)));
            assert_eq!(ToolBasis::CALCULATED, depth[0].basis);
            let d = st.in_die[0].max_width_mm();
            let bore = jfmt::f(d + heading::stage_clearance(d), 2);
            assert!(st.items.iter().find(|i| i.name.starts_with("Die bore (")).unwrap().value.contains(&format!("Ø{bore}")));
        }
    }
}

#[test]
fn the_socket_is_a_punch_pin_with_its_size_and_depth() {
    let r = shcs();
    let sheet = tooling::build(&r, &none()).unwrap();
    let last = sheet.stations.last().unwrap();
    let rc = last.station.recesses.iter().find(|x| x.from_top).unwrap();
    let pin = last.items.iter().find(|i| i.name == "Punch pin (recess)").unwrap();
    assert!(pin.value.starts_with(&format!("hex {}", jfmt::f(rc.size_mm, 2))), "{}", pin.value);
}

#[test]
fn shrink_fit_and_insert_od_are_left_to_the_toolmaker() {
    let sheet = tooling::build(&shcs(), &none()).unwrap();
    assert!(sheet.not_decided.iter().any(|n| n.contains("no published rule")));
    assert!(!sheet.stations.iter().flat_map(|s| &s.items).any(|i| ci(&i.name, "interference")));
    assert_eq!(ToolBasis::TOOLMAKER, sheet.cut_off.iter().find(|i| i.name.starts_with("Cut-off quill")).unwrap().basis);
}

#[test]
fn machine_sizes_are_checked_only_when_entered() {
    let r = shcs();
    assert!(tooling::build(&r, &none()).unwrap().problems().is_empty());
    let tight = tooling::build(&r, &MachineSetup { die_case_od_mm: Some(30.0), die_case_length_mm: Some(20.0), ko_stroke_mm: Some(10.0), insert_od_mm: Some(40.0), ..none() }).unwrap();
    let p = tight.problems();
    assert!(p.iter().any(|x| x.contains("does not fit the 20.0 mm die case")), "{p:?}");
    assert!(p.iter().any(|x| x.contains("knock-out stroke 10.0 mm")));
    assert!(p.iter().any(|x| x.contains("die case Ø30.00 has no room")));
    let roomy = tooling::build(
        &r,
        &MachineSetup { die_case_od_mm: Some(90.0), die_case_length_mm: Some(120.0), punch_case_od_mm: Some(60.0), punch_case_length_mm: Some(90.0), ko_stroke_mm: Some(80.0), insert_od_mm: Some(40.0), ..none() },
    )
    .unwrap();
    assert!(roomy.problems().is_empty(), "{:?}", roomy.problems());
}

#[test]
fn shared_sheet_carries_every_source() {
    let r = shcs();
    let m = MachineSetup { die_case_od_mm: Some(90.0), ..none() };
    let t = tooling::text(&r, &tooling::build(&r, &m).unwrap(), &m);
    for x in ["[calculated]", "[your machine]", "[catalogue]", "[toolmaker decides]", "NOT SET BY THE APP"] {
        assert!(t.contains(x), "{x}");
    }
}

#[test]
fn gathering_station_names_the_sliding_punch_and_its_travel() {
    let r = shcs();
    let sheet = tooling::build(&r, &none()).unwrap();
    let cones: Vec<_> = sheet.stations.iter().filter(|st| st.items.iter().any(|i| i.name == "Cone punch type")).collect();
    assert_eq!(1, cones.len());
    let cone = cones[0];
    let free = cone.items.iter().find(|i| i.name == "Free stock before this blow").unwrap();
    assert!(free.value.contains("L/D 2.31"), "{}", free.value);
    assert_eq!(ToolBasis::SHOP_PRACTICE, cone.items.iter().find(|i| i.name == "Cone punch type").unwrap().basis);
    assert_eq!(1, cone.above_die.len());
    let cone_h = cone.above_die[0].height_mm;
    let travel = cone.items.iter().find(|i| i.name == "Sleeve travel").unwrap();
    let want = jfmt::f(22.75 - cone_h, 2);
    assert!(travel.value.contains(&want[..want.len() - 1]), "{}", travel.value);
    assert_eq!(ToolBasis::TOOLMAKER, cone.items.iter().find(|i| i.name.starts_with("Sleeve bore")).unwrap().basis);
}
