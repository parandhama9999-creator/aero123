package com.aistudio.aerofast.trmst.data

import com.aistudio.aerofast.trmst.util.AppLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class JarvisQuery { CUT_OFF, WEIGHT, BLANK, HARDNESS, MACHINE, STATUS }
enum class JarvisAction { SHARE_REPORT, OPEN_SCANNER, OPEN_SIZING, OPEN_FORGING, OPEN_COPILOT }

/** What a spoken sentence asks the app to set, answer or do. A null field was not said. */
data class JarvisIntent(
    val type: FastenerType? = null,
    val unit: UnitSystem? = null,
    val diaMm: Double? = null,
    /** Pitch in mm for metric, TPI for inch. */
    val pitch: Double? = null,
    val lengthMm: Double? = null,
    val threadLengthMm: Double? = null,
    val materialKey: String? = null,
    val propertyClass: PropertyClass? = null,
    val tensileRange: TensileRange? = null,
    val coating: CoatingType? = null,
    val batchQty: Int? = null,
    val headerChoice: HeaderChoice? = null,
    val threadLockPatch: Boolean? = null,
    val queries: Set<JarvisQuery> = emptySet(),
    val action: JarvisAction? = null
) {
    val changesSetup: Boolean
        get() = listOf(type, unit, diaMm, pitch, lengthMm, threadLengthMm, materialKey, propertyClass, tensileRange, coating, batchQty, headerChoice, threadLockPatch)
            .any { it != null }
    val isEmpty: Boolean get() = !changesSetup && queries.isEmpty() && action == null

    /** This intent's values first, the other's where this one is silent. */
    fun orElse(o: JarvisIntent) = JarvisIntent(
        type ?: o.type, unit ?: o.unit, diaMm ?: o.diaMm, pitch ?: o.pitch, lengthMm ?: o.lengthMm, threadLengthMm ?: o.threadLengthMm,
        materialKey ?: o.materialKey, propertyClass ?: o.propertyClass, tensileRange ?: o.tensileRange, coating ?: o.coating,
        batchQty ?: o.batchQty, headerChoice ?: o.headerChoice, threadLockPatch ?: o.threadLockPatch, queries + o.queries, action ?: o.action
    )
}

/**
 * Reads a spoken command in any of the app's languages, offline. Numbers come from the recogniser as digits; the
 * engineering words (bolt, cut-off, weight, report …) are matched in English, Telugu, Hindi, Spanish, German, French,
 * Russian, Chinese and Japanese. Anything it cannot read is left null for Gemini (or the user) to fill.
 */
object JarvisParser {
    private val WAKE_WORDS = listOf("jarvis", "జార్విస్", "जार्विस", "джарвис", "贾维斯", "ジャービス")

    private fun normalizeDigits(s: String): String = buildString {
        for (ch in s) append(
            when (ch) {
                in '౦'..'౯' -> '0' + (ch - '౦')   // Telugu digits
                in '०'..'९' -> '0' + (ch - '०')   // Devanagari digits
                in '０'..'９' -> '0' + (ch - '０')   // full-width digits
                '．' -> '.'
                '×', '✕', '＊' -> 'x'
                else -> ch
            }
        )
    }

    private val CLASS_WORDS = mapOf(
        "4.6" to PropertyClass.CLASS_4_6, "4.8" to PropertyClass.CLASS_4_8, "5.6" to PropertyClass.CLASS_5_6, "5.8" to PropertyClass.CLASS_5_8,
        "6.8" to PropertyClass.CLASS_6_8, "8.8" to PropertyClass.CLASS_8_8, "9.8" to PropertyClass.CLASS_9_8, "10.9" to PropertyClass.CLASS_10_9,
        "12.9" to PropertyClass.CLASS_12_9
    )

    private val PIECE_WORDS = "pieces|piece|pcs|nos|numbers|units|పీసెస్|పీసులు|పీస్|ముక్కలు|నంబర్లు|పీసు|पीस|नग|piezas|unidades|stück|stk|pièces|штук|шт|件|个|個|本"
    private val QTY_WORDS = "quantity|qty|batch|order|బ్యాచ్|క్వాంటిటీ|ఆర్డర్|क्वांटिटी|मात्रा|बैच|cantidad|lote|menge|los|quantité|lot|количество|партия|数量|批量|ロット"

    private val COATINGS = listOf(
        listOf("zinc nickel", "zinc-nickel", "zn ni", "zn-ni", "znni", "జింక్ నికెల్", "జింక్ నికిల్", "जिंक निकल", "zinc níquel", "zink nickel", "zink-nickel", "zinc nickel", "цинк никель", "锌镍", "亜鉛ニッケル") to CoatingType.ZN_NI,
        listOf("geomet", "జియోమెట్", "जियोमेट", "ジオメット", "达克罗") to CoatingType.GEOMET,
        listOf("dacromet", "డాక్రోమెట్") to CoatingType.DACROMET,
        listOf("black oxide", "blackening", "బ్లాక్ ఆక్సైడ్", "ब्लैक ऑक्साइड", "óxido negro", "brüniert", "oxyde noir", "воронение", "发黑", "黒染め") to CoatingType.BLACK_OXIDE,
        listOf("manganese phosphate") to CoatingType.PHOSPHATE_MN,
        listOf("phosphate", "ఫాస్ఫేట్", "फॉस्फेट", "fosfato", "phosphat", "фосфат", "磷化", "リン酸") to CoatingType.PHOSPHATE,
        listOf("hot dip", "galvaniz", "galvanis", "హాట్ డిప్", "गैल्वनाइज", "feuerverzink", "горячее цинк", "热镀锌", "溶融亜鉛") to CoatingType.HOT_DIP_GALV,
        listOf("yellow zinc", "zinc yellow", "ఎల్లో జింక్", "पीला जिंक") to CoatingType.ZINC_YELLOW,
        listOf("passivat", "పాసివేషన్", "पैसिवेशन", "pasivado", "passivier", "пассивац", "钝化", "不動態") to CoatingType.PASSIVATION,
        listOf("ivd", "ion vapour", "ion vapor") to CoatingType.AERO_IVD,
        listOf("xylan", "ptfe") to CoatingType.PTFE_XYLAN,
        listOf("zinc", "జింక్", "जिंक", "zinc", "zink", "цинк", "镀锌", "亜鉛") to CoatingType.ZINC_BLUE
    )
    private val CADMIUM = listOf("cadmium", "కాడ్మియం", "క్యాడ్మియం", "कैडमियम", "cadmio", "kadmium", "кадми", "镉", "カドミ")

    private val HEADER = listOf(
        listOf("1 die 2 blow", "one die two blow", "1 die two blow", "one die 2 blow", "1-die 2-blow", "2 blow", "two blow", "వన్ డై", "టూ బ్లో", "1 డై 2 బ్లో", "वन डाई", "टू ब्लो", "1 डाई 2 ब्लो", "dos golpes", "zwei schlag", "doppeldruck", "deux coups", "двухудар", "双击", "2ブロー") to HeaderChoice.TWO_BLOW,
        listOf("multi station", "multistation", "multi-station", "multi die", "progressive", "మల్టీ స్టేషన్", "మల్టీ డై", "ప్రోగ్రెసివ్", "मल्टी स्टेशन", "प्रोग्रेसिव", "multiestación", "mehrstufen", "multipostes", "многопозиц", "多工位", "多段") to HeaderChoice.MULTI_STATION,
        listOf("auto machine", "machine auto", "automatic machine", "ఆటో మెషిన్", "मशीन ऑटो", "ऑटो मशीन") to HeaderChoice.AUTO
    )

    /** Most specific first: "flange nut" before "nut", "pan torx" before "pan". */
    private val TYPES = listOf(
        listOf("torx pan", "pan torx", "pan head torx", "hexalobular pan", "pan hexalobular", "టార్క్స్ పాన్", "పాన్ హెడ్ టార్క్స్", "टॉर्क्स पैन") to FastenerType.PAN_HEXALOBULAR,
        listOf("pan head slot", "slotted pan") to FastenerType.PAN_SLOT,
        listOf("pan head", "pan screw", "పాన్ హెడ్", "पैन हेड", "cabeza alomada", "linsenkopf", "tête cylindrique bombée", "полукруглой голов", "盘头", "なべ") to FastenerType.PAN_CROSS,
        listOf("flange nut", "ఫ్లాంజ్ నట్", "फ्लैंज नट") to FastenerType.FLANGE_NUT,
        listOf("nyloc", "nylock", "nylon nut", "నైలాక్", "नायलॉक") to FastenerType.NYLOC_NUT,
        listOf("flange bolt", "flange screw", "hex flange", "ఫ్లాంజ్ బోల్ట్", "ఫ్లాంజ్", "फ्लैंज बोल्ट", "perno con brida", "flanschschraube", "vis à embase", "болт с фланцем", "法兰螺栓", "フランジボルト") to FastenerType.FLANGE_BOLT,
        listOf("12 point", "twelve point", "12-point", "ట్వెల్వ్ పాయింట్") to FastenerType.TWELVE_POINT,
        listOf("nas bolt", "aircraft bolt", "close tolerance", "ఎయిర్‌క్రాఫ్ట్ బోల్ట్") to FastenerType.NAS_CLOSE_TOL_BOLT,
        listOf("socket head", "allen", "shcs", "cap screw", "సాకెట్ హెడ్", "అలెన్", "एलन", "सॉकेट हेड", "allen", "innensechskant", "vis chc", "с внутренним шестигран", "内六角", "六角穴付") to FastenerType.SHCS,
        listOf("button head", "బటన్ హెడ్") to FastenerType.SOCKET_BUTTON,
        listOf("countersunk", "csk", "కౌంటర్‌సంక్", "కౌంటర్ సంక్", "काउंटरसंक", "avellanad", "senkkopf", "tête fraisée", "потайн", "沉头", "皿") to FastenerType.SOCKET_CSK,
        listOf("set screw", "grub screw", "సెట్ స్క్రూ", "ग्रब") to FastenerType.SOCKET_SET,
        listOf("full thread", "tap bolt", "ఫుల్ థ్రెడ్", "फुल थ्रेड") to FastenerType.HEX_BOLT_FULL,
        listOf("stud", "స్టడ్", "स्टड", "espárrago", "stiftschraube", "goujon", "шпильк", "双头螺柱", "スタッド") to FastenerType.STUD,
        listOf("washer", "వాషర్", "वॉशर", "arandela", "scheibe", "rondelle", "шайб", "垫圈", "ワッシャ") to FastenerType.FLAT_WASHER,
        listOf("rivet", "రివెట్", "रिवेट", "remache", "niete", "rivet", "заклёп", "заклеп", "铆钉", "リベット") to FastenerType.SOLID_RIVET,
        listOf("nut", "నట్", "नट", "tuerca", "mutter", "écrou", "гайк", "螺母", "ナット") to FastenerType.HEX_NUT,
        listOf("hex bolt", "bolt", "హెక్స్ బోల్ట్", "బోల్ట్", "हेक्स बोल्ट", "बोल्ट", "perno", "tornillo hexagonal", "sechskantschraube", "schraube", "boulon", "vis hexagonale", "болт", "螺栓", "ボルト") to FastenerType.HEX_BOLT
    )

    private val QUERY_WORDS = mapOf(
        JarvisQuery.CUT_OFF to listOf("cut off", "cut-off", "cutoff", "cut length", "slug length", "కట్ ఆఫ్", "కటాఫ్", "కట్ఆఫ్", "కట్ లెంగ్త్", "कट ऑफ", "कटऑफ", "कट लेंथ", "corte", "ablänge", "abschnitt", "schnittlänge", "longueur de coupe", "débit", "отрезк", "длина заготовки", "切断", "下料", "カットオフ"),
        JarvisQuery.WEIGHT to listOf("weight", "how heavy", "raw material", "wire", "kg", "kilo", "బరువు", "వెయిట్", "వైర్", "కిలో", "మెటీరియల్ ఎంత", "वजन", "वज़न", "तार", "किलो", "peso", "alambre", "gewicht", "draht", "poids", "fil", "вес", "проволок", "重量", "线材", "重さ", "線材"),
        JarvisQuery.BLANK to listOf("blank", "rolling", "pitch dia", "బ్లాంక్", "రోలింగ్", "ब्लैंक", "रोलिंग", "rodado", "rollier", "roulage", "накатк", "滚丝", "転造"),
        JarvisQuery.HARDNESS to listOf("hardness", "heat treat", "temper", "hrc", "హార్డ్‌నెస్", "హార్డ్నెస్", "హీట్ ట్రీట్", "టెంపర్", "कठोरता", "हार्डनेस", "हीट ट्रीट", "dureza", "tratamiento térmico", "härte", "wärmebehandlung", "dureté", "traitement thermique", "твердост", "термообработ", "硬度", "热处理", "熱処理"),
        JarvisQuery.MACHINE to listOf("which machine", "machine", "header", "former", "die progression", "మెషిన్", "హెడర్", "మెషీన్", "मशीन", "हेडर", "máquina", "maschine", "presse", "machine", "станок", "автомат", "机器", "冷镦机", "マシン", "圧造機"),
        JarvisQuery.STATUS to listOf("status", "poka", "check", "problem", "issue", "ok?", "release", "స్టేటస్", "సమస్య", "చెక్", "ప్రాబ్లమ్", "स्टेटस", "समस्या", "जांच", "चेक", "estado", "problema", "prüf", "problem", "état", "problème", "проверк", "проблем", "状态", "问题", "状態", "問題")
    )
    private val ACTION_WORDS = listOf(
        JarvisAction.SHARE_REPORT to listOf("report", "రిపోర్ట్", "నివేదిక", "रिपोर्ट", "informe", "reporte", "bericht", "rapport", "отчёт", "отчет", "报告", "レポート"),
        JarvisAction.OPEN_SCANNER to listOf("open scanner", "scan drawing", "scanner", "స్కానర్", "స్కాన్", "स्कैनर", "स्कैन", "escáner", "scanner", "сканер", "扫描", "スキャン"),
        JarvisAction.OPEN_FORGING to listOf("open forging", "forging tab", "heat treatment tab", "ఫోర్జింగ్", "फोर्जिंग"),
        JarvisAction.OPEN_SIZING to listOf("open sizing", "sizing tab", "సైజింగ్", "साइजिंग"),
        JarvisAction.OPEN_COPILOT to listOf("open co-pilot", "open copilot", "కో-పైలట్", "कोपायलट")
    )

    private val wordCache = HashMap<String, Regex>()

    /** Latin words match whole words (plural allowed) so "nut" is not found in "minute"; other scripts match as written. */
    internal fun has(t: String, w: String): Boolean {
        if (w.any { it.isLetter() && it.code > 0x24F } || w.any { !it.isLetterOrDigit() && it != ' ' && it != '-' }) return w in t
        val re = wordCache.getOrPut(w) { Regex("(?<![\\p{L}\\d])" + Regex.escape(w) + "(?:s|es)?(?![\\p{L}\\d])") }
        return re.containsMatchIn(t)
    }

    fun parse(raw: String, currentUnit: UnitSystem = UnitSystem.METRIC): JarvisIntent {
        var t = " " + normalizeDigits(raw).lowercase(Locale.ROOT) + " "
        WAKE_WORDS.forEach { t = t.replace(it, " ") }
        t = t.replace(Regex("""(\d),(\d{3})"""), "$1$2").replace(Regex("""(\d)\s+(\d{3})(?!\d)"""), "$1$2")
        // "10 point 9" → "10.9"
        t = t.replace(Regex("""(\d{1,2})\s*(?:point|పాయింట్|पॉइंट|punto|punkt|точка|点)\s*(\d)"""), "$1.$2")
        fun cut(m: MatchResult) { t = t.replaceRange(m.range, " ".repeat(m.value.length)) }

        val tensile = Regex("""\d+(?:\.\d+)?\s*(?:-|–|to|టు|से|a|bis|à|до|至|から)?\s*\d*(?:\.\d+)?\s*(?:mpa|ksi|n/mm)""").find(t)?.let { m ->
            TensileRange.parse(m.value)?.also { cut(m) }
        }
        var pc: PropertyClass? = null
        Regex("""(?<![\d.])(4\.6|4\.8|5\.6|5\.8|6\.8|8\.8|9\.8|10\.9|12\.9)(?![\d])""").find(t)?.let { pc = CLASS_WORDS[it.groupValues[1]]; cut(it) }
        Regex("""grade\s*(2|5|8)\b""").find(t)?.let {
            pc = when (it.groupValues[1]) { "8" -> PropertyClass.SAE_GRADE_8; "5" -> PropertyClass.SAE_GRADE_5; else -> PropertyClass.SAE_GRADE_2 }; cut(it)
        }

        var batch: Int? = null
        (Regex("""(\d{2,7})\s*(?:$PIECE_WORDS)""").find(t) ?: Regex("""(?:$QTY_WORDS)\s*(?:of|is|=|:)?\s*(\d{2,7})""").find(t))?.let {
            batch = it.groupValues[1].toIntOrNull(); cut(it)
        }

        var threadLen: Double? = null
        Regex("""(?:thread length|thread|థ్రెడ్ లెంగ్త్|థ్రెడ్|थ्रेड लेंथ|थ्रेड|longitud de rosca|gewindelänge|longueur filetée|длина резьбы|螺纹长度|ねじ長さ)\s*(?:is|=|:|b)?\s*(\d+(?:\.\d+)?)""").find(t)?.let {
            threadLen = it.groupValues[1].toDoubleOrNull(); cut(it)
        }

        // Size: metric M-size, or an inch number / fraction size
        var unit: UnitSystem? = null
        var dia: Double? = null
        var pitch: Double? = null
        var length: Double? = null
        Regex("""(?<!\p{L})(?:m|ఎం|ఎమ్|एम|м)\s?(\d{1,2}(?:\.\d+)?)(?![\d.])(?:\s*(?:x|by|బై|बाय|into)\s*(\d(?:\.\d+)?)(?=\s*(?:x|by|బై|बाय|into)\s*\d))?""").find(t)?.let { m ->
            unit = UnitSystem.METRIC; dia = m.groupValues[1].toDoubleOrNull()
            m.groupValues[2].toDoubleOrNull()?.let { pitch = it }
            cut(m)
        }
        if (dia == null) {
            Regex("""(?:#|number|no\.|నంబర్|नंबर)\s?(\d{1,2})(?:\s*-\s*(\d{2}))?(?![\d/])""").find(t)?.let { m ->
                val n = m.groupValues[1].toInt()
                FastenerCatalog.INCH_SIZES.firstOrNull { it.label.startsWith("#$n ") }?.let { s ->
                    unit = UnitSystem.INCH; dia = s.dInch * 25.4; m.groupValues[2].toDoubleOrNull()?.let { pitch = it }; cut(m)
                }
            }
        }
        if (dia == null) {
            Regex("""(?<![\d/.])(\d{1,2})\s*/\s*(2|4|8|16)(?![\d/])(?:\s*(?:inch|in\b|"|ఇంచ్|इंच))?(?:\s*-\s*(\d{2}))?""").find(t)?.let { m ->
                val v = m.groupValues[1].toDouble() / m.groupValues[2].toDouble()
                if (v <= 1.5) {
                    unit = UnitSystem.INCH; dia = v * 25.4; m.groupValues[3].toDoubleOrNull()?.let { pitch = it }; cut(m)
                }
            }
        }
        val lengthUnit = unit ?: currentUnit
        fun toLen(v: Double, explicitMm: Boolean) = if (lengthUnit == UnitSystem.INCH && !explicitMm && v <= 12.0) v * 25.4 else v
        (Regex("""(?:^|\s)(?:x|by|into|బై|बाय|इंटू|इनटू|por|mal|fois|на|乘|かける)\s*(\d+(?:\.\d+)?)(\s*(?:mm|ఎంఎం|मिमी))?""").find(t)
            ?: Regex("""(?:length|long|లెంగ్త్|పొడవు|लंबाई|लेंथ|longitud|largo|länge|longueur|длина|长度|長さ)\s*(?:is|=|:|l)?\s*(\d+(?:\.\d+)?)(\s*(?:mm|ఎంఎం|मिमी))?""").find(t)
            ?: Regex("""(\d+(?:\.\d+)?)(\s*(?:mm|millimet|మిల్లీమీటర్|ఎంఎం|मिलीमीटर|मिमी|milímetro|millimeter|millimètre|миллиметр|毫米|ミリ))(?:\s*(?:long|length|పొడవు|लंबा))?""").find(t))?.let { m ->
            length = m.groupValues[1].toDoubleOrNull()?.let { toLen(it, m.groupValues[2].isNotBlank()) }; cut(m)
        }

        // Material: a catalogue key said as one token or two (10 b21, ss 304, inconel 718)
        val tokens = t.uppercase(Locale.ROOT).split(Regex("""[^A-Z0-9]+""")).filter { it.isNotBlank() }
        val keys = FastenerCatalog.MATERIALS.keys.sortedByDescending { it.length }
        fun norm(k: String) = k.uppercase(Locale.ROOT).replace(Regex("""[^A-Z0-9]"""), "")
        var material: String? = keys.firstOrNull { k ->
            val nk = norm(k)
            nk.length >= 4 && (tokens.any { it == nk } || tokens.zipWithNext().any { (a, b) -> a + b == nk })
        }
        if (material == null && Regex("""titanium|టైటానియం|टाइटेनियम|titanio|titan|титан|钛|チタン""").containsMatchIn(t)) material = "TI_GR5".takeIf { it in FastenerCatalog.MATERIALS }

        var coating: CoatingType? = null
        if (CADMIUM.any { has(t, it) }) {
            coating = when {
                Regex("""class\s*2|క్లాస్\s*2|क्लास\s*2""").containsMatchIn(t) -> CoatingType.AERO_CAD_CL2
                Regex("""class\s*3|క్లాస్\s*3|क्लास\s*3""").containsMatchIn(t) -> CoatingType.AERO_CAD_CL3
                else -> CoatingType.AERO_CAD
            }
        } else {
            coating = COATINGS.firstOrNull { (words, _) -> words.any { has(t, it) } }?.second
        }

        val header = HEADER.firstOrNull { (words, _) -> words.any { has(t, it) } }?.second
        val patch = when {
            Regex("""(?:no|without|remove|వద్దు|बिना|sin|ohne|sans|без|不要|なし)\s*\S*\s*(?:patch|ప్యాచ్|पैच|parche|patch|патч)|(?:patch|ప్యాచ్|पैच)\s*(?:వద్దు|नहीं|off)""").containsMatchIn(t) -> false
            Regex("""patch|ప్యాచ్|पैच|parche|патч|尼龙|ナイロン""").containsMatchIn(t) -> true
            else -> null
        }
        val type = TYPES.firstOrNull { (words, _) -> words.any { w -> has(t, w) } }?.second

        val queries = QUERY_WORDS.filter { (_, words) -> words.any { has(t, it) } }.keys
            .let { q -> if (header != null) q - JarvisQuery.MACHINE else q }
        val action = ACTION_WORDS.firstOrNull { (_, words) -> words.any { has(t, it) } }?.first

        return JarvisIntent(
            type = type, unit = unit, diaMm = dia, pitch = pitch, lengthMm = length, threadLengthMm = threadLen,
            materialKey = material, propertyClass = pc, tensileRange = tensile, coating = coating, batchQty = batch,
            headerChoice = header, threadLockPatch = patch, queries = queries, action = action
        )
    }

    /** The JSON Gemini returns for a sentence the offline parser could not fully read. */
    fun fromJson(o: JSONObject): JarvisIntent {
        fun num(k: String) = o.optDouble(k, Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
        fun str(k: String) = o.optString(k, "").trim().takeIf { it.isNotEmpty() && it != "null" }
        fun norm(s: String) = s.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        val unit = str("unitSystem")?.let { if ("INCH" in it.uppercase(Locale.ROOT)) UnitSystem.INCH else UnitSystem.METRIC }
        val pc = str("propertyClass")?.let { s -> CLASS_WORDS[s] ?: PropertyClass.values().firstOrNull { it.name == norm(s) } }
        val list = (o.optJSONArray("queries") ?: JSONArray())
        val queries = (0 until list.length()).mapNotNull { i -> JarvisQuery.values().firstOrNull { it.name == norm(list.optString(i)) } }.toSet()
        val patch = if (o.has("threadLockPatch") && !o.isNull("threadLockPatch")) o.optBoolean("threadLockPatch") else null
        return JarvisIntent(
            type = str("fastenerType")?.let { s -> FastenerType.values().firstOrNull { it.name == norm(s) } },
            unit = unit,
            diaMm = num("nominalDiameterMm"),
            pitch = num("pitch"),
            lengthMm = num("lengthMm"),
            threadLengthMm = num("threadLengthMm"),
            materialKey = str("materialKey")?.let { s -> FastenerCatalog.MATERIALS.keys.firstOrNull { norm(it) == norm(s) } },
            propertyClass = pc,
            tensileRange = TensileRange.of(num("tensileMinMpa"), num("tensileMaxMpa")),
            coating = str("coating")?.let { s -> CoatingType.values().firstOrNull { it.name == norm(s) } },
            batchQty = num("batchQty")?.toInt(),
            headerChoice = str("headerChoice")?.let { s -> HeaderChoice.values().firstOrNull { it.name == norm(s) } },
            threadLockPatch = patch,
            queries = queries,
            action = str("action")?.let { s -> JarvisAction.values().firstOrNull { it.name == norm(s) } }
        )
    }
}

/** Spoken replies in the app's nine languages, built from the app's own figures. */
object JarvisReply {
    private fun n(v: Double, dp: Int = 1) = String.format(Locale.US, "%.${dp}f", v)

    /** BCP-47 tag for speech recognition and text-to-speech. */
    fun localeTag(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "en-IN"; AppLanguage.HI -> "hi-IN"; AppLanguage.TE -> "te-IN"; AppLanguage.ES -> "es-ES"
        AppLanguage.DE -> "de-DE"; AppLanguage.FR -> "fr-FR"; AppLanguage.RU -> "ru-RU"; AppLanguage.ZH -> "zh-CN"; AppLanguage.JA -> "ja-JP"
    }

    private data class Units(val mm: String, val g: String, val kg: String, val pcs: String)
    private fun units(l: AppLanguage) = when (l) {
        AppLanguage.EN -> Units("millimetres", "grams", "kilograms", "pieces")
        AppLanguage.TE -> Units("మిల్లీమీటర్లు", "గ్రాములు", "కిలోలు", "పీసులు")
        AppLanguage.HI -> Units("मिलीमीटर", "ग्राम", "किलो", "पीस")
        AppLanguage.ES -> Units("milímetros", "gramos", "kilos", "piezas")
        AppLanguage.DE -> Units("Millimeter", "Gramm", "Kilogramm", "Stück")
        AppLanguage.FR -> Units("millimètres", "grammes", "kilos", "pièces")
        AppLanguage.RU -> Units("миллиметров", "грамм", "килограммов", "штук")
        AppLanguage.ZH -> Units("毫米", "克", "公斤", "件")
        AppLanguage.JA -> Units("ミリ", "グラム", "キロ", "個")
    }

    fun listening(l: AppLanguage) = when (l) {
        AppLanguage.EN -> "Jarvis is listening…"; AppLanguage.TE -> "జార్విస్ వింటోంది…"; AppLanguage.HI -> "जार्विस सुन रहा है…"
        AppLanguage.ES -> "Jarvis escucha…"; AppLanguage.DE -> "Jarvis hört zu…"; AppLanguage.FR -> "Jarvis écoute…"
        AppLanguage.RU -> "Джарвис слушает…"; AppLanguage.ZH -> "贾维斯正在聆听…"; AppLanguage.JA -> "ジャービスが聞いています…"
    }

    fun notUnderstood(l: AppLanguage) = when (l) {
        AppLanguage.EN -> "Sorry, I did not catch that. Say for example: M10 by 60 hex bolt, 4135, 10.9, zinc nickel, 13000 pieces."
        AppLanguage.TE -> "క్షమించండి, అర్థం కాలేదు. ఇలా చెప్పండి: M10 బై 60 హెక్స్ బోల్ట్, 4135, 10.9, జింక్ నికెల్, 13000 పీసులు."
        AppLanguage.HI -> "माफ़ कीजिए, समझ नहीं आया। ऐसे बोलिए: M10 बाय 60 हेक्स बोल्ट, 4135, 10.9, जिंक निकल, 13000 पीस।"
        AppLanguage.ES -> "Perdón, no lo entendí. Diga por ejemplo: M10 por 60 perno hexagonal, 4135, 10.9, zinc níquel, 13000 piezas."
        AppLanguage.DE -> "Entschuldigung, nicht verstanden. Sagen Sie z. B.: M10 mal 60 Sechskantschraube, 4135, 10.9, Zink-Nickel, 13000 Stück."
        AppLanguage.FR -> "Désolé, je n'ai pas compris. Dites par exemple : M10 fois 60 boulon, 4135, 10.9, zinc nickel, 13000 pièces."
        AppLanguage.RU -> "Извините, не понял. Скажите, например: M10 на 60 болт, 4135, 10.9, цинк никель, 13000 штук."
        AppLanguage.ZH -> "抱歉，没有听清。例如说：M10 乘 60 螺栓，4135，10.9，锌镍，13000 件。"
        AppLanguage.JA -> "すみません、聞き取れませんでした。例：M10 かける 60 ボルト、4135、10.9、亜鉛ニッケル、13000 個。"
    }

    fun sharingReport(l: AppLanguage) = when (l) {
        AppLanguage.EN -> "Sharing the production report."; AppLanguage.TE -> "ప్రొడక్షన్ రిపోర్ట్ పంపుతున్నాను."; AppLanguage.HI -> "प्रोडक्शन रिपोर्ट भेज रहा हूँ।"
        AppLanguage.ES -> "Compartiendo el informe de producción."; AppLanguage.DE -> "Ich teile den Produktionsbericht."; AppLanguage.FR -> "Je partage le rapport de production."
        AppLanguage.RU -> "Отправляю производственный отчёт."; AppLanguage.ZH -> "正在分享生产报告。"; AppLanguage.JA -> "生産レポートを共有します。"
    }

    fun opening(l: AppLanguage, what: String) = when (l) {
        AppLanguage.EN -> "Opening $what."; AppLanguage.TE -> "$what తెరుస్తున్నాను."; AppLanguage.HI -> "$what खोल रहा हूँ।"
        AppLanguage.ES -> "Abriendo $what."; AppLanguage.DE -> "Öffne $what."; AppLanguage.FR -> "J'ouvre $what."
        AppLanguage.RU -> "Открываю $what."; AppLanguage.ZH -> "正在打开$what。"; AppLanguage.JA -> "$what を開きます。"
    }

    fun machineName(l: AppLanguage, plan: HeadingPlan): String = when (plan.route) {
        HeaderRoute.TWO_BLOW -> when (l) {
            AppLanguage.EN -> "1-die 2-blow header"; AppLanguage.TE -> "వన్ డై టూ బ్లో హెడర్"; AppLanguage.HI -> "वन डाई टू ब्लो हेडर"
            AppLanguage.ES -> "cabezadora de 1 matriz y 2 golpes"; AppLanguage.DE -> "1-Matrizen-2-Schlag-Presse"; AppLanguage.FR -> "frappeuse 1 matrice 2 coups"
            AppLanguage.RU -> "одноматричный двухударный автомат"; AppLanguage.ZH -> "单模双击冷镦机"; AppLanguage.JA -> "1ダイ2ブロー圧造機"
        }
        HeaderRoute.MULTI_STATION -> plan.dieStations.let { d ->
            when (l) {
                AppLanguage.EN -> "$d-die progressive former"; AppLanguage.TE -> "$d డై ప్రోగ్రెసివ్ ఫార్మర్"; AppLanguage.HI -> "$d डाई प्रोग्रेसिव फॉर्मर"
                AppLanguage.ES -> "formadora progresiva de $d matrices"; AppLanguage.DE -> "$d-stufige Mehrstufenpresse"; AppLanguage.FR -> "presse multipostes à $d matrices"
                AppLanguage.RU -> "$d-позиционный автомат"; AppLanguage.ZH -> "${d}工位冷镦机"; AppLanguage.JA -> "${d}段フォーマー"
            }
        }
        else -> plan.machineDescription
    }

    /** The answer to [intent] for the part as it now stands in [r]. */
    fun build(intent: JarvisIntent, r: FastenerCalculationResult, l: AppLanguage): String {
        val u = units(l)
        val s = ProductionReport.summary(r)
        val plan = r.headingPlan
        val parts = mutableListOf<String>()
        if (intent.changesSetup) {
            // Only what was said is reported as set; everything else is named as unchanged, never passed off as heard
            val strength = r.tensileRange?.label ?: r.propertyClass?.label?.substringBefore(" (") ?: ""
            val said = mutableListOf<String>()
            val kept = mutableListOf<String>()
            fun put(wasSaid: Boolean, text: String) { if (text.isNotBlank()) (if (wasSaid) said else kept) += text }
            put(intent.diaMm != null || intent.unit != null || intent.pitch != null, r.threadLabel)
            put(intent.lengthMm != null, "L ${n(r.lengthMm)}")
            put(intent.type != null, r.fastenerType.label.substringBefore(" ("))
            put(intent.materialKey != null, r.material.key)
            put(intent.propertyClass != null || intent.tensileRange != null, strength)
            put(intent.coating != null, r.coating.label.substringBefore(" ("))
            put(intent.batchQty != null, "${r.batchQty} ${u.pcs}")
            if (intent.threadLengthMm != null) said += "b ${n(r.threadLengthMm)}"
            if (intent.threadLockPatch != null) said += if (r.threadLockPatch) "patch" else "no patch"
            if (said.isNotEmpty()) parts += said.joinToString(", ").let { set ->
                when (l) {
                    AppLanguage.EN -> "Set: $set."; AppLanguage.TE -> "సెట్ చేశాను: $set."; AppLanguage.HI -> "सेट किया: $set।"
                    AppLanguage.ES -> "Configurado: $set."; AppLanguage.DE -> "Eingestellt: $set."; AppLanguage.FR -> "Réglé : $set."
                    AppLanguage.RU -> "Установлено: $set."; AppLanguage.ZH -> "已设置：$set。"; AppLanguage.JA -> "設定しました：$set。"
                }
            }
            if (kept.isNotEmpty()) parts += kept.joinToString(", ").let { k ->
                when (l) {
                    AppLanguage.EN -> "Unchanged: $k."; AppLanguage.TE -> "మారలేదు: $k."; AppLanguage.HI -> "पहले जैसा: $k।"
                    AppLanguage.ES -> "Sin cambios: $k."; AppLanguage.DE -> "Unverändert: $k."; AppLanguage.FR -> "Inchangé : $k."
                    AppLanguage.RU -> "Без изменений: $k."; AppLanguage.ZH -> "未更改：$k。"; AppLanguage.JA -> "変更なし：$k。"
                }
            }
        }
        // After a change with no question, the two numbers a planner wants are the cut-off and the weight
        val q = if (intent.queries.isEmpty() && intent.changesSetup) setOf(JarvisQuery.CUT_OFF, JarvisQuery.WEIGHT) else intent.queries
        if (JarvisQuery.CUT_OFF in q || JarvisQuery.MACHINE in q || intent.headerChoice != null) {
            val cut = n(r.cutOffLengthMm); val wire = n(r.wireDiaMm, 2)
            parts += when (l) {
                AppLanguage.EN -> "Cut-off length $cut ${u.mm} on $wire ${u.mm} wire."
                AppLanguage.TE -> "కట్ ఆఫ్ లెంగ్త్ $cut ${u.mm}, వైర్ $wire ${u.mm}."
                AppLanguage.HI -> "कट ऑफ लंबाई $cut ${u.mm}, तार $wire ${u.mm}।"
                AppLanguage.ES -> "Longitud de corte $cut ${u.mm} con alambre de $wire ${u.mm}."
                AppLanguage.DE -> "Ablänge $cut ${u.mm} bei $wire ${u.mm} Draht."
                AppLanguage.FR -> "Longueur de coupe $cut ${u.mm} sur fil de $wire ${u.mm}."
                AppLanguage.RU -> "Длина отрезки $cut ${u.mm}, проволока $wire ${u.mm}."
                AppLanguage.ZH -> "下料长度 $cut ${u.mm}，线材 $wire ${u.mm}。"
                AppLanguage.JA -> "切断長 $cut ${u.mm}、線材 $wire ${u.mm}。"
            }
        }
        if (plan != null && (JarvisQuery.MACHINE in q || intent.headerChoice != null)) {
            val m = machineName(l, plan)
            parts += when (l) {
                AppLanguage.EN -> "Machine: $m."; AppLanguage.TE -> "మెషిన్: $m."; AppLanguage.HI -> "मशीन: $m।"
                AppLanguage.ES -> "Máquina: $m."; AppLanguage.DE -> "Maschine: $m."; AppLanguage.FR -> "Machine : $m."
                AppLanguage.RU -> "Оборудование: $m."; AppLanguage.ZH -> "设备：$m。"; AppLanguage.JA -> "設備：$m。"
            }
            plan.feasibilityIssue?.let { parts += "⚠ $it" }
        }
        if (JarvisQuery.WEIGHT in q) {
            val fin = n(s.finishedWeightG, 2); val slug = n(s.slugWeightG, 2); val kg = n(s.batchRawMaterialKg); val b = s.batchQty
            parts += when (l) {
                AppLanguage.EN -> "Finished weight $fin ${u.g}, slug $slug ${u.g}. $b ${u.pcs} need $kg ${u.kg} of wire."
                AppLanguage.TE -> "ఫినిష్డ్ బరువు $fin ${u.g}, స్లగ్ $slug ${u.g}. $b ${u.pcs}కి $kg ${u.kg} వైర్ కావాలి."
                AppLanguage.HI -> "तैयार वजन $fin ${u.g}, स्लग $slug ${u.g}। $b ${u.pcs} के लिए $kg ${u.kg} तार चाहिए।"
                AppLanguage.ES -> "Peso terminado $fin ${u.g}, tocho $slug ${u.g}. $b ${u.pcs} necesitan $kg ${u.kg} de alambre."
                AppLanguage.DE -> "Fertiggewicht $fin ${u.g}, Abschnitt $slug ${u.g}. $b ${u.pcs} brauchen $kg ${u.kg} Draht."
                AppLanguage.FR -> "Poids fini $fin ${u.g}, lopin $slug ${u.g}. $b ${u.pcs} demandent $kg ${u.kg} de fil."
                AppLanguage.RU -> "Готовый вес $fin ${u.g}, заготовка $slug ${u.g}. На $b ${u.pcs} нужно $kg ${u.kg} проволоки."
                AppLanguage.ZH -> "成品重量 $fin ${u.g}，坯料 $slug ${u.g}。$b ${u.pcs}需要线材 $kg ${u.kg}。"
                AppLanguage.JA -> "完成重量 $fin ${u.g}、スラグ $slug ${u.g}。$b ${u.pcs}で線材 $kg ${u.kg} 必要です。"
            }
        }
        if (JarvisQuery.BLANK in q && !r.fastenerType.isUnthreaded) {
            val lo = n(r.blankLowerLimit, 3); val hi = n(r.blankUpperLimit, 3)
            parts += when (l) {
                AppLanguage.EN -> "Rolling blank $lo to $hi ${u.mm}."; AppLanguage.TE -> "రోలింగ్ బ్లాంక్ $lo నుండి $hi ${u.mm}."
                AppLanguage.HI -> "रोलिंग ब्लैंक $lo से $hi ${u.mm}।"; AppLanguage.ES -> "Diámetro previo al rodado $lo a $hi ${u.mm}."
                AppLanguage.DE -> "Rollrohling $lo bis $hi ${u.mm}."; AppLanguage.FR -> "Ébauche de roulage $lo à $hi ${u.mm}."
                AppLanguage.RU -> "Заготовка под накатку $lo – $hi ${u.mm}."; AppLanguage.ZH -> "滚丝坯径 $lo 至 $hi ${u.mm}。"
                AppLanguage.JA -> "転造ブランク $lo ～ $hi ${u.mm}。"
            }
        }
        if (JarvisQuery.HARDNESS in q) {
            val ht = r.heatTreatmentSpec
            parts += when (l) {
                AppLanguage.EN -> "Hardness ${ht.targetHardnessHrc}. Temper ${ht.temperingOrAgingTempC}."
                AppLanguage.TE -> "హార్డ్‌నెస్ ${ht.targetHardnessHrc}. టెంపర్ ${ht.temperingOrAgingTempC}."
                AppLanguage.HI -> "कठोरता ${ht.targetHardnessHrc}। टेम्पर ${ht.temperingOrAgingTempC}।"
                AppLanguage.ES -> "Dureza ${ht.targetHardnessHrc}. Revenido ${ht.temperingOrAgingTempC}."
                AppLanguage.DE -> "Härte ${ht.targetHardnessHrc}. Anlassen ${ht.temperingOrAgingTempC}."
                AppLanguage.FR -> "Dureté ${ht.targetHardnessHrc}. Revenu ${ht.temperingOrAgingTempC}."
                AppLanguage.RU -> "Твёрдость ${ht.targetHardnessHrc}. Отпуск ${ht.temperingOrAgingTempC}."
                AppLanguage.ZH -> "硬度 ${ht.targetHardnessHrc}。回火 ${ht.temperingOrAgingTempC}。"
                AppLanguage.JA -> "硬さ ${ht.targetHardnessHrc}。焼戻し ${ht.temperingOrAgingTempC}。"
            }
        }
        if (JarvisQuery.STATUS in q) {
            val c = r.criticalIssues.size
            val m = r.pokaYokeIssues.count { it.severity == PokaYokeSeverity.MANDATORY }
            val w = r.pokaYokeIssues.count { it.severity == PokaYokeSeverity.WARNING }
            parts += when (l) {
                AppLanguage.EN -> "Standard check: $c critical, $m mandatory, $w warnings."
                AppLanguage.TE -> "స్టాండర్డ్ చెక్: $c క్రిటికల్, $m తప్పనిసరి, $w హెచ్చరికలు."
                AppLanguage.HI -> "स्टैंडर्ड जांच: $c क्रिटिकल, $m अनिवार्य, $w चेतावनियाँ।"
                AppLanguage.ES -> "Verificación: $c críticos, $m obligatorios, $w avisos."
                AppLanguage.DE -> "Normprüfung: $c kritisch, $m Pflicht, $w Warnungen."
                AppLanguage.FR -> "Contrôle : $c critiques, $m obligatoires, $w avertissements."
                AppLanguage.RU -> "Проверка: $c критических, $m обязательных, $w предупреждений."
                AppLanguage.ZH -> "标准检查：$c 项严重，$m 项必须，$w 项警告。"
                AppLanguage.JA -> "規格チェック：重大 $c、必須 $m、警告 $w。"
            }
            r.criticalIssues.firstOrNull()?.let { parts += "⚠ ${it.title}." }
        }
        if (intent.action == JarvisAction.SHARE_REPORT) parts += sharingReport(l)
        return parts.joinToString(" ")
    }

    /** Text for the speaker: no markdown, bullets or emoji. */
    fun forSpeech(text: String): String = text
        .replace(Regex("""[*_#`>|]+"""), " ")
        .replace(Regex("""[•▪●◆✓✔✗⚠️🤖💡✈✅❌📌🔧]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}
