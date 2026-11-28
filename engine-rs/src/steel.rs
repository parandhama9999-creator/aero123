//! Port of SteelComposition.kt: nominal composition of the headable steels and the A1 it implies.

use crate::catalog::MaterialInfo;
use serde::{Deserialize, Serialize};

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
pub struct SteelComposition {
    pub carbon: f64,
    pub manganese: f64,
    pub silicon: f64,
    pub chromium: f64,
    pub nickel: f64,
    pub molybdenum: f64,
}

const fn c(carbon: f64, manganese: f64, silicon: f64, chromium: f64, nickel: f64, molybdenum: f64) -> SteelComposition {
    SteelComposition { carbon, manganese, silicon, chromium, nickel, molybdenum }
}

/// A1 for a plain carbon steel, the fallback when a grade's composition is not on file.
pub const A1_PLAIN_CARBON_C: f64 = 723.0;
/// Carbon below this leaves nothing to spheroidise (maraging steel at about 0.02 %).
pub const MIN_CARBON_TO_SPHEROIDISE: f64 = 0.05;

/// Mid-range compositions by catalogue key (upper case), in the Kotlin table's order.
pub static BY_KEY: &[(&str, SteelComposition)] = &[
    ("1008", c(0.08, 0.4, 0.08, 0.0, 0.0, 0.0)),
    ("1018", c(0.18, 0.75, 0.2, 0.0, 0.0, 0.0)),
    ("1022", c(0.22, 0.85, 0.2, 0.0, 0.0, 0.0)),
    ("1035", c(0.35, 0.75, 0.2, 0.0, 0.0, 0.0)),
    ("1045", c(0.45, 0.75, 0.2, 0.0, 0.0, 0.0)),
    ("EN8", c(0.4, 0.8, 0.2, 0.0, 0.0, 0.0)),
    ("10B21", c(0.21, 0.9, 0.2, 0.0, 0.0, 0.0)),
    ("10B33", c(0.33, 0.9, 0.2, 0.0, 0.0, 0.0)),
    ("20MNB4", c(0.2, 1.0, 0.2, 0.0, 0.0, 0.0)),
    ("23MNB4", c(0.23, 1.05, 0.2, 0.0, 0.0, 0.0)),
    ("30MNB4", c(0.3, 1.05, 0.2, 0.0, 0.0, 0.0)),
    ("32CRB4", c(0.32, 0.6, 0.2, 0.4, 0.0, 0.0)),
    ("36CRB4", c(0.36, 0.6, 0.2, 0.4, 0.0, 0.0)),
    ("41CR4", c(0.41, 0.75, 0.25, 1.05, 0.0, 0.0)),
    ("AISI_51B37", c(0.37, 0.9, 0.22, 0.85, 0.0, 0.0)),
    ("16MNCR5", c(0.16, 1.15, 0.25, 0.95, 0.0, 0.0)),
    ("AISI_4130", c(0.3, 0.5, 0.25, 0.95, 0.0, 0.2)),
    ("4135", c(0.35, 0.8, 0.25, 0.95, 0.0, 0.2)),
    ("SCM435", c(0.35, 0.75, 0.25, 1.05, 0.0, 0.2)),
    ("4140", c(0.4, 0.88, 0.25, 0.95, 0.0, 0.2)),
    ("SCM440", c(0.4, 0.75, 0.25, 1.05, 0.0, 0.22)),
    ("42CRMO4", c(0.42, 0.75, 0.25, 1.05, 0.0, 0.22)),
    ("34CRMO4", c(0.34, 0.7, 0.25, 1.05, 0.0, 0.22)),
    ("18CRMO4", c(0.18, 0.7, 0.25, 1.05, 0.0, 0.22)),
    ("AISI_4037", c(0.37, 0.8, 0.25, 0.0, 0.0, 0.25)),
    ("EN16", c(0.36, 1.5, 0.25, 0.0, 0.0, 0.27)),
    ("8740", c(0.4, 0.85, 0.25, 0.5, 0.55, 0.25)),
    ("4340", c(0.4, 0.7, 0.25, 0.8, 1.83, 0.25)),
    ("EN24", c(0.4, 0.65, 0.25, 1.1, 1.5, 0.28)),
    ("EN24_ESR", c(0.4, 0.65, 0.25, 1.1, 1.5, 0.28)),
    ("34CRNIMO6", c(0.34, 0.65, 0.25, 1.55, 1.55, 0.22)),
    ("35NCD16", c(0.35, 0.45, 0.25, 1.8, 3.85, 0.35)),
    ("300M", c(0.42, 0.75, 1.6, 0.8, 1.8, 0.4)),
    ("30KHGSA", c(0.3, 0.95, 1.05, 0.95, 0.0, 0.0)),
    ("16KHGSN", c(0.16, 1.0, 0.8, 0.85, 1.4, 0.0)),
    ("H11_H13", c(0.38, 0.35, 1.0, 5.1, 0.0, 1.35)),
    ("MARAGING_250", c(0.02, 0.08, 0.08, 0.0, 18.5, 4.8)),
];

/// Andrews (1965): Ac1 = 723 − 10.7·Mn − 16.9·Ni + 29.1·Si + 16.9·Cr.
pub fn ac1(c: &SteelComposition) -> f64 {
    723.0 - 10.7 * c.manganese - 16.9 * c.nickel + 29.1 * c.silicon + 16.9 * c.chromium
}

pub fn for_material(m: &MaterialInfo) -> Option<SteelComposition> {
    let k = m.key.to_uppercase();
    BY_KEY.iter().find(|(key, _)| *key == k).map(|(_, c)| *c)
}

/// Andrews is a low-alloy regression: not valid past about 5 % Ni or 6 % Cr.
pub fn andrews_applies(c: &SteelComposition) -> bool {
    c.nickel <= 5.0 && c.chromium <= 6.0
}

/// A1 for this grade, or None when its composition is not on file or is outside Andrews' range.
pub fn ac1_for(m: &MaterialInfo) -> Option<f64> {
    for_material(m).filter(andrews_applies).map(|c| ac1(&c))
}

/// Whether a grade has enough carbon for a spheroidize anneal to mean anything (unknown = yes).
pub fn can_spheroidise(m: &MaterialInfo) -> bool {
    for_material(m).map(|c| c.carbon >= MIN_CARBON_TO_SPHEROIDISE).unwrap_or(true)
}

/// Soak length from alloy content alone (grade factor).
pub fn soak_hours_for_grade(m: &MaterialInfo) -> (f64, f64) {
    let Some(c) = for_material(m) else { return (6.0, 8.0) };
    let carbide_formers = c.chromium + c.molybdenum;
    if carbide_formers >= 0.80 {
        (8.0, 10.0)
    } else if carbide_formers >= 0.30 || c.carbon >= 0.18 {
        (6.0, 8.0)
    } else {
        (5.0, 7.0)
    }
}

/// Soak length from wire diameter alone (thermal penetration).
pub fn soak_hours_for_diameter(wire_dia_mm: f64) -> (f64, f64) {
    if wire_dia_mm <= 4.0 {
        (6.0, 7.0)
    } else if wire_dia_mm <= 8.0 {
        (7.0, 9.0)
    } else if wire_dia_mm <= 16.0 {
        (9.0, 12.0)
    } else if wire_dia_mm <= 25.0 {
        (12.0, 16.0)
    } else {
        (16.0, 20.0)
    }
}

/// Combined soak: the longer of grade factor and diameter factor at each end (grade only when wire ≤ 0).
pub fn soak_hours(m: &MaterialInfo, wire_dia_mm: f64) -> (f64, f64) {
    let (g_lo, g_hi) = soak_hours_for_grade(m);
    if wire_dia_mm <= 0.0 {
        return (g_lo, g_hi);
    }
    let (d_lo, d_hi) = soak_hours_for_diameter(wire_dia_mm);
    (g_lo.max(d_lo), g_hi.max(d_hi))
}
