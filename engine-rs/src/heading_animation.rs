//! Port of HeadingAnimation.kt: timeline and interpolated outlines for the cold-heading animation.

use crate::gen_enums::SectionShape;
use crate::heading::{section_area, HeadingPlan, HeadingStation, ProfileSegment};
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AnimFrame {
    pub from_idx: usize,
    pub to_idx: usize,
    pub morph: f64,
    pub punch_gap_mm: f64,
    pub blow_number: i32,
    pub blow_count: i32,
}

pub const SEC_PER_BLOW: f64 = 2.4;
pub const HOLD_START_SEC: f64 = 1.0;
pub const HOLD_END_SEC: f64 = 1.6;
const APPROACH: f64 = 0.30;
const FORM_END: f64 = 0.70;

pub fn blow_count(plan: &HeadingPlan) -> i32 {
    (plan.stations.len() as i32 - 1).max(0)
}

pub fn duration_sec(plan: &HeadingPlan) -> f64 {
    HOLD_START_SEC + blow_count(plan) as f64 * SEC_PER_BLOW + HOLD_END_SEC
}

fn smooth(x: f64) -> f64 {
    let t = x.clamp(0.0, 1.0);
    t * t * (3.0 - 2.0 * t)
}

pub fn frame_at(plan: &HeadingPlan, t_sec: f64) -> AnimFrame {
    let n = blow_count(plan);
    // Kotlin maxOf: NaN propagates, as Math.max does
    let lift = plan.stations.iter().map(|s| s.height_mm()).fold(f64::NEG_INFINITY, |a, b| if a.is_nan() || b.is_nan() { f64::NAN } else { a.max(b) }) * 0.45;
    if n == 0 {
        return AnimFrame { from_idx: 0, to_idx: 0, morph: 0.0, punch_gap_mm: lift, blow_number: 0, blow_count: 0 };
    }
    let t = t_sec - HOLD_START_SEC;
    if t <= 0.0 {
        return AnimFrame { from_idx: 0, to_idx: 1, morph: 0.0, punch_gap_mm: lift, blow_number: 1, blow_count: n };
    }
    let blow = (t / SEC_PER_BLOW) as i32;
    if blow >= n {
        return AnimFrame { from_idx: (n - 1) as usize, to_idx: n as usize, morph: 1.0, punch_gap_mm: lift, blow_number: n, blow_count: n };
    }
    let p = (t - blow as f64 * SEC_PER_BLOW) / SEC_PER_BLOW;
    let (morph, gap) = if p < APPROACH {
        (0.0, lift * (1.0 - smooth(p / APPROACH)))
    } else if p < FORM_END {
        (smooth((p - APPROACH) / (FORM_END - APPROACH)), 0.0)
    } else {
        (1.0, lift * smooth((p - FORM_END) / (1.0 - FORM_END)))
    };
    AnimFrame { from_idx: blow as usize, to_idx: blow as usize + 1, morph, punch_gap_mm: gap, blow_number: blow + 1, blow_count: n }
}

/// Drawn half width and true cross-section area of a segment at fraction s of its height.
fn at(seg: &ProfileSegment, s: f64) -> (f64, f64) {
    match seg.shape {
        SectionShape::ROUND => {
            let d = seg.d_bottom_mm + (seg.d_top_mm - seg.d_bottom_mm) * s;
            (d / 2.0, PI / 4.0 * d * d)
        }
        SectionShape::DOME => {
            let r0 = seg.d_bottom_mm / 2.0;
            let h = seg.height_mm;
            let rs = (r0 * r0 + h * h) / (2.0 * h);
            let y = h * s;
            let x = 0.0f64.max(rs * rs - (rs - h + y) * (rs - h + y)).sqrt();
            (x, PI * x * x)
        }
        _ => (seg.max_width_mm() / 2.0, section_area(seg.shape, seg.d_bottom_mm)),
    }
}

/// Samples (y, half width, cumulative volume) up the station.
fn samples(st: &HeadingStation, per_seg: usize) -> Vec<(f64, f64, f64)> {
    let mut out = Vec::new();
    let mut y0 = 0.0;
    let mut vol = 0.0;
    for seg in &st.segments {
        let mut prev_a = at(seg, 0.0).1;
        out.push((y0, at(seg, 0.0).0, vol));
        for k in 1..=per_seg {
            let s = k as f64 / per_seg as f64;
            let (w, a) = at(seg, s);
            vol += (prev_a + a) / 2.0 * seg.height_mm / per_seg as f64;
            prev_a = a;
            out.push((y0 + seg.height_mm * s, w, vol));
        }
        y0 += seg.height_mm;
    }
    out
}

/// (y, half width) at each of n volume fractions of the station, bottom to top.
pub fn by_volume(st: &HeadingStation, n: usize) -> Vec<(f64, f64)> {
    let pts = samples(st, 24);
    let total = match pts.last() {
        Some(p) if p.2 > 0.0 => p.2,
        _ => return pts.iter().map(|p| (p.0, p.1)).collect(),
    };
    (0..=n)
        .map(|k| {
            let target = total * k as f64 / n as f64;
            let j = match pts.iter().position(|p| p.2 >= target) {
                Some(i) if i > 0 => i,
                _ => 1.min(pts.len() - 1),
            };
            let (a, b) = (pts[j - 1], pts[j]);
            let u = if b.2 - a.2 > 1e-12 { (target - a.2) / (b.2 - a.2) } else { 0.0 };
            (a.0 + (b.0 - a.0) * u, a.1 + (b.1 - a.1) * u)
        })
        .collect()
}

/// The part outline at this frame: (y from the die bottom, half width) in mm.
pub fn outline(plan: &HeadingPlan, f: &AnimFrame, n: usize) -> Vec<(f64, f64)> {
    let a = by_volume(&plan.stations[f.from_idx], n);
    if f.from_idx == f.to_idx {
        return a;
    }
    let b = by_volume(&plan.stations[f.to_idx], n);
    a.iter().zip(&b).map(|(p, q)| (p.0 + (q.0 - p.0) * f.morph, p.1 + (q.1 - p.1) * f.morph)).collect()
}
