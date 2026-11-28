//! Port of CalculationEngine.kt: the full fastener calculation (thread limits, slug, loads, quotation, heat treatment).

use crate::bolt_forging::{self, BoltForgingResult};
use crate::catalog::{self, HeatTreatmentSpec, MaterialInfo, INCH_SIZES, METRIC_SIZES};
use crate::dimensions::{self, HeadDimensions};
use crate::gen_enums::*;
use crate::heading::{self, BuildArgs, HeadingPlan};
use crate::heat_treat_cycle;
use crate::jfmt::{self, d as dstr, round_to_int};
use crate::part_geometry::{self, DrawingGeometry, FinishedPart};
use crate::poka_yoke::{self, CheckInput, ClassStrength, PokaYokeIssue};
use crate::soft_anneal::{self, SoftAnnealSpec};
use crate::tensile::{hardness_conversion, mill_cert_check, temper_guide, MillCert, TensileRange, WireCheck};
use crate::thread::{thread_rolling_blank, IsoThreadClass};
use crate::tooling;
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

/// Kotlin `Double.round(decimals)` in CalculationEngine: `(x * 10ⁿ).roundToInt() / 10ⁿ`.
pub fn round(x: f64, decimals: u32) -> f64 {
    let mut m = 1.0;
    for _ in 0..decimals {
        m *= 10.0;
    }
    round_to_int(x * m) as f64 / m
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ForgingStage {
    pub stage_number: i32,
    pub title: String,
    pub description: String,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FastenerCalculationResult {
    pub fastener_type: FastenerType,
    pub unit_system: UnitSystem,
    pub standard: ProductStandard,
    pub thread_label: String,
    pub nominal_dia_mm: f64,
    pub pitch_mm: f64,
    pub length_mm: f64,
    pub thread_length_mm: f64,
    pub thread_class: String,
    pub material: MaterialInfo,
    pub coating: CoatingType,
    pub batch_qty: i32,
    pub is_internal_nut: bool,
    pub blank_target_dia: f64,
    pub blank_tol_half: f64,
    pub blank_upper_limit: f64,
    pub blank_lower_limit: f64,
    pub fundamental_deviation_mm: f64,
    pub major_dia_min: f64,
    pub major_dia_mean: f64,
    pub major_dia_max: f64,
    pub pitch_dia_min: f64,
    pub pitch_dia_sweet: f64,
    pub pitch_dia_max: f64,
    pub minor_dia_min: f64,
    pub minor_dia_max: f64,
    pub root_radius_min_mm: Option<f64>,
    pub root_radius_max_mm: Option<f64>,
    pub pre_plate_pitch_allowance_um: f64,
    pub coating_spec_text: String,
    pub wire_dia_mm: f64,
    pub net_weight_grams: f64,
    pub gross_weight_grams: f64,
    pub cut_off_length_mm: f64,
    pub upset_ld_ratio: f64,
    pub raw_material_sourcing_uts: String,
    pub post_forging_target_uts_mpa: f64,
    pub yield_strength_mpa: f64,
    pub min_elongation_pct: f64,
    #[serde(rename = "reductionOfAreaZPct")]
    pub reduction_of_area_z_pct: f64,
    pub flow_stress_mpa: f64,
    pub stress_area_as_mm2: f64,
    pub proof_load_kn: f64,
    pub tensile_breaking_load_kn: f64,
    pub area_reduction_pct: f64,
    pub forging_tonnage: f64,
    pub recommended_machine: String,
    pub forging_stages: Vec<ForgingStage>,
    pub heat_treatment_spec: HeatTreatmentSpec,
    pub cost_raw_material: f64,
    pub cost_forging_heading: f64,
    pub cost_cnc_turning: f64,
    pub cost_thread_rolling: f64,
    pub cost_heat_treatment: f64,
    pub cost_inspection_ndt: f64,
    pub unit_cost_subtotal: f64,
    pub final_unit_price: f64,
    pub batch_total_price: f64,
    pub currency_symbol: String,
    pub has_hydrogen_embrittlement_risk: bool,
    pub stress_concentration_kt: f64,
    pub peak_thread_root_stress_mpa: f64,
    pub fatigue_endurance_limit_se_mpa: f64,
    pub allowable_alternating_force_kn: f64,
    pub forging_risks: Vec<String>,
    pub forging_recommendations: Vec<String>,
    pub property_class: Option<PropertyClass>,
    pub poka_yoke_issues: Vec<PokaYokeIssue>,
    pub head_dimensions: Option<HeadDimensions>,
    pub heading_plan: Option<HeadingPlan>,
    pub bolt_forging: Option<BoltForgingResult>,
    pub soft_anneal_spec: Option<SoftAnnealSpec>,
    pub finished_part: Option<FinishedPart>,
    pub tensile_range: Option<TensileRange>,
    pub thread_lock_patch: bool,
    pub open_extrusion_limit_pct: f64,
    pub wire_dia_override_mm: Option<f64>,
    pub auto_wire_dia_mm: f64,
    pub mill_cert: Option<MillCert>,
    pub wire_check: Option<WireCheck>,
    pub header_choice: HeaderChoice,
    pub drawing: DrawingGeometry,
}

impl FastenerCalculationResult {
    pub fn part_volume_mm3(&self) -> f64 {
        self.finished_part.as_ref().map(|p| p.volume_mm3()).unwrap_or(0.0)
    }
    pub fn head_volume_mm3(&self) -> f64 {
        self.finished_part.as_ref().map(|p| p.head_volume_mm3()).unwrap_or(0.0)
    }
    pub fn shank_volume_mm3(&self) -> f64 {
        self.finished_part.as_ref().map(|p| p.shank_volume_mm3()).unwrap_or(0.0)
    }
    pub fn head_weight_grams(&self) -> f64 {
        self.head_volume_mm3() / 1000.0 * self.material.density
    }
    pub fn shank_weight_grams(&self) -> f64 {
        self.shank_volume_mm3() / 1000.0 * self.material.density
    }
    pub fn critical_issues(&self) -> Vec<&PokaYokeIssue> {
        self.poka_yoke_issues.iter().filter(|i| i.severity == PokaYokeSeverity::CRITICAL).collect()
    }
    pub fn is_release_blocked(&self) -> bool {
        self.poka_yoke_issues.iter().any(|i| i.severity == PokaYokeSeverity::CRITICAL)
    }
}

/// Inputs of `calculate`, with the Kotlin default values.
#[derive(Clone, Debug)]
pub struct CalcInput {
    pub fastener_type: FastenerType,
    pub unit_system: UnitSystem,
    pub standard: ProductStandard,
    pub nominal_size_index: usize,
    pub pitch_value: f64,
    pub length_mm: f64,
    pub thread_class: String,
    pub material: MaterialInfo,
    pub coating: CoatingType,
    pub batch_qty: i32,
    pub currency_is_usd: bool,
    pub cnc_hour_rate_inr: f64,
    pub ht_rate_per_kg_inr: f64,
    pub margin_pct: f64,
    pub custom_nominal_dia_mm: Option<f64>,
    pub extracted_radius: Option<f64>,
    pub property_class: Option<PropertyClass>,
    pub drawing: DrawingGeometry,
    pub tensile_range: Option<TensileRange>,
    pub thread_lock_patch: bool,
    pub mill_cert: Option<MillCert>,
    pub wire_dia_override_mm: Option<f64>,
    pub open_extrusion_limit_pct: f64,
    pub header_choice: HeaderChoice,
}

impl CalcInput {
    #[allow(clippy::too_many_arguments)]
    pub fn new(
        fastener_type: FastenerType, unit_system: UnitSystem, standard: ProductStandard, nominal_size_index: usize, pitch_value: f64,
        length_mm: f64, thread_class: &str, material: MaterialInfo, coating: CoatingType, batch_qty: i32,
    ) -> Self {
        CalcInput {
            fastener_type,
            unit_system,
            standard,
            nominal_size_index,
            pitch_value,
            length_mm,
            thread_class: thread_class.into(),
            material,
            coating,
            batch_qty,
            currency_is_usd: false,
            cnc_hour_rate_inr: 750.0,
            ht_rate_per_kg_inr: 180.0,
            margin_pct: 22.0,
            custom_nominal_dia_mm: None,
            extracted_radius: None,
            property_class: None,
            drawing: DrawingGeometry::default(),
            tensile_range: None,
            thread_lock_patch: false,
            mill_cert: None,
            wire_dia_override_mm: None,
            open_extrusion_limit_pct: heading::MAX_DIE_EXTRUSION_PCT,
            header_choice: HeaderChoice::AUTO,
        }
    }
}

/// Kotlin `Double.toString()` of a whole number prints "10.0"; the thread label prints "10".
fn size_text(v: f64) -> String {
    if v % 1.0 == 0.0 {
        (v as i32).to_string()
    } else {
        dstr(v)
    }
}

fn substring_before(s: &str, d: &str) -> String {
    s.find(d).map(|i| s[..i].to_string()).unwrap_or_else(|| s.to_string())
}

fn metric_size(i: usize) -> &'static catalog::MetricSize {
    METRIC_SIZES.get(i).unwrap_or(&METRIC_SIZES[catalog::default_size_index(UnitSystem::METRIC)])
}
fn inch_size(i: usize) -> &'static catalog::InchSize {
    INCH_SIZES.get(i).unwrap_or(&INCH_SIZES[catalog::default_size_index(UnitSystem::INCH)])
}

pub fn calculate(inp: &CalcInput) -> FastenerCalculationResult {
    let fastener_type = inp.fastener_type;
    let unit_system = inp.unit_system;
    let material = &inp.material;
    let coating = inp.coating;
    let thread_class = inp.thread_class.as_str();
    let length_mm = inp.length_mm;
    let drawing = &inp.drawing;
    let tensile_range = inp.tensile_range.as_ref();
    let property_class = if tensile_range.is_some() { Some(PropertyClass::MATERIAL_SPEC) } else { inp.property_class };
    let safe_batch_qty = inp.batch_qty.max(1);
    let d: f64 = inp.custom_nominal_dia_mm.unwrap_or_else(|| {
        if unit_system == UnitSystem::METRIC {
            metric_size(inp.nominal_size_index).d
        } else {
            round_to_int(inch_size(inp.nominal_size_index).d_inch * 25.4 * 1000.0) as f64 / 1000.0
        }
    });
    let p = if unit_system == UnitSystem::METRIC || inp.pitch_value < 5.0 { inp.pitch_value } else { 25.4 / inp.pitch_value };

    let is_nut = fastener_type.is_nut();
    let thread_label = if fastener_type.is_unthreaded() {
        format!("Ø{} rivet (no thread)", dstr(round(d, 2)))
    } else if let Some(custom) = inp.custom_nominal_dia_mm {
        let std = if unit_system == UnitSystem::METRIC {
            METRIC_SIZES
                .iter()
                .find(|s| (s.d - custom).abs() < 0.02 && s.pitches.iter().any(|pp| (pp - p).abs() < 0.005))
                .map(|s| format!("M{}×{} - {}", size_text(s.d), dstr(p), thread_class))
        } else {
            INCH_SIZES
                .iter()
                .find(|s| (s.d_inch * 25.4 - custom).abs() < 0.03)
                .and_then(|s| s.tpis.iter().find(|&&t| (t as f64 - 25.4 / p).abs() < 0.1).map(|tpi| format!("{}-{} {}", substring_before(s.label, " "), tpi, thread_class)))
        };
        std.unwrap_or_else(|| format!("Custom Ø{}×{} - {}", dstr(custom), dstr(round(p, 3)), thread_class))
    } else if unit_system == UnitSystem::METRIC {
        format!("M{}×{} - {}", size_text(metric_size(inp.nominal_size_index).d), dstr(p), thread_class)
    } else {
        format!("{}-{} {}", substring_before(inch_size(inp.nominal_size_index).label, " "), inp.pitch_value as i32, thread_class)
    };

    let is_unj = thread_class.to_lowercase().contains("unj");
    let mut root_r_min: Option<f64> = None;
    let mut root_r_max: Option<f64> = None;
    let (es, d2_min, d2_max, d2_sweet, major_min, major_max, major_mean, minor_min, minor_max, blank_dia, blank_tol_half);

    if unit_system == UnitSystem::METRIC {
        let td2_grade6 = 0.090 * p.powf(0.4) * d.powf(0.1);
        let td_maj_grade6 = 0.180 * p.powf(0.65);
        if is_nut {
            let cls = IsoThreadClass::parse(thread_class);
            let ei = IsoThreadClass::internal_ei(&cls.position, p);
            let td2_nut = IsoThreadClass::grade_factor(cls.pitch_grade) * 1.32 * td2_grade6;
            let td1_grade6 = if p < 1.0 { 0.433 * p - 0.190 * p.powf(1.22) } else { 0.230 * p.powf(0.7) };
            let td1_nut = IsoThreadClass::grade_factor(cls.crest_grade) * td1_grade6;
            es = ei;
            let d2_basic = d - 0.649519 * p;
            d2_min = d2_basic + ei;
            d2_max = d2_min + td2_nut;
            d2_sweet = d2_min + 0.62 * (d2_max - d2_min);
            major_min = d + ei;
            major_max = major_min + 0.15 * p;
            major_mean = (major_min + major_max) / 2.0;
            let d1_basic = d - (if cls.is_mj { 0.974278 } else { 1.082532 }) * p;
            minor_min = d1_basic + ei;
            minor_max = minor_min + td1_nut;
            blank_dia = (minor_min + minor_max) / 2.0;
            blank_tol_half = (minor_max - minor_min) / 2.0;
        } else {
            let cls = IsoThreadClass::parse(thread_class);
            let deviation = IsoThreadClass::external_es(&cls.position, p);
            let pitch_grade_mult = IsoThreadClass::grade_factor(cls.pitch_grade);
            let major_grade_mult = IsoThreadClass::major_grade_factor(cls.crest_grade);
            es = deviation;
            let td2 = pitch_grade_mult * td2_grade6;
            let td_maj = major_grade_mult * td_maj_grade6;
            let d2_basic = d - 0.649519 * p;
            d2_max = d2_basic + es;
            d2_min = d2_max - td2;
            d2_sweet = d2_min + 0.38 * (d2_max - d2_min);
            major_max = d + es;
            major_min = major_max - td_maj;
            major_mean = (major_max + major_min) / 2.0;
            if cls.is_mj {
                minor_max = d2_max - 0.50518 * p;
                minor_min = d2_min - 0.5658 * p;
                root_r_min = Some(0.15011 * p);
                root_r_max = Some(0.18042 * p);
            } else {
                minor_max = d2_max - 0.577350 * p;
                minor_min = d2_min - 0.577350 * p;
                root_r_min = Some(0.125 * p);
                root_r_max = Some(0.144 * p);
            }
            let d2_target = d2_min + 0.55 * td2;
            blank_dia = d2_target - 0.014 * p + material.km * p + if cls.is_mj { thread_rolling_blank::controlled_root_delta_mid(p) } else { 0.0 };
            blank_tol_half = 0.22 * td2;
        }
    } else {
        let d_inch = d / 25.4;
        let p_inch = p / 25.4;
        let td2_2a_inch = 0.0015 * d_inch.powf(1.0 / 3.0) + 0.0015 * d_inch.sqrt() + 0.015 * p_inch.powf(2.0 / 3.0);
        let td2_2a = td2_2a_inch * 25.4;
        let allowance = 0.30 * td2_2a;
        if is_nut {
            es = 0.0;
            let nut_mult = match thread_class {
                "3B" | "3B-UNJ" => 0.975,
                "1B" => 1.95,
                _ => 1.30,
            };
            let td2 = nut_mult * td2_2a;
            let d2_basic = d - 0.649519 * p;
            d2_min = d2_basic;
            d2_max = d2_min + td2;
            d2_sweet = d2_min + 0.62 * (d2_max - d2_min);
            major_min = d;
            major_max = d + 0.15 * p;
            major_mean = (major_min + major_max) / 2.0;
            if is_unj {
                minor_min = d - 0.974278 * p;
                minor_max = minor_min + 0.15 * p;
                root_r_min = Some(0.15011 * p);
                root_r_max = Some(0.18042 * p);
            } else {
                minor_min = d - 1.082532 * p;
                minor_max = minor_min + 0.20 * p;
            }
            blank_dia = (minor_min + minor_max) / 2.0;
            blank_tol_half = (minor_max - minor_min) / 2.0;
        } else {
            let dev = match thread_class {
                "3A" | "3A-UNJ" => 0.0,
                _ => -allowance,
            };
            let tol_mult = match thread_class {
                "3A" | "3A-UNJ" => 0.75,
                "1A" => 1.50,
                _ => 1.00,
            };
            es = dev;
            let td2 = tol_mult * td2_2a;
            let td_maj = tol_mult * (0.060 * p_inch.powf(2.0 / 3.0) * 25.4);
            let d2_basic = d - 0.649519 * p;
            d2_max = d2_basic + es;
            d2_min = d2_max - td2;
            d2_sweet = d2_min + 0.38 * (d2_max - d2_min);
            major_max = d + es;
            major_min = major_max - td_maj;
            major_mean = (major_max + major_min) / 2.0;
            if is_unj {
                root_r_min = Some(0.15011 * p);
                root_r_max = Some(0.18042 * p);
                minor_max = d2_max - 0.50518 * p;
                minor_min = d2_min - 0.5658 * p;
            } else {
                minor_max = d2_max - 0.577350 * p;
                minor_min = d2_min - 0.577350 * p;
                root_r_min = Some(0.125 * p);
                root_r_max = Some(0.144 * p);
            }
            let d2_target = d2_min + 0.55 * td2;
            blank_dia = d2_target - 0.014 * p + material.km * p + if is_unj { thread_rolling_blank::controlled_root_delta_mid(p) } else { 0.0 };
            blank_tol_half = 0.22 * td2;
        }
    }

    let blank_upper = blank_dia + blank_tol_half;
    let blank_lower = blank_dia - blank_tol_half;
    let pre_plate_offset_um = 4.0 * coating.thickness_max_um();

    let lt = drawing.thread_length_mm.unwrap_or_else(|| poka_yoke::thread_length_b(fastener_type, unit_system, d, length_mm));
    let mut dw = round_to_int(d * 0.985 * 100.0) as f64 / 100.0;
    let wire_choice = inp.wire_dia_override_mm.filter(|&v| v > 0.5);
    if let Some(w) = wire_choice {
        dw = w;
    }
    let dims = dimensions::for_part(fastener_type, unit_system, d);
    let s = dims.across_flats_s;
    let k = dims.head_height_k;
    let socket = dimensions::socket_for(fastener_type, unit_system, d);
    let flange = if fastener_type == FastenerType::FLANGE_BOLT {
        Some(bolt_forging::resolve_flange(d, drawing.flange_dia_mm, drawing.head_height_mm, drawing.across_flats_mm, drawing.flange_thickness_mm))
    } else {
        None
    };
    let bolt_forging = if bolt_forging::HEX_TYPES.contains(&fastener_type) {
        Some(bolt_forging::compute(
            fastener_type, unit_system, d, p, length_mm, blank_dia, dw, Some(&dims), material.density, Some(lt), drawing.shank_dia_mm,
            drawing.across_flats_mm, drawing.head_height_mm, None, None,
        ))
    } else if let Some(fl) = flange.filter(|f| f.complete()) {
        Some(bolt_forging::compute(fastener_type, unit_system, d, p, length_mm, blank_dia, dw, Some(&dims), material.density, Some(lt), drawing.shank_dia_mm, fl.s, fl.k, fl.dc, fl.c))
    } else {
        None
    }
    .filter(|b| b.finished_volume_mm3.is_some() && b.cheese_volume_mm3.is_some());
    let trim_s = if let (Some(fl), Some(_)) = (flange, &bolt_forging) { fl.s.unwrap() } else { drawing.across_flats_mm.unwrap_or(s) };
    let trim_k = if let (Some(fl), Some(_)) = (flange, &bolt_forging) { fl.k.unwrap() } else { drawing.head_height_mm.unwrap_or(k) };
    let forged_head_dia = bolt_forging.as_ref().map(|_| if let Some(fl) = flange { fl.dc.unwrap() } else { 1.2 * trim_s });

    let flange_for_part = flange.filter(|_| bolt_forging.is_some());
    let part = part_geometry::build(fastener_type, unit_system, d, length_mm, lt, blank_dia, &dims, socket, drawing, bolt_forging.as_ref(), flange_for_part.as_ref());
    let full_thread_headed = part.head_segment_count > 0
        && part.style != HeadStyle::HEX_TRIMMED
        && part.style != HeadStyle::NUT
        && part.style != HeadStyle::WASHER
        && !fastener_type.is_unthreaded()
        && !part.die_segments().is_empty()
        && part.die_segments().iter().all(|s| (s.d_bottom_mm - blank_dia).abs() < 1e-9 && (s.d_top_mm - blank_dia).abs() < 1e-9);
    if full_thread_headed && wire_choice.is_none() {
        dw = (blank_dia * 100.0 - 1e-6).ceil() / 100.0;
    }
    let auto_wire = if full_thread_headed { (blank_dia * 100.0 - 1e-6).ceil() / 100.0 } else { round_to_int(d * 0.985 * 100.0) as f64 / 100.0 };
    let total_vol_mm3 = part.volume_mm3();
    let hex_head_area = (3.0f64.sqrt() / 2.0) * s.powf(2.0);
    let vol_head = match part.style {
        HeadStyle::HEADLESS => 0.0,
        HeadStyle::NUT | HeadStyle::WASHER => hex_head_area * k,
        _ => part.head_volume_mm3(),
    };
    let head_area = if let Some(fh) = forged_head_dia {
        (PI / 4.0) * fh.powf(2.0)
    } else if part.style == HeadStyle::HEADLESS || part.style == HeadStyle::WASHER {
        hex_head_area
    } else {
        part.head_segments().iter().map(|s| s.cross_section_mm2()).fold(None, |a: Option<f64>, v| Some(a.map_or(v, |a| a.max(v)))).unwrap_or(hex_head_area)
    };

    let net_weight_g = 0.05f64.max((total_vol_mm3 / 1000.0) * material.density);
    let wire_area_mm2 = ((PI / 4.0) * dw.powf(2.0)).max(1e-6);
    let heading_plan = heading::build(&BuildArgs {
        t: fastener_type,
        wire_dia_mm: dw,
        part: &part,
        length_mm,
        thread_label: &thread_label,
        forging: bolt_forging.as_ref(),
        forged_head_dia_mm: forged_head_dia,
        trimmed_hex_s: trim_s,
        trimmed_hex_k: trim_k,
        flange_dia_mm: flange.and_then(|f| f.dc).filter(|_| bolt_forging.is_some()),
        flange_thickness_mm: flange.and_then(|f| f.c).filter(|_| bolt_forging.is_some()),
        choice: inp.header_choice,
        wire_fixed: wire_choice.is_some(),
        open_limit: inp.open_extrusion_limit_pct,
    });
    let two_blow = heading_plan.as_ref().map(|h| h.route) == Some(HeaderRoute::TWO_BLOW);
    if two_blow {
        dw = heading_plan.as_ref().unwrap().slug_dia_mm;
    }
    let cut_off_len_mm = heading_plan.as_ref().map(|h| h.slug_length_mm).unwrap_or(total_vol_mm3 / wire_area_mm2);
    let gross_weight_g = heading_plan.as_ref().map(|h| net_weight_g.max(h.slug_volume_mm3 / 1000.0 * material.density)).unwrap_or(net_weight_g * 1.04);
    let ld_ratio = if two_blow {
        part.head_volume_mm3() / ((PI / 4.0) * dw * dw) / dw
    } else if let Some(hs) = bolt_forging.as_ref().and_then(|b| b.head_stock_mm) {
        hs / bolt_forging.as_ref().unwrap().shank_dia_mm
    } else if vol_head > 0.0 {
        (vol_head / wire_area_mm2) / dw
    } else {
        0.0
    };

    let true_strain_eps = 1.15f64.max(ld_ratio + 1.0).ln();
    let flow_stress_mpa = material.flow_stress_coeff_k * true_strain_eps.powf(material.strain_hardening_n);
    let stress_area_as_mm2 = if fastener_type.is_unthreaded() {
        (PI / 4.0) * d.powf(2.0)
    } else {
        (PI / 4.0) * (d - (if unit_system == UnitSystem::INCH { 0.974279 } else { 0.938194 }) * p).powf(2.0)
    };
    let strength = if let Some(tr) = tensile_range {
        tr.strength()
    } else if let Some(pc) = property_class {
        poka_yoke::class_strength(pc, d, material)
    } else {
        ClassStrength { rm_min_mpa: material.post_forging_target_uts_mpa, yield_min_mpa: material.yield_strength_mpa, proof_stress_mpa: material.yield_strength_mpa * 0.9, is_iso_steel_class: false }
    };
    let effective_rm_mpa = strength.rm_min_mpa;
    let effective_yield_mpa = strength.yield_min_mpa;
    let proof_load_kn = (stress_area_as_mm2 * if is_nut { effective_rm_mpa } else { strength.proof_stress_mpa }) / 1000.0;
    let tensile_breaking_load_kn = (stress_area_as_mm2 * effective_rm_mpa) / 1000.0;

    let blank_area_mm2 = (PI / 4.0) * blank_dia.powf(2.0);
    let area_red_pct = 0.0f64.max(((wire_area_mm2 - blank_area_mm2) / wire_area_mm2) * 100.0);
    let forging_tonnage = ((head_area * flow_stress_mpa * 1.55) / 9806.65) * 1.20;
    let machine_capacity = ((forging_tonnage / 25.0).ceil() * 25.0) as i32;
    let recommended_machine = match &heading_plan {
        None => "Progressive blank & pierce press".to_string(),
        Some(hp) => {
            let warm = if is_warm_headed(material, d) { "warm " } else { "" };
            let desc = hp.machine_description();
            let mut ch = desc.chars();
            let cap = match ch.next() {
                Some(c) => c.to_uppercase().collect::<String>() + ch.as_str(),
                None => String::new(),
            };
            format!("{} ({}{}T @ {} SPM)", cap, warm, machine_capacity, material.heading_spm)
        }
    };

    let mut numbered_stages: Vec<ForgingStage> = match &heading_plan {
        None => vec![
            ForgingStage { stage_number: 1, title: "Strip feed & level".into(), description: "Level coil strip to washer thickness".into() },
            ForgingStage { stage_number: 2, title: "Pierce".into(), description: "Pierce bore in progressive die".into() },
            ForgingStage { stage_number: 3, title: "Blank".into(), description: "Blank outside diameter".into() },
            ForgingStage { stage_number: 4, title: "Deburr & flatten".into(), description: "Vibratory deburr, check flatness".into() },
        ],
        Some(hp) => {
            let mut v: Vec<ForgingStage> = hp
                .stations
                .iter()
                .map(|st| {
                    let label = if st.is_cut_off() { st.title.clone() } else { format!("Die {} · {}", st.index, st.title) };
                    let extra = if st.is_cut_off() {
                        format!(" ({})", material.raw_material_sourcing_uts_mpa)
                    } else if st.is_peak_blow {
                        format!(" ({}T)", dstr(round(forging_tonnage, 1)))
                    } else {
                        String::new()
                    };
                    ForgingStage { stage_number: 0, title: label, description: format!("{}{}", st.operation, extra) }
                })
                .collect();
            v.extend(hp.off_header_ops.iter().map(|o| ForgingStage { stage_number: 0, title: format!("Off-header · {}", o.title), description: o.operation.clone() }));
            v
        }
    };
    for (i, st) in numbered_stages.iter_mut().enumerate() {
        st.stage_number = i as i32 + 1;
    }

    let he_family = poka_yoke::material_family(material);
    let has_he_risk = coating.is_electroplating()
        && effective_rm_mpa >= 1000.0
        && (he_family == MaterialFamily::CARBON_ALLOY_STEEL || he_family == MaterialFamily::STAINLESS && material.has_high_hardness);

    let max_rpm = if material.is_aero { 1800 } else { 3200 };
    let calc_rpm = max_rpm.min(round_to_int((1000.0 * material.vc) / (PI * 1.0f64.max(d))));
    let total_cnc_sec = if fastener_type.is_unthreaded() { 0.0 } else { ((length_mm / (calc_rpm as f64 * material.feed)) * 60.0 * 2.1) + 5.5 };

    let wire_check = mill_cert_check::check(material, fastener_type, Some(&part), inp.mill_cert.as_ref());

    let fx = if inp.currency_is_usd { 1.0 / 86.50 } else { 1.0 };
    let currency_symbol = if inp.currency_is_usd { "$" } else { "₹" };
    let mat_rate = material.rate_inr * fx;
    let aero = material.is_aero;
    let batch = safe_batch_qty as f64;
    let c_raw_mat = ((gross_weight_g / 1000.0) * mat_rate) - (((gross_weight_g - net_weight_g) / 1000.0) * mat_rate * 0.35);
    let c_forging = (((if aero { 1800.0 } else { 850.0 }) * fx) / (material.heading_spm as f64 * 60.0 * 0.75)) + (((if aero { 24000.0 } else { 6500.0 }) * fx) / batch);
    let c_cnc = if fastener_type.is_unthreaded() { 0.0 } else { ((total_cnc_sec / 3600.0) * (inp.cnc_hour_rate_inr * fx)) + ((if aero { 3.8 } else { 0.75 }) * fx) };
    let c_roll = if fastener_type.is_unthreaded() { 0.0 } else { (if aero { 4.8 } else { 0.65 }) * fx };
    let c_ht = ((net_weight_g / 1000.0) * (inp.ht_rate_per_kg_inr * fx)) + ((if aero { 5.5 } else { 0.45 }) * fx);
    let c_ndt = (if aero { 3.8 + (7500.0 / batch) } else { 0.30 }) * fx;
    let unit_subtotal = c_raw_mat + c_forging + c_cnc + c_roll + c_ht + c_ndt;
    let final_unit = unit_subtotal * (1.0 + inp.margin_pct / 100.0);
    let batch_total = final_unit * inp.batch_qty as f64;

    let (risks, recommendations) = analyze_forging_and_crack_risks(inp.extracted_radius, material, ld_ratio, d);

    let root_radius_r = 0.144 * p;
    let kt_factor = if fastener_type.is_unthreaded() { 1.0 } else { jfmt::clamp(1.0 + 2.0 * ((p / 2.0) / root_radius_r).sqrt(), 2.2, 4.2) };
    let nominal_tensile_stress = (proof_load_kn * 1000.0) / stress_area_as_mm2.max(1.0);
    let peak_thread_root_stress = nominal_tensile_stress * kt_factor;
    let sigma_asv = 0.85 * (150.0 / d.max(1.0) + 45.0);
    let endurance_se = if aero { (2.0 - 0.75) * sigma_asv } else { sigma_asv };
    let allowable_alt_force_kn = endurance_se * stress_area_as_mm2 / 1.2 / 1000.0;

    let mut ht = calculate_dynamic_heat_treatment(material, d, has_he_risk, property_class);
    if let Some(tr) = tensile_range {
        ht = heat_treat_for_range(&ht, material, tr);
    }

    let mut issues = poka_yoke::check(&CheckInput {
        t: fastener_type,
        unit: unit_system,
        standard: inp.standard,
        nominal_size_index: inp.nominal_size_index,
        pitch_value: inp.pitch_value,
        d,
        length_mm,
        thread_class,
        property_class,
        material,
        coating,
        batch_qty: inp.batch_qty,
        fundamental_deviation_mm: es,
        pitch_dia_min_mm: d2_min,
        pitch_dia_max_mm: d2_max,
        is_custom_scan: inp.custom_nominal_dia_mm.is_some(),
        stress_area_mm2: stress_area_as_mm2,
        head_dims: Some(&dims),
        thread_length_mm: drawing.thread_length_mm,
        tensile_range,
        thread_lock_patch: inp.thread_lock_patch,
        head_from_drawing: drawing.head_height_mm.is_some() && (drawing.head_dia_mm.is_some() || drawing.across_flats_mm.is_some()),
    });
    if let Some(f) = heading_plan.as_ref().and_then(|h| h.feasibility_issue.as_ref()) {
        issues.push(PokaYokeIssue::new(
            PokaYokeSeverity::CRITICAL,
            "1-die 2-blow cannot make this part",
            &format!("On a 1-die 2-blow header: {}.", f),
            "Choose Multi-station, or change the head size / wire so the head gathers in two blows.",
        ));
    }
    issues.extend(mill_cert_check::issues(wire_check.as_ref()));
    issues.extend(wire_choice_issues(wire_choice, dw, &part, blank_dia, ld_ratio, inp.open_extrusion_limit_pct));
    if let Some(pl) = &heading_plan {
        for e in tooling::extrusions(pl).iter().filter(|e| e.reduction_pct > heading::TRAPPED_EXTRUSION_MAX_PCT) {
            issues.push(PokaYokeIssue::new(
                PokaYokeSeverity::CRITICAL,
                "Extrusion over the trapped limit",
                &format!(
                    "Station {} extrudes Ø{} → Ø{}: {} % area, over the ~{} % that trapped extrusion takes in one blow.",
                    e.station_index,
                    dstr(round(e.from_mm, 2)),
                    dstr(round(e.to_mm, 2)),
                    round(e.reduction_pct, 0) as i32,
                    heading::TRAPPED_EXTRUSION_MAX_PCT as i32
                ),
                "Extrude in two stations, or choose a thinner wire.",
            ));
        }
    }

    FastenerCalculationResult {
        fastener_type,
        unit_system,
        standard: inp.standard,
        thread_label,
        nominal_dia_mm: d,
        pitch_mm: p,
        length_mm,
        thread_length_mm: lt,
        thread_class: thread_class.to_string(),
        material: material.clone(),
        coating,
        batch_qty: inp.batch_qty,
        is_internal_nut: is_nut,
        blank_target_dia: round(blank_dia, 3),
        blank_tol_half: round(blank_tol_half, 3),
        blank_upper_limit: round(blank_upper, 3),
        blank_lower_limit: round(blank_lower, 3),
        fundamental_deviation_mm: round(es, 4),
        major_dia_min: round(major_min, 3),
        major_dia_mean: round(major_mean, 3),
        major_dia_max: round(major_max, 3),
        pitch_dia_min: round(d2_min, 3),
        pitch_dia_sweet: round(d2_sweet, 3),
        pitch_dia_max: round(d2_max, 3),
        minor_dia_min: round(minor_min, 3),
        minor_dia_max: round(minor_max, 3),
        root_radius_min_mm: root_r_min.map(|v| round(v, 4)),
        root_radius_max_mm: root_r_max.map(|v| round(v, 4)),
        pre_plate_pitch_allowance_um: round(pre_plate_offset_um, 1),
        coating_spec_text: if coating.spec_standard().contains("µm") {
            coating.spec_standard().to_string()
        } else {
            format!("{} ({}–{} µm)", coating.spec_standard(), coating.thickness_min_um() as i32, coating.thickness_max_um() as i32)
        },
        wire_dia_mm: dw,
        net_weight_grams: round(net_weight_g, 2),
        gross_weight_grams: round(gross_weight_g, 2),
        cut_off_length_mm: round(cut_off_len_mm, 1),
        upset_ld_ratio: round(ld_ratio, 2),
        raw_material_sourcing_uts: material.raw_material_sourcing_uts_mpa.clone(),
        post_forging_target_uts_mpa: effective_rm_mpa,
        yield_strength_mpa: effective_yield_mpa,
        min_elongation_pct: if is_nut { material.min_elongation_pct } else { class_ductility(property_class).map(|x| x.0).unwrap_or(material.min_elongation_pct) },
        reduction_of_area_z_pct: if is_nut { material.reduction_of_area_z_pct } else { class_ductility(property_class).map(|x| x.1).unwrap_or(material.reduction_of_area_z_pct) },
        flow_stress_mpa: round(flow_stress_mpa, 1),
        stress_area_as_mm2: round(stress_area_as_mm2, 2),
        proof_load_kn: round(proof_load_kn, 1),
        tensile_breaking_load_kn: round(tensile_breaking_load_kn, 1),
        area_reduction_pct: round(area_red_pct, 1),
        forging_tonnage: round(forging_tonnage, 1),
        recommended_machine,
        forging_stages: numbered_stages,
        heat_treatment_spec: ht,
        cost_raw_material: round(c_raw_mat, 2),
        cost_forging_heading: round(c_forging, 2),
        cost_cnc_turning: round(c_cnc, 2),
        cost_thread_rolling: round(c_roll, 2),
        cost_heat_treatment: round(c_ht, 2),
        cost_inspection_ndt: round(c_ndt, 2),
        unit_cost_subtotal: round(unit_subtotal, 2),
        final_unit_price: round(final_unit, 2),
        batch_total_price: round(batch_total, 2),
        currency_symbol: currency_symbol.into(),
        has_hydrogen_embrittlement_risk: has_he_risk,
        stress_concentration_kt: round(kt_factor, 2),
        peak_thread_root_stress_mpa: round(peak_thread_root_stress, 1),
        fatigue_endurance_limit_se_mpa: round(endurance_se, 1),
        allowable_alternating_force_kn: round(allowable_alt_force_kn, 2),
        forging_risks: risks,
        forging_recommendations: recommendations,
        property_class,
        poka_yoke_issues: issues,
        head_dimensions: Some(dims),
        heading_plan,
        bolt_forging,
        soft_anneal_spec: Some(soft_anneal::build(material)),
        finished_part: Some(part),
        tensile_range: tensile_range.cloned(),
        thread_lock_patch: inp.thread_lock_patch,
        open_extrusion_limit_pct: inp.open_extrusion_limit_pct,
        wire_dia_override_mm: wire_choice,
        auto_wire_dia_mm: auto_wire,
        mill_cert: inp.mill_cert.clone(),
        wire_check,
        header_choice: inp.header_choice,
        drawing: drawing.clone(),
    }
}

fn f2(v: f64) -> String {
    jfmt::f(v, 2)
}

fn analyze_forging_and_crack_risks(extracted_radius: Option<f64>, material: &MaterialInfo, ld_ratio: f64, d: f64) -> (Vec<String>, Vec<String>) {
    let mut risks = Vec::new();
    let mut recs = Vec::new();
    let gather = "Gather the head in a cone pre-upset before the finishing blow (see the die progression for the stations).";
    if ld_ratio > heading::THREE_BLOW_LD {
        risks.push(format!(
            "⚠️ High Upset Ratio ({} > {}): beyond three blows — severe risk of buckling and folding laps; use larger wire or warm forming.",
            f2(ld_ratio),
            dstr(heading::THREE_BLOW_LD)
        ));
        recs.push(gather.to_string());
    } else if ld_ratio > 2.3 {
        risks.push(format!("⚠️ Upset L/D {} > 2.3: a single open upset would buckle; the die progression gathers the head in cone preform blow(s) first.", f2(ld_ratio)));
        recs.push(gather.to_string());
    } else {
        risks.push(format!("✅ Safe Upset Ratio ({} ≤ 2.3): Stable metal flow without buckling risk.", f2(ld_ratio)));
    }
    let family = poka_yoke::material_family(material);
    let name_lc = material.name.to_lowercase();
    if material.is_aero && family == MaterialFamily::CARBON_ALLOY_STEEL && !is_warm_headed(material, d) {
        risks.push(format!(
            "✈️ Aerospace alloy steel ({}): cold headable only from fully spheroidize-annealed wire; it work-hardens faster than commercial grades.",
            material.name
        ));
        recs.push("Buy or anneal the wire fully spheroidized (typically ≤ 92 HRB), use phosphate + soap or polymer coating, and magnetic-particle check the first-off heads for cracks.".into());
    } else if material.is_aero && family == MaterialFamily::NICKEL_SUPERALLOY {
        risks.push(format!(
            "✈️ Nickel superalloy ({}): work-hardens fast. Head from solution-annealed wire (cold, small sizes, re-anneal between blows if needed) or hot head locally by induction from cold-drawn bar.",
            material.name
        ));
        recs.push("Re-anneal with the solution (mill) anneal and a RAPID cool (water for 718); a furnace cool leaves the wire hard. Do not soak the wire between about 600 and 1000 °C (ageing and δ range) except for the final age on the finished part. Take the hot-heading temperature window from your process qualification.".into());
    } else if material.has_high_hardness && material.is_aero {
        risks.push(format!(
            "✈️ High-Strength Aerospace Alloy Warning ({}): Rapid work-hardening behavior. Room-temperature cold working will cause catastrophic micro-cracking.",
            material.name
        ));
        recs.push("Strictly enforce Controlled Warm Forging / Induction Heating (650°C–720°C) with Glass-MoS2 dual lubrication.".into());
    } else if name_lc.contains("stainless") || name_lc.contains("ss204") || name_lc.contains("ss304") {
        risks.push("⚠️ Austenitic Stainless Steel Warning: High strain-hardening rate. Single-stage trap extrusion will cause internal tearing.".into());
        recs.push("Use multi-stage progressive extrusion with max 22-25% reduction per stage + Oxalate/MoS2 coating.".into());
    } else {
        recs.push(format!("Standard Cold/Warm progressive forging sequence is fully feasible for {}.", material.name));
    }
    if let Some(r) = extracted_radius {
        if r < 0.15 {
            risks.push(format!(
                "⚠️ Sharp Fillet Radius Detected ({} mm): High stress concentration factor (Kt). Risk of quenching cracks and fatigue failure.",
                dstr(r)
            ));
            recs.push("Increase fillet radius or specify a controlled root radius transition (e.g., UNJC thread profile with R0.12 min).".into());
        }
    }
    (risks, recs)
}

const COLD_FORMED_CLASSES: [PropertyClass; 5] = [PropertyClass::CLASS_4_6, PropertyClass::CLASS_4_8, PropertyClass::CLASS_5_6, PropertyClass::CLASS_5_8, PropertyClass::CLASS_6_8];
const QT_STEELS: [&str; 14] = ["10B21", "4135", "4140", "4340", "42CRMO4", "AISI_51B37", "EN24", "EN24_ESR", "8740", "AISI_4130", "30KHGSA", "16KHGSN", "35NCD16", "SS410"];

fn wire_choice_issues(choice: Option<f64>, dw: f64, part: &FinishedPart, blank_dia: f64, ld: f64, open_limit: f64) -> Vec<PokaYokeIssue> {
    if choice.is_none() {
        return vec![];
    }
    let f = |v: f64, dp: usize| jfmt::f(v, dp);
    let mut out = Vec::new();
    if ld > heading::THREE_BLOW_LD {
        out.push(PokaYokeIssue::new(
            PokaYokeSeverity::CRITICAL,
            "Wire too thin to gather the head",
            &format!(
                "Head stock L/D {} on your Ø{} wire is past the 3-blow limit of {} used by the app: severe buckling / folding risk.",
                f(ld, 2),
                f(dw, 2),
                dstr(heading::THREE_BLOW_LD)
            ),
            "Choose a larger wire (Auto picks one), or plan warm forming.",
        ));
    }
    let shank = part.die_segments().iter().map(|s| s.d_bottom_mm.max(s.d_top_mm)).fold(None, |a: Option<f64>, v| Some(a.map_or(v, |a| a.max(v))));
    if let Some(shank) = shank {
        if dw < shank - 0.005 {
            out.push(PokaYokeIssue::new(
                PokaYokeSeverity::WARNING,
                "Wire under the shank",
                &format!(
                    "Your Ø{} wire is under the Ø{} shank: the shank is upset back up by {} % in area in the die.",
                    f(dw, 2),
                    f(shank, 2),
                    f((shank * shank / (dw * dw) - 1.0) * 100.0, 0)
                ),
                "Check the die can fill the shank, or choose a wire at the shank size.",
            ));
        }
    }
    if dw < blank_dia - 0.05 {
        out.push(PokaYokeIssue::new(
            PokaYokeSeverity::WARNING,
            "Wire under the rolling blank",
            &format!("Ø{} is under the Ø{} thread rolling blank: the thread section has to be upset back up, or the thread rolls undersize.", f(dw, 2), f(blank_dia, 3)),
            "Choose a wire at or above the rolling blank.",
        ));
    }
    let reduction = (1.0 - (blank_dia / dw) * (blank_dia / dw)) * 100.0;
    if part.thread_length_mm > 0.0 && reduction > open_limit && reduction <= heading::TRAPPED_EXTRUSION_MAX_PCT {
        out.push(PokaYokeIssue::new(
            PokaYokeSeverity::WARNING,
            "Large extrusion from this wire",
            &format!(
                "Extruding Ø{} down to the Ø{} blank removes {} % of area, over your {} % open-extrusion limit.",
                f(dw, 2),
                f(blank_dia, 3),
                f(reduction, 0),
                f(open_limit, 0)
            ),
            "Extrude in a trapped die, or choose a thinner wire.",
        ));
    }
    out
}

/// Hardness window and temper guidance for a tensile band taken from the drawing.
pub fn heat_treat_for_range(ht: &HeatTreatmentSpec, material: &MaterialInfo, r: &TensileRange) -> HeatTreatmentSpec {
    let lo = hardness_conversion::hrc_for(r.min_mpa);
    let hi = hardness_conversion::hrc_for(r.max_mpa);
    let aim_lo = lo + (hi - lo) / 3.0;
    let aim_hi = hi - (hi - lo) / 3.0;
    let h = |v: f64| jfmt::f(v, 0);
    let (mn, mx) = (r.min_mpa as i32, r.max_mpa as i32);
    let range_text = format!("{}–{} HRC for {}–{} MPa; aim {}–{} HRC", h(lo), h(hi), mn, mx, h(aim_lo), h(aim_hi));
    let mut out = ht.clone();
    if poka_yoke::material_family(material) != MaterialFamily::CARBON_ALLOY_STEEL {
        out.target_uts_mpa = format!("{}–{} MPa (drawing)", mn, mx);
        out.target_hardness_hrc = format!("{} (check against {})", ht.target_hardness_hrc, range_text);
        return out;
    }
    let start = temper_guide::start_temp_c(&material.key, (aim_lo + aim_hi) / 2.0);
    out.tempering_or_aging_temp_c = match start {
        Some(s) => format!(
            "Trial temper {}–{}°C (typical {} response for {}–{} HRC); adjust after the first hardness and tensile check",
            round_to_int(s - 15.0),
            round_to_int(s + 15.0),
            material.key,
            h(aim_lo),
            h(aim_hi)
        ),
        None => format!(
            "Temper by trial to {}–{} HRC: a higher temperature lowers the strength ({} is the class cycle)",
            h(aim_lo),
            h(aim_hi),
            ht.tempering_or_aging_temp_c
        ),
    };
    out.target_hardness_hrc = range_text;
    out.target_uts_mpa = format!("{}–{} MPa (drawing)", mn, mx);
    out
}

/// Titanium, nickel and other aerospace superalloys are headed warm; aerospace alloy steels above ~M16.
pub fn is_warm_headed(material: &MaterialInfo, d_mm: f64) -> bool {
    if !material.is_aero {
        return false;
    }
    match poka_yoke::material_family(material) {
        MaterialFamily::CARBON_ALLOY_STEEL => d_mm > 16.0,
        MaterialFamily::ALUMINUM | MaterialFamily::COPPER_ALLOY => false,
        _ => true,
    }
}

#[allow(clippy::too_many_arguments)]
fn hts(
    aust: &str, soak: String, quench: &str, temper: &str, temper_time: &str, hardness: &str, bake_req: bool, bake_cycle: &str, std_ref: &str,
    preheat: &str, preheat_soak: String, atmosphere: &str, quench_details: &str, cooling: &str, target_uts: &str, double: bool, aging: bool,
) -> HeatTreatmentSpec {
    HeatTreatmentSpec {
        austenitizing_or_solution_temp_c: aust.into(),
        soaking_time_minutes: soak,
        quench_medium: quench.into(),
        tempering_or_aging_temp_c: temper.into(),
        tempering_or_aging_time_hours: temper_time.into(),
        target_hardness_hrc: hardness.into(),
        hydrogen_baking_required: bake_req,
        hydrogen_baking_cycle: bake_cycle.into(),
        standard_reference: std_ref.into(),
        preheat_temp_c: preheat.into(),
        preheat_soak_time: preheat_soak,
        atmosphere: atmosphere.into(),
        quench_details: quench_details.into(),
        cooling_method: cooling.into(),
        target_uts_mpa: target_uts.into(),
        baking_temp_c: "N/A".into(),
        baking_soak_time: "N/A".into(),
        is_double_temper: double,
        is_aging: aging,
    }
}

fn if_blank(s: &str, d: &str) -> String {
    if s.trim().is_empty() { d.to_string() } else { s.to_string() }
}

pub fn calculate_dynamic_heat_treatment(material: &MaterialInfo, d: f64, has_he_risk: bool, property_class: Option<PropertyClass>) -> HeatTreatmentSpec {
    use PropertyClass as PC;
    let key = material.key.to_uppercase();
    let base = &material.heat_treatment_spec;
    let bake_cycle = "190°C – 220°C for 8 to 24 hrs, start within 4 hrs of plating (ISO 4042 / ASTM F1941), verify per ASTM F1940";
    let mut base_with_bake = base.clone();
    base_with_bake.hydrogen_baking_required = has_he_risk;
    if has_he_risk {
        base_with_bake.hydrogen_baking_cycle = bake_cycle.into();
    }
    let is_steel_class = matches!(
        property_class,
        Some(PC::CLASS_4_6 | PC::CLASS_4_8 | PC::CLASS_5_6 | PC::CLASS_5_8 | PC::CLASS_6_8 | PC::CLASS_8_8 | PC::CLASS_9_8 | PC::CLASS_10_9 | PC::CLASS_12_9)
    );
    let family = poka_yoke::material_family(material);

    if let Some(pc) = property_class.filter(|pc| COLD_FORMED_CLASSES.contains(pc) && family == MaterialFamily::CARBON_ALLOY_STEEL) {
        let mut o = base_with_bake;
        o.austenitizing_or_solution_temp_c = format!("N/A — cold formed, no hardening (ISO 898-1 {})", substring_before(pc.label(), " ("));
        o.soaking_time_minutes = "N/A".into();
        o.quench_medium = "N/A".into();
        o.tempering_or_aging_temp_c = "Optional stress relief 520°C – 560°C after heading".into();
        o.tempering_or_aging_time_hours = "60–90 mins if stress relieved".into();
        o.target_hardness_hrc = match pc {
            PC::CLASS_4_6 => "67–95 HRB (120–220 HV)",
            PC::CLASS_4_8 => "71–95 HRB (130–220 HV)",
            PC::CLASS_5_6 => "79–95 HRB (155–220 HV)",
            PC::CLASS_6_8 => "89–99.5 HRB (190–250 HV)",
            _ => "82–95 HRB (160–220 HV)",
        }
        .into();
        o.standard_reference = "ISO 898-1".into();
        o.is_double_temper = false;
        o.is_aging = false;
        return o;
    }
    let k = key.as_str();
    if !QT_STEELS.contains(&k)
        && !k.contains("TI")
        && !k.contains("718")
        && !k.contains("WASP")
        && k != "A286"
        && k != "SS204"
        && k != "SS304"
        && k != "SS316"
        && !k.contains("15_5")
        && !k.contains("17_4")
    {
        return base_with_bake;
    }
    if k == "TI_GR1" || k == "TI_6242" || k == "TI_3AL25V" || k == "TI_BETA_C" {
        return base_with_bake;
    }
    let d1 = dstr(round(d, 1));
    if k == "17_4PH" || k.contains("15_5") || k.contains("17_4") || k.contains("15-5") || k.contains("17-4") {
        let sol_soak = 45.max(round_to_int(30.0 + 1.5 * d));
        return hts(
            "1038°C ± 10°C (Solution Condition A)",
            format!("{} mins soak (for Ø{}mm)", sol_soak, d1),
            "Rapid Air Cool or Agitated Oil to <32°C",
            "H900: 482°C | H1025: 552°C",
            "H900: 60 mins (1 hr) soak | H1025: 240 mins (4 hrs) soak",
            "40–45 HRC (H900) / 35–40 HRC (H1025)",
            false,
            "Not required for condition H900/H1025",
            "AMS 5643 / ASTM A564",
            "550°C (Pre-Heating / Stress Relief)",
            format!("{} mins", 30.max(round_to_int(20.0 + 1.0 * d))),
            "Inert Argon or High Vacuum (<10⁻³ mbar)",
            "Agitated oil or forced air quench below 32°C to ensure 100% martensite transformation",
            "Still air cool to ambient",
            "1310 MPa (H900) / 1070 MPa (H1025)",
            false,
            true,
        );
    }
    if k == "A286" {
        return hts(
            "982 °C (1800 °F), AMS 5731 / 5732 — or 899 °C (1650 °F), AMS 5737, for better ductility and room-temperature strength",
            "About 1 h at temperature".into(),
            "Oil or water quench",
            "Age 718 °C (1325 °F) 16 h, air cool (AMS window 704 – 760 °C, at least 16 h)",
            "16 h",
            "About 24 – 37 HRC aged (248 – 341 HB; sources differ — confirm the AMS acceptance value)",
            false,
            "Not susceptible to hydrogen embrittlement",
            "AMS 5731 / AMS 5732 / AMS 5737",
            "",
            String::new(),
            "Vacuum, argon or protective atmosphere",
            "Oil or water quench from the solution temperature",
            "Air cool after ageing",
            "900 – 1050 MPa (AMS 5732 ≈ 130 ksi min; AMS 5737 ≈ 140 ksi min)",
            false,
            true,
        );
    }
    if k == "TI_GR5" || k.contains("TI") {
        return hts(
            "913°C – 954°C (1675 – 1750°F) Solution Treatment, below the β-transus",
            "60 mins soak (ATI 6-4 / AMS 4965)".into(),
            "Water Quench — minimum delay from furnace to water",
            "524°C – 552°C (975 – 1025°F) Aging",
            "4 – 8 Hours, air cool",
            "36–40 HRC",
            false,
            "Vacuum degassing at 700°C if hydrogen pick-up exceeds 125 ppm",
            "AMS 4965 (STA) / AMS 2801",
            "600°C (Induction Pre-heat in Vacuum)",
            "30 mins".into(),
            "High Vacuum (<10⁻⁴ mbar) to avoid alpha-case embrittlement",
            "Water quench with minimum delay: STA strength depends on a fast quench; large sections or a slow transfer give lower properties (ATI)",
            "Air cool after aging",
            "1100–1200 MPa",
            false,
            true,
        );
    }
    if k == "INCONEL718" || k.contains("718") {
        return hts(
            "927 – 1010 °C (1700 – 1850 °F), 955 – 980 °C typical (Solution treatment, AMS 5662)",
            "Time commensurate with section size (AMS 5662)".into(),
            "Rapid cool: water, or fast gas / air (air cool or faster). Never furnace cool",
            "Age 718 °C (1325 °F) 8 h → furnace cool 55 °C/h to 621 °C (1150 °F) → hold to 18 h total ageing → air cool (AMS 5663)",
            "18 h total: 8 h at 718 °C + ≈ 1.8 h furnace cool + ≈ 8.2 h at 621 °C",
            "36 – 43 HRC aged (≤ 28 HRC annealed)",
            false,
            "Not applicable to nickel superalloys",
            "AMS 5662 / AMS 5663 / AMS 2774",
            "",
            String::new(),
            "Vacuum or argon",
            "Cool fast from the solution temperature: slow cooling precipitates δ / γ″ before the age and weakens the ageing response",
            "Furnace cool 55 °C/h (100 °F/h) from 718 to 621 °C; air cool after the 621 °C hold",
            "1380–1500 MPa",
            true,
            true,
        );
    }
    if k == "WASPALOY" || k.contains("WASP") {
        return hts(
            "996 – 1038 °C (1825 – 1900 °F), 1010 °C typical (Solution treatment)",
            "240 mins (4 hrs) soak (Special Metals heat treatment B; Haynes uses 2 h, Upmet 1 h)".into(),
            "Oil quench (or fast gas / water)",
            "Stabilize 845 °C (1550 °F) 4 h, air cool + Age 760 °C (1400 °F) 16 h, air cool",
            "4 h stabilize + 16 h age",
            "34 – 44 HRC",
            false,
            "Not susceptible to hydrogen embrittlement",
            "Special Metals heat treatment B / AMS 5706 – 5709",
            "",
            String::new(),
            "Vacuum or argon",
            "Heat through 650 – 870 °C fast (strain-age cracking) and quench fast from the solution temperature",
            "Air cool after the 845 °C stabilize; air cool after the 760 °C age",
            "1250–1350 MPa",
            true,
            true,
        );
    }
    if k == "SS204" || k == "SS304" || k == "SS316" {
        let sol_soak = 30.max(round_to_int(20.0 + 1.5 * d));
        return hts(
            "1050°C – 1080°C (Solution Annealing)",
            format!("{} mins soak (for Ø{}mm)", sol_soak, d1),
            "Rapid Water Quench to <400°C in <30 seconds",
            "N/A (Non-Hardenable by Heat Treatment)",
            "N/A (Work-hardening alloy)",
            "75–95 HRB (Annealed) / 25–32 HRC (Cold-Worked)",
            false,
            "Not required for austenitic grades",
            "ASTM A193 / ISO 3506-1",
            "500°C – 550°C",
            "30 mins".into(),
            "Inert Argon or Vacuum",
            "Continuous agitated water bath to avoid Cr23C6 carbide precipitation",
            "Rapid water quench",
            "700–850 MPa",
            false,
            false,
        );
    }
    // Carbon / boron / alloy steels
    let aust_soak_h = heat_treat_cycle::soak_hours(d);
    let temper_h = heat_treat_cycle::temper_hours(d);
    let preheat_soak = 30.max(round_to_int(15.0 + 1.2 * d));
    let is_double = k == "4340" || k.contains("30KH") || k.contains("GSNA");
    let temp_soak_text = if is_double {
        format!("Double Temper: 2 × {} hrs soak (AMS 2759/1: 2 h min + 1 h per 25 mm over 25 mm)", dstr(round(temper_h, 1)))
    } else {
        format!("{} hrs soak for Ø{}mm (AMS 2759/1: 2 h min + 1 h per 25 mm over 25 mm)", dstr(round(temper_h, 1)), d1)
    };
    let aust_temp = match k {
        "10B21" => "860°C – 880°C".to_string(),
        "4135" => "850°C – 870°C".into(),
        "4140" => "840°C – 860°C".into(),
        "4340" => "830°C – 850°C".into(),
        "SS410" => "980°C – 1020°C".into(),
        _ => if_blank(&base.austenitizing_or_solution_temp_c, "860°C"),
    };
    let is_boron = k.contains("B21") || k.contains("51B37");
    let temp_temp = if k == "SS410" {
        "600°C – 650°C (Tempering)".to_string()
    } else if property_class == Some(PC::CLASS_8_8) {
        if is_boron { "480°C – 540°C (Tempering, ≥ 425°C ISO 898-1)" } else { "560°C – 620°C (Tempering, ≥ 425°C ISO 898-1)" }.into()
    } else if property_class == Some(PC::CLASS_9_8) {
        "≥ 425°C (Tempering, ISO 898-1 minimum)".into()
    } else if property_class == Some(PC::CLASS_10_9) {
        if is_boron { "425°C – 460°C (Tempering, ≥ 425°C ISO 898-1)" } else { "500°C – 560°C (Tempering, ≥ 425°C ISO 898-1)" }.into()
    } else if property_class == Some(PC::CLASS_12_9) {
        "440°C – 500°C (Tempering, ≥ 425°C ISO 898-1)".into()
    } else if k == "4340" {
        "480°C – 540°C (Double Tempering)".into()
    } else {
        if_blank(&base.tempering_or_aging_temp_c, "≥ 425°C")
    };
    let quench_med = match k {
        "10B21" => "Polymer Quench (8–12%) or Fast Oil @ 40–60°C".to_string(),
        "4135" | "4140" | "4340" => "Accelerated Quench Oil @ 50–70°C (Agitation 0.8 m/s)".into(),
        "SS410" => "Oil Quench or Air Cool from 1000°C".into(),
        _ => if_blank(&base.quench_medium, "Accelerated Quench Oil @ 50–70°C"),
    };
    let hardness = if k == "SS410" {
        "36–42 HRC".to_string()
    } else {
        match property_class {
            Some(PC::CLASS_8_8) => if d <= 16.0 { "22–32 HRC (Class 8.8, d ≤ 16)" } else { "23–34 HRC (Class 8.8, d > 16)" }.into(),
            Some(PC::CLASS_9_8) => "28–37 HRC (Class 9.8, d ≤ 16)".into(),
            Some(PC::CLASS_10_9) => "32–39 HRC (Class 10.9)".into(),
            Some(PC::CLASS_12_9) => "39–44 HRC (Class 12.9)".into(),
            Some(PC::SAE_GRADE_5) => if d <= 25.4 { "25–34 HRC (SAE Grade 5, ≤ 1\")" } else { "19–30 HRC (SAE Grade 5, > 1\")" }.into(),
            Some(PC::SAE_GRADE_8) => "33–39 HRC (SAE Grade 8)".into(),
            Some(PC::ASTM_A574) => if d <= 12.7 { "39–45 HRC (ASTM A574, ≤ 1/2\")" } else { "37–45 HRC (ASTM A574, > 1/2\")" }.into(),
            Some(PC::ASTM_A193_B7) => "35 HRC max (ASTM A193 B7)".into(),
            _ => if_blank(&base.target_hardness_hrc, "32–39 HRC"),
        }
    };
    let carbon_potential = if k.contains("B21") {
        "0.20%–0.25%"
    } else if k == "4135" || k.contains("51B37") || k.contains("30KH") || k == "AISI_4130" {
        "0.30%–0.38%"
    } else if k == "16KHGSN" {
        "0.14%–0.20%"
    } else {
        "0.38%–0.43%"
    };
    let std_ref = if k.contains("4340") || k.contains("30KH") {
        "AMS 2759/1 & AMS 2759/2".to_string()
    } else if k.contains("4140") || k.contains("4135") {
        "ISO 898-1 / AMS 2759/1".into()
    } else if k.contains("10B21") {
        "ISO 898-1 Class 10.9 / SAE J429".into()
    } else {
        if_blank(&base.standard_reference, "ISO 898-1 / AMS 2759")
    };
    let target_uts = if is_steel_class {
        let pc = property_class.unwrap();
        format!("≥ {} MPa (ISO 898-1 {})", poka_yoke::class_strength(pc, d, material).rm_min_mpa as i32, substring_before(pc.label(), " ("))
    } else {
        format!("{} MPa", material.post_forging_target_uts_mpa as i32)
    };
    HeatTreatmentSpec {
        austenitizing_or_solution_temp_c: aust_temp,
        soaking_time_minutes: format!("{} hrs soak for Ø{}mm (1 h per 25 mm, min 1 h, from the load thermocouple)", dstr(round(aust_soak_h, 1)), d1),
        quench_medium: quench_med,
        tempering_or_aging_temp_c: temp_temp,
        tempering_or_aging_time_hours: temp_soak_text,
        target_hardness_hrc: hardness,
        hydrogen_baking_required: has_he_risk,
        hydrogen_baking_cycle: if has_he_risk { bake_cycle.into() } else { "Not required (no electroplating, or strength below 1000 MPa)".into() },
        standard_reference: std_ref,
        preheat_temp_c: "600°C – 650°C (Pre-Heating / Stress Relief)".into(),
        preheat_soak_time: format!("{} mins", preheat_soak),
        atmosphere: format!("Endothermic gas, neutral C-potential {} (no carburizing / decarburizing per ISO 898-1)", carbon_potential),
        quench_details: "Bath temp 50–70°C with 0.8 m/s agitation, transfer time < 10s to prevent pearlite nose interception".into(),
        cooling_method: "Still air cooling to ambient room temperature".into(),
        target_uts_mpa: target_uts,
        baking_temp_c: "190°C – 220°C".into(),
        baking_soak_time: "8 to 24 Hours mandatory soak".into(),
        is_double_temper: is_double,
        is_aging: false,
    }
}

pub fn evaluate_shop_gauge(measured: f64, target: f64, upper: f64, lower: f64, tol_half: f64) -> GaugeStatus {
    let warn_zone = tol_half * 0.65;
    if measured > upper {
        GaugeStatus::SCRAP_GO_BINDS
    } else if measured < lower {
        GaugeStatus::SCRAP_NO_GO_FAILS
    } else if measured - target > warn_zone {
        GaugeStatus::TIGHT_HIGH
    } else if target - measured > warn_zone {
        GaugeStatus::LOOSE_LOW
    } else {
        GaugeStatus::IDEAL_PASS
    }
}

/// Minimum elongation A and reduction of area Z (%) the property class itself requires.
pub fn class_ductility(pc: Option<PropertyClass>) -> Option<(f64, f64)> {
    use PropertyClass as PC;
    Some(match pc? {
        PC::CLASS_4_6 => (22.0, 0.0),
        PC::CLASS_5_6 => (20.0, 0.0),
        PC::CLASS_4_8 | PC::CLASS_5_8 | PC::CLASS_6_8 => (0.0, 0.0),
        PC::CLASS_8_8 => (12.0, 52.0),
        PC::CLASS_9_8 => (10.0, 48.0),
        PC::CLASS_10_9 => (9.0, 48.0),
        PC::CLASS_12_9 => (8.0, 44.0),
        PC::SAE_GRADE_2 => (18.0, 35.0),
        PC::SAE_GRADE_5 => (14.0, 35.0),
        PC::SAE_GRADE_8 => (12.0, 35.0),
        PC::ASTM_A193_B7 => (16.0, 50.0),
        _ => return None,
    })
}
