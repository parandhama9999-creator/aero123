//! Port of DimensionTables.kt: head, nut and washer dimensions from ISO / ASME / DIN tables.

use crate::bolt_forging;
use crate::gen_enums::{FastenerType, UnitSystem};
use crate::poka_yoke;
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HeadDimensions {
    /// Hex s, or the head diameter dk of a round head.
    pub across_flats_s: f64,
    pub head_height_k: f64,
    /// Min bearing face diameter.
    pub bearing_dia_dw: f64,
    pub nut_height_m: f64,
    pub source: String,
    pub is_table: bool,
    /// Square neck height f of a carriage bolt (DIN 603); None for every other head.
    pub neck_height_mm: Option<f64>,
}

impl HeadDimensions {
    fn new(s: f64, k: f64, dw: f64, m: f64, source: &str, is_table: bool) -> Self {
        HeadDimensions { across_flats_s: s, head_height_k: k, bearing_dia_dw: dw, nut_height_m: m, source: source.into(), is_table, neck_height_mm: None }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WasherDimensions {
    pub inner_dia_mm: f64,
    pub outer_dia_mm: f64,
    pub thickness_mm: f64,
    pub source: String,
    pub is_table: bool,
}

type Table = &'static [(f64, &'static [f64])];

static ISO_HEX: Table = &[
    (1.6, &[3.2, 1.1, 1.3, 2.27]),
    (2.0, &[4.0, 1.4, 1.6, 3.07]),
    (2.5, &[5.0, 1.7, 2.0, 4.07]),
    (3.0, &[5.5, 2.0, 2.4, 4.57]),
    (4.0, &[7.0, 2.8, 3.2, 5.88]),
    (5.0, &[8.0, 3.5, 4.7, 6.88]),
    (6.0, &[10.0, 4.0, 5.2, 8.88]),
    (8.0, &[13.0, 5.3, 6.8, 11.63]),
    (10.0, &[16.0, 6.4, 8.4, 14.63]),
    (12.0, &[18.0, 7.5, 10.8, 16.63]),
    (14.0, &[21.0, 8.8, 12.8, 19.64]),
    (16.0, &[24.0, 10.0, 14.8, 22.49]),
    (20.0, &[30.0, 12.5, 18.0, 28.19]),
    (24.0, &[36.0, 15.0, 21.5, 33.61]),
    (30.0, &[46.0, 18.7, 25.6, 42.75]),
    (36.0, &[55.0, 22.5, 31.0, 51.11]),
];
static ASME_HEX: Table = &[
    (0.25, &[0.4375, 0.156, 0.4375, 0.219]),
    (0.3125, &[0.5, 0.203, 0.5, 0.266]),
    (0.375, &[0.5625, 0.234, 0.5625, 0.328]),
    (0.4375, &[0.625, 0.27, 0.6875, 0.375]),
    (0.5, &[0.75, 0.312, 0.75, 0.438]),
    (0.5625, &[0.8125, 0.348, 0.875, 0.484]),
    (0.625, &[0.9375, 0.391, 0.9375, 0.547]),
    (0.75, &[1.125, 0.469, 1.125, 0.641]),
    (0.875, &[1.3125, 0.547, 1.3125, 0.75]),
    (1.0, &[1.5, 0.627, 1.5, 0.859]),
];
static ISO_SHCS_SOCKET: Table = &[
    (1.6, &[1.5, 0.7]),
    (2.0, &[1.5, 1.0]),
    (2.5, &[2.0, 1.1]),
    (3.0, &[2.5, 1.3]),
    (4.0, &[3.0, 2.0]),
    (5.0, &[4.0, 2.5]),
    (6.0, &[5.0, 3.0]),
    (8.0, &[6.0, 4.0]),
    (10.0, &[8.0, 5.0]),
    (12.0, &[10.0, 6.0]),
    (14.0, &[12.0, 7.0]),
    (16.0, &[14.0, 8.0]),
    (20.0, &[17.0, 10.0]),
    (24.0, &[19.0, 12.0]),
    (30.0, &[22.0, 15.5]),
    (36.0, &[27.0, 19.0]),
];
static ISO_CSK: Table = &[
    (3.0, &[6.72, 1.86, 2.0, 1.1]),
    (4.0, &[8.96, 2.48, 2.5, 1.5]),
    (5.0, &[11.2, 3.1, 3.0, 1.9]),
    (6.0, &[13.44, 3.72, 4.0, 2.2]),
    (8.0, &[17.92, 4.96, 5.0, 3.0]),
    (10.0, &[22.4, 6.2, 6.0, 3.6]),
    (12.0, &[26.88, 7.44, 8.0, 4.3]),
    (14.0, &[30.8, 8.4, 10.0, 4.5]),
    (16.0, &[33.6, 8.8, 10.0, 4.8]),
    (20.0, &[40.32, 10.16, 12.0, 5.6]),
];
static ISO_BUTTON: Table = &[
    (3.0, &[5.7, 1.65, 2.0, 1.04]),
    (4.0, &[7.6, 2.2, 2.5, 1.3]),
    (5.0, &[9.5, 2.75, 3.0, 1.56]),
    (6.0, &[10.5, 3.3, 4.0, 2.08]),
    (8.0, &[14.0, 4.4, 5.0, 2.6]),
    (10.0, &[17.5, 5.5, 6.0, 3.12]),
    (12.0, &[21.0, 6.6, 8.0, 4.16]),
    (16.0, &[28.0, 8.8, 10.0, 5.2]),
];
static DIN_603: Table = &[
    (5.0, &[13.55, 3.3]),
    (6.0, &[16.55, 3.88]),
    (8.0, &[20.65, 4.88]),
    (10.0, &[24.65, 5.38]),
    (12.0, &[30.65, 6.95]),
    (16.0, &[38.8, 8.95]),
    (20.0, &[46.8, 11.05]),
];
static ISO_7089: Table = &[
    (1.6, &[1.7, 4.0, 0.3]),
    (2.0, &[2.2, 5.0, 0.3]),
    (2.5, &[2.7, 6.0, 0.5]),
    (3.0, &[3.2, 7.0, 0.5]),
    (4.0, &[4.3, 9.0, 0.8]),
    (5.0, &[5.3, 10.0, 1.0]),
    (6.0, &[6.4, 12.0, 1.6]),
    (8.0, &[8.4, 16.0, 1.6]),
    (10.0, &[10.5, 20.0, 2.0]),
    (12.0, &[13.0, 24.0, 2.5]),
    (14.0, &[15.0, 28.0, 2.5]),
    (16.0, &[17.0, 30.0, 3.0]),
    (20.0, &[21.0, 37.0, 3.0]),
    (24.0, &[25.0, 44.0, 4.0]),
    (30.0, &[31.0, 56.0, 4.0]),
    (36.0, &[37.0, 66.0, 5.0]),
];
static ASME_SHCS: Table = &[
    (0.06, &[0.096, 0.05]),
    (0.073, &[0.118, 0.0625]),
    (0.086, &[0.14, 0.078125]),
    (0.099, &[0.161, 0.078125]),
    (0.112, &[0.183, 0.09375]),
    (0.125, &[0.205, 0.09375]),
    (0.138, &[0.226, 0.109375]),
    (0.164, &[0.27, 0.140625]),
    (0.19, &[0.312, 0.15625]),
    (0.25, &[0.375, 0.1875]),
    (0.3125, &[0.469, 0.25]),
    (0.375, &[0.562, 0.3125]),
    (0.4375, &[0.656, 0.375]),
    (0.5, &[0.75, 0.375]),
    (0.5625, &[0.844, 0.4375]),
    (0.625, &[0.938, 0.5]),
    (0.75, &[1.125, 0.625]),
    (0.875, &[1.312, 0.75]),
    (1.0, &[1.5, 0.75]),
    (1.125, &[1.688, 0.875]),
    (1.25, &[1.875, 0.875]),
    (1.375, &[2.062, 1.0]),
    (1.5, &[2.25, 1.0]),
    (1.75, &[2.625, 1.25]),
];
static DIN_6923: Table = &[
    (5.0, &[8.0, 5.0, 11.8, 1.0, 9.8]),
    (6.0, &[10.0, 6.0, 14.2, 1.1, 12.2]),
    (8.0, &[13.0, 8.0, 17.9, 1.2, 15.8]),
    (10.0, &[15.0, 10.0, 21.8, 1.5, 19.6]),
    (12.0, &[18.0, 12.0, 26.0, 1.8, 23.8]),
    (16.0, &[24.0, 16.0, 34.5, 2.4, 31.9]),
    (20.0, &[30.0, 20.0, 42.8, 3.0, 39.9]),
];
static ISO_SHCS_DK: &[(f64, f64)] = &[(1.6, 3.0), (2.0, 3.8), (2.5, 4.5), (3.0, 5.5), (4.0, 7.0), (5.0, 8.5), (6.0, 10.0), (8.0, 13.0), (10.0, 16.0), (12.0, 18.0), (14.0, 21.0), (16.0, 24.0), (20.0, 30.0), (24.0, 36.0), (30.0, 45.0), (36.0, 54.0)];
const APPROXIMATED: &[FastenerType] = &[FastenerType::TTY_HEAD_BOLT, FastenerType::THREAD_FORMING_SCREW, FastenerType::WHEEL_BOLT, FastenerType::WHEEL_NUT, FastenerType::BANJO_BOLT, FastenerType::NAS_CLOSE_TOL_BOLT, FastenerType::HI_LOK_PIN, FastenerType::HI_LOK_COLLAR, FastenerType::TAPER_LOK, FastenerType::LOCKBOLT_PIN, FastenerType::BLIND_BOLT, FastenerType::AERO_LOCK_NUT, FastenerType::NUTPLATE, FastenerType::SOLID_RIVET, FastenerType::BLIND_RIVET, FastenerType::TWELVE_POINT, FastenerType::T_BOLT, FastenerType::EYE_BOLT, FastenerType::HEAVY_HEX_BOLT, FastenerType::HEAVY_HEX_NUT, FastenerType::NYLOC_NUT, FastenerType::METAL_LOCK_NUT, FastenerType::CASTLE_NUT, FastenerType::ACORN_NUT, FastenerType::WELD_NUT, FastenerType::SQUARE_BOLT, FastenerType::SHOULDER_BOLT, FastenerType::SET_SCREW_CONE, FastenerType::SET_SCREW_DOG, FastenerType::SET_SCREW_CUP, FastenerType::SLOTTED_SET_SCREW, FastenerType::THUMB_SCREW, FastenerType::WING_SCREW, FastenerType::HANGER_BOLT, FastenerType::ANCHOR_BOLT, FastenerType::LAG_SCREW, FastenerType::PAN_SLOT, FastenerType::PAN_CROSS, FastenerType::PAN_HEXALOBULAR, FastenerType::CHEESE_SLOT, FastenerType::CSK_SLOT, FastenerType::CSK_CROSS, FastenerType::RAISED_CSK_CROSS, FastenerType::HEX_WASHER_SCREW, FastenerType::SEMS_SCREW, FastenerType::TAPPING_PAN, FastenerType::TAPPING_CSK, FastenerType::TAPPING_HEX, FastenerType::SELF_DRILLING, FastenerType::DRYWALL_SCREW, FastenerType::CHIPBOARD_SCREW, FastenerType::CONROD_BOLT, FastenerType::CLEVIS_BOLT, FastenerType::SHEAR_BOLT, FastenerType::THREADED_ROD, FastenerType::TAP_END_STUD, FastenerType::DOWEL_PIN, FastenerType::TAPER_PIN, FastenerType::CLEVIS_PIN, FastenerType::SPLIT_PIN, FastenerType::SPRING_PIN, FastenerType::GROOVED_PIN, FastenerType::RETAINING_RING, FastenerType::THIN_NUT, FastenerType::HIGH_NUT, FastenerType::SQUARE_NUT, FastenerType::COUPLING_NUT, FastenerType::T_SLOT_NUT, FastenerType::CAGE_NUT, FastenerType::WING_NUT, FastenerType::THUMB_NUT, FastenerType::RIVET_NUT, FastenerType::CLINCH_NUT, FastenerType::FENDER_WASHER, FastenerType::STRUCT_WASHER, FastenerType::TOOTH_LOCK_WASHER, FastenerType::BELLEVILLE_WASHER, FastenerType::WAVE_WASHER, FastenerType::TAPER_WASHER];

fn lookup<V: Copy>(map: &[(f64, V)], key: f64) -> Option<V> {
    map.iter().find(|(k, _)| (k - key).abs() < 0.002).map(|(_, v)| *v)
}

/// Hexalobular drive point-to-point A of ISO 10664 for a pan head screw by nominal diameter.
pub fn hexalobular_drive_a(d_mm: f64) -> f64 {
    const DRIVES: [(f64, f64); 9] = [(2.2, 1.75), (2.7, 2.40), (3.2, 2.80), (3.7, 3.35), (4.4, 3.95), (5.4, 4.50), (7.0, 5.60), (9.0, 6.75), (11.0, 8.95)];
    DRIVES.iter().find(|(d, _)| d_mm <= *d).map(|(_, a)| *a).unwrap_or(8.95)
}

/// Hex socket (key size, depth) in mm, or None when the part has no socket.
pub fn socket_for(t: FastenerType, unit: UnitSystem, d_mm: f64) -> Option<(f64, f64)> {
    let metric = unit == UnitSystem::METRIC;
    match t {
        FastenerType::SOCKET_SET => Some((0.5 * d_mm, 0.5 * d_mm)),
        FastenerType::SHCS | FastenerType::HEXALOBULAR_SCREW => Some(
            (if metric {
                lookup(ISO_SHCS_SOCKET, d_mm).map(|it| (it[0], it[1]))
            } else {
                lookup(ASME_SHCS, d_mm / 25.4).map(|it| (it[1] * 25.4, 0.5 * d_mm))
            })
            .unwrap_or((0.8 * d_mm, 0.5 * d_mm)),
        ),
        FastenerType::SOCKET_CSK => Some((if metric { lookup(ISO_CSK, d_mm).map(|it| (it[2], it[3])) } else { None }).unwrap_or((0.6 * d_mm, 0.36 * d_mm))),
        FastenerType::SOCKET_BUTTON => Some((if metric { lookup(ISO_BUTTON, d_mm).map(|it| (it[2], it[3])) } else { None }).unwrap_or((0.6 * d_mm, 0.31 * d_mm))),
        FastenerType::PAN_HEXALOBULAR => Some((hexalobular_drive_a(d_mm), 0.27 * d_mm)),
        _ => None,
    }
}

fn wd(i: f64, o: f64, t: f64, s: &str, table: bool) -> WasherDimensions {
    WasherDimensions { inner_dia_mm: i, outer_dia_mm: o, thickness_mm: t, source: s.into(), is_table: table }
}

/// Washer bore, outside diameter and thickness: ISO 7089 for metric plain washers, otherwise a flagged estimate.
pub fn washer_for(t: FastenerType, unit: UnitSystem, d: f64) -> WasherDimensions {
    if t == FastenerType::FLAT_WASHER && unit == UnitSystem::METRIC {
        if let Some(it) = lookup(ISO_7089, d) {
            return wd(it[0], it[1], it[2], "ISO 7089", true);
        }
    }
    if t == FastenerType::FENDER_WASHER && unit == UnitSystem::METRIC {
        return wd(1.08 * d, 3.0 * d, 0.25 * d, "Estimated (ISO 7093 large washer proportions)", false);
    }
    match t {
        FastenerType::STRUCT_WASHER => wd(1.08 * d, 2.0 * d, 0.22 * d, "Estimated (ASTM F436 hardened washer proportions)", false),
        FastenerType::TOOTH_LOCK_WASHER => wd(1.05 * d, 2.0 * d, 0.10 * d, "Estimated (DIN 6797 / 6798 serrated washer; teeth not modelled)", false),
        FastenerType::BELLEVILLE_WASHER => wd(1.05 * d, 2.0 * d, 0.20 * d, "Estimated (DIN 6796 conical washer, flattened thickness)", false),
        FastenerType::WAVE_WASHER => wd(1.05 * d, 1.7 * d, 0.12 * d, "Estimated (DIN 137 wave washer, flattened thickness)", false),
        FastenerType::TAPER_WASHER => wd(1.10 * d, 2.2 * d, 0.45 * d, "Estimated (DIN 434 / 435 taper washer, mean thickness)", false),
        FastenerType::RETAINING_RING => wd(0.92 * d, 1.5 * d, 0.10 * d, "Estimated (DIN 471 / 472 circlip, lugs not modelled)", false),
        FastenerType::SPRING_WASHER => wd(1.02 * d, 1.75 * d, 0.22 * d, "Estimated (no spring washer table in the app)", false),
        _ => wd(1.06 * d, 2.0 * d, 0.18 * d, "Estimated (no washer table for this size / standard)", false),
    }
}

/// DIN 6923 flange Ø dc max and thickness c min for a metric size; None outside the table.
pub fn din6923_flange(d_mm: f64) -> Option<(f64, f64)> {
    DIN_6923.iter().find(|(k, _)| (k - d_mm).abs() < 0.01).map(|(_, v)| (v[2], v[3]))
}

pub fn for_part(t: FastenerType, unit: UnitSystem, d_mm: f64) -> HeadDimensions {
    let dims = table_for(t, unit, d_mm);
    if !APPROXIMATED.contains(&t) {
        return dims;
    }
    let std = poka_yoke::default_standard(t, unit).code();
    HeadDimensions { source: format!("Approximated from {} — no {} table in the app", dims.source, std), is_table: false, ..dims }
}

fn estimated(dk: f64, k: f64, d_mm: f64, why: &str) -> HeadDimensions {
    HeadDimensions::new(dk, k, 0.92 * dk, 0.84 * d_mm, &format!("Estimated ({})", why), false)
}

fn table_for(t: FastenerType, unit: UnitSystem, d: f64) -> HeadDimensions {
    use FastenerType::*;
    let metric = unit == UnitSystem::METRIC;
    let nut_m = lookup(ISO_HEX, d).map(|h| h[2]).unwrap_or(0.84 * d);
    match t {
        SHCS | HEXALOBULAR_SCREW => {
            if metric {
                if let Some(dk) = lookup(ISO_SHCS_DK, d) {
                    let src = if t == HEXALOBULAR_SCREW { "ISO 14579 (head per ISO 4762)" } else { "ISO 4762" };
                    return HeadDimensions::new(dk, d, 0.92 * dk, nut_m, src, true);
                }
            } else if let Some(a) = lookup(ASME_SHCS, d / 25.4) {
                return HeadDimensions::new(a[0] * 25.4, d, 0.92 * a[0] * 25.4, nut_m, "ASME B18.3 (1960 series, H = D)", true);
            }
            return estimated(1.5 * d, d, d, "outside the socket cap screw table");
        }
        SOCKET_CSK => {
            if metric {
                if let Some(it) = lookup(ISO_CSK, d) {
                    return HeadDimensions::new(it[0], it[1], it[0], nut_m, "ISO 10642", true);
                }
            }
            return estimated(2.0 * d, 0.5 * d, d, if metric { "outside the ISO 10642 table" } else { "no inch countersunk table in the app" });
        }
        SOCKET_BUTTON => {
            if metric {
                if let Some(it) = lookup(ISO_BUTTON, d) {
                    return HeadDimensions::new(it[0], it[1], 0.92 * it[0], nut_m, "ISO 7380-1", true);
                }
            }
            return estimated(1.75 * d, 0.55 * d, d, if metric { "outside the ISO 7380 table" } else { "no inch button head table in the app" });
        }
        CARRIAGE_BOLT => {
            if metric {
                if let Some(it) = lookup(DIN_603, d) {
                    return HeadDimensions::new(it[0], it[1], 0.92 * it[0], nut_m, "DIN 603 (head only)", true);
                }
            }
            return estimated(2.4 * d, 0.55 * d, d, if metric { "outside the DIN 603 table" } else { "no inch carriage bolt table in the app" });
        }
        FLANGE_NUT => {
            if metric {
                if let Some((_, r)) = DIN_6923.iter().find(|(k, _)| (k - d).abs() < 0.01) {
                    return HeadDimensions::new(r[0], r[1], r[4], r[1], "DIN 6923 (supplier tables)", true);
                }
            }
            let hex = table_for(HEX_NUT, unit, d);
            return HeadDimensions { source: format!("Approximated from {} — no DIN 6923 row for this size", hex.source), is_table: false, ..hex };
        }
        FLANGE_BOLT => {
            if metric {
                if let Some(g) = bolt_forging::din6921(d) {
                    if let (Some(s), Some(k), Some(dc)) = (g.s, g.k, g.dc) {
                        return HeadDimensions::new(s, k, 0.95 * dc, nut_m, "DIN 6921 (1983 edition, supplier tables)", true);
                    }
                }
            }
        }
        PAN_SLOT | PAN_CROSS | PAN_HEXALOBULAR | TAPPING_PAN | SELF_DRILLING => return estimated(2.0 * d, 0.60 * d, d, "pan head proportions, ISO 7045 / ISO 1481"),
        CHEESE_SLOT => return estimated(1.6 * d, 0.60 * d, d, "cheese head proportions, ISO 1207"),
        CSK_SLOT | CSK_CROSS | TAPPING_CSK | CHIPBOARD_SCREW => return estimated(1.9 * d, 0.55 * d, d, "90° countersunk head proportions, ISO 7046 / ISO 7050"),
        RAISED_CSK_CROSS => return estimated(1.9 * d, 0.75 * d, d, "raised countersunk head proportions, ISO 7047"),
        DRYWALL_SCREW => return estimated(2.2 * d, 0.50 * d, d, "bugle head proportions, DIN 18182"),
        HEX_WASHER_SCREW | SEMS_SCREW => return estimated(2.2 * d, 0.80 * d, d, "hex washer / SEMS head proportions, DIN 6900"),
        SHOULDER_BOLT => return estimated(1.5 * d, 0.70 * d, d, "shoulder screw head proportions, ISO 7379"),
        THUMB_SCREW | WING_SCREW => return estimated(2.5 * d, 0.90 * d, d, "knurled / wing head proportions, DIN 464 / DIN 316"),
        CLEVIS_BOLT | CLEVIS_PIN => return estimated(1.5 * d, 0.35 * d, d, "headed pin proportions, AN21 / ISO 2341"),
        SET_SCREW_CONE | SET_SCREW_DOG | SET_SCREW_CUP | SLOTTED_SET_SCREW | HANGER_BOLT | ANCHOR_BOLT | THREADED_ROD | TAP_END_STUD | DOWEL_PIN
        | TAPER_PIN | SPLIT_PIN | SPRING_PIN | GROOVED_PIN | RETAINING_RING => return estimated(d, 0.0, d, "headless part — no head volume"),
        THIN_NUT => {
            if let Some(it) = lookup(ISO_HEX, d) {
                return HeadDimensions::new(it[0], 0.55 * d, it[3], 0.55 * d, "ISO 4035 thin nut (m ≈ 0.55 d)", false);
            }
        }
        HIGH_NUT => {
            if let Some(it) = lookup(ISO_HEX, d) {
                return HeadDimensions::new(it[0], 0.90 * d, it[3], 0.90 * d, "ISO 4033 style 2 nut (m ≈ 0.90 d)", false);
            }
        }
        COUPLING_NUT => {
            if let Some(it) = lookup(ISO_HEX, d) {
                return HeadDimensions::new(it[0], 3.0 * d, it[3], 3.0 * d, "DIN 6334 coupling nut (m = 3 d)", false);
            }
        }
        SOLID_RIVET | BLIND_RIVET => {
            return HeadDimensions::new(2.0 * d, 0.425 * d, 2.0 * d, 0.84 * d, "MS20470 universal head proportions (A = 2 D, H = 0.425 D)", false)
        }
        _ => {}
    }
    if !metric {
        if let Some(a) = lookup(ASME_HEX, d / 25.4) {
            let f = if t.is_nut() { a[2] } else { a[0] };
            return HeadDimensions::new(f * 25.4, a[1] * 25.4, 0.95 * f * 25.4, a[3] * 25.4, "ASME B18.2.1 / B18.2.2", true);
        }
    } else if let Some(h) = lookup(ISO_HEX, d) {
        return HeadDimensions::new(h[0], h[1], h[3], h[2], "ISO 4014 / ISO 4032", true);
    }
    HeadDimensions::new(1.6 * d, 0.65 * d, 1.45 * d, 0.84 * d, "Estimated (outside standard table)", false)
}
