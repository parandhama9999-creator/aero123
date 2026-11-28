//! Port of PartGeometry.kt: the finished part as a stack of solids, with a labelled volume breakdown.

use crate::bolt_forging::{self, BoltForgingResult, FlangeGeom};
use crate::dimensions::{self, HeadDimensions};
use crate::gen_enums::{DimSource, FastenerType, HeadStyle, SectionShape, UnitSystem};
use crate::heading::{HeadingStation, ProfileSegment, Recess};
use crate::jfmt;
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

/// Dimensions read from a drawing, in mm. None = not on the drawing.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DrawingGeometry {
    pub thread_length_mm: Option<f64>,
    pub shank_dia_mm: Option<f64>,
    pub across_flats_mm: Option<f64>,
    pub head_dia_mm: Option<f64>,
    pub head_height_mm: Option<f64>,
    pub flange_dia_mm: Option<f64>,
    pub flange_thickness_mm: Option<f64>,
    pub socket_size_mm: Option<f64>,
    pub socket_depth_mm: Option<f64>,
    pub neck_height_mm: Option<f64>,
    pub nut_height_mm: Option<f64>,
    pub washer_outer_dia_mm: Option<f64>,
    pub washer_inner_dia_mm: Option<f64>,
    pub washer_thickness_mm: Option<f64>,
    pub head_dome_radius_mm: Option<f64>,
    pub head_edge_height_mm: Option<f64>,
}

/// One feature of the finished part. A negative volume is material removed (socket, bore).
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VolumeItem {
    pub feature: String,
    pub detail: String,
    pub volume_mm3: f64,
    pub source: DimSource,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FinishedPart {
    pub style: HeadStyle,
    pub segments: Vec<ProfileSegment>,
    pub recesses: Vec<Recess>,
    pub through_hole_mm: Option<f64>,
    pub head_segment_count: usize,
    pub thread_length_mm: f64,
    pub blank_dia_mm: f64,
    pub items: Vec<VolumeItem>,
    pub from_drawing: Vec<String>,
    pub not_on_drawing: Vec<String>,
    pub cautions: Vec<String>,
}

impl FinishedPart {
    pub fn height_mm(&self) -> f64 {
        self.segments.iter().map(|s| s.height_mm).sum()
    }
    pub fn volume_mm3(&self) -> f64 {
        let segs: f64 = self.segments.iter().map(|s| s.volume_mm3()).sum();
        let recs: f64 = self.recesses.iter().map(|r| r.volume_mm3()).sum();
        segs - recs - self.through_hole_mm.map(|h| PI / 4.0 * h * h * self.height_mm()).unwrap_or(0.0)
    }
    pub fn die_segments(&self) -> &[ProfileSegment] {
        &self.segments[..self.segments.len().saturating_sub(self.head_segment_count)]
    }
    pub fn head_segments(&self) -> &[ProfileSegment] {
        &self.segments[self.segments.len().saturating_sub(self.head_segment_count)..]
    }
    /// Material formed outside the die bore, net of the socket it displaces.
    pub fn head_volume_mm3(&self) -> f64 {
        if self.head_segment_count == 0 {
            0.0
        } else {
            let die: f64 = self.die_segments().iter().map(|s| s.volume_mm3()).sum();
            0.0f64.max(self.volume_mm3() - die)
        }
    }
    pub fn shank_volume_mm3(&self) -> f64 {
        self.volume_mm3() - self.head_volume_mm3()
    }
    pub fn is_complete(&self) -> bool {
        self.not_on_drawing.is_empty() && self.cautions.is_empty()
    }
    pub fn as_station(&self) -> HeadingStation {
        let mut s = HeadingStation::new(1, "Finished part", "", self.segments.clone());
        s.recesses = self.recesses.clone();
        s.through_hole_mm = self.through_hole_mm;
        s
    }
}

use FastenerType as FT;
const ROUND_PIN_TYPES: &[FastenerType] = &[FT::HI_LOK_PIN, FT::TAPER_LOK, FT::LOCKBOLT_PIN, FT::BLIND_BOLT, FT::CLEVIS_PIN];
const HEADLESS_TYPES: &[FastenerType] = &[
    FT::SOCKET_SET, FT::SET_SCREW_CONE, FT::SET_SCREW_DOG, FT::SET_SCREW_CUP, FT::SLOTTED_SET_SCREW, FT::HANGER_BOLT, FT::ANCHOR_BOLT,
    FT::DOWEL_PIN, FT::TAPER_PIN, FT::SPRING_PIN, FT::GROOVED_PIN, FT::SPLIT_PIN,
];
const CYLINDRICAL_HEAD_TYPES: &[FastenerType] = &[FT::PAN_SLOT, FT::PAN_CROSS, FT::PAN_HEXALOBULAR, FT::CHEESE_SLOT, FT::SHOULDER_BOLT, FT::THUMB_SCREW, FT::CLEVIS_BOLT];
pub const PAN_DOME_TYPES: &[FastenerType] = &[FT::PAN_SLOT, FT::PAN_CROSS, FT::PAN_HEXALOBULAR];
const COUNTERSUNK_TYPES: &[FastenerType] = &[FT::SOCKET_CSK, FT::CSK_SLOT, FT::CSK_CROSS, FT::RAISED_CSK_CROSS, FT::TAPPING_CSK, FT::DRYWALL_SCREW, FT::CHIPBOARD_SCREW];
const SOCKET_STYLES: &[HeadStyle] = &[HeadStyle::SOCKET_CAP, HeadStyle::SOCKET_COUNTERSUNK, HeadStyle::SOCKET_BUTTON];
const ROUND_HEAD_STYLES: &[HeadStyle] = &[HeadStyle::SOCKET_CAP, HeadStyle::SOCKET_COUNTERSUNK, HeadStyle::SOCKET_BUTTON, HeadStyle::CARRIAGE, HeadStyle::ROUND_PIN, HeadStyle::RIVET];

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct PanDome {
    pub edge_mm: f64,
    pub cap_mm: f64,
    pub radius_mm: f64,
}

/// Splits a pan head into its cylindrical edge and spherical crown.
pub fn pan_dome(dk: f64, k: f64, radius: Option<f64>, edge: Option<f64>) -> PanDome {
    let a = dk / 2.0;
    let cap_of = |r: f64| if r > a { r - (r * r - a * a).sqrt() } else { a };
    match (radius, edge) {
        (Some(r), Some(e)) => PanDome { edge_mm: e, cap_mm: cap_of(r), radius_mm: r },
        (Some(r), None) => {
            let cap = cap_of(r);
            PanDome { edge_mm: 0.0f64.max(k - cap), cap_mm: cap.min(k), radius_mm: r }
        }
        (None, Some(e)) => {
            let cap = (k - e).max(0.0);
            PanDome { edge_mm: e.min(k), cap_mm: cap, radius_mm: if cap > 0.0 { (a * a + cap * cap) / (2.0 * cap) } else { 0.0 } }
        }
        (None, None) => {
            let r = 0.8 * dk;
            let cap = cap_of(r);
            PanDome { edge_mm: 0.0f64.max(k - cap), cap_mm: cap.min(k), radius_mm: r }
        }
    }
}

pub fn style_of(t: FastenerType) -> HeadStyle {
    if t.is_washer() {
        HeadStyle::WASHER
    } else if t.is_nut() {
        HeadStyle::NUT
    } else if t.is_stud() || HEADLESS_TYPES.contains(&t) {
        HeadStyle::HEADLESS
    } else if bolt_forging::applies(t) {
        HeadStyle::HEX_TRIMMED
    } else if t == FT::SHCS || t == FT::HEXALOBULAR_SCREW {
        HeadStyle::SOCKET_CAP
    } else if COUNTERSUNK_TYPES.contains(&t) {
        HeadStyle::SOCKET_COUNTERSUNK
    } else if CYLINDRICAL_HEAD_TYPES.contains(&t) {
        HeadStyle::SOCKET_CAP
    } else if t == FT::SOCKET_BUTTON || t == FT::TAPPING_PAN || t == FT::SELF_DRILLING {
        HeadStyle::SOCKET_BUTTON
    } else if t == FT::TWELVE_POINT {
        HeadStyle::TWELVE_POINT
    } else if t == FT::CARRIAGE_BOLT {
        HeadStyle::CARRIAGE
    } else if t == FT::RETAINING_RING {
        HeadStyle::WASHER
    } else if ROUND_PIN_TYPES.contains(&t) {
        HeadStyle::ROUND_PIN
    } else if t.is_unthreaded() {
        HeadStyle::RIVET
    } else {
        HeadStyle::HEX_FORMED
    }
}

/// Features of a fastener type that its modelled shape leaves out.
fn shape_caution(t: FastenerType) -> Option<&'static str> {
    use FastenerType::*;
    Some(match t {
        T_BOLT => "T-head is counted as a hex prism s × k: take the head volume from the drawing.",
        EYE_BOLT => "The eye is counted as a hex prism s × k: the eye and its bore are not modelled.",
        WHEEL_BOLT | WHEEL_NUT => "The conical / spherical seat is not in the volume.",
        BANJO_BOLT => "The axial bore and cross hole are not in the volume (drilled after heading).",
        TTY_HEAD_BOLT => "The reduced (waisted) shank is not modelled: the shank is taken at one diameter.",
        HI_LOK_PIN | TAPER_LOK | LOCKBOLT_PIN | BLIND_BOLT => "Head counted as a plain cylinder; lock grooves, pintail, taper and sleeve are not in the volume.",
        HEXALOBULAR_SCREW => "The hexalobular recess is counted as a hex socket of the same key size.",
        CASTLE_NUT => "The crown slots are not in the volume (milled after forming).",
        ACORN_NUT => "The dome cap and blind bore are not modelled: counted as a through-bored hex.",
        NYLOC_NUT => "The insert collar and nylon ring are not in the volume.",
        WELD_NUT => "The weld projections are not in the volume.",
        NUTPLATE => "The lug plate is not in the volume: nut element only.",
        HI_LOK_COLLAR => "The torque-off wrenching element is not modelled.",
        AERO_LOCK_NUT | METAL_LOCK_NUT => "The locking deflection is not modelled: counted as a plain hex nut.",
        SPRING_WASHER => "Counted as a flat ring: the split and the helix are not in the volume.",
        U_BOLT => "The length is the developed (straight) length before bending.",
        WHEEL_STUD => "The head and the knurl are not modelled: counted as a plain stud.",
        SET_SCREW_CONE | SET_SCREW_DOG | SET_SCREW_CUP | SLOTTED_SET_SCREW => "The point (cone / dog / cup) and the drive recess are not modelled: counted as a plain threaded cylinder.",
        SHOULDER_BOLT => "The ground shoulder is taken at the thread diameter: give the shoulder Ø and length on the drawing.",
        SQUARE_BOLT => "The square head is counted as a hex prism of the same across flats.",
        HANGER_BOLT => "Counted as a plain stud: the wood thread (coarse, different pitch) is not modelled.",
        ANCHOR_BOLT => "The L or J bend is not modelled: the length is the developed (straight) length.",
        LAG_SCREW | CHIPBOARD_SCREW | DRYWALL_SCREW | SELF_DRILLING | TAPPING_PAN | TAPPING_CSK | TAPPING_HEX => {
            "The tapping / wood thread form and the point (or drill tip) are not modelled: the shank is taken at the rolling blank Ø."
        }
        SEMS_SCREW => "The captive washer is not in the volume: screw only.",
        PAN_SLOT | PAN_CROSS | CHEESE_SLOT | CSK_SLOT | CSK_CROSS | RAISED_CSK_CROSS | HEX_WASHER_SCREW => "The slot or cross recess is not in the volume: the head is counted as solid.",
        PAN_HEXALOBULAR => "The hexalobular recess is counted as a circle of the mean of A and B (ISO 10664); a few mm³ either way.",
        CONROD_BOLT => "The necked-down (waisted) shank is not modelled: the shank is taken at one diameter.",
        SHEAR_BOLT => "The reduced head height of a shear bolt is taken from the drawing; no shear-bolt table in the app.",
        TAPER_PIN => "Counted as a plain cylinder at the nominal Ø: the 1:50 taper is not in the volume.",
        SPRING_PIN => "Counted as a solid cylinder: the slot and the rolled wall thickness are not modelled.",
        GROOVED_PIN => "The three grooves are not in the volume: counted as a plain cylinder.",
        SPLIT_PIN => "Counted as a solid cylinder: the split, the eye and the semicircular section are not modelled.",
        RETAINING_RING => "Counted as a flat ring: the gap, lugs and the tapered section are not modelled.",
        THREADED_ROD | TAP_END_STUD => "Counted as a plain threaded rod over the whole length.",
        SQUARE_NUT | T_SLOT_NUT => "The square / T profile is counted as a hex prism of the same width.",
        COUPLING_NUT => "Counted as a long hex nut: the full-length thread is bored through.",
        CAGE_NUT | CLINCH_NUT | RIVET_NUT => "Only the nut body is in the volume: the cage, the clinch ring or the deforming sleeve is not modelled.",
        WING_NUT | WING_SCREW | THUMB_NUT | THUMB_SCREW => "The wings / knurl are counted as a prism of the same envelope: the real profile is not modelled.",
        THIN_NUT | HIGH_NUT => "Nut height m is taken from the type's own standard, not ISO 4032.",
        TOOTH_LOCK_WASHER => "Counted as a flat ring: the teeth are not in the volume.",
        BELLEVILLE_WASHER | WAVE_WASHER => "Counted as a flat ring at the material thickness: the cone / wave is not modelled.",
        TAPER_WASHER => "Counted as a flat ring at the mean thickness: the 8° / 14° taper is not modelled.",
        BLIND_RIVET => "Blind rivet: only the sleeve is modelled, as a solid shank. The sleeve bore and the mandrel are separate parts not covered here.",
        _ => return None,
    })
}

fn n(v: f64) -> String {
    jfmt::f(v, 2)
}
fn nd(v: f64, dp: usize) -> String {
    jfmt::f(v, dp)
}
fn worst(s: &[DimSource]) -> DimSource {
    jfmt::max_by(s.iter().copied(), |x| x.ordinal()).unwrap()
}

struct Builder<'a> {
    table: &'a HeadDimensions,
    segs: Vec<ProfileSegment>,
    items: Vec<VolumeItem>,
    recs: Vec<Recess>,
    from_drawing: Vec<String>,
    not_on_drawing: Vec<String>,
    cautions: Vec<String>,
    hole: Option<f64>,
    head_count: usize,
}

impl<'a> Builder<'a> {
    fn pick_from(&mut self, name: &str, drawn: Option<f64>, fallback: f64, from: &str, from_table: bool) -> (f64, DimSource) {
        match drawn {
            Some(d) if d > 0.0 => {
                self.from_drawing.push(format!("{} {} mm", name, n(d)));
                (d, DimSource::DRAWING)
            }
            _ => {
                self.not_on_drawing.push(format!("{} {} mm — {}", name, n(fallback), from));
                (fallback, if from_table { DimSource::STANDARD } else { DimSource::ESTIMATED })
            }
        }
    }
    fn pick(&mut self, name: &str, drawn: Option<f64>, fallback: f64) -> (f64, DimSource) {
        let (src, tab) = (self.table.source.clone(), self.table.is_table);
        self.pick_from(name, drawn, fallback, &src, tab)
    }
    fn seg(&mut self, s: ProfileSegment, feature: &str, detail: &str, source: DimSource, head: bool) {
        if s.height_mm <= 1e-9 {
            return;
        }
        self.items.push(VolumeItem { feature: feature.into(), detail: detail.into(), volume_mm3: s.volume_mm3(), source });
        self.segs.push(s);
        if head {
            self.head_count += 1;
        }
    }
    fn recess(&mut self, rc: Recess, feature: &str, detail: &str, source: DimSource) {
        self.items.push(VolumeItem { feature: feature.into(), detail: detail.into(), volume_mm3: -rc.volume_mm3(), source });
        self.recs.push(rc);
    }
    fn bore(&mut self, dia: f64, feature: &str, detail: &str, source: DimSource) {
        self.hole = Some(dia);
        let h: f64 = self.segs.iter().map(|s| s.height_mm).sum();
        self.items.push(VolumeItem { feature: feature.into(), detail: detail.into(), volume_mm3: -PI / 4.0 * dia * dia * h, source });
    }
}

#[allow(clippy::too_many_arguments)]
pub fn build(
    t: FastenerType, unit: UnitSystem, d_mm: f64, length_mm: f64, thread_length_mm: f64,
    blank_dia_mm: f64, table: &HeadDimensions, table_socket: Option<(f64, f64)>, g: &DrawingGeometry,
    forging: Option<&BoltForgingResult>, flange: Option<&FlangeGeom>,
) -> FinishedPart {
    let mut style = style_of(t);
    if style == HeadStyle::HEX_TRIMMED && forging.is_none() {
        style = HeadStyle::HEX_FORMED;
    }
    let mut b = Builder {
        table,
        segs: vec![],
        items: vec![],
        recs: vec![],
        from_drawing: vec![],
        not_on_drawing: vec![],
        cautions: vec![],
        hole: None,
        head_count: 0,
    };
    let mut lt = 0.0;
    let thread_src = if g.thread_length_mm.is_some() { DimSource::DRAWING } else { DimSource::STANDARD };
    let note_thread = |b: &mut Builder, len: f64| {
        if g.thread_length_mm.is_some() {
            b.from_drawing.push(format!("thread length b {} mm", n(len)));
        } else {
            b.not_on_drawing.push(format!("thread length b {} mm — standard thread length rule", n(len)));
        }
    };
    let thread_seg = |b: &mut Builder, len: f64, feature: &str| {
        b.seg(ProfileSegment::round(len, blank_dia_mm), feature, &format!("rolling blank Ø{} × {} mm", nd(blank_dia_mm, 3), n(len)), thread_src, false);
    };

    match style {
        HeadStyle::WASHER => {
            let w = dimensions::washer_for(t, unit, d_mm);
            let (od, a) = b.pick_from("washer outside Ø", g.washer_outer_dia_mm, w.outer_dia_mm, &w.source, w.is_table);
            let (id, bb) = b.pick_from("washer bore Ø", g.washer_inner_dia_mm, w.inner_dia_mm, &w.source, w.is_table);
            let (th, c) = b.pick_from("washer thickness", g.washer_thickness_mm, w.thickness_mm, &w.source, w.is_table);
            b.seg(ProfileSegment::round(th, od), "Washer disc", &format!("Ø{} × {} mm", n(od), n(th)), worst(&[a, c]), true);
            b.bore(id.min(0.98 * od), "Bore", &format!("Ø{} × {} mm", n(id), n(th)), worst(&[bb, c]));
            if id >= od {
                b.cautions.push(format!("Bore Ø {} is not smaller than the outside Ø {}: check the drawing values.", n(id), n(od)));
            }
        }
        HeadStyle::NUT => {
            let (s, a) = b.pick("across flats s", g.across_flats_mm, table.across_flats_s);
            let (m, bb) = b.pick("nut height m", g.nut_height_mm.or(g.head_height_mm), table.nut_height_m);
            let table_flange = if t == FT::FLANGE_NUT && unit == UnitSystem::METRIC { dimensions::din6923_flange(d_mm) } else { None };
            let dc = g.flange_dia_mm.or(table_flange.map(|x| x.0));
            let c = g.flange_thickness_mm.or(table_flange.map(|x| x.1));
            match (dc, c) {
                (Some(dc), Some(c)) if c > 0.0 && c < m && dc > s => {
                    if g.flange_dia_mm.is_some() {
                        b.from_drawing.push(format!("flange Ø dc {} mm", n(dc)));
                    } else {
                        b.not_on_drawing.push(format!("flange Ø dc {} mm — DIN 6923 max", n(dc)));
                    }
                    if g.flange_thickness_mm.is_some() {
                        b.from_drawing.push(format!("flange thickness c {} mm", n(c)));
                    } else {
                        b.not_on_drawing.push(format!("flange thickness c {} mm — DIN 6923 min", n(c)));
                    }
                    let flange_src = if g.flange_dia_mm.is_some() && g.flange_thickness_mm.is_some() { DimSource::DRAWING } else { DimSource::STANDARD };
                    b.seg(ProfileSegment::round(c, dc), "Flange", &format!("Ø{} × {} mm", n(dc), n(c)), flange_src, true);
                    if flange_src != DimSource::DRAWING {
                        b.cautions.push("Flange modelled as a disc Ø dc max × c min (DIN 6923); the conical top of the flange is not in the volume, so the weight is slightly low.".into());
                    }
                    b.seg(ProfileSegment::shaped(m - c, s, SectionShape::HEX), "Hex body", &format!("s {} × {} mm", n(s), n(m - c)), worst(&[a, bb]), true);
                }
                _ => {
                    b.seg(ProfileSegment::shaped(m, s, SectionShape::HEX), "Hex body", &format!("s {} × m {} mm", n(s), n(m)), worst(&[a, bb]), true);
                    if t == FT::FLANGE_NUT {
                        b.cautions.push("Flange Ø dc and thickness c are not given: the volume is the hex body without the flange.".into());
                    }
                }
            }
            b.bore(blank_dia_mm, "Threaded bore", &format!("tap drill Ø{} × {} mm", nd(blank_dia_mm, 3), n(m)), DimSource::CALCULATED);
        }
        HeadStyle::HEADLESS => {
            if t == FT::SOCKET_SET {
                lt = length_mm;
                thread_seg(&mut b, length_mm, "Threaded body");
                if let Some((key0, depth0)) = table_socket {
                    let (key, a) = b.pick_from("socket key s", g.socket_size_mm, key0, "ISO 4026 key = d / 2", true);
                    let (depth, bb) = b.pick_from("socket depth t", g.socket_depth_mm, depth0, "typical depth d / 2", false);
                    b.recess(Recess::new(true, key, depth, SectionShape::HEX), "Hex socket", &format!("s {} × t {} mm", nd(key, 1), nd(depth, 1)), worst(&[a, bb]));
                }
            } else {
                lt = thread_length_mm.min(length_mm).max(0.0);
                note_thread(&mut b, lt);
                if lt >= length_mm - 1e-9 {
                    thread_seg(&mut b, length_mm, "Thread section");
                } else {
                    let (ds, a) = b.pick_from("shank Ø", g.shank_dia_mm, d_mm, "nominal diameter", true);
                    if lt <= 1e-9 {
                        b.seg(ProfileSegment::round(length_mm, ds), "Plain body", &format!("Ø{} × {} mm", n(ds), n(length_mm)), a, false);
                    } else {
                        thread_seg(&mut b, lt / 2.0, "Thread section (first end)");
                        b.seg(ProfileSegment::round(length_mm - lt, ds), "Plain shank", &format!("Ø{} × {} mm", n(ds), n(length_mm - lt)), a, false);
                        thread_seg(&mut b, lt / 2.0, "Thread section (second end)");
                    }
                }
            }
        }
        HeadStyle::HEX_TRIMMED => {
            let fg = forging.unwrap();
            lt = fg.thread_length_mm;
            note_thread(&mut b, lt);
            thread_seg(&mut b, lt, "Thread section");
            b.seg(
                ProfileSegment::cone(fg.cone_length_mm, fg.blank_dia_mm, fg.shank_dia_mm),
                "Extrusion cone",
                &format!("15°/side, Ø{} → Ø{} × {} mm", nd(fg.blank_dia_mm, 3), n(fg.shank_dia_mm), n(fg.cone_length_mm)),
                DimSource::CALCULATED,
                false,
            );
            let plain = length_mm - lt - fg.cone_length_mm;
            if plain > 1e-9 {
                if fg.shank_from_drawing {
                    b.from_drawing.push(format!("shank Ø {} mm", n(fg.shank_dia_mm)));
                } else {
                    b.not_on_drawing.push(format!("shank Ø {} mm — wire diameter", n(fg.shank_dia_mm)));
                }
                b.seg(ProfileSegment::round(plain, fg.shank_dia_mm), "Plain shank", &format!("Ø{} × {} mm", n(fg.shank_dia_mm), n(plain)), if fg.shank_from_drawing { DimSource::DRAWING } else { DimSource::STANDARD }, false);
            }
            match flange.filter(|f| f.complete()) {
                Some(fl) => {
                    let used = |b: &mut Builder, name: &str, drawn: Option<f64>, value: f64| -> DimSource {
                        if drawn.is_some() {
                            b.from_drawing.push(format!("{} {} mm", name, n(value)));
                            DimSource::DRAWING
                        } else {
                            b.not_on_drawing.push(format!("{} {} mm — DIN 6921", name, n(value)));
                            DimSource::STANDARD
                        }
                    };
                    let (dc, c, s, k) = (fl.dc.unwrap(), fl.c.unwrap(), fl.s.unwrap(), fl.k.unwrap());
                    let s_dc = used(&mut b, "flange Ø dc", g.flange_dia_mm, dc);
                    let s_c = used(&mut b, "flange thickness c", g.flange_thickness_mm, c);
                    let s_s = used(&mut b, "across flats s", g.across_flats_mm, s);
                    let s_k = used(&mut b, "head height k", g.head_height_mm, k);
                    b.seg(ProfileSegment::round(c, dc), "Flange", &format!("Ø{} × c {} mm", n(dc), n(c)), worst(&[s_dc, s_c]), true);
                    if g.flange_thickness_mm.is_none() {
                        b.cautions.push("Flange modelled as a disc Ø dc × c min (DIN 6921); the conical top of the flange is not in the volume, so the weight is slightly low.".into());
                    }
                    b.seg(ProfileSegment::shaped(k - c, s, SectionShape::HEX), "Hex head", &format!("s {} × {} mm", n(s), n(k - c)), worst(&[s_s, s_k, s_c]), true);
                }
                None => {
                    let (s, a) = b.pick("across flats s", g.across_flats_mm, table.across_flats_s);
                    let (k, bb) = b.pick("head height k", g.head_height_mm, table.head_height_k);
                    b.seg(ProfileSegment::shaped(k, s, SectionShape::HEX), "Hex head", &format!("s {} × k {} mm", n(s), n(k)), worst(&[a, bb]), true);
                }
            }
        }
        _ => {
            let round = ROUND_HEAD_STYLES.contains(&style);
            let (head_size, a) = if round { b.pick("head Ø dk", g.head_dia_mm, table.across_flats_s) } else { b.pick("across flats s", g.across_flats_mm, table.across_flats_s) };
            let (k, bb) = b.pick("head height k", g.head_height_mm, table.head_height_k);
            let under = if style == HeadStyle::SOCKET_COUNTERSUNK { 0.0f64.max(length_mm - k) } else { length_mm };
            let mut neck_h = 0.0;
            let mut neck_src = DimSource::STANDARD;
            if style == HeadStyle::CARRIAGE {
                let from = if table.neck_height_mm.is_some() { table.source.clone() } else { "estimated 0.45 d — take f from the drawing".to_string() };
                let (h, c) = b.pick_from("square neck height f", g.neck_height_mm, table.neck_height_mm.unwrap_or(0.45 * d_mm), &from, table.neck_height_mm.is_some());
                neck_h = h.min(under);
                neck_src = c;
            }
            lt = if t.is_unthreaded() { 0.0 } else { thread_length_mm.min(under - neck_h).max(0.0) };
            let plain = under - neck_h - lt;
            if !t.is_unthreaded() {
                note_thread(&mut b, lt);
            }
            let mut shank_dia = d_mm;
            let mut shank_src = DimSource::STANDARD;
            if plain > 1e-9 {
                let (ds, c) = b.pick_from("shank Ø", g.shank_dia_mm, d_mm, "nominal diameter", true);
                shank_dia = ds;
                shank_src = c;
            }
            thread_seg(&mut b, lt, "Thread section");
            b.seg(ProfileSegment::round(plain, shank_dia), if t.is_unthreaded() { "Rivet shank" } else { "Plain shank" }, &format!("Ø{} × {} mm", n(shank_dia), n(plain)), shank_src, false);
            b.seg(ProfileSegment::shaped(neck_h, d_mm, SectionShape::SQUARE), "Square neck", &format!("{} × {} × {} mm", n(d_mm), n(d_mm), n(neck_h)), neck_src, true);

            let with_flange = matches!((g.flange_dia_mm, g.flange_thickness_mm), (Some(dc), Some(c)) if (style == HeadStyle::HEX_FORMED || style == HeadStyle::TWELVE_POINT) && c > 0.0 && c < k && dc > head_size);
            if with_flange {
                let (dc, c) = (g.flange_dia_mm.unwrap(), g.flange_thickness_mm.unwrap());
                b.from_drawing.push(format!("flange Ø dc {} mm", n(dc)));
                b.from_drawing.push(format!("flange thickness c {} mm", n(c)));
                b.seg(ProfileSegment::round(c, dc), "Flange", &format!("Ø{} × c {} mm", n(dc), n(c)), DimSource::DRAWING, true);
            }
            let head_h = if with_flange { k - g.flange_thickness_mm.unwrap() } else { k };
            let head_src = worst(&[a, bb]);
            match style {
                HeadStyle::TWELVE_POINT => b.seg(ProfileSegment::shaped(head_h, head_size, SectionShape::TWELVE_POINT), "12-point head", &format!("s {} × {} mm", n(head_size), n(head_h)), head_src, true),
                HeadStyle::SOCKET_CAP => {
                    if PAN_DOME_TYPES.contains(&t) {
                        let r = g.head_dome_radius_mm.filter(|x| *x > 0.0);
                        let e = g.head_edge_height_mm.filter(|x| *x > 0.0);
                        let dome = pan_dome(head_size, k, r, e);
                        match r {
                            Some(r) => b.from_drawing.push(format!("crown radius rf {} mm", n(r))),
                            None => b.not_on_drawing.push(format!(
                                "crown radius rf {} mm — {}",
                                n(dome.radius_mm),
                                if e.is_some() { "from k and the edge height" } else { "estimated 0.8 dk (ISO 7045 / 14583 proportion)" }
                            )),
                        }
                        if let Some(e) = e {
                            b.from_drawing.push(format!("edge height {} mm", n(e)));
                        }
                        let dome_src = if r.is_some() || e.is_some() { head_src } else { worst(&[head_src, DimSource::ESTIMATED]) };
                        b.seg(ProfileSegment::round(dome.edge_mm, head_size), "Pan head edge", &format!("Ø{} × {} mm", n(head_size), n(dome.edge_mm)), dome_src, true);
                        b.seg(ProfileSegment::shaped(dome.cap_mm, head_size, SectionShape::DOME), "Pan head crown", &format!("R{} dome on Ø{} × {} mm", n(dome.radius_mm), n(head_size), n(dome.cap_mm)), dome_src, true);
                        let k_model = dome.edge_mm + dome.cap_mm;
                        if let (Some(r), Some(e)) = (r, e) {
                            if g.head_height_mm.is_some() && (k_model - k).abs() > 0.05 {
                                b.cautions.push(format!(
                                    "Crown R{} with edge {} mm on Ø{} gives head height {} mm, not the drawn k {} mm: the volume uses R and the edge; confirm which the head is gauged to.",
                                    n(r), n(e), n(head_size), n(k_model), n(k)
                                ));
                            }
                        }
                    } else {
                        b.seg(ProfileSegment::round(k, head_size), "Cylindrical head", &format!("Ø{} × k {} mm", n(head_size), n(k)), head_src, true);
                    }
                }
                HeadStyle::ROUND_PIN => b.seg(ProfileSegment::round(k, head_size), "Round head", &format!("Ø{} × k {} mm", n(head_size), n(k)), head_src, true),
                HeadStyle::SOCKET_COUNTERSUNK => {
                    let base = if plain > 1e-9 { shank_dia } else { blank_dia_mm };
                    b.seg(ProfileSegment::cone(k, base, head_size), "Countersunk head", &format!("cone Ø{} → Ø{} × k {} mm", n(base), n(head_size), n(k)), head_src, true);
                }
                HeadStyle::SOCKET_BUTTON => b.seg(ProfileSegment::shaped(k, head_size, SectionShape::DOME), "Button head", &format!("dome Ø{} × k {} mm", n(head_size), n(k)), head_src, true),
                HeadStyle::CARRIAGE => b.seg(ProfileSegment::shaped(k, head_size, SectionShape::DOME), "Mushroom head", &format!("dome Ø{} × k {} mm", n(head_size), n(k)), head_src, true),
                HeadStyle::RIVET => b.seg(ProfileSegment::shaped(k, head_size, SectionShape::DOME), "Universal head", &format!("dome Ø{} × k {} mm", n(head_size), n(k)), head_src, true),
                _ => b.seg(ProfileSegment::shaped(head_h, head_size, SectionShape::HEX), "Hex head", &format!("s {} × {} mm", n(head_size), n(head_h)), head_src, true),
            }
            if t == FT::PAN_HEXALOBULAR {
                if let Some((a0, depth0)) = table_socket {
                    let (a_size, x) = b.pick_from("hexalobular drive A", g.socket_size_mm, a0, "ISO 14583 drive size (ISO 10664 nominal A)", true);
                    let (depth, y) = b.pick_from("recess depth t", g.socket_depth_mm, depth0, "estimated 0.27 d — take the recess penetration from the drawing", false);
                    b.recess(Recess::new(true, a_size, depth, SectionShape::HEXALOBULAR), "Hexalobular recess", &format!("A {} × t {} mm", n(a_size), n(depth)), worst(&[x, y]));
                }
            } else if SOCKET_STYLES.contains(&style) {
                if let Some((key0, depth0)) = table_socket {
                    let (key, x) = b.pick("socket key s", g.socket_size_mm, key0);
                    let (depth, y) = b.pick("socket depth t", g.socket_depth_mm, depth0);
                    b.recess(Recess::new(true, key, depth, SectionShape::HEX), "Hex socket", &format!("s {} × t {} mm", nd(key, 1), nd(depth, 1)), worst(&[x, y]));
                }
            }
            if t == FT::FLANGE_BOLT {
                b.cautions.push("Flange Ø dc and thickness c are not both given: the flange is not in the volume.".into());
            }
            if style == HeadStyle::TWELVE_POINT && !with_flange {
                b.cautions.push("The flange / washer face counts only when the drawing gives dc and c.".into());
            }
        }
    }
    if let Some(c) = shape_caution(t) {
        b.cautions.push(c.into());
    }
    FinishedPart {
        style,
        segments: b.segs,
        recesses: b.recs,
        through_hole_mm: b.hole,
        head_segment_count: b.head_count,
        thread_length_mm: lt,
        blank_dia_mm,
        items: b.items,
        from_drawing: b.from_drawing,
        not_on_drawing: b.not_on_drawing,
        cautions: b.cautions,
    }
}
