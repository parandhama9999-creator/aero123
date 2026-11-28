//! Port of HeadingProgression.kt: profile segments, stations, and the cold-heading station sequence.

use crate::bolt_forging::BoltForgingResult;
use crate::gen_enums::{FastenerType, HeadStyle, HeaderChoice, HeaderRoute, SectionShape};
use crate::jfmt::{self, d as dstr};
use crate::part_geometry::FinishedPart;
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

fn hex_area_factor() -> f64 {
    3.0f64.sqrt() / 2.0
}

/// Two hexagons 30° apart (bi-hex): (√3 − 3·tan 15°)·s².
fn twelve_point_area_factor() -> f64 {
    3.0f64.sqrt() - 3.0 * (15.0f64).to_radians().tan()
}

/// Cross-section area; size is the diameter of a round or dome, across flats of a hex / 12-point, the side of a square.
pub fn section_area(shape: SectionShape, size: f64) -> f64 {
    match shape {
        SectionShape::HEX => hex_area_factor() * size * size,
        SectionShape::TWELVE_POINT => twelve_point_area_factor() * size * size,
        SectionShape::SQUARE => size * size,
        SectionShape::HEXALOBULAR => PI / 4.0 * (0.8565 * size) * (0.8565 * size),
        _ => PI / 4.0 * size * size,
    }
}

/// One axial slice of the part, listed from the die end (bottom) to the punch end (top).
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProfileSegment {
    pub height_mm: f64,
    pub d_bottom_mm: f64,
    pub d_top_mm: f64,
    pub shape: SectionShape,
}

impl ProfileSegment {
    pub fn round(h: f64, d: f64) -> Self {
        ProfileSegment { height_mm: h, d_bottom_mm: d, d_top_mm: d, shape: SectionShape::ROUND }
    }
    pub fn cone(h: f64, d_bottom: f64, d_top: f64) -> Self {
        ProfileSegment { height_mm: h, d_bottom_mm: d_bottom, d_top_mm: d_top, shape: SectionShape::ROUND }
    }
    pub fn shaped(h: f64, d: f64, shape: SectionShape) -> Self {
        ProfileSegment { height_mm: h, d_bottom_mm: d, d_top_mm: d, shape }
    }

    pub fn volume_mm3(&self) -> f64 {
        let (h, db, dt) = (self.height_mm, self.d_bottom_mm, self.d_top_mm);
        match self.shape {
            SectionShape::ROUND => PI * h / 12.0 * (db * db + db * dt + dt * dt),
            SectionShape::DOME => PI * h / 6.0 * (3.0 * (db / 2.0) * (db / 2.0) + h * h),
            _ => section_area(self.shape, db) * h,
        }
    }

    /// Width seen in side view; a hex or 12-point is drawn across corners.
    pub fn max_width_mm(&self) -> f64 {
        match self.shape {
            SectionShape::HEX | SectionShape::TWELVE_POINT => self.d_bottom_mm * 2.0 / 3.0f64.sqrt(),
            SectionShape::ROUND => self.d_bottom_mm.max(self.d_top_mm),
            _ => self.d_bottom_mm,
        }
    }

    /// Largest cross-section, which the forming load acts on.
    pub fn cross_section_mm2(&self) -> f64 {
        match self.shape {
            SectionShape::ROUND => section_area(self.shape, self.d_bottom_mm.max(self.d_top_mm)),
            _ => section_area(self.shape, self.d_bottom_mm),
        }
    }
}

/// Blind cavity (indent or socket) entering from the top or bottom face.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Recess {
    pub from_top: bool,
    pub size_mm: f64,
    pub depth_mm: f64,
    pub shape: SectionShape,
}

impl Recess {
    pub fn new(from_top: bool, size_mm: f64, depth_mm: f64, shape: SectionShape) -> Self {
        Recess { from_top, size_mm, depth_mm, shape }
    }
    pub fn volume_mm3(&self) -> f64 {
        section_area(self.shape, self.size_mm) * self.depth_mm
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HeadingStation {
    /// 0 = shear cut-off, 1..n = die stations.
    pub index: i32,
    pub title: String,
    pub operation: String,
    pub segments: Vec<ProfileSegment>,
    pub recesses: Vec<Recess>,
    pub through_hole_mm: Option<f64>,
    pub in_die_mm: f64,
    pub scrap_mm3: f64,
    pub is_peak_blow: bool,
}

impl HeadingStation {
    pub fn new(index: i32, title: &str, operation: &str, segments: Vec<ProfileSegment>) -> Self {
        HeadingStation {
            index,
            title: title.into(),
            operation: operation.into(),
            segments,
            recesses: vec![],
            through_hole_mm: None,
            in_die_mm: 0.0,
            scrap_mm3: 0.0,
            is_peak_blow: false,
        }
    }
    fn in_die(mut self, v: f64) -> Self {
        self.in_die_mm = v;
        self
    }
    fn peak(mut self, v: bool) -> Self {
        self.is_peak_blow = v;
        self
    }
    fn with_recesses(mut self, r: Vec<Recess>) -> Self {
        self.recesses = r;
        self
    }

    pub fn is_cut_off(&self) -> bool {
        self.index == 0
    }
    pub fn height_mm(&self) -> f64 {
        self.segments.iter().map(|s| s.height_mm).sum()
    }
    /// Diameter of the part where it sits in the die; segments run die end first.
    pub fn in_die_dia_mm(&self) -> Option<f64> {
        if self.is_cut_off() { None } else { self.segments.first().map(|s| s.max_width_mm()) }
    }
    /// Bore this station's die is cut to: the stock it holds, plus the transfer clearance.
    pub fn die_bore_mm(&self) -> Option<f64> {
        self.in_die_dia_mm().map(|d| d + stage_clearance(d))
    }
    pub fn max_width_mm(&self) -> f64 {
        self.segments.iter().map(|s| s.max_width_mm()).fold(f64::NEG_INFINITY, f64::max)
    }
    pub fn volume_mm3(&self) -> f64 {
        let segs: f64 = self.segments.iter().map(|s| s.volume_mm3()).sum();
        let recs: f64 = self.recesses.iter().map(|r| r.volume_mm3()).sum();
        segs - recs - self.through_hole_mm.map(|h| section_area(SectionShape::ROUND, h) * self.height_mm()).unwrap_or(0.0)
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OffHeaderOp {
    pub title: String,
    pub operation: String,
}

fn op(t: &str, o: &str) -> OffHeaderOp {
    OffHeaderOp { title: t.into(), operation: o.into() }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HeadingPlan {
    pub slug_dia_mm: f64,
    pub slug_length_mm: f64,
    pub slug_volume_mm3: f64,
    pub stations: Vec<HeadingStation>,
    pub off_header_ops: Vec<OffHeaderOp>,
    /// Upsetting blows the head stock needs (1 ≤ L/D 2.3, 2 ≤ 4.5, else 3).
    pub upset_blows: i32,
    pub notes: Vec<String>,
    pub route: HeaderRoute,
    pub route_reason: String,
    pub feasibility_issue: Option<String>,
}

impl HeadingPlan {
    fn new(wire: f64, len: f64, vol: f64, stations: Vec<HeadingStation>, ops: Vec<OffHeaderOp>, blows: i32, notes: Vec<String>) -> Self {
        HeadingPlan {
            slug_dia_mm: wire,
            slug_length_mm: len,
            slug_volume_mm3: vol,
            stations,
            off_header_ops: ops,
            upset_blows: blows,
            notes,
            route: HeaderRoute::MULTI_STATION,
            route_reason: String::new(),
            feasibility_issue: None,
        }
    }

    pub fn die_stations(&self) -> usize {
        self.stations.iter().filter(|s| !s.is_cut_off()).count()
    }

    /// "1-die 2-blow header", "4-die progressive former".
    pub fn machine_description(&self) -> String {
        match self.route {
            HeaderRoute::MULTI_STATION => format!("{}-die progressive former", self.die_stations()),
            HeaderRoute::NUT_FORMER => format!("{}-die nut former", self.die_stations()),
            HeaderRoute::PRESS => "progressive press".into(),
            HeaderRoute::TWO_BLOW => "1-die 2-blow header".into(),
        }
    }

    /// Largest relative gap between the slug volume and (station volume + scrap removed so far).
    pub fn max_volume_error(&self) -> f64 {
        let mut scrap = 0.0;
        let mut best: Option<f64> = None;
        for s in &self.stations {
            scrap += s.scrap_mm3;
            let e = (s.volume_mm3() + scrap - self.slug_volume_mm3).abs() / self.slug_volume_mm3;
            best = Some(match best {
                Some(b) if b >= e => b,
                _ => e,
            });
        }
        best.unwrap_or(0.0)
    }
}

pub const SINGLE_BLOW_LD: f64 = 2.3;
pub const TWO_BLOW_LD: f64 = 4.5;
pub const MIN_STAGE_CLEARANCE_MM: f64 = 0.02;
pub const MAX_STAGE_CLEARANCE_MM: f64 = 0.09;
const CLEARANCE_MIN_DIA: f64 = 3.0;
const CLEARANCE_MAX_DIA: f64 = 24.0;
pub const THREE_BLOW_LD: f64 = 6.5;
/// Largest open (free) forward extrusion in one blow (shop default).
pub const MAX_DIE_EXTRUSION_PCT: f64 = 30.0;
/// Trapped (contained) forward extrusion limit.
pub const TRAPPED_EXTRUSION_MAX_PCT: f64 = 70.0;

pub fn stage_clearance(dia_mm: f64) -> f64 {
    let t = jfmt::clamp((dia_mm - CLEARANCE_MIN_DIA) / (CLEARANCE_MAX_DIA - CLEARANCE_MIN_DIA), 0.0, 1.0);
    let raw = MIN_STAGE_CLEARANCE_MM + t * (MAX_STAGE_CLEARANCE_MM - MIN_STAGE_CLEARANCE_MM);
    jfmt::rint(raw / 0.01) * 0.01
}

fn f(v: f64) -> String {
    jfmt::f(v, 2)
}
fn fd(v: f64, dp: usize) -> String {
    jfmt::f(v, dp)
}
fn area(d: f64) -> f64 {
    section_area(SectionShape::ROUND, d)
}

/// Inputs of [build] beyond the part itself.
pub struct BuildArgs<'a> {
    pub t: FastenerType,
    pub wire_dia_mm: f64,
    pub part: &'a FinishedPart,
    pub length_mm: f64,
    pub thread_label: &'a str,
    pub forging: Option<&'a BoltForgingResult>,
    pub forged_head_dia_mm: Option<f64>,
    pub trimmed_hex_s: f64,
    pub trimmed_hex_k: f64,
    pub flange_dia_mm: Option<f64>,
    pub flange_thickness_mm: Option<f64>,
    pub choice: HeaderChoice,
    pub wire_fixed: bool,
    pub open_limit: f64,
}

pub fn build(a: &BuildArgs) -> Option<HeadingPlan> {
    let part = a.part;
    if part.style == HeadStyle::WASHER {
        return None;
    }
    if part.style == HeadStyle::NUT {
        return Some(nut(a.t, a.wire_dia_mm, part, a.thread_label));
    }
    let headed = part.style != HeadStyle::HEADLESS && part.head_volume_mm3() > 0.0;
    let fixed = if a.wire_fixed { Some(a.wire_dia_mm) } else { None };
    if headed && a.choice == HeaderChoice::TWO_BLOW {
        return Some(two_blow(a.t, part, a.thread_label, true, fixed, a.open_limit));
    }
    if headed && a.choice == HeaderChoice::AUTO && a.forging.is_none() && two_blow_is_enough(a.t, part, a.wire_dia_mm) {
        return Some(two_blow(a.t, part, a.thread_label, false, fixed, a.open_limit));
    }
    let reason = if part.style == HeadStyle::HEADLESS {
        "Progressive former: the thread sections are extruded and there is no head to upset."
    } else if a.choice == HeaderChoice::MULTI_STATION {
        "Multi-station progressive former (your choice): extrusion, pre-upsets, socket back-extrusion and trimming each run in their own station."
    } else {
        "Progressive former: extrusion, pre-upsets, socket back-extrusion and trimming each run in their own station."
    };
    let mut plan = match (a.forging, a.forged_head_dia_mm) {
        (Some(fg), Some(d_round)) => trimmed_hex(a.t, a.length_mm, fg, d_round, a.trimmed_hex_s, a.trimmed_hex_k, a.thread_label, a.flange_dia_mm, a.flange_thickness_mm, &part.cautions, a.open_limit),
        _ if part.style == HeadStyle::HEADLESS || part.head_volume_mm3() <= 0.0 => headless(a.t, a.wire_dia_mm, part, a.thread_label),
        _ => headed_plan(a.t, a.wire_dia_mm, part, a.thread_label, a.open_limit),
    };
    plan.route = HeaderRoute::MULTI_STATION;
    plan.route_reason = reason.into();
    Some(plan)
}

fn extrusion_notes(reduction_pct: f64, open_limit: f64) -> Vec<String> {
    let mut v = vec![];
    if reduction_pct > TRAPPED_EXTRUSION_MAX_PCT {
        v.push(format!("Extrusion {} % is over the ~{} % trapped-extrusion limit: extrude in two stations or choose a thinner wire.", fd(reduction_pct, 0), TRAPPED_EXTRUSION_MAX_PCT as i32));
    } else if reduction_pct > open_limit {
        v.push(format!("Extrusion {} % is over your {} % open-extrusion limit: extrude in a trapped die (billet held in the bore) to avoid upsetting the shank.", fd(reduction_pct, 0), fd(open_limit, 0)));
    }
    v
}

/// Auto: one die is enough when nothing is extruded, the head needs ≤ 2 blows and is formed, not trimmed.
fn two_blow_is_enough(t: FastenerType, part: &FinishedPart, wire: f64) -> bool {
    let lt = part.thread_length_mm;
    let extrude = lt > 0.0 && (1.0 - area(part.blank_dia_mm) / area(wire)) * 100.0 >= 1.0;
    let ld = part.head_volume_mm3() / area(wire) / wire;
    let upset_is_final = part.style == HeadStyle::RIVET || part.style == HeadStyle::ROUND_PIN;
    !extrude && ld <= TWO_BLOW_LD && !upset_is_final && (t.category() == "Machine Screws" || part.style == HeadStyle::HEX_FORMED)
}

fn two_blow(t: FastenerType, part: &FinishedPart, thread_label: &str, chosen: bool, fixed_wire: Option<f64>, open_limit: f64) -> HeadingPlan {
    let body = part.die_segments();
    let in_die: f64 = body.iter().map(|s| s.height_mm).sum();
    let head_vol = part.head_volume_mm3();
    let min_d = body.iter().map(|s| s.d_bottom_mm.min(s.d_top_mm)).fold(None, |a: Option<f64>, x| Some(a.map_or(x, |a| if x < a { x } else { a }))).unwrap_or(part.blank_dia_mm);
    let max_d = body.iter().map(|s| s.d_bottom_mm.max(s.d_top_mm)).fold(None, |a: Option<f64>, x| Some(a.map_or(x, |a| if x > a { x } else { a }))).unwrap_or(part.blank_dia_mm);
    let ld_at = |w: f64| head_vol / area(w) / w;
    let reduction_at = |w: f64| (1.0 - area(min_d) / area(w)) * 100.0;
    let mut wire = fixed_wire.unwrap_or(((max_d * 100.0 - 1e-6).ceil()) / 100.0);
    let mut issue: Option<String> = None;
    let mut enlarged = false;
    if fixed_wire.is_some() && ld_at(wire) > TWO_BLOW_LD {
        issue = Some(format!("the head stock L/D is {} on your Ø{} wire, more than two blows can gather ({})", f(ld_at(wire)), f(wire), dstr(TWO_BLOW_LD)));
    } else if ld_at(wire) > TWO_BLOW_LD {
        let need = ((4.0 * head_vol / (PI * TWO_BLOW_LD)).cbrt() / 0.05 - 1e-9).ceil() * 0.05;
        if reduction_at(need) <= open_limit {
            wire = need;
            enlarged = true;
        } else {
            issue = Some(format!(
                "the head stock L/D is {} on Ø{} wire, more than two blows can gather ({}); wire large enough for two blows (Ø{}) would have to be extruded {} % in the die, over the ~{} % one die takes",
                f(ld_at(wire)), f(wire), dstr(TWO_BLOW_LD), f(need), fd(reduction_at(need), 0), open_limit as i32
            ));
        }
    }
    let reduction_pct = reduction_at(wire).max(0.0);
    if issue.is_none() && reduction_pct > open_limit {
        issue = Some(format!("extruding Ø{} → Ø{} is {} % area reduction, over the ~{} % one die takes in the first blow", f(wire), f(min_d), fd(reduction_pct, 0), open_limit as i32));
    }
    let aw = area(wire);
    let slug_vol = part.volume_mm3();
    let slug_len = slug_vol / aw;
    let stock = head_vol / aw;
    let ld = stock / wire;
    let heads = part.head_segments();
    let top = heads.last().unwrap();
    let prism = top.shape == SectionShape::HEX || top.shape == SectionShape::TWELVE_POINT;
    let d_cavity = if prism { 0.96 * top.d_bottom_mm } else { top.max_width_mm() };
    let mut hc = stock * 0.62;
    let mut dc = cone_large_end(head_vol, wire, hc);
    if dc > 0.9 * d_cavity {
        dc = 0.9 * d_cavity;
        hc = 12.0 * head_vol / (PI * (dc * dc + dc * wire + wire * wire));
    }
    let extrude = reduction_pct >= 1.0;
    let mut stations = vec![cut_off(wire, slug_len)];
    let mut segs1 = body.to_vec();
    segs1.push(ProfileSegment::cone(hc, dc, wire));
    stations.push(
        HeadingStation::new(
            1,
            if extrude { "1st blow: extrude & cone preform" } else { "1st blow: cone preform" },
            &(if extrude { format!("Forward-extrude the shank in the closed die Ø{} → Ø{} ({} % area reduction); ", f(wire), fd(min_d, 3), fd(reduction_pct, 0)) } else { String::new() }
                + &format!("gather head stock {} mm (L/D {}) in the cone punch: Ø{} → Ø{} × {} mm", fd(stock, 1), f(ld), f(dc), f(wire), fd(hc, 1))),
            segs1,
        )
        .in_die(in_die),
    );
    let (title, operation) = finishing_blow(t, part);
    stations.push(
        HeadingStation::new(2, &format!("2nd blow: {}", title), &operation, part.segments.clone())
            .with_recesses(part.recesses.clone())
            .in_die(in_die)
            .peak(true),
    );
    let mut notes = vec![];
    if let Some(i) = &issue {
        notes.push(format!("NOT FEASIBLE on a 1-die 2-blow header: {}. Choose Multi-station, or change the head / wire.", i));
    }
    if enlarged {
        notes.push(format!("Wire enlarged to Ø{} so the head gathers in two blows; the shank is extruded back to Ø{} in the first blow.", f(wire), fd(min_d, 3)));
    } else if !extrude && part.thread_length_mm > 0.0 {
        notes.push(format!("Wire is drawn to the rolling blank Ø{} mm: the shank is not extruded, the thread is rolled straight onto it.", fd(min_d, 3)));
    }
    if prism {
        notes.push(format!("Formed head, no trim: the {} is closed in the punch insert, so there is no trim cheese; corners are slightly fuller than a trimmed head.", if top.shape == SectionShape::HEX { "hex" } else { "12-point" }));
    }
    if part.recesses.iter().any(|r| r.from_top) {
        notes.push("The recess is back-extruded in the finishing blow.".into());
    }
    notes.extend(part.cautions.iter().cloned());
    let reason = if let Some(i) = &issue {
        format!("1-die 2-blow header (your choice) cannot make this part as drawn: {}.", i)
    } else if chosen {
        format!(
            "1-die 2-blow header (your choice): {}, head{} finished in the second.",
            if extrude { "shank extruded in the die and head gathered in the first blow" } else { "head gathered in the first blow" },
            if prism { " formed (no trim)" } else { "" }
        )
    } else {
        format!("1-die 2-blow header (auto): no extrusion and head L/D {} ≤ {}, so one die with a cone blow and a finishing blow makes the part. Choose Multi-station to run it on the progressive former.", f(ld), dstr(TWO_BLOW_LD))
    };
    let mut plan = HeadingPlan::new(wire, slug_len, slug_vol, stations, off_header_ops(t, thread_label), 2, notes);
    plan.route = HeaderRoute::TWO_BLOW;
    plan.route_reason = reason;
    plan.feasibility_issue = issue;
    plan
}

fn cut_off(wire: f64, length: f64) -> HeadingStation {
    HeadingStation::new(0, "Shear cut-off", &format!("Shear slug Ø{} × {} mm from wire coil", f(wire), fd(length, 1)), vec![ProfileSegment::round(length, wire)])
}

/// Work done on the blank after it leaves the header: differs by fastener type.
fn off_header_ops(t: FastenerType, thread_label: &str) -> Vec<OffHeaderOp> {
    use FastenerType::*;
    let point = op("Pointing", "Chamfer thread start to minor diameter (45°)");
    let roll = op("Thread rolling", &format!("Roll {} on flat or cylindrical dies", thread_label));
    let roll_after_ht = op("Thread rolling", &format!("Roll {} after heat treatment (controlled root radius)", thread_label));
    let fillet_roll = op("Fillet rolling", "Cold-roll the head-to-shank fillet after heat treatment");
    let tap = op("Tapping", &format!("Tap {} on bent-shank nut tapper", thread_label));
    let deflect = op("Locking deflection", "Deflect the top threads (oval / tri-lobe) for the prevailing torque");
    match t {
        SOLID_RIVET | BLIND_RIVET => vec![],
        SOCKET_SET => vec![op("Point forming", "Form the cup / cone / dog point on the end opposite the socket"), roll],
        TWELVE_POINT => vec![point, roll_after_ht, fillet_roll],
        NAS_CLOSE_TOL_BOLT => vec![op("Shank grinding", "Centreless grind the shank to the close-tolerance diameter"), point, roll_after_ht, fillet_roll],
        HI_LOK_PIN => vec![
            op("Shank grinding", "Centreless grind the shank to the interference-fit diameter"),
            roll_after_ht,
            op("Key recess", "Broach the hex key recess in the threaded end"),
        ],
        TAPER_LOK => vec![op("Taper grinding", "Grind the tapered shank to the OEM taper"), roll_after_ht],
        LOCKBOLT_PIN => vec![
            op("Groove rolling", "Roll the annular lock grooves and the pintail pull grooves"),
            op("Breakneck", "Form the breakneck groove between the lock grooves and the pintail"),
        ],
        BLIND_BOLT => vec![point, roll, op("Assembly", "Assemble the core bolt with its sleeve and drive nut")],
        BANJO_BOLT => vec![op("Drilling", "Drill the axial bore and the cross hole"), point, roll],
        THREAD_FORMING_SCREW => vec![op("Thread rolling", &format!("Roll the trilobular thread-forming thread {} with its tapered lead-in", thread_label))],
        EYE_BOLT => vec![op("Eye piercing", "Pierce and size the eye bore"), point, roll],
        T_BOLT => vec![op("Head trimming", "Trim two sides of the head to the T shape"), point, roll],
        STUD => vec![op("Pointing", "Chamfer both ends to minor diameter (45°)"), op("Thread rolling", &format!("Roll {} on both ends", thread_label))],
        WHEEL_STUD => vec![op("Knurling", "Roll the press-fit serration on the collar"), point, roll],
        U_BOLT => vec![
            op("Pointing", "Chamfer both ends to minor diameter (45°)"),
            op("Thread rolling", &format!("Roll {} on both ends", thread_label)),
            op("Bending", "Bend to the U form"),
        ],
        NYLOC_NUT => vec![tap, op("Insert assembly", "Press the polyamide ring into the collar and crimp it")],
        METAL_LOCK_NUT | AERO_LOCK_NUT => vec![tap, deflect],
        CASTLE_NUT => vec![op("Slot milling", "Mill the six slots in the crown"), tap],
        ACORN_NUT => vec![op("Tapping", &format!("Tap {} blind with a bottoming tap", thread_label))],
        HI_LOK_COLLAR => vec![tap, op("Torque-off groove", "Machine the break groove under the wrenching hex")],
        NUTPLATE => vec![tap, op("Plate assembly", "Stamp the lug plate and swage the nut element into it")],
        _ => {
            if t.is_nut() {
                vec![tap]
            } else {
                vec![point, roll]
            }
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn trimmed_hex(
    t: FastenerType, length: f64, fg: &BoltForgingResult, d_round: f64, s: f64, k: f64, thread_label: &str,
    flange_dc: Option<f64>, flange_c: Option<f64>, cautions: &[String], open_limit: f64,
) -> HeadingPlan {
    let is_flange = flange_dc.is_some() && flange_c.is_some();
    let wire = fg.shank_dia_mm;
    let aw = area(wire);
    let cheese = fg.cheese_volume_mm3.unwrap();
    let head_forged = fg.head_stock_mm.unwrap() * aw;
    let slug_vol = fg.finished_volume_mm3.unwrap() + cheese;
    let slug_len = slug_vol / aw;
    let stock = fg.head_stock_mm.unwrap();
    let ld = stock / wire;
    let blows = blows_for(ld);
    let lt = fg.thread_length_mm;
    let mut shank: Vec<ProfileSegment> = vec![];
    if lt > 0.0 {
        shank.push(ProfileSegment::round(lt, fg.blank_dia_mm));
    }
    if fg.cone_length_mm > 0.0 {
        shank.push(ProfileSegment::cone(fg.cone_length_mm, fg.blank_dia_mm, wire));
    }
    let body = length - lt - fg.cone_length_mm;
    if body > 0.0 {
        shank.push(ProfileSegment::round(body, wire));
    }
    let with = |extra: Vec<ProfileSegment>| -> Vec<ProfileSegment> {
        let mut v = shank.clone();
        v.extend(extra);
        v
    };
    let reduction_pct = (1.0 - area(fg.blank_dia_mm) / aw) * 100.0;
    let mut stations = vec![cut_off(wire, slug_len)];
    stations.push(
        HeadingStation::new(
            1,
            "Square & forward extrude",
            &(format!("Square sheared ends; extrude thread section Ø{} → Ø{} × {} mm ", f(wire), fd(fg.blank_dia_mm, 3), fd(lt, 1))
                + &(if fg.cone_length_mm > 0.01 {
                    format!("through 15°/side cone ({} mm, {} % area reduction)", f(fg.cone_length_mm), fd(reduction_pct, 0))
                } else {
                    format!("up to the die face, full length ({} % area reduction)", fd(reduction_pct, 0))
                })
                + &format!("; stock before extrusion {} mm; head stock {} mm, L/D {}", f(fg.stock_to_extrude_mm), fd(stock, 1), f(ld))),
            with(vec![ProfileSegment::round(stock, wire)]),
        )
        .in_die(length),
    );
    let cone_fractions: Vec<f64> = match blows {
        1 => vec![],
        2 => vec![0.62],
        _ => vec![0.75, 0.52],
    };
    for (i, fraction) in cone_fractions.iter().enumerate() {
        let mut hc = stock * fraction;
        let mut dc = cone_large_end(head_forged, wire, hc);
        if dc > 0.9 * d_round {
            dc = 0.9 * d_round;
            hc = 12.0 * head_forged / (PI * (dc * dc + dc * wire + wire * wire));
        }
        let idx = stations.len() as i32;
        stations.push(
            HeadingStation::new(
                idx,
                &(if cone_fractions.len() > 1 { format!("Cone pre-upset {}", i + 1) } else { "Cone pre-upset".into() }),
                &format!("Gather head stock in cone punch: Ø{} → Ø{} × {} mm (L/D {} > {})", f(dc), f(wire), fd(hc, 1), f(ld), dstr(SINGLE_BLOW_LD)),
                with(vec![ProfileSegment::cone(hc, dc, wire)]),
            )
            .in_die(length),
        );
    }
    if is_flange {
        let c = flange_c.unwrap();
        let dc = flange_dc.unwrap();
        let hex_part = ProfileSegment::shaped(k - c, s, SectionShape::HEX);
        let d_pre = 0.95 * dc;
        let idx = stations.len() as i32;
        stations.push(
            HeadingStation::new(
                idx,
                "Head upset",
                &format!("Upset round head Ø{} × {} mm against die face, to enter the flange cavity", f(d_pre), f(head_forged / area(d_pre))),
                with(vec![ProfileSegment::round(head_forged / area(d_pre), d_pre)]),
            )
            .in_die(length),
        );
        let idx = stations.len() as i32;
        stations.push(
            HeadingStation::new(
                idx,
                "Hex & flange form",
                &format!("Coin flange Ø{} × c={} mm in the die cavity and close hex s={} × {} mm in the punch hex insert; formed to size, no trim", f(dc), f(c), f(s), f(k - c)),
                with(vec![ProfileSegment::round(c, dc), hex_part]),
            )
            .in_die(length)
            .peak(true),
        );
    } else {
        let idx = stations.len() as i32;
        stations.push(
            HeadingStation::new(
                idx,
                "Head upset (round, for trimming)",
                &format!("Upset round head Ø{} (1.20 × across flats) × {} mm against die face", f(d_round), f(head_forged / area(d_round))),
                with(vec![ProfileSegment::round(head_forged / area(d_round), d_round)]),
            )
            .in_die(length)
            .peak(true),
        );
        let idx = stations.len() as i32;
        let mut st = HeadingStation::new(
            idx,
            "Trim to hex",
            &format!("Trim round head to hex s={} × k={} mm; trim cheese {} mm³ removed", f(s), f(k), fd(cheese, 0)),
            with(vec![ProfileSegment::shaped(k, s, SectionShape::HEX)]),
        )
        .in_die(length);
        st.scrap_mm3 = cheese;
        stations.push(st);
    }
    let mut notes = vec![];
    if ld > THREE_BLOW_LD {
        notes.push(format!("Head stock L/D {} exceeds the 3-blow limit ({}): use larger wire or warm forming.", f(ld), dstr(THREE_BLOW_LD)));
    }
    notes.extend(extrusion_notes(reduction_pct, open_limit));
    notes.extend(cautions.iter().cloned());
    HeadingPlan::new(wire, slug_len, slug_vol, stations, off_header_ops(t, thread_label), blows, notes)
}

/// Blows the free head stock needs: 1 up to L/D 2.3, 2 up to 4.5, else 3.
fn blows_for(ld: f64) -> i32 {
    if ld <= SINGLE_BLOW_LD {
        1
    } else if ld <= TWO_BLOW_LD {
        2
    } else {
        3
    }
}

/// Title and operation of the blow that finishes the head, by fastener type.
fn finishing_blow(t: FastenerType, part: &FinishedPart) -> (String, String) {
    let head = part.head_segments();
    let top_i = head.len() - 1;
    let top = &head[top_i];
    let size = f(if top.shape == SectionShape::ROUND { top.d_bottom_mm.max(top.d_top_mm) } else { top.d_bottom_mm });
    let k = f(top.height_mm);
    let socket = part.recesses.iter().find(|r| r.from_top).map(|r| format!("hex socket s={} × t={} mm", fd(r.size_mm, 1), fd(r.depth_mm, 1)));
    let socket_s = socket.unwrap_or_else(|| "null".into());
    let flange = head
        .iter()
        .enumerate()
        .find(|(i, s)| *i != top_i && s.shape == SectionShape::ROUND)
        .map(|(_, s)| format!(", flange Ø{} × c={} mm on the die face", f(s.d_bottom_mm), f(s.height_mm)))
        .unwrap_or_default();
    let hex = format!("Close hex s={} × k={} mm in punch hex insert, 30° chamfer, bearing face", size, k);
    let p = |a: &str, b: String| (a.to_string(), b);
    match part.style {
        HeadStyle::SOCKET_CAP => {
            let rec = part.recesses.iter().find(|r| r.from_top);
            if top.shape == SectionShape::DOME {
                let edge = if head.len() >= 2 { Some(&head[head.len() - 2]).filter(|s| s.shape == SectionShape::ROUND) } else { None };
                let k_all = f(head.iter().map(|s| s.height_mm).sum());
                let crown = format!("pan head Ø{} × k={} mm (edge {} mm + crown {} mm) in the crowned punch", size, k_all, f(edge.map(|e| e.height_mm).unwrap_or(0.0)), f(top.height_mm));
                match rec {
                    None => p("Pan head form", format!("Form {}, under-head fillet in the die", crown)),
                    Some(r) if r.shape == SectionShape::HEXALOBULAR => p("Pan head & hexalobular recess", format!("Form {}; back-extrude hexalobular recess A={} × t={} mm with the recess pin", crown, f(r.size_mm), f(r.depth_mm))),
                    Some(r) => p("Pan head & recess", format!("Form {}; back-extrude recess {} × t={} mm", crown, fd(r.size_mm, 1), fd(r.depth_mm, 1))),
                }
            } else if let Some(r) = rec {
                p(
                    "Socket back-extrude",
                    (if t == FastenerType::HEXALOBULAR_SCREW {
                        format!("Back-extrude hexalobular recess (volume modelled as hex s={})", fd(r.size_mm, 1))
                    } else {
                        format!("Back-extrude hex socket s={}", fd(r.size_mm, 1))
                    }) + &format!(" × t={} mm; finish head Ø{} × k={} mm", fd(r.depth_mm, 1), size, k),
                )
            } else {
                p("Cylindrical head form", format!("Close cylindrical head Ø{} × k={} mm in the punch cavity, bearing face and edge radius", size, k))
            }
        }
        HeadStyle::SOCKET_COUNTERSUNK => {
            let angle = 2.0 * ((top.d_top_mm - top.d_bottom_mm) / 2.0 / top.height_mm).atan().to_degrees();
            p("Countersunk head & socket", format!("Form countersunk head Ø{} × k={} mm ({}° included) in the die countersink; back-extrude {}", size, k, fd(angle, 0), socket_s))
        }
        HeadStyle::SOCKET_BUTTON => p("Button head & socket", format!("Form dome head Ø{} × k={} mm in the cupped punch; back-extrude {}", size, k, socket_s)),
        HeadStyle::TWELVE_POINT => p("12-point form", format!("Close 12-point (bi-hex) s={} × k={} mm in the punch insert{}", size, k, flange)),
        HeadStyle::CARRIAGE => p(
            "Mushroom head & square neck",
            format!("Form dome head Ø{} × k={} mm in the cupped punch", size, k)
                + &head
                    .iter()
                    .find(|s| s.shape == SectionShape::SQUARE)
                    .map(|s| format!("; square neck {} × {} × {} mm fills the square die recess", f(s.d_bottom_mm), f(s.d_bottom_mm), f(s.height_mm)))
                    .unwrap_or_default(),
        ),
        HeadStyle::RIVET => p("Head upset (universal head)", format!("Form dome head Ø{} × k={} mm in the cupped punch in one blow", size, k)),
        HeadStyle::ROUND_PIN => (
            match t {
                FastenerType::HI_LOK_PIN => "Head upset (Hi-Lok head)",
                FastenerType::TAPER_LOK => "Head upset (Taper-Lok head)",
                FastenerType::LOCKBOLT_PIN => "Head upset (lockbolt head)",
                _ => "Head upset (core bolt head)",
            }
            .to_string(),
            format!("Upset round head Ø{} × k={} mm against die face", size, k),
        ),
        _ => match t {
            FastenerType::T_BOLT => p("T-head form", format!("Flatten the upset into the T-head blank (counted as hex s={} × k={} mm); two sides are trimmed off the header", size, k)),
            FastenerType::EYE_BOLT => p("Eye blank form", format!("Flatten the upset into the eye blank (counted as hex s={} × k={} mm); the eye is pierced off the header", size, k)),
            FastenerType::WHEEL_BOLT => p("Hex & seat form", format!("{}; form the conical / spherical seat in the die", hex)),
            FastenerType::TTY_HEAD_BOLT => p("Hex flange head form", format!("{}{}", hex, flange)),
            FastenerType::THREAD_FORMING_SCREW => p("Hex washer head form", format!("{}{}", hex, flange)),
            _ => p(if !flange.is_empty() { "Hex & flange form" } else { "Hex form" }, format!("{}{}", hex, flange)),
        },
    }
}

fn headed_plan(t: FastenerType, wire: f64, part: &FinishedPart, thread_label: &str, open_limit: f64) -> HeadingPlan {
    let aw = area(wire);
    let slug_vol = part.volume_mm3();
    let slug_len = slug_vol / aw;
    let body = part.die_segments();
    let in_die: f64 = body.iter().map(|s| s.height_mm).sum();
    let head_vol = part.head_volume_mm3();
    let stock = head_vol / aw;
    let ld = stock / wire;
    let blows = blows_for(ld);
    let lt = part.thread_length_mm;
    let blank = part.blank_dia_mm;
    let reduction_pct = if lt > 0.0 { (1.0 - area(blank) / aw) * 100.0 } else { 0.0 };
    let heads = part.head_segments();
    let top = heads.last().unwrap();
    let head_size = if top.shape == SectionShape::ROUND { top.d_bottom_mm.max(top.d_top_mm) } else { top.d_bottom_mm };
    let has_socket = part.recesses.iter().any(|r| r.from_top);
    let upset_is_final = part.style == HeadStyle::RIVET || part.style == HeadStyle::ROUND_PIN;
    let d_round = match part.style {
        HeadStyle::SOCKET_CAP => 0.97 * head_size,
        HeadStyle::SOCKET_COUNTERSUNK | HeadStyle::SOCKET_BUTTON | HeadStyle::CARRIAGE => 0.90 * head_size,
        HeadStyle::RIVET | HeadStyle::ROUND_PIN => head_size,
        _ => 0.96 * head_size,
    };
    let with = |extra: ProfileSegment| -> Vec<ProfileSegment> {
        let mut v = body.to_vec();
        v.push(extra);
        v
    };
    let mut stations = vec![cut_off(wire, slug_len)];
    let extrude = lt > 0.0 && reduction_pct >= 1.0;
    stations.push(
        HeadingStation::new(
            1,
            if extrude { "Square & forward extrude" } else { "Square ends" },
            &if extrude {
                format!("Square sheared ends; extrude thread section Ø{} → Ø{} × {} mm ({} % area reduction); head stock {} mm, L/D {}", f(wire), fd(blank, 3), fd(lt, 1), fd(reduction_pct, 0), fd(stock, 1), f(ld))
            } else if lt > 0.0 {
                format!("Square sheared ends; wire is at the rolling blank Ø{}, so the shank is not extruded; head stock {} mm, L/D {}", fd(blank, 3), fd(stock, 1), f(ld))
            } else {
                format!("Square sheared ends; head stock {} mm, L/D {}", fd(stock, 1), f(ld))
            },
            with(ProfileSegment::round(stock, wire)),
        )
        .in_die(in_die),
    );
    let cone_fractions: Vec<f64> = match blows {
        1 => vec![],
        2 => vec![0.62],
        _ => vec![0.75, 0.52],
    };
    for (i, fraction) in cone_fractions.iter().enumerate() {
        let mut hc = stock * fraction;
        let mut dc = cone_large_end(head_vol, wire, hc);
        if dc > 0.9 * d_round {
            dc = 0.9 * d_round;
            hc = 12.0 * head_vol / (PI * (dc * dc + dc * wire + wire * wire));
        }
        let idx = stations.len() as i32;
        stations.push(
            HeadingStation::new(
                idx,
                &(if cone_fractions.len() > 1 { format!("Cone pre-upset {}", i + 1) } else { "Cone pre-upset".into() }),
                &format!("Gather head stock in cone punch: Ø{} → Ø{} × {} mm (L/D {} > {})", f(dc), f(wire), fd(hc, 1), f(ld), dstr(SINGLE_BLOW_LD)),
                with(ProfileSegment::cone(hc, dc, wire)),
            )
            .in_die(in_die),
        );
    }
    if !upset_is_final {
        let h_round = head_vol / area(d_round);
        let idx = stations.len() as i32;
        stations.push(
            HeadingStation::new(idx, "Head upset", &format!("Upset round head Ø{} × {} mm against die face", f(d_round), f(h_round)), with(ProfileSegment::round(h_round, d_round)))
                .in_die(in_die)
                .peak(has_socket),
        );
    }
    let (title, operation) = finishing_blow(t, part);
    let idx = stations.len() as i32;
    stations.push(
        HeadingStation::new(idx, &title, &operation, part.segments.clone())
            .with_recesses(part.recesses.clone())
            .in_die(in_die)
            .peak(!has_socket),
    );
    let mut notes = vec![];
    if ld > THREE_BLOW_LD {
        notes.push(format!("Head stock L/D {} exceeds the 3-blow limit ({}): use larger wire or warm forming.", f(ld), dstr(THREE_BLOW_LD)));
    }
    notes.extend(extrusion_notes(reduction_pct, open_limit));
    notes.extend(part.cautions.iter().cloned());
    HeadingPlan::new(wire, slug_len, slug_vol, stations, off_header_ops(t, thread_label), blows, notes)
}

fn headless(t: FastenerType, wire: f64, part: &FinishedPart, thread_label: &str) -> HeadingPlan {
    let slug_vol = part.volume_mm3();
    let slug_len = slug_vol / area(wire);
    let blank = part.blank_dia_mm;
    let mut stations = vec![cut_off(wire, slug_len)];
    if let Some(socket) = part.recesses.first() {
        let solid_len = slug_vol / area(blank);
        stations.push(
            HeadingStation::new(1, "Square & extrude", &format!("Square ends; extrude full length Ø{} → Ø{} mm", f(wire), fd(blank, 3)), vec![ProfileSegment::round(solid_len, blank)])
                .in_die(solid_len),
        );
        stations.push(
            HeadingStation::new(
                2,
                "Socket back-extrude",
                &format!("Back-extrude hex socket s={} × t={} mm; part grows to L={} mm", fd(socket.size_mm, 1), fd(socket.depth_mm, 1), fd(part.height_mm(), 1)),
                part.segments.clone(),
            )
            .with_recesses(part.recesses.clone())
            .in_die(part.height_mm())
            .peak(true),
        );
    } else {
        let threaded = part.thread_length_mm > 0.0;
        stations.push(
            HeadingStation::new(
                1,
                if threaded { "Square & extrude ends" } else { "Square ends" },
                &if threaded { format!("Square ends; extrude thread sections Ø{} → Ø{} mm", f(wire), fd(blank, 3)) } else { "Square both sheared ends".into() },
                part.segments.clone(),
            )
            .in_die(part.height_mm())
            .peak(true),
        );
    }
    HeadingPlan::new(wire, slug_len, slug_vol, stations, off_header_ops(t, thread_label), 0, part.cautions.clone())
}

fn nut(t: FastenerType, wire: f64, part: &FinishedPart, thread_label: &str) -> HeadingPlan {
    use FastenerType::*;
    let hex_i = part.segments.len() - 1;
    let hex_seg = &part.segments[hex_i];
    let s = hex_seg.d_bottom_mm;
    let m = part.height_mm();
    let hole = part.through_hole_mm.unwrap();
    let nut_vol = part.volume_mm3();
    let web = 0.8f64.max(0.25 * hole).min(0.4 * m);
    let web_vol = area(hole) * web;
    let slug_vol = nut_vol + web_vol;
    let slug_len = slug_vol / area(wire);
    let indent_final = (m - web) / 2.0;
    let d_round = 0.96 * s;
    let d1 = if d_round > wire { wire + 0.35 * (d_round - wire) } else { wire };
    let h1 = slug_vol / area(d1);
    let indent2 = 0.4 * indent_final;
    let h2 = (slug_vol + 2.0 * area(hole) * indent2) / area(d_round);
    let flange = part
        .segments
        .iter()
        .enumerate()
        .find(|(i, sg)| *i != hex_i && sg.shape == SectionShape::ROUND)
        .map(|(_, sg)| format!(", flange Ø{} × c={} mm", f(sg.d_bottom_mm), f(sg.height_mm)))
        .unwrap_or_default();
    let form_title = match t {
        FLANGE_NUT => "Hex & flange form",
        NYLOC_NUT => "Hex & collar form",
        CASTLE_NUT => "Hex & crown form",
        ACORN_NUT => "Hex & dome form",
        WELD_NUT => "Hex & weld projection form",
        WHEEL_NUT => "Hex & seat form",
        AERO_LOCK_NUT => "Reduced hex form",
        HI_LOK_COLLAR => "Collar form",
        NUTPLATE => "Nut element form",
        _ => "Hex form",
    };
    let form_extra = match t {
        NYLOC_NUT => "; raise the collar that holds the insert",
        CASTLE_NUT => "; raise the round crown for the slots",
        ACORN_NUT => "; raise the dome cap",
        WELD_NUT => "; coin the weld projections",
        WHEEL_NUT => "; form the conical / spherical seat",
        HI_LOK_COLLAR => "; form the wrenching hex above the collar",
        _ => "",
    };
    let mut pierce = HeadingStation::new(4, "Pierce", &format!("Punch out web Ø{} × {} mm ({} cm³ scrap); bore = tap drill", f(hole), f(web), fd(web_vol / 1000.0, 3)), part.segments.clone());
    pierce.through_hole_mm = Some(hole);
    pierce.scrap_mm3 = web_vol;
    let stations = vec![
        cut_off(wire, slug_len),
        HeadingStation::new(1, "Square & pre-upset", &format!("Flatten sheared ends, upset to Ø{} × {} mm", f(d1), f(h1)), vec![ProfileSegment::round(h1, d1)]),
        HeadingStation::new(
            2,
            "Pre-form & indent",
            &format!("Upset round Ø{} × {} mm, start indents Ø{} × {} mm both faces", f(d_round), f(h2), f(hole), f(indent2)),
            vec![ProfileSegment::round(h2, d_round)],
        )
        .with_recesses(vec![Recess::new(true, hole, indent2, SectionShape::ROUND), Recess::new(false, hole, indent2, SectionShape::ROUND)]),
        HeadingStation::new(
            3,
            form_title,
            &format!("Close hex s={} × m={} mm{}, deepen indents to {} mm, leave web {} mm{}", f(s), f(m), flange, f(indent_final), f(web), form_extra),
            part.segments.clone(),
        )
        .with_recesses(vec![Recess::new(true, hole, indent_final, SectionShape::ROUND), Recess::new(false, hole, indent_final, SectionShape::ROUND)])
        .peak(true),
        pierce,
    ];
    let mut plan = HeadingPlan::new(wire, slug_len, slug_vol, stations, off_header_ops(t, thread_label), 0, part.cautions.clone());
    plan.route = HeaderRoute::NUT_FORMER;
    plan.route_reason = "Nut former: the blank is upset, indented, hex-formed and pierced from wire in line.".into();
    plan
}

/// Large-end diameter D of a frustum of height h with small end d holding volume v.
fn cone_large_end(v: f64, d: f64, h: f64) -> f64 {
    let c = 12.0 * v / (PI * h);
    let disc = (4.0 * c - 3.0 * d * d).max(0.0);
    (-d + disc.sqrt()) / 2.0
}
