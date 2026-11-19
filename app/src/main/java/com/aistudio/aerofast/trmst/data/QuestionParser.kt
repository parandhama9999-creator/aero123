package com.aistudio.aerofast.trmst.data

import com.aistudio.aerofast.trmst.util.AppLanguage
import java.util.Locale

enum class QuestionTopic { CUT_OFF, WEIGHT, BLANK, HARDNESS, MACHINE, STATUS }

/** What a typed question names (part, material, class …) and asks about. A null field was not said. */
data class ParsedQuestion(
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
    val queries: Set<QuestionTopic> = emptySet()
) {
    val changesSetup: Boolean
        get() = listOf(type, unit, diaMm, pitch, lengthMm, threadLengthMm, materialKey, propertyClass, tensileRange, coating, batchQty, headerChoice, threadLockPatch)
            .any { it != null }
    val isEmpty: Boolean get() = !changesSetup && queries.isEmpty()

    /** This intent's values first, the other's where this one is silent. */
    fun orElse(o: ParsedQuestion) = ParsedQuestion(
        type ?: o.type, unit ?: o.unit, diaMm ?: o.diaMm, pitch ?: o.pitch, lengthMm ?: o.lengthMm, threadLengthMm ?: o.threadLengthMm,
        materialKey ?: o.materialKey, propertyClass ?: o.propertyClass, tensileRange ?: o.tensileRange, coating ?: o.coating,
        batchQty ?: o.batchQty, headerChoice ?: o.headerChoice, threadLockPatch ?: o.threadLockPatch, queries + o.queries
    )
}

/**
 * Reads a typed question in any of the app's languages, offline. The engineering words (bolt, cut-off, weight …) are
 * matched in English, Telugu, Hindi, Spanish, German, French, Russian, Chinese and Japanese. Anything it cannot read
 * is left null.
 */
object QuestionParser {
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
        QuestionTopic.CUT_OFF to listOf("cut off", "cut-off", "cutoff", "cut length", "slug length", "కట్ ఆఫ్", "కటాఫ్", "కట్ఆఫ్", "కట్ లెంగ్త్", "कट ऑफ", "कटऑफ", "कट लेंथ", "corte", "ablänge", "abschnitt", "schnittlänge", "longueur de coupe", "débit", "отрезк", "длина заготовки", "切断", "下料", "カットオフ"),
        QuestionTopic.WEIGHT to listOf("weight", "how heavy", "raw material", "wire", "kg", "kilo", "బరువు", "వెయిట్", "వైర్", "కిలో", "మెటీరియల్ ఎంత", "वजन", "वज़न", "तार", "किलो", "peso", "alambre", "gewicht", "draht", "poids", "fil", "вес", "проволок", "重量", "线材", "重さ", "線材"),
        QuestionTopic.BLANK to listOf("blank", "rolling", "pitch dia", "బ్లాంక్", "రోలింగ్", "ब्लैंक", "रोलिंग", "rodado", "rollier", "roulage", "накатк", "滚丝", "転造"),
        QuestionTopic.HARDNESS to listOf("hardness", "heat treat", "temper", "hrc", "హార్డ్‌నెస్", "హార్డ్నెస్", "హీట్ ట్రీట్", "టెంపర్", "कठोरता", "हार्डनेस", "हीट ट्रीट", "dureza", "tratamiento térmico", "härte", "wärmebehandlung", "dureté", "traitement thermique", "твердост", "термообработ", "硬度", "热处理", "熱処理"),
        QuestionTopic.MACHINE to listOf("which machine", "machine", "header", "former", "die progression", "మెషిన్", "హెడర్", "మెషీన్", "मशीन", "हेडर", "máquina", "maschine", "presse", "machine", "станок", "автомат", "机器", "冷镦机", "マシン", "圧造機"),
        QuestionTopic.STATUS to listOf("status", "poka", "check", "problem", "issue", "ok?", "release", "స్టేటస్", "సమస్య", "చెక్", "ప్రాబ్లమ్", "स्टेटस", "समस्या", "जांच", "चेक", "estado", "problema", "prüf", "problem", "état", "problème", "проверк", "проблем", "状态", "问题", "状態", "問題")
    )

    private val wordCache = HashMap<String, Regex>()

    /** Latin words match whole words (plural allowed) so "nut" is not found in "minute"; other scripts match as written. */
    internal fun has(t: String, w: String): Boolean {
        if (w.any { it.isLetter() && it.code > 0x24F } || w.any { !it.isLetterOrDigit() && it != ' ' && it != '-' }) return w in t
        val re = wordCache.getOrPut(w) { Regex("(?<![\\p{L}\\d])" + Regex.escape(w) + "(?:s|es)?(?![\\p{L}\\d])") }
        return re.containsMatchIn(t)
    }

    fun parse(raw: String, currentUnit: UnitSystem = UnitSystem.METRIC): ParsedQuestion {
        var t = " " + normalizeDigits(raw).lowercase(Locale.ROOT) + " "
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
        var sizeEnd = -1
        Regex("""(?<!\p{L})(?:m|ఎం|ఎమ్|एम|м)\s?(\d{1,2}(?:\.\d+)?)(?![\d.])(?:\s*(?:x|by|బై|बाय|into)\s*(\d(?:\.\d+)?)(?=\s*(?:x|by|బై|बाय|into)\s*\d))?""").find(t)?.let { m ->
            unit = UnitSystem.METRIC; dia = m.groupValues[1].toDoubleOrNull()
            m.groupValues[2].toDoubleOrNull()?.let { pitch = it }
            sizeEnd = m.range.last + 1
            cut(m)
        }
        if (dia == null) {
            Regex("""(?:#|number|no\.|నంబర్|नंबर)\s?(\d{1,2})(?:\s*-\s*(\d{2}))?(?![\d/])""").find(t)?.let { m ->
                val n = m.groupValues[1].toInt()
                FastenerCatalog.INCH_SIZES.firstOrNull { it.label.startsWith("#$n ") }?.let { s ->
                    unit = UnitSystem.INCH; dia = s.dInch * 25.4; m.groupValues[2].toDoubleOrNull()?.let { pitch = it }; sizeEnd = m.range.last + 1; cut(m)
                }
            }
        }
        if (dia == null) {
            Regex("""(?<![\d/.])(\d{1,2})\s*/\s*(2|4|8|16)(?![\d/])(?:\s*(?:inch|in\b|"|ఇంచ్|इंच))?(?:\s*-\s*(\d{2}))?""").find(t)?.let { m ->
                val v = m.groupValues[1].toDouble() / m.groupValues[2].toDouble()
                if (v <= 1.5) {
                    unit = UnitSystem.INCH; dia = v * 25.4; m.groupValues[3].toDoubleOrNull()?.let { pitch = it }; sizeEnd = m.range.last + 1; cut(m)
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

        // "M8 25": a plain number right after the size is the length (material keys and batches are 4+ digits or carry a word)
        if (dia != null && length == null && sizeEnd >= 0) {
            Regex("""^\s*(\d{1,3}(?:\.\d+)?)(?![\d.])""").find(t.substring(sizeEnd))?.let { m ->
                val v = m.groupValues[1].toDouble()
                if (v >= 2.0 && v <= 500.0) {
                    length = toLen(v, false)
                    val r = m.groups[1]!!.range
                    t = t.replaceRange(sizeEnd + r.first, sizeEnd + r.last + 1, " ".repeat(r.last - r.first + 1))
                }
            }
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
            .let { q -> if (header != null) q - QuestionTopic.MACHINE else q }

        return ParsedQuestion(
            type = type, unit = unit, diaMm = dia, pitch = pitch, lengthMm = length, threadLengthMm = threadLen,
            materialKey = material, propertyClass = pc, tensileRange = tensile, coating = coating, batchQty = batch,
            headerChoice = header, threadLockPatch = patch, queries = queries
        )
    }
}
