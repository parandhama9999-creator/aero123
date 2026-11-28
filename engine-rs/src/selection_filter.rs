//! Port of SelectionFilter.kt: material, property-class and thread-class lists for the selectors.

use crate::catalog::{thread_class_catalog, MaterialInfo};
use crate::gen_enums::{ClassGroup, FastenerType, MaterialFamily, MaterialGroup, ProductStandard, PropertyClass, UnitSystem};
use crate::poka_yoke;

pub fn has_thread(t: FastenerType) -> bool {
    !t.is_unthreaded() && !t.is_washer()
}

pub fn group_of_material(m: &MaterialInfo) -> MaterialGroup {
    let fam = poka_yoke::material_family(m);
    MaterialGroup::VALUES.iter().copied().find(|g| g.family() == Some(fam)).unwrap_or(MaterialGroup::ALL)
}

/// Kotlin `contains(q, ignoreCase = true)`.
fn contains_ic(s: &str, q: &str) -> bool {
    s.to_lowercase().contains(&q.to_lowercase())
}

/// The alloys a property class can legally be made from (same rules as the poka-yoke gate).
pub fn class_allows(pc: PropertyClass, m: &MaterialInfo, d_mm: f64) -> bool {
    use MaterialFamily as MF;
    let family = poka_yoke::material_family(m);
    let cs = poka_yoke::class_strength(pc, d_mm, m);
    let family_ok = if cs.is_iso_steel_class || poka_yoke::INCH_STEEL_CLASSES.contains(&pc) {
        family == MF::CARBON_ALLOY_STEEL
    } else if poka_yoke::STAINLESS_CLASSES.contains(&pc) {
        poka_yoke::is_austenitic_a2_a4(m)
    } else if poka_yoke::MARTENSITIC_FERRITIC_CLASSES.contains(&pc) {
        family == MF::STAINLESS && !poka_yoke::is_austenitic_a2_a4(m)
    } else if pc == PropertyClass::AERO_AL {
        family == MF::ALUMINUM
    } else if pc == PropertyClass::AERO_1100 {
        family == MF::TITANIUM
    } else if pc == PropertyClass::AERO_1380 || pc == PropertyClass::AERO_1750 {
        family == MF::NICKEL_SUPERALLOY || family == MF::CARBON_ALLOY_STEEL || family == MF::STAINLESS
    } else if pc == PropertyClass::MATERIAL_SPEC {
        family != MF::CARBON_ALLOY_STEEL || m.is_aero
    } else {
        true
    };
    family_ok && m.post_forging_target_uts_mpa >= poka_yoke::required_rm_mpa(pc, d_mm, m) * 0.98
}

/// Materials for the selector, narrowed to the group, the class and the search text. Never empty.
pub fn materials<'a>(all: &'a [MaterialInfo], group: MaterialGroup, pc: Option<PropertyClass>, d_mm: f64, only_for_class: bool, search: &str) -> Vec<&'a MaterialInfo> {
    let by_group: Vec<&MaterialInfo> = all.iter().filter(|m| group.family().is_none_or(|f| poka_yoke::material_family(m) == f)).collect();
    let by_class: Vec<&MaterialInfo> = match pc {
        Some(pc) if only_for_class => by_group.iter().copied().filter(|m| class_allows(pc, m, d_mm)).collect(),
        _ => by_group.clone(),
    };
    let q = search.trim();
    let by_search: Vec<&MaterialInfo> = if q.is_empty() { by_class.clone() } else { by_class.iter().copied().filter(|m| contains_ic(&m.name, q) || contains_ic(&m.key, q)).collect() };
    if !by_search.is_empty() {
        by_search
    } else if !by_class.is_empty() {
        by_class
    } else if !by_group.is_empty() {
        by_group
    } else {
        all.iter().collect()
    }
}

/// Alloy families that have at least one grade in the catalogue.
pub fn material_groups(all: &[MaterialInfo]) -> Vec<MaterialGroup> {
    let mut v = vec![MaterialGroup::ALL];
    v.extend(MaterialGroup::VALUES.iter().copied().filter(|g| g.family().is_some_and(|f| all.iter().any(|m| poka_yoke::material_family(m) == f))));
    v
}

pub fn group_of_class(pc: PropertyClass) -> ClassGroup {
    let name = pc.name();
    if poka_yoke::ISO_NUT_CLASSES.contains(&pc) {
        ClassGroup::ISO_898_2
    } else if poka_yoke::STAINLESS_CLASSES.contains(&pc) && name.starts_with("ASTM") {
        ClassGroup::ASTM
    } else if poka_yoke::STAINLESS_CLASSES.contains(&pc) || poka_yoke::MARTENSITIC_FERRITIC_CLASSES.contains(&pc) {
        ClassGroup::ISO_3506
    } else if name.starts_with("CLASS_") {
        ClassGroup::ISO_898_1
    } else if name.starts_with("SAE") {
        ClassGroup::SAE
    } else if name.starts_with("ASTM") {
        ClassGroup::ASTM
    } else if name.starts_with("AERO") {
        ClassGroup::AERO
    } else {
        ClassGroup::SPEC
    }
}

/// Washers are not graded by bolt property class.
pub fn uses_property_class(t: FastenerType) -> bool {
    !t.is_washer()
}

/// Property classes for the selector, the measurement system's own families first. Never empty.
pub fn property_classes(t: FastenerType, unit: UnitSystem, group: ClassGroup, search: &str) -> Vec<PropertyClass> {
    if !uses_property_class(t) {
        return vec![];
    }
    let by_type: Vec<PropertyClass> = PropertyClass::VALUES.iter().copied().filter(|pc| !poka_yoke::NUT_ONLY_CLASSES.contains(pc) || t.is_nut()).collect();
    let by_group: Vec<PropertyClass> = if group == ClassGroup::ALL { by_type.clone() } else { by_type.iter().copied().filter(|&pc| group_of_class(pc) == group).collect() };
    let q = search.trim();
    let by_search: Vec<PropertyClass> = if q.is_empty() { by_group.clone() } else { by_group.iter().copied().filter(|pc| contains_ic(pc.label(), q)).collect() };
    let mut picked = if !by_search.is_empty() {
        by_search
    } else if !by_group.is_empty() {
        by_group
    } else {
        by_type
    };
    let inch_first = unit == UnitSystem::INCH;
    picked.sort_by_key(|&pc| {
        let g = group_of_class(pc);
        let inch_family = g == ClassGroup::SAE || g == ClassGroup::ASTM;
        if inch_first && inch_family || !inch_first && (g == ClassGroup::ISO_898_1 || g == ClassGroup::ISO_898_2 || g == ClassGroup::ISO_3506) {
            0
        } else {
            1
        }
    });
    picked
}

/// Class groups that have at least one class for this fastener type.
pub fn class_groups(t: FastenerType) -> Vec<ClassGroup> {
    let available: Vec<ClassGroup> = property_classes(t, UnitSystem::METRIC, ClassGroup::ALL, "").into_iter().map(group_of_class).collect();
    let mut v = vec![ClassGroup::ALL];
    v.extend(ClassGroup::VALUES.iter().copied().filter(|g| *g != ClassGroup::ALL && available.contains(g)));
    v
}

/// Thread classes in order of preference for this standard; the first is the default.
pub fn thread_classes(unit: UnitSystem, t: FastenerType, standard: ProductStandard) -> Vec<&'static str> {
    let all = thread_class_catalog::get_options(unit, t.is_nut());
    match poka_yoke::standard_thread_classes(standard, t.is_nut()) {
        None => {
            let mut v: Vec<&'static str> = all.to_vec();
            if standard.category() == "Aerospace/Defense" {
                // sortedByDescending on a Boolean, stable: controlled-root profiles first
                v.sort_by_key(|c| !(c.contains("UNJ") || c.ends_with("-MJ")));
            }
            v
        }
        Some(allowed) => {
            let v: Vec<&'static str> = allowed.into_iter().filter(|c| all.contains(c)).collect();
            if v.is_empty() {
                all.to_vec()
            } else {
                v
            }
        }
    }
}
