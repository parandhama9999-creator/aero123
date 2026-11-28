//! Port of FastenerData.kt: material record, size and length tables, thread classes, shop-floor routes.

use crate::gen_enums::UnitSystem;
use crate::jfmt;
use serde::{Deserialize, Serialize};
use std::sync::LazyLock;

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HeatTreatmentSpec {
    pub austenitizing_or_solution_temp_c: String,
    pub soaking_time_minutes: String,
    pub quench_medium: String,
    pub tempering_or_aging_temp_c: String,
    pub tempering_or_aging_time_hours: String,
    pub target_hardness_hrc: String,
    pub hydrogen_baking_required: bool,
    pub hydrogen_baking_cycle: String,
    pub standard_reference: String,
    pub preheat_temp_c: String,
    pub preheat_soak_time: String,
    pub atmosphere: String,
    pub quench_details: String,
    pub cooling_method: String,
    pub target_uts_mpa: String,
    pub baking_temp_c: String,
    pub baking_soak_time: String,
    pub is_double_temper: bool,
    pub is_aging: bool,
}

impl HeatTreatmentSpec {
    /// Kotlin constructor defaults for the optional fields.
    #[allow(clippy::too_many_arguments)]
    pub fn new(
        aust: &str, soak: &str, quench: &str, temper: &str, temper_time: &str, hardness: &str,
        baking_required: bool, baking_cycle: &str, standard: &str,
    ) -> Self {
        HeatTreatmentSpec {
            austenitizing_or_solution_temp_c: aust.into(),
            soaking_time_minutes: soak.into(),
            quench_medium: quench.into(),
            tempering_or_aging_temp_c: temper.into(),
            tempering_or_aging_time_hours: temper_time.into(),
            target_hardness_hrc: hardness.into(),
            hydrogen_baking_required: baking_required,
            hydrogen_baking_cycle: baking_cycle.into(),
            standard_reference: standard.into(),
            preheat_temp_c: String::new(),
            preheat_soak_time: String::new(),
            atmosphere: String::new(),
            quench_details: String::new(),
            cooling_method: String::new(),
            target_uts_mpa: String::new(),
            baking_temp_c: "190°C – 200°C".into(),
            baking_soak_time: "8 to 24 Hours mandatory soak".into(),
            is_double_temper: false,
            is_aging: false,
        }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MaterialInfo {
    pub key: String,
    pub name: String,
    pub density: f64,
    pub uts: f64,
    pub vc: f64,
    pub feed: f64,
    pub doc: f64,
    pub rate_inr: f64,
    pub km: f64,
    pub is_aero: bool,
    pub has_high_hardness: bool,
    pub heading_spm: i32,
    pub insert_spec: String,
    pub heat_treatment: String,
    pub lubrication_coating: String,
    pub die_tooling: String,
    pub raw_material_sourcing_uts_mpa: String,
    pub post_forging_target_uts_mpa: f64,
    pub yield_strength_mpa: f64,
    pub min_elongation_pct: f64,
    pub reduction_of_area_z_pct: f64,
    pub flow_stress_coeff_k: f64,
    pub strain_hardening_n: f64,
    pub heat_treatment_spec: HeatTreatmentSpec,
    pub estimated_from: Option<String>,
    pub data_sources: Vec<String>,
}

pub struct MetricSize {
    pub label: &'static str,
    pub d: f64,
    /// Coarse first, then fine.
    pub pitches: &'static [f64],
}

pub struct InchSize {
    pub label: &'static str,
    pub d_inch: f64,
    /// Coarse (UNC) first, then fine (UNF).
    pub tpis: &'static [i32],
    /// #0 exists only as 0-80 UNF.
    pub has_unc: bool,
}

pub struct ShopFloorProcessRoute {
    pub route_number: i32,
    pub category_name: &'static str,
    pub applicable_grades: &'static str,
    pub metallurgical_behavior: &'static str,
    pub sequence_steps: &'static str,
    pub poka_yoke_quality_gate: &'static str,
}

static DEFAULT_MATERIALS: LazyLock<Vec<MaterialInfo>> = LazyLock::new(crate::gen_materials::default_materials);

/// The material catalogue (base, researched and extended grades) in its Kotlin order.
pub fn materials() -> &'static [MaterialInfo] {
    &DEFAULT_MATERIALS
}

pub fn material(key: &str) -> Option<&'static MaterialInfo> {
    DEFAULT_MATERIALS.iter().find(|m| m.key == key)
}

/// Nominal diameter in mm of a size position.
pub fn diameter_mm(unit: UnitSystem, index: usize) -> f64 {
    match unit {
        UnitSystem::METRIC => METRIC_SIZES.get(index).unwrap_or(&METRIC_SIZES[default_size_index(unit)]).d,
        UnitSystem::INCH => INCH_SIZES.get(index).unwrap_or(&INCH_SIZES[default_size_index(unit)]).d_inch * 25.4,
    }
}

/// Position of the size closest to a diameter in mm.
pub fn size_index_for(unit: UnitSystem, d_mm: f64) -> usize {
    match unit {
        UnitSystem::METRIC => jfmt::min_by(0..METRIC_SIZES.len(), |&i| (METRIC_SIZES[i].d - d_mm).abs()).unwrap_or(0),
        UnitSystem::INCH => jfmt::min_by(0..INCH_SIZES.len(), |&i| (INCH_SIZES[i].d_inch * 25.4 - d_mm).abs()).unwrap_or(0),
    }
}

/// M10 or 3/8".
pub fn default_size_index(unit: UnitSystem) -> usize {
    size_index_for(unit, if unit == UnitSystem::METRIC { 10.0 } else { 9.525 })
}

/// Maps a position saved by an older version (before sizes were inserted) onto today's list.
pub fn migrate_legacy_size_index(unit: UnitSystem, legacy_index: usize) -> usize {
    let d = match unit {
        UnitSystem::METRIC => LEGACY_METRIC_D.get(legacy_index).copied(),
        UnitSystem::INCH => LEGACY_INCH_D.get(legacy_index).map(|x| x * 25.4),
    };
    match d {
        None => default_size_index(unit),
        Some(d) => size_index_for(unit, d),
    }
}

pub mod thread_class_catalog {
    use super::*;

    pub fn get_options(unit: UnitSystem, is_nut: bool) -> &'static [&'static str] {
        match (unit, is_nut) {
            (UnitSystem::METRIC, true) => METRIC_INTERNAL,
            (UnitSystem::METRIC, false) => METRIC_EXTERNAL,
            (UnitSystem::INCH, true) => INCH_INTERNAL,
            (UnitSystem::INCH, false) => INCH_EXTERNAL,
        }
    }

    pub fn get_label(code: &str) -> String {
        let l = match code {
            "6g" => "6g (Standard Medium Fit - ISO 965)",
            "4g6g" => "4g6g (Aerospace / High-Precision Fit)",
            "4h" => "4h (Zero-Allowance Ultra-Close Fit)",
            "6h" => "6h (Zero-Allowance Medium Fit)",
            "6e" => "6e (Large Allowance for Thick Coatings)",
            "6f" => "6f (Moderate Allowance for Electroplating)",
            "8g" => "8g (Loose / Commercial Fit)",
            "4g" => "4g (Close Pitch & Major Fit)",
            "5g6g" => "5g6g (Precision Machine Screw Fit)",
            "6H" => "6H (Standard Internal Nut Fit)",
            "5H" => "5H (Close Internal Fit)",
            "4H" => "4H (Precision Aerospace Internal Fit)",
            "6G" => "6G (Internal Fit with Plating Allowance)",
            "7H" => "7H (Coarse / Loose Internal Fit)",
            "2A" => "2A (Standard Commercial External Fit)",
            "3A" => "3A (Close Zero-Allowance Fit)",
            "3A-UNJ" => "3A-UNJ (Aerospace AS8879 Controlled Root)",
            "1A" => "1A (Loose / Free Field Assembly Fit)",
            "2B" => "2B (Standard Internal Nut Fit)",
            "3B" => "3B (Close Precision Internal Fit)",
            "3B-UNJ" => "3B-UNJ (Aerospace Controlled Root AS8879)",
            "1B" => "1B (Loose Internal Fit)",
            "3h4h" => "3h4h (Finest Zero-Allowance Fit)",
            "4h6h" => "4h6h (Close Pitch, Medium Crest)",
            "5h4h" => "5h4h (Close Zero-Allowance, Fine Crest)",
            "5h6h" => "5h6h (Close Zero-Allowance Fit)",
            "6d" => "6d (Extra-Large Allowance, Heavy Coatings)",
            "7e6e" => "7e6e (Coarse Fit, Large Allowance)",
            "7g6g" => "7g6g (Coarse Fit with Allowance)",
            "7h6h" => "7h6h (Coarse Zero-Allowance Fit)",
            "8h" => "8h (Loose Zero-Allowance Fit)",
            "9g8g" => "9g8g (Very Loose Fit)",
            "4h6h-MJ" => "4h6h MJ (Aerospace ISO 5855, Controlled Root)",
            "4H5H" => "4H5H (Close Pitch, Fine Minor)",
            "5G" => "5G (Close Fit with Plating Allowance)",
            "5H6H" => "5H6H (Close Pitch, Medium Minor)",
            "7G" => "7G (Coarse Fit with Allowance)",
            "8G" => "8G (Loose Fit with Allowance)",
            "8H" => "8H (Loose Internal Fit)",
            "6AZ" => "6AZ (Oversize Nut for Centrifuged Hot-Dip Galv, ISO 965-5)",
            "6AX" => "6AX (Oversize Nut for Heavy Hot-Dip Galv, ISO 965-5)",
            "4H5H-MJ" => "4H5H MJ (Aerospace ISO 5855 Internal)",
            "5H6H-MJ" => "5H6H MJ (Aerospace ISO 5855 Internal)",
            _ => return code.to_string(),
        };
        l.to_string()
    }
}

pub static METRIC_SIZES: &[MetricSize] = &[
    MetricSize { label: "M0.25 (0.25 mm)", d: 0.25, pitches: &[0.075] },
    MetricSize { label: "M0.5 (0.50 mm)", d: 0.5, pitches: &[0.125] },
    MetricSize { label: "M1.0 (1.00 mm)", d: 1.0, pitches: &[0.25, 0.2] },
    MetricSize { label: "M1.2 (1.20 mm)", d: 1.2, pitches: &[0.25, 0.2] },
    MetricSize { label: "M1.4 (1.40 mm)", d: 1.4, pitches: &[0.3, 0.2] },
    MetricSize { label: "M1.6 (1.60 mm)", d: 1.6, pitches: &[0.35, 0.2] },
    MetricSize { label: "M2.0 (2.00 mm)", d: 2.0, pitches: &[0.4, 0.25] },
    MetricSize { label: "M2.5 (2.50 mm)", d: 2.5, pitches: &[0.45, 0.35] },
    MetricSize { label: "M3 (3.00 mm)", d: 3.0, pitches: &[0.5, 0.35] },
    MetricSize { label: "M3.5 (3.50 mm)", d: 3.5, pitches: &[0.6, 0.35] },
    MetricSize { label: "M4 (4.00 mm)", d: 4.0, pitches: &[0.7, 0.5] },
    MetricSize { label: "M5 (5.00 mm)", d: 5.0, pitches: &[0.8, 0.5] },
    MetricSize { label: "M6 (6.00 mm)", d: 6.0, pitches: &[1.0, 0.75, 0.5] },
    MetricSize { label: "M8 (8.00 mm)", d: 8.0, pitches: &[1.25, 1.0, 0.75] },
    MetricSize { label: "M10 (10.00 mm)", d: 10.0, pitches: &[1.5, 1.25, 1.0, 0.75] },
    MetricSize { label: "M12 (12.00 mm)", d: 12.0, pitches: &[1.75, 1.5, 1.25, 1.0] },
    MetricSize { label: "M14 (14.00 mm)", d: 14.0, pitches: &[2.0, 1.5, 1.25, 1.0] },
    MetricSize { label: "M16 (16.00 mm)", d: 16.0, pitches: &[2.0, 1.5, 1.0] },
    MetricSize { label: "M18 (18.00 mm)", d: 18.0, pitches: &[2.5, 2.0, 1.5, 1.0] },
    MetricSize { label: "M20 (20.00 mm)", d: 20.0, pitches: &[2.5, 2.0, 1.5, 1.0] },
    MetricSize { label: "M22 (22.00 mm)", d: 22.0, pitches: &[2.5, 2.0, 1.5, 1.0] },
    MetricSize { label: "M24 (24.00 mm)", d: 24.0, pitches: &[3.0, 2.0, 1.5, 1.0] },
    MetricSize { label: "M27 (27.00 mm)", d: 27.0, pitches: &[3.0, 2.0, 1.5, 1.0] },
    MetricSize { label: "M30 (30.00 mm)", d: 30.0, pitches: &[3.5, 2.0, 1.5, 1.0] },
    MetricSize { label: "M33 (33.00 mm)", d: 33.0, pitches: &[3.5, 2.0, 1.5] },
    MetricSize { label: "M36 (36.00 mm)", d: 36.0, pitches: &[4.0, 3.0, 2.0, 1.5] },
    MetricSize { label: "M39 (39.00 mm)", d: 39.0, pitches: &[4.0, 3.0, 2.0, 1.5] },
    MetricSize { label: "M42 (42.00 mm)", d: 42.0, pitches: &[4.5, 3.0, 2.0, 1.5] },
    MetricSize { label: "M45 (45.00 mm)", d: 45.0, pitches: &[4.5, 3.0, 2.0, 1.5] },
    MetricSize { label: "M48 (48.00 mm)", d: 48.0, pitches: &[5.0, 3.0, 2.0, 1.5] },
    MetricSize { label: "M52 (52.00 mm)", d: 52.0, pitches: &[5.0, 4.0, 3.0, 2.0, 1.5] },
    MetricSize { label: "M56 (56.00 mm)", d: 56.0, pitches: &[5.5, 4.0, 3.0, 2.0, 1.5] },
    MetricSize { label: "M60 (60.00 mm)", d: 60.0, pitches: &[5.5, 4.0, 3.0, 2.0, 1.5] },
    MetricSize { label: "M64 (64.00 mm)", d: 64.0, pitches: &[6.0, 4.0, 3.0, 2.0, 1.5] },
];
pub static INCH_SIZES: &[InchSize] = &[
    InchSize { label: "#0 (0.0600\")", d_inch: 0.06, tpis: &[80], has_unc: false },
    InchSize { label: "#1 (0.0730\")", d_inch: 0.073, tpis: &[64, 72], has_unc: true },
    InchSize { label: "#2 (0.0860\")", d_inch: 0.086, tpis: &[56, 64], has_unc: true },
    InchSize { label: "#3 (0.0990\")", d_inch: 0.099, tpis: &[48, 56], has_unc: true },
    InchSize { label: "#4 (0.1120\")", d_inch: 0.112, tpis: &[40, 48], has_unc: true },
    InchSize { label: "#5 (0.1250\")", d_inch: 0.125, tpis: &[40, 44], has_unc: true },
    InchSize { label: "#6 (0.1380\")", d_inch: 0.138, tpis: &[32, 40], has_unc: true },
    InchSize { label: "#8 (0.1640\")", d_inch: 0.164, tpis: &[32, 36], has_unc: true },
    InchSize { label: "#10 (0.1900\")", d_inch: 0.19, tpis: &[24, 32], has_unc: true },
    InchSize { label: "#12 (0.2160\")", d_inch: 0.216, tpis: &[24, 28], has_unc: true },
    InchSize { label: "1/4\" (0.2500\")", d_inch: 0.25, tpis: &[20, 28], has_unc: true },
    InchSize { label: "5/16\" (0.3125\")", d_inch: 0.3125, tpis: &[18, 24], has_unc: true },
    InchSize { label: "3/8\" (0.3750\")", d_inch: 0.375, tpis: &[16, 24], has_unc: true },
    InchSize { label: "7/16\" (0.4375\")", d_inch: 0.4375, tpis: &[14, 20], has_unc: true },
    InchSize { label: "1/2\" (0.5000\")", d_inch: 0.5, tpis: &[13, 20], has_unc: true },
    InchSize { label: "9/16\" (0.5625\")", d_inch: 0.5625, tpis: &[12, 18], has_unc: true },
    InchSize { label: "5/8\" (0.6250\")", d_inch: 0.625, tpis: &[11, 18], has_unc: true },
    InchSize { label: "3/4\" (0.7500\")", d_inch: 0.75, tpis: &[10, 16], has_unc: true },
    InchSize { label: "7/8\" (0.8750\")", d_inch: 0.875, tpis: &[9, 14], has_unc: true },
    InchSize { label: "1\" (1.0000\")", d_inch: 1.0, tpis: &[8, 12, 14], has_unc: true },
    InchSize { label: "1-1/8\" (1.1250\")", d_inch: 1.125, tpis: &[7, 12], has_unc: true },
    InchSize { label: "1-1/4\" (1.2500\")", d_inch: 1.25, tpis: &[7, 12], has_unc: true },
    InchSize { label: "1-3/8\" (1.3750\")", d_inch: 1.375, tpis: &[6, 12], has_unc: true },
    InchSize { label: "1-1/2\" (1.5000\")", d_inch: 1.5, tpis: &[6, 12], has_unc: true },
    InchSize { label: "1-3/4\" (1.7500\")", d_inch: 1.75, tpis: &[5], has_unc: true },
];
pub static LEGACY_METRIC_D: &[f64] = &[0.25, 0.5, 1.0, 1.6, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 20.0, 24.0, 30.0, 36.0];
pub static LEGACY_INCH_D: &[f64] = &[0.073, 0.086, 0.112, 0.138, 0.164, 0.19, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0];
pub static METRIC_LENGTHS: &[f64] = &[2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 16.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0, 80.0, 90.0, 100.0, 110.0, 120.0, 130.0, 140.0, 150.0, 160.0, 180.0, 200.0, 220.0, 240.0, 260.0, 280.0, 300.0, 320.0, 340.0, 360.0, 380.0, 400.0, 420.0, 440.0, 460.0, 480.0, 500.0];
pub static INCH_LENGTHS: &[f64] = &[3.175, 4.762499999999999, 6.35, 7.9375, 9.524999999999999, 11.112499999999999, 12.7, 15.875, 19.049999999999997, 22.224999999999998, 25.4, 28.575, 31.75, 34.925, 38.099999999999994, 44.449999999999996, 50.8, 57.15, 63.5, 69.85, 76.19999999999999, 82.55, 88.89999999999999, 95.25, 101.6, 114.3, 127.0, 139.7, 152.39999999999998, 165.1, 177.79999999999998, 190.5, 203.2];
pub static METRIC_EXTERNAL: &[&str] = &["6g", "3h4h", "4g", "4h", "4g6g", "4h6h", "5g6g", "5h4h", "5h6h", "6d", "6e", "6f", "6h", "7e6e", "7g6g", "7h6h", "8g", "8h", "9g8g", "4h6h-MJ"];
pub static METRIC_INTERNAL: &[&str] = &["6H", "4H", "4H5H", "5G", "5H", "5H6H", "6G", "7G", "7H", "8G", "8H", "6AZ", "6AX", "4H5H-MJ", "5H6H-MJ"];
pub static INCH_EXTERNAL: &[&str] = &["2A", "3A", "3A-UNJ", "1A"];
pub static INCH_INTERNAL: &[&str] = &["2B", "3B", "3B-UNJ", "1B"];
pub static PROCESS_ROUTES: &[ShopFloorProcessRoute] = &[
    ShopFloorProcessRoute { route_number: 1, category_name: "Route 1: Medium-Carbon & Boron Alloy Steels", applicable_grades: "EN24, EN24 ESR, AISI 4135, 4140, 4340, 8740, 30KhGSA, 16KhGSN, 35NCD16, 51B37, 10B21/15B25/10B35, 4130", metallurgical_behavior: "Moderate work-hardening; high formability in spheroidized annealed state (Z >= 50%).", sequence_steps: "1. Spheroidized Annealed Wire + Zn-Phosphate & Soap Lube -> 2. Cold/Warm Multi-Station Heading (Forward Extrude shank + Upset Head) -> 3. Thread Roll in Green/Soft State -> 4. Wash -> 5. Austenitize & Oil Quench + Temper -> 6. Plating + De-embrittlement Baking.", poka_yoke_quality_gate: "Bake at 190–220°C within 1–4 hrs post-plating if UTS >= 1000 MPa (>31 HRC) per ISO 4042 / ASTM F1940." },
    ShopFloorProcessRoute { route_number: 2, category_name: "Route 2: Austenitic Fe/Ni Superalloys", applicable_grades: "A286 (AMS 5731), Inconel 718 (AMS 5662), 10Kh11N23T3MR (EP33)", metallurgical_behavior: "Severe rapid strain-hardening; high galling risk; micro-cracking if cold headed heavily at room temp.", sequence_steps: "1. Solution Annealed Wire + Oxalate / Glass-MoS2 Dual Lube -> 2. Controlled Warm Heading (Induction 650–720°C) with Carbide Dies -> 3. Solution Heat Treat (955–1100°C) -> 4. Precipitation Aging (e.g. 720°C 16h) -> 5. Post-Aging Thread Rolling -> 6. Nitric Passivation + FPI.", poka_yoke_quality_gate: "Roll threads AFTER aging for aerospace fatigue life (NASM/AMS). Strictly monitor warm heading temp." },
    ShopFloorProcessRoute { route_number: 3, category_name: "Route 3: Aerospace Titanium Alloys", applicable_grades: "Ti-6Al-4V (Grade 5), VT-16 (GOST ВТ16), Titanium Grade 1", metallurgical_behavior: "Gr 5 requires warm forming due to HCP crystal structure & springback; VT-16 & Gr 1 allow cold heading.", sequence_steps: "1. Annealed Wire + Fluoride-Phosphate + MoS2 Lube -> 2. Warm Heading (650–700°C for Ti6Al4V; Cold/Warm for VT-16) -> 3. Solution Treat + Vacuum Age (STA) -> 4. Slow-Speed Thread Rolling with EP Oil -> 5. Chemical Pickling (remove alpha-case) + FPI.", poka_yoke_quality_gate: "Never exceed beta-transus or 750°C in open air to prevent brittle oxygen alpha-case layer." },
    ShopFloorProcessRoute { route_number: 4, category_name: "Route 4: Martensitic & PH Stainless Steels", applicable_grades: "15-5PH (AMS 5659), 17-4PH (AMS 5643), 14Kh17N2, 20X13, 30X13", metallurgical_behavior: "High initial yield strength in Condition A (for PH grades); magnetic & heat-treatable via Q&T or Aging.", sequence_steps: "1. Condition A / Annealed Wire + Oxalate & Soap -> 2. Warm or Rigid Cold Heading -> 3. Thread Rolling prior to final aging/Q&T -> 4. Solution + Age (H900/H1025 for PH) or Oil Quench + Temper (for 14Kh17N2/20X13/30X13) -> 5. Passivation + Magnetic Particle / FPI.", poka_yoke_quality_gate: "Avoid tempering 14Kh17N2/20X13/30X13 in the 400–525°C embrittlement zone." },
    ShopFloorProcessRoute { route_number: 5, category_name: "Route 5: Austenitic Stainless & Solid-Solution Alloys", applicable_grades: "SS 304, SS 316, 12Kh18N10T (AISI 321), Inconel 600", metallurgical_behavior: "Non-heat-treatable by quenching; strength is achieved strictly through cold-work strain hardening.", sequence_steps: "1. Solution Annealed Wire + Oxalate + Calcium Stearate Soap -> 2. High-Rigidity Cold Heading -> 3. Direct Cold Thread Rolling (work-hardens thread roots to A2/A4-70 or 80) -> 4. Wash & Nitric Passivation.", poka_yoke_quality_gate: "No quench & temper HT; Exempt from hydrogen baking under standard nitric passivation." },
];
