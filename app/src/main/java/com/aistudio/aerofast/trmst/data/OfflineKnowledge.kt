package com.aistudio.aerofast.trmst.data

import android.content.Context
import com.aistudio.aerofast.trmst.util.AppLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow

/** A fact the user has checked and approved, with where it comes from. */
data class ApprovedFact(val id: Long, val question: String, val answer: String, val source: String, val addedAt: Long)

/** The user's own verified knowledge, kept encrypted on the phone. */
object ApprovedFacts {
    private const val PREFS = "aerofast_approved_facts_secure"

    fun load(context: Context): List<ApprovedFact> = try {
        val arr = JSONArray(GeminiAiService.encryptedPrefs(context, PREFS).getString("facts", "[]"))
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map {
            ApprovedFact(it.optLong("id"), it.optString("q"), it.optString("a"), it.optString("s"), it.optLong("t"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun store(context: Context, list: List<ApprovedFact>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("q", it.question).put("a", it.answer).put("s", it.source).put("t", it.addedAt)) }
        GeminiAiService.encryptedPrefs(context, PREFS).edit().putString("facts", arr.toString()).apply()
    }

    fun add(context: Context, question: String, answer: String, source: String): List<ApprovedFact> {
        val now = System.currentTimeMillis()
        val list = load(context) + ApprovedFact(now, question.trim(), answer.trim(), source.trim(), now)
        store(context, list)
        return list
    }

    fun delete(context: Context, id: Long): List<ApprovedFact> {
        val list = load(context).filter { it.id != id }
        store(context, list)
        return list
    }

    private fun tokens(s: String) = s.lowercase(Locale.ROOT).split(Regex("""[^\p{L}\p{N}.]+""")).filter { it.length >= 2 }.toSet()

    /** The fact whose question the user's question covers best: most of its words present, at least two (or all of a short one). */
    fun bestMatch(question: String, facts: List<ApprovedFact>): ApprovedFact? {
        val q = tokens(question)
        if (q.isEmpty()) return null
        return facts.mapNotNull { f ->
            val ft = tokens(f.question)
            if (ft.isEmpty()) return@mapNotNull null
            val hit = ft.count { it in q }
            val score = hit.toDouble() / ft.size
            if (hit >= minOf(2, ft.size) && score >= 0.6) f to score else null
        }.maxByOrNull { it.second }?.first
    }
}

enum class KTopic {
    CUT_OFF, WEIGHT, RAW_MATERIAL, BLANK, THREAD, LOADS, HARDNESS, HEAT_TREAT, ANNEAL, COATING, BAKE,
    PROCESS, STATIONS, MACHINE, STATUS, DENSITY, STRENGTH, PITCH, STANDARD, COST, GCODE, CAN, HELP
}

data class KnowledgeAnswer(val text: String, val sources: List<String>, val found: Boolean) {
    /** The answer with its sources, as shown in the Co-Pilot. */
    fun withSources(lang: AppLanguage): String =
        if (sources.isEmpty()) text else text + "\n\n" + OfflineKnowledge.sourceLabel(lang) + ": " + sources.joinToString("; ")
}

/**
 * Answers without the internet, only from what the app itself holds: its calculations, its data tables, its
 * poka-yoke rules and the user's approved facts. Anything outside that is declined, never guessed.
 */
object OfflineKnowledge {
    private fun n(v: Double, dp: Int = 2) = String.format(Locale.US, "%.${dp}f", v)

    private fun L(l: AppLanguage, en: String, te: String, hi: String, es: String, de: String, fr: String, ru: String, zh: String, ja: String) = when (l) {
        AppLanguage.EN -> en; AppLanguage.TE -> te; AppLanguage.HI -> hi; AppLanguage.ES -> es; AppLanguage.DE -> de
        AppLanguage.FR -> fr; AppLanguage.RU -> ru; AppLanguage.ZH -> zh; AppLanguage.JA -> ja
    }

    fun sourceLabel(l: AppLanguage) = L(l, "Source", "మూలం", "स्रोत", "Fuente", "Quelle", "Source", "Источник", "来源", "出典")

    fun notFound(l: AppLanguage) = L(l,
        "This is not in the offline knowledge base, so I will not guess. Check the drawing or the standard, or add it with 📚 as an approved fact.",
        "ఇది ఆఫ్‌లైన్ నాలెడ్జ్‌లో లేదు, కాబట్టి నేను ఊహించి చెప్పను. డ్రాయింగ్ లేదా స్టాండర్డ్ చూడండి, లేదా 📚 తో ఆమోదించిన ఫ్యాక్ట్‌గా జోడించండి.",
        "यह ऑफ़लाइन नॉलेज में नहीं है, इसलिए मैं अनुमान नहीं लगाऊँगा। ड्रॉइंग या मानक देखें, या 📚 से स्वीकृत तथ्य के रूप में जोड़ें।",
        "Esto no está en la base de conocimiento sin conexión, así que no voy a adivinar. Consulte el plano o la norma, o añádalo con 📚 como dato aprobado.",
        "Das steht nicht in der Offline-Wissensbasis, daher rate ich nicht. Prüfen Sie Zeichnung oder Norm, oder fügen Sie es mit 📚 als geprüfte Angabe hinzu.",
        "Ce n'est pas dans la base de connaissances hors ligne, je ne vais donc pas deviner. Consultez le plan ou la norme, ou ajoutez-le avec 📚 comme fait approuvé.",
        "Этого нет в офлайн-базе знаний, поэтому я не буду угадывать. Проверьте чертёж или стандарт либо добавьте это через 📚 как подтверждённый факт.",
        "离线知识库中没有这项内容，我不会猜测。请查看图纸或标准，或通过 📚 将其添加为已确认的事实。",
        "これはオフライン知識ベースにないため、推測では答えません。図面や規格を確認するか、📚 で承認済みの情報として追加してください。")

    fun help(l: AppLanguage) = L(l,
        "Offline knowledge (no internet): ask about cut-off, weight, raw material, blank, thread limits, loads, hardness, heat treatment, anneal, coating, bake, process, die stations, machine, standard check, cost or G-code — for the current part or another size (e.g. \"M12 x 80 weight\"). Also a material (4140), a class (10.9), a coating (Zn-Ni), a size (M12), a standard, or \"can I …\" checks. Add your own facts with 📚.",
        "ఆఫ్‌లైన్ నాలెడ్జ్ (ఇంటర్నెట్ లేకుండా): కట్ ఆఫ్, బరువు, ముడి పదార్థం, బ్లాంక్, థ్రెడ్ లిమిట్స్, లోడ్స్, హార్డ్‌నెస్, హీట్ ట్రీట్‌మెంట్, అనీల్, కోటింగ్, బేక్, ప్రాసెస్, డై స్టేషన్లు, మెషిన్, స్టాండర్డ్ చెక్, ధర, G-code గురించి అడగండి — ప్రస్తుత పార్ట్ లేదా వేరే సైజ్ (ఉదా: \"M12 x 80 బరువు\"). మెటీరియల్ (4140), క్లాస్ (10.9), కోటింగ్ (Zn-Ni), సైజ్ (M12), స్టాండర్డ్, లేదా \"చేయవచ్చా\" చెక్‌లు కూడా. మీ సొంత ఫ్యాక్ట్స్ 📚 తో జోడించండి.",
        "ऑफ़लाइन नॉलेज (बिना इंटरनेट): कट ऑफ, वजन, कच्चा माल, ब्लैंक, थ्रेड लिमिट, लोड, कठोरता, हीट ट्रीटमेंट, एनील, कोटिंग, बेक, प्रक्रिया, डाई स्टेशन, मशीन, स्टैंडर्ड जांच, लागत या G-code पूछें — मौजूदा पार्ट या दूसरा साइज़ (जैसे \"M12 x 80 वजन\")। मटेरियल (4140), क्लास (10.9), कोटिंग (Zn-Ni), साइज़ (M12), मानक, या \"कर सकते हैं?\" जांच भी। अपने तथ्य 📚 से जोड़ें।",
        "Conocimiento sin conexión: pregunte por corte, peso, materia prima, diámetro de rodado, límites de rosca, cargas, dureza, tratamiento térmico, recocido, recubrimiento, deshidrogenado, proceso, estaciones, máquina, verificación, coste o código G — de la pieza actual u otra medida (p. ej. \"M12 x 80 peso\"). También un material (4140), una clase (10.9), un recubrimiento (Zn-Ni), una medida (M12), una norma o \"¿puedo…?\". Añada sus datos con 📚.",
        "Offline-Wissen: fragen Sie nach Ablänge, Gewicht, Rohmaterial, Rollrohling, Gewindegrenzmaßen, Kräften, Härte, Wärmebehandlung, Weichglühen, Beschichtung, Wasserstoff-Tempern, Ablauf, Stufen, Maschine, Normprüfung, Kosten oder G-Code — für das aktuelle Teil oder eine andere Größe (z. B. \"M12 x 80 Gewicht\"). Auch Werkstoff (4140), Klasse (10.9), Beschichtung (Zn-Ni), Größe (M12), Norm oder \"darf ich …\". Eigene Angaben mit 📚.",
        "Connaissances hors ligne : demandez longueur de coupe, poids, matière, ébauche de roulage, limites de filetage, charges, dureté, traitement thermique, recuit, revêtement, dégazage, gamme, postes, machine, contrôle, coût ou code G — pour la pièce actuelle ou une autre taille (ex. \"M12 x 80 poids\"). Aussi un matériau (4140), une classe (10.9), un revêtement (Zn-Ni), une taille (M12), une norme ou \"puis-je…\". Ajoutez vos faits avec 📚.",
        "Офлайн-знания: спросите про длину отрезки, вес, сырьё, заготовку под накатку, пределы резьбы, нагрузки, твёрдость, термообработку, отжиг, покрытие, обезводороживание, процесс, переходы, оборудование, проверку, стоимость или G-код — для текущей детали или другого размера (например, \"M12 x 80 вес\"). Также материал (4140), класс (10.9), покрытие (Zn-Ni), размер (M12), стандарт или \"можно ли…\". Свои факты — через 📚.",
        "离线知识（无需联网）：可询问下料长度、重量、原材料、滚丝坯径、螺纹极限、载荷、硬度、热处理、退火、涂层、除氢烘烤、工艺、工位、设备、标准检查、成本或 G 代码——针对当前零件或其他规格（如\"M12 x 80 重量\"）。也可查询材料（4140）、等级（10.9）、涂层（Zn-Ni）、规格（M12）、标准或\"可以…吗\"。用 📚 添加您自己的事实。",
        "オフライン知識（インターネット不要）：切断長、重量、原材料、転造ブランク、ねじ限界、荷重、硬さ、熱処理、焼なまし、表面処理、ベーキング、工程、ステーション、設備、規格チェック、コスト、Gコードを質問できます——現在の部品または別サイズ（例：\"M12 x 80 重量\"）。材料（4140）、強度区分（10.9）、表面処理（Zn-Ni）、サイズ（M12）、規格、\"できますか\" も可。独自の情報は 📚 で追加。")

    private val TOPIC_WORDS: Map<KTopic, List<String>> = mapOf(
        KTopic.CUT_OFF to listOf("cut off", "cut-off", "cutoff", "cut length", "slug length", "కట్ ఆఫ్", "కటాఫ్", "కట్ఆఫ్", "కట్ లెంగ్త్", "कट ऑफ", "कटऑफ", "कट लेंथ", "longitud de corte", "corte", "ablänge", "schnittlänge", "longueur de coupe", "отрезк", "下料", "切断"),
        KTopic.WEIGHT to listOf("weight", "heavy", "mass", "grams", "బరువు", "వెయిట్", "वजन", "वज़न", "peso", "gewicht", "poids", "вес", "重量", "重さ"),
        KTopic.RAW_MATERIAL to listOf("raw material", "wire", "kg", "kilo", "coil", "ముడి పదార్థం", "వైర్", "కాయిల్", "కిలో", "कच्चा माल", "तार", "किलो", "materia prima", "alambre", "rohmaterial", "draht", "fil machine", "сырьё", "сырье", "проволок", "原材料", "线材", "線材"),
        KTopic.BLANK to listOf("blank", "rolling", "బ్లాంక్", "రోలింగ్", "ब्लैंक", "रोलिंग", "rodado", "rollier", "roulage", "накат", "滚丝", "転造"),
        KTopic.THREAD to listOf("pitch diameter", "pitch dia", "major dia", "minor dia", "major diameter", "minor diameter", "thread limit", "thread limits", "thread class", "gauge", "go no go", "పిచ్ డయా", "మేజర్", "మైనర్", "థ్రెడ్ లిమిట్", "గేజ్", "मेजर", "माइनर", "गेज", "diámetro medio", "flankendurchmesser", "diamètre sur flancs", "средний диаметр", "中径", "有効径"),
        KTopic.LOADS to listOf("proof load", "breaking load", "load", "loads", "stress area", "kn", "ప్రూఫ్ లోడ్", "బ్రేకింగ్ లోడ్", "లోడ్", "प्रूफ लोड", "ब्रेकिंग लोड", "लोड", "carga", "prüfkraft", "bruchkraft", "charge d'épreuve", "charge de rupture", "нагрузк", "载荷", "荷重"),
        KTopic.HARDNESS to listOf("hardness", "hrc", "hrb", "hv", "హార్డ్‌నెస్", "హార్డ్నెస్", "కఠినత్వం", "कठोरता", "हार्डनेस", "dureza", "härte", "dureté", "твердост", "硬度", "硬さ"),
        KTopic.HEAT_TREAT to listOf("heat treat", "heat treatment", "harden", "hardening", "quench", "temper", "tempering", "austenit", "హీట్ ట్రీట్", "హీట్ ట్రీట్‌మెంట్", "టెంపర్", "క్వెంచ్", "हीट ट्रीट", "हीट ट्रीटमेंट", "टेम्पर", "tratamiento térmico", "temple", "revenido", "wärmebehandlung", "härten", "anlassen", "traitement thermique", "trempe", "revenu", "термообработ", "закалк", "отпуск", "热处理", "淬火", "回火", "熱処理", "焼入れ", "焼戻し"),
        KTopic.ANNEAL to listOf("anneal", "annealing", "spheroid", "spheroidize", "అనీల్", "అనీలింగ్", "స్ఫెరాయిడ్", "एनील", "एनीलिंग", "recocido", "glühen", "weichglühen", "recuit", "отжиг", "退火", "焼なまし"),
        KTopic.COATING to listOf("coating", "plating", "plate", "plated", "finish", "కోటింగ్", "ప్లేటింగ్", "कोटिंग", "प्लेटिंग", "recubrimiento", "beschichtung", "revêtement", "покрыти", "涂层", "电镀", "表面処理", "めっき"),
        KTopic.BAKE to listOf("bake", "baking", "hydrogen", "embrittlement", "బేక్", "బేకింగ్", "హైడ్రోజన్", "बेक", "बेकिंग", "हाइड्रोजन", "deshidrogen", "wasserstoff", "hydrogène", "водород", "除氢", "水素"),
        KTopic.PROCESS to listOf("process", "sequence", "route", "steps", "operations", "ప్రాసెస్", "ప్రక్రియ", "క్రమం", "ఆపరేషన్", "प्रक्रिया", "क्रम", "proceso", "secuencia", "prozess", "ablauf", "processus", "gamme", "процесс", "последовательност", "工艺", "工序", "工程順"),
        KTopic.STATIONS to listOf("station", "stations", "die progression", "dies", "stage", "stages", "blow", "blows", "డై ప్రోగ్రెషన్", "స్టేషన్", "స్టేజ్", "డైస్", "डाई प्रोग्रेशन", "स्टेशन", "estación", "estaciones", "stufe", "stufen", "poste", "postes", "переход", "工位", "ステーション"),
        KTopic.MACHINE to listOf("machine", "header", "former", "మెషిన్", "మెషీన్", "హెడర్", "मशीन", "हेडर", "máquina", "maschine", "presse", "станок", "автомат", "设备", "冷镦机", "設備", "圧造機"),
        KTopic.STATUS to listOf("status", "poka", "poka-yoke", "check", "problem", "problems", "issue", "issues", "release", "స్టేటస్", "సమస్య", "సమస్యలు", "చెక్", "स्टेटस", "समस्या", "जांच", "estado", "problema", "prüfung", "problème", "проверк", "проблем", "状态", "问题", "状態", "問題"),
        KTopic.DENSITY to listOf("density", "g/cm3", "సాంద్రత", "డెన్సిటీ", "घनत्व", "डेंसिटी", "densidad", "dichte", "densité", "плотност", "密度"),
        KTopic.STRENGTH to listOf("tensile", "yield", "strength", "uts", "rm", "proof stress", "టెన్సైల్", "స్ట్రెంగ్త్", "యీల్డ్", "बल", "तन्य", "स्ट्रेंथ", "यील्ड", "resistencia", "zugfestigkeit", "streckgrenze", "résistance", "прочност", "текучест", "强度", "抗拉", "屈服", "強さ", "引張", "降伏"),
        KTopic.PITCH to listOf("pitch", "pitches", "tpi", "threads per inch", "పిచ్", "पिच", "टीपीआई", "paso", "steigung", "pas", "шаг", "螺距", "ピッチ"),
        KTopic.STANDARD to listOf("standard", "which standard", "iso", "din", "asme", "స్టాండర్డ్", "मानक", "स्टैंडर्ड", "norma", "norm", "norme", "стандарт", "标准", "規格"),
        KTopic.COST to listOf("cost", "price", "quotation", "quote", "ధర", "ఖర్చు", "కోటేషన్", "कीमत", "लागत", "कोटेशन", "precio", "coste", "costo", "preis", "kosten", "prix", "coût", "цена", "стоимост", "价格", "成本", "価格", "コスト"),
        KTopic.GCODE to listOf("g-code", "gcode", "g code", "cnc program", "fanuc", "జీ కోడ్", "जी कोड", "programa cnc", "cnc-programm", "programme cn", "программа чпу", "数控程序", "ncプログラム"),
        KTopic.CAN to listOf("can i", "can we", "is it ok", "is it allowed", "allowed", "possible", "should i", "safe to", "చేయవచ్చా", "వాడవచ్చా", "పర్వాలేదా", "సాధ్యమా", "చేయొచ్చా", "कर सकते", "चलेगा", "सकता है", "ठीक है", "puedo", "se puede", "darf", "kann ich", "zulässig", "puis-je", "peut-on", "можно", "可以吗", "能否", "できますか", "可能"),
        KTopic.HELP to listOf("help", "what can you do", "hello", "hi", "hey", "నమస్కారం", "సహాయం", "హలో", "नमस्ते", "मदद", "hola", "ayuda", "hallo", "hilfe", "bonjour", "aide", "привет", "помощь", "你好", "帮助", "こんにちは", "ヘルプ")
    )

    fun topics(question: String): Set<KTopic> {
        val t = " " + question.lowercase(Locale.ROOT) + " "
        val found = TOPIC_WORDS.filter { (_, words) -> words.any { JarvisParser.has(t, it) } }.keys.toMutableSet()
        if (KTopic.THREAD in found) found -= KTopic.PITCH          // "pitch diameter" is a thread limit, not the pitch list
        if (KTopic.LOADS in found && "proof stress" in t) found -= KTopic.LOADS
        return found
    }

    private val PART_TOPICS = setOf(KTopic.CUT_OFF, KTopic.WEIGHT, KTopic.RAW_MATERIAL, KTopic.BLANK, KTopic.THREAD, KTopic.LOADS, KTopic.HARDNESS,
        KTopic.HEAT_TREAT, KTopic.ANNEAL, KTopic.COATING, KTopic.BAKE, KTopic.PROCESS, KTopic.STATIONS, KTopic.MACHINE, KTopic.STATUS, KTopic.COST, KTopic.GCODE)
    private val CARD_TOPICS = setOf(KTopic.STRENGTH, KTopic.HARDNESS, KTopic.DENSITY, KTopic.COATING, KTopic.BAKE, KTopic.PITCH, KTopic.STANDARD, KTopic.HEAT_TREAT, KTopic.ANNEAL)

    /** The current part recalculated with only what the question changes; drawing dimensions are kept unless the size changes. */
    fun recalc(base: FastenerCalculationResult, i: JarvisIntent): FastenerCalculationResult {
        val unit = i.unit ?: base.unitSystem
        val type = i.type ?: base.fastenerType
        val sizeChanged = i.diaMm != null || unit != base.unitSystem
        val dims = sizeChanged || type != base.fastenerType || i.lengthMm != null
        val d = i.diaMm ?: if (unit != base.unitSystem) FastenerCatalog.diameterMm(unit, FastenerCatalog.defaultSizeIndex(unit)) else base.nominalDiaMm
        val idx = FastenerCatalog.sizeIndexFor(unit, d)
        val pitch = when {
            i.pitch != null -> i.pitch
            sizeChanged -> if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES[idx].pitches.first() else FastenerCatalog.INCH_SIZES[idx].tpis.first().toDouble()
            unit == UnitSystem.INCH -> 25.4 / base.pitchMm
            else -> base.pitchMm
        }
        val std = if (type != base.fastenerType || unit != base.unitSystem) PokaYokeEngine.defaultStandard(type, unit) else base.standard
        val tc = if (type != base.fastenerType || unit != base.unitSystem) SelectionFilter.threadClasses(unit, type, std).first() else base.threadClass
        return CalculationEngine.calculate(
            fastenerType = type, unitSystem = unit, standard = std, nominalSizeIndex = idx, pitchValue = pitch,
            lengthMm = i.lengthMm ?: base.lengthMm, threadClass = tc,
            material = i.materialKey?.let { FastenerCatalog.MATERIALS[it] } ?: base.material,
            coating = i.coating ?: base.coating, batchQty = i.batchQty ?: base.batchQty,
            customNominalDiaMm = if (sizeChanged) null else base.nominalDiaMm,
            propertyClass = i.propertyClass ?: base.propertyClass,
            drawing = if (dims) DrawingGeometry(threadLengthMm = i.threadLengthMm) else base.drawing.let { g -> if (i.threadLengthMm != null) g.copy(threadLengthMm = i.threadLengthMm) else g },
            tensileRange = if (i.propertyClass != null) null else i.tensileRange ?: base.tensileRange,
            threadLockPatch = i.threadLockPatch ?: base.threadLockPatch,
            headerChoice = i.headerChoice ?: base.headerChoice
        )
    }

    // Designations of standards and materials: ISO 4014, DIN 931, ASME B18.2.1, NAS1352, AMS 6304, ASTM A193, SAE 1045, Inconel 625 …
    private val CODE_PATTERNS = listOf(
        Regex("""(?<![\p{L}\d])(iso|din|bs|jis|gb|nf|uni)\s?-?\s?\d{2,5}(?:[-/:.]\d+)*(?![\p{L}\d])""", RegexOption.IGNORE_CASE),
        Regex("""(?<![\p{L}\d])asme\s?b\s?\d+(?:\.\d+)+""", RegexOption.IGNORE_CASE),
        Regex("""(?<![\p{L}\d])(nasm|nas|ams|bac[a-z]?|abs)\s?-?\s?\d{2,6}[a-z]?(?:[-/]\d+)?(?![\p{L}\d])""", RegexOption.IGNORE_CASE),
        Regex("""(?<![\p{L}\d])astm\s?[a-f]\s?\d{2,4}[a-z]?(?![\p{L}\d])""", RegexOption.IGNORE_CASE),
        Regex("""(?<![\p{L}\d])(sae|aisi|uns)\s?[a-z]?\d{4,5}(?![\p{L}\d])""", RegexOption.IGNORE_CASE),
        Regex("""(?<![\p{L}\d])(inconel|monel|hastelloy|waspaloy|incoloy|nimonic|haynes)\s?[a-z]?\d{2,4}(?![\p{L}\d])""", RegexOption.IGNORE_CASE),
        Regex("""(?<![\p{L}\d])(mil|ams-qq|qq)-?[a-z]{1,4}-?\d{2,6}[a-z]?(?![\p{L}\d])""", RegexOption.IGNORE_CASE),
        // Upper-case only, so ordinary words ("is", "an", "ms", "en") are not read as standards
        Regex("""(?<![\p{L}\d])(IS|EN|MS|AN)\s?-?\s?\d{2,6}[A-Z]?(?:[-/]\d+)?(?![\p{L}\d])""")
    )

    private fun norm(s: String) = s.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")

    /** Standards the app's calculations and poka-yoke rules are written to (taken from the engine source). */
    private val ENGINE_REFERENCES = listOf("ISO 898-1", "SAE J429", "ISO 898-2", "ISO 4042", "AMS 2759", "ISO 3506-2", "ISO 3506-1", "ISO 965-1", "ISO 4032", "ISO 4014", "ASTM A563", "SAE J995", "ISO 4762", "ASME B18.2.1", "ASME B1.1", "ISO 7089", "ISO 10684", "DIN 603", "ASTM F1940", "ASTM A193", "ISO 965", "ISO 5855", "ISO 4033", "ISO 3506", "ISO 10642", "ASTM F1941", "ASTM A574", "ASTM A194", "ASME B18.3", "AMS 5731", "ISO 7045", "ISO 1207", "DIN 6921", "ASTM A370", "AMS 5708", "AMS 5662", "AMS 4928", "ISO 965-5", "ISO 7380-1", "ISO 7380", "ISO 4035", "ISO 14583", "ISO 10683", "EN 2882", "EN 10263", "ASTM F436", "ASTM B601", "ASME B18.6.3", "ASME B18.2.2", "AS8879", "SAE J417", "MIL-PRF-8625", "MIL-DTL-18240", "MIL-A-8625", "ISO 9717", "ISO 7379", "ISO 7093", "ISO 7050", "ISO 7047", "ISO 7046", "ISO 7042", "ISO 7040", "ISO 4026", "ISO 4017", "ISO 261", "ISO 2341", "ISO 15330", "ISO 1481", "ISO 14579", "ISO 10664", "EN 10263-4", "EN 10263-2", "DIN 76", "DIN 6900", "DIN 6797", "DIN 6796", "DIN 6334", "DIN 50942", "DIN 471", "ASME B1.15", "ISO 18265", "ASTM F519", "AMS-QQ-P-416")

    /** Every designation the app holds, normalised: standards, types, materials (with their specs and sources), coatings, classes. */
    private fun appCorpus(): String = buildString {
        ENGINE_REFERENCES.forEach { append(norm(it)).append('|') }
        ProductStandard.values().forEach { append(norm(it.code)).append('|').append(norm(it.label)).append('|') }
        FastenerType.values().forEach { append(norm(it.label)).append('|') }
        FastenerCatalog.MATERIALS.values.forEach { m ->
            append(norm(m.key)).append('|').append(norm(m.name)).append('|').append(norm(m.heatTreatmentSpec.standardReference)).append('|')
            append(norm(m.rawMaterialSourcingUtsMpa)).append('|'); m.dataSources.forEach { append(norm(it)).append('|') }
        }
        CoatingType.values().forEach { append(norm(it.label)).append('|').append(norm(it.specStandard)).append('|') }
        PropertyClass.values().forEach { append(norm(it.label)).append('|') }
    }

    /** Designations in the question that the app holds no data for. */
    fun unknownCodes(question: String): List<String> {
        val corpus = appCorpus()
        return CODE_PATTERNS.flatMap { re -> re.findAll(question).map { it.value.trim() }.toList() }
            .distinctBy { norm(it) }
            .filter { norm(it) !in corpus }
    }

    /** What the app does hold of the same family (NAS → its NAS standards; SAE 1045 → steels starting 10…). */
    private fun closestInApp(code: String): List<String> {
        val c = norm(code)
        val family = c.takeWhile { it.isLetter() }
        val digits = c.dropWhile { it.isLetter() }.takeWhile { it.isDigit() }
        val standards = ProductStandard.values().map { it.code }.filter { family.length >= 2 && norm(it).startsWith(family) }
        val materials = FastenerCatalog.MATERIALS.keys.filter { k -> digits.length >= 2 && norm(k).contains(digits.take(2)) && (family in setOf("SAE", "AISI", "UNS", "SS", "SUS") || family.isEmpty()) }
        val alloys = FastenerCatalog.MATERIALS.values.filter { family.length >= 4 && norm(it.name).contains(family) }.map { it.key }
        return (standards + materials + alloys).distinct().take(6)
    }

    private fun unknownAnswer(codes: List<String>, l: AppLanguage): KnowledgeAnswer {
        val list = codes.joinToString(", ")
        val closest = codes.flatMap { closestInApp(it) }.distinct().take(6)
        val near = if (closest.isEmpty()) "" else " " + L(l, "Closest in the app", "యాప్‌లో దగ్గరగా ఉన్నవి", "ऐप में सबसे नज़दीक", "Lo más cercano en la app", "Am nächsten in der App",
            "Le plus proche dans l'appli", "Ближайшее в приложении", "应用中最接近的", "アプリ内で最も近いもの") + ": " + closest.joinToString(", ") + "."
        val text = L(l,
            "“$list” is not in the app's data, so I cannot confirm any value for it.$near To add it: a material with “+ Add Alloy” on the Sizing screen; any other value as an approved fact (📚) with its source document. If you allow online AI it can look it up — check the answer, then save it as an approved fact.",
            "“$list” యాప్ డేటాలో లేదు, కాబట్టి దాని విలువలను నేను నిర్ధారించలేను.$near జోడించాలంటే: మెటీరియల్ అయితే Sizing లో “+ Add Alloy”; ఇతర విలువలు మూల డాక్యుమెంట్‌తో ఆమోదించిన ఫ్యాక్ట్‌గా (📚). ఆన్‌లైన్ AI అనుమతిస్తే అది వెతకగలదు — జవాబు సరిచూసి ఆమోదించిన ఫ్యాక్ట్‌గా సేవ్ చేయండి.",
            "“$list” ऐप के डेटा में नहीं है, इसलिए मैं इसका कोई मान पक्का नहीं कर सकता।$near जोड़ने के लिए: मटेरियल हो तो Sizing में “+ Add Alloy”; बाकी मान स्रोत दस्तावेज़ के साथ स्वीकृत तथ्य (📚) के रूप में। ऑनलाइन AI की अनुमति हो तो वह खोज सकता है — जवाब जांचकर स्वीकृत तथ्य के रूप में सेव करें।",
            "“$list” no está en los datos de la app, así que no puedo confirmar ningún valor.$near Para añadirlo: un material con “+ Add Alloy” en Sizing; otros valores como dato aprobado (📚) con su documento fuente. Si permite la IA en línea, puede buscarlo: compruebe la respuesta y guárdela como dato aprobado.",
            "„$list“ ist nicht in den App-Daten, daher kann ich keinen Wert dafür bestätigen.$near Hinzufügen: Werkstoff mit „+ Add Alloy“ im Sizing; andere Werte als geprüfte Angabe (📚) mit Quelldokument. Mit erlaubter Online-KI kann sie es nachschlagen — Antwort prüfen und als geprüfte Angabe speichern.",
            "« $list » n'est pas dans les données de l'appli, je ne peux donc confirmer aucune valeur.$near Pour l'ajouter : un matériau avec « + Add Alloy » dans Sizing ; toute autre valeur comme fait approuvé (📚) avec son document source. Si vous autorisez l'IA en ligne, elle peut le chercher — vérifiez la réponse puis enregistrez-la comme fait approuvé.",
            "«$list» нет в данных приложения, поэтому я не могу подтвердить ни одного значения.$near Чтобы добавить: материал — через «+ Add Alloy» в Sizing; прочие значения — как подтверждённый факт (📚) с исходным документом. Если разрешить онлайн-ИИ, он может найти это — проверьте ответ и сохраните как подтверждённый факт.",
            "“$list”不在应用数据中，因此我无法确认它的任何数值。$near 添加方法：材料用 Sizing 中的“+ Add Alloy”；其他数值作为已确认事实（📚）并注明来源文件。若允许在线 AI，可由其查询——核对答案后保存为已确认事实。",
            "「$list」はアプリのデータにないため、値を確認できません。$near 追加方法：材料は Sizing の「+ Add Alloy」、その他の値は出典文書付きで承認済み情報（📚）として。オンライン AI を許可すれば検索できます。回答を確認してから承認済み情報として保存してください。")
        return KnowledgeAnswer(text, emptyList(), false)
    }

    fun answer(question: String, current: FastenerCalculationResult?, lang: AppLanguage, facts: List<ApprovedFact> = emptyList()): KnowledgeAnswer {
        val l = lang
        ApprovedFacts.bestMatch(question, facts)?.let { f ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(f.addedAt))
            return KnowledgeAnswer(f.answer, listOf(L(l, "your approved fact", "మీరు ఆమోదించిన ఫ్యాక్ట్", "आपका स्वीकृत तथ्य", "su dato aprobado", "Ihre geprüfte Angabe",
                "votre fait approuvé", "ваш подтверждённый факт", "您确认的事实", "承認済みの情報") + " ($date) — ${f.source}"), true)
        }
        // A standard, material or product the app does not hold: say so, never answer it from the current part
        unknownCodes(question).takeIf { it.isNotEmpty() }?.let { return unknownAnswer(it, l) }
        val topics = topics(question)
        val intent = JarvisParser.parse(question, current?.unitSystem ?: UnitSystem.METRIC)
        val dimsAsked = intent.diaMm != null || intent.type != null || intent.lengthMm != null || intent.unit != null
        val entities = intent.materialKey != null || intent.propertyClass != null || intent.coating != null || intent.tensileRange != null
        val overrides = dimsAsked || entities || intent.headerChoice != null || intent.threadLockPatch != null || intent.batchQty != null

        if (topics == setOf(KTopic.HELP) && !overrides) return KnowledgeAnswer(help(l), emptyList(), true)

        // "Can I …?": the app's own rules decide, for the current part with the asked change
        if (KTopic.CAN in topics && current != null && overrides) return whatIf(current, recalc(current, intent), l)

        // A part named with no question: its cut-off and weight, as Jarvis gives after a change
        val partTopics = topics.intersect(PART_TOPICS).ifEmpty { if (dimsAsked && (topics - KTopic.HELP).isEmpty()) setOf(KTopic.CUT_OFF, KTopic.WEIGHT) else emptySet() }
        val cardOnly = !dimsAsked && entities && (topics - KTopic.HELP).all { it in CARD_TOPICS }
        if (current != null && partTopics.isNotEmpty() && !cardOnly) {
            val r = if (overrides) recalc(current, intent) else current
            val lines = mutableListOf<String>()
            if (overrides) lines += L(l, "For", "దీని కోసం", "इसके लिए", "Para", "Für", "Pour", "Для", "针对", "対象") +
                " ${r.threadLabel} × ${n(r.lengthMm, 1)} mm ${r.fastenerType.label.substringBefore(" (")}, ${r.material.key}, ${r.tensileRange?.label ?: r.propertyClass?.label?.substringBefore(" (") ?: ""}, ${r.coating.label.substringBefore(" (")} (" +
                L(l, "other inputs from your current setup", "మిగతా వివరాలు ప్రస్తుత సెటప్ నుండి", "बाकी इनपुट मौजूदा सेटअप से", "resto de datos de la configuración actual",
                    "übrige Eingaben aus der aktuellen Einstellung", "autres données de la configuration actuelle", "остальные данные из текущей настройки", "其他输入取自当前设置", "その他の入力は現在の設定から") + "):"
            val sources = linkedSetOf("AeroFast calculation")
            partTopics.sortedBy { it.ordinal }.forEach { t -> lines += partLine(t, r, l, sources) }
            return KnowledgeAnswer(lines.filter { it.isNotBlank() }.joinToString("\n"), sources.toList(), true)
        }

        // Cards from the app's tables
        val cards = mutableListOf<String>()
        val sources = linkedSetOf<String>()
        intent.materialKey?.let { FastenerCatalog.MATERIALS[it] }?.let { m -> cards += materialCard(m, l, topics, sources) }
        (intent.propertyClass)?.let { pc -> cards += classCard(pc, l, sources) }
        intent.tensileRange?.takeIf { intent.propertyClass == null }?.let { tr ->
            cards += "${tr.label}: " + L(l, "Hardness", "హార్డ్‌నెస్", "कठोरता", "Dureza", "Härte", "Dureté", "Твёрдость", "硬度", "硬さ") +
                " ≈ ${n(HardnessConversion.hrcFor(tr.minMpa), 0)}–${n(HardnessConversion.hrcFor(tr.maxMpa), 0)} HRC"
            sources += "ISO 898-1 class pairs / ISO 18265 conversion in the app (approximate)"
        }
        intent.coating?.let { c -> cards += coatingCard(c, l, sources) }
        if (intent.diaMm != null && (KTopic.PITCH in topics || (!entities && intent.type == null && topics.isEmpty()))) cards += sizeCard(intent, l, sources)
        if (KTopic.STANDARD in topics) (intent.type ?: current?.fastenerType)?.let { t -> cards += standardCard(t, l, sources) }
        if (cards.isNotEmpty()) return KnowledgeAnswer(cards.joinToString("\n\n"), sources.toList(), true)

        if (KTopic.HELP in topics) return KnowledgeAnswer(help(l), emptyList(), true)
        return KnowledgeAnswer(notFound(l), emptyList(), false)
    }

    private fun partLine(t: KTopic, r: FastenerCalculationResult, l: AppLanguage, sources: MutableSet<String>): String {
        val s = ProductionReport.summary(r)
        return when (t) {
            KTopic.CUT_OFF -> L(l, "Cut-off length", "కట్ ఆఫ్ పొడవు", "कट ऑफ लंबाई", "Longitud de corte", "Ablänge", "Longueur de coupe", "Длина отрезки", "下料长度", "切断長") +
                ": ${n(r.cutOffLengthMm)} mm · " + L(l, "Wire Ø", "వైర్ Ø", "तार Ø", "Alambre Ø", "Draht Ø", "Fil Ø", "Проволока Ø", "线材 Ø", "線材 Ø") + " ${n(r.wireDiaMm)} mm" +
                (r.headingPlan?.let { " · ${it.machineDescription}" } ?: "")
            KTopic.WEIGHT -> L(l, "Finished weight", "ఫినిష్డ్ బరువు", "तैयार वजन", "Peso terminado", "Fertiggewicht", "Poids fini", "Готовый вес", "成品重量", "完成重量") +
                ": ${n(s.finishedWeightG, 3)} g · " + L(l, "Slug weight", "స్లగ్ బరువు", "स्लग वजन", "Peso del tocho", "Abschnittgewicht", "Poids du lopin", "Вес заготовки", "坯料重量", "スラグ重量") +
                ": ${n(s.slugWeightG, 3)} g (scrap ${n(s.scrapPct, 1)} %)"
            KTopic.RAW_MATERIAL -> L(l, "Raw material for the batch", "బ్యాచ్‌కి ముడి పదార్థం", "बैच के लिए कच्चा माल", "Materia prima del lote", "Rohmaterial für das Los", "Matière pour le lot", "Сырьё на партию", "批量原材料", "ロットの原材料") +
                ": ${n(s.batchRawMaterialKg, 1)} kg / ${s.batchQty} pcs (${s.piecesPerTonne} pcs/t), wire Ø${n(r.wireDiaMm)} mm"
            KTopic.BLANK -> if (r.fastenerType.isUnthreaded) "" else L(l, "Rolling blank", "రోలింగ్ బ్లాంక్", "रोलिंग ब्लैंक", "Diámetro previo al rodado", "Rollrohling", "Ébauche de roulage", "Заготовка под накатку", "滚丝坯径", "転造ブランク") +
                ": ${n(r.blankLowerLimit, 3)} – ${n(r.blankUpperLimit, 3)} mm (target ${n(r.blankTargetDia, 3)})"
            KTopic.THREAD -> if (r.fastenerType.isUnthreaded) "" else "${r.threadLabel}: " +
                L(l, "Major Ø", "మేజర్ Ø", "मेजर Ø", "Ø mayor", "Außen-Ø", "Ø majeur", "Наружный Ø", "大径", "外径") + " ${n(r.majorDiaMin, 3)}–${n(r.majorDiaMax, 3)} · " +
                L(l, "Pitch Ø", "పిచ్ Ø", "पिच Ø", "Ø medio", "Flanken-Ø", "Ø sur flancs", "Средний Ø", "中径", "有効径") + " ${n(r.pitchDiaMin, 3)}–${n(r.pitchDiaMax, 3)} · " +
                L(l, "Minor Ø", "మైనర్ Ø", "माइनर Ø", "Ø menor", "Kern-Ø", "Ø mineur", "Внутренний Ø", "小径", "谷径") + " ${n(r.minorDiaMin, 3)}–${n(r.minorDiaMax, 3)} mm"
            KTopic.LOADS -> L(l, "Stress area As", "స్ట్రెస్ ఏరియా As", "स्ट्रेस एरिया As", "Sección resistente As", "Spannungsquerschnitt As", "Section résistante As", "Расчётное сечение As", "应力截面积 As", "有効断面積 As") +
                " ${n(r.stressAreaAsMm2, 1)} mm² · " + L(l, "Proof load", "ప్రూఫ్ లోడ్", "प्रूफ लोड", "Carga de prueba", "Prüfkraft", "Charge d'épreuve", "Пробная нагрузка", "保证载荷", "保証荷重") +
                " ${n(r.proofLoadKn, 1)} kN · " + L(l, "Breaking load", "బ్రేకింగ్ లోడ్", "ब्रेकिंग लोड", "Carga de rotura", "Bruchkraft", "Charge de rupture", "Разрушающая нагрузка", "断裂载荷", "破断荷重") +
                " ${n(r.tensileBreakingLoadKn, 1)}" + (r.tensileRange?.let { " – ${n(r.stressAreaAsMm2 * it.maxMpa / 1000.0, 1)}" } ?: "") + " kN"
            KTopic.HARDNESS -> L(l, "Hardness", "హార్డ్‌నెస్", "कठोरता", "Dureza", "Härte", "Dureté", "Твёрдость", "硬度", "硬さ") + ": ${r.heatTreatmentSpec.targetHardnessHrc}"
            KTopic.HEAT_TREAT -> r.heatTreatmentSpec.let { ht ->
                L(l, "Heat treatment", "హీట్ ట్రీట్‌మెంట్", "हीट ट्रीटमेंट", "Tratamiento térmico", "Wärmebehandlung", "Traitement thermique", "Термообработка", "热处理", "熱処理") +
                    ": ${ht.austenitizingOrSolutionTempC}, ${ht.soakingTimeMinutes}; ${ht.quenchMedium}; ${ht.temperingOrAgingTempC} (${ht.temperingOrAgingTimeHours}); ${ht.targetHardnessHrc} [${ht.standardReference}]"
            }
            KTopic.ANNEAL -> r.softAnnealSpec?.let { a ->
                L(l, "Soft anneal", "సాఫ్ట్ అనీల్", "सॉफ्ट एनील", "Recocido", "Weichglühen", "Recuit", "Отжиг", "软化退火", "焼なまし") +
                    ": ${a.route.label}; ${a.temperatureC.ifBlank { "—" }}; ${a.soakTime.ifBlank { "—" }}; ${a.coolingRate.ifBlank { "—" }}" +
                    (if (a.route == AnnealRoute.SPHEROIDIZE) AnnealCycleEngine.spheroidize(r.material, AnnealCycleEngine.DEFAULT_CHARGE_TONNES, r.wireDiaMm)?.let { "; cycle ${it.totalText} (10 t charge)" } ?: "" else "")
            } ?: ""
            KTopic.COATING -> L(l, "Coating", "కోటింగ్", "कोटिंग", "Recubrimiento", "Beschichtung", "Revêtement", "Покрытие", "涂层", "表面処理") + ": ${r.coating.label} — ${r.coatingSpecText}"
            KTopic.BAKE -> L(l, "Hydrogen bake", "హైడ్రోజన్ బేక్", "हाइड्रोजन बेक", "Deshidrogenado", "Wasserstoff-Tempern", "Dégazage hydrogène", "Обезводороживание", "除氢烘烤", "水素脆化除去ベーキング") + ": " +
                if (r.hasHydrogenEmbrittlementRisk) L(l, "required", "అవసరం", "आवश्यक", "necesario", "erforderlich", "nécessaire", "требуется", "需要", "必要") + " — ${r.heatTreatmentSpec.bakingTempC}, ${r.heatTreatmentSpec.bakingSoakTime}"
                else L(l, "not required", "అవసరం లేదు", "आवश्यक नहीं", "no necesario", "nicht erforderlich", "non nécessaire", "не требуется", "不需要", "不要")
            KTopic.PROCESS -> L(l, "Process sequence", "ప్రాసెస్ క్రమం", "प्रक्रिया क्रम", "Secuencia de proceso", "Prozessfolge", "Séquence de fabrication", "Последовательность операций", "工艺顺序", "工程順序") +
                ":\n" + ProductionReport.processSequence(r).mapIndexed { i, st -> "${(i + 1) * 10}. $st" }.joinToString("\n")
            KTopic.STATIONS -> r.headingPlan?.let { p ->
                L(l, "Die progression", "డై ప్రోగ్రెషన్", "डाई प्रोग्रेशन", "Secuencia de matrices", "Stufenfolge", "Gamme de frappe", "Переходы", "工位顺序", "工程") +
                    " (${p.machineDescription}):\n" + p.stations.joinToString("\n") { "${it.index}. ${it.title}: ${it.operation}" }
            } ?: ""
            KTopic.MACHINE -> r.headingPlan?.let { p ->
                L(l, "Machine", "మెషిన్", "मशीन", "Máquina", "Maschine", "Machine", "Оборудование", "设备", "設備") + ": ${p.machineDescription} — ${p.routeReason}"
            } ?: ""
            KTopic.STATUS -> {
                sources += "AeroFast poka-yoke rules"
                val c = r.criticalIssues; val m = r.pokaYokeIssues.filter { it.severity == PokaYokeSeverity.MANDATORY }
                val w = r.pokaYokeIssues.count { it.severity == PokaYokeSeverity.WARNING }
                JarvisReply.build(JarvisIntent(queries = setOf(JarvisQuery.STATUS)), r, l).substringBefore(" ⚠") +
                    (c + m).joinToString("") { "\n• [${it.severity.name}] ${it.title}: ${it.fix}" } +
                    (if (w > 0) "\n(${w} warning(s) on the Sizing card)" else "")
            }
            KTopic.COST -> {
                sources += "AeroFast costing with your rates"
                L(l, "Unit price", "యూనిట్ ధర", "इकाई मूल्य", "Precio unitario", "Stückpreis", "Prix unitaire", "Цена за штуку", "单价", "単価") + " ${r.currencySymbol}${n(r.finalUnitPrice)} · " +
                    L(l, "Batch total", "బ్యాచ్ మొత్తం", "बैच कुल", "Total del lote", "Losgesamtpreis", "Total du lot", "Итого за партию", "批量总价", "ロット合計") + " ${r.currencySymbol}${n(r.batchTotalPrice)} (${r.batchQty} pcs)"
            }
            KTopic.GCODE -> "Fanuc:\n${r.fanucGCode}"
            else -> ""
        }
    }

    private fun whatIf(base: FastenerCalculationResult, r: FastenerCalculationResult, l: AppLanguage): KnowledgeAnswer {
        val crit = r.criticalIssues
        val mand = r.pokaYokeIssues.filter { it.severity == PokaYokeSeverity.MANDATORY }
        val head = "${r.threadLabel} × ${n(r.lengthMm, 1)} ${r.fastenerType.label.substringBefore(" (")}, ${r.material.key}, ${r.tensileRange?.label ?: r.propertyClass?.label?.substringBefore(" (") ?: ""}, ${r.coating.label.substringBefore(" (")}: "
        val verdict = when {
            crit.isNotEmpty() -> L(l, "Not allowed (critical)", "అనుమతి లేదు (క్రిటికల్)", "अनुमति नहीं (क्रिटिकल)", "No permitido (crítico)", "Nicht zulässig (kritisch)", "Non autorisé (critique)", "Недопустимо (критично)", "不允许（严重）", "不可（重大）")
            mand.isNotEmpty() -> L(l, "Allowed by the app's rules, with these mandatory controls", "యాప్ నియమాల ప్రకారం అనుమతి ఉంది, ఈ తప్పనిసరి నియంత్రణలతో", "ऐप के नियमों के अनुसार अनुमति है, इन अनिवार्य नियंत्रणों के साथ",
                "Permitido según las reglas de la app, con estos controles obligatorios", "Nach den App-Regeln zulässig, mit diesen Pflichtmaßnahmen", "Autorisé selon les règles de l'appli, avec ces contrôles obligatoires",
                "Допустимо по правилам приложения, с обязательными мерами", "按应用规则允许，需执行以下强制控制", "アプリの規則上可能（以下の必須管理付き）")
            else -> L(l, "Allowed by the app's rules", "యాప్ నియమాల ప్రకారం అనుమతి ఉంది", "ऐप के नियमों के अनुसार अनुमति है", "Permitido según las reglas de la app", "Nach den App-Regeln zulässig",
                "Autorisé selon les règles de l'appli", "Допустимо по правилам приложения", "按应用规则允许", "アプリの規則上可能")
        }
        val details = (crit + mand).joinToString("") { "\n• [${it.severity.name}] ${it.title}: ${it.fix}" }
        return KnowledgeAnswer(head + verdict + "." + details, listOf("AeroFast poka-yoke rules"), true)
    }

    private fun materialCard(m: MaterialInfo, l: AppLanguage, topics: Set<KTopic>, sources: MutableSet<String>): String {
        sources += "AeroFast material catalogue" + if (m.dataSources.isNotEmpty()) " (${m.dataSources.joinToString(", ")})" else ""
        val lines = mutableListOf("${m.key} — ${m.name}")
        val all = topics.none { it in setOf(KTopic.DENSITY, KTopic.STRENGTH, KTopic.HEAT_TREAT, KTopic.ANNEAL, KTopic.HARDNESS) }
        if (all || KTopic.DENSITY in topics) lines += L(l, "Density", "సాంద్రత", "घनत्व", "Densidad", "Dichte", "Densité", "Плотность", "密度", "密度") + ": ${m.density} g/cm³"
        if (all || KTopic.STRENGTH in topics) {
            lines += L(l, "Tensile strength", "టెన్సైల్ స్ట్రెంగ్త్", "तन्य शक्ति", "Resistencia a la tracción", "Zugfestigkeit", "Résistance à la traction", "Предел прочности", "抗拉强度", "引張強さ") +
                ": ${m.postForgingTargetUtsMpa.toInt()} MPa · " + L(l, "Yield strength", "యీల్డ్ స్ట్రెంగ్త్", "यील्ड स्ट्रेंथ", "Límite elástico", "Streckgrenze", "Limite d'élasticité", "Предел текучести", "屈服强度", "降伏点") +
                ": ${m.yieldStrengthMpa.toInt()} MPa · wire as supplied: ${m.rawMaterialSourcingUtsMpa}"
        }
        if (all || KTopic.HEAT_TREAT in topics || KTopic.HARDNESS in topics) lines += L(l, "Heat treatment", "హీట్ ట్రీట్‌మెంట్", "हीट ट्रीटमेंट", "Tratamiento térmico", "Wärmebehandlung", "Traitement thermique", "Термообработка", "热处理", "熱処理") +
            ": ${m.heatTreatment} [${m.heatTreatmentSpec.standardReference}]"
        if (all || KTopic.ANNEAL in topics) SoftAnnealEngine.build(m).let { a ->
            lines += L(l, "Soft anneal", "సాఫ్ట్ అనీల్", "सॉफ्ट एनील", "Recocido", "Weichglühen", "Recuit", "Отжиг", "软化退火", "焼なまし") + ": ${a.route.label}; ${a.temperatureC.ifBlank { "—" }}"
        }
        m.estimatedFrom?.let { lines += "Note: machining, flow stress, price and tooling values are copied from $it, not sourced for ${m.key}." }
        return lines.joinToString("\n")
    }

    private fun classCard(pc: PropertyClass, l: AppLanguage, sources: MutableSet<String>): String {
        val mat = FastenerCatalog.MATERIALS[pc.defaultMatKey] ?: FastenerCatalog.MATERIALS.getValue("10B21")
        val cs = PokaYokeEngine.classStrength(pc, 10.0, mat)
        sources += if (cs.isIsoSteelClass) "ISO 898-1 class values in the app" else "class values in the app"
        val hard = CalculationEngine.calculateDynamicHeatTreatment(mat, 10.0, false, pc).targetHardnessHrc
        val small = if (pc == PropertyClass.CLASS_8_8) " (d ≤ 16 mm; d > 16 mm: Rm 830, Rp0.2 660, Sp 600 MPa)" else ""
        return pc.label + "\n" +
            L(l, "Tensile strength", "టెన్సైల్ స్ట్రెంగ్త్", "तन्य शक्ति", "Resistencia a la tracción", "Zugfestigkeit", "Résistance à la traction", "Предел прочности", "抗拉强度", "引張強さ") + " Rm ≥ ${cs.rmMinMpa.toInt()} MPa · " +
            L(l, "Yield strength", "యీల్డ్ స్ట్రెంగ్త్", "यील्ड स्ट्रेंथ", "Límite elástico", "Streckgrenze", "Limite d'élasticité", "Предел текучести", "屈服强度", "降伏点") + " ≥ ${cs.yieldMinMpa.toInt()} MPa · " +
            L(l, "Proof stress", "ప్రూఫ్ స్ట్రెస్", "प्रूफ स्ट्रेस", "Tensión de prueba", "Prüfspannung", "Contrainte d'épreuve", "Пробное напряжение", "保证应力", "保証荷重応力") + " Sp ${cs.proofStressMpa.toInt()} MPa$small\n" +
            L(l, "Hardness", "హార్డ్‌నెస్", "कठोरता", "Dureza", "Härte", "Dureté", "Твёрдость", "硬度", "硬さ") + ": $hard"
    }

    private fun coatingCard(c: CoatingType, l: AppLanguage, sources: MutableSet<String>): String {
        sources += "AeroFast coating table"
        return "${c.label}\n" +
            L(l, "Standard", "స్టాండర్డ్", "मानक", "Norma", "Norm", "Norme", "Стандарт", "标准", "規格") + ": ${c.specStandard}\n" +
            L(l, "Thickness", "మందం", "मोटाई", "Espesor", "Schichtdicke", "Épaisseur", "Толщина", "厚度", "膜厚") + ": ${n(c.thicknessMinUm, 0)}–${n(c.thicknessMaxUm, 0)} µm · " +
            (if (c.isElectroplating) "electroplated" else "not electroplated") + " · " +
            L(l, "Hydrogen bake", "హైడ్రోజన్ బేక్", "हाइड्रोजन बेक", "Deshidrogenado", "Wasserstoff-Tempern", "Dégazage hydrogène", "Обезводороживание", "除氢烘烤", "水素脆化除去ベーキング") + ": " +
            if (c.requiresHydrogenBaking) L(l, "required on high-strength steel", "హై-స్ట్రెంగ్త్ స్టీల్‌కు అవసరం", "हाई-स्ट्रेंथ स्टील पर आवश्यक", "necesario en acero de alta resistencia",
                "bei hochfestem Stahl erforderlich", "nécessaire sur acier à haute résistance", "требуется для высокопрочной стали", "高强度钢需要", "高強度鋼で必要")
            else L(l, "not required", "అవసరం లేదు", "आवश्यक नहीं", "no necesario", "nicht erforderlich", "non nécessaire", "не требуется", "不需要", "不要")
    }

    private fun sizeCard(i: JarvisIntent, l: AppLanguage, sources: MutableSet<String>): String {
        val unit = i.unit ?: UnitSystem.METRIC
        val idx = FastenerCatalog.sizeIndexFor(unit, i.diaMm!!)
        return if (unit == UnitSystem.METRIC) {
            sources += "ISO 261 size table in the app"
            val s = FastenerCatalog.METRIC_SIZES[idx]
            val p = s.pitches.first()
            val asMm2 = PI / 4.0 * (s.d - 0.938194 * p).pow(2.0)
            "${s.label}: " + L(l, "Standard pitches", "స్టాండర్డ్ పిచ్‌లు", "मानक पिच", "Pasos normalizados", "Normsteigungen", "Pas normalisés", "Стандартные шаги", "标准螺距", "標準ピッチ") +
                " ${s.pitches.joinToString(" / ")} mm (coarse first) · As ${n(asMm2, 1)} mm² @ ${p} mm"
        } else {
            sources += "ASME B1.1 size table in the app"
            val s = FastenerCatalog.INCH_SIZES[idx]
            "${s.label}: " + L(l, "Threads per inch", "అంగుళానికి థ్రెడ్లు", "प्रति इंच थ्रेड", "Hilos por pulgada", "Gänge pro Zoll", "Filets par pouce", "Ниток на дюйм", "每英寸牙数", "山数/インチ") +
                " ${s.tpis.joinToString(" / ")} (UNC first, then UNF)"
        }
    }

    private fun standardCard(t: FastenerType, l: AppLanguage, sources: MutableSet<String>): String {
        sources += "AeroFast type–standard map"
        val m = PokaYokeEngine.defaultStandard(t, UnitSystem.METRIC)
        val i = PokaYokeEngine.defaultStandard(t, UnitSystem.INCH)
        return "${t.label}\n" + L(l, "Standard", "స్టాండర్డ్", "मानक", "Norma", "Norm", "Norme", "Стандарт", "标准", "規格") +
            ": metric ${m.code} (${m.label}); inch ${i.code} (${i.label})"
    }
}
