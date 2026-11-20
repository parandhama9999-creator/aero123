package com.aistudio.aerofast.trmst.data

enum class FastenerType(
    val label: String,
    val isNut: Boolean = false,
    val isStud: Boolean = false,
    val isWasher: Boolean = false,
    val category: String = "Bolts & Screws",
    /** Rivets: no thread, so thread limits, rolling, tapping, CNC turning and thread-root fatigue do not apply. */
    val isUnthreaded: Boolean = false
) {
    // External Threaded Bolts & Screws
    HEX_BOLT("Hex Bolt - Partial Thread (ISO 4014 / DIN 931)", false, false, false, "Bolts & Screws"),
    HEX_BOLT_FULL("Hex Tap Bolt - Full Thread (ISO 4017 / DIN 933)", false, false, false, "Bolts & Screws"),
    SHCS("Socket Head Cap Screw - SHCS (ISO 4762 / DIN 912)", false, false, false, "Bolts & Screws"),
    SOCKET_CSK("Countersunk Socket Screw - CSK (ISO 10642 / DIN 7991)", false, false, false, "Bolts & Screws"),
    SOCKET_BUTTON("Button Head Socket Screw (ISO 7380)", false, false, false, "Bolts & Screws"),
    SOCKET_SET("Socket Set Screw / Grub Screw (ISO 4026 / DIN 913)", false, false, false, "Bolts & Screws"),
    FLANGE_BOLT("Hex Flange Bolt - Heavy Duty (EN 1665 / DIN 6921)", false, false, false, "Bolts & Screws"),
    TWELVE_POINT("12-Point Aero Jet Bolt (MS21250 / NAS 624)", false, false, false, "Bolts & Screws"),
    CARRIAGE_BOLT("Carriage Bolt / Square Neck (DIN 603 / ISO 8677)", false, false, false, "Bolts & Screws"),
    T_BOLT("T-Head / Slot Bolt (DIN 186 / DIN 261)", false, false, false, "Bolts & Screws"),
    HEAVY_HEX_BOLT("Heavy Hex Structural Bolt (ASTM A325 / A490)", false, false, false, "Bolts & Screws"),
    EYE_BOLT("Precision Eye Bolt / Lifting Ring (DIN 444 / ISO 9001)", false, false, false, "Bolts & Screws"),
    SQUARE_BOLT("Square Head Bolt (ASME B18.2.1 / DIN 478)", false, false, false, "Bolts & Screws"),
    SHOULDER_BOLT("Shoulder Screw / Stripper Bolt (ISO 7379)", false, false, false, "Bolts & Screws"),
    SET_SCREW_CONE("Cone Point Set Screw (ISO 4027 / DIN 914)", false, false, false, "Bolts & Screws"),
    SET_SCREW_DOG("Dog Point Set Screw (ISO 4028 / DIN 915)", false, false, false, "Bolts & Screws"),
    SET_SCREW_CUP("Cup Point Set Screw (ISO 4029 / DIN 916)", false, false, false, "Bolts & Screws"),
    SLOTTED_SET_SCREW("Slotted Set Screw (DIN 438 / ISO 7436)", false, false, false, "Bolts & Screws"),
    THUMB_SCREW("Knurled Thumb Screw (DIN 464 / DIN 653)", false, false, false, "Bolts & Screws"),
    WING_SCREW("Wing Screw (DIN 316)", false, false, false, "Bolts & Screws"),
    HANGER_BOLT("Hanger / Dowel Screw - Wood to Metal (DIN 7961)", false, false, false, "Bolts & Screws"),
    ANCHOR_BOLT("Anchor / Foundation Bolt, L & J Type (ASTM F1554)", false, false, false, "Bolts & Screws"),
    LAG_SCREW("Hex Lag / Coach Screw (DIN 571 / ASME B18.2.1)", false, false, false, "Bolts & Screws"),

    // Machine Screws (small-diameter headed screws, ISO 1207 family)
    PAN_SLOT("Slotted Pan Head Machine Screw (ISO 1580 / DIN 85)", false, false, false, "Machine Screws"),
    PAN_CROSS("Cross-Recess Pan Head Screw (ISO 7045 / DIN 7985)", false, false, false, "Machine Screws"),
    PAN_HEXALOBULAR("Hexalobular (Torx) Pan Head Screw (ISO 14583 / ASME B18.6.3)", false, false, false, "Machine Screws"),
    CHEESE_SLOT("Slotted Cheese Head Screw (ISO 1207 / DIN 84)", false, false, false, "Machine Screws"),
    CSK_SLOT("Slotted Countersunk Head Screw (ISO 2009 / DIN 963)", false, false, false, "Machine Screws"),
    CSK_CROSS("Cross-Recess Countersunk Head Screw (ISO 7046 / DIN 965)", false, false, false, "Machine Screws"),
    RAISED_CSK_CROSS("Cross-Recess Raised Countersunk (Oval) Screw (ISO 7047 / DIN 966)", false, false, false, "Machine Screws"),
    HEX_WASHER_SCREW("Hex Washer Head Screw (DIN 6900-3 / ASME B18.6.3)", false, false, false, "Machine Screws"),
    SEMS_SCREW("SEMS Screw & Captive Washer Assembly (DIN 6900)", false, false, false, "Machine Screws"),

    // Tapping, Self-Drilling & Wood Screws
    TAPPING_PAN("Self-Tapping Pan Head Screw (ISO 1481 / DIN 7981)", false, false, false, "Tapping & Wood Screws"),
    TAPPING_CSK("Self-Tapping Countersunk Screw (ISO 7050 / DIN 7982)", false, false, false, "Tapping & Wood Screws"),
    TAPPING_HEX("Hex Head Tapping Screw (ISO 10509 / DIN 7976)", false, false, false, "Tapping & Wood Screws"),
    SELF_DRILLING("Self-Drilling Screw (ISO 15480 / DIN 7504K)", false, false, false, "Tapping & Wood Screws"),
    DRYWALL_SCREW("Drywall Bugle Head Screw (DIN 18182 / ASTM C1002)", false, false, false, "Tapping & Wood Screws"),
    CHIPBOARD_SCREW("Chipboard / Wood Screw (DIN 7505 / EN 14592)", false, false, false, "Tapping & Wood Screws"),

    // Automotive
    TTY_HEAD_BOLT("Torque-to-Yield Head Bolt - Reduced Shank, Flange (OEM)", false, false, false, "Automotive"),
    HEXALOBULAR_SCREW("Hexalobular (Torx) Socket Head Cap Screw (ISO 14579)", false, false, false, "Automotive"),
    THREAD_FORMING_SCREW("Thread-Forming / Taptite Screw (DIN 7500)", false, false, false, "Automotive"),
    WHEEL_BOLT("Wheel Bolt - Conical / Spherical Seat (DIN 74361)", false, false, false, "Automotive"),
    BANJO_BOLT("Banjo / Hollow Bolt (DIN 7623)", false, false, false, "Automotive"),
    CONROD_BOLT("Connecting Rod Bolt - Necked Down Shank (OEM)", false, false, false, "Automotive"),

    // Aerospace
    NAS_CLOSE_TOL_BOLT("Aircraft Hex Head Bolt - Close Tolerance (NAS 6203 / AN3)", false, false, false, "Aerospace"),
    HI_LOK_PIN("Hi-Lok Pin - Torque-Off Collar System (Hi-Shear)", false, false, false, "Aerospace"),
    TAPER_LOK("Taper-Lok Interference-Fit Bolt (OEM spec)", false, false, false, "Aerospace"),
    LOCKBOLT_PIN("Lockbolt Pin - Swaged Collar (Huck type)", false, false, false, "Aerospace"),
    BLIND_BOLT("Blind Bolt - Core Bolt (OEM / NAS series)", false, false, false, "Aerospace"),
    SOLID_RIVET("Solid Rivet - Universal Head (MS20470)", false, false, false, "Aerospace", isUnthreaded = true),
    BLIND_RIVET("Structural Blind Rivet (NAS 1398)", false, false, false, "Aerospace", isUnthreaded = true),
    CLEVIS_BOLT("Aircraft Clevis Bolt (AN21 – AN36)", false, false, false, "Aerospace"),
    SHEAR_BOLT("Aircraft Shear Bolt - Reduced Head (AN173 / NAS 464)", false, false, false, "Aerospace"),

    // Studs & Rods
    STUD("Double-End / Continuous Thread Stud (ASTM A193 B7)", false, true, false, "Studs & Rods"),
    WHEEL_STUD("Knurled Wheel / Hub Stud", false, true, false, "Studs & Rods"),
    U_BOLT("U-Bolt / Pipe Clamp Bolt (DIN 3570)", false, true, false, "Studs & Rods"),
    THREADED_ROD("Threaded Rod / Stud Bolt, Full Length (DIN 975 / ASTM A193)", false, true, false, "Studs & Rods"),
    TAP_END_STUD("Tap-End Flange Stud (ASME B16.5 / ASTM A193)", false, true, false, "Studs & Rods"),

    // Pins & Retainers (unthreaded: no rolling, tapping or thread-root fatigue)
    DOWEL_PIN("Dowel Pin - Ground (ISO 2338 / DIN 7)", false, false, false, "Pins & Retainers", isUnthreaded = true),
    TAPER_PIN("Taper Pin 1:50 (ISO 2339 / DIN 1)", false, false, false, "Pins & Retainers", isUnthreaded = true),
    CLEVIS_PIN("Clevis Pin with Head (ISO 2341 / DIN 1444)", false, false, false, "Pins & Retainers", isUnthreaded = true),
    SPLIT_PIN("Split Cotter Pin (ISO 1234 / DIN 94)", false, false, false, "Pins & Retainers", isUnthreaded = true),
    SPRING_PIN("Slotted Spring Pin (ISO 8752 / DIN 1481)", false, false, false, "Pins & Retainers", isUnthreaded = true),
    GROOVED_PIN("Grooved Drive Pin (ISO 8739 / DIN 1473)", false, false, false, "Pins & Retainers", isUnthreaded = true),
    // A circlip is a flat ring like a washer: it is parted and formed from strip, not upset on a header
    RETAINING_RING("Retaining Ring / Circlip (DIN 471 / DIN 472)", false, false, true, "Pins & Retainers", isUnthreaded = true),

    // Nuts & Internal Taps
    HEX_NUT("Standard Hex Nut (ISO 4032 / DIN 934)", true, false, false, "Nuts & Locking"),
    HEAVY_HEX_NUT("Heavy Hex Nut (ASME B18.2.2 / ASTM A194 2H)", true, false, false, "Nuts & Locking"),
    FLANGE_NUT("Hex Flange Nut / Serrated Lock Nut (DIN 6923)", true, false, false, "Nuts & Locking"),
    NYLOC_NUT("Nyloc Insert Lock Nut (ISO 7040 / DIN 985)", true, false, false, "Nuts & Locking"),
    METAL_LOCK_NUT("Prevailing Torque All-Metal Lock Nut (ISO 7042)", true, false, false, "Nuts & Locking"),
    CASTLE_NUT("Slotted / Castle Aero Nut (DIN 935 / AN310)", true, false, false, "Nuts & Locking"),
    ACORN_NUT("Dome Cap Acorn Nut (DIN 1587)", true, false, false, "Nuts & Locking"),
    WELD_NUT("Square / Hex Weld Nut (DIN 928 / DIN 929)", true, false, false, "Nuts & Locking"),
    WHEEL_NUT("Wheel Nut - Conical / Spherical Seat (DIN 74361)", true, false, false, "Automotive"),
    AERO_LOCK_NUT("Self-Locking Reduced Hex Aero Nut (MS21042 / NAS 1291)", true, false, false, "Aerospace"),
    HI_LOK_COLLAR("Hi-Lok Torque-Off Collar (Hi-Shear)", true, false, false, "Aerospace"),
    NUTPLATE("Nutplate / Anchor Nut - Two Lug (MS21047)", true, false, false, "Aerospace"),
    THIN_NUT("Thin Jam Hex Nut (ISO 4035 / DIN 439B)", true, false, false, "Nuts & Locking"),
    HIGH_NUT("High Hex Nut, Style 2 (ISO 4033 / DIN 934 high)", true, false, false, "Nuts & Locking"),
    SQUARE_NUT("Square Nut (DIN 557 / DIN 562)", true, false, false, "Nuts & Locking"),
    COUPLING_NUT("Coupling / Extension Nut (DIN 6334)", true, false, false, "Nuts & Locking"),
    T_SLOT_NUT("T-Slot Nut (DIN 508)", true, false, false, "Nuts & Locking"),
    CAGE_NUT("Cage / Spring Channel Nut (sheet metal, OEM)", true, false, false, "Nuts & Locking"),
    WING_NUT("Wing Nut (DIN 315 / ASME B18.17)", true, false, false, "Nuts & Locking"),
    THUMB_NUT("Knurled Thumb Nut (DIN 466 / DIN 467)", true, false, false, "Nuts & Locking"),
    RIVET_NUT("Blind Rivet Nut / Rivnut (OEM spec)", true, false, false, "Nuts & Locking"),
    CLINCH_NUT("Self-Clinching Nut (IFI-100 / OEM)", true, false, false, "Nuts & Locking"),

    // Washers
    FLAT_WASHER("Flat Plain Washer (ISO 7089 / DIN 125A)", false, false, true, "Washers"),
    SPRING_WASHER("Spring Lock Washer (DIN 127B / ISO 10270)", false, false, true, "Washers"),
    FENDER_WASHER("Large OD Fender Washer (ISO 7093 / DIN 9021)", false, false, true, "Washers"),
    STRUCT_WASHER("Hardened Structural Washer (ASTM F436)", false, false, true, "Washers"),
    TOOTH_LOCK_WASHER("Serrated Tooth Lock Washer (DIN 6797 / DIN 6798)", false, false, true, "Washers"),
    BELLEVILLE_WASHER("Conical Belleville Spring Washer (DIN 6796 / DIN 2093)", false, false, true, "Washers"),
    WAVE_WASHER("Curved / Wave Spring Washer (DIN 137)", false, false, true, "Washers"),
    TAPER_WASHER("Square Taper Washer for Sections (DIN 434 / DIN 435)", false, false, true, "Washers")
}

enum class UnitSystem(val label: String) {
    METRIC("Metric (M0.25 – M64, ISO 261)"),
    INCH("Inch (#0-80 – 1-3/4\" UNC/UNF, ASME B1.1)")
}

enum class ProductStandard(val code: String, val label: String, val category: String = "ISO/DIN Metric") {
    // Metric ISO & DIN Standards
    ISO_4014("ISO 4014", "ISO 4014 / DIN 931 (Hex Bolt - Partial Thread)", "ISO/DIN Metric"),
    ISO_4017("ISO 4017", "ISO 4017 / DIN 933 (Hex Bolt - Full Thread)", "ISO/DIN Metric"),
    ISO_4762("ISO 4762", "ISO 4762 / DIN 912 (Socket Head Cap Screw)", "ISO/DIN Metric"),
    ISO_10642("ISO 10642", "ISO 10642 / DIN 7991 (Countersunk Socket Screw)", "ISO/DIN Metric"),
    ISO_7380("ISO 7380", "ISO 7380-1 / ISO 7380-2 (Button Head Socket Screw)", "ISO/DIN Metric"),
    ISO_4026("ISO 4026", "ISO 4026 / DIN 913 (Socket Set Screw, Flat Point)", "ISO/DIN Metric"),
    ISO_4027("ISO 4027", "ISO 4027 / DIN 914 (Socket Set Screw, Cone Point)", "ISO/DIN Metric"),
    ISO_4028("ISO 4028", "ISO 4028 / DIN 915 (Socket Set Screw, Dog Point)", "ISO/DIN Metric"),
    ISO_4029("ISO 4029", "ISO 4029 / DIN 916 (Socket Set Screw, Cup Point)", "ISO/DIN Metric"),
    DIN_438("DIN 438", "DIN 438 / ISO 7436 (Slotted Set Screw)", "ISO/DIN Metric"),
    ISO_7379("ISO 7379", "ISO 7379 (Shoulder Screw / Stripper Bolt)", "ISO/DIN Metric"),
    DIN_478("DIN 478", "DIN 478 / DIN 479 (Square Head Bolt)", "ISO/DIN Metric"),
    DIN_464("DIN 464", "DIN 464 / DIN 653 (Knurled Thumb Screw)", "ISO/DIN Metric"),
    DIN_316("DIN 316", "DIN 316 (Wing Screw)", "ISO/DIN Metric"),
    DIN_7961("DIN 7961", "DIN 7961 (Hanger / Dowel Screw)", "ISO/DIN Metric"),
    DIN_571("DIN 571", "DIN 571 (Hex Lag / Coach Screw for Wood)", "ISO/DIN Metric"),
    ISO_1580("ISO 1580", "ISO 1580 / DIN 85 (Slotted Pan Head Machine Screw)", "ISO/DIN Metric"),
    ISO_7045("ISO 7045", "ISO 7045 / DIN 7985 (Cross-Recess Pan Head Screw)", "ISO/DIN Metric"),
    ISO_14583("ISO 14583", "ISO 14583 (Hexalobular Socket Pan Head Screw)", "ISO/DIN Metric"),
    ISO_1207("ISO 1207", "ISO 1207 / DIN 84 (Slotted Cheese Head Screw)", "ISO/DIN Metric"),
    ISO_2009("ISO 2009", "ISO 2009 / DIN 963 (Slotted Countersunk Head Screw)", "ISO/DIN Metric"),
    ISO_7046("ISO 7046", "ISO 7046 / DIN 965 (Cross-Recess Countersunk Screw)", "ISO/DIN Metric"),
    ISO_7047("ISO 7047", "ISO 7047 / DIN 966 (Cross-Recess Raised Countersunk Screw)", "ISO/DIN Metric"),
    DIN_6900("DIN 6900", "DIN 6900 / DIN 6900-3 (SEMS & Hex Washer Head Screw)", "ISO/DIN Metric"),
    ISO_1481("ISO 1481", "ISO 1481 / DIN 7981 (Self-Tapping Pan Head Screw)", "ISO/DIN Metric"),
    ISO_7050("ISO 7050", "ISO 7050 / DIN 7982 (Self-Tapping Countersunk Screw)", "ISO/DIN Metric"),
    ISO_10509("ISO 10509", "ISO 10509 / DIN 7976 (Hex Head Tapping Screw)", "ISO/DIN Metric"),
    ISO_15480("ISO 15480", "ISO 15480 / DIN 7504K (Self-Drilling Screw)", "ISO/DIN Metric"),
    DIN_18182("DIN 18182", "DIN 18182-2 / ASTM C1002 (Drywall Screw)", "ISO/DIN Metric"),
    DIN_7505("DIN 7505", "DIN 7505 / EN 14592 (Chipboard / Wood Screw)", "ISO/DIN Metric"),
    DIN_975("DIN 975", "DIN 975 / DIN 976 (Threaded Rod / Stud Bolt)", "ISO/DIN Metric"),
    ISO_2338("ISO 2338", "ISO 2338 / DIN 7 (Ground Dowel Pin)", "ISO/DIN Metric"),
    ISO_2339("ISO 2339", "ISO 2339 / DIN 1 (Taper Pin 1:50)", "ISO/DIN Metric"),
    ISO_2341("ISO 2341", "ISO 2341 / DIN 1444 (Clevis Pin with Head)", "ISO/DIN Metric"),
    ISO_1234("ISO 1234", "ISO 1234 / DIN 94 (Split Cotter Pin)", "ISO/DIN Metric"),
    ISO_8752("ISO 8752", "ISO 8752 / DIN 1481 (Slotted Spring Pin)", "ISO/DIN Metric"),
    ISO_8739("ISO 8739", "ISO 8739 / DIN 1473 (Grooved Drive Pin)", "ISO/DIN Metric"),
    DIN_471("DIN 471", "DIN 471 / DIN 472 (Retaining Ring / Circlip)", "ISO/DIN Metric"),
    ISO_4035("ISO 4035", "ISO 4035 / DIN 439B (Thin Jam Hex Nut)", "ISO/DIN Metric"),
    DIN_557("DIN 557", "DIN 557 / DIN 562 (Square Nut)", "ISO/DIN Metric"),
    DIN_6334("DIN 6334", "DIN 6334 (Coupling / Extension Nut)", "ISO/DIN Metric"),
    DIN_508("DIN 508", "DIN 508 (T-Slot Nut)", "ISO/DIN Metric"),
    DIN_315("DIN 315", "DIN 315 / ASME B18.17 (Wing Nut)", "ISO/DIN Metric"),
    DIN_466("DIN 466", "DIN 466 / DIN 467 (Knurled Thumb Nut)", "ISO/DIN Metric"),
    OEM_RIVET_NUT("OEM Rivet Nut", "Blind Rivet Nut (manufacturer spec)", "ISO/DIN Metric"),
    IFI_100("IFI-100", "IFI-100 / IFI-107 (Self-Clinching Nut)", "ISO/DIN Metric"),
    OEM_CAGE_NUT("OEM Cage Nut", "Cage / Spring Channel Nut (manufacturer spec)", "ISO/DIN Metric"),
    DIN_6797("DIN 6797", "DIN 6797 / DIN 6798 (Serrated Tooth Lock Washer)", "ISO/DIN Metric"),
    DIN_6796("DIN 6796", "DIN 6796 / DIN 2093 (Conical Belleville Spring Washer)", "ISO/DIN Metric"),
    DIN_137("DIN 137", "DIN 137 (Curved / Wave Spring Washer)", "ISO/DIN Metric"),
    DIN_434("DIN 434", "DIN 434 / DIN 435 (Square Taper Washer)", "ISO/DIN Metric"),
    OEM_CONROD("OEM Con-Rod", "OEM Connecting Rod Bolt Spec (necked shank, angle tightened)", "ISO/DIN Metric"),
    ISO_1665("EN 1665", "EN 1665 / DIN 6921 (Hex Flange Bolt)", "ISO/DIN Metric"),
    DIN_603("DIN 603", "DIN 603 / ISO 8677 (Carriage Bolt Square Neck)", "ISO/DIN Metric"),
    DIN_444("DIN 444", "DIN 444 / ISO 9001 (Precision Eye Bolt)", "ISO/DIN Metric"),
    DIN_186("DIN 186", "DIN 186 / DIN 261 (T-Head Bolt)", "ISO/DIN Metric"),
    ISO_4032("ISO 4032", "ISO 4032 / DIN 934 (Standard Hex Nut)", "ISO/DIN Metric"),
    ISO_4033("ISO 4033", "ISO 4033 / DIN 934 high (Hex Nut, Style 2)", "ISO/DIN Metric"),
    DIN_6923("DIN 6923", "DIN 6923 / ISO 4161 (Hex Flange Nut)", "ISO/DIN Metric"),
    ISO_7040("ISO 7040", "ISO 7040 / DIN 985 (Nyloc Insert Lock Nut)", "ISO/DIN Metric"),
    ISO_7042("ISO 7042", "ISO 7042 / DIN 980 (All-Metal Lock Nut)", "ISO/DIN Metric"),
    DIN_935("DIN 935", "DIN 935 / ISO 7035 (Slotted Castle Nut)", "ISO/DIN Metric"),
    DIN_1587("DIN 1587", "DIN 1587 / ISO 8673 (Dome Cap Acorn Nut)", "ISO/DIN Metric"),
    DIN_929("DIN 929", "DIN 929 / DIN 928 (Hex / Square Weld Nut)", "ISO/DIN Metric"),
    ISO_7089("ISO 7089", "ISO 7089 / DIN 125A (Flat Plain Washer)", "ISO/DIN Metric"),
    ISO_7093("ISO 7093", "ISO 7093 / DIN 9021 (Large OD Fender Washer)", "ISO/DIN Metric"),
    DIN_127("DIN 127", "DIN 127B / ISO 10270 (Spring Lock Washer)", "ISO/DIN Metric"),
    ISO_14579("ISO 14579", "ISO 14579 (Hexalobular Socket Head Cap Screw)", "ISO/DIN Metric"),
    DIN_7500("DIN 7500", "DIN 7500 (Thread-Forming / Thread-Rolling Screw)", "ISO/DIN Metric"),
    DIN_74361("DIN 74361", "DIN 74361 (Wheel Bolt, Conical / Spherical Seat)", "ISO/DIN Metric"),
    DIN_74361_NUT("DIN 74361 Nut", "DIN 74361 (Wheel Nut, Conical / Spherical Seat)", "ISO/DIN Metric"),
    DIN_7623("DIN 7623", "DIN 7623 (Banjo / Hollow Bolt)", "ISO/DIN Metric"),
    OEM_TTY("OEM TTY", "OEM Torque-to-Yield Spec (ISO 898-1 base, torque + angle)", "ISO/DIN Metric"),

    // Imperial / ASME / ANSI / ASTM Standards
    ASME_B18_2_1("ASME B18.2.1", "ASME B18.2.1 / ANSI B18.2.1 (Square & Hex Bolts)", "ASME/Imperial"),
    ASME_B18_2_2("ASME B18.2.2", "ASME B18.2.2 / ANSI B18.2.2 (Square & Hex Nuts)", "ASME/Imperial"),
    ASME_B18_3("ASME B18.3", "ASME B18.3 (Socket Head, Flat & Button Screws)", "ASME/Imperial"),
    ASTM_A325("ASTM A325", "ASTM A325 / F3125 (Structural Heavy Hex Bolt)", "ASME/Imperial"),
    ASTM_A490("ASTM A490", "ASTM A490 / F3125 (High Strength Structural Bolt)", "ASME/Imperial"),
    ASTM_A193("ASTM A193", "ASTM A193 B7/B16 (High Temp Pressure Vessel Studs)", "ASME/Imperial"),
    ASTM_A194("ASTM A194", "ASTM A194 2H/7 (High Temp Heavy Hex Nuts)", "ASME/Imperial"),
    ASTM_F436("ASTM F436", "ASTM F436 (Hardened Structural Washers)", "ASME/Imperial"),
    ASME_B18_6_3("ASME B18.6.3", "ASME B18.6.3 (Machine Screws, Inch)", "ASME/Imperial"),
    ASME_B18_6_4("ASME B18.6.4", "ASME B18.6.4 (Tapping & Self-Drilling Screws, Inch)", "ASME/Imperial"),
    ASME_B18_8_2("ASME B18.8.2", "ASME B18.8.2 (Dowel, Taper & Clevis Pins, Inch)", "ASME/Imperial"),
    ASME_B18_22_1("ASME B18.22.1", "ASME B18.22.1 / B18.21.1 (Plain & Lock Washers, Inch)", "ASME/Imperial"),
    ASME_B16_5("ASME B16.5", "ASME B16.5 / ASTM A193 (Tap-End Flange Stud)", "ASME/Imperial"),
    ASTM_F1554("ASTM F1554", "ASTM F1554 (Anchor / Foundation Bolt, Gr 36/55/105)", "ASME/Imperial"),
    ASTM_A563("ASTM A563", "ASTM A563 Gr A/C/DH (Structural Nuts)", "ASME/Imperial"),
    ASTM_A307("ASTM A307", "ASTM A307 Gr A/B (Low Carbon Bolts & Studs)", "ASME/Imperial"),
    ASTM_A320("ASTM A320", "ASTM A320 L7/L43 (Low Temperature Service Studs)", "ASME/Imperial"),

    // Aerospace & Defense Standards (NAS / MS / AS / EN)
    NAS624("NAS 624", "MS21250 / NAS 624 (12-Point Aero Flange Bolt)", "Aerospace/Defense"),
    NAS1351("NAS 1351", "NAS 1351 / NAS 1352 (High Fatigue Aero SHCS)", "Aerospace/Defense"),
    NAS1801("NAS 1801", "NAS 1801 / NAS 1802 (Torx Plus / Tri-Slot Aero Bolt)", "Aerospace/Defense"),
    MS21042("MS21042", "MS21042 / NAS 1291 (All-Metal Reduced Hex Lock Nut)", "Aerospace/Defense"),
    MS20500("MS20500", "MS20500 / AN310 (Aero Slotted Castle Nut)", "Aerospace/Defense"),
    AS1150("AS 1150", "AS 1150 / AS 1151 (Aerospace Titanium Engine Stud)", "Aerospace/Defense"),
    EN2882("EN 2882", "EN 2882 / EN 2883 (Aerospace FE-PM1501 High Temp Bolt)", "Aerospace/Defense"),
    NAS6203("NAS 6203", "NAS 6203–6220 (Close-Tolerance) / AN3–AN20 (Aircraft Hex Bolt)", "Aerospace/Defense"),
    HI_LOK("Hi-Lok Pin", "Hi-Lok HL Pin (Hi-Shear spec)", "Aerospace/Defense"),
    HI_LOK_COLLAR_STD("Hi-Lok Collar", "Hi-Lok HL Torque-Off Collar (Hi-Shear spec)", "Aerospace/Defense"),
    TAPER_LOK_STD("Taper-Lok", "Taper-Lok Interference-Fit Bolt (OEM spec)", "Aerospace/Defense"),
    LOCKBOLT_STD("Lockbolt", "Lockbolt Pin & Swaged Collar (Huck / OEM spec)", "Aerospace/Defense"),
    BLIND_BOLT_STD("Blind Bolt", "Blind Bolt (OEM / NAS series)", "Aerospace/Defense"),
    MS21047("MS21047", "MS21047 (Two-Lug Nutplate / Anchor Nut)", "Aerospace/Defense"),
    MS20470("MS20470", "MS20470 (Solid Rivet, Universal Head)", "Aerospace/Defense"),
    NAS1398("NAS 1398", "NAS 1398 (Structural Blind Rivet)", "Aerospace/Defense"),
    AN21("AN 21", "AN21 – AN36 (Aircraft Clevis Bolt)", "Aerospace/Defense"),
    AN173("AN 173", "AN173 – AN186 / NAS 464 (Aircraft Shear Bolt)", "Aerospace/Defense")
}

/** utsMpa is the minimum tensile strength of the class; for the aerospace ranges it is the bottom of the range. */
enum class PropertyClass(val label: String, val defaultMatKey: String, val utsMpa: Double) {
    CLASS_4_6("Class 4.6 (400 MPa • Low Carbon)", "1008", 400.0),
    CLASS_4_8("Class 4.8 (420 MPa • Cold Formed)", "1008", 420.0),
    CLASS_5_6("Class 5.6 (500 MPa • Low / Medium Carbon)", "1018", 500.0),
    CLASS_5_8("Class 5.8 (520 MPa • Stress Relieved)", "1018", 520.0),
    CLASS_6_8("Class 6.8 (600 MPa • Cold Formed)", "20MNB4", 600.0),
    CLASS_8_8("Class 8.8 (830 MPa • Q&T Medium C)", "10B21", 830.0),
    CLASS_9_8("Class 9.8 (900 MPa • Q&T, d ≤ 16)", "10B21", 900.0),
    CLASS_10_9("Class 10.9 (1040 MPa • Q&T Alloy)", "10B21", 1040.0),
    CLASS_12_9("Class 12.9 (1220 MPa • Q&T CrMo)", "4140", 1220.0),
    // ISO 898-2 nut classes: the figure is the proof stress of the class, not a tensile strength
    NUT_04("ISO 898-2 Nut Class 04 (380 MPa proof • thin nut)", "1008", 380.0),
    NUT_05("ISO 898-2 Nut Class 05 (500 MPa proof • thin nut)", "1018", 500.0),
    NUT_5("ISO 898-2 Nut Class 5 (500 MPa proof • mates 3.6–5.8)", "1018", 500.0),
    NUT_6("ISO 898-2 Nut Class 6 (600 MPa proof • mates 6.8)", "10B21", 600.0),
    NUT_8("ISO 898-2 Nut Class 8 (800 MPa proof • mates 8.8)", "10B21", 800.0),
    NUT_9("ISO 898-2 Nut Class 9 (900 MPa proof • mates 9.8)", "10B21", 900.0),
    NUT_10("ISO 898-2 Nut Class 10 (1040 MPa proof • mates 10.9)", "10B21", 1040.0),
    NUT_12("ISO 898-2 Nut Class 12 (1150 MPa proof • mates 12.9)", "4140", 1150.0),
    // ISO 3506-1 / -2 stainless classes
    A2_50("Stainless A2-50 (500 MPa • 304 austenitic, soft)", "SS304", 500.0),
    A2_70("Stainless A2-70 (700 MPa • 304 austenitic, cold worked)", "SS304", 700.0),
    A2_80("Stainless A2-80 (800 MPa • 304 austenitic, high strength)", "SS316", 800.0),
    A4_50("Stainless A4-50 (500 MPa • 316 austenitic, soft)", "SS316", 500.0),
    A4_70("Stainless A4-70 (700 MPa • 316 austenitic, cold worked)", "SS316", 700.0),
    A4_80("Stainless A4-80 (800 MPa • 316 austenitic, high strength)", "SS316", 800.0),
    C1_50("Stainless C1-50 (500 MPa • 410 martensitic, annealed)", "SS410", 500.0),
    C1_70("Stainless C1-70 (700 MPa • 410 martensitic, Q&T)", "SS410", 700.0),
    C4_70("Stainless C4-70 (700 MPa • 430F/416 martensitic, Q&T)", "SS410", 700.0),
    C3_80("Stainless C3-80 (800 MPa • 1.4057 martensitic, Q&T)", "14KH17N2", 800.0),
    F1_60("Stainless F1-60 (600 MPa • 430 ferritic, cold worked)", "SS430", 600.0),
    SAE_GRADE_2("SAE J429 Grade 2 (74 ksi / 510 MPa)", "1018", 510.0),
    SAE_GRADE_5("SAE J429 Grade 5 (120 ksi / 827 MPa)", "10B21", 827.0),
    SAE_GRADE_8("SAE J429 Grade 8 (150 ksi / 1034 MPa)", "4140", 1034.0),
    SAE_NUT_5("SAE J995 Grade 5 Nut (120 ksi proof)", "10B21", 827.0),
    SAE_NUT_8("SAE J995 Grade 8 Nut (150 ksi proof)", "4140", 1034.0),
    ASTM_A574("ASTM A574 Socket Head (180 / 170 ksi)", "4140", 1172.0),
    ASTM_A193_B7("ASTM A193 B7 (125 ksi / 862 MPa)", "4140", 862.0),
    ASTM_A193_B16("ASTM A193 B16 (125 ksi / 862 MPa • 1Cr-0.5Mo-V, high temp)", "4140", 862.0),
    ASTM_A193_B8_CL1("ASTM A193 B8 Class 1 (75 ksi / 515 MPa • 304 solution annealed)", "SS304", 515.0),
    ASTM_A193_B8M_CL2("ASTM A193 B8M Class 2 (95 ksi / 655 MPa • 316 strain hardened)", "SS316", 655.0),
    ASTM_A320_L7("ASTM A320 L7 (125 ksi / 862 MPa • low temperature)", "4140", 862.0),
    ASTM_A325("ASTM A325 / F3125 Gr A325 (120 ksi / 830 MPa)", "10B21", 830.0),
    ASTM_A490("ASTM A490 / F3125 Gr A490 (150 ksi / 1035 MPa)", "4140", 1035.0),
    ASTM_A307_A("ASTM A307 Grade A (60 ksi / 414 MPa)", "1008", 414.0),
    ASTM_A307_B("ASTM A307 Grade B (60–100 ksi / 414 MPa min)", "1018", 414.0),
    ASTM_A194_2H("ASTM A194 Grade 2H Nut (24–35 HRC • mates B7)", "4140", 862.0),
    ASTM_A194_8("ASTM A194 Grade 8 / 8M Nut (austenitic • mates B8/B8M)", "SS304", 515.0),
    ASTM_A563_DH("ASTM A563 Grade DH Nut (structural • mates A490)", "10B21", 1035.0),
    ASTM_F1554_36("ASTM F1554 Grade 36 (58 ksi / 400 MPa • anchor rod)", "1008", 400.0),
    ASTM_F1554_105("ASTM F1554 Grade 105 (125 ksi / 862 MPa • anchor rod)", "4140", 862.0),
    AERO_1100("Aero Titanium (1050-1120 MPa • STA)", "TI_GR5", 1050.0),
    AERO_1380("Aero Superalloy (1380-1420 MPa • Aged)", "INCONEL718", 1380.0),
    AERO_1750("Aero Ultra-High (1720–1950 MPa • 300M/MP35N)", "300M", 1720.0),
    AERO_AL("Aerospace Aluminum (450–570 MPa • T6/T73)", "AL7075", 450.0),
    // Strength comes from the alloy's own procurement spec (classStrength falls back to the material values)
    MATERIAL_SPEC("Per material spec (AMS / ASTM / MIL) • alloy's own strength", "A286", 0.0)
}

enum class CoatingType(
    val code: String,
    val label: String,
    val isElectroplating: Boolean,
    val thicknessMinUm: Double,
    val thicknessMaxUm: Double,
    val requiresHydrogenBaking: Boolean,
    val specStandard: String
) {
    ZN_NI(
        "ZN_NI",
        "Zn-Ni 1000h (AMS 2417)",
        true,
        8.0,
        15.0,
        true,
        "AMS 2417 / ASTM B841 (8–15 µm)"
    ),
    ZINC_BLUE(
        "ZINC_BLUE",
        "Electro-Zinc Blue Cr3+ (ISO 4042)",
        true,
        5.0,
        8.0,
        true,
        "ISO 4042 / ASTM B633 (5–8 µm)"
    ),
    ZINC_YELLOW(
        "ZINC_YELLOW",
        "Electro-Zinc Yellow Iridescent",
        true,
        8.0,
        12.0,
        true,
        "ISO 4042 / ASTM B633 (8–12 µm)"
    ),
    GEOMET(
        "GEOMET",
        "Geomet 321/500 Zinc Flake",
        false,
        5.0,
        10.0,
        false,
        "ISO 10683 / ASTM F1136 (5–10 µm)"
    ),
    DACROMET(
        "DACROMET",
        "Dacromet 320 Zinc-Al Flake",
        false,
        6.0,
        12.0,
        false,
        "ASTM F1136 / ISO 10683 (6–12 µm)"
    ),
    PHOSPHATE(
        "PHOSPHATE",
        "Zinc Phosphate + MoS2 Lube",
        false,
        4.0,
        8.0,
        false,
        "MIL-DTL-16232 / MIL-L-46010 (4–8 µm)"
    ),
    PHOSPHATE_MN(
        "PHOSPHATE_MN",
        "Manganese Phosphate Parkerizing",
        false,
        5.0,
        10.0,
        false,
        "MIL-DTL-16232 Type M (5–10 µm)"
    ),
    // AMS-QQ-P-416 sets only a minimum per class (Class 1 0.0005", Class 2 0.0003", Class 3 0.0002"); the maximum is the shop limit
    AERO_CAD(
        "AERO_CAD",
        "Cadmium QQ-P-416 Type II Class 1 (13 µm min)",
        true,
        13.0,
        18.0,
        true,
        "AMS-QQ-P-416 Type II Class 1: 13 µm min (18 µm shop max)"
    ),
    AERO_CAD_CL2(
        "AERO_CAD_CL2",
        "Cadmium QQ-P-416 Type II Class 2 (8 µm min)",
        true,
        8.0,
        13.0,
        true,
        "AMS-QQ-P-416 Type II Class 2: 8 µm min (13 µm shop max)"
    ),
    AERO_CAD_CL3(
        "AERO_CAD_CL3",
        "Cadmium QQ-P-416 Type II Class 3 (5 µm min)",
        true,
        5.0,
        8.0,
        true,
        "AMS-QQ-P-416 Type II Class 3: 5 µm min (8 µm shop max)"
    ),
    AERO_CAD_LHE(
        "AERO_CAD_LHE",
        "Cadmium LHE (BAC 5701 / Low H2)",
        true,
        8.0,
        13.0,
        true,
        "Boeing BAC 5701 / AMS-QQ-P-416 (8–13 µm)"
    ),
    AERO_IVD(
        "AERO_IVD",
        "IVD Aluminum (MIL-DTL-83488)",
        false,
        8.0,
        13.0,
        false,
        "MIL-DTL-83488 Class 1 (8–13 µm)"
    ),
    ENP(
        "ENP",
        "Electroless Nickel (AMS 2404 / 50 HRC)",
        false,
        10.0,
        20.0,
        false,
        "AMS 2404 / ASTM B733 (10–20 µm)"
    ),
    PASSIVATION(
        "PASSIVATION",
        "Nitric Passivation (ASTM A967)",
        false,
        0.0,
        0.5,
        false,
        "ASTM A967 Nitric 2 / AMS 2700 (<0.5 µm)"
    ),
    PASSIVATION_CITRIC(
        "PASSIVATION_CITRIC",
        "Citric Passivation (ASTM A967)",
        false,
        0.0,
        0.5,
        false,
        "ASTM A967 Citric 4 / AMS 2700 (<0.5 µm)"
    ),
    BLACK_OXIDE(
        "BLACK_OXIDE",
        "Black Oxide + Oil (MIL-DTL-13924)",
        false,
        1.0,
        2.0,
        false,
        "MIL-DTL-13924 Class 1 (1–2 µm)"
    ),
    HOT_DIP_GALV(
        "HOT_DIP_GALV",
        "Hot-Dip Galvanizing (ISO 10684)",
        false,
        45.0,
        65.0,
        false,
        "ISO 10684 / ASTM A153 (45–65 µm)"
    ),
    PTFE_XYLAN(
        "PTFE_XYLAN",
        "PTFE / Xylan 1424 Fluoropolymer",
        false,
        15.0,
        25.0,
        false,
        "Whitford Xylan 1424 (15–25 µm)"
    ),
    ZN_FE("ZN_FE", "Zinc-Iron Fe/Zn (ISO 4042)", true, 5.0, 12.0, true, "ISO 4042 Zn-Fe (5–12 µm, e.g. Fe/Zn 12)"),
    MAGNI_565("MAGNI_565", "Magni 565 Zinc Flake (ISO 10683)", false, 13.0, 18.0, false, "ISO 10683 flZn / Magni 565 (8 µm base + 5 µm top min; 13–18 µm)"),
    DELTA_PROTEKT("DELTA_PROTEKT", "Delta-Protekt KL 100 Zinc Flake (ISO 10683)", false, 6.0, 12.0, false, "ISO 10683 / Dörken Delta-Protekt KL 100 basecoat (6–12 µm)"),
    TIN("TIN", "Tin Plating (ASTM B545)", true, 2.5, 5.1, true, "ASTM B545 Class A – B (2.5–5 µm)"),
    ANODIZE_II("ANODIZE_II", "Sulfuric Anodize Type II — Aluminium (MIL-PRF-8625)", false, 5.0, 20.0, false, "MIL-PRF-8625 Type II (≈ 5–20 µm typical)"),
    HARD_ANODIZE("HARD_ANODIZE", "Hard Anodize Type III — Aluminium (MIL-PRF-8625)", false, 13.0, 50.0, false, "MIL-PRF-8625 Type III (12.7–115 µm; 50 µm when not specified)"),
    TI_ANODIZE("TI_ANODIZE", "Titanium Anodize Type II (AMS 2488)", false, 0.5, 5.0, false, "AMS 2488 Type II (0.5–5 µm, vendor figures — verify)"),
    CHEM_FILM("CHEM_FILM", "Chemical Conversion / Chem Film — Aluminium (MIL-DTL-5541)", false, 0.0, 0.5, false, "MIL-DTL-5541 Class 1A / 3 (negligible build-up)")
}

data class HeatTreatmentSpec(
    val austenitizingOrSolutionTempC: String,
    val soakingTimeMinutes: String,
    val quenchMedium: String,
    val temperingOrAgingTempC: String,
    val temperingOrAgingTimeHours: String,
    val targetHardnessHrc: String,
    val hydrogenBakingRequired: Boolean,
    val hydrogenBakingCycle: String,
    val standardReference: String,
    // Stage-by-Stage Dynamic & Soaking Schedule Fields (AMS 2759 / ISO 898-1).
    // Blank = not stated for this alloy: steel furnace values must never default onto aluminium, titanium or nickel.
    val preheatTempC: String = "",
    val preheatSoakTime: String = "",
    val atmosphere: String = "",
    val quenchDetails: String = "",
    val coolingMethod: String = "",
    val targetUtsMpa: String = "",
    val bakingTempC: String = "190°C – 200°C",
    val bakingSoakTime: String = "8 to 24 Hours mandatory soak",
    val isDoubleTemper: Boolean = false,
    val isAging: Boolean = false
)

data class MaterialInfo(
    val key: String,
    val name: String,
    val density: Double, // g/cm³
    val uts: Double, // Nominal Finished UTS (MPa)
    val vc: Double, // Cutting speed m/min
    val feed: Double, // mm/rev
    val doc: Double, // mm
    val rateInr: Double, // ₹/kg
    val km: Double, // Material factor for rolling blank
    val isAero: Boolean,
    val hasHighHardness: Boolean, // >32 HRC
    val headingSpm: Int,
    val insertSpec: String,
    val heatTreatment: String,
    val lubricationCoating: String,
    val dieTooling: String,
    // Mechanical & Forging UTS Sourcing Specs
    val rawMaterialSourcingUtsMpa: String,
    val postForgingTargetUtsMpa: Double,
    val yieldStrengthMpa: Double,
    val minElongationPct: Double,
    val reductionOfAreaZPct: Double,
    val flowStressCoeffK: Double, // Strength coefficient K (MPa)
    val strainHardeningN: Double, // Strain hardening exponent n
    val heatTreatmentSpec: HeatTreatmentSpec,
    /** Base grade whose unsourced values (machining, flow stress, price, tooling) this grade copies. */
    val estimatedFrom: String? = null,
    /** Datasheets the sourced values came from. */
    val dataSources: List<String> = emptyList()
)

data class MetricSize(
    val label: String,
    val d: Double,
    val pitches: List<Double> // Coarse first, then fine
)

data class InchSize(
    val label: String,
    val dInch: Double,
    val tpis: List<Int>, // Coarse (UNC) first, then fine (UNF)
    /** #0 exists only as 0-80 UNF. */
    val hasUnc: Boolean = true
)

object FastenerCatalog {
    private val BASE_MATERIALS: Map<String, MaterialInfo> = mapOf(
        "1008" to MaterialInfo(
            key = "1008",
            name = "Mild Steel (AISI 1008/1010)",
            density = 7.85, uts = 420.0, vc = 220.0, feed = 0.24, doc = 1.8, rateInr = 84.0, km = 0.010,
            isAero = false, hasHighHardness = false, headingSpm = 150,
            insertSpec = "CNMG 120408-PM (CVD ISO P25)",
            heatTreatment = "Cold Formed / Stress Relieved at 520°C.",
            lubricationCoating = "Electro-Zinc Blue.",
            dieTooling = "Carbide GT20 Die + D2/M2 Punch.",
            rawMaterialSourcingUtsMpa = "380 – 440 MPa (Annealed Drawn Wire)",
            postForgingTargetUtsMpa = 420.0,
            yieldStrengthMpa = 340.0,
            minElongationPct = 14.0,
            reductionOfAreaZPct = 55.0,
            flowStressCoeffK = 620.0,
            strainHardeningN = 0.22,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "N/A (Cold Formed Only)",
                soakingTimeMinutes = "Stress relief: 520°C–550°C (60 min)",
                quenchMedium = "Air Cool / Furnace Cool",
                temperingOrAgingTempC = "N/A (No hardening)",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "71 – 85 HRB (120–165 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required (<32 HRC)",
                standardReference = "ISO 898-1 Class 4.8"
            )
        ),
        "1018" to MaterialInfo(
            key = "1018",
            name = "Low Carbon (AISI 1018/1020)",
            density = 7.85, uts = 520.0, vc = 200.0, feed = 0.22, doc = 1.5, rateInr = 92.0, km = 0.012,
            isAero = false, hasHighHardness = false, headingSpm = 140,
            insertSpec = "CNMG 120408-PM (CVD ISO P25)",
            heatTreatment = "Normalized at 890°C or Stress Relieved at 540°C.",
            lubricationCoating = "Electro-Zinc.",
            dieTooling = "Carbide GT20 Die + M2 Punch.",
            rawMaterialSourcingUtsMpa = "440 – 520 MPa (Cold Drawn Rod)",
            postForgingTargetUtsMpa = 520.0,
            yieldStrengthMpa = 420.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 680.0,
            strainHardeningN = 0.18,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Normalizing: 880°C–900°C",
                soakingTimeMinutes = "45 – 60 min",
                quenchMedium = "Still Air Cool",
                temperingOrAgingTempC = "Stress relief: 540°C (60 min)",
                temperingOrAgingTimeHours = "1.0 hr",
                targetHardnessHrc = "82 – 95 HRB (150–205 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required (<32 HRC)",
                standardReference = "ISO 898-1 Class 5.8 / ASTM A307"
            )
        ),
        "10B21" to MaterialInfo(
            key = "10B21",
            name = "10B21 / 10B38 Boron Steel",
            density = 7.85, uts = 1040.0, vc = 180.0, feed = 0.20, doc = 1.5, rateInr = 118.0, km = 0.015,
            isAero = false, hasHighHardness = true, headingSpm = 120,
            insertSpec = "CNMG 120408-PM (CVD ISO P20)",
            heatTreatment = "Austenitize 870°C -> Oil Quench -> Temper 450°C (32-39 HRC).",
            lubricationCoating = "Zn-Ni + Mandatory Hydrogen Baking (195°C, 8h).",
            dieTooling = "GT55 Carbide Die + M2/ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "480 – 560 MPa (Spheroidized Annealed Wire, >85% globular)",
            postForgingTargetUtsMpa = 1040.0,
            yieldStrengthMpa = 940.0,
            minElongationPct = 9.0,
            reductionOfAreaZPct = 48.0,
            flowStressCoeffK = 780.0,
            strainHardeningN = 0.15,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "860°C – 880°C (Endothermic atm, C-pot 0.21%)",
                soakingTimeMinutes = "45 – 60 min continuous mesh belt",
                quenchMedium = "Agitated Accelerated Quench Oil (50°C–70°C)",
                temperingOrAgingTempC = "425°C – 475°C",
                temperingOrAgingTimeHours = "1.5 – 2.0 hrs (90–120 min)",
                targetHardnessHrc = "32 – 39 HRC (320–380 HV10)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 8 to 14 hours within 2 hours post-electroplating",
                standardReference = "ISO 898-1 Class 10.9 / ASTM F1940 / ASTM A490"
            )
        ),
        "4135" to MaterialInfo(
            key = "4135",
            name = "AISI 4135 / 35CrMo Alloy",
            density = 7.85, uts = 1100.0, vc = 155.0, feed = 0.17, doc = 1.2, rateInr = 162.0, km = 0.016,
            isAero = false, hasHighHardness = true, headingSpm = 100,
            insertSpec = "CNMG 120408-PR (CVD ISO P15)",
            heatTreatment = "Austenitize 855°C -> Oil Quench -> Temper 510°C (38-42 HRC).",
            lubricationCoating = "Phosphate or Zn-Ni + Baking.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "580 – 660 MPa (Annealed / Spheroidized Wire)",
            postForgingTargetUtsMpa = 1100.0,
            yieldStrengthMpa = 990.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 840.0,
            strainHardeningN = 0.14,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "845°C – 865°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Agitated Quench Oil (60°C)",
                temperingOrAgingTempC = "480°C – 530°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "35 – 41 HRC (345–400 HV10)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 12 to 16 hours within 2 hours post-plating",
                standardReference = "ISO 898-1 Class 10.9 / DIN EN 10269"
            )
        ),
        "4140" to MaterialInfo(
            key = "4140",
            name = "AISI 4140 / EN24 Alloy",
            density = 7.85, uts = 1220.0, vc = 145.0, feed = 0.16, doc = 1.2, rateInr = 175.0, km = 0.016,
            isAero = false, hasHighHardness = true, headingSpm = 95,
            insertSpec = "CNMG 120408-PR (CVD ISO P15)",
            heatTreatment = "Austenitize 850°C -> Oil Quench -> Temper 530°C (39-44 HRC).",
            lubricationCoating = "Phosphate/Zn-Ni + Baking at 195°C (12h).",
            dieTooling = "GT55 Carbide + Vacuum HT ASP30 Punch (63 HRC).",
            rawMaterialSourcingUtsMpa = "650 – 730 MPa (Spheroidized Wire Coil)",
            postForgingTargetUtsMpa = 1220.0,
            yieldStrengthMpa = 1100.0,
            minElongationPct = 8.0,
            reductionOfAreaZPct = 44.0,
            flowStressCoeffK = 920.0,
            strainHardeningN = 0.13,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "840°C – 860°C (Controlled Carbon Potential)",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Vigorous Agitated Oil Quench (0.5 m/s, 50°C)",
                temperingOrAgingTempC = "510°C – 540°C (Double Temper for Aero)",
                temperingOrAgingTimeHours = "2.0 hrs + 2.0 hrs",
                targetHardnessHrc = "39 – 44 HRC (385–435 HV10)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 14 to 24 hours within 1 hour post-electroplating",
                standardReference = "ISO 898-1 Class 12.9 / ASTM A574 / MIL-S-5626"
            )
        ),
        "4340" to MaterialInfo(
            key = "4340",
            name = "AISI 4340 / 35NCD16",
            density = 7.85, uts = 1300.0, vc = 130.0, feed = 0.14, doc = 1.0, rateInr = 220.0, km = 0.017,
            isAero = true, hasHighHardness = true, headingSpm = 80,
            insertSpec = "CNMG 120408-PR (CVD ISO P10)",
            heatTreatment = "Vacuum Harden 845°C -> Oil Quench -> Double Temper 520°C.",
            lubricationCoating = "Aero Cd / IVD Al + 24h Baking.",
            dieTooling = "WC-15%Co Carbide + ASP30 AlCrN Punch.",
            rawMaterialSourcingUtsMpa = "720 – 800 MPa (Vacuum Degassed VAR Wire)",
            postForgingTargetUtsMpa = 1300.0,
            yieldStrengthMpa = 1180.0,
            minElongationPct = 9.0,
            reductionOfAreaZPct = 45.0,
            flowStressCoeffK = 980.0,
            strainHardeningN = 0.12,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "830°C – 850°C (Vacuum Furnace <10^-3 torr)",
                soakingTimeMinutes = "60 min",
                quenchMedium = "High-Pressure Gas Quench (6 bar N2) or Vacuum Oil",
                temperingOrAgingTempC = "510°C – 530°C",
                temperingOrAgingTimeHours = "2.0 hrs + 2.0 hrs Double Temper",
                targetHardnessHrc = "40 – 45 HRC (392–446 HV10)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "195°C – 205°C for 24 hours per AMS 2759/9",
                standardReference = "AMS 6414 / AMS 6415 / MIL-H-6875"
            )
        ),
        "SS204" to MaterialInfo(
            key = "SS204",
            name = "SS204Cu Austenitic SS",
            density = 7.90, uts = 760.0, vc = 115.0, feed = 0.15, doc = 1.0, rateInr = 235.0, km = 0.023,
            isAero = false, hasHighHardness = false, headingSpm = 85,
            insertSpec = "WNMG 080408-MM (PVD TiAlN ISO M20)",
            heatTreatment = "Warm Form at 180°C-240°C to prevent trap extrusion die breakage.",
            lubricationCoating = "Oxalate + MoS2. Citric Passivation.",
            dieTooling = "2-Stage Trap Extrusion GT55 + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "650 – 720 MPa (Soft Annealed Wire)",
            postForgingTargetUtsMpa = 760.0,
            yieldStrengthMpa = 520.0,
            minElongationPct = 25.0,
            reductionOfAreaZPct = 60.0,
            flowStressCoeffK = 950.0,
            strainHardeningN = 0.28,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1020°C – 1050°C (Solution Anneal)",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Rapid Water Quench (<3 sec transfer)",
                temperingOrAgingTempC = "N/A (Non-hardenable by HT)",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "24 – 30 HRC (Work hardened)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required (Austenitic matrix)",
                standardReference = "ASTM A493 / ISO 3506-1"
            )
        ),
        "SS304" to MaterialInfo(
            key = "SS304",
            name = "SS304 / A2-70 Stainless",
            density = 7.93, uts = 750.0, vc = 120.0, feed = 0.15, doc = 1.0, rateInr = 265.0, km = 0.024,
            isAero = false, hasHighHardness = false, headingSpm = 85,
            insertSpec = "WNMG 080408-MM (PVD TiAlN ISO M20)",
            heatTreatment = "Solution Anneal 1050°C + Water Quench.",
            lubricationCoating = "Nitric/Citric Passivation.",
            dieTooling = "GT55 Carbide + ASP30 TiAlN Punch.",
            rawMaterialSourcingUtsMpa = "550 – 650 MPa (Bright Annealed Wire)",
            postForgingTargetUtsMpa = 750.0,
            yieldStrengthMpa = 450.0,
            minElongationPct = 20.0,
            reductionOfAreaZPct = 55.0,
            flowStressCoeffK = 1050.0,
            strainHardeningN = 0.32,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1040°C – 1080°C (Carbide Solution)",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Rapid Agitated Water Quench",
                temperingOrAgingTempC = "Passivation: ASTM A967 Citric/Nitric",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "22 – 28 HRC (Post cold-heading)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required",
                standardReference = "ISO 3506-1 Class A2-70 / ASTM F593"
            )
        ),
        "SS316" to MaterialInfo(
            key = "SS316",
            name = "SS316 / A4-80 Stainless",
            density = 7.98, uts = 800.0, vc = 105.0, feed = 0.14, doc = 1.0, rateInr = 345.0, km = 0.025,
            isAero = false, hasHighHardness = false, headingSpm = 80,
            insertSpec = "CNMG 120408-SM (PVD AlTiN ISO M15)",
            heatTreatment = "Solution Anneal 1060°C + Water Quench.",
            lubricationCoating = "Passivation per ASTM A967.",
            dieTooling = "Sub-micron Carbide + ASP60 Punch.",
            rawMaterialSourcingUtsMpa = "580 – 680 MPa (Annealed Wire Coil)",
            postForgingTargetUtsMpa = 800.0,
            yieldStrengthMpa = 600.0,
            minElongationPct = 18.0,
            reductionOfAreaZPct = 52.0,
            flowStressCoeffK = 1120.0,
            strainHardeningN = 0.30,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1060°C – 1100°C",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Rapid Water Quench",
                temperingOrAgingTempC = "Passivation: 20–25% Nitric Acid (50°C, 30 min)",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "25 – 32 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required",
                standardReference = "ISO 3506-1 Class A4-80 / ASTM F593 Group 2"
            )
        ),
        "SS410" to MaterialInfo(
            key = "SS410",
            name = "SS410 Martensitic SS",
            density = 7.75, uts = 850.0, vc = 135.0, feed = 0.16, doc = 1.2, rateInr = 210.0, km = 0.018,
            isAero = false, hasHighHardness = true, headingSpm = 95,
            insertSpec = "CNMG 120408-PM (PVD ISO M15)",
            heatTreatment = "Harden 980°C -> Oil Quench -> Temper 580°C.",
            lubricationCoating = "Passivated & Baked.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "520 – 600 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 850.0,
            yieldStrengthMpa = 700.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 48.0,
            flowStressCoeffK = 820.0,
            strainHardeningN = 0.15,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "970°C – 1000°C",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Agitated Oil Quench or Pressurized Air",
                temperingOrAgingTempC = "560°C – 600°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "32 – 38 HRC (320–370 HV)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 8 to 12 hours post-plating",
                standardReference = "ASTM A493 / ISO 3506-1 Class C1"
            )
        ),
        "17_4PH" to MaterialInfo(
            key = "17_4PH",
            name = "17-4PH AMS 5643 SS",
            density = 7.80, uts = 1150.0, vc = 85.0, feed = 0.13, doc = 0.9, rateInr = 680.0, km = 0.021,
            isAero = true, hasHighHardness = true, headingSpm = 65,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO S15)",
            heatTreatment = "Solution 1040°C -> H900/H1025 Precipitation Age.",
            lubricationCoating = "Passivation + MoS2.",
            dieTooling = "HIP Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "950 – 1050 MPa (Condition A Solution Annealed)",
            postForgingTargetUtsMpa = 1150.0,
            yieldStrengthMpa = 1000.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 40.0,
            flowStressCoeffK = 1200.0,
            strainHardeningN = 0.11,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Condition A Solution: 1040°C ± 15°C",
                soakingTimeMinutes = "30 min",
                quenchMedium = "Oil Quench or Forced Air Cool below 32°C",
                temperingOrAgingTempC = "Precipitation Aging: H900 (480°C, 1h) or H1025 (550°C, 4h)",
                temperingOrAgingTimeHours = "1.0 – 4.0 hrs",
                targetHardnessHrc = "35 – 44 HRC (Condition dependent)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required for passivated finish",
                standardReference = "AMS 5643 / AMS 2759/3"
            )
        ),
        "A286" to MaterialInfo(
            key = "A286",
            name = "A286 Superalloy (AMS 5731)",
            density = 7.92, uts = 1100.0, vc = 52.0, feed = 0.12, doc = 0.8, rateInr = 1550.0, km = 0.026,
            isAero = true, hasHighHardness = true, headingSpm = 50,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO S15)",
            heatTreatment = "Solution 980°C -> Age at 720°C for 16 hrs.",
            lubricationCoating = "Copper + MoS2. Final: IVD Aluminum.",
            dieTooling = "HIP Carbide GT55 + Triple Tempered ASP30.",
            rawMaterialSourcingUtsMpa = "650 – 750 MPa (Solution Annealed Bar/Wire)",
            postForgingTargetUtsMpa = 1100.0,
            yieldStrengthMpa = 720.0,
            minElongationPct = 15.0,
            reductionOfAreaZPct = 35.0,
            flowStressCoeffK = 1280.0,
            strainHardeningN = 0.20,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Anneal: 980°C ± 10°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Rapid Oil Quench or Water Quench",
                temperingOrAgingTempC = "Precipitation Hardening: 720°C ± 5°C",
                temperingOrAgingTimeHours = "16.0 hrs continuous soak",
                targetHardnessHrc = "32 – 38 HRC (320–375 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required (Aero IVD Al coating used)",
                standardReference = "AMS 5731 / AMS 5732 / NAS624 / MS21250"
            )
        ),
        "TI_GR1" to MaterialInfo(
            key = "TI_GR1",
            name = "Titanium Grade 1 / 2 CP",
            density = 4.51, uts = 480.0, vc = 80.0, feed = 0.15, doc = 1.0, rateInr = 2400.0, km = 0.019,
            isAero = true, hasHighHardness = false, headingSpm = 70,
            insertSpec = "CCMT 120408 Sharp Rake (ISO S10)",
            heatTreatment = "Stress Relieve Anneal 540°C (1h).",
            lubricationCoating = "Anodizing + Lube.",
            dieTooling = "Carbide + ASP30 AlCrN Punch.",
            rawMaterialSourcingUtsMpa = "420 – 520 MPa (CP Grade 2 Wire)",
            postForgingTargetUtsMpa = 480.0,
            yieldStrengthMpa = 345.0,
            minElongationPct = 20.0,
            reductionOfAreaZPct = 40.0,
            flowStressCoeffK = 750.0,
            strainHardeningN = 0.18,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Stress Relief Annealing: 520°C–540°C",
                soakingTimeMinutes = "60 min in Argon Vacuum",
                quenchMedium = "Inert Argon Gas Cool",
                temperingOrAgingTempC = "N/A",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "20 – 24 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required (Vacuum degassed)",
                standardReference = "ASTM B348 / AMS 4921"
            )
        ),
        "TI_GR5" to MaterialInfo(
            key = "TI_GR5",
            name = "Titanium Gr 5 (Ti-6Al-4V)",
            density = 4.43, uts = 1120.0, vc = 60.0, feed = 0.12, doc = 0.8, rateInr = 3300.0, km = 0.022,
            isAero = true, hasHighHardness = true, headingSpm = 45,
            insertSpec = "VCMT 160404 Sharp Rake (PVD ISO S10)",
            heatTreatment = "Warm Head at 680°C. Vacuum Solution 955°C + Age 540°C (6h).",
            lubricationCoating = "Dual Lube: Glass + MoS2. Final: Cetyl Alcohol.",
            dieTooling = "H13 Nitrided Die + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "890 – 960 MPa (Annealed Wire, max H2: 125 ppm)",
            postForgingTargetUtsMpa = 1120.0,
            yieldStrengthMpa = 1030.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 25.0,
            flowStressCoeffK = 1350.0,
            strainHardeningN = 0.10,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Vacuum Solution: 955°C ± 10°C (<10^-4 torr)",
                soakingTimeMinutes = "45 min",
                quenchMedium = "High-pressure Argon/Helium Gas Quench",
                temperingOrAgingTempC = "Precipitation Aging: 540°C ± 5°C",
                temperingOrAgingTimeHours = "6.0 – 8.0 hrs vacuum soak",
                targetHardnessHrc = "36 – 42 HRC (350–410 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Vacuum outgassing verified (H2 < 125 ppm)",
                standardReference = "AMS 4928 / AMS 4967 / AMS 2759/4"
            )
        ),
        "WASPALOY" to MaterialInfo(
            key = "WASPALOY",
            name = "Waspaloy (AMS 5708)",
            density = 8.19, uts = 1320.0, vc = 32.0, feed = 0.09, doc = 0.5, rateInr = 5400.0, km = 0.029,
            isAero = true, hasHighHardness = true, headingSpm = 30,
            insertSpec = "CNMG 120408-QM (PVD AlTiN ISO S05)",
            heatTreatment = "Solution 1010°C -> Stabilize 845°C -> Age 760°C (16h).",
            lubricationCoating = "Cu Plate + MoS2. Roll Post-HT.",
            dieTooling = "WC-15%Co + ASP60 Punch.",
            rawMaterialSourcingUtsMpa = "850 – 950 MPa (Solution Treated Wire)",
            postForgingTargetUtsMpa = 1320.0,
            yieldStrengthMpa = 980.0,
            minElongationPct = 15.0,
            reductionOfAreaZPct = 20.0,
            flowStressCoeffK = 1550.0,
            strainHardeningN = 0.14,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Stage 1 Solution: 1010°C–1040°C (4 hrs)",
                soakingTimeMinutes = "240 min",
                quenchMedium = "Agitated Oil Quench",
                temperingOrAgingTempC = "Stage 2 Stabilize: 845°C (4h) -> Stage 3 Age: 760°C (16h)",
                temperingOrAgingTimeHours = "20.0 hrs total aging",
                targetHardnessHrc = "37 – 44 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required for nickel superalloys",
                standardReference = "AMS 5708 / UNS N07001"
            )
        ),
        "INCONEL718" to MaterialInfo(
            key = "INCONEL718",
            name = "Inconel 718 (AMS 5662)",
            density = 8.19, uts = 1380.0, vc = 36.0, feed = 0.10, doc = 0.6, rateInr = 4950.0, km = 0.028,
            isAero = true, hasHighHardness = true, headingSpm = 35,
            insertSpec = "CNMG 120408-QM (PVD AlTiN ISO S05)",
            heatTreatment = "Solution 955°C -> Double Age: 720°C (8h) + 620°C (8h).",
            lubricationCoating = "Cu + MoS2. Final: IVD Al. Roll Post-HT.",
            dieTooling = "WC-15%Co + Mirror ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "920 – 1020 MPa (AMS 5662 Annealed Wire)",
            postForgingTargetUtsMpa = 1380.0,
            yieldStrengthMpa = 1170.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 22.0,
            flowStressCoeffK = 1600.0,
            strainHardeningN = 0.13,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Anneal: 955°C – 980°C (Vacuum)",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Rapid Inert Gas Quench (Pressure > 4 bar)",
                temperingOrAgingTempC = "Double Age: 720°C (8h) -> furnace cool 50°C/h -> 620°C (8h)",
                temperingOrAgingTimeHours = "18.0 hrs total cycle",
                targetHardnessHrc = "42 – 47 HRC (415–465 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required (Aero IVD Al / Passivated)",
                standardReference = "AMS 5662 / AMS 5664 / AMS 2759/3 / NAS624"
            )
        ),
        "300M" to MaterialInfo(
            key = "300M",
            name = "300M Ultra-High Strength (AMS 6417)",
            density = 7.85, uts = 1950.0, vc = 85.0, feed = 0.12, doc = 0.8, rateInr = 480.0, km = 0.017,
            isAero = true, hasHighHardness = true, headingSpm = 60,
            insertSpec = "CNMG 120408-PR (CVD ISO P10)",
            heatTreatment = "Austenitize 870°C -> Oil Quench -> Double Temper 300°C–315°C (52-56 HRC).",
            lubricationCoating = "Aero Cd / IVD Al + Mandatory 24h Baking (195°C).",
            dieTooling = "WC-15%Co GT55 Die + ASP60 Punch (64 HRC).",
            rawMaterialSourcingUtsMpa = "820 – 900 MPa (Vacuum Arc Remelted VAR Wire)",
            postForgingTargetUtsMpa = 1950.0,
            yieldStrengthMpa = 1550.0,
            minElongationPct = 8.0,
            reductionOfAreaZPct = 38.0,
            flowStressCoeffK = 1250.0,
            strainHardeningN = 0.11,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "870°C ± 5°C (Vacuum / Controlled Atmosphere)",
                soakingTimeMinutes = "45 – 60 min",
                quenchMedium = "Agitated Quench Oil (50°C–70°C)",
                temperingOrAgingTempC = "300°C – 315°C",
                temperingOrAgingTimeHours = "Double Temper: 2.0 hrs + 2.0 hrs (2 x 120 mins)",
                targetHardnessHrc = "52 – 56 HRC (550–615 HV10)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 24 hours mandatory soak within 1 hr post-electroplating per AMS 2759/9",
                standardReference = "AMS 6417 / AMS 6419 / MIL-S-8844"
            )
        ),
        "8740" to MaterialInfo(
            key = "8740",
            name = "AISI 8740 Aircraft Alloy (AMS 6322)",
            density = 7.85, uts = 1150.0, vc = 150.0, feed = 0.16, doc = 1.2, rateInr = 195.0, km = 0.016,
            isAero = true, hasHighHardness = true, headingSpm = 95,
            insertSpec = "CNMG 120408-PR (CVD ISO P15)",
            heatTreatment = "Austenitize 845°C–865°C -> Oil Quench -> Temper 480°C–540°C (36-42 HRC).",
            lubricationCoating = "Cadmium / Phosphate + Mandatory Hydrogen Baking.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "600 – 700 MPa (Spheroidized Annealed Wire)",
            postForgingTargetUtsMpa = 1150.0,
            yieldStrengthMpa = 1000.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 48.0,
            flowStressCoeffK = 860.0,
            strainHardeningN = 0.14,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "845°C – 865°C (Controlled Carbon Potential)",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Agitated Quench Oil (50°C–65°C)",
                temperingOrAgingTempC = "510°C – 540°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "36 – 42 HRC (355–415 HV10)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 14 to 24 hours post-plating",
                standardReference = "AMS 6322 / AMS 6325 / MIL-S-6049"
            )
        ),
        "H11_H13" to MaterialInfo(
            key = "H11_H13",
            name = "H11 / H13 Tool Steel (AMS 6487)",
            density = 7.80, uts = 1750.0, vc = 90.0, feed = 0.12, doc = 0.8, rateInr = 380.0, km = 0.018,
            isAero = true, hasHighHardness = true, headingSpm = 65,
            insertSpec = "CNMG 120408-PR (PVD AlTiN ISO P10)",
            heatTreatment = "Preheat 650°C -> Austenitize 1020°C -> Gas/Oil Quench -> Triple Temper 540°C–560°C.",
            lubricationCoating = "Diffusion Coating / IVD Al (High temp up to 500°C).",
            dieTooling = "GT55 Carbide + ASP60 Triple Tempered Punch.",
            rawMaterialSourcingUtsMpa = "720 – 820 MPa (Annealed Wire Coil)",
            postForgingTargetUtsMpa = 1750.0,
            yieldStrengthMpa = 1450.0,
            minElongationPct = 7.0,
            reductionOfAreaZPct = 35.0,
            flowStressCoeffK = 1150.0,
            strainHardeningN = 0.12,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1010°C – 1030°C (Vacuum Furnace <10⁻³ mbar)",
                soakingTimeMinutes = "45 – 60 min",
                quenchMedium = "High-pressure N2 Gas Quench (6 bar) or Vacuum Oil",
                temperingOrAgingTempC = "540°C – 560°C (Secondary Hardening Peak)",
                temperingOrAgingTimeHours = "Triple Temper: 3 x 2.0 hrs (3 x 120 mins)",
                targetHardnessHrc = "48 – 53 HRC (480–560 HV)",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 24 hours mandatory soak post-coating",
                standardReference = "AMS 6487 / AMS 6488 / ASTM A681"
            )
        ),
        "15_5PH" to MaterialInfo(
            key = "15_5PH",
            name = "15-5PH AMS 5659 Stainless",
            density = 7.80, uts = 1250.0, vc = 85.0, feed = 0.13, doc = 0.9, rateInr = 720.0, km = 0.021,
            isAero = true, hasHighHardness = true, headingSpm = 65,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO S15)",
            heatTreatment = "Solution 1038°C -> Rapid Air/Oil Quench -> Age H900 (482°C) / H1025 (552°C).",
            lubricationCoating = "Citric Passivation per AMS 2700 + Cetyl Alcohol.",
            dieTooling = "HIP Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "950 – 1050 MPa (Condition A Solution Treated)",
            postForgingTargetUtsMpa = 1250.0,
            yieldStrengthMpa = 1100.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 45.0,
            flowStressCoeffK = 1200.0,
            strainHardeningN = 0.11,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1038°C ± 10°C (Condition A Solution)",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Rapid Air Cool or Agitated Oil below 32°C",
                temperingOrAgingTempC = "Precipitation Aging: H900 (482°C, 1h) or H1025 (552°C, 4h)",
                temperingOrAgingTimeHours = "1.0 – 4.0 hrs",
                targetHardnessHrc = "38 – 45 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required for passivated finish",
                standardReference = "AMS 5659 / AMS 5862 / AMS 2759/3"
            )
        ),
        "CUSTOM_455" to MaterialInfo(
            key = "CUSTOM_455",
            name = "Custom 455 / 465 (AMS 5617/5936)",
            density = 7.78, uts = 1720.0, vc = 65.0, feed = 0.11, doc = 0.7, rateInr = 1450.0, km = 0.022,
            isAero = true, hasHighHardness = true, headingSpm = 50,
            insertSpec = "CNMG 120408-SM (PVD AlTiN ISO S10)",
            heatTreatment = "Solution 840°C -> Deep Freeze -73°C (2h) -> Precipitation Age H900 (482°C, 4h).",
            lubricationCoating = "AMS 2700 Passivation.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "900 – 1020 MPa (Condition A Wire)",
            postForgingTargetUtsMpa = 1720.0,
            yieldStrengthMpa = 1580.0,
            minElongationPct = 8.0,
            reductionOfAreaZPct = 32.0,
            flowStressCoeffK = 1380.0,
            strainHardeningN = 0.10,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "830°C – 845°C (Solution Anneal Condition A)",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Water Quench -> Sub-zero -73°C (2 hrs)",
                temperingOrAgingTempC = "Precipitation Age H900 (482°C ± 5°C)",
                temperingOrAgingTimeHours = "4.0 hrs soak",
                targetHardnessHrc = "49 – 54 HRC (500–580 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required for passivated stainless",
                standardReference = "AMS 5617 / AMS 5936 / AMS 2759/3"
            )
        ),
        "AM350" to MaterialInfo(
            key = "AM350",
            name = "AM-350 / AM-355 (AMS 5548/5745)",
            density = 7.92, uts = 1380.0, vc = 75.0, feed = 0.12, doc = 0.8, rateInr = 1100.0, km = 0.023,
            isAero = true, hasHighHardness = true, headingSpm = 60,
            insertSpec = "WNMG 080408-MM (PVD TiAlN ISO M15)",
            heatTreatment = "Solution 1050°C -> Sub-zero -73°C (3h) -> Double Age 510°C (3h) SCT1000.",
            lubricationCoating = "Passivation + Dry Film Lubricant.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "850 – 950 MPa (Annealed Condition H)",
            postForgingTargetUtsMpa = 1380.0,
            yieldStrengthMpa = 1150.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 38.0,
            flowStressCoeffK = 1250.0,
            strainHardeningN = 0.12,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1050°C ± 10°C (Condition H Solution)",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Rapid Water Quench -> Sub-zero -73°C (3 hrs)",
                temperingOrAgingTempC = "510°C (SCT1000 Aging)",
                temperingOrAgingTimeHours = "Double Age: 3.0 hrs + 3.0 hrs",
                targetHardnessHrc = "42 – 47 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required",
                standardReference = "AMS 5548 / AMS 5745"
            )
        ),
        "RENE41" to MaterialInfo(
            key = "RENE41",
            name = "René 41 Superalloy (AMS 5545/5712)",
            density = 8.25, uts = 1420.0, vc = 28.0, feed = 0.08, doc = 0.5, rateInr = 5800.0, km = 0.030,
            isAero = true, hasHighHardness = true, headingSpm = 25,
            insertSpec = "CNMG 120408-QM (PVD AlTiN ISO S05)",
            heatTreatment = "Vacuum Solution 1100°C (2h) -> High-pressure Argon Quench -> Age 760°C (16h).",
            lubricationCoating = "Glass + MoS2 Dual Lubrication (Jet engine fasteners to 980°C).",
            dieTooling = "Micro-grain WC-15%Co Die + ASP60 Punch.",
            rawMaterialSourcingUtsMpa = "920 – 1020 MPa (Solution Annealed Bar)",
            postForgingTargetUtsMpa = 1420.0,
            yieldStrengthMpa = 1050.0,
            minElongationPct = 14.0,
            reductionOfAreaZPct = 18.0,
            flowStressCoeffK = 1680.0,
            strainHardeningN = 0.13,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1080°C – 1120°C (Vacuum Solution 2 hrs)",
                soakingTimeMinutes = "120 min",
                quenchMedium = "High-velocity Argon gas quench (<60°C in <3 min)",
                temperingOrAgingTempC = "Precipitation Age: 760°C ± 5°C",
                temperingOrAgingTimeHours = "16.0 hrs continuous soak in vacuum",
                targetHardnessHrc = "40 – 45 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not susceptible to hydrogen embrittlement",
                standardReference = "AMS 5545 / AMS 5712 / UNS N07041"
            )
        ),
        "MP35N" to MaterialInfo(
            key = "MP35N",
            name = "MP35N Multiphase (AMS 5844/5758)",
            density = 8.43, uts = 1930.0, vc = 24.0, feed = 0.07, doc = 0.4, rateInr = 7200.0, km = 0.032,
            isAero = true, hasHighHardness = true, headingSpm = 20,
            insertSpec = "CNMG 120408-QM (PVD AlTiN ISO S05)",
            heatTreatment = "Work Hardened Condition -> Vacuum Age 565°C–595°C (4 hrs). Immune to SCC.",
            lubricationCoating = "Copper plating + MoS2. Roll threads post-aging.",
            dieTooling = "Heavy-duty WC-15%Co Carbide + Mirror Finished ASP60.",
            rawMaterialSourcingUtsMpa = "1150 – 1300 MPa (Cold Drawn Fastener Wire)",
            postForgingTargetUtsMpa = 1930.0,
            yieldStrengthMpa = 1720.0,
            minElongationPct = 8.0,
            reductionOfAreaZPct = 25.0,
            flowStressCoeffK = 1850.0,
            strainHardeningN = 0.11,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Work Hardened Cold Drawn Matrix",
                soakingTimeMinutes = "N/A (Pre-formed in work-hardened condition)",
                quenchMedium = "Inert Argon cooling to room temperature",
                temperingOrAgingTempC = "Vacuum Aging: 565°C – 595°C",
                temperingOrAgingTimeHours = "4.0 hrs (240 mins) vacuum soak",
                targetHardnessHrc = "48 – 52 HRC (480–545 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Immune to hydrogen embrittlement & SCC per NACE MR0175",
                standardReference = "AMS 5844 / AMS 5758 / NACE MR0175"
            )
        ),
        "TI_6242" to MaterialInfo(
            key = "TI_6242",
            name = "Ti-6Al-2Sn-4Zr-2Mo (AMS 4975/4976)",
            density = 4.54, uts = 1050.0, vc = 50.0, feed = 0.11, doc = 0.7, rateInr = 4100.0, km = 0.023,
            isAero = true, hasHighHardness = true, headingSpm = 40,
            insertSpec = "VCMT 160404 Sharp Rake (PVD ISO S10)",
            heatTreatment = "Duplex Anneal: Solution 970°C (60m) -> Air Cool -> Stabilize/Age 595°C (8h).",
            lubricationCoating = "Anodize Type II + MoS2. High creep resistance to 540°C.",
            dieTooling = "Hot Work Die H13 + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "880 – 940 MPa (Annealed Wire, H2 < 125 ppm)",
            postForgingTargetUtsMpa = 1050.0,
            yieldStrengthMpa = 950.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 28.0,
            flowStressCoeffK = 1280.0,
            strainHardeningN = 0.11,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Duplex Anneal Solution: 970°C ± 5°C (<10⁻⁴ mbar vacuum)",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Circulating Argon gas cool to room temp",
                temperingOrAgingTempC = "Stabilization / Aging: 595°C ± 5°C",
                temperingOrAgingTimeHours = "8.0 hrs (480 mins) soak",
                targetHardnessHrc = "35 – 39 HRC (340–385 HV)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Vacuum outgassing verified (H2 < 125 ppm)",
                standardReference = "AMS 4975 / AMS 4976 / AMS 2801"
            )
        ),
        "AL2024" to MaterialInfo(
            key = "AL2024",
            name = "Alloy 2024-T4 / T351 (AMS 4120)",
            density = 2.78, uts = 470.0, vc = 350.0, feed = 0.25, doc = 2.0, rateInr = 380.0, km = 0.010,
            isAero = true, hasHighHardness = false, headingSpm = 160,
            insertSpec = "CCGT 120408-AL (Polished Uncoated ISO N10)",
            heatTreatment = "Solution 493°C -> Rapid Cold Water Quench (<10s) -> Natural Age (T4, 96h) or T6 (190°C, 12h).",
            lubricationCoating = "Chemical Conversion (MIL-DTL-5541) / Anodize (MIL-A-8625).",
            dieTooling = "Carbide GT20 Die + M2 Punch.",
            rawMaterialSourcingUtsMpa = "280 – 340 MPa (Condition O Annealed Wire)",
            postForgingTargetUtsMpa = 470.0,
            yieldStrengthMpa = 325.0,
            minElongationPct = 14.0,
            reductionOfAreaZPct = 40.0,
            flowStressCoeffK = 450.0,
            strainHardeningN = 0.20,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Heat Treat: 493°C ± 5°C",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Immediate Cold Water Quench (<10 sec transfer delay)",
                temperingOrAgingTempC = "T4 Natural Aging (20°C, 96 hrs) or T6 Artificial (190°C, 12 hrs)",
                temperingOrAgingTimeHours = "T4: 96 hrs natural | T6: 12 hrs artificial",
                targetHardnessHrc = "75 – 80 HRB (120–135 HBW)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not susceptible to hydrogen embrittlement",
                standardReference = "AMS 4120 / AMS 2770 / QQ-A-225/6"
            )
        ),
        "AL7075" to MaterialInfo(
            key = "AL7075",
            name = "Alloy 7075-T6 / T73 (AMS 4122)",
            density = 2.81, uts = 570.0, vc = 320.0, feed = 0.22, doc = 1.8, rateInr = 420.0, km = 0.011,
            isAero = true, hasHighHardness = false, headingSpm = 150,
            insertSpec = "CCGT 120408-AL (Polished Uncoated ISO N10)",
            heatTreatment = "Solution 470°C -> Water Quench -> Two-step overage: 105°C (6h) + 175°C (8h) for T73 SCC immunity.",
            lubricationCoating = "Hard Anodize Type III (MIL-A-8625) + PTFE Seal.",
            dieTooling = "Carbide GT20 Die + ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "320 – 380 MPa (Condition O Wire Coil)",
            postForgingTargetUtsMpa = 570.0,
            yieldStrengthMpa = 505.0,
            minElongationPct = 11.0,
            reductionOfAreaZPct = 32.0,
            flowStressCoeffK = 540.0,
            strainHardeningN = 0.16,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Heat Treat: 470°C ± 5°C",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Immediate Cold Water Quench (<10 sec transfer delay)",
                temperingOrAgingTempC = "Two-Step T73 Aging: 105°C (6h) -> 175°C (8h)",
                temperingOrAgingTimeHours = "14.0 hrs total two-stage aging",
                targetHardnessHrc = "85 – 88 HRB (150–160 HBW)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not applicable to aluminum alloys",
                standardReference = "AMS 4122 / AMS 2770 / AMS 2772"
            )
        ),
        "AL2219" to MaterialInfo(
            key = "AL2219",
            name = "Alloy 2219-T6 / T851 (AMS 4162)",
            density = 2.84, uts = 450.0, vc = 340.0, feed = 0.24, doc = 1.8, rateInr = 460.0, km = 0.011,
            isAero = true, hasHighHardness = false, headingSpm = 155,
            insertSpec = "CCGT 120408-AL (Polished Uncoated ISO N10)",
            heatTreatment = "Solution 535°C -> Cold Water Quench -> Artificial Age 175°C (18 hrs) Condition T851/T6.",
            lubricationCoating = "Chemical Film / Cryogenic Dry Film Lubricant (-253°C to +300°C).",
            dieTooling = "Carbide GT20 Die + M2 Punch.",
            rawMaterialSourcingUtsMpa = "260 – 320 MPa (Condition O Annealed Wire)",
            postForgingTargetUtsMpa = 450.0,
            yieldStrengthMpa = 350.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 35.0,
            flowStressCoeffK = 480.0,
            strainHardeningN = 0.18,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Heat Treat: 535°C ± 5°C",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Cold Water Quench (<10 sec transfer delay)",
                temperingOrAgingTempC = "Artificial Aging: 175°C ± 5°C",
                temperingOrAgingTimeHours = "18.0 hrs continuous soak",
                targetHardnessHrc = "78 – 82 HRB (130–140 HBW)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not applicable (Cryogenic flight certified)",
                standardReference = "AMS 4162 / AMS 4163 / NASA-STD-5001"
            )
        ),
        "INCONEL625" to MaterialInfo(
            key = "INCONEL625",
            name = "Inconel 625 (AMS 5666 / N06625)",
            density = 8.44, uts = 980.0, vc = 34.0, feed = 0.10, doc = 0.6, rateInr = 5200.0, km = 0.027,
            isAero = true, hasHighHardness = true, headingSpm = 35,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO S15)",
            heatTreatment = "Solution Anneal 1090°C -> Rapid Air/Water Quench.",
            lubricationCoating = "Copper + MoS2. Passivated per AMS 2700.",
            dieTooling = "Micro-grain GT55 Carbide + ASP60 Punch.",
            rawMaterialSourcingUtsMpa = "880 – 960 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 980.0,
            yieldStrengthMpa = 520.0,
            minElongationPct = 30.0,
            reductionOfAreaZPct = 40.0,
            flowStressCoeffK = 1450.0,
            strainHardeningN = 0.22,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1090°C – 1150°C (Anneal)",
                soakingTimeMinutes = "45 – 60 min",
                quenchMedium = "Rapid Air or Water Quench",
                temperingOrAgingTempC = "N/A (Solid Solution Strengthened)",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "28 – 35 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Immune to hydrogen embrittlement",
                standardReference = "AMS 5666 / ASTM B446 / UNS N06625"
            )
        ),
        "NITRONIC60" to MaterialInfo(
            key = "NITRONIC60",
            name = "Nitronic 60 (AMS 5848 / S21800)",
            density = 7.62, uts = 850.0, vc = 65.0, feed = 0.12, doc = 0.8, rateInr = 1650.0, km = 0.023,
            isAero = true, hasHighHardness = false, headingSpm = 55,
            insertSpec = "WNMG 080408-MM (PVD TiAlN ISO M15)",
            heatTreatment = "Solution Anneal 1065°C -> Rapid Water Quench.",
            lubricationCoating = "Anti-galling self-lubricating alloy, Citric Passivation.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "700 – 780 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 850.0,
            yieldStrengthMpa = 480.0,
            minElongationPct = 35.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 1180.0,
            strainHardeningN = 0.28,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1065°C ± 15°C",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Water Quench",
                temperingOrAgingTempC = "Passivation per ASTM A967",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "24 – 30 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required (Austenitic anti-galling matrix)",
                standardReference = "AMS 5848 / ASTM A276 / UNS S21800"
            )
        ),
        "HASTELLOY_C276" to MaterialInfo(
            key = "HASTELLOY_C276",
            name = "Hastelloy C-276 (AMS 5750)",
            density = 8.89, uts = 800.0, vc = 30.0, feed = 0.09, doc = 0.5, rateInr = 6200.0, km = 0.026,
            isAero = true, hasHighHardness = false, headingSpm = 30,
            insertSpec = "CNMG 120408-SM (PVD AlTiN ISO S10)",
            heatTreatment = "Solution Anneal 1120°C -> Rapid Water Quench.",
            lubricationCoating = "MoS2 Dry Lube + Passivation.",
            dieTooling = "Micro-grain Carbide + ASP60 Punch.",
            rawMaterialSourcingUtsMpa = "750 – 820 MPa",
            postForgingTargetUtsMpa = 800.0,
            yieldStrengthMpa = 410.0,
            minElongationPct = 40.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 1350.0,
            strainHardeningN = 0.25,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1120°C ± 10°C",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Rapid Water Quench",
                temperingOrAgingTempC = "N/A",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "22 – 28 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Immune to hydrogen embrittlement",
                standardReference = "AMS 5750 / ASTM B574 / UNS N10276"
            )
        ),
        "MONEL_K500" to MaterialInfo(
            key = "MONEL_K500",
            name = "Monel K-500 (QQ-N-286 / N05500)",
            density = 8.44, uts = 1100.0, vc = 45.0, feed = 0.11, doc = 0.7, rateInr = 3800.0, km = 0.025,
            isAero = true, hasHighHardness = true, headingSpm = 45,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO S15)",
            heatTreatment = "Solution 980°C -> Water Quench -> Age 580°C (16h).",
            lubricationCoating = "Copper + MoS2. Marine immersion certified.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "680 – 780 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 1100.0,
            yieldStrengthMpa = 790.0,
            minElongationPct = 20.0,
            reductionOfAreaZPct = 35.0,
            flowStressCoeffK = 1250.0,
            strainHardeningN = 0.18,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "980°C ± 10°C",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Rapid Water Quench",
                temperingOrAgingTempC = "Age: 580°C (16 hrs) furnace cool to 480°C",
                temperingOrAgingTimeHours = "16.0 hrs",
                targetHardnessHrc = "33 – 38 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required for copper-nickel alloys",
                standardReference = "QQ-N-286 / AMS 4676 / UNS N05500"
            )
        ),
        "42CRMO4" to MaterialInfo(
            key = "42CRMO4",
            name = "42CrMo4 / EN19 (ISO 898-1 10.9)",
            density = 7.85, uts = 1050.0, vc = 160.0, feed = 0.18, doc = 1.4, rateInr = 145.0, km = 0.015,
            isAero = false, hasHighHardness = true, headingSpm = 110,
            insertSpec = "CNMG 120408-PM (CVD ISO P20)",
            heatTreatment = "Austenitize 860°C -> Oil Quench -> Temper 540°C.",
            lubricationCoating = "Phosphate / Zn-Ni + 10h De-Embrittlement Baking.",
            dieTooling = "GT55 Carbide + ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "580 – 660 MPa (Spheroidized Wire)",
            postForgingTargetUtsMpa = 1050.0,
            yieldStrengthMpa = 940.0,
            minElongationPct = 11.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 820.0,
            strainHardeningN = 0.15,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "850°C – 870°C",
                soakingTimeMinutes = "45 – 60 min",
                quenchMedium = "Agitated Quench Oil (60°C)",
                temperingOrAgingTempC = "530°C – 580°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "32 – 39 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 10 to 14 hours within 2 hours post-plating",
                standardReference = "ISO 898-1 Class 10.9 / DIN EN 10083-3"
            )
        ),
        "SS316L" to MaterialInfo(
            key = "SS316L",
            name = "SS316L Low Carbon Stainless",
            density = 8.00, uts = 720.0, vc = 110.0, feed = 0.15, doc = 1.0, rateInr = 360.0, km = 0.024,
            isAero = false, hasHighHardness = false, headingSpm = 85,
            insertSpec = "CNMG 120408-SM (PVD AlTiN ISO M15)",
            heatTreatment = "Solution Anneal 1060°C + Water Quench.",
            lubricationCoating = "Citric Passivation per ASTM A967.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "520 – 600 MPa (Annealed Wire Coil)",
            postForgingTargetUtsMpa = 720.0,
            yieldStrengthMpa = 480.0,
            minElongationPct = 30.0,
            reductionOfAreaZPct = 55.0,
            flowStressCoeffK = 1080.0,
            strainHardeningN = 0.29,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1050°C – 1080°C",
                soakingTimeMinutes = "30 – 45 min",
                quenchMedium = "Rapid Water Quench",
                temperingOrAgingTempC = "Passivation: ASTM A967 Citric",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "20 – 26 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required",
                standardReference = "ISO 3506-1 Class A4-70 / ASTM F593"
            )
        ),
        "13_8PH" to MaterialInfo(
            key = "13_8PH",
            name = "13-8 Mo PH Stainless (AMS 5629)",
            density = 7.78, uts = 1520.0, vc = 70.0, feed = 0.12, doc = 0.8, rateInr = 1150.0, km = 0.021,
            isAero = true, hasHighHardness = true, headingSpm = 55,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO S15)",
            heatTreatment = "Solution 927°C -> Oil Quench -> Precipitation Age H950 (510°C, 4h).",
            lubricationCoating = "AMS 2700 Citric Passivation + Cetyl Alcohol.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "900 – 1000 MPa (Condition A Wire)",
            postForgingTargetUtsMpa = 1520.0,
            yieldStrengthMpa = 1410.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 45.0,
            flowStressCoeffK = 1300.0,
            strainHardeningN = 0.11,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Condition A Solution: 927°C ± 10°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Oil Quench below 15°C to complete martensite transformation",
                temperingOrAgingTempC = "Precipitation Age H950 (510°C ± 5°C)",
                temperingOrAgingTimeHours = "4.0 hrs soak",
                targetHardnessHrc = "45 – 48 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required for passivated finish",
                standardReference = "AMS 5629 / AMS 5864 / AMS 2759/3"
            )
        ),
        "AL6061" to MaterialInfo(
            key = "AL6061",
            name = "Alloy 6061-T6 Structural Al (AMS 4117)",
            density = 2.70, uts = 310.0, vc = 380.0, feed = 0.28, doc = 2.2, rateInr = 290.0, km = 0.009,
            isAero = false, hasHighHardness = false, headingSpm = 180,
            insertSpec = "CCGT 120408-AL (Polished ISO N10)",
            heatTreatment = "Solution 530°C -> Water Quench -> Artificial Age 160°C (18 hrs).",
            lubricationCoating = "Clear Anodize / MIL-A-8625 Type II.",
            dieTooling = "Carbide GT20 Die + M2 Punch.",
            rawMaterialSourcingUtsMpa = "180 – 220 MPa (Condition O Wire)",
            postForgingTargetUtsMpa = 310.0,
            yieldStrengthMpa = 275.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 40.0,
            flowStressCoeffK = 380.0,
            strainHardeningN = 0.19,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Heat Treat: 525°C – 535°C",
                soakingTimeMinutes = "30 min",
                quenchMedium = "Immediate Water Quench (<10 sec transfer)",
                temperingOrAgingTempC = "T6 Artificial Aging: 160°C ± 5°C",
                temperingOrAgingTimeHours = "18.0 hrs continuous soak",
                targetHardnessHrc = "60 – 65 HRB (95 HBW)",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not applicable",
                standardReference = "AMS 4117 / ASTM B211 / SAE J454"
            )
        ),
        "BRASS_C360" to MaterialInfo(
            key = "BRASS_C360",
            name = "Free-Cutting Brass C36000 (ASTM B16)",
            density = 8.50, uts = 440.0, vc = 420.0, feed = 0.30, doc = 2.5, rateInr = 520.0, km = 0.008,
            isAero = false, hasHighHardness = false, headingSpm = 200,
            insertSpec = "CCGT 120408-AL (Sharp Uncoated ISO N05)",
            heatTreatment = "Stress Relieve 280°C (1h) if cold worked.",
            lubricationCoating = "Bare / Nickel Plated.",
            dieTooling = "Steel D2 / M2 Die.",
            rawMaterialSourcingUtsMpa = "380 – 420 MPa (Half-Hard Rod)",
            postForgingTargetUtsMpa = 440.0,
            yieldStrengthMpa = 310.0,
            minElongationPct = 15.0,
            reductionOfAreaZPct = 45.0,
            flowStressCoeffK = 520.0,
            strainHardeningN = 0.12,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Stress Relief: 260°C – 290°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Air Cool",
                temperingOrAgingTempC = "N/A",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "65 – 75 HRB",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not applicable",
                standardReference = "ASTM B16 / SAE J463"
            )
        ),
        "TI_GR23" to MaterialInfo(
            key = "TI_GR23",
            name = "Titanium Gr 23 Ti-6Al-4V ELI (AMS 4930)",
            density = 4.43, uts = 1020.0, vc = 65.0, feed = 0.12, doc = 0.8, rateInr = 3800.0, km = 0.021,
            isAero = true, hasHighHardness = true, headingSpm = 50,
            insertSpec = "VCMT 160404 Sharp Rake (PVD ISO S10)",
            heatTreatment = "Vacuum Solution 930°C + Age 540°C (6h). Superior damage tolerance & cryogenic toughness.",
            lubricationCoating = "Anodize Type II + MoS2 Dry Film.",
            dieTooling = "H13 Nitrided Die + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "860 – 920 MPa (ELI Annealed Wire, H2 < 80 ppm)",
            postForgingTargetUtsMpa = 1020.0,
            yieldStrengthMpa = 950.0,
            minElongationPct = 15.0,
            reductionOfAreaZPct = 35.0,
            flowStressCoeffK = 1300.0,
            strainHardeningN = 0.10,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Vacuum Solution: 925°C – 940°C (<10⁻⁴ mbar)",
                soakingTimeMinutes = "45 min",
                quenchMedium = "High-pressure Argon Gas Quench",
                temperingOrAgingTempC = "Precipitation Age: 540°C ± 5°C",
                temperingOrAgingTimeHours = "6.0 hrs vacuum soak",
                targetHardnessHrc = "34 – 38 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Verified H2 < 80 ppm",
                standardReference = "AMS 4930 / ASTM F136 / AMS 2801"
            )
        ),
        "CUBE2" to MaterialInfo(
            key = "CUBE2",
            name = "Beryllium Copper CuBe2 / C17200 (AMS 4650)",
            density = 8.25, uts = 1280.0, vc = 90.0, feed = 0.14, doc = 0.8, rateInr = 4200.0, km = 0.022,
            isAero = true, hasHighHardness = true, headingSpm = 65,
            insertSpec = "CCGT 120408 (Uncoated ISO N10)",
            heatTreatment = "Solution 800°C -> Water Quench -> Precipitation Age 315°C (3 hrs) to TH04 condition.",
            lubricationCoating = "Silver / Cadmium / Passivation. Non-sparking & non-magnetic.",
            dieTooling = "GT55 Carbide + ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "450 – 550 MPa (Condition A Wire)",
            postForgingTargetUtsMpa = 1280.0,
            yieldStrengthMpa = 1100.0,
            minElongationPct = 8.0,
            reductionOfAreaZPct = 25.0,
            flowStressCoeffK = 1200.0,
            strainHardeningN = 0.14,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Heat Treat: 780°C – 800°C",
                soakingTimeMinutes = "30 min",
                quenchMedium = "Rapid Water Quench",
                temperingOrAgingTempC = "Precipitation Age: 315°C ± 5°C",
                temperingOrAgingTimeHours = "3.0 hrs soak",
                targetHardnessHrc = "38 – 44 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not applicable",
                standardReference = "AMS 4650 / ASTM B196 / UNS C17200"
            )
        ),
        "EN24" to MaterialInfo(
            key = "EN24",
            name = "EN24 / 34CrNiMo6 (BS 970 817M40)",
            density = 7.85, uts = 1050.0, vc = 160.0, feed = 0.20, doc = 1.5, rateInr = 165.0, km = 0.016,
            isAero = false, hasHighHardness = true, headingSpm = 110,
            insertSpec = "CNMG 120408-PR (CVD ISO P20)",
            heatTreatment = "Oil Quench 840°C -> Temper 540–620°C (T to V Condition).",
            lubricationCoating = "Phosphate + Zn-Ni or Electro-Zinc.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "620 – 700 MPa (Spheroidized Annealed Wire)",
            postForgingTargetUtsMpa = 950.0,
            yieldStrengthMpa = 850.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 980.0,
            strainHardeningN = 0.18,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "840°C – 860°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Agitated Quench Oil (60°C)",
                temperingOrAgingTempC = "540°C – 620°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "28 – 36 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "200°C for 4 to 8 hours within 2 hours post-plating",
                standardReference = "BS 970 817M40 / ISO 898-1 Class 10.9"
            )
        ),
        "EN24_ESR" to MaterialInfo(
            key = "EN24_ESR",
            name = "EN24 ESR Pure (Electro-Slag Remelted 34CrNiMo6)",
            density = 7.85, uts = 1150.0, vc = 150.0, feed = 0.18, doc = 1.2, rateInr = 380.0, km = 0.016,
            isAero = true, hasHighHardness = true, headingSpm = 100,
            insertSpec = "CNMG 120408-PR (CVD ISO P15)",
            heatTreatment = "Vacuum/Oil Quench 840°C -> Temper 520–600°C (High Fatigue Aero).",
            lubricationCoating = "LHE Cadmium / IVD Aluminum + Bake.",
            dieTooling = "WC-15%Co Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "600 – 680 MPa (Ultra-Clean ESR Wire)",
            postForgingTargetUtsMpa = 1050.0,
            yieldStrengthMpa = 950.0,
            minElongationPct = 14.0,
            reductionOfAreaZPct = 55.0,
            flowStressCoeffK = 1050.0,
            strainHardeningN = 0.17,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "830°C – 850°C Vacuum",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Vacuum Oil Quench",
                temperingOrAgingTempC = "520°C – 600°C",
                temperingOrAgingTimeHours = "2.0 hrs + 2.0 hrs",
                targetHardnessHrc = "34 – 40 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "200°C – 220°C for 8 to 12 hours within 4 hours post-plating",
                standardReference = "EN24 ESR / AMS 6414"
            )
        ),
        "30KHGSA" to MaterialInfo(
            key = "30KHGSA",
            name = "30KHGSA (GOST 4543 30ХГСА Aero Steel)",
            density = 7.85, uts = 1220.0, vc = 140.0, feed = 0.18, doc = 1.2, rateInr = 290.0, km = 0.018,
            isAero = true, hasHighHardness = true, headingSpm = 95,
            insertSpec = "CNMG 120408-PR (CVD ISO P15)",
            heatTreatment = "Austenitize 890°C -> Isothermal/Oil Quench -> Temper 240–500°C.",
            lubricationCoating = "LHE Cadmium / Zinc + 12–24h Baking.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "580 – 650 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 1080.0,
            yieldStrengthMpa = 980.0,
            minElongationPct = 11.0,
            reductionOfAreaZPct = 48.0,
            flowStressCoeffK = 1120.0,
            strainHardeningN = 0.16,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "880°C – 900°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Isothermal / Agitated Oil Quench",
                temperingOrAgingTempC = "240°C – 500°C",
                temperingOrAgingTimeHours = "2.5 hrs",
                targetHardnessHrc = "35 – 42 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "195°C for 12 to 24 hours after LHE Cadmium/Zinc plating",
                standardReference = "GOST 4543 30ХГСА"
            )
        ),
        "16KHGSN" to MaterialInfo(
            key = "16KHGSN",
            name = "16KHGSN (GOST 16ХГСН Russian Aero Steel)",
            density = 7.85, uts = 1060.0, vc = 155.0, feed = 0.20, doc = 1.4, rateInr = 260.0, km = 0.016,
            isAero = true, hasHighHardness = true, headingSpm = 105,
            insertSpec = "CNMG 120408-PR (CVD ISO P20)",
            heatTreatment = "Oil Quench 870°C -> Temper 220–450°C.",
            lubricationCoating = "Electro-Cadmium or Zinc-Nickel.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "560 – 650 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 950.0,
            yieldStrengthMpa = 850.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 990.0,
            strainHardeningN = 0.18,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "860°C – 880°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Oil Quench",
                temperingOrAgingTempC = "220°C – 450°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "30 – 38 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "200°C for 4 to 8 hours post-plating",
                standardReference = "GOST 16ХГСН"
            )
        ),
        "35NCD16" to MaterialInfo(
            key = "35NCD16",
            name = "35NCD16 / EN 36NiCrMo16 (AFNOR Aero High-Strength)",
            density = 7.85, uts = 1350.0, vc = 130.0, feed = 0.16, doc = 1.0, rateInr = 420.0, km = 0.018,
            isAero = true, hasHighHardness = true, headingSpm = 85,
            insertSpec = "CNMG 120408-PR (CVD ISO P10)",
            heatTreatment = "Air/Oil Quench 850°C -> Temper 220–550°C (Ultra-High Strength).",
            lubricationCoating = "Cadmium Plating / IVD Aluminum + 12–24h Baking.",
            dieTooling = "WC-15%Co Carbide + ASP30 AlCrN Punch.",
            rawMaterialSourcingUtsMpa = "680 – 760 MPa (Soft Annealed Wire)",
            postForgingTargetUtsMpa = 1250.0,
            yieldStrengthMpa = 1100.0,
            minElongationPct = 10.0,
            reductionOfAreaZPct = 45.0,
            flowStressCoeffK = 1250.0,
            strainHardeningN = 0.15,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "840°C – 860°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Agitated Air / Oil Quench",
                temperingOrAgingTempC = "220°C – 550°C",
                temperingOrAgingTimeHours = "2.0 hrs + 2.0 hrs",
                targetHardnessHrc = "38 – 44 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C – 200°C for 12 to 24 hours within 1 hour post-plating",
                standardReference = "AFNOR 35NCD16 / AIR 9160 / EN 36NiCrMo16"
            )
        ),
        "VT16" to MaterialInfo(
            key = "VT16",
            name = "VT-16 Titanium Alloy (GOST ВТ16 Ti-3Al-5Mo-4.5V)",
            density = 4.50, uts = 1120.0, vc = 65.0, feed = 0.12, doc = 0.8, rateInr = 3800.0, km = 0.021,
            isAero = true, hasHighHardness = true, headingSpm = 80,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO M15)",
            heatTreatment = "Quench 810°C -> Vacuum Age 500–520°C (8–12 hrs).",
            lubricationCoating = "Anodized / Passivated Titanium Alloy (Exempt from H2 Baking).",
            dieTooling = "WC-15%Co Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "750 – 850 MPa (Annealed α+β Wire)",
            postForgingTargetUtsMpa = 1030.0,
            yieldStrengthMpa = 950.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 1180.0,
            strainHardeningN = 0.12,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "800°C – 820°C Solution",
                soakingTimeMinutes = "40 min",
                quenchMedium = "Water Quench",
                temperingOrAgingTempC = "500°C – 520°C Vacuum Age",
                temperingOrAgingTimeHours = "8.0 to 12.0 hrs",
                targetHardnessHrc = "32 – 38 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Exempt (Anodized / Passivated)",
                standardReference = "GOST ВТ16 / OST 1 90013"
            )
        ),
        "14KH17N2" to MaterialInfo(
            key = "14KH17N2",
            name = "14KH17N2 / AISI 431 Stainless (GOST 5632 14Х17Н2)",
            density = 7.75, uts = 1050.0, vc = 140.0, feed = 0.18, doc = 1.2, rateInr = 310.0, km = 0.020,
            isAero = true, hasHighHardness = true, headingSpm = 100,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO M15)",
            heatTreatment = "Oil Quench 1000°C -> Temper 280–600°C (Martensitic Q&T).",
            lubricationCoating = "Nitric Passivation / Electro-Cadmium.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "680 – 780 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 950.0,
            yieldStrengthMpa = 800.0,
            minElongationPct = 13.0,
            reductionOfAreaZPct = 48.0,
            flowStressCoeffK = 1020.0,
            strainHardeningN = 0.17,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "980°C – 1020°C",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Oil Quench",
                temperingOrAgingTempC = "280°C – 600°C (Avoid 400–525°C embrittlement)",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "30 – 38 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "200°C for 4 to 8 hours if electroplated",
                standardReference = "GOST 5632 14Х17Н2 / AISI 431"
            )
        ),
        "12KH18N10T" to MaterialInfo(
            key = "12KH18N10T",
            name = "12KH18N10T / AISI 321 Stainless (GOST 5632 Ti-Stabilized)",
            density = 7.90, uts = 760.0, vc = 160.0, feed = 0.20, doc = 1.5, rateInr = 340.0, km = 0.024,
            isAero = true, hasHighHardness = false, headingSpm = 120,
            insertSpec = "CNMG 120408-MF (PVD TiAlN ISO M10)",
            heatTreatment = "Cold Forge + Thread Roll -> Nitric Passivation (Ti-Stabilized).",
            lubricationCoating = "Nitric Passivation per AMS 2700.",
            dieTooling = "GT20 Carbide + ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "510 – 620 MPa (Solution Annealed)",
            postForgingTargetUtsMpa = 680.0,
            yieldStrengthMpa = 420.0,
            minElongationPct = 35.0,
            reductionOfAreaZPct = 60.0,
            flowStressCoeffK = 1150.0,
            strainHardeningN = 0.42,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1020°C – 1080°C Solution Annealed",
                soakingTimeMinutes = "30 min",
                quenchMedium = "Water Quench",
                temperingOrAgingTempC = "N/A (Work-Hardened)",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "20 – 28 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Exempt (Austenitic Stainless)",
                standardReference = "GOST 5632 12Х18Н10Т / AISI 321"
            )
        ),
        "AISI_51B37" to MaterialInfo(
            key = "AISI_51B37",
            name = "AISI 51B37 / EN 38CrB4 Boron Steel",
            density = 7.85, uts = 1120.0, vc = 175.0, feed = 0.22, doc = 1.6, rateInr = 125.0, km = 0.015,
            isAero = false, hasHighHardness = true, headingSpm = 130,
            insertSpec = "CNMG 120408-PR (CVD ISO P20)",
            heatTreatment = "Oil Quench 860°C -> Temper 450–500°C (ISO Class 10.9).",
            lubricationCoating = "Phosphate + Soap / Electro-Zinc + Bake.",
            dieTooling = "GT20 Carbide + ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "540 – 620 MPa (Spheroidized Wire)",
            postForgingTargetUtsMpa = 1040.0,
            yieldStrengthMpa = 940.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 52.0,
            flowStressCoeffK = 960.0,
            strainHardeningN = 0.19,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "850°C – 870°C",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Agitated Oil Quench",
                temperingOrAgingTempC = "450°C – 500°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "32 – 39 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "200°C for 4 to 8 hours per ISO 4042",
                standardReference = "SAE 51B37 / EN 38CrB4 / ISO 898-1 Class 10.9"
            )
        ),
        "AISI_4130" to MaterialInfo(
            key = "AISI_4130",
            name = "AISI 4130 / EN 25CrMo4 (AMS 6370)",
            density = 7.85, uts = 950.0, vc = 180.0, feed = 0.22, doc = 1.6, rateInr = 140.0, km = 0.015,
            isAero = true, hasHighHardness = false, headingSpm = 135,
            insertSpec = "CNMG 120408-PM (CVD ISO P20)",
            heatTreatment = "Oil Quench 860°C -> Temper 480–560°C.",
            lubricationCoating = "Phosphate / Cadmium Plating.",
            dieTooling = "GT20 Carbide + ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "540 – 620 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 850.0,
            yieldStrengthMpa = 750.0,
            minElongationPct = 15.0,
            reductionOfAreaZPct = 55.0,
            flowStressCoeffK = 910.0,
            strainHardeningN = 0.20,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "850°C – 870°C",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Oil Quench",
                temperingOrAgingTempC = "480°C – 560°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "26 – 34 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "Mandatory only if tempered >1000 MPa & Electroplated",
                standardReference = "AMS 6370 / EN 25CrMo4 / MIL-S-6758"
            )
        ),
        "INCONEL_600" to MaterialInfo(
            key = "INCONEL_600",
            name = "Inconel 600 / UNS N06600 (AMS 5665 Ni-Cr-Fe)",
            density = 8.47, uts = 850.0, vc = 90.0, feed = 0.14, doc = 0.9, rateInr = 2800.0, km = 0.025,
            isAero = true, hasHighHardness = false, headingSpm = 80,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO M15)",
            heatTreatment = "Stress Relieve 480°C (Solid-Solution Nickel Alloy).",
            lubricationCoating = "Oxalate Lube + Passivation.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "550 – 680 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 750.0,
            yieldStrengthMpa = 550.0,
            minElongationPct = 30.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 1300.0,
            strainHardeningN = 0.38,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1010°C – 1050°C Solution",
                soakingTimeMinutes = "30 min",
                quenchMedium = "Air / Water Quench",
                temperingOrAgingTempC = "480°C Stress Relief",
                temperingOrAgingTimeHours = "1.0 hr",
                targetHardnessHrc = "22 – 30 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Exempt (Solid-solution Ni-Cr-Fe alloy)",
                standardReference = "AMS 5665 / UNS N06600"
            )
        ),
        "20X13" to MaterialInfo(
            key = "20X13",
            name = "20X13 / 20Kh13 / AISI 420 Martensitic (GOST 5632)",
            density = 7.75, uts = 850.0, vc = 160.0, feed = 0.20, doc = 1.5, rateInr = 220.0, km = 0.019,
            isAero = false, hasHighHardness = false, headingSpm = 115,
            insertSpec = "CNMG 120408-PM (CVD ISO P20)",
            heatTreatment = "Oil Quench 1000°C -> Temper 550–650°C.",
            lubricationCoating = "Passivation / Zinc Plating.",
            dieTooling = "GT20 Carbide + ASP23 Punch.",
            rawMaterialSourcingUtsMpa = "500 – 600 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 750.0,
            yieldStrengthMpa = 600.0,
            minElongationPct = 16.0,
            reductionOfAreaZPct = 55.0,
            flowStressCoeffK = 880.0,
            strainHardeningN = 0.21,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "980°C – 1020°C",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Oil Quench",
                temperingOrAgingTempC = "550°C – 650°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "24 – 32 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required below 1000 MPa with standard passivation",
                standardReference = "GOST 5632 20Х13 / AISI 420"
            )
        ),
        "30X13" to MaterialInfo(
            key = "30X13",
            name = "30X13 / 30Kh13 / AISI 420B Martensitic (GOST 5632)",
            density = 7.75, uts = 1060.0, vc = 145.0, feed = 0.18, doc = 1.2, rateInr = 240.0, km = 0.020,
            isAero = true, hasHighHardness = true, headingSpm = 105,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO M15)",
            heatTreatment = "Oil Quench 1010°C -> Temper 250–550°C.",
            lubricationCoating = "Passivation / Cadmium Plating.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "560 – 660 MPa (Annealed Wire)",
            postForgingTargetUtsMpa = 950.0,
            yieldStrengthMpa = 800.0,
            minElongationPct = 12.0,
            reductionOfAreaZPct = 48.0,
            flowStressCoeffK = 980.0,
            strainHardeningN = 0.18,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1000°C – 1020°C",
                soakingTimeMinutes = "45 min",
                quenchMedium = "Oil Quench",
                temperingOrAgingTempC = "250°C – 550°C",
                temperingOrAgingTimeHours = "2.0 hrs",
                targetHardnessHrc = "32 – 40 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "Mandatory if >1000 MPa (32 HRC) and acid pickled/plated",
                standardReference = "GOST 5632 30Х13 / AISI 420B"
            )
        ),
        "10KH11N23T3MR" to MaterialInfo(
            key = "10KH11N23T3MR",
            name = "10KH11N23T3MR / EP33 (GOST ЭП33 Aero Superalloy)",
            density = 7.95, uts = 1200.0, vc = 80.0, feed = 0.12, doc = 0.8, rateInr = 3900.0, km = 0.026,
            isAero = true, hasHighHardness = true, headingSpm = 75,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO M10)",
            heatTreatment = "Quench 1100°C -> Double Age 750°C (16h) + 650°C (10h).",
            lubricationCoating = "Passivation per AMS 2700 (Exempt from H2 Baking).",
            dieTooling = "WC-15%Co Carbide + ASP30 AlCrN Punch.",
            rawMaterialSourcingUtsMpa = "680 – 780 MPa (Sol. Annealed Wire)",
            postForgingTargetUtsMpa = 1080.0,
            yieldStrengthMpa = 820.0,
            minElongationPct = 15.0,
            reductionOfAreaZPct = 45.0,
            flowStressCoeffK = 1350.0,
            strainHardeningN = 0.32,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1080°C – 1110°C Solution",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Oil / Water Quench",
                temperingOrAgingTempC = "750°C (16h) + 650°C (10h) Double Age",
                temperingOrAgingTimeHours = "26.0 hrs total aging",
                targetHardnessHrc = "34 – 42 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Exempt under Passivation (Austenitic Fe-Ni-Cr-Ti-Mo Superalloy)",
                standardReference = "GOST ЭП33 / OST 1 90005"
            )
        )
    )

    /** Base catalogue plus the researched automotive / aerospace grades (see ResearchedMaterials). */
    val DEFAULT_MATERIALS: Map<String, MaterialInfo> =
        (BASE_MATERIALS + ResearchedMaterials.extend(BASE_MATERIALS)).let { it + ExtendedMaterials.extend(it) }

    // Observable Compose state map with all 38+ default alloys + dynamic custom additions
    val MATERIALS = androidx.compose.runtime.mutableStateMapOf<String, MaterialInfo>().apply {
        putAll(DEFAULT_MATERIALS)
    }

    fun addMaterial(material: MaterialInfo) {
        MATERIALS[material.key] = material
    }

    fun getAvailableCustomCandidates(): List<MaterialInfo> = listOf(
        MaterialInfo(
            key = "INVAR36",
            name = "Invar 36 Low Thermal Expansion (AMS 2315)",
            density = 8.05, uts = 620.0, vc = 110.0, feed = 0.16, doc = 1.0, rateInr = 2900.0, km = 0.020,
            isAero = true, hasHighHardness = false, headingSpm = 75,
            insertSpec = "CNMG 120408-SM (PVD TiAlN ISO M15)",
            heatTreatment = "Stress Relieve 315°C (1h) or Solution 830°C + Water Quench.",
            lubricationCoating = "Passivation per AMS 2700.",
            dieTooling = "GT55 Carbide + ASP30 Punch.",
            rawMaterialSourcingUtsMpa = "490 – 550 MPa",
            postForgingTargetUtsMpa = 620.0,
            yieldStrengthMpa = 380.0,
            minElongationPct = 30.0,
            reductionOfAreaZPct = 50.0,
            flowStressCoeffK = 920.0,
            strainHardeningN = 0.22,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Stress Relief: 315°C ± 10°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Air Cool",
                temperingOrAgingTempC = "N/A",
                temperingOrAgingTimeHours = "N/A",
                targetHardnessHrc = "18 – 24 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required",
                standardReference = "AMS 2315 / ASTM F1684"
            )
        ),
        MaterialInfo(
            key = "MARAGING300",
            name = "Maraging 300 / Vascomax 300 (AMS 6514)",
            density = 8.00, uts = 2050.0, vc = 55.0, feed = 0.10, doc = 0.6, rateInr = 680.0, km = 0.025,
            isAero = true, hasHighHardness = true, headingSpm = 45,
            insertSpec = "CNMG 120408-SM (PVD ISO S10)",
            heatTreatment = "Solution 815°C -> Air Cool -> Age 480°C (6h) to 55-60 HRC.",
            lubricationCoating = "Cadmium / IVD Al + Baking.",
            dieTooling = "Micro-grain WC-15%Co + ASP60 Punch.",
            rawMaterialSourcingUtsMpa = "950 – 1100 MPa (Condition A Wire)",
            postForgingTargetUtsMpa = 2050.0,
            yieldStrengthMpa = 2000.0,
            minElongationPct = 8.0,
            reductionOfAreaZPct = 40.0,
            flowStressCoeffK = 1450.0,
            strainHardeningN = 0.08,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "Solution Anneal: 815°C ± 10°C",
                soakingTimeMinutes = "60 min",
                quenchMedium = "Air Cool to room temperature",
                temperingOrAgingTempC = "Maraging Age: 480°C ± 5°C",
                temperingOrAgingTimeHours = "6.0 hrs continuous soak",
                targetHardnessHrc = "55 – 60 HRC",
                hydrogenBakingRequired = true,
                hydrogenBakingCycle = "190°C for 24 hours post-plating",
                standardReference = "AMS 6514 / MIL-S-46850"
            )
        ),
        MaterialInfo(
            key = "NIMONIC80A",
            name = "Nimonic 80A High-Temp Superalloy (BS HR 1)",
            density = 8.19, uts = 1250.0, vc = 35.0, feed = 0.10, doc = 0.6, rateInr = 4800.0, km = 0.027,
            isAero = true, hasHighHardness = true, headingSpm = 35,
            insertSpec = "CNMG 120408-QM (PVD AlTiN ISO S05)",
            heatTreatment = "Solution 1080°C (8h) -> Air Cool -> Precipitation Age 700°C (16h).",
            lubricationCoating = "Copper Plate + MoS2. Service to 815°C.",
            dieTooling = "GT55 Carbide + ASP60 Punch.",
            rawMaterialSourcingUtsMpa = "780 – 880 MPa",
            postForgingTargetUtsMpa = 1250.0,
            yieldStrengthMpa = 820.0,
            minElongationPct = 20.0,
            reductionOfAreaZPct = 25.0,
            flowStressCoeffK = 1500.0,
            strainHardeningN = 0.16,
            heatTreatmentSpec = HeatTreatmentSpec(
                austenitizingOrSolutionTempC = "1080°C ± 10°C",
                soakingTimeMinutes = "480 min (8 hrs)",
                quenchMedium = "Air Cool",
                temperingOrAgingTempC = "Precipitation Age: 700°C ± 5°C",
                temperingOrAgingTimeHours = "16.0 hrs soak",
                targetHardnessHrc = "34 – 40 HRC",
                hydrogenBakingRequired = false,
                hydrogenBakingCycle = "Not required",
                standardReference = "BS HR 1 / AECMA PrEN 2178"
            )
        )
    )


    // ISO 261 general-purpose sizes: coarse pitch first, then fine pitches (Modulus Metal / induspecs ISO 261 charts)
    val METRIC_SIZES = listOf(
        MetricSize("M0.25 (0.25 mm)", 0.25, listOf(0.075)),
        MetricSize("M0.5 (0.50 mm)", 0.50, listOf(0.125)),
        MetricSize("M1.0 (1.00 mm)", 1.00, listOf(0.25, 0.20)),
        MetricSize("M1.2 (1.20 mm)", 1.20, listOf(0.25, 0.20)),
        MetricSize("M1.4 (1.40 mm)", 1.40, listOf(0.30, 0.20)),
        MetricSize("M1.6 (1.60 mm)", 1.60, listOf(0.35, 0.20)),
        MetricSize("M2.0 (2.00 mm)", 2.00, listOf(0.40, 0.25)),
        MetricSize("M2.5 (2.50 mm)", 2.50, listOf(0.45, 0.35)),
        MetricSize("M3 (3.00 mm)", 3.00, listOf(0.50, 0.35)),
        MetricSize("M3.5 (3.50 mm)", 3.50, listOf(0.60, 0.35)),
        MetricSize("M4 (4.00 mm)", 4.00, listOf(0.70, 0.50)),
        MetricSize("M5 (5.00 mm)", 5.00, listOf(0.80, 0.50)),
        MetricSize("M6 (6.00 mm)", 6.00, listOf(1.00, 0.75, 0.50)),
        MetricSize("M8 (8.00 mm)", 8.00, listOf(1.25, 1.00, 0.75)),
        MetricSize("M10 (10.00 mm)", 10.00, listOf(1.50, 1.25, 1.00, 0.75)),
        MetricSize("M12 (12.00 mm)", 12.00, listOf(1.75, 1.50, 1.25, 1.00)),
        MetricSize("M14 (14.00 mm)", 14.00, listOf(2.00, 1.50, 1.25, 1.00)),
        MetricSize("M16 (16.00 mm)", 16.00, listOf(2.00, 1.50, 1.00)),
        MetricSize("M18 (18.00 mm)", 18.00, listOf(2.50, 2.00, 1.50, 1.00)),
        MetricSize("M20 (20.00 mm)", 20.00, listOf(2.50, 2.00, 1.50, 1.00)),
        MetricSize("M22 (22.00 mm)", 22.00, listOf(2.50, 2.00, 1.50, 1.00)),
        MetricSize("M24 (24.00 mm)", 24.00, listOf(3.00, 2.00, 1.50, 1.00)),
        MetricSize("M27 (27.00 mm)", 27.00, listOf(3.00, 2.00, 1.50, 1.00)),
        MetricSize("M30 (30.00 mm)", 30.00, listOf(3.50, 2.00, 1.50, 1.00)),
        MetricSize("M33 (33.00 mm)", 33.00, listOf(3.50, 2.00, 1.50)),
        MetricSize("M36 (36.00 mm)", 36.00, listOf(4.00, 3.00, 2.00, 1.50)),
        MetricSize("M39 (39.00 mm)", 39.00, listOf(4.00, 3.00, 2.00, 1.50)),
        MetricSize("M42 (42.00 mm)", 42.00, listOf(4.50, 3.00, 2.00, 1.50)),
        MetricSize("M45 (45.00 mm)", 45.00, listOf(4.50, 3.00, 2.00, 1.50)),
        MetricSize("M48 (48.00 mm)", 48.00, listOf(5.00, 3.00, 2.00, 1.50)),
        MetricSize("M52 (52.00 mm)", 52.00, listOf(5.00, 4.00, 3.00, 2.00, 1.50)),
        MetricSize("M56 (56.00 mm)", 56.00, listOf(5.50, 4.00, 3.00, 2.00, 1.50)),
        MetricSize("M60 (60.00 mm)", 60.00, listOf(5.50, 4.00, 3.00, 2.00, 1.50)),
        MetricSize("M64 (64.00 mm)", 64.00, listOf(6.00, 4.00, 3.00, 2.00, 1.50))
    )

    // ASME B1.1 Unified sizes: UNC first, then UNF. 2" (4.5 TPI) is left out — TPI is stored as a whole number.
    val INCH_SIZES = listOf(
        InchSize("#0 (0.0600\")", 0.0600, listOf(80), hasUnc = false),
        InchSize("#1 (0.0730\")", 0.0730, listOf(64, 72)),
        InchSize("#2 (0.0860\")", 0.0860, listOf(56, 64)),
        InchSize("#3 (0.0990\")", 0.0990, listOf(48, 56)),
        InchSize("#4 (0.1120\")", 0.1120, listOf(40, 48)),
        InchSize("#5 (0.1250\")", 0.1250, listOf(40, 44)),
        InchSize("#6 (0.1380\")", 0.1380, listOf(32, 40)),
        InchSize("#8 (0.1640\")", 0.1640, listOf(32, 36)),
        InchSize("#10 (0.1900\")", 0.1900, listOf(24, 32)),
        InchSize("#12 (0.2160\")", 0.2160, listOf(24, 28)),
        InchSize("1/4\" (0.2500\")", 0.2500, listOf(20, 28)),
        InchSize("5/16\" (0.3125\")", 0.3125, listOf(18, 24)),
        InchSize("3/8\" (0.3750\")", 0.3750, listOf(16, 24)),
        InchSize("7/16\" (0.4375\")", 0.4375, listOf(14, 20)),
        InchSize("1/2\" (0.5000\")", 0.5000, listOf(13, 20)),
        InchSize("9/16\" (0.5625\")", 0.5625, listOf(12, 18)),
        InchSize("5/8\" (0.6250\")", 0.6250, listOf(11, 18)),
        InchSize("3/4\" (0.7500\")", 0.7500, listOf(10, 16)),
        InchSize("7/8\" (0.8750\")", 0.8750, listOf(9, 14)),
        InchSize("1\" (1.0000\")", 1.0000, listOf(8, 12, 14)),
        InchSize("1-1/8\" (1.1250\")", 1.1250, listOf(7, 12)),
        InchSize("1-1/4\" (1.2500\")", 1.2500, listOf(7, 12)),
        InchSize("1-3/8\" (1.3750\")", 1.3750, listOf(6, 12)),
        InchSize("1-1/2\" (1.5000\")", 1.5000, listOf(6, 12)),
        InchSize("1-3/4\" (1.7500\")", 1.7500, listOf(5))
    )

    /** Saved sessions stored the size as a list position; these are the lists those positions referred to. */
    private val LEGACY_METRIC_D = listOf(0.25, 0.5, 1.0, 1.6, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 20.0, 24.0, 30.0, 36.0)
    private val LEGACY_INCH_D = listOf(0.073, 0.086, 0.112, 0.138, 0.164, 0.19, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0)

    /** Nominal diameter in mm of a size position. */
    fun diameterMm(unit: UnitSystem, index: Int): Double =
        if (unit == UnitSystem.METRIC) METRIC_SIZES.getOrElse(index) { METRIC_SIZES[defaultSizeIndex(unit)] }.d
        else INCH_SIZES.getOrElse(index) { INCH_SIZES[defaultSizeIndex(unit)] }.dInch * 25.4

    /** Position of the size closest to a diameter in mm. */
    fun sizeIndexFor(unit: UnitSystem, dMm: Double): Int =
        if (unit == UnitSystem.METRIC) METRIC_SIZES.indices.minByOrNull { kotlin.math.abs(METRIC_SIZES[it].d - dMm) } ?: 0
        else INCH_SIZES.indices.minByOrNull { kotlin.math.abs(INCH_SIZES[it].dInch * 25.4 - dMm) } ?: 0

    /** M10 or 3/8". */
    fun defaultSizeIndex(unit: UnitSystem): Int = sizeIndexFor(unit, if (unit == UnitSystem.METRIC) 10.0 else 9.525)

    /** Maps a position saved by an older version (before sizes were inserted) onto today's list. */
    fun migrateLegacySizeIndex(unit: UnitSystem, legacyIndex: Int): Int {
        val d = if (unit == UnitSystem.METRIC) LEGACY_METRIC_D.getOrNull(legacyIndex)
        else LEGACY_INCH_D.getOrNull(legacyIndex)?.times(25.4)
        return if (d == null) defaultSizeIndex(unit) else sizeIndexFor(unit, d)
    }

    /** Nominal lengths of ISO 4014 / 4017 / 4762 (2 mm for ISO 4017 M1.6 up to 500 mm for ISO 4014 M64). */
    val METRIC_LENGTHS = listOf(
        2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 16.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0,
        80.0, 90.0, 100.0, 110.0, 120.0, 130.0, 140.0, 150.0, 160.0, 180.0, 200.0, 220.0, 240.0, 260.0, 280.0, 300.0,
        320.0, 340.0, 360.0, 380.0, 400.0, 420.0, 440.0, 460.0, 480.0, 500.0
    )

    /** Nominal lengths of ASME B18.2.1 / B18.3 / B18.6.3, in mm (1/8" for #0 machine screws up to 8"). */
    val INCH_LENGTHS = listOf(
        0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.5, 1.75,
        2.0, 2.25, 2.5, 2.75, 3.0, 3.25, 3.5, 3.75, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0
    ).map { it * 25.4 }
}

object ThreadClassCatalog {
    // ISO 965-1 / 965-3 classes: pitch-diameter tolerance, then crest tolerance (e.g. 4g6g). First entry is the default.
    val METRIC_EXTERNAL = listOf(
        "6g", "3h4h", "4g", "4h", "4g6g", "4h6h", "5g6g", "5h4h", "5h6h",
        "6d", "6e", "6f", "6h", "7e6e", "7g6g", "7h6h", "8g", "8h", "9g8g",
        "4h6h-MJ"
    )

    val METRIC_INTERNAL = listOf(
        "6H", "4H", "4H5H", "5G", "5H", "5H6H", "6G", "7G", "7H", "8G", "8H",
        "6AZ", "6AX", "4H5H-MJ", "5H6H-MJ"
    )

    val INCH_EXTERNAL = listOf(
        "2A",
        "3A",
        "3A-UNJ",
        "1A"
    )

    val INCH_INTERNAL = listOf(
        "2B",
        "3B",
        "3B-UNJ",
        "1B"
    )

    fun getOptions(unitSystem: UnitSystem, isNut: Boolean): List<String> {
        return if (unitSystem == UnitSystem.METRIC) {
            if (isNut) METRIC_INTERNAL else METRIC_EXTERNAL
        } else {
            if (isNut) INCH_INTERNAL else INCH_EXTERNAL
        }
    }

    fun getLabel(code: String): String {
        return when (code) {
            "6g" -> "6g (Standard Medium Fit - ISO 965)"
            "4g6g" -> "4g6g (Aerospace / High-Precision Fit)"
            "4h" -> "4h (Zero-Allowance Ultra-Close Fit)"
            "6h" -> "6h (Zero-Allowance Medium Fit)"
            "6e" -> "6e (Large Allowance for Thick Coatings)"
            "6f" -> "6f (Moderate Allowance for Electroplating)"
            "8g" -> "8g (Loose / Commercial Fit)"
            "4g" -> "4g (Close Pitch & Major Fit)"
            "5g6g" -> "5g6g (Precision Machine Screw Fit)"
            "6H" -> "6H (Standard Internal Nut Fit)"
            "5H" -> "5H (Close Internal Fit)"
            "4H" -> "4H (Precision Aerospace Internal Fit)"
            "6G" -> "6G (Internal Fit with Plating Allowance)"
            "7H" -> "7H (Coarse / Loose Internal Fit)"
            "2A" -> "2A (Standard Commercial External Fit)"
            "3A" -> "3A (Close Zero-Allowance Fit)"
            "3A-UNJ" -> "3A-UNJ (Aerospace AS8879 Controlled Root)"
            "1A" -> "1A (Loose / Free Field Assembly Fit)"
            "2B" -> "2B (Standard Internal Nut Fit)"
            "3B" -> "3B (Close Precision Internal Fit)"
            "3B-UNJ" -> "3B-UNJ (Aerospace Controlled Root AS8879)"
            "1B" -> "1B (Loose Internal Fit)"
            "3h4h" -> "3h4h (Finest Zero-Allowance Fit)"
            "4h6h" -> "4h6h (Close Pitch, Medium Crest)"
            "5h4h" -> "5h4h (Close Zero-Allowance, Fine Crest)"
            "5h6h" -> "5h6h (Close Zero-Allowance Fit)"
            "6d" -> "6d (Extra-Large Allowance, Heavy Coatings)"
            "7e6e" -> "7e6e (Coarse Fit, Large Allowance)"
            "7g6g" -> "7g6g (Coarse Fit with Allowance)"
            "7h6h" -> "7h6h (Coarse Zero-Allowance Fit)"
            "8h" -> "8h (Loose Zero-Allowance Fit)"
            "9g8g" -> "9g8g (Very Loose Fit)"
            "4h6h-MJ" -> "4h6h MJ (Aerospace ISO 5855, Controlled Root)"
            "4H5H" -> "4H5H (Close Pitch, Fine Minor)"
            "5G" -> "5G (Close Fit with Plating Allowance)"
            "5H6H" -> "5H6H (Close Pitch, Medium Minor)"
            "7G" -> "7G (Coarse Fit with Allowance)"
            "8G" -> "8G (Loose Fit with Allowance)"
            "8H" -> "8H (Loose Internal Fit)"
            "6AZ" -> "6AZ (Oversize Nut for Centrifuged Hot-Dip Galv, ISO 965-5)"
            "6AX" -> "6AX (Oversize Nut for Heavy Hot-Dip Galv, ISO 965-5)"
            "4H5H-MJ" -> "4H5H MJ (Aerospace ISO 5855 Internal)"
            "5H6H-MJ" -> "5H6H MJ (Aerospace ISO 5855 Internal)"
            else -> code
        }
    }
}

data class ShopFloorProcessRoute(
    val routeNumber: Int,
    val categoryName: String,
    val applicableGrades: String,
    val metallurgicalBehavior: String,
    val sequenceSteps: String,
    val pokaYokeQualityGate: String
)

object ShopFloorData {
    val PROCESS_ROUTES = listOf(
        ShopFloorProcessRoute(
            routeNumber = 1,
            categoryName = "Route 1: Medium-Carbon & Boron Alloy Steels",
            applicableGrades = "EN24, EN24 ESR, AISI 4135, 4140, 4340, 8740, 30KhGSA, 16KhGSN, 35NCD16, 51B37, 10B21/15B25/10B35, 4130",
            metallurgicalBehavior = "Moderate work-hardening; high formability in spheroidized annealed state (Z >= 50%).",
            sequenceSteps = "1. Spheroidized Annealed Wire + Zn-Phosphate & Soap Lube -> 2. Cold/Warm Multi-Station Heading (Forward Extrude shank + Upset Head) -> 3. Thread Roll in Green/Soft State -> 4. Wash -> 5. Austenitize & Oil Quench + Temper -> 6. Plating + De-embrittlement Baking.",
            pokaYokeQualityGate = "Bake at 190–220°C within 1–4 hrs post-plating if UTS >= 1000 MPa (>31 HRC) per ISO 4042 / ASTM F1940."
        ),
        ShopFloorProcessRoute(
            routeNumber = 2,
            categoryName = "Route 2: Austenitic Fe/Ni Superalloys",
            applicableGrades = "A286 (AMS 5731), Inconel 718 (AMS 5662), 10Kh11N23T3MR (EP33)",
            metallurgicalBehavior = "Severe rapid strain-hardening; high galling risk; micro-cracking if cold headed heavily at room temp.",
            sequenceSteps = "1. Solution Annealed Wire + Oxalate / Glass-MoS2 Dual Lube -> 2. Controlled Warm Heading (Induction 650–720°C) with Carbide Dies -> 3. Solution Heat Treat (955–1100°C) -> 4. Precipitation Aging (e.g. 720°C 16h) -> 5. Post-Aging Thread Rolling -> 6. Nitric Passivation + FPI.",
            pokaYokeQualityGate = "Roll threads AFTER aging for aerospace fatigue life (NASM/AMS). Strictly monitor warm heading temp."
        ),
        ShopFloorProcessRoute(
            routeNumber = 3,
            categoryName = "Route 3: Aerospace Titanium Alloys",
            applicableGrades = "Ti-6Al-4V (Grade 5), VT-16 (GOST ВТ16), Titanium Grade 1",
            metallurgicalBehavior = "Gr 5 requires warm forming due to HCP crystal structure & springback; VT-16 & Gr 1 allow cold heading.",
            sequenceSteps = "1. Annealed Wire + Fluoride-Phosphate + MoS2 Lube -> 2. Warm Heading (650–700°C for Ti6Al4V; Cold/Warm for VT-16) -> 3. Solution Treat + Vacuum Age (STA) -> 4. Slow-Speed Thread Rolling with EP Oil -> 5. Chemical Pickling (remove alpha-case) + FPI.",
            pokaYokeQualityGate = "Never exceed beta-transus or 750°C in open air to prevent brittle oxygen alpha-case layer."
        ),
        ShopFloorProcessRoute(
            routeNumber = 4,
            categoryName = "Route 4: Martensitic & PH Stainless Steels",
            applicableGrades = "15-5PH (AMS 5659), 17-4PH (AMS 5643), 14Kh17N2, 20X13, 30X13",
            metallurgicalBehavior = "High initial yield strength in Condition A (for PH grades); magnetic & heat-treatable via Q&T or Aging.",
            sequenceSteps = "1. Condition A / Annealed Wire + Oxalate & Soap -> 2. Warm or Rigid Cold Heading -> 3. Thread Rolling prior to final aging/Q&T -> 4. Solution + Age (H900/H1025 for PH) or Oil Quench + Temper (for 14Kh17N2/20X13/30X13) -> 5. Passivation + Magnetic Particle / FPI.",
            pokaYokeQualityGate = "Avoid tempering 14Kh17N2/20X13/30X13 in the 400–525°C embrittlement zone."
        ),
        ShopFloorProcessRoute(
            routeNumber = 5,
            categoryName = "Route 5: Austenitic Stainless & Solid-Solution Alloys",
            applicableGrades = "SS 304, SS 316, 12Kh18N10T (AISI 321), Inconel 600",
            metallurgicalBehavior = "Non-heat-treatable by quenching; strength is achieved strictly through cold-work strain hardening.",
            sequenceSteps = "1. Solution Annealed Wire + Oxalate + Calcium Stearate Soap -> 2. High-Rigidity Cold Heading -> 3. Direct Cold Thread Rolling (work-hardens thread roots to A2/A4-70 or 80) -> 4. Wash & Nitric Passivation.",
            pokaYokeQualityGate = "No quench & temper HT; Exempt from hydrogen baking under standard nitric passivation."
        )
    )
}


