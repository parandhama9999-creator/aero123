//! Number formatting and rounding that match the JVM (Kotlin) behaviour exactly, so text produced by
//! the Rust engine is byte-for-byte the text the Kotlin engine produced.

/// Shortest round-trip decimal digits of |x| and the exponent of the first digit (x = d1.d2d3… × 10^exp).
fn shortest_digits(x: f64) -> (Vec<u8>, i32) {
    let s = format!("{:e}", x.abs());
    let (mant, exp) = s.split_once('e').unwrap();
    let digits: Vec<u8> = mant.bytes().filter(|b| b.is_ascii_digit()).collect();
    (digits, exp.parse().unwrap())
}

/// `String.format(Locale.US, "%.{dp}f", x)`: half-up rounding of the shortest decimal representation.
pub fn f(x: f64, dp: usize) -> String {
    fixed(x, dp, false)
}

/// `String.format(Locale.US, "%,.{dp}f", x)`: as [f] with thousands separators.
pub fn fg(x: f64, dp: usize) -> String {
    fixed(x, dp, true)
}

fn fixed(x: f64, dp: usize, group: bool) -> String {
    if x.is_nan() {
        return "NaN".into();
    }
    if x.is_infinite() {
        return if x > 0.0 { "Infinity".into() } else { "-Infinity".into() };
    }
    let neg = x.is_sign_negative();
    let (mut digits, mut exp) = if x == 0.0 { (vec![b'0'], 0) } else { shortest_digits(x) };
    // number of digits kept before rounding: integer digits (exp + 1) plus dp decimals
    let keep = exp + 1 + dp as i32;
    if keep < 0 {
        digits = vec![b'0'];
        exp = 0;
    } else if (keep as usize) < digits.len() {
        let k = keep as usize;
        let round_up = digits[k] >= b'5';
        digits.truncate(k);
        if round_up {
            let mut i = k;
            loop {
                if i == 0 {
                    digits.insert(0, b'1');
                    exp += 1;
                    break;
                }
                i -= 1;
                if digits[i] == b'9' {
                    digits[i] = b'0';
                } else {
                    digits[i] += 1;
                    break;
                }
            }
        }
        if digits.is_empty() {
            digits = vec![b'0'];
            exp = 0;
        }
    }
    // integer and fraction parts
    let mut int_part = String::new();
    let mut frac = String::new();
    if exp >= 0 {
        for i in 0..=(exp as usize) {
            int_part.push(*digits.get(i).unwrap_or(&b'0') as char);
        }
        for i in 0..dp {
            frac.push(*digits.get(exp as usize + 1 + i).unwrap_or(&b'0') as char);
        }
    } else {
        int_part.push('0');
        let lead = (-exp - 1) as usize;
        for i in 0..dp {
            if i < lead {
                frac.push('0');
            } else {
                frac.push(*digits.get(i - lead).unwrap_or(&b'0') as char);
            }
        }
    }
    let int_part = int_part.trim_start_matches('0');
    let int_part = if int_part.is_empty() { "0".to_string() } else { int_part.to_string() };
    let int_part = if group { group3(&int_part) } else { int_part };
    let mut out = String::new();
    if neg {
        out.push('-');
    }
    out.push_str(&int_part);
    if dp > 0 {
        out.push('.');
        out.push_str(&frac);
    }
    out
}

fn group3(s: &str) -> String {
    let b = s.as_bytes();
    let mut out = String::new();
    for (i, c) in b.iter().enumerate() {
        if i > 0 && (b.len() - i) % 3 == 0 {
            out.push(',');
        }
        out.push(*c as char);
    }
    out
}

/// `Double.toString(x)` (what Kotlin string templates print for a Double).
pub fn d(x: f64) -> String {
    if x.is_nan() {
        return "NaN".into();
    }
    if x.is_infinite() {
        return if x > 0.0 { "Infinity".into() } else { "-Infinity".into() };
    }
    if x == 0.0 {
        return if x.is_sign_negative() { "-0.0".into() } else { "0.0".into() };
    }
    let neg = x < 0.0;
    let a = x.abs();
    let (digits, exp) = shortest_digits(a);
    let ds: String = digits.iter().map(|&c| c as char).collect();
    let mut out = String::new();
    if neg {
        out.push('-');
    }
    if (1e-3..1e7).contains(&a) {
        if exp >= 0 {
            let e = exp as usize;
            if ds.len() > e + 1 {
                out.push_str(&ds[..=e]);
                out.push('.');
                out.push_str(&ds[e + 1..]);
            } else {
                out.push_str(&ds);
                for _ in ds.len()..=e {
                    out.push('0');
                }
                out.push_str(".0");
            }
        } else {
            out.push_str("0.");
            for _ in 0..(-exp - 1) {
                out.push('0');
            }
            out.push_str(&ds);
        }
    } else {
        out.push_str(&ds[..1]);
        out.push('.');
        if ds.len() > 1 {
            out.push_str(&ds[1..]);
        } else {
            out.push('0');
        }
        out.push('E');
        out.push_str(&exp.to_string());
    }
    out
}

/// Kotlin `Double.roundToInt()` / Java `Math.round`: half rounds up (towards +∞).
pub fn round_to_int(x: f64) -> i32 {
    if x.is_nan() {
        return 0;
    }
    (x + 0.5).floor() as i32
}

/// Kotlin `Double.roundToLong()`.
pub fn round_to_long(x: f64) -> i64 {
    if x.is_nan() {
        return 0;
    }
    (x + 0.5).floor() as i64
}

/// Kotlin `kotlin.math.round(x)`: half to even (`Math.rint`).
pub fn rint(x: f64) -> f64 {
    x.round_ties_even()
}

/// Kotlin `minByOrNull`: the first element with the smallest key.
pub fn min_by<T, K: PartialOrd>(items: impl IntoIterator<Item = T>, key: impl Fn(&T) -> K) -> Option<T> {
    let mut best: Option<(T, K)> = None;
    for it in items {
        let k = key(&it);
        match &best {
            Some((_, bk)) if !(k < *bk) => {}
            _ => best = Some((it, k)),
        }
    }
    best.map(|(t, _)| t)
}

/// Kotlin `maxByOrNull`: the first element with the largest key.
pub fn max_by<T, K: PartialOrd>(items: impl IntoIterator<Item = T>, key: impl Fn(&T) -> K) -> Option<T> {
    let mut best: Option<(T, K)> = None;
    for it in items {
        let k = key(&it);
        match &best {
            Some((_, bk)) if !(k > *bk) => {}
            _ => best = Some((it, k)),
        }
    }
    best.map(|(t, _)| t)
}

/// `String.format("%-{w}s", s)` and `"%{w}s"`.
pub fn pad_right(s: &str, w: usize) -> String {
    let n = s.chars().count();
    if n >= w { s.to_string() } else { format!("{}{}", s, " ".repeat(w - n)) }
}

pub fn pad_left(s: &str, w: usize) -> String {
    let n = s.chars().count();
    if n >= w { s.to_string() } else { format!("{}{}", " ".repeat(w - n), s) }
}

/// Kotlin `coerceIn` for doubles.
pub fn clamp(x: f64, lo: f64, hi: f64) -> f64 {
    if x < lo { lo } else if x > hi { hi } else { x }
}

/// Kotlin `String.toDoubleOrNull()` (Java `Double.valueOf` grammar): surrounding chars ≤ ' ' allowed,
/// optional sign, `NaN`, `Infinity`, decimal with optional exponent, hex float with binary exponent,
/// and an optional trailing f/F/d/D.
pub fn to_double_or_null(s: &str) -> Option<f64> {
    let t = s.trim_matches(|c: char| c <= ' ');
    let (neg, body) = match t.as_bytes().first() {
        Some(b'+') => (false, &t[1..]),
        Some(b'-') => (true, &t[1..]),
        _ => (false, t),
    };
    let sign = |v: f64| if neg { -v } else { v };
    if body == "NaN" {
        return Some(f64::NAN);
    }
    if body == "Infinity" {
        return Some(sign(f64::INFINITY));
    }
    let body = body.strip_suffix(['f', 'F', 'd', 'D']).unwrap_or(body);
    let b = body.as_bytes();
    if b.len() > 2 && b[0] == b'0' && (b[1] == b'x' || b[1] == b'X') {
        return parse_hex_float(&body[2..]).map(sign);
    }
    // Digits (. Digits?)? Exp? | . Digits Exp?
    let mut i = 0;
    let int_start = i;
    while i < b.len() && b[i].is_ascii_digit() {
        i += 1;
    }
    let int_len = i - int_start;
    let mut frac_len = 0;
    if i < b.len() && b[i] == b'.' {
        i += 1;
        let fs = i;
        while i < b.len() && b[i].is_ascii_digit() {
            i += 1;
        }
        frac_len = i - fs;
    }
    if int_len == 0 && frac_len == 0 {
        return None;
    }
    if i < b.len() && (b[i] == b'e' || b[i] == b'E') {
        i += 1;
        if i < b.len() && (b[i] == b'+' || b[i] == b'-') {
            i += 1;
        }
        let es = i;
        while i < b.len() && b[i].is_ascii_digit() {
            i += 1;
        }
        if i == es {
            return None;
        }
    }
    if i != b.len() {
        return None;
    }
    body.parse::<f64>().ok().map(sign)
}

fn parse_hex_float(h: &str) -> Option<f64> {
    let (mant, exp) = h.split_once(['p', 'P'])?;
    let exp: i32 = {
        let e = exp.strip_prefix('+').unwrap_or(exp);
        if e.is_empty() || !e.trim_start_matches('-').bytes().all(|c| c.is_ascii_digit()) || e.trim_start_matches('-').is_empty() {
            return None;
        }
        e.parse::<i64>().ok()?.clamp(-100_000, 100_000) as i32
    };
    let (ip, fp) = mant.split_once('.').unwrap_or((mant, ""));
    if ip.is_empty() && fp.is_empty() {
        return None;
    }
    let mut v = 0.0f64;
    for c in ip.chars() {
        v = v * 16.0 + c.to_digit(16)? as f64;
    }
    let mut scale = 1.0 / 16.0;
    for c in fp.chars() {
        v += c.to_digit(16)? as f64 * scale;
        scale /= 16.0;
    }
    Some(v * 2f64.powi(exp))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn java_fixed_formatting() {
        assert_eq!(f(0.125, 2), "0.13"); // Java HALF_UP on the decimal digits
        assert_eq!(f(1.005, 2), "1.01"); // shortest digits 1.005 round up (binary value is 1.00499…)
        assert_eq!(f(2.5, 0), "3");
        assert_eq!(f(-0.001, 2), "-0.00");
        assert_eq!(f(9.995, 2), "10.00");
        assert_eq!(f(1234567.891, 1), "1234567.9");
        assert_eq!(fg(1234567.891, 2), "1,234,567.89");
        assert_eq!(f(0.0004, 3), "0.000");
        assert_eq!(f(0.0005, 3), "0.001");
        assert_eq!(f(5e-7, 2), "0.00");
        assert_eq!(f(99.0, 0), "99");
        assert_eq!(f(1e21, 1), "1000000000000000000000.0");
    }

    #[test]
    fn java_double_to_string() {
        assert_eq!(d(10.0), "10.0");
        assert_eq!(d(0.1), "0.1");
        assert_eq!(d(1e7), "1.0E7");
        assert_eq!(d(1.5e-4), "1.5E-4");
        assert_eq!(d(0.001), "0.001");
        assert_eq!(d(123456.789), "123456.789");
        assert_eq!(d(-2.5), "-2.5");
        assert_eq!(d(100.0), "100.0");
    }

    #[test]
    fn rounding() {
        assert_eq!(round_to_int(2.5), 3);
        assert_eq!(round_to_int(-2.5), -2);
        assert_eq!(rint(2.5), 2.0);
        assert_eq!(rint(3.5), 4.0);
        assert_eq!(max_by(vec![1, 3, 3], |x| *x), Some(3));
        assert_eq!(min_by(vec![(1, 'a'), (0, 'b'), (0, 'c')], |x| x.0), Some((0, 'b')));
    }

    #[test]
    fn kotlin_to_double_or_null() {
        assert_eq!(to_double_or_null(" 5.5 "), Some(5.5));
        assert_eq!(to_double_or_null("6"), Some(6.0));
        assert_eq!(to_double_or_null(".5"), Some(0.5));
        assert_eq!(to_double_or_null("5."), Some(5.0));
        assert_eq!(to_double_or_null("-1e3"), Some(-1000.0));
        assert_eq!(to_double_or_null("1.5f"), Some(1.5));
        assert_eq!(to_double_or_null("0x1p3"), Some(8.0));
        assert!(to_double_or_null("NaN").unwrap().is_nan());
        assert_eq!(to_double_or_null("-Infinity"), Some(f64::NEG_INFINITY));
        for bad in ["", ".", "inf", "nan", "1e", "1,5", "abc", "1.2.3", "e5"] {
            assert_eq!(to_double_or_null(bad), None, "{bad}");
        }
    }
}
