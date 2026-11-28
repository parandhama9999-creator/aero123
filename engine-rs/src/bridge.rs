//! The app's door into the engine: a function name plus its arguments as a JSON array, answered with
//! `{"ok": result}` or `{"err": message}`. The argument order and the JSON shapes are those of the
//! Kotlin functions the app calls (Kotlin `Pair` / `Triple` as `{first, second[, third]}`), so the
//! Kotlin side only has to encode its arguments and decode the answer.

use crate::calculation::{self, CalcInput, FastenerCalculationResult};
use crate::catalog::{self, HeatTreatmentSpec, MaterialInfo};
use crate::drawing_scan::{self, ExtractedFastenerSpec};
use crate::heading::{HeadingPlan, HeadingStation, ProfileSegment};
use crate::heading_animation::{self, AnimFrame};
use crate::pairs::{p2, p3};
use crate::part_geometry::{self, DrawingGeometry, FinishedPart};
use crate::poka_yoke::{self, CheckInput, PokaYokeIssue};
use crate::tensile::{hardness_conversion, joint_grip, mill_cert_check, temper_guide, MillCert, TensileRange, WireCheck};
use crate::thread::thread_rolling_blank;
use crate::tooling::{self, extrusion_die, MachineSetup, StationExtrusion, ToolingSheet};
use crate::{bolt_forging, dimensions, heat_treat_cycle, production_report, quality, raw_material, selection_filter, soft_anneal, steel};
use serde::de::DeserializeOwned;
use serde::Serialize;
use serde_json::{json, Value};
use std::sync::Mutex;

/// Every material the app knows (catalogue + the user's own), for the selection filters.
static MATERIALS: Mutex<Vec<MaterialInfo>> = Mutex::new(Vec::new());

struct Args(Vec<Value>);

impl Args {
    fn get<T: DeserializeOwned>(&self, i: usize) -> Result<T, String> {
        let v = self.0.get(i).cloned().unwrap_or(Value::Null);
        serde_json::from_value(v).map_err(|e| format!("argument {i}: {e}"))
    }
}

fn ok<T: Serialize>(v: T) -> Result<Value, String> {
    serde_json::to_value(v).map_err(|e| e.to_string())
}

/// Runs one engine call. `args` is a JSON array.
pub fn dispatch(method: &str, args: &str) -> String {
    let parsed: Result<Vec<Value>, String> = serde_json::from_str(args).map_err(|e| format!("arguments: {e}"));
    let out = parsed.and_then(|a| call(method, &Args(a)));
    match out {
        Ok(v) => json!({ "ok": v }).to_string(),
        Err(e) => json!({ "err": format!("{method}: {e}") }).to_string(),
    }
}

fn call(method: &str, a: &Args) -> Result<Value, String> {
    match method {
        // ---------------- catalogue
        "FastenerCatalog.defaultMaterials" => ok(catalog::materials()),
        "Engine.registerMaterials" => {
            let list: Vec<MaterialInfo> = a.get(0)?;
            let mut reg = MATERIALS.lock().map_err(|e| e.to_string())?;
            for m in list {
                match reg.iter_mut().find(|x| x.key == m.key) {
                    Some(x) => *x = m,
                    None => reg.push(m),
                }
            }
            ok(reg.len())
        }

        // ---------------- calculation
        "CalculationEngine.calculate" => {
            let mut c = CalcInput::new(a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?, a.get(5)?, &a.get::<String>(6)?, a.get(7)?, a.get(8)?, a.get(9)?);
            c.currency_is_usd = a.get(10)?;
            c.cnc_hour_rate_inr = a.get(11)?;
            c.ht_rate_per_kg_inr = a.get(12)?;
            c.margin_pct = a.get(13)?;
            c.custom_nominal_dia_mm = a.get(14)?;
            c.extracted_radius = a.get(15)?;
            c.property_class = a.get(16)?;
            c.drawing = a.get(17)?;
            c.tensile_range = a.get(18)?;
            c.thread_lock_patch = a.get(19)?;
            c.mill_cert = a.get(20)?;
            c.wire_dia_override_mm = a.get(21)?;
            c.open_extrusion_limit_pct = a.get(22)?;
            c.header_choice = a.get(23)?;
            ok(calculation::calculate(&c))
        }
        "CalculationEngine.isWarmHeaded" => ok(calculation::is_warm_headed(&a.get::<MaterialInfo>(0)?, a.get(1)?)),
        "CalculationEngine.calculateDynamicHeatTreatment" => {
            ok(calculation::calculate_dynamic_heat_treatment(&a.get::<MaterialInfo>(0)?, a.get(1)?, a.get(2)?, a.get(3)?))
        }
        "CalculationEngine.heatTreatForRange" => {
            ok(calculation::heat_treat_for_range(&a.get::<HeatTreatmentSpec>(0)?, &a.get::<MaterialInfo>(1)?, &a.get::<TensileRange>(2)?))
        }
        "CalculationEngine.evaluateShopGauge" => ok(calculation::evaluate_shop_gauge(a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?)),
        "CalculationEngine.classDuctility" => ok(calculation::class_ductility(a.get(0)?).map(p2)),

        // ---------------- poka-yoke
        "PokaYokeEngine.materialFamily" => ok(poka_yoke::material_family(&a.get::<MaterialInfo>(0)?)),
        "PokaYokeEngine.stainlessType" => ok(poka_yoke::stainless_type(&a.get::<MaterialInfo>(0)?)),
        "PokaYokeEngine.isAusteniticA2A4" => ok(poka_yoke::is_austenitic_a2_a4(&a.get::<MaterialInfo>(0)?)),
        "PokaYokeEngine.isInchStandard" => ok(poka_yoke::is_inch_standard(a.get(0)?)),
        "PokaYokeEngine.isMetricOnlyStandard" => ok(poka_yoke::is_metric_only_standard(a.get(0)?)),
        "PokaYokeEngine.standardThreadClasses" => ok(poka_yoke::standard_thread_classes(a.get(0)?, a.get(1)?)),
        "PokaYokeEngine.isCommercialClass" => ok(poka_yoke::is_commercial_class(a.get(0)?)),
        "PokaYokeEngine.isAerospaceType" => ok(poka_yoke::is_aerospace_type(a.get(0)?)),
        "PokaYokeEngine.classStrength" => ok(poka_yoke::class_strength(a.get(0)?, a.get(1)?, &a.get::<MaterialInfo>(2)?)),
        "PokaYokeEngine.requiredRmMpa" => ok(poka_yoke::required_rm_mpa(a.get(0)?, a.get(1)?, &a.get::<MaterialInfo>(2)?)),
        "PokaYokeEngine.isFullyThreaded" => ok(poka_yoke::is_fully_threaded(a.get(0)?)),
        "PokaYokeEngine.nutClassFor" => ok(poka_yoke::nut_class_for(a.get(0)?)),
        "PokaYokeEngine.reportSection" => ok(poka_yoke::report_section(&a.get::<Vec<PokaYokeIssue>>(0)?)),
        "PokaYokeEngine.defaultStandard" => ok(poka_yoke::default_standard(a.get(0)?, a.get(1)?)),
        "PokaYokeEngine.threadLengthB" => ok(poka_yoke::thread_length_b(a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?)),
        "PokaYokeEngine.check" => {
            let m: MaterialInfo = a.get(9)?;
            let tc: String = a.get(7)?;
            let hd: Option<dimensions::HeadDimensions> = a.get(17)?;
            let tr: Option<TensileRange> = a.get(19)?;
            ok(poka_yoke::check(&CheckInput {
                t: a.get(0)?,
                unit: a.get(1)?,
                standard: a.get(2)?,
                nominal_size_index: a.get(3)?,
                pitch_value: a.get(4)?,
                d: a.get(5)?,
                length_mm: a.get(6)?,
                thread_class: &tc,
                property_class: a.get(8)?,
                material: &m,
                coating: a.get(10)?,
                batch_qty: a.get(11)?,
                fundamental_deviation_mm: a.get(12)?,
                pitch_dia_min_mm: a.get(13)?,
                pitch_dia_max_mm: a.get(14)?,
                is_custom_scan: a.get(15)?,
                stress_area_mm2: a.get(16)?,
                head_dims: hd.as_ref(),
                thread_length_mm: a.get(18)?,
                tensile_range: tr.as_ref(),
                thread_lock_patch: a.get(20)?,
                head_from_drawing: a.get(21)?,
            }))
        }

        // ---------------- dimensions, forging, geometry
        "DimensionTables.forPart" => ok(dimensions::for_part(a.get(0)?, a.get(1)?, a.get(2)?)),
        "DimensionTables.socketFor" => ok(dimensions::socket_for(a.get(0)?, a.get(1)?, a.get(2)?).map(p2)),
        "DimensionTables.washerFor" => ok(dimensions::washer_for(a.get(0)?, a.get(1)?, a.get(2)?)),
        "DimensionTables.din6923Flange" => ok(dimensions::din6923_flange(a.get(0)?).map(p2)),
        "DimensionTables.hexalobularDriveA" => ok(dimensions::hexalobular_drive_a(a.get(0)?)),
        "BoltForging.applies" => ok(bolt_forging::applies(a.get(0)?)),
        "BoltForging.din6921" => ok(bolt_forging::din6921(a.get(0)?)),
        "BoltForging.resolveFlange" => ok(bolt_forging::resolve_flange(a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?)),
        "BoltForging.compute" => {
            let head: Option<dimensions::HeadDimensions> = a.get(7)?;
            ok(bolt_forging::compute(
                a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?, a.get(5)?, a.get(6)?, head.as_ref(), a.get(8)?, a.get(9)?, a.get(10)?, a.get(11)?, a.get(12)?,
                a.get(13)?, a.get(14)?,
            ))
        }
        "PartGeometry.styleOf" => ok(part_geometry::style_of(a.get(0)?)),
        "PartGeometry.build" => {
            let table: dimensions::HeadDimensions = a.get(6)?;
            let socket: Option<crate::pairs::Pair<f64, f64>> = a.get(7)?;
            let g: DrawingGeometry = a.get(8)?;
            let forging: Option<bolt_forging::BoltForgingResult> = a.get(9)?;
            let flange: Option<bolt_forging::FlangeGeom> = a.get(10)?;
            ok(part_geometry::build(
                a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?, a.get(5)?, &table, socket.map(|p| (p.first, p.second)), &g, forging.as_ref(), flange.as_ref(),
            ))
        }
        "HeadingProgression.stageClearance" => ok(crate::heading::stage_clearance(a.get(0)?)),

        // ---------------- heat treatment and anneal
        "HeatTreatCycleEngine.temperHours" => ok(heat_treat_cycle::temper_hours(a.get(0)?)),
        "HeatTreatCycleEngine.soakHours" => ok(heat_treat_cycle::soak_hours(a.get(0)?)),
        "HeatTreatCycleEngine.quenchTemper" => {
            let spec: Option<HeatTreatmentSpec> = a.get(2)?;
            ok(heat_treat_cycle::quench_temper(&a.get::<MaterialInfo>(0)?, a.get(1)?, spec.as_ref()))
        }
        "HeatTreatCycleEngine.softAnneal" => ok(heat_treat_cycle::soft_anneal(&a.get::<MaterialInfo>(0)?, a.get(1)?)),
        "HeatTreatCycleEngine.precipitationHarden" => ok(heat_treat_cycle::precipitation_harden(&a.get::<MaterialInfo>(0)?, a.get(1)?)),
        "SoftAnnealEngine.build" => ok(soft_anneal::build(&a.get::<MaterialInfo>(0)?)),
        "SoftAnnealEngine.hbToHrb" => ok(soft_anneal::hb_to_hrb(a.get(0)?)),
        "SoftAnnealEngine.parseUtsRange" => ok(soft_anneal::parse_uts_range(&a.get::<String>(0)?).map(p2)),
        "SoftAnnealEngine.millAnneal" => ok(soft_anneal::mill_anneal(&a.get::<String>(0)?).map(|m| {
            json!({ "temperatureC": m.temperature_c, "hold": m.hold, "cooling": m.cooling, "source": m.source, "confidence": m.confidence, "note": m.note })
        })),
        "AnnealCycleEngine.spheroidize" => ok(soft_anneal::spheroidize(&a.get::<MaterialInfo>(0)?, a.get(1)?, a.get(2)?)),
        "SteelCompositions.ac1For" => ok(steel::ac1_for(&a.get::<MaterialInfo>(0)?)),
        "SteelCompositions.canSpheroidise" => ok(steel::can_spheroidise(&a.get::<MaterialInfo>(0)?)),
        "SteelCompositions.forMaterial" => ok(steel::for_material(&a.get::<MaterialInfo>(0)?)),
        "SteelCompositions.soakHours" => ok(p2(steel::soak_hours(&a.get::<MaterialInfo>(0)?, a.get(1)?))),
        "HardnessConversion.hrcFor" => ok(hardness_conversion::hrc_for(a.get(0)?)),
        "TemperGuide.startTempC" => ok(temper_guide::start_temp_c(&a.get::<String>(0)?, a.get(1)?)),
        "TensileRange.of" => ok(TensileRange::of(a.get(0)?, a.get(1)?)),
        "TensileRange.parse" => ok(TensileRange::parse(&a.get::<String>(0)?)),
        "MillCert.of" => ok(MillCert::of(a.get(0)?, a.get(1)?)),
        "MillCertCheck.hasRecessHead" => ok(mill_cert_check::has_recess_head(a.get(0)?, a.get::<Option<FinishedPart>>(1)?.as_ref())),
        "MillCertCheck.check" => {
            let part: Option<FinishedPart> = a.get(2)?;
            let cert: Option<MillCert> = a.get(3)?;
            ok(mill_cert_check::check(&a.get::<MaterialInfo>(0)?, a.get(1)?, part.as_ref(), cert.as_ref()))
        }
        "MillCertCheck.issues" => ok(mill_cert_check::issues(a.get::<Option<WireCheck>>(0)?.as_ref())),
        "JointGrip.appliesTo" => ok(joint_grip::applies_to(a.get(0)?)),
        "JointGrip.stepMm" => ok(joint_grip::step_mm(a.get(0)?)),
        "JointGrip.check" => ok(joint_grip::check(a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?)),
        "ThreadRollingBlank.controlledRootDelta" => ok(thread_rolling_blank::controlled_root_delta(a.get(0)?, a.get(1)?)),
        "ThreadRollingBlank.controlledRootDeltaRange" => ok(p2(thread_rolling_blank::controlled_root_delta_range(a.get(0)?))),

        // ---------------- tooling
        "ToolingDesign.split" => {
            let (lo, hi) = tooling::split(&a.get::<Vec<ProfileSegment>>(0)?, a.get(1)?);
            ok(p2((lo, hi)))
        }
        "ToolingDesign.extrusions" => ok(tooling::extrusions(&a.get::<HeadingPlan>(0)?)),
        "ToolingDesign.build" => ok(tooling::build(&a.get::<FastenerCalculationResult>(0)?, &a.get::<MachineSetup>(1)?)),
        "ToolingDesign.text" => ok(tooling::text(&a.get::<FastenerCalculationResult>(0)?, &a.get::<ToolingSheet>(1)?, &a.get::<MachineSetup>(2)?)),
        "ExtrusionDie.items" => ok(p2(extrusion_die::items(&a.get::<StationExtrusion>(0)?, &a.get::<MaterialInfo>(1)?, &a.get::<MachineSetup>(2)?))),
        "ExtrusionDie.pressureMpa" => ok(p2(extrusion_die::pressure_mpa(&a.get::<MaterialInfo>(0)?, a.get(1)?, a.get(2)?))),
        "ExtrusionDie.coneLengthMm" => ok(extrusion_die::cone_length_mm(a.get(0)?, a.get(1)?, a.get(2)?)),

        // ---------------- reports and quality
        "ProductionReport.summary" => ok(production_report::summary(&a.get::<FastenerCalculationResult>(0)?)),
        "ProductionReport.processSequence" => ok(production_report::process_sequence(&a.get::<FastenerCalculationResult>(0)?)),
        "ProductionReport.build" => {
            let spec: Option<ExtractedFastenerSpec> = a.get(1)?;
            ok(production_report::build(&a.get::<FastenerCalculationResult>(0)?, spec.as_ref(), &a.get::<String>(2)?))
        }
        "RawMaterialPlanner.suggestedDiameters" => ok(raw_material::suggested_diameters(a.get(0)?, a.get(1)?)),
        "RawMaterialPlanner.plan" => ok(raw_material::plan(a.get(0)?, a.get(1)?, a.get(2)?, &a.get::<Vec<f64>>(3)?, a.get(4)?, a.get(5)?, a.get(6)?)),
        "RawMaterialPlanner.coilsNeeded" => ok(raw_material::coils_needed(a.get(0)?, a.get(1)?)),
        "RawMaterialPlanner.parseDiameters" => ok(raw_material::parse_diameters(&a.get::<String>(0)?)),
        "QualityDocs.productGradeA" => ok(quality::product_grade_a(&a.get::<FastenerCalculationResult>(0)?)),
        "QualityDocs.hexBoltTolerances" => ok(quality::hex_bolt_tolerances(&a.get::<FastenerCalculationResult>(0)?).into_iter().map(p3).collect::<Vec<_>>()),
        "QualityDocs.controlPlan" => ok(quality::control_plan(&a.get::<FastenerCalculationResult>(0)?)),
        "QualityDocs.apqp" => ok(quality::apqp(&a.get::<FastenerCalculationResult>(0)?)),
        "QualityDocs.specialCharacteristics" => ok(quality::special_characteristics(&a.get::<FastenerCalculationResult>(0)?)),
        "QualityDocs.capabilityCharacteristics" => ok(quality::capability_characteristics(&a.get::<FastenerCalculationResult>(0)?)),
        "QualityDocs.ppapElements" => ok(quality::ppap_elements(&a.get::<FastenerCalculationResult>(0)?)),
        "QualityDocs.psw" => ok(quality::psw(
            &a.get::<FastenerCalculationResult>(0)?,
            &a.get::<String>(1)?,
            &a.get::<String>(2)?,
            &a.get::<String>(3)?,
            a.get(4)?,
            &a.get::<String>(5)?,
            &a.get::<String>(6)?,
        )),
        "QualityDocs.ecn" => ok(quality::ecn(
            &a.get::<FastenerCalculationResult>(0)?,
            &a.get::<String>(1)?,
            &a.get::<String>(2)?,
            &a.get::<String>(3)?,
            &a.get::<String>(4)?,
            &a.get::<String>(5)?,
            &a.get::<Vec<usize>>(6)?,
            &a.get::<String>(7)?,
        )),
        "QualityDocs.coatingThicknessMethods" => ok(quality::coating_thickness_methods(a.get(0)?).into_iter().map(p2).collect::<Vec<_>>()),
        "QualityEngine.parseValues" => ok(quality::parse_values(&a.get::<String>(0)?)),
        "QualityEngine.capability" => ok(quality::capability(&a.get::<Vec<f64>>(0)?, a.get(1)?, a.get(2)?)),
        "QualityEngine.normalCdf" => ok(quality::normal_cdf(a.get(0)?)),
        "QualityEngine.normalInv" => ok(quality::normal_inv(a.get(0)?)),
        "QualityEngine.sigmaLevel" => ok(quality::sigma_level(a.get(0)?)),
        "QualityEngine.dpmo" => ok(quality::dpmo(a.get(0)?, a.get(1)?, a.get(2)?)),
        "QualityEngine.oee" => ok(quality::oee(a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?)),
        "QualityEngine.clearanceHoleMedium" => ok(quality::clearance_hole_medium(a.get(0)?)),
        "QualityEngine.torqueTension" => ok(quality::torque_tension(a.get(0)?, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?, a.get(5)?, a.get(6)?, a.get(7)?)),
        "QualityEngine.itGrade" => ok(quality::it_grade(a.get(0)?, a.get(1)?)),

        // ---------------- animation, scan, selection
        "HeadingAnimation.frameAt" => ok(heading_animation::frame_at(&a.get::<HeadingPlan>(0)?, a.get(1)?)),
        "HeadingAnimation.outline" => ok(heading_animation::outline(&a.get::<HeadingPlan>(0)?, &a.get::<AnimFrame>(1)?, a.get(2)?).into_iter().map(p2).collect::<Vec<_>>()),
        "HeadingAnimation.byVolume" => ok(heading_animation::by_volume(&a.get::<HeadingStation>(0)?, a.get(1)?).into_iter().map(p2).collect::<Vec<_>>()),
        "DrawingScan.calculate" => ok(drawing_scan::calculate(&a.get::<ExtractedFastenerSpec>(0)?, a.get(1)?, a.get(2)?)),
        "DrawingScan.volume" => ok(drawing_scan::volume(&a.get::<ExtractedFastenerSpec>(0)?, &a.get::<FastenerCalculationResult>(1)?)),
        "SelectionFilter.groupOfMaterial" => ok(selection_filter::group_of_material(&a.get::<MaterialInfo>(0)?)),
        "SelectionFilter.groupOfClass" => ok(selection_filter::group_of_class(a.get(0)?)),
        "SelectionFilter.classAllows" => ok(selection_filter::class_allows(a.get(0)?, &a.get::<MaterialInfo>(1)?, a.get(2)?)),
        "SelectionFilter.materials" => {
            // args: keys of the collection, group, class, d, onlyForClass, search; answers the chosen keys
            let keys: Vec<String> = a.get(0)?;
            let reg = MATERIALS.lock().map_err(|e| e.to_string())?;
            let all: Vec<MaterialInfo> = keys.iter().filter_map(|k| reg.iter().find(|m| &m.key == k).cloned()).collect();
            let chosen = selection_filter::materials(&all, a.get(1)?, a.get(2)?, a.get(3)?, a.get(4)?, &a.get::<String>(5)?);
            ok(chosen.iter().map(|m| m.key.clone()).collect::<Vec<_>>())
        }
        "SelectionFilter.materialGroups" => {
            let keys: Vec<String> = a.get(0)?;
            let reg = MATERIALS.lock().map_err(|e| e.to_string())?;
            let all: Vec<MaterialInfo> = keys.iter().filter_map(|k| reg.iter().find(|m| &m.key == k).cloned()).collect();
            ok(selection_filter::material_groups(&all))
        }
        "SelectionFilter.propertyClasses" => ok(selection_filter::property_classes(a.get(0)?, a.get(1)?, a.get(2)?, &a.get::<String>(3)?)),
        "SelectionFilter.classGroups" => ok(selection_filter::class_groups(a.get(0)?)),
        "SelectionFilter.threadClasses" => ok(selection_filter::thread_classes(a.get(0)?, a.get(1)?, a.get(2)?)),
        "SelectionFilter.hasThread" => ok(selection_filter::has_thread(a.get(0)?)),
        "SelectionFilter.usesPropertyClass" => ok(selection_filter::uses_property_class(a.get(0)?)),

        _ => Err("unknown engine function".into()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn unknown_function_and_bad_arguments_are_errors_not_panics() {
        assert!(dispatch("Nope.nothing", "[]").contains("\"err\""));
        assert!(dispatch("HardnessConversion.hrcFor", "not json").contains("\"err\""));
        assert!(dispatch("HardnessConversion.hrcFor", "[\"x\"]").contains("\"err\""));
        assert_eq!(dispatch("HardnessConversion.hrcFor", "[1040.0]"), "{\"ok\":32.0}");
    }
}
