//! Ports of TensileRange.kt (TensileRange, HardnessConversion, TemperGuide), MillCert.kt and JointGrip.kt.

use crate::catalog::MaterialInfo;
use crate::gen_enums::{FastenerType, GripStatus, MaterialFamily, PokaYokeSeverity, UnitSystem, WireVerdict};
use crate::jfmt;
use crate::part_geometry::FinishedPart;
use crate::poka_yoke::{self, ClassStrength, PokaYokeIssue};
use crate::soft_anneal;
use serde::{Deserialize, Serialize};

/// A tensile strength band from the customer drawing (e.g. "1000–1240 MPa") instead of a property class.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TensileRange {
    pub min_mpa: f64,
    pub max_mpa: f64,
}

/// Every run of digits, with a decimal part when a '.' is followed by digits (`\d+(\.\d+)?`).
pub(crate) fn find_numbers(t: &str) -> Vec<f64> {
    let c: Vec<char> = t.chars().collect();
    let mut out = Vec::new();
    let mut i = 0;
    while i < c.len() {
        if c[i].is_ascii_digit() {
            let start = i;
            while i < c.len() && c[i].is_ascii_digit() {
                i += 1;
            }
            if i + 1 < c.len() && c[i] == '.' && c[i + 1].is_ascii_digit() {
                i += 1;
                while i < c.len() && c[i].is_ascii_digit() {
                    i += 1;
                }
            }
            let s: String = c[start..i].iter().collect();
            out.push(s.parse().unwrap());
        } else {
            i += 1;
        }
    }
    out
}

impl TensileRange {
    pub fn label(&self) -> String {
        format!("Rm {}–{} MPa (drawing)", self.min_mpa as i32, self.max_mpa as i32)
    }

    /// Q&T alloy steel ratios as in ISO 898-1 class 10.9: Rp0.2 ≈ 0.9 Rm, proof stress ≈ 0.8 Rm.
    pub fn strength(&self) -> ClassStrength {
        ClassStrength { rm_min_mpa: self.min_mpa, yield_min_mpa: 0.9 * self.min_mpa, proof_stress_mpa: 0.8 * self.min_mpa, is_iso_steel_class: false }
    }

    pub fn of(min: Option<f64>, max: Option<f64>) -> Option<TensileRange> {
        let min = min?;
        if min <= 0.0 {
            return None;
        }
        let hi = max.filter(|&m| m >= min).unwrap_or(min);
        Some(TensileRange { min_mpa: min, max_mpa: hi })
    }

    /// Reads "1000-1240 MPa", "160-180 KSI", "TENSILE STRENGTH = 1000-1240MPa" or a single minimum.
    pub fn parse(text: &str) -> Option<TensileRange> {
        let t = text.to_uppercase().replace(',', ".");
        let ksi = t.contains("KSI");
        if !ksi && !t.contains("MPA") && !t.contains("N/MM") {
            return None;
        }
        let nums: Vec<f64> = find_numbers(&t)
            .into_iter()
            .filter(|&v| if ksi { (40.0..=400.0).contains(&v) } else { (300.0..=2500.0).contains(&v) })
            .collect();
        if nums.is_empty() {
            return None;
        }
        let f = if ksi { 6.894757 } else { 1.0 };
        Self::of(Some(nums[0] * f), nums.get(1).map(|v| v * f))
    }
}

/// Steel hardness for a tensile strength, from the ISO 898-1 class pairs (8.8 → 12.9) extended by ISO 18265.
pub mod hardness_conversion {
    const POINTS: [(f64, f64); 6] = [(800.0, 22.0), (1040.0, 32.0), (1220.0, 39.0), (1420.0, 44.0), (1650.0, 48.0), (1950.0, 53.0)];

    pub fn hrc_for(rm_mpa: f64) -> f64 {
        if rm_mpa <= POINTS[0].0 {
            return POINTS[0].1;
        }
        for w in POINTS.windows(2) {
            let (a, b) = (w[0], w[1]);
            if rm_mpa <= b.0 {
                return a.1 + (rm_mpa - a.0) / (b.0 - a.0) * (b.1 - a.1);
            }
        }
        POINTS[POINTS.len() - 1].1
    }
}

/// Typical tempering response of oil-quenched alloy steels: a starting temperature for the first trial temper.
pub mod temper_guide {
    const C4340: [(f64, f64); 5] = [(205.0, 53.0), (315.0, 49.5), (425.0, 46.0), (540.0, 39.0), (650.0, 31.0)];

    pub fn start_temp_c(material_key: &str, target_hrc: f64) -> Option<f64> {
        let c: &[(f64, f64)] = match material_key.to_uppercase().as_str() {
            "4340" => &C4340,
            _ => return None,
        };
        if target_hrc >= c[0].1 {
            return Some(c[0].0);
        }
        if target_hrc <= c[c.len() - 1].1 {
            return Some(c[c.len() - 1].0);
        }
        for w in c.windows(2) {
            let (a, b) = (w[0], w[1]);
            if target_hrc <= a.1 && target_hrc >= b.1 {
                return Some(a.0 + (a.1 - target_hrc) / (a.1 - b.1) * (b.0 - a.0));
            }
        }
        None
    }
}

// ---------------------------------------------------------------- MillCert.kt

/// Raw wire as tested on the supplier's mill certificate. A null field was not entered.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MillCert {
    pub rm_mpa: Option<f64>,
    pub hrb: Option<f64>,
}

impl MillCert {
    pub fn is_empty(&self) -> bool {
        self.rm_mpa.is_none() && self.hrb.is_none()
    }

    pub fn of(rm_mpa: Option<f64>, hrb: Option<f64>) -> Option<MillCert> {
        let c = MillCert {
            rm_mpa: rm_mpa.filter(|v| (100.0..=3000.0).contains(v)),
            hrb: hrb.filter(|v| (20.0..=120.0).contains(v)),
        };
        if c.is_empty() { None } else { Some(c) }
    }
}

/// The certificate compared with the catalogue's headable (annealed) condition for the grade.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WireCheck {
    pub verdict: WireVerdict,
    pub reference: String,
    pub message: String,
    pub recess_head: bool,
}

pub mod mill_cert_check {
    use super::*;

    fn f(v: f64) -> String {
        jfmt::f(v, 0)
    }

    /// True for parts whose head carries a back-extruded socket or recess.
    pub fn has_recess_head(t: FastenerType, part: Option<&FinishedPart>) -> bool {
        part.map(|p| p.recesses.iter().any(|r| r.from_top)).unwrap_or(false)
            || t == FastenerType::SHCS
            || t == FastenerType::SOCKET_BUTTON
            || t == FastenerType::SOCKET_CSK
    }

    pub fn check(material: &MaterialInfo, t: FastenerType, part: Option<&FinishedPart>, cert: Option<&MillCert>) -> Option<WireCheck> {
        let cert = cert?;
        if cert.is_empty() {
            return None;
        }
        let recess = has_recess_head(t, part);
        let supplied = &material.raw_material_sourcing_uts_mpa;
        let family = poka_yoke::material_family(material);
        let is_steel = family == MaterialFamily::CARBON_ALLOY_STEEL || family == MaterialFamily::STAINLESS;
        let up = supplied.to_uppercase();
        let hard_catalogue = up.contains("COLD DRAWN") || up.contains("HALF-HARD") || up.contains("HALF HARD");
        let range = soft_anneal::parse_uts_range(supplied);

        if range.is_none() || hard_catalogue {
            let why = if hard_catalogue {
                format!("The catalogue lists {} wire in the hard (cold drawn / half-hard) condition, which is not a headable condition, so there is no headable range to compare with.", material.key)
            } else {
                format!("The catalogue has no raw-wire Rm for {} (\"{}\").", material.key, supplied)
            };
            return Some(WireCheck {
                verdict: WireVerdict::NO_REFERENCE,
                reference: supplied.clone(),
                message: format!("{} Confirm the condition with the wire supplier (see the Soft Anneal card).", why),
                recess_head: recess,
            });
        }
        let (lo, hi) = range.unwrap();
        let hrb_lo = if is_steel { soft_anneal::hb_to_hrb(lo / soft_anneal::HB_PER_MPA) } else { None };
        let hrb_hi = if is_steel { soft_anneal::hb_to_hrb(hi / soft_anneal::HB_PER_MPA) } else { None };

        let rm_hard = cert.rm_mpa.map_or(false, |v| v > hi);
        let hrb_hard = matches!((cert.hrb, hrb_hi), (Some(h), Some(hh)) if h > hh);
        let rm_soft = cert.rm_mpa.map_or(false, |v| v < lo);
        let hrb_soft = matches!((cert.hrb, hrb_lo), (Some(h), Some(hl)) if h < hl);
        let ref_text = format!("{} – {} MPa", f(lo), f(hi))
            + &match (hrb_lo, hrb_hi) {
                (Some(a), Some(b)) => format!(" (≈ {} – {} HRB, derived)", f(a), f(b)),
                _ => String::new(),
            };
        let given = [cert.rm_mpa.map(|v| format!("Rm {} MPa", f(v))), cert.hrb.map(|v| format!("{} HRB", f(v)))]
            .into_iter()
            .flatten()
            .collect::<Vec<_>>()
            .join(", ");
        let hrb_ignored = if cert.hrb.is_some() && hrb_hi.is_none() {
            " The HRB value is not compared: the HRB band is derived for steels only."
        } else {
            ""
        };
        let key = &material.key;
        Some(if rm_hard || hrb_hard {
            WireCheck {
                verdict: WireVerdict::TOO_HARD,
                reference: ref_text.clone(),
                message: format!("Mill cert {given} is above the catalogue's headable range for {key} ({ref_text}): the wire is not in the annealed condition the heading plan assumes.")
                    + if recess { " The socket / recess is back-extruded, the most severe heading operation: hard wire risks punch breakage and cracks at the socket corners." } else { "" }
                    + hrb_ignored,
                recess_head: recess,
            }
        } else if rm_soft || hrb_soft {
            WireCheck {
                verdict: WireVerdict::SOFTER_THAN_RANGE,
                reference: ref_text.clone(),
                message: format!("Mill cert {given} is below the catalogue range for {key} ({ref_text}). Softer wire heads more easily; the final strength comes from the heat treatment, so check the grade is right on the certificate.{hrb_ignored}"),
                recess_head: recess,
            }
        } else {
            WireCheck {
                verdict: WireVerdict::HEADABLE,
                reference: ref_text.clone(),
                message: format!("Mill cert {given} is within the catalogue's headable range for {key} ({ref_text}).")
                    + if recess { " Socket / recess heading: OK on raw-wire strength." } else { "" }
                    + hrb_ignored,
                recess_head: recess,
            }
        })
    }

    pub fn issues(c: Option<&WireCheck>) -> Vec<PokaYokeIssue> {
        match c.map(|c| c.verdict) {
            Some(WireVerdict::TOO_HARD) => {
                let c = c.unwrap();
                vec![PokaYokeIssue::new(
                    if c.recess_head { PokaYokeSeverity::MANDATORY } else { PokaYokeSeverity::WARNING },
                    if c.recess_head { "Wire too hard for the socket head" } else { "Wire harder than the headable range" },
                    &c.message,
                    &format!("Spheroidize / soft anneal the wire before heading (cycle on the Soft Anneal card), or source annealed wire with Rm in {}.", c.reference),
                )]
            }
            Some(WireVerdict::NO_REFERENCE) => {
                let c = c.unwrap();
                vec![PokaYokeIssue::new(PokaYokeSeverity::WARNING, "Mill cert not checked against a range", &c.message, "Get the headable Rm / hardness for this grade from the wire supplier.")]
            }
            _ => vec![],
        }
    }
}

// ---------------------------------------------------------------- JointGrip.kt

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GripCheck {
    pub status: GripStatus,
    pub grip_min_mm: f64,
    pub grip_max_mm: f64,
    pub stack_mm: f64,
    pub suggested_length_mm: f64,
    pub step_mm: f64,
}

impl GripCheck {
    pub fn threads_in_joint_mm(&self) -> f64 {
        (self.stack_mm - self.grip_min_mm).max(0.0)
    }
    pub fn excess_mm(&self) -> f64 {
        (self.grip_min_mm - self.stack_mm).max(0.0)
    }
}

pub mod joint_grip {
    use super::*;

    /// AN / NAS / Hi-Lok grips step in 1/16"; metric aerospace grips are taken in 1 mm steps.
    pub fn step_mm(unit: UnitSystem) -> f64 {
        if unit == UnitSystem::INCH { 25.4 / 16.0 } else { 1.0 }
    }

    pub fn applies_to(t: FastenerType) -> bool {
        poka_yoke::is_aerospace_type(t) && !t.is_nut() && !t.is_washer() && !t.is_unthreaded() && !t.is_stud()
    }

    pub fn check(unit: UnitSystem, length_mm: f64, thread_length_mm: f64, runout_mm: f64, stack_mm: f64) -> GripCheck {
        let step = step_mm(unit);
        let grip_max = (length_mm - thread_length_mm).max(0.0);
        let grip_min = (grip_max - runout_mm).max(0.0);
        let eps = 1e-6;
        let suggested = (stack_mm / step - eps).ceil().max(0.0) * step + thread_length_mm + runout_mm;
        let status = if grip_min + eps < stack_mm {
            GripStatus::THREADS_IN_JOINT
        } else if grip_min - stack_mm > step + eps {
            GripStatus::EXCESS_GRIP
        } else {
            GripStatus::OK
        };
        GripCheck { status, grip_min_mm: grip_min, grip_max_mm: grip_max, stack_mm, suggested_length_mm: suggested, step_mm: step }
    }
}
