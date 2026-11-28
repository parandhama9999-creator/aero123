//! Ports of ExtractedFastenerSpec.kt, DrawingScan.kt and ShopDefaults.kt: a scanned drawing turned into
//! the engine's calculation and into the part's total volume.

use crate::calculation::{self, CalcInput, FastenerCalculationResult};
use crate::catalog::{self, MaterialInfo};
use crate::gen_enums::*;
use crate::heading::{section_area, HeadingStation, ProfileSegment, Recess};
use crate::jfmt;
use crate::part_geometry::{self, DrawingGeometry, VolumeItem};
use crate::poka_yoke;
use crate::tensile::TensileRange;
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

/// The plant's own standing requirement for anything a drawing does not state (ShopDefaults.kt).
/// The app keeps the user's edited values; these are the factory values.
pub mod shop_defaults {
    use crate::gen_enums::{CoatingType, PropertyClass};
    pub const FACTORY_MATERIAL: &str = "4135";
    pub const FACTORY_CLASS: PropertyClass = PropertyClass::CLASS_10_9;
    pub const FACTORY_COATING: CoatingType = CoatingType::ZN_NI;
    pub const FACTORY_BATCH: i32 = 10000;
}

/// A material that exists in the catalogue; an unknown key falls back to the factory default (ShopDefaults.material).
pub fn shop_material(key: &str) -> &'static MaterialInfo {
    catalog::material(key).or_else(|| catalog::material(shop_defaults::FACTORY_MATERIAL)).expect("factory material in catalogue")
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DetectedDimension {
    pub feature: String,
    pub nominal: String,
    pub tolerance: String,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DrawnSolid {
    pub feature: String,
    pub shape: SolidShape,
    pub d1_mm: f64,
    pub d2_mm: f64,
    pub length_mm: f64,
    pub threaded: bool,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DrawnCutout {
    pub feature: String,
    pub shape: SolidShape,
    pub d1_mm: f64,
    pub d2_mm: f64,
    pub length_mm: f64,
    pub count: i32,
    pub from: CutoutFrom,
    pub threaded: bool,
}

fn default_model() -> String {
    "gemini-3.5-flash".into()
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtractedFastenerSpec {
    pub fastener_type: FastenerType,
    pub unit_system: UnitSystem,
    pub nominal_size_index: usize,
    pub nominal_dia_mm: f64,
    pub pitch_value: f64,
    pub length_mm: f64,
    pub property_class: PropertyClass,
    pub material_key: String,
    pub thread_class: String,
    pub coating: CoatingType,
    pub technical_summary: String,
    pub detected_dimensions: Vec<DetectedDimension>,
    pub is_ai_vision_extracted: bool,
    #[serde(default = "default_model")]
    pub model_used: String,
    #[serde(default)]
    pub radius: Option<f64>,
    #[serde(default)]
    pub thread_length_mm: Option<f64>,
    #[serde(default)]
    pub head_across_flats_mm: Option<f64>,
    #[serde(default)]
    pub head_height_mm: Option<f64>,
    #[serde(default)]
    pub flange_dia_mm: Option<f64>,
    #[serde(default)]
    pub flange_thickness_mm: Option<f64>,
    #[serde(default)]
    pub shank_dia_mm: Option<f64>,
    #[serde(default)]
    pub head_dia_mm: Option<f64>,
    #[serde(default)]
    pub socket_size_mm: Option<f64>,
    #[serde(default)]
    pub socket_depth_mm: Option<f64>,
    #[serde(default)]
    pub neck_height_mm: Option<f64>,
    #[serde(default)]
    pub nut_height_mm: Option<f64>,
    #[serde(default)]
    pub washer_outer_dia_mm: Option<f64>,
    #[serde(default)]
    pub washer_inner_dia_mm: Option<f64>,
    #[serde(default)]
    pub washer_thickness_mm: Option<f64>,
    #[serde(default)]
    pub head_dome_radius_mm: Option<f64>,
    #[serde(default)]
    pub head_edge_height_mm: Option<f64>,
    #[serde(default)]
    pub tensile_min_mpa: Option<f64>,
    #[serde(default)]
    pub tensile_max_mpa: Option<f64>,
    #[serde(default)]
    pub thread_lock_patch: bool,
    #[serde(default)]
    pub batch_qty: Option<i32>,
    #[serde(default)]
    pub assumed: Vec<String>,
    #[serde(default)]
    pub solids: Vec<DrawnSolid>,
    #[serde(default)]
    pub cutouts: Vec<DrawnCutout>,
}

impl ExtractedFastenerSpec {
    /// Every drawing dimension the volume model can use.
    pub fn geometry(&self) -> DrawingGeometry {
        let round_head = matches!(
            part_geometry::style_of(self.fastener_type),
            HeadStyle::SOCKET_CAP | HeadStyle::SOCKET_COUNTERSUNK | HeadStyle::SOCKET_BUTTON | HeadStyle::CARRIAGE | HeadStyle::ROUND_PIN | HeadStyle::RIVET
        );
        DrawingGeometry {
            thread_length_mm: self.thread_length_mm,
            shank_dia_mm: self.shank_dia_mm,
            across_flats_mm: self.head_across_flats_mm,
            head_dia_mm: self.head_dia_mm.or(self.head_across_flats_mm.filter(|_| round_head)),
            head_height_mm: self.head_height_mm,
            flange_dia_mm: self.flange_dia_mm,
            flange_thickness_mm: self.flange_thickness_mm,
            socket_size_mm: self.socket_size_mm,
            socket_depth_mm: self.socket_depth_mm,
            neck_height_mm: self.neck_height_mm,
            nut_height_mm: self.nut_height_mm,
            washer_outer_dia_mm: self.washer_outer_dia_mm,
            washer_inner_dia_mm: self.washer_inner_dia_mm,
            washer_thickness_mm: self.washer_thickness_mm,
            head_dome_radius_mm: self.head_dome_radius_mm,
            head_edge_height_mm: self.head_edge_height_mm,
        }
    }

    pub fn tensile_range(&self) -> Option<TensileRange> {
        TensileRange::of(self.tensile_min_mpa, self.tensile_max_mpa)
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DrawingVolume {
    pub basis: VolumeBasis,
    pub items: Vec<VolumeItem>,
    pub total_mm3: f64,
    pub density_g_per_cm3: f64,
    pub from_drawing: Vec<String>,
    pub not_on_drawing: Vec<String>,
    pub cautions: Vec<String>,
    pub model_volume_mm3: f64,
    pub scrap_mm3: f64,
    pub wire_dia_mm: f64,
    pub outline: Option<HeadingStation>,
    pub head_mm3: f64,
    pub head_is_scaled: bool,
}

impl DrawingVolume {
    pub fn shank_mm3(&self) -> f64 {
        self.total_mm3 - self.head_mm3
    }
    pub fn head_mass_g(&self) -> f64 {
        self.head_mm3 / 1000.0 * self.density_g_per_cm3
    }
    pub fn mass_g(&self) -> f64 {
        self.total_mm3 / 1000.0 * self.density_g_per_cm3
    }
    pub fn cut_off_length_mm(&self) -> f64 {
        (self.total_mm3 + self.scrap_mm3) / (PI / 4.0 * self.wire_dia_mm * self.wire_dia_mm)
    }
    pub fn is_complete(&self) -> bool {
        self.not_on_drawing.is_empty() && self.cautions.is_empty()
    }
}

fn n(v: f64) -> String {
    jfmt::f(v, 2)
}
fn nd(v: f64, dp: usize) -> String {
    jfmt::f(v, dp)
}

/// The full engineering calculation for the scanned part. `shop_batch_qty` stands in when the drawing gives no quantity.
pub fn calculate(spec: &ExtractedFastenerSpec, header_choice: HeaderChoice, shop_batch_qty: i32) -> FastenerCalculationResult {
    let material = catalog::material(&spec.material_key).or_else(|| catalog::material("10B21")).expect("10B21 in catalogue").clone();
    let mut c = CalcInput::new(
        spec.fastener_type,
        spec.unit_system,
        poka_yoke::default_standard(spec.fastener_type, spec.unit_system),
        spec.nominal_size_index,
        spec.pitch_value,
        spec.length_mm,
        &spec.thread_class,
        material,
        spec.coating,
        spec.batch_qty.unwrap_or(shop_batch_qty),
    );
    c.custom_nominal_dia_mm = Some(spec.nominal_dia_mm);
    c.extracted_radius = spec.radius;
    c.property_class = Some(spec.property_class);
    c.drawing = spec.geometry();
    c.tensile_range = spec.tensile_range();
    c.thread_lock_patch = spec.thread_lock_patch;
    c.header_choice = header_choice;
    calculation::calculate(&c)
}

struct SolidsResult {
    items: Vec<VolumeItem>,
    total: f64,
    outline: Option<HeadingStation>,
    cautions: Vec<String>,
    rejected: Option<String>,
}

pub fn volume(spec: &ExtractedFastenerSpec, result: &FastenerCalculationResult) -> DrawingVolume {
    let part = result.finished_part.as_ref();
    let model_vol = part.map(|p| p.volume_mm3()).unwrap_or(0.0);
    let scrap = 0.0f64.max(result.heading_plan.as_ref().map(|h| h.slug_volume_mm3).unwrap_or(model_vol) - model_vol);
    let core_assumed: Vec<String> = spec.assumed.iter().filter(|a| a.starts_with("nominal diameter") || a.starts_with("length") || a.starts_with("pitch")).cloned().collect();
    let mut core_read = Vec::new();
    if !spec.assumed.iter().any(|a| a.starts_with("nominal diameter")) {
        core_read.push(format!("nominal diameter d {} mm", n(spec.nominal_dia_mm)));
    }
    if !spec.assumed.iter().any(|a| a.starts_with("length")) && !spec.fastener_type.is_nut() && !spec.fastener_type.is_washer() {
        core_read.push(format!("length L {} mm", n(spec.length_mm)));
    }
    let model_head = part.map(|p| p.head_volume_mm3()).unwrap_or(0.0);
    let head_share = if model_vol > 0.0 { model_head / model_vol } else { 0.0 };

    let solids = solids_volume(spec, result);
    if let Some(sr) = solids.as_ref().filter(|s| s.rejected.is_none()) {
        let delta = if model_vol > 0.0 { (sr.total - model_vol) / model_vol * 100.0 } else { 0.0 };
        let mut cautions = sr.cautions.clone();
        if delta.abs() > 2.0 {
            cautions.push(format!(
                "The solids give {} mm³; the standard shape of this type gives {} mm³ ({}{} %). Check the solids list against the drawing.",
                nd(sr.total, 0),
                nd(model_vol, 0),
                if delta > 0.0 { "+" } else { "" },
                nd(delta, 1)
            ));
        }
        let mut from_drawing = core_read;
        from_drawing.extend(sr.items.iter().map(|i| format!("{}: {}", i.feature, i.detail)));
        return DrawingVolume {
            basis: VolumeBasis::SOLIDS,
            items: sr.items.clone(),
            total_mm3: sr.total,
            density_g_per_cm3: result.material.density,
            from_drawing,
            not_on_drawing: core_assumed,
            cautions,
            model_volume_mm3: model_vol,
            scrap_mm3: scrap,
            wire_dia_mm: result.wire_dia_mm,
            outline: sr.outline.clone(),
            head_mm3: sr.total * head_share,
            head_is_scaled: true,
        };
    }
    let mut from_drawing = core_read;
    from_drawing.extend(part.map(|p| p.from_drawing.clone()).unwrap_or_default());
    let mut not_on_drawing = core_assumed;
    not_on_drawing.extend(part.map(|p| p.not_on_drawing.clone()).unwrap_or_default());
    let mut cautions = part.map(|p| p.cautions.clone()).unwrap_or_default();
    if let Some(rej) = solids.and_then(|s| s.rejected) {
        cautions.push(rej);
    }
    DrawingVolume {
        basis: VolumeBasis::DIMENSIONS,
        items: part.map(|p| p.items.clone()).unwrap_or_default(),
        total_mm3: model_vol,
        density_g_per_cm3: result.material.density,
        from_drawing,
        not_on_drawing,
        cautions,
        model_volume_mm3: model_vol,
        scrap_mm3: scrap,
        wire_dia_mm: result.wire_dia_mm,
        outline: part.map(|p| p.as_station()),
        head_mm3: model_head,
        head_is_scaled: false,
    }
}

/// Overall axial length the solids must add up to, when the drawing gave what is needed to know it.
fn expected_overall_mm(spec: &ExtractedFastenerSpec) -> Option<f64> {
    let length_read = !spec.assumed.iter().any(|a| a.starts_with("length"));
    match part_geometry::style_of(spec.fastener_type) {
        HeadStyle::NUT => spec.nut_height_mm.or(spec.head_height_mm),
        HeadStyle::WASHER => spec.washer_thickness_mm,
        HeadStyle::HEADLESS | HeadStyle::SOCKET_COUNTERSUNK => Some(spec.length_mm).filter(|_| length_read),
        _ => spec.head_height_mm.filter(|_| length_read).map(|h| spec.length_mm + h),
    }
}

fn solids_volume(spec: &ExtractedFastenerSpec, result: &FastenerCalculationResult) -> Option<SolidsResult> {
    if spec.solids.is_empty() {
        return None;
    }
    let reject = |why: &str| SolidsResult { items: vec![], total: 0.0, outline: None, cautions: vec![], rejected: Some(format!("The solids read from the drawing were not used: {}.", why)) };
    if spec.solids.iter().any(|s| s.length_mm <= 0.0 || s.d1_mm <= 0.0 || (s.shape == SolidShape::CONE && s.d2_mm < 0.0)) {
        return Some(reject("a section has no size"));
    }
    if spec.cutouts.iter().any(|c| c.length_mm <= 0.0 || c.d1_mm <= 0.0 || c.count < 1) {
        return Some(reject("a cut-out has no size"));
    }
    let pitch = result.pitch_mm;
    let thread_dia = |nominal: f64, internal: bool| -> f64 {
        if (nominal - result.nominal_dia_mm).abs() <= 0.02 * result.nominal_dia_mm && internal == result.is_internal_nut {
            result.blank_target_dia
        } else if internal {
            nominal - pitch
        } else {
            nominal - 0.6495 * pitch
        }
    };
    let area = |shape: SolidShape, size: f64| match shape {
        SolidShape::HEX => section_area(SectionShape::HEX, size),
        SolidShape::TWELVE_POINT => section_area(SectionShape::TWELVE_POINT, size),
        SolidShape::SQUARE => section_area(SectionShape::SQUARE, size),
        _ => section_area(SectionShape::ROUND, size),
    };
    let segment = |s: &DrawnSolid| -> ProfileSegment {
        match s.shape {
            SolidShape::CYLINDER => ProfileSegment::round(s.length_mm, if s.threaded { thread_dia(s.d1_mm, false) } else { s.d1_mm }),
            SolidShape::CONE => ProfileSegment::cone(s.length_mm, s.d2_mm, s.d1_mm),
            SolidShape::HEX => ProfileSegment::shaped(s.length_mm, s.d1_mm, SectionShape::HEX),
            SolidShape::TWELVE_POINT => ProfileSegment::shaped(s.length_mm, s.d1_mm, SectionShape::TWELVE_POINT),
            SolidShape::SQUARE => ProfileSegment::shaped(s.length_mm, s.d1_mm, SectionShape::SQUARE),
            SolidShape::DOME => ProfileSegment::shaped(s.length_mm, s.d1_mm, SectionShape::DOME),
        }
    };
    let describe = |s: &DrawnSolid| -> String {
        match s.shape {
            SolidShape::CYLINDER => {
                if s.threaded {
                    format!("thread Ø{}, blank Ø{} × {} mm", n(s.d1_mm), nd(thread_dia(s.d1_mm, false), 3), n(s.length_mm))
                } else {
                    format!("Ø{} × {} mm", n(s.d1_mm), n(s.length_mm))
                }
            }
            SolidShape::CONE => format!("cone Ø{} → Ø{} × {} mm", n(s.d1_mm), n(s.d2_mm), n(s.length_mm)),
            SolidShape::HEX => format!("hex s {} × {} mm", n(s.d1_mm), n(s.length_mm)),
            SolidShape::TWELVE_POINT => format!("12-point s {} × {} mm", n(s.d1_mm), n(s.length_mm)),
            SolidShape::SQUARE => format!("square {} × {} × {} mm", n(s.d1_mm), n(s.d1_mm), n(s.length_mm)),
            SolidShape::DOME => format!("dome Ø{} × {} mm", n(s.d1_mm), n(s.length_mm)),
        }
    };
    let overall: f64 = spec.solids.iter().map(|s| s.length_mm).sum();
    let expected = expected_overall_mm(spec);
    if let Some(e) = expected {
        if (overall - e).abs() > 0.5f64.max(0.02 * e) {
            return Some(reject(&format!("their lengths add up to {} mm but the drawing's overall length is {} mm", n(overall), n(e))));
        }
    }
    let segments: Vec<(&DrawnSolid, ProfileSegment)> = spec.solids.iter().map(|s| (s, segment(s))).collect();
    let mut items: Vec<VolumeItem> = segments
        .iter()
        .map(|(s, seg)| VolumeItem {
            feature: if s.feature.trim().is_empty() { s.shape.name().to_lowercase() } else { s.feature.clone() },
            detail: describe(s),
            volume_mm3: seg.volume_mm3(),
            source: DimSource::DRAWING,
        })
        .collect();
    for c in &spec.cutouts {
        let one = match c.shape {
            SolidShape::CONE => PI * c.length_mm / 12.0 * (c.d1_mm * c.d1_mm + c.d1_mm * c.d2_mm + c.d2_mm * c.d2_mm),
            SolidShape::CYLINDER => area(c.shape, if c.threaded { thread_dia(c.d1_mm, true) } else { c.d1_mm }) * c.length_mm,
            _ => area(c.shape, c.d1_mm) * c.length_mm,
        };
        let vol = c.count as f64 * one;
        let what = format!(
            "{} × {} mm{}{}",
            if c.shape == SolidShape::CYLINDER { format!("Ø{}", n(c.d1_mm)) } else { format!("{} {}", c.shape.name().to_lowercase().replace('_', "-"), n(c.d1_mm)) },
            n(c.length_mm),
            if c.count > 1 { format!(" × {}", c.count) } else { String::new() },
            if c.threaded { " (tapped)" } else { "" }
        );
        items.push(VolumeItem { feature: if c.feature.trim().is_empty() { "cut-out".into() } else { c.feature.clone() }, detail: what, volume_mm3: -vol, source: DimSource::DRAWING });
    }
    let total: f64 = items.iter().map(|i| i.volume_mm3).sum();
    if total <= 0.0 {
        return Some(reject("the cut-outs remove more than the solids hold"));
    }
    let recess_shape = |shape: SolidShape| match shape {
        SolidShape::HEX => SectionShape::HEX,
        SolidShape::TWELVE_POINT => SectionShape::TWELVE_POINT,
        SolidShape::SQUARE => SectionShape::SQUARE,
        _ => SectionShape::ROUND,
    };
    let mut outline = HeadingStation::new(1, "Part as read from the drawing", "", segments.iter().rev().map(|(_, seg)| seg.clone()).collect());
    outline.recesses = spec
        .cutouts
        .iter()
        .filter(|c| c.from == CutoutFrom::HEAD_END || c.from == CutoutFrom::POINT_END)
        .map(|c| Recess::new(c.from == CutoutFrom::HEAD_END, c.d1_mm, c.length_mm.min(overall), recess_shape(c.shape)))
        .collect();
    outline.through_hole_mm = spec
        .cutouts
        .iter()
        .find(|c| c.from == CutoutFrom::THROUGH && c.shape == SolidShape::CYLINDER)
        .map(|c| if c.threaded { thread_dia(c.d1_mm, true) } else { c.d1_mm });
    let mut cautions = Vec::new();
    if expected.is_none() {
        cautions.push("The overall length could not be cross-checked against the drawing: verify the solids list.".into());
    }
    Some(SolidsResult { items, total, outline: Some(outline), cautions, rejected: None })
}
