//! Port of BoltForging.kt: forging figures for hex and hex-flange bolts.

use crate::dimensions::HeadDimensions;
use crate::gen_enums::{FastenerType, UnitSystem};
use crate::jfmt::{self, d as dstr};
use crate::poka_yoke;
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BoltForgingResult {
    pub thread_length_mm: f64,
    pub thread_from_drawing: bool,
    pub shank_dia_mm: f64,
    pub shank_from_drawing: bool,
    pub blank_dia_mm: f64,
    pub cone_length_mm: f64,
    pub stock_to_extrude_mm: f64,
    pub length_under_head_before_mm: f64,
    pub head_note: String,
    pub head_from_drawing: bool,
    pub finished_volume_mm3: Option<f64>,
    pub cheese_volume_mm3: Option<f64>,
    pub head_stock_mm: Option<f64>,
    pub cut_off_length_mm: Option<f64>,
    pub density_g_per_mm3: f64,
    pub missing: Vec<String>,
}

pub const HEX_TYPES: &[FastenerType] = &[FastenerType::HEX_BOLT, FastenerType::HEX_BOLT_FULL, FastenerType::HEAVY_HEX_BOLT];

pub fn applies(t: FastenerType) -> bool {
    HEX_TYPES.contains(&t) || t == FastenerType::FLANGE_BOLT
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
pub struct FlangeGeom {
    pub dc: Option<f64>,
    pub k: Option<f64>,
    pub s: Option<f64>,
    pub c: Option<f64>,
}

impl FlangeGeom {
    pub fn complete(&self) -> bool {
        matches!((self.dc, self.k, self.s, self.c), (Some(_), Some(k), Some(_), Some(c)) if c > 0.0 && k > c)
    }
}

/// DIN 6921 (supplier tables): d → dc max, k max, s, c min.
const DIN_6921: [(f64, f64, f64, Option<f64>, Option<f64>); 7] = [
    (5.0, 11.8, 5.4, Some(8.0), Some(1.0)),
    (6.0, 14.2, 6.6, Some(10.0), Some(1.1)),
    (8.0, 18.0, 8.1, Some(13.0), Some(1.2)),
    (10.0, 22.3, 9.2, Some(15.0), Some(1.5)),
    (12.0, 26.6, 11.5, Some(16.0), Some(1.8)),
    (16.0, 35.0, 14.4, Some(21.0), Some(2.4)),
    (20.0, 43.0, 17.1, None, None),
];

pub fn din6921(d_mm: f64) -> Option<FlangeGeom> {
    DIN_6921.iter().find(|r| (r.0 - d_mm).abs() < 0.01).map(|r| FlangeGeom { dc: Some(r.1), k: Some(r.2), s: r.3, c: r.4 })
}

/// Drawing / typed values first, then DIN 6921.
pub fn resolve_flange(d_mm: f64, dc: Option<f64>, k: Option<f64>, s: Option<f64>, c: Option<f64>) -> FlangeGeom {
    let din = din6921(d_mm);
    FlangeGeom {
        dc: dc.or(din.and_then(|x| x.dc)),
        k: k.or(din.and_then(|x| x.k)),
        s: s.or(din.and_then(|x| x.s)),
        c: c.or(din.and_then(|x| x.c)),
    }
}

#[allow(clippy::too_many_arguments)]
pub fn compute(
    t: FastenerType, unit: UnitSystem, d_mm: f64, _pitch_mm: f64, length_mm: f64,
    blank_dia_mm: f64, wire_dia_mm: f64, table_head: Option<&HeadDimensions>, density_g_per_cm3: f64,
    thread_length_mm: Option<f64>, shank_dia_mm: Option<f64>, across_flats_mm: Option<f64>, head_height_mm: Option<f64>,
    flange_dia_mm: Option<f64>, flange_thickness_mm: Option<f64>,
) -> BoltForgingResult {
    let mut missing: Vec<String> = Vec::new();
    let b = thread_length_mm.unwrap_or_else(|| poka_yoke::thread_length_b(t, unit, d_mm, length_mm)).min(length_mm);
    let ds = shank_dia_mm.unwrap_or(wire_dia_mm);
    let db = blank_dia_mm;
    let a_shank = PI / 4.0 * ds * ds;
    let a_blank = PI / 4.0 * db * db;
    let cone = if ds > db { ((ds - db) / 2.0 / (15.0f64).to_radians().tan()).min(0.0f64.max(length_mm - b)) } else { 0.0 };
    let cone_vol = PI * cone / 12.0 * (ds * ds + ds * db + db * db);
    let stock = (a_blank * b + cone_vol) / a_shank;
    let under_head_before = length_mm - b - cone + stock;
    let body_vol = a_shank * (length_mm - b - cone) + cone_vol + a_blank * b;

    let mut head_finished: Option<f64> = None;
    let mut cheese: Option<f64> = None;
    let mut head_forged: Option<f64> = None;
    let note: String;
    let s = across_flats_mm.or(table_head.map(|h| h.across_flats_s));
    let k = head_height_mm.or(table_head.map(|h| h.head_height_k));
    let head_from_drawing = across_flats_mm.is_some() && head_height_mm.is_some();
    if t == FastenerType::FLANGE_BOLT {
        if flange_dia_mm.is_none() {
            missing.push("flange Ø dc".into());
        }
        if flange_thickness_mm.is_none() {
            missing.push("flange thickness c".into());
        }
        match (s, k, flange_dia_mm, flange_thickness_mm) {
            (Some(s), Some(k), Some(dc), Some(c)) if k > c => {
                cheese = Some(0.0);
                let hf = PI / 4.0 * dc * dc * c + 3.0f64.sqrt() / 2.0 * s * s * (k - c);
                head_finished = Some(hf);
                head_forged = Some(hf);
                note = format!("Flange Ø{} × c {} formed in the die cavity, hex s {} closed in the punch; no trim", dstr(dc), dstr(c), dstr(s));
            }
            _ => note = "Needs dc and c from the drawing".into(),
        }
    } else if let (Some(s), Some(k)) = (s, k) {
        let d_forged = 1.2 * s;
        let hf = PI / 4.0 * d_forged * d_forged * k;
        head_forged = Some(hf);
        let fin = 3.0f64.sqrt() / 2.0 * s * s * k;
        head_finished = Some(fin);
        cheese = Some(hf - fin);
        note = format!("Round head forged Ø{} (1.20 × s) × k {}, trimmed to hex s {}", jfmt::f(d_forged, 2), dstr(k), dstr(s));
    } else {
        note = "Needs across flats and head height".into();
    }
    if across_flats_mm.is_none() {
        missing.push("across flats s".into());
    }
    if head_height_mm.is_none() {
        missing.push("head height k".into());
    }
    if thread_length_mm.is_none() {
        missing.push("thread length b".into());
    }
    if shank_dia_mm.is_none() {
        missing.push("shank Ø".into());
    }
    let head_stock = head_forged.map(|h| h / a_shank);
    BoltForgingResult {
        thread_length_mm: b,
        thread_from_drawing: thread_length_mm.is_some(),
        shank_dia_mm: ds,
        shank_from_drawing: shank_dia_mm.is_some(),
        blank_dia_mm: db,
        cone_length_mm: cone,
        stock_to_extrude_mm: stock,
        length_under_head_before_mm: under_head_before,
        head_note: note,
        head_from_drawing,
        finished_volume_mm3: head_finished.map(|h| h + body_vol),
        cheese_volume_mm3: cheese,
        head_stock_mm: head_stock,
        cut_off_length_mm: head_stock.map(|h| h + under_head_before),
        density_g_per_mm3: density_g_per_cm3 / 1000.0,
        missing,
    }
}
