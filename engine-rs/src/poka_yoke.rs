//! Port of PokaYokeEngine.kt: material families, class strengths, standards, and the strict release gate.

use crate::catalog::{self, MaterialInfo, INCH_SIZES, METRIC_SIZES};
use crate::catalog::thread_class_catalog;
use crate::dimensions::HeadDimensions;
use crate::gen_enums::*;
use crate::jfmt::{self, d as dstr};
use crate::tensile::{hardness_conversion, TensileRange};
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PokaYokeIssue {
    pub severity: PokaYokeSeverity,
    pub title: String,
    pub detail: String,
    pub fix: String,
}

impl PokaYokeIssue {
    pub fn new(severity: PokaYokeSeverity, title: &str, detail: &str, fix: &str) -> Self {
        PokaYokeIssue { severity, title: title.into(), detail: detail.into(), fix: fix.into() }
    }
}

/// ISO 898-1 / ISO 3506 minimum mechanical properties used for proof and breaking loads.
#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ClassStrength {
    pub rm_min_mpa: f64,
    pub yield_min_mpa: f64,
    pub proof_stress_mpa: f64,
    pub is_iso_steel_class: bool,
}

const fn cs(rm: f64, y: f64, p: f64, iso: bool) -> ClassStrength {
    ClassStrength { rm_min_mpa: rm, yield_min_mpa: y, proof_stress_mpa: p, is_iso_steel_class: iso }
}

/// Kotlin `substringBefore(" (")`.
pub(crate) fn before_paren(s: &str) -> &str {
    s.split(" (").next().unwrap_or(s)
}

fn is_word(c: char) -> bool {
    c.is_alphanumeric() || c == '_'
}

/// Regex `\bword\b` (ASCII word boundaries around a literal).
pub(crate) fn contains_word(hay: &str, word: &str) -> bool {
    let h: Vec<char> = hay.chars().collect();
    let w: Vec<char> = word.chars().collect();
    if w.is_empty() || h.len() < w.len() {
        return false;
    }
    for i in 0..=(h.len() - w.len()) {
        if h[i..i + w.len()] == w[..] {
            let before_ok = i == 0 || !is_word(h[i - 1]);
            let after_ok = i + w.len() == h.len() || !is_word(h[i + w.len()]);
            if before_ok && after_ok {
                return true;
            }
        }
    }
    false
}

pub fn material_family(m: &MaterialInfo) -> MaterialFamily {
    let k = m.key.to_uppercase();
    let n = m.name.to_uppercase();
    if k.starts_with("TI_") || k == "VT16" || n.contains("TITANIUM") {
        MaterialFamily::TITANIUM
    } else if (k.starts_with("AL") && n.contains("ALLOY")) || n.contains(" AL ") || n.contains("ALUMINUM") || n.contains("ALUMINIUM") {
        MaterialFamily::ALUMINUM
    } else if k.starts_with("BRASS") || k.contains("BRONZE") || k.starts_with("CUNI") || k == "CUBE2"
        || n.contains("BRASS") || n.contains("BRONZE") || n.contains("COPPER") || n.contains("CUSN")
        || n.contains("CUSI") || n.contains("CUNI")
    {
        MaterialFamily::COPPER_ALLOY
    } else if ["INCONEL", "INCOLOY", "WASPALOY", "RENE", "MP35N", "MP159", "HASTELLOY", "MONEL", "NIMONIC", "A286", "A453", "10KH11N23T3MR", "INVAR"]
        .iter()
        .any(|x| k.contains(x))
    {
        MaterialFamily::NICKEL_SUPERALLOY
    } else if k.starts_with("SS") || k.contains("DUPLEX") || k.contains("PH") || k.contains("X13")
        || k == "NITRONIC60" || k == "CUSTOM_455" || k == "AM350" || k == "14KH17N2"
        || k == "12KH18N10T" || n.contains("STAINLESS") || n.contains("DUPLEX")
    {
        MaterialFamily::STAINLESS
    } else {
        MaterialFamily::CARBON_ALLOY_STEEL
    }
}

pub fn stainless_type(m: &MaterialInfo) -> Option<StainlessType> {
    if material_family(m) != MaterialFamily::STAINLESS {
        return None;
    }
    let id = format!("{} {}", m.key, m.name).to_uppercase();
    let key = m.key.to_uppercase();
    Some(if id.contains("DUPLEX") {
        StainlessType::DUPLEX
    } else if key.contains("PH") || key == "CUSTOM_455" || key == "AM350" || id.contains("PRECIPITATION") {
        StainlessType::PRECIPITATION_HARDENING
    } else if id.contains("FERRITIC") || contains_word(&id, "430") {
        StainlessType::FERRITIC
    } else if id.contains("MARTENSITIC") || ["410", "420", "431", "440C"].iter().any(|w| contains_word(&id, w)) {
        StainlessType::MARTENSITIC
    } else {
        StainlessType::AUSTENITIC
    })
}

/// ISO 3506-1 A2/A4 classes cover only austenitic A2 (304-type) and A4 (316-type) stainless.
pub fn is_austenitic_a2_a4(m: &MaterialInfo) -> bool {
    if material_family(m) != MaterialFamily::STAINLESS {
        return false;
    }
    let id = format!("{} {}", m.key, m.name).to_uppercase();
    id.contains("304") || id.contains("316")
}

pub fn is_inch_standard(s: ProductStandard) -> bool {
    s.category() == "ASME/Imperial" || s.code().starts_with("NAS") || s.code().starts_with("MS")
}

pub fn is_metric_only_standard(s: ProductStandard) -> bool {
    s.category() == "ISO/DIN Metric" || s.code().starts_with("EN ")
}

/// Thread tolerance classes a product standard allows; None = no restriction.
pub fn standard_thread_classes(s: ProductStandard, is_nut: bool) -> Option<Vec<&'static str>> {
    if s == ProductStandard::DIN_7500 || s.category() == "Aerospace/Defense" {
        None
    } else if s.category() == "ISO/DIN Metric" {
        Some(if is_nut { vec!["6H", "6G", "6AZ", "6AX"] } else { vec!["6g", "6f", "6e", "6h"] })
    } else if s == ProductStandard::ASME_B18_3 {
        if is_nut { None } else { Some(vec!["3A", "2A", "6g", "6f", "6e", "6h"]) }
    } else if is_nut {
        Some(vec!["2B", "1B", "6H", "6G", "6AZ", "6AX"])
    } else {
        Some(vec!["2A", "1A", "6g", "6f", "6e", "6h"])
    }
}

/// Commercial grades (ISO 898-1, SAE / ASTM, ISO 3506) are made from commercial wire.
pub fn is_commercial_class(pc: PropertyClass) -> bool {
    class_strength(pc, 10.0, &catalog::materials()[0]).is_iso_steel_class || INCH_STEEL_CLASSES.contains(&pc) || STAINLESS_CLASSES.contains(&pc)
}

/// Coatings finished with a chromate or passivate layer that the bake would destroy.
pub const CHROMATED_COATINGS: &[CoatingType] = &[
    CoatingType::ZN_NI, CoatingType::ZINC_BLUE, CoatingType::ZINC_YELLOW,
    CoatingType::AERO_CAD, CoatingType::AERO_CAD_CL2, CoatingType::AERO_CAD_CL3, CoatingType::AERO_CAD_LHE,
];

pub fn is_aerospace_type(t: FastenerType) -> bool {
    t.category() == "Aerospace" || t == FastenerType::TWELVE_POINT
}

/// Minimum strength the selected property class guarantees (ISO 898-1 Table 3 / ISO 3506-1).
pub fn class_strength(pc: PropertyClass, d_mm: f64, material: &MaterialInfo) -> ClassStrength {
    use PropertyClass::*;
    match pc {
        CLASS_4_6 => cs(400.0, 240.0, 225.0, true),
        CLASS_4_8 => cs(420.0, 340.0, 310.0, true),
        CLASS_5_6 => cs(500.0, 300.0, 280.0, true),
        CLASS_5_8 => cs(520.0, 420.0, 380.0, true),
        CLASS_6_8 => cs(600.0, 480.0, 440.0, true),
        CLASS_8_8 => if d_mm <= 16.0 { cs(800.0, 640.0, 580.0, true) } else { cs(830.0, 660.0, 600.0, true) },
        CLASS_9_8 => cs(900.0, 720.0, 650.0, true),
        CLASS_10_9 => cs(1040.0, 940.0, 830.0, true),
        CLASS_12_9 => cs(1220.0, 1100.0, 970.0, true),
        NUT_04 => cs(380.0, 380.0, 380.0, true),
        NUT_05 => cs(500.0, 500.0, 500.0, true),
        NUT_5 => cs(500.0, 500.0, 500.0, true),
        NUT_6 => cs(600.0, 600.0, 600.0, true),
        NUT_8 => cs(800.0, 800.0, 800.0, true),
        NUT_9 => cs(900.0, 900.0, 900.0, true),
        NUT_10 => cs(1040.0, 1040.0, 1040.0, true),
        NUT_12 => cs(1150.0, 1150.0, 1150.0, true),
        A2_70 | A4_70 => cs(700.0, 450.0, 450.0, false),
        A2_50 | A4_50 => cs(500.0, 210.0, 210.0, false),
        A2_80 | A4_80 => cs(800.0, 600.0, 600.0, false),
        C1_50 => cs(500.0, 250.0, 250.0, false),
        C1_70 | C4_70 => cs(700.0, 410.0, 410.0, false),
        C3_80 => cs(800.0, 640.0, 640.0, false),
        F1_60 => cs(600.0, 410.0, 410.0, false),
        SAE_NUT_5 => cs(827.0, 827.0, 827.0, false),
        SAE_NUT_8 => cs(1034.0, 1034.0, 1034.0, false),
        ASTM_A563_DH => cs(1035.0, 1035.0, 1035.0, false),
        ASTM_A194_2H => cs(862.0, 862.0, 862.0, false),
        ASTM_A194_8 => cs(515.0, 515.0, 515.0, false),
        ASTM_A325 => if d_mm <= 25.4 { cs(830.0, 660.0, 600.0, false) } else { cs(725.0, 560.0, 510.0, false) },
        ASTM_A490 => cs(1035.0, 895.0, 825.0, false),
        ASTM_A307_A | ASTM_A307_B => cs(414.0, 248.0, 228.0, false),
        ASTM_A193_B16 => if d_mm <= 63.5 { cs(862.0, 724.0, 0.9 * 724.0, false) } else { cs(758.0, 655.0, 0.9 * 655.0, false) },
        ASTM_A320_L7 => if d_mm <= 63.5 { cs(862.0, 724.0, 0.9 * 724.0, false) } else { cs(793.0, 655.0, 0.9 * 655.0, false) },
        ASTM_A193_B8_CL1 => cs(515.0, 205.0, 205.0, false),
        ASTM_A193_B8M_CL2 => if d_mm <= 19.05 { cs(655.0, 448.0, 448.0, false) } else { cs(620.0, 345.0, 345.0, false) },
        ASTM_F1554_36 => cs(400.0, 248.0, 228.0, false),
        ASTM_F1554_105 => cs(862.0, 724.0, 0.9 * 724.0, false),
        SAE_GRADE_2 => if d_mm <= 19.05 { cs(510.0, 393.0, 379.0, false) } else { cs(414.0, 248.0, 228.0, false) },
        SAE_GRADE_5 => if d_mm <= 25.4 { cs(827.0, 634.0, 586.0, false) } else { cs(724.0, 558.0, 510.0, false) },
        SAE_GRADE_8 => cs(1034.0, 896.0, 827.0, false),
        ASTM_A574 => cs(if d_mm <= 12.7 { 1241.0 } else { 1172.0 }, 1055.0, 0.9 * 1055.0, false),
        ASTM_A193_B7 => if d_mm <= 63.5 { cs(862.0, 724.0, 0.9 * 724.0, false) } else { cs(793.0, 655.0, 0.9 * 655.0, false) },
        _ => cs(material.post_forging_target_uts_mpa, material.yield_strength_mpa, material.yield_strength_mpa * 0.9, false),
    }
}

/// Minimum Rm a material must reach to be sold as this class.
pub fn required_rm_mpa(pc: PropertyClass, d_mm: f64, material: &MaterialInfo) -> f64 {
    if is_commercial_class(pc) { class_strength(pc, d_mm, material).rm_min_mpa } else { pc.uts_mpa() }
}

use ProductStandard as PS;
pub const NUT_STANDARDS: &[ProductStandard] = &[
    PS::ISO_4032, PS::ISO_4033, PS::DIN_6923, PS::ISO_7040, PS::ISO_7042, PS::DIN_935, PS::DIN_1587, PS::DIN_929, PS::ASME_B18_2_2,
    PS::ASTM_A194, PS::MS21042, PS::MS20500, PS::DIN_74361_NUT, PS::HI_LOK_COLLAR_STD, PS::MS21047,
    PS::ISO_4035, PS::DIN_557, PS::DIN_6334, PS::DIN_508, PS::DIN_315, PS::DIN_466, PS::OEM_RIVET_NUT, PS::IFI_100,
    PS::OEM_CAGE_NUT, PS::ASTM_A563,
];
pub const WASHER_STANDARDS: &[ProductStandard] = &[
    PS::ISO_7089, PS::ISO_7093, PS::DIN_127, PS::ASTM_F436, PS::DIN_6797, PS::DIN_6796, PS::DIN_137, PS::DIN_434, PS::ASME_B18_22_1,
];

use FastenerType as FT;
const FULL_THREAD_TYPES: &[FastenerType] = &[
    FT::HEX_BOLT_FULL, FT::SOCKET_SET, FT::SOCKET_BUTTON, FT::SOCKET_CSK, FT::STUD, FT::THREAD_FORMING_SCREW,
    FT::SET_SCREW_CONE, FT::SET_SCREW_DOG, FT::SET_SCREW_CUP, FT::SLOTTED_SET_SCREW, FT::THREADED_ROD, FT::CSK_SLOT, FT::CSK_CROSS,
    FT::RAISED_CSK_CROSS, FT::PAN_SLOT, FT::PAN_CROSS, FT::PAN_HEXALOBULAR, FT::CHEESE_SLOT,
    FT::HEX_WASHER_SCREW, FT::SEMS_SCREW, FT::TAPPING_PAN, FT::TAPPING_CSK, FT::TAPPING_HEX, FT::SELF_DRILLING, FT::DRYWALL_SCREW,
];

pub fn is_fully_threaded(t: FastenerType) -> bool {
    FULL_THREAD_TYPES.contains(&t)
}

use PropertyClass as PC;
pub const STAINLESS_CLASSES: &[PropertyClass] = &[
    PC::A2_50, PC::A2_70, PC::A2_80, PC::A4_50, PC::A4_70, PC::A4_80, PC::ASTM_A193_B8_CL1, PC::ASTM_A193_B8M_CL2, PC::ASTM_A194_8,
];
pub const MARTENSITIC_FERRITIC_CLASSES: &[PropertyClass] = &[PC::C1_50, PC::C1_70, PC::C3_80, PC::C4_70, PC::F1_60];
pub const ISO_NUT_CLASSES: &[PropertyClass] = &[PC::NUT_04, PC::NUT_05, PC::NUT_5, PC::NUT_6, PC::NUT_8, PC::NUT_9, PC::NUT_10, PC::NUT_12];
pub const NUT_ONLY_CLASSES: &[PropertyClass] = &[
    PC::NUT_04, PC::NUT_05, PC::NUT_5, PC::NUT_6, PC::NUT_8, PC::NUT_9, PC::NUT_10, PC::NUT_12,
    PC::SAE_NUT_5, PC::SAE_NUT_8, PC::ASTM_A563_DH, PC::ASTM_A194_2H, PC::ASTM_A194_8,
];
pub const INCH_STEEL_CLASSES: &[PropertyClass] = &[
    PC::SAE_GRADE_2, PC::SAE_GRADE_5, PC::SAE_GRADE_8, PC::ASTM_A574, PC::ASTM_A193_B7, PC::ASTM_A193_B16, PC::ASTM_A320_L7,
    PC::ASTM_A325, PC::ASTM_A490, PC::ASTM_A307_A, PC::ASTM_A307_B, PC::ASTM_F1554_36, PC::ASTM_F1554_105,
    PC::SAE_NUT_5, PC::SAE_NUT_8, PC::ASTM_A563_DH, PC::ASTM_A194_2H,
];
pub const QT_CLASSES: &[PropertyClass] = &[
    PC::CLASS_8_8, PC::CLASS_9_8, PC::CLASS_10_9, PC::CLASS_12_9, PC::SAE_GRADE_5, PC::SAE_GRADE_8, PC::ASTM_A574, PC::ASTM_A193_B7,
    PC::ASTM_A193_B16, PC::ASTM_A320_L7, PC::ASTM_A325, PC::ASTM_A490, PC::ASTM_F1554_105, PC::ASTM_A194_2H, PC::ASTM_A563_DH, PC::SAE_NUT_8,
    PC::NUT_8, PC::NUT_9, PC::NUT_10, PC::NUT_12,
];
pub const ALUMINIUM_ONLY_COATINGS: &[CoatingType] = &[CoatingType::ANODIZE_II, CoatingType::HARD_ANODIZE, CoatingType::CHEM_FILM];
pub const PHOSPHATE_COATINGS: &[CoatingType] = &[CoatingType::PHOSPHATE, CoatingType::PHOSPHATE_MN];

/// ISO 898-2 nut property class that mates with a bolt class.
pub fn nut_class_for(pc: PropertyClass) -> &'static str {
    match pc {
        PC::CLASS_4_6 | PC::CLASS_4_8 | PC::CLASS_5_6 | PC::CLASS_5_8 => "5",
        PC::CLASS_6_8 => "6",
        PC::CLASS_9_8 => "9",
        PC::CLASS_8_8 => "8",
        PC::CLASS_10_9 => "10",
        PC::CLASS_12_9 => "12",
        PC::A2_70 => "A2-70 (ISO 3506-2)",
        PC::A4_70 => "A4-70 (ISO 3506-2)",
        PC::A2_50 => "A2-50 (ISO 3506-2)",
        PC::A4_50 => "A4-50 (ISO 3506-2)",
        PC::A2_80 => "A2-80 (ISO 3506-2)",
        PC::A4_80 => "A4-80 (ISO 3506-2)",
        PC::C1_50 | PC::C1_70 | PC::C3_80 | PC::C4_70 | PC::F1_60 => "the same C / F class (ISO 3506-2)",
        PC::ASTM_A325 | PC::ASTM_F1554_105 => "ASTM A563 Grade C or DH",
        PC::ASTM_A490 => "ASTM A563 Grade DH (or DH3)",
        PC::ASTM_A307_A | PC::ASTM_A307_B | PC::ASTM_F1554_36 => "ASTM A563 Grade A",
        PC::ASTM_A193_B16 | PC::ASTM_A320_L7 => "ASTM A194 Grade 4 or 7",
        PC::ASTM_A193_B8_CL1 => "ASTM A194 Grade 8",
        PC::ASTM_A193_B8M_CL2 => "ASTM A194 Grade 8M",
        p if NUT_ONLY_CLASSES.contains(&p) => "not applicable — this is already the nut class",
        PC::SAE_GRADE_2 => "SAE J995 Grade 2",
        PC::SAE_GRADE_5 => "SAE J995 Grade 5",
        PC::SAE_GRADE_8 => "SAE J995 Grade 8",
        PC::ASTM_A193_B7 => "ASTM A194 Grade 2H",
        PC::ASTM_A574 => "per joint design (ASTM A563 / A194)",
        PC::MATERIAL_SPEC => "per the mating nut's procurement spec (same alloy family)",
        _ => "per mating aerospace nut spec (NAS/MS)",
    }
}

/// Plain-text poka-yoke section for exported reports.
pub fn report_section(issues: &[PokaYokeIssue]) -> String {
    let critical = issues.iter().filter(|i| i.severity == PokaYokeSeverity::CRITICAL).count();
    let mut s = String::from("POKA-YOKE GATE: ");
    s.push_str(&if critical > 0 {
        format!("RELEASE BLOCKED ({} critical failure{})", critical, if critical > 1 { "s" } else { "" })
    } else if issues.iter().any(|i| i.severity == PokaYokeSeverity::MANDATORY) {
        "RELEASE WITH MANDATORY CONTROLS".into()
    } else if !issues.is_empty() {
        "OK - ENGINEERING SIGN-OFF ITEMS".into()
    } else {
        "ALL CHECKS PASSED".into()
    });
    s.push('\n');
    for i in issues {
        s.push_str(&format!(" [{}] {}\n", i.severity.name(), i.title));
        s.push_str(&format!("    {}\n", i.detail));
        s.push_str(&format!("    Fix: {}\n", i.fix));
    }
    s
}

/// The governing product standard for a fastener type in the chosen unit system.
pub fn default_standard(t: FastenerType, unit: UnitSystem) -> ProductStandard {
    if unit == UnitSystem::INCH {
        return if t.is_washer() {
            PS::ASTM_F436
        } else if t == FT::AERO_LOCK_NUT {
            PS::MS21042
        } else if t == FT::HI_LOK_COLLAR {
            PS::HI_LOK_COLLAR_STD
        } else if t == FT::NUTPLATE {
            PS::MS21047
        } else if t == FT::NAS_CLOSE_TOL_BOLT {
            PS::NAS6203
        } else if t == FT::HI_LOK_PIN {
            PS::HI_LOK
        } else if t == FT::TAPER_LOK {
            PS::TAPER_LOK_STD
        } else if t == FT::LOCKBOLT_PIN {
            PS::LOCKBOLT_STD
        } else if t == FT::BLIND_BOLT {
            PS::BLIND_BOLT_STD
        } else if t == FT::HEXALOBULAR_SCREW {
            PS::ASME_B18_3
        } else if t == FT::SOLID_RIVET {
            PS::MS20470
        } else if t == FT::BLIND_RIVET {
            PS::NAS1398
        } else if t == FT::HEAVY_HEX_NUT {
            PS::ASTM_A194
        } else if t == FT::CASTLE_NUT {
            PS::MS20500
        } else if t == FT::METAL_LOCK_NUT {
            PS::MS21042
        } else if t.is_nut() {
            PS::ASME_B18_2_2
        } else if t == FT::TWELVE_POINT {
            PS::NAS624
        } else if t == FT::HEAVY_HEX_BOLT {
            PS::ASTM_A325
        } else if t.is_stud() {
            PS::ASTM_A193
        } else if t == FT::CLEVIS_BOLT {
            PS::AN21
        } else if t == FT::SHEAR_BOLT {
            PS::AN173
        } else if t.category() == "Machine Screws" {
            PS::ASME_B18_6_3
        } else if t.category() == "Tapping & Wood Screws" {
            PS::ASME_B18_6_4
        } else if t.category() == "Pins & Retainers" {
            PS::ASME_B18_8_2
        } else if t == FT::TAP_END_STUD {
            PS::ASME_B16_5
        } else if t == FT::ANCHOR_BOLT {
            PS::ASTM_F1554
        } else if [FT::SHCS, FT::SOCKET_CSK, FT::SOCKET_BUTTON, FT::SOCKET_SET, FT::SET_SCREW_CONE, FT::SET_SCREW_DOG, FT::SET_SCREW_CUP, FT::SHOULDER_BOLT].contains(&t) {
            PS::ASME_B18_3
        } else {
            PS::ASME_B18_2_1
        };
    }
    match t {
        FT::HEX_BOLT => PS::ISO_4014,
        FT::HEX_BOLT_FULL => PS::ISO_4017,
        FT::SHCS => PS::ISO_4762,
        FT::SOCKET_CSK => PS::ISO_10642,
        FT::SOCKET_BUTTON => PS::ISO_7380,
        FT::SOCKET_SET => PS::ISO_4026,
        FT::FLANGE_BOLT => PS::ISO_1665,
        FT::TWELVE_POINT => PS::EN2882,
        FT::CARRIAGE_BOLT => PS::DIN_603,
        FT::T_BOLT => PS::DIN_186,
        FT::HEAVY_HEX_BOLT => PS::ASTM_A325,
        FT::EYE_BOLT => PS::DIN_444,
        FT::STUD | FT::WHEEL_STUD | FT::U_BOLT => PS::ASTM_A193,
        FT::HEX_NUT => PS::ISO_4032,
        FT::HEAVY_HEX_NUT => PS::ASTM_A194,
        FT::FLANGE_NUT => PS::DIN_6923,
        FT::NYLOC_NUT => PS::ISO_7040,
        FT::METAL_LOCK_NUT => PS::ISO_7042,
        FT::CASTLE_NUT => PS::DIN_935,
        FT::ACORN_NUT => PS::DIN_1587,
        FT::WELD_NUT => PS::DIN_929,
        FT::FLAT_WASHER => PS::ISO_7089,
        FT::SPRING_WASHER => PS::DIN_127,
        FT::TTY_HEAD_BOLT => PS::OEM_TTY,
        FT::HEXALOBULAR_SCREW => PS::ISO_14579,
        FT::THREAD_FORMING_SCREW => PS::DIN_7500,
        FT::WHEEL_BOLT => PS::DIN_74361,
        FT::WHEEL_NUT => PS::DIN_74361_NUT,
        FT::BANJO_BOLT => PS::DIN_7623,
        FT::NAS_CLOSE_TOL_BOLT => PS::EN2882,
        FT::HI_LOK_PIN => PS::HI_LOK,
        FT::HI_LOK_COLLAR => PS::HI_LOK_COLLAR_STD,
        FT::TAPER_LOK => PS::TAPER_LOK_STD,
        FT::LOCKBOLT_PIN => PS::LOCKBOLT_STD,
        FT::BLIND_BOLT => PS::BLIND_BOLT_STD,
        FT::AERO_LOCK_NUT => PS::MS21042,
        FT::NUTPLATE => PS::MS21047,
        FT::SOLID_RIVET => PS::MS20470,
        FT::BLIND_RIVET => PS::NAS1398,
        FT::CLEVIS_BOLT => PS::AN21,
        FT::SHEAR_BOLT => PS::AN173,
        FT::SQUARE_BOLT => PS::DIN_478,
        FT::SHOULDER_BOLT => PS::ISO_7379,
        FT::SET_SCREW_CONE => PS::ISO_4027,
        FT::SET_SCREW_DOG => PS::ISO_4028,
        FT::SET_SCREW_CUP => PS::ISO_4029,
        FT::SLOTTED_SET_SCREW => PS::DIN_438,
        FT::THUMB_SCREW => PS::DIN_464,
        FT::WING_SCREW => PS::DIN_316,
        FT::HANGER_BOLT => PS::DIN_7961,
        FT::ANCHOR_BOLT => PS::ASTM_F1554,
        FT::LAG_SCREW => PS::DIN_571,
        FT::PAN_SLOT => PS::ISO_1580,
        FT::PAN_CROSS => PS::ISO_7045,
        FT::PAN_HEXALOBULAR => PS::ISO_14583,
        FT::CHEESE_SLOT => PS::ISO_1207,
        FT::CSK_SLOT => PS::ISO_2009,
        FT::CSK_CROSS => PS::ISO_7046,
        FT::RAISED_CSK_CROSS => PS::ISO_7047,
        FT::HEX_WASHER_SCREW | FT::SEMS_SCREW => PS::DIN_6900,
        FT::TAPPING_PAN => PS::ISO_1481,
        FT::TAPPING_CSK => PS::ISO_7050,
        FT::TAPPING_HEX => PS::ISO_10509,
        FT::SELF_DRILLING => PS::ISO_15480,
        FT::DRYWALL_SCREW => PS::DIN_18182,
        FT::CHIPBOARD_SCREW => PS::DIN_7505,
        FT::CONROD_BOLT => PS::OEM_CONROD,
        FT::THREADED_ROD => PS::DIN_975,
        FT::TAP_END_STUD => PS::ASME_B16_5,
        FT::DOWEL_PIN => PS::ISO_2338,
        FT::TAPER_PIN => PS::ISO_2339,
        FT::CLEVIS_PIN => PS::ISO_2341,
        FT::SPLIT_PIN => PS::ISO_1234,
        FT::SPRING_PIN => PS::ISO_8752,
        FT::GROOVED_PIN => PS::ISO_8739,
        FT::RETAINING_RING => PS::DIN_471,
        FT::THIN_NUT => PS::ISO_4035,
        FT::HIGH_NUT => PS::ISO_4033,
        FT::SQUARE_NUT => PS::DIN_557,
        FT::COUPLING_NUT => PS::DIN_6334,
        FT::T_SLOT_NUT => PS::DIN_508,
        FT::CAGE_NUT => PS::OEM_CAGE_NUT,
        FT::WING_NUT => PS::DIN_315,
        FT::THUMB_NUT => PS::DIN_466,
        FT::RIVET_NUT => PS::OEM_RIVET_NUT,
        FT::CLINCH_NUT => PS::IFI_100,
        FT::FENDER_WASHER => PS::ISO_7093,
        FT::STRUCT_WASHER => PS::ASTM_F436,
        FT::TOOTH_LOCK_WASHER => PS::DIN_6797,
        FT::BELLEVILLE_WASHER => PS::DIN_6796,
        FT::WAVE_WASHER => PS::DIN_137,
        FT::TAPER_WASHER => PS::DIN_434,
    }
}

/// Thread length b for partially threaded bolts (ISO 4014 / ISO 4762 / ASME B18.2.1).
pub fn thread_length_b(t: FastenerType, unit: UnitSystem, d: f64, length_mm: f64) -> f64 {
    if t.is_unthreaded() {
        return 0.0;
    }
    if is_fully_threaded(t) {
        return length_mm;
    }
    let socket_head = t == FT::SHCS || t == FT::HEXALOBULAR_SCREW;
    let b = if unit == UnitSystem::INCH {
        if socket_head || length_mm > 152.4 { 2.0 * d + 12.7 } else { 2.0 * d + 6.35 }
    } else if socket_head {
        2.0 * d + 12.0
    } else if length_mm <= 125.0 {
        2.0 * d + 6.0
    } else if length_mm <= 200.0 {
        2.0 * d + 12.0
    } else {
        2.0 * d + 25.0
    };
    length_mm.min(b)
}

fn family_text(f: MaterialFamily) -> String {
    f.name().to_lowercase().replace('_', " ")
}

fn join_doubles(v: &[f64]) -> String {
    v.iter().map(|x| dstr(*x)).collect::<Vec<_>>().join(", ")
}

/// Inputs of the release gate (Kotlin `check(...)` arguments).
pub struct CheckInput<'a> {
    pub t: FastenerType,
    pub unit: UnitSystem,
    pub standard: ProductStandard,
    pub nominal_size_index: usize,
    pub pitch_value: f64,
    pub d: f64,
    pub length_mm: f64,
    pub thread_class: &'a str,
    pub property_class: Option<PropertyClass>,
    pub material: &'a MaterialInfo,
    pub coating: CoatingType,
    pub batch_qty: i32,
    pub fundamental_deviation_mm: f64,
    pub pitch_dia_min_mm: f64,
    pub pitch_dia_max_mm: f64,
    pub is_custom_scan: bool,
    pub stress_area_mm2: f64,
    pub head_dims: Option<&'a HeadDimensions>,
    pub thread_length_mm: Option<f64>,
    pub tensile_range: Option<&'a TensileRange>,
    pub thread_lock_patch: bool,
    pub head_from_drawing: bool,
}

pub fn check(i: &CheckInput) -> Vec<PokaYokeIssue> {
    let material = i.material;
    let d = i.d;
    let t = i.t;
    let strength_of = |pc: PropertyClass| i.tensile_range.map(|r| r.strength()).unwrap_or_else(|| class_strength(pc, d, material));
    // one list for all three severities, sorted by severity at the end (stable, as Kotlin's sortedBy)
    let mut critical_list: Vec<PokaYokeIssue> = Vec::new();
    macro_rules! crit {
        ($a:expr, $b:expr, $c:expr) => {
            critical_list.push(PokaYokeIssue::new(PokaYokeSeverity::CRITICAL, &$a, &$b, &$c))
        };
    }
    macro_rules! warn {
        ($a:expr, $b:expr, $c:expr) => {
            critical_list.push(PokaYokeIssue::new(PokaYokeSeverity::WARNING, &$a, &$b, &$c))
        };
    }
    macro_rules! mandatory {
        ($a:expr, $b:expr, $c:expr) => {
            critical_list.push(PokaYokeIssue::new(PokaYokeSeverity::MANDATORY, &$a, &$b, &$c))
        };
    }

    let family = material_family(material);
    let is_nut = t.is_nut();
    let is_washer = t.is_washer();
    let tr = i.tensile_range;

    // ---- 1. Inputs ----
    if i.length_mm <= 0.0 {
        crit!("Invalid length", format!("Length is {} mm.", dstr(i.length_mm)), "Select a positive nominal length.");
    }
    if i.batch_qty <= 0 {
        crit!("Invalid batch quantity", format!("Batch quantity is {}.", i.batch_qty), "Enter a quantity of at least 1.");
    }

    // ---- 2. Standard matches the product ----
    let std_is_nut = NUT_STANDARDS.contains(&i.standard);
    let std_is_washer = WASHER_STANDARDS.contains(&i.standard);
    if is_nut && !std_is_nut {
        crit!("Standard does not fit a nut", format!("{} is not a nut standard, but {} is selected.", i.standard.code(), t.label()), "Choose a nut standard (e.g. ISO 4032, ISO 7040, ASME B18.2.2).");
    } else if is_washer && !std_is_washer {
        crit!("Standard does not fit a washer", format!("{} is not a washer standard.", i.standard.code()), "Choose a washer standard (ISO 7089, DIN 127, ASTM F436).");
    } else if !is_nut && !is_washer && (std_is_nut || std_is_washer) {
        crit!("Standard does not fit a bolt/screw", format!("{} is a {} standard, but {} is selected.", i.standard.code(), if std_is_nut { "nut" } else { "washer" }, t.label()), "Choose the bolt/screw standard for this product.");
    }
    if i.unit == UnitSystem::INCH && is_metric_only_standard(i.standard) {
        crit!("Metric standard with inch thread", format!("{} defines metric fasteners only.", i.standard.code()), "Use an ASME/ASTM standard for UNC/UNF threads, or switch to Metric.");
    }
    if i.unit == UnitSystem::METRIC && is_inch_standard(i.standard) {
        warn!("Inch standard with metric thread", format!("{} is an inch standard; the metric version has its own number (e.g. ASTM A325M / F3125M).", i.standard.code()), "Confirm the metric edition of the standard on the drawing.");
    }

    // ---- 3. Thread: pitch and tolerance class ----
    let tc = i.thread_class;
    if !is_washer && !i.is_custom_scan {
        if i.unit == UnitSystem::METRIC {
            if let Some(size) = METRIC_SIZES.get(i.nominal_size_index) {
                if !size.pitches.iter().any(|p| (p - i.pitch_value).abs() < 1e-6) {
                    crit!("Non-standard pitch", format!("{} does not have a {} mm pitch in ISO 261 (allowed: {}).", size.label, dstr(i.pitch_value), join_doubles(size.pitches)), "Select a standard coarse or fine pitch.");
                }
            }
        } else if let Some(size) = INCH_SIZES.get(i.nominal_size_index) {
            if !size.tpis.iter().any(|&p| (p as f64 - i.pitch_value).abs() < 1e-6) {
                crit!("Non-standard TPI", format!("{} does not have {} TPI in ASME B1.1 (allowed: {}).", size.label, i.pitch_value as i32, size.tpis.iter().map(|x| x.to_string()).collect::<Vec<_>>().join(", ")), "Select the UNC or UNF thread for this size.");
            }
        }
        let options = thread_class_catalog::get_options(i.unit, is_nut);
        let std_classes = standard_thread_classes(i.standard, is_nut)
            .map(|v| v.into_iter().filter(|c| options.contains(c)).collect::<Vec<_>>())
            .filter(|v| !v.is_empty());
        let aero_thread = (tc.contains("UNJ") || tc.ends_with("-MJ")) && (material.is_aero || tr.is_some());
        if let Some(sc) = &std_classes {
            if !sc.contains(&tc) && options.contains(&tc) && !aero_thread {
                let extra = if sc.len() > 1 { format!(" (or {} for coated parts)", sc[1..].join(" / ")) } else { String::new() };
                warn!(format!("Thread class not per {}", i.standard.code()), format!("{} specifies {}{}; {} is a special class.", i.standard.code(), sc[0], extra, tc), format!("Use {}, or put the special class on the drawing as a deviation.", sc.join(" / ")));
            }
        }
        if !options.contains(&tc) {
            crit!("Wrong thread tolerance class", format!("'{}' is not a valid {} {} class.", tc, if is_nut { "internal" } else { "external" }, i.unit.name().to_lowercase()), format!("Use one of: {}.", options.join(", ")));
        }
    }
    if is_washer {
        warn!("Washer selected", "Washers have no thread, so the thread limits, rolling blank and G-code are not applicable.", "Use the weight, material, coating and hardness results only.");
    }

    // ---- 4. Material vs property class ----
    let pc = if is_washer { None } else { i.property_class };
    if let Some(pc) = pc {
        let csx = strength_of(pc);
        let pcs = before_paren(pc.label());
        if (csx.is_iso_steel_class || INCH_STEEL_CLASSES.contains(&pc)) && family != MaterialFamily::CARBON_ALLOY_STEEL {
            crit!(format!("{} needs carbon/alloy steel", pcs), format!("ISO 898-1 classes apply only to carbon and alloy steel; {} is {}.", material.name, family_text(family)), "Use the material's own class (A2-70/A4-80 for austenitic stainless, aerospace class for Ti/Ni alloys) or change the material.");
        } else if STAINLESS_CLASSES.contains(&pc) && !is_austenitic_a2_a4(material) {
            crit!(format!("{} needs A2/A4 austenitic stainless", pcs), format!("ISO 3506-1 A2/A4 classes cover 304-type (A2) and 316-type (A4) steels only; {} is not one of them.", material.name), "Select SS304 / SS316 / SS316L, or use 'Per material spec' for martensitic, PH and other stainless grades.");
        } else if MARTENSITIC_FERRITIC_CLASSES.contains(&pc) && family != MaterialFamily::STAINLESS {
            crit!(format!("{} needs a stainless steel", pcs), format!("ISO 3506-1 C and F classes are martensitic (C) and ferritic (F) stainless grades; {} is {}.", material.name, family_text(family)), "Select a martensitic / ferritic stainless (410, 1.4057, 420) or change the class.");
        } else if MARTENSITIC_FERRITIC_CLASSES.contains(&pc) && is_austenitic_a2_a4(material) {
            crit!(format!("{} is not an austenitic class", pcs), format!("C and F classes are hardened martensitic / ferritic grades; {} is an austenitic 304/316-type steel that cannot be quenched and tempered.", material.name), "Use A2 / A4 for austenitic stainless, or select a martensitic grade.");
        } else if pc == PC::MATERIAL_SPEC && family == MaterialFamily::CARBON_ALLOY_STEEL && !material.is_aero && tr.is_none() {
            crit!("Commercial steel needs a grade", format!("{} fasteners are graded by ISO 898-1 (metric) or SAE J429 / ASTM (inch).", material.name), "Select the ISO / SAE property class.");
        } else if pc == PC::AERO_AL && family != MaterialFamily::ALUMINUM {
            crit!("Aluminium class with non-aluminium material", format!("{} is not an aluminium alloy.", material.name), "Select an aluminium alloy (2024, 7075, 2219) or change the class.");
        } else if pc == PC::AERO_1100 && family != MaterialFamily::TITANIUM {
            warn!("Titanium class with non-titanium material", format!("Class {} is defined for Ti-6Al-4V; {} selected.", pc.label(), material.name), "Confirm the procurement spec, or select a titanium alloy.");
        }
        let required_rm = tr.map(|r| r.min_mpa).unwrap_or_else(|| required_rm_mpa(pc, d, material));
        let req_label = tr.map(|r| r.label()).unwrap_or_else(|| pcs.to_string());
        if material.post_forging_target_uts_mpa < required_rm * 0.98 {
            crit!("Material cannot reach the class strength", format!("{} reaches about {} MPa, but {} needs at least {} MPa.", material.name, material.post_forging_target_uts_mpa as i32, req_label, required_rm as i32), "Select a stronger, hardenable material (e.g. 4140/42CrMo4 for 12.9, 10B21 or 4135 for 10.9).");
        }
        if let Some(r) = tr {
            if r.max_mpa - r.min_mpa < 120.0 && family == MaterialFamily::CARBON_ALLOY_STEEL {
                warn!("Narrow tensile band", format!("{} is only {} MPa wide (~{} HRC): normal Q&T scatter can fall outside it.", r.label(), (r.max_mpa - r.min_mpa) as i32, jfmt::f(hardness_conversion::hrc_for(r.max_mpa) - hardness_conversion::hrc_for(r.min_mpa), 0)), "Tight furnace uniformity (AMS 2750 class 2 or better) and per-load hardness checks; agree the band with the customer.");
            }
        }
        if NUT_ONLY_CLASSES.contains(&pc) && !is_nut {
            crit!(format!("Nut class on a {}", if t.is_stud() { "stud" } else { "bolt / screw" }), format!("{} is a nut class: it is specified by proof load and hardness, with no tensile strength for a threaded shank.", pcs), "Select the bolt class (ISO 898-1 for metric, SAE J429 / ASTM for inch).");
        }
        if is_nut && !NUT_ONLY_CLASSES.contains(&pc) && !STAINLESS_CLASSES.contains(&pc) && pc != PC::MATERIAL_SPEC && !is_aerospace_type(t) {
            warn!("Bolt class on a nut", format!("{} is a bolt class; a nut is graded by ISO 898-2 (class {}) or by SAE J995 / ASTM A563 / A194 in inch.", pcs, nut_class_for(pc)), "Select the matching nut class so the proof load and hardness limits are the nut's own.");
        }
        if (pc == PC::NUT_04 || pc == PC::NUT_05) && t != FT::THIN_NUT {
            warn!("Thin-nut class on a full-height nut", format!("ISO 898-2 classes 04 and 05 are for thin nuts (m < 0.8 d) with a reduced proof load; {} is a full-height nut.", before_paren(t.label())), "Use class 5 / 6 / 8 / 10 / 12 for a full-height nut.");
        }
        if pc == PC::CLASS_9_8 && d > 16.0 {
            crit!("Class 9.8 is limited to M16", format!("ISO 898-1 defines class 9.8 only for d ≤ 16 mm; M{} selected.", dstr(d)), "Use class 8.8 or 10.9 for this size.");
        }
        if material.is_aero && is_commercial_class(pc) {
            warn!("Aerospace alloy on a commercial class", format!("{} is bought to an aerospace (AMS / VAR) specification; {} is a commercial grade made from commercial wire.", material.name, pcs), "Use a commercial grade (e.g. 10B21, 42CrMo4, 4140) or class 'Per material spec' for the aerospace alloy.");
        }
        if is_aerospace_type(t) && (csx.is_iso_steel_class || INCH_STEEL_CLASSES.contains(&pc) || STAINLESS_CLASSES.contains(&pc)) {
            warn!("Commercial class on an aerospace fastener", format!("{} is bought to an aerospace procurement spec (NAS / MS / AMS), not {}.", before_paren(t.label()), pcs), "Use an aerospace class or 'Per material spec' with the alloy's AMS specification.");
        }
        if INCH_STEEL_CLASSES.contains(&pc) && i.unit == UnitSystem::METRIC && pc != PC::ASTM_A574 && pc != PC::ASTM_A193_B7 {
            warn!("SAE J429 grade on a metric thread", "SAE J429 grades are defined for inch fasteners.", "Use an ISO 898-1 property class for metric threads.");
        }
        if QT_CLASSES.contains(&pc) && !material.has_high_hardness && family == MaterialFamily::CARBON_ALLOY_STEEL {
            crit!("Material is not quench-and-temper grade", format!("{} cannot be quenched and tempered to {}.", material.name, pcs), "Use a medium-carbon, boron or alloy steel.");
        }
    }

    // ---- 5. Coating vs material ----
    let coating = i.coating;
    let effective_rm = tr.map(|r| r.max_mpa).unwrap_or_else(|| match pc {
        Some(p) => class_strength(p, d, material).rm_min_mpa,
        None => material.post_forging_target_uts_mpa,
    });
    let cname = coating.name();
    let is_zinc_based = cname.starts_with("ZN") || cname.starts_with("ZINC") || coating == CoatingType::HOT_DIP_GALV;
    let is_passivation = cname.starts_with("PASSIVATION");
    let is_cadmium = cname.contains("CAD");

    if is_passivation && family != MaterialFamily::STAINLESS {
        crit!("Passivation on non-stainless material", format!("Passivation (ASTM A967 / AMS 2700) only works on stainless steel; {} will rust.", material.name), "Select a protective coating (Zn-Ni, zinc flake, phosphate & oil).");
    }
    if family == MaterialFamily::TITANIUM && is_cadmium {
        crit!("Cadmium on titanium is prohibited", "Cadmium causes solid-metal embrittlement of titanium (forbidden by aerospace specs).", "Use silver plating, aluminium-pigmented coating or dry-film lubricant.");
    }
    if family == MaterialFamily::TITANIUM && coating.is_electroplating() {
        crit!("Electroplating on titanium", "Titanium fasteners are not electroplated: hydrogen pick-up embrittles titanium.", "Use anodizing, dry-film lubricant or an aluminium-pigmented coating.");
    }
    if (family == MaterialFamily::STAINLESS || family == MaterialFamily::NICKEL_SUPERALLOY) && is_zinc_based {
        warn!("Zinc coating on corrosion-resistant alloy", format!("{} does not need sacrificial zinc; zinc also melts/embrittles above ~400 °C.", material.name), "Use passivation (stainless) or silver/dry-film lubricant (superalloys).");
    }
    if family == MaterialFamily::ALUMINUM && (is_zinc_based || is_cadmium) {
        warn!("Zinc/cadmium on aluminium fastener", "Aluminium fasteners are normally anodized or chromate-converted.", "Specify anodize (MIL-A-8625) or chemical conversion coating.");
    }
    if is_cadmium && !material.is_aero {
        warn!("Cadmium is restricted", "Cadmium is banned for general industry under RoHS/REACH; allowed only for aerospace/defence.", "Use Zn-Ni (AMS 2417) as the cadmium replacement.");
    }

    // ---- 6. Hydrogen embrittlement ----
    let steel_like = family == MaterialFamily::CARBON_ALLOY_STEEL || (family == MaterialFamily::STAINLESS && material.has_high_hardness);
    if coating.is_electroplating() && steel_like && effective_rm >= 1000.0 {
        let aerospace_cadmium = is_cadmium && (material.is_aero || is_aerospace_type(t) || tr.is_some());
        if (pc == Some(PC::CLASS_12_9) || effective_rm >= 1200.0) && aerospace_cadmium {
            mandatory!("High-strength steel cadmium plated: hydrogen controls", format!("{} up to {} MPa with {}: high hydrogen-embrittlement sensitivity at this strength.", material.name, effective_rm as i32, before_paren(coating.label())), "Stress relieve before cleaning if the plating spec requires it, use a low-embrittlement cadmium process, bake within 4 h of plating per AMS-QQ-P-416, and qualify the line with ASTM F519 notched-bar tests.");
        } else if pc == Some(PC::CLASS_12_9) || effective_rm >= 1200.0 {
            crit!("Electroplating ≥ 1200 MPa fasteners", "ISO 4042 does not recommend electroplating for class 12.9 / ≥ 1200 MPa: baking cannot reliably remove the hydrogen-embrittlement risk.", "Use a non-electrolytic zinc-flake coating (Geomet / Dacromet, ISO 10683) or mechanical zinc.");
        } else {
            mandatory!("De-embrittlement bake required", format!("{} at {} MPa with {}: hydrogen embrittlement risk.", material.name, effective_rm as i32, coating.label()), "Bake 190–220 °C for 8–24 h, starting within 4 h of plating (ISO 4042 / ASTM F1941), and verify with ASTM F1940 / ISO 15330.");
        }
        if t == FT::NYLOC_NUT {
            warn!("Nylon insert vs bake temperature", "The polyamide insert is damaged above ~120 °C, but the bake is ~200 °C.", "Plate and bake the nut body first, then insert the nylon ring.");
        }
        if CHROMATED_COATINGS.contains(&coating) {
            mandatory!("Bake before the chromate / passivate", format!("{} carries a chromate or passivate top layer, which cracks and loses its corrosion protection above ~66 °C; the de-embrittlement bake runs at ~190 °C.", before_paren(coating.label())), "Sequence: plate → bake (within 4 h) → chromate / passivate. Never bake a chromated part.");
        }
    }
    if i.thread_lock_patch && !t.is_unthreaded() && !is_washer {
        if coating.is_electroplating() && steel_like && effective_rm >= 1000.0 {
            mandatory!("Locking patch after the bake", "A nylon patch / pellet softens above ~120 °C; the hydrogen bake runs at ~190 °C.", "Sequence: plate → bake → chromate → apply patch. Record the patch lot on the traveller.");
        }
        mandatory!("Gauge the thread before the patch", "The GO ring gauge cannot pass over a nylon patch, and the patch hides the pitch diameter.", "Gauge the thread (GO / NO-GO and UNJ root radius) after plating and before patching; test prevailing torque on patched parts per the customer's locking-element spec (e.g. MIL-DTL-18240).");
    }

    // ---- 7. Hot-dip galvanizing (ISO 10684) ----
    if coating == CoatingType::HOT_DIP_GALV {
        if pc == Some(PC::CLASS_12_9) || effective_rm >= 1200.0 {
            crit!("Hot-dip galvanizing on 12.9", "ISO 10684 excludes class 12.9: risk of liquid-metal and hydrogen embrittlement.", "Use zinc flake (ISO 10683) or change the class.");
        } else if pc == Some(PC::CLASS_10_9) {
            warn!("Hot-dip galvanizing on 10.9", "Allowed by ISO 10684 only with special process control (e.g. ASTM F3125 Grade A490 forbids it).", "Confirm the customer standard permits HDG for 10.9.");
        }
        if d < 8.0 {
            warn!("Hot-dip galvanizing below M8", "ISO 10684 covers M8–M64; smaller threads fill with zinc.", "Use electro-zinc or zinc flake for small sizes.");
        }
        if !is_nut && !is_washer && !t.is_unthreaded() && !tc.contains('e') && !tc.contains("1A") {
            crit!("HDG thread needs oversize fit", format!("45–65 µm zinc will not fit a {} external thread.", tc), "Use 6az / 6e external with oversize-tapped nuts (6AZ/6AX per ISO 965-5), or undercut the bolt thread before galvanizing.");
        }
    }

    // ---- 7b. Coating suits the material ----
    if ALUMINIUM_ONLY_COATINGS.contains(&coating) && family != MaterialFamily::ALUMINUM {
        crit!(format!("{} is for aluminium only", before_paren(coating.label())), format!("{} is not an aluminium alloy; anodizing and chromate conversion only form on aluminium.", material.name), "Choose a coating made for this material (e.g. Zn-Ni or zinc flake for steel, passivation for stainless).");
    }
    if coating == CoatingType::TI_ANODIZE && family != MaterialFamily::TITANIUM {
        crit!("Titanium anodize needs a titanium alloy", format!("{} is not titanium.", material.name), "Select a titanium alloy or a coating made for this material.");
    }
    if PHOSPHATE_COATINGS.contains(&coating) && family != MaterialFamily::CARBON_ALLOY_STEEL {
        crit!("Phosphating needs carbon or alloy steel", format!("{} converts free iron in the surface; {} is {} and will not take the coating.", before_paren(coating.label()), material.name, family_text(family)), "Use passivation (stainless), anodize (titanium / aluminium) or a zinc-flake / dry-film coating.");
    }
    if coating == CoatingType::BLACK_OXIDE && (family == MaterialFamily::ALUMINUM || family == MaterialFamily::TITANIUM) {
        crit!("Black oxide needs a ferrous or copper alloy", format!("{} forms no black oxide: there is no iron to convert to magnetite.", material.name), "Use black anodize (MIL-PRF-8625 Type II, dyed) for aluminium, or a dry-film lubricant for titanium.");
    }

    // ---- 8. Coating thickness vs thread allowance ----
    if !t.is_unthreaded() && !is_washer && coating.thickness_max_um() > 2.0 && coating != CoatingType::HOT_DIP_GALV {
        let allowance_um = i.fundamental_deviation_mm.abs() * 1000.0;
        let growth_um = 4.0 * coating.thickness_max_um();
        let excess_um = growth_um - allowance_um;
        if excess_um > 0.5 {
            let msg = format!("Max coating {} µm adds ~{} µm to the pitch diameter; class {} only has {} µm allowance.", coating.thickness_max_um() as i32, growth_um as i32, tc, allowance_um as i32);
            let (pmin, pmax) = (i.pitch_dia_min_mm, i.pitch_dia_max_mm);
            if is_nut {
                warn!("Coating thicker than nut thread allowance", msg, "Use 6G (internal allowance) or mask the internal thread during coating.");
            } else {
                let pre_plate_max = pmax - excess_um / 1000.0;
                let td2_all = (pmax - pmin) * 1000.0;
                let t_cap = (allowance_um + 0.75 * td2_all) / 4.0;
                if pre_plate_max <= pmin && t_cap >= coating.thickness_min_um() {
                    let cap_max = pmax - (4.0 * t_cap - allowance_um) / 1000.0;
                    mandatory!("Control coating thickness on the thread", format!("{} At the full {} µm nothing of the pitch-diameter tolerance is left.", msg, coating.thickness_max_um() as i32),
                        format!("Hold the coating on the thread to {}–{} µm (agree with the plater) and roll pre-plate pitch diameter {} – {} mm; gauge GO / NO-GO after plating.", coating.thickness_min_um() as i32, t_cap as i32, jfmt::f(pmin, 3), jfmt::f(cap_max, 3)));
                } else if pre_plate_max <= pmin {
                    crit!("Coating consumes the whole thread tolerance", format!("{} Pre-plate pitch diameter would have to be below the lower limit {} mm.", msg, jfmt::f(pmin, 3)), "Use class 6f/6e (2A for inch) or a thinner coating.");
                } else {
                    let td2_um = (pmax - pmin) * 1000.0;
                    let band_um = (pre_plate_max - pmin) * 1000.0;
                    let t_ctrl_um = (allowance_um + 0.75 * td2_um) / 4.0;
                    if band_um < 0.25 * td2_um && t_ctrl_um >= coating.thickness_min_um() {
                        let ctrl_max = pmax - (4.0 * t_ctrl_um - allowance_um) / 1000.0;
                        mandatory!("Control coating thickness on the thread", format!("{} At the full {} µm the pre-plate band is only {} µm wide, which rolling cannot hold.", msg, coating.thickness_max_um() as i32, band_um as i32),
                            format!("Hold the coating on the thread to {}–{} µm (agree with the plater) and roll pre-plate pitch diameter {} – {} mm; gauge GO / NO-GO after plating.", coating.thickness_min_um() as i32, t_ctrl_um as i32, jfmt::f(pmin, 3), jfmt::f(ctrl_max, 3)));
                    } else {
                        mandatory!("Roll thread undersize before plating", msg, format!("Pre-plate pitch diameter {} – {} mm (instead of up to {} mm), then check with the GO/NO-GO gauge after plating.", jfmt::f(pmin, 3), jfmt::f(pre_plate_max, 3), jfmt::f(pmax, 3)));
                    }
                }
            }
        }
    }

    // ---- 8b. Bolt–nut matching ----
    if !t.is_unthreaded() && !is_washer {
        if let Some(pc) = pc {
            let nut_class = nut_class_for(pc);
            let min_load_kn = strength_of(pc).rm_min_mpa * i.stress_area_mm2 / 1000.0;
            let iso_nut = strength_of(pc).is_iso_steel_class;
            let pcs = before_paren(pc.label());
            if is_nut {
                mandatory!(if iso_nut { format!("Nut class {} (ISO 898-2)", nut_class) } else { format!("Nut grade: {}", nut_class) }, format!("Matches {} bolts. Proof load must be at least the mating bolt's minimum breaking load, {} kN, so the bolt breaks before the thread strips.", pcs, jfmt::f(min_load_kn, 1)), format!("Mark the nut '{}' and certify the proof-load test per ISO 898-2.", nut_class));
                if i.standard == PS::ISO_4033 {
                    warn!("Thin nut (ISO 4033)", "Thin/jam nuts are classes 04/05 and cannot carry the full bolt load.", "Use ISO 4032 or ISO 4033 only as a lock/jam nut.");
                }
            } else {
                let who = tr.map(|r| r.label()).unwrap_or_else(|| pcs.to_string());
                mandatory!(if iso_nut { format!("Mate with nut class {} or higher", nut_class) } else { format!("Mate with nut: {}", nut_class) },
                    format!("A weaker nut or tapped hole strips before a {} {} reaches its {} kN breaking load.", who, if t.category() == "Machine Screws" { "screw" } else { "bolt" }, jfmt::f(min_load_kn, 1)),
                    if iso_nut { format!("Specify the nut on the BOM as class {}+ (e.g. ISO 4032-{}) with the same thread size.", nut_class, nut_class) } else { format!("Specify the nut on the BOM as {} with the same thread size.", nut_class) });
            }
        }
    }

    // ---- 9. Geometry & manufacturability ----
    if t.is_unthreaded() {
        warn!("Rivet joints are designed in shear", "Tensile proof load, thread-root Kt and fatigue figures assume a threaded bolt; for a rivet they are shank-area values only.", "Size the joint from rivet shear and bearing allowables (MMPDS) and the rivet procurement spec.");
    }
    if i.head_from_drawing {
        // head sizes come from the drawing
    } else if let Some(hd) = i.head_dims.filter(|h| !is_washer && !h.is_table && h.source.starts_with("Approximated")) {
        warn!("No dimension table for this type", format!("{}.", hd.source), "Take head and nut dimensions from the part drawing before releasing weights, tooling or G-code.");
    } else if i.head_dims.map_or(false, |h| !is_washer && !h.is_table) {
        warn!("Head/nut size estimated", format!("M{} is outside the ISO/ASME dimension tables, so head and nut dimensions are proportional estimates.", dstr(d)), "Take head dimensions from the drawing before releasing weights, tooling or G-code.");
    }
    let threaded_shank = !is_nut && !is_washer && !t.is_unthreaded() && !t.is_stud();
    if let Some(tl) = i.thread_length_mm {
        if threaded_shank && i.length_mm > 0.0 && tl > i.length_mm {
            warn!("Thread length longer than the part", format!("Thread length b = {} mm is more than L = {} mm.", jfmt::f(tl, 1), jfmt::f(i.length_mm, 1)), "Check b and L against the customer drawing; the part is calculated as fully threaded.");
        }
    }
    if threaded_shank && i.thread_length_mm.is_none() && is_aerospace_type(t) && !is_fully_threaded(t) {
        warn!("Aerospace thread length not from the drawing", "Thread length b uses the commercial ISO/ASME rule. NAS, AN, MS and OEM bolts take a fixed thread length per diameter from their standard sheet, or the customer's value.", "Type the thread length b from the customer drawing or the NAS/AN sheet on the Sizing card.");
    }
    if !is_nut && !is_washer && t == FT::HEX_BOLT {
        let b = i.thread_length_mm.unwrap_or_else(|| thread_length_b(t, i.unit, d, i.length_mm));
        if i.length_mm < b + d {
            let std = if i.unit == UnitSystem::METRIC { "ISO 4014" } else { "ASME B18.2.1" };
            let full = if i.unit == UnitSystem::METRIC { "ISO 4017 (fully threaded)" } else { "a fully threaded hex tap bolt" };
            warn!("Too short for a partially threaded bolt", format!("{} needs L ≥ b + plain shank; at L = {} mm the thread length b = {} mm leaves almost no shank.", std, i.length_mm as i32, b as i32), format!("Use {} for this length.", full));
        }
    }
    let short_limit = if t.category() == "Machine Screws" || t == FT::SOCKET_BUTTON || t == FT::SOCKET_CSK { 1.0 } else { 2.0 };
    if !is_nut && !is_washer && !t.is_unthreaded() && i.length_mm > 0.0 && i.length_mm < short_limit * d && t != FT::SOCKET_SET {
        warn!("Very short length for diameter", format!("L = {} mm is less than {}d ({} mm).", jfmt::f(i.length_mm, 2), if short_limit == 1.0 { "" } else { "2 × " }, jfmt::f(short_limit * d, 2)), "Check the head can be formed and the thread fully engaged.");
    }
    if !material.is_aero && d < 1.6 && !is_washer {
        warn!("Below standard size range", format!("ISO 4014/4017/4032 start at M1.6; M{} is micro-screw territory (ISO 1207 / ISO 7045).", dstr(d)), "Confirm the micro-fastener standard and specialized heading/rolling equipment.");
    }
    if t == FT::NYLOC_NUT && (family == MaterialFamily::NICKEL_SUPERALLOY || (material.is_aero && family != MaterialFamily::ALUMINUM)) {
        warn!("Nylon insert in high-temperature alloy", "Nylon-insert nuts are limited to ~120 °C service.", "Use an all-metal prevailing-torque nut (ISO 7042 / MS21042).");
    }

    let mut issues = critical_list;
    issues.sort_by_key(|x| x.severity.ordinal());
    issues
}
