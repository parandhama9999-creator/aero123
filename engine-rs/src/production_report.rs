//! Port of ProductionReport.kt: production summary, process sequence and the plain-text production report.

use crate::calculation::{self, FastenerCalculationResult};
use crate::drawing_scan::ExtractedFastenerSpec;
use crate::gen_enums::{AnnealRoute, HeaderRoute, MaterialFamily, StainlessType, WireVerdict};
use crate::jfmt::{self, pad_right};
use crate::poka_yoke;
use crate::raw_material::{self, RawMaterialPlan};
use crate::soft_anneal;
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProductionSummary {
    pub batch_qty: i32,
    pub wire_dia_mm: f64,
    pub cut_off_length_mm: f64,
    pub finished_weight_g: f64,
    pub slug_weight_g: f64,
    pub finished_kg_per1000: f64,
    pub batch_finished_kg: f64,
    pub batch_raw_material_kg: f64,
    pub pieces_per_tonne: i32,
    pub plan: Option<RawMaterialPlan>,
}

impl ProductionSummary {
    pub fn scrap_per_piece_g(&self) -> f64 {
        (self.slug_weight_g - self.finished_weight_g).max(0.0)
    }
    pub fn scrap_pct(&self) -> f64 {
        if self.slug_weight_g > 0.0 {
            self.scrap_per_piece_g() / self.slug_weight_g * 100.0
        } else {
            0.0
        }
    }
}

/// `String.format("%,.Nf")`.
fn n(v: f64, dp: usize) -> String {
    jfmt::fg(v, dp)
}
fn n2(v: f64) -> String {
    n(v, 2)
}

pub fn summary(r: &FastenerCalculationResult) -> ProductionSummary {
    let density = r.material.density;
    let gross_vol = if density > 0.0 { r.gross_weight_grams / density * 1000.0 } else { 0.0 };
    let mut wires = vec![r.wire_dia_mm];
    wires.extend(raw_material::suggested_diameters(r.blank_target_dia, r.wire_dia_mm).into_iter().filter(|w| (w - r.wire_dia_mm).abs() > 0.004));
    let plan = raw_material::plan(gross_vol, density, r.batch_qty, &wires, r.head_volume_mm3(), None, Some(r.blank_target_dia));
    ProductionSummary {
        batch_qty: r.batch_qty,
        wire_dia_mm: r.wire_dia_mm,
        cut_off_length_mm: r.cut_off_length_mm,
        finished_weight_g: r.net_weight_grams,
        slug_weight_g: r.gross_weight_grams,
        finished_kg_per1000: r.net_weight_grams,
        batch_finished_kg: r.net_weight_grams * r.batch_qty as f64 / 1000.0,
        batch_raw_material_kg: plan.as_ref().map(|p| p.batch_weight_kg).unwrap_or(r.gross_weight_grams * r.batch_qty as f64 / 1000.0),
        pieces_per_tonne: plan.as_ref().map(|p| p.pieces_per_tonne).unwrap_or(if r.gross_weight_grams > 0.0 { (1_000_000.0 / r.gross_weight_grams) as i32 } else { 0 }),
        plan,
    }
}

/// The operations in the order they must run.
pub fn process_sequence(r: &FastenerCalculationResult) -> Vec<String> {
    let mut out = Vec::new();
    let threaded = !r.fastener_type.is_unthreaded() && !r.fastener_type.is_washer();
    let anneal = r.soft_anneal_spec.as_ref();
    let cert_too_hard = r.wire_check.as_ref().map(|w| w.verdict) == Some(WireVerdict::TOO_HARD);
    let anneal_text = if anneal.map(|a| a.in_house_anneal_needed) == Some(true) || cert_too_hard {
        format!(
            ", {}{}",
            anneal.map(|a| a.route.label()).unwrap_or("soft anneal").to_lowercase(),
            if cert_too_hard { " (mill cert above the headable range: anneal before heading)" } else { "" }
        )
    } else {
        String::new()
    };
    out.push(format!("Wire Ø{} mm {}{}, coat for heading (phosphate + soap / polymer)", n2(r.wire_dia_mm), r.material.key, anneal_text));
    out.push(format!(
        "Cut-off {} mm and head on the {}{}",
        n2(r.cut_off_length_mm),
        r.heading_plan.as_ref().map(|p| p.machine_description()).unwrap_or_else(|| "header".into()),
        if calculation::is_warm_headed(&r.material, r.nominal_dia_mm) { " (warm)" } else { " (cold)" }
    ));
    let superalloy = poka_yoke::material_family(&r.material) == MaterialFamily::NICKEL_SUPERALLOY;
    if threaded {
        out.push(format!(
            "Roll the thread: blank Ø{}–{} mm{}",
            n(r.blank_lower_limit, 3),
            n(r.blank_upper_limit, 3),
            if r.thread_class.contains("UNJ") || r.thread_class.ends_with("-MJ") || superalloy {
                "; check whether the drawing / spec requires rolling AFTER heat treatment (common for UNJ / MJ fatigue-rated parts and nickel-alloy aerospace bolts)"
            } else {
                ", before heat treatment"
            }
        ));
    }
    let ht = &r.heat_treatment_spec;
    if !ht.austenitizing_or_solution_temp_c.starts_with("N/A") {
        out.push(format!(
            "Heat treat: {}, {}; {} {}",
            ht.austenitizing_or_solution_temp_c,
            ht.quench_medium,
            if ht.is_aging { "age" } else { "temper" },
            ht.tempering_or_aging_temp_c
        ));
        let steel = poka_yoke::material_family(&r.material) == MaterialFamily::CARBON_ALLOY_STEEL || poka_yoke::stainless_type(&r.material) == Some(StainlessType::MARTENSITIC);
        out.push(format!("Check hardness {}{} and tensile on the first load", ht.target_hardness_hrc, if steel { ", decarburisation" } else { "" }));
    }
    let bake = r.has_hydrogen_embrittlement_risk;
    let chromated = poka_yoke::CHROMATED_COATINGS.contains(&r.coating);
    if r.coating.is_electroplating() {
        out.push(format!("Plate {}{}", r.coating.label(), if chromated { " (metal layer only, no chromate yet)" } else { "" }));
        if bake {
            out.push(format!("Hydrogen de-embrittlement bake {}, {}, within 4 h of plating", ht.baking_temp_c, ht.baking_soak_time));
        }
        if chromated {
            out.push(format!("Chromate / passivate{}", if bake { " after the bake" } else { "" }));
        }
    } else if r.coating.thickness_max_um() > 0.0 {
        out.push(format!("Coat {}", r.coating.label()));
    }
    if threaded {
        out.push(format!("Gauge the thread GO / NO-GO{} after coating", if r.thread_class.contains("UNJ") { " and the UNJ root radius" } else { "" }));
    }
    if r.thread_lock_patch {
        out.push("Apply the nylon locking patch (last: it cannot see the bake); test prevailing torque".into());
    }
    let recess = r.finished_part.as_ref().and_then(|p| p.recesses.iter().find(|x| x.from_top));
    out.push(format!(
        "Final inspection: dimensions{}{}{}; pack and label",
        if recess.is_some() { ", recess gauge" } else { "" },
        if r.coating.thickness_max_um() > 0.0 { ", coating thickness" } else { "" },
        if bake { ", ASTM F519 / F1940 hydrogen check" } else { "" }
    ));
    out
}

pub fn build(r: &FastenerCalculationResult, spec: Option<&ExtractedFastenerSpec>, timestamp: &str) -> String {
    let s = summary(r);
    let line = "=".repeat(72);
    let rule = "-".repeat(72);
    let mut o = String::new();
    let section = |o: &mut String, title: &str| {
        o.push('\n');
        o.push_str(title);
        o.push('\n');
        o.push_str(&rule);
        o.push('\n');
    };
    let row = |o: &mut String, label: &str, value: &str| {
        o.push(' ');
        o.push_str(&pad_right(label, 24));
        o.push_str(": ");
        o.push_str(value);
        o.push('\n');
    };

    o.push_str(&line);
    o.push('\n');
    o.push_str(" AEROFAST PRO - PRODUCTION REPORT FROM DRAWING\n");
    o.push_str(&line);
    o.push('\n');
    if !timestamp.trim().is_empty() {
        row(&mut o, "Date", timestamp);
    }
    row(&mut o, "Part", r.fastener_type.label());
    row(&mut o, "Thread x length", &format!("{} x {} mm", r.thread_label, n(r.length_mm, 1)));
    row(&mut o, "Governing standard", &format!("{} ({})", r.standard.code(), r.standard.label()));
    row(&mut o, "Material", &format!("{} - {}", r.material.key, r.material.name));
    let strength = r.tensile_range.as_ref().map(|t| t.label()).or_else(|| r.property_class.map(|p| p.label().to_string())).unwrap_or_else(|| "Per material specification".into());
    row(&mut o, "Strength", &strength);
    row(&mut o, "Coating", r.coating.label());
    if r.thread_lock_patch {
        row(&mut o, "Thread locking", "Nylon patch / pellet (drawing)");
    }
    row(&mut o, "Batch quantity", &format!("{} pcs", r.batch_qty));
    if let Some(sp) = spec {
        row(&mut o, "Read by", &sp.model_used);
        if !sp.assumed.is_empty() {
            row(&mut o, "Not on the drawing", &format!("shop default / standard used for: {}", sp.assumed.join("; ")));
        }
    }
    row(
        &mut o,
        "Release status",
        if r.is_release_blocked() { "BLOCKED - fix the CRITICAL items in section 1" } else { "OK to plan - complete the controls in section 1" },
    );

    section(&mut o, "1. REALITY CHECK AGAINST THE STANDARD (poka-yoke)");
    o.push_str(&poka_yoke::report_section(&r.poka_yoke_issues));

    section(&mut o, "2. CUT-OFF LENGTH & WEIGHT");
    row(&mut o, "Wire / shank dia", &format!("{} mm", n2(s.wire_dia_mm)));
    row(&mut o, "CUT-OFF LENGTH", &format!("{} mm", n2(s.cut_off_length_mm)));
    row(&mut o, "Slug weight (gross)", &format!("{} g", n(s.slug_weight_g, 3)));
    row(&mut o, "Finished weight (net)", &format!("{} g", n(s.finished_weight_g, 3)));
    if r.part_volume_mm3() > 0.0 {
        row(
            &mut o,
            "Finished volume",
            &format!("{} mm3 (head {} g + shank {} g)", n(r.part_volume_mm3(), 0), n(r.head_weight_grams(), 2), n(r.shank_weight_grams(), 2)),
        );
    }
    row(&mut o, "Scrap per piece", &format!("{} g ({} %)", n(s.scrap_per_piece_g(), 3), n(s.scrap_pct(), 1)));
    row(&mut o, "Weight per 1000 pcs", &format!("{} kg finished", n(s.finished_kg_per1000, 2)));
    row(&mut o, "Batch finished weight", &format!("{} kg for {} pcs", n(s.batch_finished_kg, 1), s.batch_qty));
    row(&mut o, "Batch raw material", &format!("{} kg ({} pcs per tonne)", n(s.batch_raw_material_kg, 1), s.pieces_per_tonne));

    if let Some(plan) = &s.plan {
        section(&mut o, &format!("3. RAW MATERIAL - WIRE OPTIONS FOR {} PCS", plan.batch_qty));
        o.push_str(" Weight is the same at any wire dia; cut-off and length change.\n");
        let rec = plan.recommended();
        for opt in &plan.options {
            let tag = if (opt.wire_dia_mm - r.wire_dia_mm).abs() < 1e-6 {
                "  <- HEADING PLAN"
            } else if rec == Some(opt) {
                "  <- shortest cut-off"
            } else {
                ""
            };
            o.push_str(&format!(
                " Wire {} mm: cut-off {} mm, {} kg/m, {} m{}\n",
                n2(opt.wire_dia_mm),
                n2(opt.cut_off_length_mm),
                n(opt.kg_per_metre, 3),
                n(opt.batch_length_m, 0),
                tag
            ));
            if let Some(role) = &opt.role {
                o.push_str(&format!("   {}\n", role));
            }
            for c in &opt.cautions {
                o.push_str(&format!("   Caution: {}\n", c));
            }
        }
    }

    if let Some(a) = &r.soft_anneal_spec {
        section(&mut o, "4. WIRE PREPARATION - SOFT ANNEAL");
        row(&mut o, "Route", a.route.label());
        row(&mut o, "Supplied condition", &a.supplied_condition);
        if let Some(c) = &r.mill_cert {
            let parts: Vec<String> = [c.rm_mpa.map(|v| format!("Rm {} MPa", n(v, 0))), c.hrb.map(|v| format!("{} HRB", n(v, 0)))].into_iter().flatten().collect();
            row(&mut o, "Mill cert (wire)", &parts.join(", "));
            if let Some(w) = &r.wire_check {
                row(&mut o, "Cert check", &format!("{} vs {}", w.verdict.name().replace('_', " "), w.reference));
            }
        }
        row(&mut o, "In-house anneal", if a.in_house_anneal_needed { "REQUIRED" } else { "Not required" });
        if !a.temperature_c.trim().is_empty() {
            row(&mut o, "Temperature", &a.temperature_c);
        }
        if !a.soak_time.trim().is_empty() {
            row(&mut o, "Soak", &a.soak_time);
        }
        if !a.cooling_rate.trim().is_empty() {
            row(&mut o, "Cooling", &a.cooling_rate);
        }
        if !a.atmosphere.trim().is_empty() {
            row(&mut o, "Atmosphere", &a.atmosphere);
        }
        if !a.target_uts_mpa.trim().is_empty() {
            let t: Vec<&str> = [a.target_uts_mpa.as_str(), a.target_hardness_hb.as_str()].into_iter().filter(|x| !x.trim().is_empty()).collect();
            row(&mut o, "Target after anneal", &t.join("; "));
        }
        if a.route == AnnealRoute::SPHEROIDIZE {
            if let Some(c) = soft_anneal::spheroidize(&r.material, soft_anneal::DEFAULT_CHARGE_TONNES, r.wire_dia_mm) {
                o.push_str(&format!(" Furnace cycle ({} t charge, wire {} mm):\n", n(c.charge_tonnes, 0), n2(r.wire_dia_mm)));
                for st in &c.stages {
                    o.push_str(&format!("   {} {} {}\n", pad_right(&st.name, 22), pad_right(&st.hours_text(), 12), st.detail));
                }
                o.push_str(&format!("   Total {}\n", c.total_text()));
                for ca in &c.cautions {
                    o.push_str(&format!("   Caution: {}\n", ca));
                }
            }
        }
    }

    section(&mut o, "5. HEADING - CUT-OFF TO FINISHED HEAD");
    row(&mut o, "Machine", &r.recommended_machine);
    row(&mut o, "Forging load", &format!("{} t", n(r.forging_tonnage, 1)));
    row(&mut o, "Upset L/D", &n(r.upset_ld_ratio, 2));
    if let Some(f) = r.bolt_forging.as_ref().filter(|_| r.heading_plan.as_ref().map(|p| p.route) != Some(HeaderRoute::TWO_BLOW)) {
        row(&mut o, "Blank dia (thread)", &format!("{} mm via 15 deg/side cone {} mm", n(f.blank_dia_mm, 3), n2(f.cone_length_mm)));
        row(&mut o, "Thread length b", &format!("{} mm", n(f.thread_length_mm, 1)));
        row(&mut o, "Stock to extrude", &format!("{} mm", n2(f.stock_to_extrude_mm)));
        if let Some(h) = f.head_stock_mm {
            row(&mut o, "Head stock", &format!("{} mm", n2(h)));
        }
        if let Some(c) = f.cheese_volume_mm3 {
            row(&mut o, "Trim cheese", &format!("{} g", n2(c * f.density_g_per_mm3)));
        }
        o.push_str(&format!(" Head: {}\n", f.head_note));
    }
    if let Some(plan) = &r.heading_plan {
        if !plan.route_reason.trim().is_empty() {
            o.push_str(&format!(" Route: {}\n", plan.route_reason));
        }
        o.push_str(&format!(" {}: slug {} x {} mm, {} upset blow(s)\n", plan.route.label(), n2(plan.slug_dia_mm), n2(plan.slug_length_mm), plan.upset_blows));
        for st in &plan.stations {
            o.push_str(&format!("   St {}: {} - {}\n", st.index, st.title, st.operation));
        }
        for op in &plan.off_header_ops {
            o.push_str(&format!("   Off-header: {} - {}\n", op.title, op.operation));
        }
        for note in &plan.notes {
            o.push_str(&format!("   Note: {}\n", note));
        }
    }

    if !r.fastener_type.is_unthreaded() && !r.fastener_type.is_washer() {
        section(&mut o, "6. THREAD ROLLING");
        row(&mut o, "Rolling blank dia", &format!("{} - {} mm (target {})", n(r.blank_lower_limit, 3), n(r.blank_upper_limit, 3), n(r.blank_target_dia, 3)));
        row(&mut o, "Major dia", &format!("{} - {} mm", n(r.major_dia_min, 3), n(r.major_dia_max, 3)));
        row(
            &mut o,
            &format!("Pitch dia ({})", r.thread_class),
            &format!("{} - {} mm (aim {})", n(r.pitch_dia_min, 3), n(r.pitch_dia_max, 3), n(r.pitch_dia_sweet, 3)),
        );
        row(&mut o, "Minor dia", &format!("{} - {} mm", n(r.minor_dia_min, 3), n(r.minor_dia_max, 3)));
        if let Some(rmin) = r.root_radius_min_mm {
            row(&mut o, "Root radius", &format!("{} - {} mm", n(rmin, 3), n(r.root_radius_max_mm.unwrap_or(rmin), 3)));
        }
        if r.pre_plate_pitch_allowance_um > 0.0 {
            row(&mut o, "Pre-plate allowance", &format!("{} um on pitch dia", n(r.pre_plate_pitch_allowance_um, 0)));
        }
    }

    let ht = &r.heat_treatment_spec;
    section(&mut o, "7. HEAT TREATMENT");
    if !ht.atmosphere.trim().is_empty() {
        row(&mut o, "Atmosphere", &ht.atmosphere);
    }
    row(&mut o, "Harden / solution", &format!("{}, soak {}", ht.austenitizing_or_solution_temp_c, ht.soaking_time_minutes));
    row(&mut o, "Quench", &ht.quench_medium);
    row(&mut o, "Temper / age", &format!("{}, {}", ht.tempering_or_aging_temp_c, ht.tempering_or_aging_time_hours));
    row(&mut o, "Hardness", &ht.target_hardness_hrc);
    row(&mut o, "Standard", &ht.standard_reference);

    section(&mut o, "8. COATING & HYDROGEN EMBRITTLEMENT");
    row(&mut o, "Coating", r.coating.label());
    row(&mut o, "Specification", &r.coating_spec_text);
    row(
        &mut o,
        "De-embrittlement bake",
        &if r.has_hydrogen_embrittlement_risk {
            format!("REQUIRED: {}, {}, start within 4 h of plating", ht.baking_temp_c, ht.baking_soak_time)
        } else {
            "Not required".into()
        },
    );

    section(&mut o, "9. MECHANICAL PROPERTIES REQUIRED");
    match &r.tensile_range {
        Some(t) => row(&mut o, "Tensile Rm (drawing)", &format!("{} - {} MPa", n(t.min_mpa, 0), n(t.max_mpa, 0))),
        None => row(&mut o, "Tensile Rm min", &format!("{} MPa", n(r.post_forging_target_uts_mpa, 0))),
    }
    row(&mut o, "Yield min", &format!("{} MPa", n(r.yield_strength_mpa, 0)));
    row(&mut o, "Elongation min", &format!("{} %", n(r.min_elongation_pct, 0)));
    if r.stress_area_as_mm2 > 0.0 {
        row(&mut o, "Stress area As", &format!("{} mm2", n(r.stress_area_as_mm2, 1)));
    }
    if r.proof_load_kn > 0.0 {
        row(&mut o, "Proof load", &format!("{} kN", n(r.proof_load_kn, 1)));
    }
    if r.tensile_breaking_load_kn > 0.0 {
        let max_load = r.tensile_range.as_ref().map(|t| r.stress_area_as_mm2 * t.max_mpa / 1000.0);
        match max_load {
            Some(m) => row(&mut o, "Breaking load", &format!("{} - {} kN", n(r.tensile_breaking_load_kn, 1), n(m, 1))),
            None => row(&mut o, "Min breaking load", &format!("{} kN", n(r.tensile_breaking_load_kn, 1))),
        }
    }

    section(&mut o, "10. PROCESS SEQUENCE");
    for (i, step) in process_sequence(r).iter().enumerate() {
        o.push_str(&format!(" {}. {}\n", (i + 1) * 10, step));
    }

    section(&mut o, "11. RECOMMENDATIONS");
    let mut issues: Vec<_> = r.poka_yoke_issues.iter().collect();
    issues.sort_by_key(|i| i.severity.ordinal());
    let mut recs: Vec<String> = issues.iter().map(|i| format!("[{}] {}: {}", i.severity.name(), i.title, i.fix)).collect();
    recs.extend(r.forging_risks.iter().map(|x| format!("Risk: {}", x)));
    if let Some(sp) = spec {
        recs.extend(sp.assumed.iter().map(|a| format!("Confirm with the customer: {}", a)));
    }
    let mut seen = std::collections::HashSet::new();
    recs.retain(|x| seen.insert(x.clone()));
    if recs.is_empty() {
        o.push_str(" No open items.\n");
    }
    for (i, t) in recs.iter().enumerate() {
        o.push_str(&format!(" {}. {}\n", i + 1, t));
    }

    o.push('\n');
    o.push_str(&line);
    o.push('\n');
    o.push_str(" Engineering reference generated by AeroFast Pro. Verify against the customer drawing,\n");
    o.push_str(" the current standard and the qualified process before release.\n");
    o.push_str(&line);
    o.push('\n');
    o
}
