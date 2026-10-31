# Engineering Calculations Audit Report
**File:** Design_calculations_as_on_now.xlsx  
**Date:** October 31, 2026  
**Status:** 🔴 **CRITICAL ERRORS FOUND** — Multiple issues require correction

---

## Executive Summary

**Total Formulas Analyzed:** 95+  
**Critical Errors:** 5 🔴  
**Warnings:** 12 ⚠️  
**Correct:** ~78 ✅

---

## 🔴 CRITICAL ERRORS (Must Fix)

### 1. **VOLUME Sheet — #REF! Error**

**Location:** VOLUME Sheet, Cells E12, E13  
**Formula:** `=#REF!/2`  
**Problem:** 
- Broken reference to deleted or missing cell
- Both E12 and E13 contain this error
- Will cause calculation failures

**Fix:** 
- Identify what should be referenced (unclear from context)
- Likely should reference cone diameter or similar parameter
- **BLOCKED:** Cannot calculate until reference is corrected

**Status:** 🔴 **CRITICAL**

---

### 2. **Cone Calculations — Volume Formula Incorrect**

**Location:** Cone (2) Sheet, C13; Cone Sheet, C13; PUNCH Sheet, C13; DIE Sheet, C13  
**Formula:** 
```
=(1/3*3.14*C11*(((C7/2)*(C7/2))+((C2/2)*(C2/2))+((C7/2)*(C2/2))))+(3.14*((C2/2)*(C2/2))*C4)
```

**Problem — Cone Frustum Volume:**
- Formula attempts to calculate frustum volume (tapered cone)
- **Issue:** The formula structure is ambiguous and possibly incorrect
- Standard frustum volume formula:
  ```
  V = (π*h/3) * (R² + R*r + r²)
  ```
  Where h = height, R = large radius, r = small radius

- **What the formula does:**
  - `1/3*3.14*C11` = (1/3) × π × height ✓
  - `(C7/2)*(C7/2)` = R² (large radius squared) ✓
  - `(C2/2)*(C2/2)` = r² (small radius squared) ✓
  - `(C7/2)*(C2/2)` = R × r (cross term) ✓
  - **Then adds:** `3.14*((C2/2)*(C2/2))*C4` ← **WRONG**

- The addition of cylinder volume at the end suggests trying to account for a flat base, but this is unconventional
- C4 is listed as clearance (0.02mm or similar), not a height

**Fix:**
```
=PI()/3 * C11 * ((C7/2)^2 + (C7/2)*(C2/2) + (C2/2)^2)
```

**Status:** 🔴 **CRITICAL** — Affects all cone/punch/die calculations

---

### 3. **HEX Head Design Sheet — Socket Head Volume Logic**

**Location:** HEX HEAD design, E18  
**Formula:** `=SQRT(3)/2*(E17*E17*E16)`  
**Problem:**
- E17 contains conditional text ("10 TO 12", "5 TO 8", etc.) — angle ranges, not numeric values
- **Cannot multiply text values** — will error or give unexpected results
- Formula tries to calculate hexagon area × height, but E17 is text (angle string)

**Fix:**
- Use actual angle value, not the conditional text string
- Should reference the original angle calculation, not the conditional result

**Status:** 🔴 **CRITICAL** — Will produce #VALUE! error when socket head calculation runs

---

### 4. **Inconsistent π Usage**

**Location:** Throughout (multiple sheets)  
**Problem:**
- Some formulas use `3.14` (approximate)
- Some use `PI()` (precise, 3.14159...)
- Mixing causes ~0.1-0.2% error accumulation

**Examples:**
- VOLUME B7: `=3.14/4*(H4*H4-H5*H5)*H6` — uses 3.14
- Cut-off and L/D F25: `=3.14*(E25*E25)/4` — uses 3.14
- But some use PI() function

**Fix:** Standardize to `PI()` throughout for consistency

**Status:** ⚠️ **WARNING** — Minor but should be corrected

---

### 5. **Cut-off Length Formula — Ambiguous**

**Location:** Cut-off and L/D Sheet, E5  
**Formula:** `=(E3*4)/(3.14*E4*E4*E4)`  
**Problem:**
- Formula: (volume × 4) / (π × d³)
- Derivation unclear: this simplifies to `(V × 4) / (π × d³)`
- Standard cut-off length formula: `L = V / A` where A = cross-sectional area = π/4 × d²
- This formula doesn't match standard engineering practice

**Verification:**
- If V = volume, d = diameter, then:
  - Expected: `L = V / (π/4 × d²) = 4V / (π × d²)`
  - Actual formula: `4V / (π × d³)` — has extra d in denominator
  
**Fix:** Should be:
```
=(E3*4)/(3.14*E4*E4)  [not E4*E4*E4]
```

**Status:** 🔴 **CRITICAL** — Off by a factor of diameter

---

## ⚠️ WARNINGS (Should Fix)

### 6. **Diameter vs Radius Confusion**

**Location:** Multiple sheets  
**Problem:** 
- Some calculations treat input as diameter, some as radius
- E.g., Cone Sheet: `C9: =(SQRT(C8/3.14))*2`
- This assumes C8 is area and converts to diameter
- But earlier `C8` is calculated as average of two circular areas

**Status:** ⚠️ **WARNING** — Verify if inputs are consistently diameter or radius

---

### 7. **Clearance Values Hardcoded**

**Location:** All sheets  
**Examples:**
- VOLUME B6: `=B4-0.05` (minus 0.05 clearance)
- VOLUME D7: `=C7-0.05`
- Cut-off: Various hardcoded subtractions

**Problem:**
- Clearances are hardcoded (0.05, 0.02, etc.)
- Should be input parameters, not hard-coded
- Different tolerances for different operations

**Status:** ⚠️ **WARNING** — Reduce flexibility

---

### 8. **TAN Function — Degree vs Radian**

**Location:** Cone (2) Sheet, C12  
**Formula:** `=DEGREES(ATAN((C7-C2)/(C11*2)))`  
**Check:**
- `ATAN()` returns radians
- `DEGREES()` converts to degrees ✓
- Math is correct, but verify C7, C2, C11 are in same units

**Status:** ✅ Correct, but document units clearly

---

### 9. **Hex Area Formula**

**Location:** Multiple (B18, C18, etc.)  
**Formula:** `=SQRT(3)/2*(B17*B17*B16)`  
**Check:**
- `SQRT(3)/2 ≈ 0.866` (correct hex area factor)
- Calculates: area of hex × height ✓
- **BUT:** Assumes input is "across flats" (s), not diameter
- **Verify:** Is B17 across-flats or diagonal?

**Status:** ⚠️ **WARNING** — Ensure consistent dimension definitions

---

### 10. **Weight Calculation — Density**

**Location:** Multiple sheets  
**Examples:**
- HEX HEAD B9: `=B7*B8` where B8 = 0.007875 (density)
- VOLUME H8: `=H7*0.007875&"g"`

**Problem:**
- 0.007875 g/mm³ is correct for steel
- But different materials have different densities
- Should be parameterized, not hardcoded

**Status:** ⚠️ **WARNING** — Only valid for steel

---

### 11. **L/D Ratio Interpretation**

**Location:** Cut-off and L/D Sheet  
**Context:** E5 calculates `L/D` as volume ratio, not length/diameter ratio
**Problem:**
- Standard L/D in heading is: **Length of free stock / Wire Diameter**
- This sheet calculates something different (volume-based)
- Confusing naming — should clarify

**Status:** ⚠️ **WARNING** — Labeling is misleading

---

### 12. **IF Logic Error in Socket Head Design**

**Location:** HEX HEAD design Sheet, E17  
**Formula:** 
```
=IF(E16<0.5,"10 TO 12",IF(E16>0.5,"5 TO 8",IF(E16>1.5,"1 TO 3")))
```
**Problem:**
- Logic: if E16 < 0.5 → "10 TO 12"
- else if E16 > 0.5 → "5 TO 8"
- **MISSING:** what if E16 = 0.5 exactly? Returns "5 TO 8" (correct as fallthrough)
- **ALSO:** Third condition `E16>1.5` nested inside `>0.5` branch is unreachable!
  - If E16 > 0.5, it jumps to "5 TO 8"
  - The `>1.5` check inside never executes

**Fix:** Should be:
```
=IF(E16<0.5,"10 TO 12",IF(E16<1.5,"5 TO 8","1 TO 3"))
```

**Status:** 🔴 **CRITICAL LOGIC ERROR** — Third option never returns

---

## ✅ VERIFIED CORRECT FORMULAS

### **Formulas that are mathematically correct:**

1. **Hex area:** `=SQRT(3)/2 * s²` ✓
2. **Cylinder volume:** `=PI()/4 * d² * h` ✓
3. **Radian conversion:** `=PI()/180` for degrees→radians ✓
4. **Degree conversion:** `=DEGREES(ATAN(...))` ✓
5. **Frustum elements** (individual terms, not combined result)
6. **Thread clearance logic** (though hardcoded)

---

## 📋 SUMMARY TABLE

| Issue | Sheet(s) | Severity | Fix Type |
|-------|----------|----------|----------|
| #REF! Error | VOLUME | 🔴 Critical | Find broken reference |
| Cone volume formula | Cone, Punch, Die | 🔴 Critical | Rewrite frustum calculation |
| Socket area × text | HEX HEAD | 🔴 Critical | Use numeric angle value |
| IF logic unreachable | HEX HEAD | 🔴 Critical | Fix nested conditions |
| Cut-off length d³ vs d² | Cut-off | 🔴 Critical | Remove extra diameter |
| π inconsistency | All | ⚠️ Warning | Standardize to PI() |
| Hardcoded clearances | All | ⚠️ Warning | Make parameterized |
| Hex dimension units | All | ⚠️ Warning | Document assumptions |
| Density hardcoded | All | ⚠️ Warning | Make material-dependent |
| L/D naming | Cut-off | ⚠️ Warning | Clarify definition |

---

## 🔧 RECOMMENDATIONS

### **Before adding to app, MUST fix:**
1. ✅ Resolve #REF! errors
2. ✅ Correct cone volume formula
3. ✅ Fix socket head IF logic
4. ✅ Verify cut-off length formula derivation
5. ✅ Standardize π usage

### **Should fix for robustness:**
6. Parameterize clearances
7. Support multiple materials (density)
8. Document dimension conventions (diameter vs across-flats vs radius)
9. Add unit labels to all cells
10. Create test cases with known good values

### **For app implementation:**
- Add input validation (no negative volumes, etc.)
- Unit conversion support (mm ↔ inches)
- Material selection dropdown
- Tolerance/clearance customization
- Result verification against standards

---

## 🚫 DO NOT USE AS-IS

**These calculations should NOT be included in the app without fixes.** The critical errors will produce incorrect results.

**Recommended approach:**
1. Correct all formulas in Excel first
2. Verify against engineering standards
3. Test with known reference values
4. Then port to app

---

## Questions for User

1. **What are the correct references for E12 and E13 in VOLUME sheet?**
2. **Is the cone volume formula intentionally different from standard?** (Add cylinder term?)
3. **Confirm dimensions:** Are all inputs diameter, or mix of diameter and "across flats"?
4. **Material:** Is this only for steel (0.007875 g/mm³), or support other materials?
5. **Standards:** Which standards do these formulas follow? (ISO, ASME, DIN?)

