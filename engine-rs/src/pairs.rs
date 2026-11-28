//! Kotlin `Pair` / `Triple` are objects `{first, second[, third]}` in JSON; Rust tuples are arrays.
//! These helpers write and read the Kotlin form.

use serde::{Deserialize, Deserializer, Serialize, Serializer};

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
pub struct Pair<A, B> {
    pub first: A,
    pub second: B,
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
pub struct Triple<A, B, C> {
    pub first: A,
    pub second: B,
    pub third: C,
}

pub fn p2<A, B>(t: (A, B)) -> Pair<A, B> {
    Pair { first: t.0, second: t.1 }
}

pub fn p3<A, B, C>(t: (A, B, C)) -> Triple<A, B, C> {
    Triple { first: t.0, second: t.1, third: t.2 }
}

/// `Vec<(A, B)>` as a list of Kotlin pairs.
pub mod vec2 {
    use super::*;
    pub fn serialize<S: Serializer, A: Serialize + Clone, B: Serialize + Clone>(v: &[(A, B)], s: S) -> Result<S::Ok, S::Error> {
        let p: Vec<Pair<A, B>> = v.iter().cloned().map(p2).collect();
        p.serialize(s)
    }
    pub fn deserialize<'de, D: Deserializer<'de>, A: Deserialize<'de>, B: Deserialize<'de>>(d: D) -> Result<Vec<(A, B)>, D::Error> {
        let p: Vec<Pair<A, B>> = Vec::deserialize(d)?;
        Ok(p.into_iter().map(|x| (x.first, x.second)).collect())
    }
}
