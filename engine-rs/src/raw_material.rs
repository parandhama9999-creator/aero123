//! Port of RawMaterialPlanner.kt: batch weight and wire length across the diameters a shop stocks.

use crate::heading;
use crate::jfmt::{self, d as dstr};
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FeedstockOption {
    pub wire_dia_mm: f64,
    pub cut_off_length_mm: f64,
    pub kg_per_metre: f64,
    pub pieces_per_metre: f64,
    pub batch_length_m: f64,
    pub batch_weight_kg: f64,
    pub upset_ld: Option<f64>,
    pub blows: Option<i32>,
    pub extrusion_reduction_pct: Option<f64>,
    pub role: Option<String>,
    pub first_die_bore_mm: f64,
    pub cautions: Vec<String>,
}

impl FeedstockOption {
    pub fn feasible(&self) -> bool {
        self.cautions.is_empty()
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RawMaterialPlan {
    pub batch_qty: i32,
    pub gross_weight_per_piece_g: f64,
    pub batch_weight_kg: f64,
    pub pieces_per_tonne: i32,
    pub options: Vec<FeedstockOption>,
}

impl RawMaterialPlan {
    /// The lightest feasible option is always the shortest one, so rank by cut-off length.
    pub fn recommended(&self) -> Option<&FeedstockOption> {
        jfmt::min_by(self.options.iter().filter(|o| o.feasible()), |o| o.cut_off_length_mm)
    }
}

/// Kotlin `List<Double>.distinct()`: equality of the boxed Double (bit pattern).
pub(crate) fn distinct_f64(v: impl IntoIterator<Item = f64>) -> Vec<f64> {
    let mut out: Vec<f64> = Vec::new();
    for x in v {
        if !out.iter().any(|y| y.to_bits() == x.to_bits() || (y.is_nan() && x.is_nan())) {
            out.push(x);
        }
    }
    out
}

/// Kotlin `List<Double>.sorted()`: Double.compareTo order (-0.0 before 0.0, NaN last).
pub(crate) fn sort_f64(v: &mut [f64]) {
    v.sort_by(|a, b| match (a.is_nan(), b.is_nan()) {
        (true, true) => std::cmp::Ordering::Equal,
        (true, false) => std::cmp::Ordering::Greater,
        (false, true) => std::cmp::Ordering::Less,
        _ => a.total_cmp(b),
    });
}

/// Wire is drawn in 0.05 mm steps.
fn to_step(v: f64) -> f64 {
    jfmt::rint(v / 0.05) * 0.05
}

/// The usable wire window for a part, from the thread rolling blank up to the shank.
pub fn suggested_diameters(rolling_blank_dia_mm: f64, shank_wire_dia_mm: f64) -> Vec<f64> {
    let lo = rolling_blank_dia_mm.min(shank_wire_dia_mm);
    let hi = rolling_blank_dia_mm.max(shank_wire_dia_mm);
    if lo <= 0.0 {
        return vec![];
    }
    if hi - lo < 0.1 {
        return vec![to_step(lo)];
    }
    distinct_f64((0..=3).map(|i| to_step(lo + (hi - lo) * i as f64 / 3.0)))
}

fn fmt(v: f64, dp: usize) -> String {
    jfmt::f(v, dp)
}

pub fn plan(
    gross_volume_mm3: f64, density_g_per_cm3: f64, batch_qty: i32, wire_diameters: &[f64], head_volume_mm3: f64,
    shank_dia_mm: Option<f64>, rolling_blank_dia_mm: Option<f64>,
) -> Option<RawMaterialPlan> {
    if gross_volume_mm3 <= 0.0 || density_g_per_cm3 <= 0.0 || batch_qty <= 0 {
        return None;
    }
    let mut dias = distinct_f64(wire_diameters.iter().copied().filter(|&d| d > 0.0));
    sort_f64(&mut dias);
    if dias.is_empty() {
        return None;
    }
    let gross_weight_g = gross_volume_mm3 * density_g_per_cm3 / 1000.0;
    let batch_weight_kg = gross_weight_g * batch_qty as f64 / 1000.0;
    let options = dias
        .iter()
        .map(|&dw| {
            let area = PI / 4.0 * dw.powf(2.0);
            let cut_off = gross_volume_mm3 / area;
            let kg_per_metre = area * density_g_per_cm3 / 1000.0;
            let batch_length_m = cut_off * batch_qty as f64 / 1000.0;
            let ld = if head_volume_mm3 > 0.0 { Some((head_volume_mm3 / area) / dw) } else { None };
            let blows = ld.map(|l| {
                if l <= heading::SINGLE_BLOW_LD {
                    1
                } else if l <= heading::TWO_BLOW_LD {
                    2
                } else {
                    3
                }
            });
            let reduction = shank_dia_mm.filter(|&s| s > 0.0 && s < dw).map(|s| (1.0 - (s / dw).powf(2.0)) * 100.0);
            let role = match (rolling_blank_dia_mm, shank_dia_mm) {
                (Some(b), _) if (dw - b).abs() < 0.06 => Some("thread rolling blank".to_string()),
                (_, Some(s)) if (dw - s).abs() < 0.06 => Some("shank Ø".to_string()),
                _ => None,
            };
            let mut cautions = Vec::new();
            if let Some(b) = rolling_blank_dia_mm {
                if dw < b - 0.05 {
                    cautions.push(format!("Ø{} is under the Ø{} thread rolling blank: the thread section has to be upset back up, or the thread rolls undersize.", dstr(dw), fmt(b, 2)));
                }
            }
            if let Some(l) = ld {
                if l > heading::THREE_BLOW_LD {
                    cautions.push(format!("Head stock L/D {} is past the 3-blow limit of {}: this diameter is too thin to gather the head.", fmt(l, 1), dstr(heading::THREE_BLOW_LD)));
                }
            }
            if let Some(r) = reduction {
                if r > 30.0 {
                    cautions.push(format!(
                        "Extruding Ø{} down to Ø{} removes {} % of area, past the ~30 % open-die limit: needs a trapped die, or a thinner wire.",
                        dstr(dw),
                        shank_dia_mm.map(dstr).unwrap_or_else(|| "null".into()),
                        fmt(r, 0)
                    ));
                }
            }
            if let Some(s) = shank_dia_mm {
                if s > dw {
                    cautions.push(format!("Shank Ø{} is larger than the wire: the shank cannot be formed from this diameter.", dstr(s)));
                }
            }
            FeedstockOption {
                wire_dia_mm: dw,
                cut_off_length_mm: cut_off,
                kg_per_metre,
                pieces_per_metre: 1000.0 / cut_off,
                batch_length_m,
                batch_weight_kg,
                upset_ld: ld,
                blows,
                extrusion_reduction_pct: reduction,
                role,
                first_die_bore_mm: dw + heading::stage_clearance(dw),
                cautions,
            }
        })
        .collect();
    Some(RawMaterialPlan {
        batch_qty,
        gross_weight_per_piece_g: gross_weight_g,
        batch_weight_kg,
        pieces_per_tonne: if gross_weight_g > 0.0 { (1_000_000.0 / gross_weight_g) as i32 } else { 0 },
        options,
    })
}

/// Coils a run needs, given the coil weight the supplier ships.
pub fn coils_needed(batch_weight_kg: f64, coil_weight_kg: f64) -> i32 {
    if coil_weight_kg <= 0.0 {
        0
    } else {
        (batch_weight_kg / coil_weight_kg).ceil() as i32
    }
}

/// Parses "5.5, 6, 6.5" from the diameter field.
pub fn parse_diameters(text: &str) -> Vec<f64> {
    let mut v = distinct_f64(
        text.split([',', ';', ' ', '\n'])
            .filter_map(|s| jfmt::to_double_or_null(s.trim()))
            .filter(|&d| d > 0.0 && d < 100.0),
    );
    sort_f64(&mut v);
    v
}
