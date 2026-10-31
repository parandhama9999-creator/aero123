# Formula Implementation Guide for AeroFast Pro
**Purpose:** Define which formulas from Design_calculations_FIXED.xlsx should be integrated into the app  
**Status:** Ready for development  
**Date:** October 31, 2026

---

## Current App Capabilities (Already Implemented)

✅ **Cold-heading die progression** (volume-conserving stations)  
✅ **Slug volume calculations** (volume = area × height)  
✅ **Thread specifications** (pitch, class, limits per ISO 965 / ASME B1.1)  
✅ **Material properties** (UTS, yield strength, hardness)  
✅ **Forging tonnage estimates**  
✅ **Heat treatment specs**  
✅ **CNC tooling & G-code**  
✅ **Cost quotations**  

---

## Recommended Formula Additions (Prioritized)

### **TIER 1: ESSENTIAL (Add First)**

These are the most useful for fastener design and directly enhance current capabilities.

---

#### **1.1 Volume Calculator Module** 
**Purpose:** Quick volume calculations for various fastener head types  
**Formulas needed:**

| Feature | Formula | Input | Output |
|---------|---------|-------|--------|
| **Hex Head Volume** | `V = (π/4) × s² × k` | s (across-flats), k (height) | Volume (mm³) |
| **Socket Head Volume** | `V = (π/4) × dk² × k` | dk (head dia), k (height) | Volume (mm³) |
| **Cylinder Volume** | `V = (π/4) × d² × h` | d (diameter), h (height) | Volume (mm³) |
| **Cone Frustum Volume** | `V = (π×H/3) × (R² + R×r + r²)` | H (height), R (large r), r (small r) | Volume (mm³) |
| **Hex Socket Volume** | `V = (√3/2) × s² × t` | s (across-flats), t (depth) | Volume (mm³) |

**Implementation:** Add as a **standalone calculator screen** or **modal dialog**  
**User input:** Dimension inputs → Auto-calculates volume  
**Priority:** 🔴 **HIGH** — Users frequently need volume calculations

---

#### **1.2 Cut-off Length Calculator**
**Purpose:** Determine slug length needed for heading operation  
**Formula:** `L = 4 × Volume / (π × d²)`

**Inputs:**
- Total part volume (mm³) — calculated from blank geometry
- Wire/slug diameter (mm)

**Output:**
- Cut-off length (mm)
- Material weight (g) — using material density from Materials sheet

**Current Status:** Partially implemented in `CalculationEngine.cutOffLengthMm`  
**Enhancement:** Add direct formula access for manual override  
**Priority:** 🔴 **HIGH** — Critical for blank planning

---

#### **1.3 Material Density & Weight Calculator**
**Purpose:** Calculate part weight based on volume and material  
**Formula:** `Weight = Volume × Density`

**Density table (from Materials sheet):**
| Material | Density |
|----------|---------|
| Steel | 0.007875 g/mm³ |
| Aluminum | 0.0027 g/mm³ |
| Brass | 0.0085 g/mm³ |
| Copper | 0.00896 g/mm³ |

**Implementation:** 
- Add material dropdown (not just hardcoded steel)
- Auto-calculate weight based on selected material
- Show weight in grams and ounces (for inch users)

**Current Status:** Hardcoded for steel only  
**Enhancement:** Parameterize with material selection  
**Priority:** 🔴 **HIGH** — Essential for cost calculation

---

#### **1.4 Hex Socket Cavity Calculations**
**Purpose:** Design socket cavity depth and size  
**Formulas:**

| Parameter | Formula | Notes |
|-----------|---------|-------|
| **Socket Key Size (s)** | Input (ISO 4762 table) | Across-flats |
| **Socket Depth (t)** | Input (ISO 4762 table) | Minimum from standard |
| **Socket Volume** | `V = (√3/2) × s² × t` | Hexagon area × depth |

**Current Status:** Referenced in `socketFor()` dimension lookup  
**Enhancement:** Add visual diagram showing socket geometry  
**Priority:** 🟡 **MEDIUM** — Useful for SHCS design

---

### **TIER 2: IMPORTANT (Add Second)**

These enhance design flexibility and user experience.

---

#### **2.1 Cone/Frustum Designer**
**Purpose:** Help design cone preforms for heading operations  
**Formulas:**

| Parameter | Formula |
|-----------|---------|
| **Frustum Volume** | `V = (π×h/3) × (R² + R×r + r²)` |
| **Cone Angle** | `θ = arctan((D_large - D_small) / (2 × height))` |
| **Cone Height for Target Volume** | Solve for h: `h = 3V / (π(R² + Rr + r²))` |
| **Upset Classification** | IF L/D > 2.5 → SLIDING CONE; IF < 1.5 → STRAIGHT UPSET; ELSE → SINGLE CONE |

**Implementation:** Interactive form with:
- Input: Large diameter, small diameter, height
- Output: Volume, angle, classification
- Diagram: Visual cone shape

**Current Status:** Partially implemented in HeadingProgression  
**Enhancement:** Standalone tool for cone optimization  
**Priority:** 🟡 **MEDIUM** — Advanced users appreciate this

---

#### **2.2 Hex Dimension Lookup & Validation**
**Purpose:** Validate hex head/nut dimensions against standards  
**Source Data:** DimensionTables.kt (ISO 4014, ISO 4032, ASME B18.2.1)

**Checks:**
- Across-flats (s) min/max
- Head height (k) nominal
- Bearing diameter (dw) min
- Nut height (m) nominal

**Implementation:** Auto-populate from standard tables based on diameter  
**Current Status:** Implemented in `DimensionTables.forPart()`  
**Enhancement:** Add error checking if user enters non-standard values  
**Priority:** 🟡 **MEDIUM** — Prevents design mistakes

---

#### **2.3 Thread Length Calculation**
**Purpose:** Determine usable thread length after forming  
**Formula:** `Thread Length = Total Length - Head Height - Blank/Shank Length`

**Inputs:**
- Total finished length (mm)
- Head height (mm) — from dimension table
- Blank diameter transition length (mm)

**Output:**
- Effective thread length (mm)
- Thread pitch runs (number)
- Validation: Is thread length adequate?

**Priority:** 🟡 **MEDIUM** — Part of design iteration

---

### **TIER 3: NICE-TO-HAVE (Add Later)**

These are useful but less critical for core functionality.

---

#### **3.1 Die/Punch Design Optimizer**
**Purpose:** Calculate die and punch geometry for cold heading  
**Formulas:**

| Parameter | Formula |
|-----------|---------|
| **Die Clearance** | Die_diameter = Part_diameter × 1.2 to 1.4 |
| **Punch Tip Radius** | Typically 0.5 to 1.0 mm |
| **Contact Pressure** | P = Load / Contact_Area |

**Current Status:** In HeadingProgression as die-station geometry  
**Enhancement:** Standalone design tool with input controls  
**Priority:** 🟢 **LOW** — Advanced feature for die makers

---

#### **3.2 Material Flow Analysis**
**Purpose:** Predict material behavior during heading  
**Formulas:**

| Property | Formula |
|----------|---------|
| **Flow Stress** | Material-specific (in CalculationEngine) |
| **Reduction %** | `(1 - Area_final / Area_initial) × 100` |
| **Upset Classification** | Based on L/D and cone angle |

**Current Status:** Basic implementation exists  
**Enhancement:** Graphical representation of material flow  
**Priority:** 🟢 **LOW** — Educational feature

---

#### **3.3 Cost Optimization**
**Purpose:** Minimize material waste and heading tonnage  
**Formulas:**

| Metric | Formula |
|--------|---------|
| **Scrap %** | `(Slug_volume - Part_volume) / Slug_volume × 100` |
| **Material Cost** | `(Weight_g / 1000) × ₹/kg` |
| **Heading Cost** | `Tonnage × ₹/hour × Setup_time` |

**Current Status:** Implemented in CalculationEngine (costRawMaterial, etc.)  
**Enhancement:** Sensitivity analysis (what-if scenarios)  
**Priority:** 🟢 **LOW** — Business feature

---

#### **3.4 Fatigue Analysis**
**Purpose:** Predict fatigue life under dynamic loading  
**Formulas:**

| Parameter | Formula |
|-----------|---------|
| **Stress Concentration** | Kt (material-dependent, FEA-based) |
| **Peak Thread Root Stress** | σ_max = Load / StressArea × Kt |
| **Endurance Limit** | Se = 0.5 × UTS (conservative estimate) |
| **Fatigue Margin** | SF = Se / (σ_max / cycles) |

**Current Status:** Implemented in CalculationEngine (peakThreadRootStressMpa, etc.)  
**Enhancement:** Interactive failure curve  
**Priority:** 🟢 **LOW** — Specialized feature

---

## Implementation Roadmap

### **Phase 1: MVP (Immediate)**
- [ ] Volume Calculator (hexagon, cylinder, cone frustum)
- [ ] Material Selection (Steel, Aluminum, Brass, Copper)
- [ ] Weight Calculator (auto-calculate based on volume + material)
- [ ] Cut-off Length (formula widget)

**Estimated effort:** 2-3 days  
**User value:** High (saves design time)

---

### **Phase 2: Enhancement (Week 2)**
- [ ] Cone Designer (interactive frustum tool)
- [ ] Hex Dimension Lookup (with validation)
- [ ] Thread Length Calculator
- [ ] Socket Cavity Designer

**Estimated effort:** 3-4 days  
**User value:** Medium (specialized workflows)

---

### **Phase 3: Advanced (Month 2)**
- [ ] Die/Punch Optimizer
- [ ] Material Flow Visualization
- [ ] Cost Optimization (what-if scenarios)
- [ ] Fatigue Analysis Tool

**Estimated effort:** 5-7 days  
**User value:** Low-Medium (advanced users)

---

## Data Structure for App Integration

### **Formulas Data Class**
```kotlin
data class EngineeringFormulas(
    val formulas: Map<String, Formula>,
    val materials: List<MaterialSpec>,
    val standards: Map<String, Standard>
)

data class Formula(
    val name: String,           // "Hex Volume"
    val equation: String,       // "V = (π/4) × s² × k"
    val derivation: String,     // Why this formula
    val inputs: List<Parameter>,
    val outputs: List<Parameter>,
    val standardReference: String  // "ISO 4014"
)

data class Parameter(
    val name: String,           // "Across-Flats"
    val symbol: String,         // "s"
    val unit: String,           // "mm"
    val min: Double?,
    val max: Double?,
    val defaultValue: Double?
)
```

---

## UI/UX Recommendations

### **Calculator Screen Layout**
```
┌─ Volume Calculator ────────┐
│ ┌─ Head Type Selector ──┐  │
│ │ ○ Hex Head           │  │
│ │ ○ Socket Head        │  │
│ │ ○ Cone/Frustum      │  │
│ └──────────────────────┘  │
│                            │
│ Input Parameters:          │
│ ┌──────────────────────┐  │
│ │ Across-Flats: [ 16.0 ] mm │
│ │ Height:       [  6.4 ] mm │
│ │ [Formula Diagram]   │
│ └──────────────────────┘  │
│                            │
│ Result:                    │
│ ┌──────────────────────┐  │
│ │ Volume: 677.3 mm³    │  │
│ │ Material: Steel      │  │
│ │ Weight: 5.33 g       │  │
│ │ [Copy] [Share]       │  │
│ └──────────────────────┘  │
│                            │
│ [Detailed Formula View]    │
│ [Save to Design]           │
└────────────────────────────┘
```

---

## Testing Strategy

### **Unit Tests**
```kotlin
@Test
fun hexVolumeCalculation() {
    val s = 16.0  // across-flats (mm)
    val k = 6.4   // height (mm)
    val expected = 677.3  // mm³
    
    val result = hexVolume(s, k)
    assertEquals(expected, result, 1.0)  // tolerance ±1 mm³
}

@Test
fun coneFrustumVolume() {
    val h = 10.0
    val R = 13.325  // large radius
    val r = 10.3    // small radius
    val expected = 4_250.5  // mm³
    
    val result = coneFrustumVolume(h, R, r)
    assertEquals(expected, result, 5.0)
}

@Test
fun cutOffLength() {
    val volume = 4_250.5  // mm³
    val d = 4.8          // diameter (mm)
    val expected = 237.6  // mm
    
    val result = cutOffLength(volume, d)
    assertEquals(expected, result, 0.5)
}
```

---

## Summary Table

| Feature | Priority | Effort | Value | Status |
|---------|----------|--------|-------|--------|
| Volume Calculator | 🔴 HIGH | 2d | High | Phase 1 |
| Cut-off Length | 🔴 HIGH | 1d | High | Phase 1 |
| Material/Weight | 🔴 HIGH | 1d | High | Phase 1 |
| Socket Designer | 🟡 MED | 2d | Medium | Phase 2 |
| Cone Frustum | 🟡 MED | 2d | Medium | Phase 2 |
| Hex Lookup | 🟡 MED | 1d | Medium | Phase 2 |
| Thread Length | 🟡 MED | 1d | Medium | Phase 2 |
| Die Optimizer | 🟢 LOW | 3d | Low | Phase 3 |
| Material Flow | 🟢 LOW | 2d | Low | Phase 3 |
| Cost Analysis | 🟢 LOW | 2d | Low | Phase 3 |
| Fatigue Tool | 🟢 LOW | 2d | Low | Phase 3 |

---

## Next Steps

1. **Approval:** Which features from this list should be implemented?
2. **Timeline:** Phase 1 → Phase 2 → Phase 3, or all at once?
3. **UI Design:** Finalize calculator interface mockups
4. **Development:** Start with Phase 1 formulas
5. **Testing:** Unit tests for each formula against known values

**Ready to start Phase 1 implementation?**

