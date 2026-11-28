//! Ports of IsoThreadClass.kt and ThreadRollingBlank.kt.

use serde::{Deserialize, Serialize};

/// An ISO 965 tolerance class: pitch-diameter grade, crest grade, position ("g", "H", …), MJ profile flag.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IsoThreadClass {
    pub pitch_grade: i32,
    pub crest_grade: i32,
    pub position: String,
    pub is_mj: bool,
}

/// Matches `^(\d)([A-Za-z]{1,2})(?:(\d)([A-Za-z]{1,2}))?$`; returns (grade1, position1, grade2).
fn match_pattern(core: &str) -> Option<(i32, String, Option<i32>)> {
    let c: Vec<char> = core.chars().collect();
    let letters = |from: usize| -> usize {
        let mut n = 0;
        while from + n < c.len() && c[from + n].is_ascii_alphabetic() && n < 2 {
            n += 1;
        }
        n
    };
    if c.is_empty() || !c[0].is_ascii_digit() {
        return None;
    }
    let g1 = c[0].to_digit(10)? as i32;
    // the regex may backtrack between one and two letters; try the longest first
    for n1 in (1..=letters(1)).rev() {
        let pos1: String = c[1..1 + n1].iter().collect();
        let rest = 1 + n1;
        if rest == c.len() {
            return Some((g1, pos1, None));
        }
        if c[rest].is_ascii_digit() {
            let n2 = letters(rest + 1);
            for k in (1..=n2).rev() {
                if rest + 1 + k == c.len() {
                    return Some((g1, pos1, Some(c[rest].to_digit(10)? as i32)));
                }
            }
        }
    }
    None
}

impl IsoThreadClass {
    pub fn parse(code: &str) -> IsoThreadClass {
        let mj = code.to_lowercase().ends_with("-mj");
        let mut core = code;
        if let Some(s) = core.strip_suffix("-MJ") {
            core = s;
        }
        if let Some(s) = core.strip_suffix("-mj") {
            core = s;
        }
        let core = core.trim();
        match match_pattern(core) {
            None => IsoThreadClass {
                pitch_grade: 6,
                crest_grade: 6,
                position: if core.chars().any(|c| c.is_uppercase()) { "H".into() } else { "g".into() },
                is_mj: mj,
            },
            Some((g1, pos, g2)) => IsoThreadClass { pitch_grade: g1, crest_grade: g2.unwrap_or(g1), position: pos, is_mj: mj },
        }
    }

    /// ISO 965-1 tolerance-grade factors relative to grade 6 (Td2, TD2, TD1).
    pub fn grade_factor(grade: i32) -> f64 {
        match grade {
            3 => 0.50,
            4 => 0.63,
            5 => 0.80,
            6 => 1.00,
            7 => 1.25,
            8 => 1.60,
            9 => 2.00,
            _ => 1.00,
        }
    }

    /// Major-diameter tolerance Td exists only in grades 4, 6 and 8.
    pub fn major_grade_factor(grade: i32) -> f64 {
        if grade <= 4 {
            0.63
        } else if grade <= 6 {
            1.00
        } else {
            1.60
        }
    }

    /// Upper deviation es (mm) of an external thread; ISO 965-1 / 965-3, P in mm.
    pub fn external_es(position: &str, p: f64) -> f64 {
        match position {
            "d" => -(0.080 + 0.011 * p),
            "e" => -(0.050 + 0.011 * p),
            "f" => -(0.030 + 0.011 * p),
            "h" => 0.0,
            _ => -(0.015 + 0.011 * p),
        }
    }

    /// Lower deviation EI (mm) of an internal thread (G, H per ISO 965-1; AZ, AX per ISO 965-5).
    pub fn internal_ei(position: &str, p: f64) -> f64 {
        match position {
            "G" => 0.015 + 0.011 * p,
            "AZ" => 0.300 + 0.020 * p,
            "AX" => 0.220 * p - 0.020,
            _ => 0.0,
        }
    }
}

pub mod thread_rolling_blank {
    const CREST_FLAT: f64 = 0.125;
    const FLANK_SPREAD: f64 = 1.1547;
    const D2_BELOW_MAJOR: f64 = 0.649519;

    pub const ROOT_DEPTH_PLAIN: f64 = 0.61340;
    pub const ROOT_DEPTH_J_SHALLOW: f64 = 0.57735;
    pub const ROOT_DEPTH_J_DEEP: f64 = 0.60766;
    pub const ROOT_DEPTH_J_MID: f64 = (ROOT_DEPTH_J_SHALLOW + ROOT_DEPTH_J_DEEP) / 2.0;

    fn blank_offset_from_d2(root_depth_per_pitch: f64) -> f64 {
        let h = root_depth_per_pitch;
        let ub = (1.0 - CREST_FLAT) * h - FLANK_SPREAD * h * h / 2.0;
        D2_BELOW_MAJOR - 2.0 * ub
    }

    /// Diameter to add to a plain-form rolling blank to roll the J form instead.
    pub fn controlled_root_delta(pitch_mm: f64, root_depth_per_pitch: f64) -> f64 {
        (blank_offset_from_d2(root_depth_per_pitch) - blank_offset_from_d2(ROOT_DEPTH_PLAIN)) * pitch_mm
    }

    pub fn controlled_root_delta_mid(pitch_mm: f64) -> f64 {
        controlled_root_delta(pitch_mm, ROOT_DEPTH_J_MID)
    }

    /// The band the correction spans across the allowed root radius, as diameters in mm.
    pub fn controlled_root_delta_range(pitch_mm: f64) -> (f64, f64) {
        (controlled_root_delta(pitch_mm, ROOT_DEPTH_J_DEEP), controlled_root_delta(pitch_mm, ROOT_DEPTH_J_SHALLOW))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_iso_classes() {
        assert_eq!(IsoThreadClass::parse("4g6g"), IsoThreadClass { pitch_grade: 4, crest_grade: 6, position: "g".into(), is_mj: false });
        assert_eq!(IsoThreadClass::parse("6g").crest_grade, 6);
        assert_eq!(IsoThreadClass::parse("6AZ").position, "AZ");
        assert!(IsoThreadClass::parse("4h6h-MJ").is_mj);
        assert_eq!(IsoThreadClass::parse("2A"), IsoThreadClass { pitch_grade: 2, crest_grade: 2, position: "A".into(), is_mj: false });
        assert_eq!(IsoThreadClass::parse("3A-UNJ").position, "H"); // no match: falls back on case
    }
}
