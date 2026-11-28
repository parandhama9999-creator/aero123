//! AeroFast Pro fastener engineering engine: a Rust port of the Kotlin engine in the Android app.
//! Every module mirrors one Kotlin file in app/src/main/java/com/aistudio/aerofast/trmst/data and is
//! checked against the Kotlin engine's own results (see tests/parity.rs and golden/).
#![forbid(unsafe_code)]
#![allow(non_camel_case_types, clippy::too_many_arguments)]

pub mod jfmt;
pub mod gen_enums;
mod gen_materials;
pub mod catalog;
pub mod thread;
pub mod tensile;
pub mod steel;
pub mod poka_yoke;
pub mod dimensions;
pub mod bolt_forging;
pub mod heading;
pub mod part_geometry;
pub mod soft_anneal;
pub mod heat_treat_cycle;
pub mod tooling;
pub mod calculation;
pub mod raw_material;
pub mod production_report;
pub mod quality;
pub mod heading_animation;
pub mod selection_filter;
pub mod drawing_scan;

pub use gen_enums::*;
