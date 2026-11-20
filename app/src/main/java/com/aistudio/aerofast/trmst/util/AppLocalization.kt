package com.aistudio.aerofast.trmst.util

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    EN("en", "English", "🇬🇧"),
    HI("hi", "हिन्दी", "🇮🇳"),
    TE("te", "తెలుగు", "🇮🇳"),
    ES("es", "Español", "🇪🇸"),
    DE("de", "Deutsch", "🇩🇪"),
    FR("fr", "Français", "🇫🇷"),
    RU("ru", "Русский", "🇷🇺"),
    ZH("zh", "中文", "🇨🇳"),
    JA("ja", "日本語", "🇯🇵")
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.EN }

object AppLocalization {
    private const val PREFS_NAME = "aerofast_prefs"
    private const val LANG_KEY = "aerofast_language_code"

    fun getSelectedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(LANG_KEY, "en") ?: "en"
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.EN
    }

    fun setSelectedLanguage(context: Context, lang: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(LANG_KEY, lang.code).apply()
    }

    fun getTabTitle(tabIndex: Int, lang: AppLanguage): String {
        return when (tabIndex) {
            1 -> when (lang) {
                AppLanguage.HI -> "1. आकार निर्धारण"
                AppLanguage.TE -> "1. పరిమాణం"
                AppLanguage.ES -> "1. Dimensionado"
                AppLanguage.DE -> "1. Bemaßung"
                AppLanguage.FR -> "1. Dimensionnement"
                AppLanguage.RU -> "1. Размеры"
                AppLanguage.ZH -> "1. 尺寸规格"
                AppLanguage.JA -> "1. サイジング"
                AppLanguage.EN -> "1. Sizing"
            }
            2 -> when (lang) {
                AppLanguage.HI -> "2. टूलिंग"
                AppLanguage.TE -> "2. టూలింగ్"
                AppLanguage.ES -> "2. Utillaje"
                AppLanguage.DE -> "2. Werkzeuge"
                AppLanguage.FR -> "2. Outillage"
                AppLanguage.RU -> "2. Оснастка"
                AppLanguage.ZH -> "2. 模具"
                AppLanguage.JA -> "2. 金型"
                AppLanguage.EN -> "2. Tooling"
            }
            3 -> when (lang) {
                AppLanguage.HI -> "3. ऊष्मा व फोर्जिंग"
                AppLanguage.TE -> "3. వేడి & ఫోర్జింగ్"
                AppLanguage.ES -> "3. TT y Forja"
                AppLanguage.DE -> "3. WB & Schmieden"
                AppLanguage.FR -> "3. TT & Forgeage"
                AppLanguage.RU -> "3. ТО и Ковка"
                AppLanguage.ZH -> "3. 热处理与锻造"
                AppLanguage.JA -> "3. 熱処理・鍛造"
                AppLanguage.EN -> "3. HT & Forging"
            }
            4 -> when (lang) {
                AppLanguage.HI -> "4. एआई स्कैनर"
                AppLanguage.TE -> "4. AI స్కానర్"
                AppLanguage.ES -> "4. Escáner IA"
                AppLanguage.DE -> "4. KI-Scanner"
                AppLanguage.FR -> "4. Scanner IA"
                AppLanguage.RU -> "4. ИИ Сканер"
                AppLanguage.ZH -> "4. AI扫描仪"
                AppLanguage.JA -> "4. AIスキャナー"
                AppLanguage.EN -> "4. AI Scanner"
            }
            5 -> when (lang) {
                AppLanguage.HI -> "5. एआई सह-पायलट"
                AppLanguage.TE -> "5. AI కో-పైలట్"
                AppLanguage.ES -> "5. Co-Piloto IA"
                AppLanguage.DE -> "5. KI-Co-Pilot"
                AppLanguage.FR -> "5. Co-Pilote IA"
                AppLanguage.RU -> "5. ИИ Ко-Пилот"
                AppLanguage.ZH -> "5. AI副驾驶"
                AppLanguage.JA -> "5. AI副操縦士"
                AppLanguage.EN -> "5. AI Co-Pilot"
            }
            6 -> when (lang) {
                AppLanguage.HI -> "6. 8डी एनपीडी इंजन"
                AppLanguage.TE -> "6. 8D ఇంజిన్"
                AppLanguage.ES -> "6. Motor NPD 8D"
                AppLanguage.DE -> "6. 8D NPD-System"
                AppLanguage.FR -> "6. Moteur 8D NPD"
                AppLanguage.RU -> "6. 8D NPD Модуль"
                AppLanguage.ZH -> "6. 8D研发引擎"
                AppLanguage.JA -> "6. 8D NPD エンジン"
                AppLanguage.EN -> "6. 8D NPD Engine"
            }
            7 -> when (lang) {
                AppLanguage.HI -> "7. चित्र और वीडियो"
                AppLanguage.TE -> "7. చిత్రాలు & వీడియో"
                AppLanguage.ES -> "7. Imágenes y vídeo"
                AppLanguage.DE -> "7. Bilder & Video"
                AppLanguage.FR -> "7. Images & vidéo"
                AppLanguage.RU -> "7. Изображения и видео"
                AppLanguage.ZH -> "7. 图片与视频"
                AppLanguage.JA -> "7. 画像と動画"
                AppLanguage.EN -> "7. Images & Video"
            }
            else -> "Tab $tabIndex"
        }
    }

    fun t(key: String, lang: AppLanguage): String {
        return when (key) {
            "app_subtitle" -> when (lang) {
                AppLanguage.HI -> "थ्रेड मास्टर • ISO 898-1"
                AppLanguage.TE -> "థ్రెడ్ మాస్టర్ • ISO 898-1"
                AppLanguage.ES -> "Maestro de Roscas • ISO 898-1"
                AppLanguage.DE -> "Gewindemeister • ISO 898-1"
                AppLanguage.FR -> "Maître du Filetage • ISO 898-1"
                AppLanguage.RU -> "Мастер Резьбы • ISO 898-1"
                AppLanguage.ZH -> "螺纹大师 • ISO 898-1"
                AppLanguage.JA -> "スレッドマスター • ISO 898-1"
                AppLanguage.EN -> "Thread Master • ISO 898-1"
            }
            "theme" -> when (lang) {
                AppLanguage.HI -> "थीम"
                AppLanguage.TE -> "థీమ్"
                AppLanguage.ES -> "Tema"
                AppLanguage.DE -> "Design"
                AppLanguage.FR -> "Thème"
                AppLanguage.RU -> "Тема"
                AppLanguage.ZH -> "主题"
                AppLanguage.JA -> "テーマ"
                AppLanguage.EN -> "Theme"
            }
            "font" -> when (lang) {
                AppLanguage.HI -> "फ़ॉन्ट"
                AppLanguage.TE -> "ఫాంట్"
                AppLanguage.ES -> "Fuente"
                AppLanguage.DE -> "Schrift"
                AppLanguage.FR -> "Police"
                AppLanguage.RU -> "Шрифт"
                AppLanguage.ZH -> "字体"
                AppLanguage.JA -> "フォント"
                AppLanguage.EN -> "Font"
            }
            "sync" -> when (lang) {
                AppLanguage.HI -> "सिंक"
                AppLanguage.TE -> "సింక్"
                AppLanguage.ES -> "Sinc"
                AppLanguage.DE -> "Sync"
                AppLanguage.FR -> "Sync"
                AppLanguage.RU -> "Синхр"
                AppLanguage.ZH -> "同步"
                AppLanguage.JA -> "同期"
                AppLanguage.EN -> "Sync"
            }
            "key" -> when (lang) {
                AppLanguage.HI -> "कुंजी"
                AppLanguage.TE -> "కీ"
                AppLanguage.ES -> "Clave"
                AppLanguage.DE -> "Key"
                AppLanguage.FR -> "Clé"
                AppLanguage.RU -> "Ключ"
                AppLanguage.ZH -> "密钥"
                AppLanguage.JA -> "キー"
                AppLanguage.EN -> "Key"
            }
            "new_chat" -> when (lang) {
                AppLanguage.HI -> "+ नई बातचीत"
                AppLanguage.TE -> "+ కొత్త చాట్"
                AppLanguage.ES -> "+ Nuevo Chat"
                AppLanguage.DE -> "+ Neuer Chat"
                AppLanguage.FR -> "+ Nouvelle Discussion"
                AppLanguage.RU -> "+ Новый Чат"
                AppLanguage.ZH -> "+ 新对话"
                AppLanguage.JA -> "+ 新規チャット"
                AppLanguage.EN -> "+ New Chat"
            }
            "ask_copilot_placeholder" -> when (lang) {
                AppLanguage.HI -> "सह-पायलट से पूछें या चित्र संलग्न करें..."
                AppLanguage.TE -> "కో-పైలట్‌ను అడగండి లేదా చిత్రం జతచేయండి..."
                AppLanguage.ES -> "Pregunte a Co-Piloto o adjunte plano..."
                AppLanguage.DE -> "Co-Pilot fragen oder Zeichnung anhängen..."
                AppLanguage.FR -> "Demandez au Co-Pilote ou joignez un plan..."
                AppLanguage.RU -> "Спросите Ко-Пилота или прикрепите чертеж..."
                AppLanguage.ZH -> "向AI副驾驶提问或附加图纸..."
                AppLanguage.JA -> "AI副操縦士に質問または図面を添付..."
                AppLanguage.EN -> "Ask Co-Pilot or attach drawing..."
            }
            "pdf_or_image" -> when (lang) {
                AppLanguage.HI -> "पीडीएफ / छवि फ़ाइल"
                AppLanguage.TE -> "PDF / చిత్ర ఫైల్"
                AppLanguage.ES -> "Archivo PDF / Imagen"
                AppLanguage.DE -> "PDF- / Bilddatei"
                AppLanguage.FR -> "Fichier PDF / Image"
                AppLanguage.RU -> "PDF / Файл Изображения"
                AppLanguage.ZH -> "PDF / 图像文件"
                AppLanguage.JA -> "PDF / 画像ファイル"
                AppLanguage.EN -> "PDF / Image File"
            }
            "camera_scan" -> when (lang) {
                AppLanguage.HI -> "📷 कैमरा स्कैन"
                AppLanguage.TE -> "📷 కెమెరా స్కాన్"
                AppLanguage.ES -> "📷 Escaneo de Cámara"
                AppLanguage.DE -> "📷 Kamera-Scan"
                AppLanguage.FR -> "📷 Scan Caméra"
                AppLanguage.RU -> "📷 Скан Камерой"
                AppLanguage.ZH -> "📷 相机扫描"
                AppLanguage.JA -> "📷 カメラ スキャン"
                AppLanguage.EN -> "📷 Camera Scan"
            }
            "analyze_blueprint" -> when (lang) {
                AppLanguage.HI -> "ब्लूप्रिंट विश्लेषण व प्रक्रिया प्रवाह"
                AppLanguage.TE -> "బ్లూప్రింట్ విశ్లేషణ & ప్రాసెస్ ఫ్లో"
                AppLanguage.ES -> "Analizar Plano y Flujo de Proceso"
                AppLanguage.DE -> "Bauplan analysieren & Prozessablauf"
                AppLanguage.FR -> "Analyser le Plan & Flux de Processus"
                AppLanguage.RU -> "Анализ Чертежа и Техпроцесса"
                AppLanguage.ZH -> "分析工程图纸与推荐工艺流"
                AppLanguage.JA -> "図面解析とプロセスフロー推薦"
                AppLanguage.EN -> "Analyze Blueprint & Recommend Process Flow"
            }
            "settings_title" -> when (lang) {
                AppLanguage.HI -> "सेटिंग्स और विन्यास"
                AppLanguage.TE -> "సెట్టింగులు & కాన్ఫిగరేషన్"
                AppLanguage.ES -> "CONFIGURACIÓN Y AJUSTES"
                AppLanguage.DE -> "EINSTELLUNGEN & KONFIGURATION"
                AppLanguage.FR -> "PARAMÈTRES & CONFIGURATION"
                AppLanguage.RU -> "НАСТРОЙКИ И ПАРАМЕТРЫ"
                AppLanguage.ZH -> "设置与系统配置"
                AppLanguage.JA -> "設定と構成"
                AppLanguage.EN -> "SETTINGS & CONFIGURATION"
            }
            "global_language" -> when (lang) {
                AppLanguage.HI -> "वैश्विक भाषा / स्थानीयकरण (i18n)"
                AppLanguage.TE -> "గ్లోబల్ భాష / స్థానికీకరణ (i18n)"
                AppLanguage.ES -> "IDIOMA GLOBAL / LOCALIZACIÓN (i18n)"
                AppLanguage.DE -> "GLOBALE SPRACHE / LOKALISIERUNG (i18n)"
                AppLanguage.FR -> "LANGUE GLOBALE / LOCALISATION (i18n)"
                AppLanguage.RU -> "ГЛОБАЛЬНЫЙ ЯЗЫК / ЛОКАЛИЗАЦИЯ (i18n)"
                AppLanguage.ZH -> "全局语言 / 本地化 (i18n)"
                AppLanguage.JA -> "グローバル言語 / ローカリゼーション (i18n)"
                AppLanguage.EN -> "GLOBAL LANGUAGE / LOCALIZATION (i18n)"
            }
            else -> key
        }
    }

    fun getCoPilotGreeting(lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.HI -> "एयरोफास्ट एआई सह-पायलट में आपका स्वागत है! मैं एयरोस्पेस फास्टनर धातु विज्ञान, पोका-योक हाइड्रोजन एम्ब्रिटलमेंट बेकिंग प्रोटोकॉल, कोल्ड हेडिंग डाइज़ और टर्निंग ब्लैंक गणना में विशेषज्ञ हूँ। कोई भी प्रश्न पूछें या फोटो संलग्न करें।"
            AppLanguage.TE -> "ఏరోఫాస్ట్ AI కో-పైలట్‌కు స్వాగతం! నేను ఏరోస్పేస్ ఫాస్టెనర్ మెటలర్జీ, పోకా-యోక్ హైడ్రోజన్ బేకింగ్ ప్రోటోకాల్స్, కోల్డ్ హెడ్డింగ్ డైస్ మరియు టర్నింగ్ బ్లాంక్ లెక్కింపులో నైపుణ్యం కలిగి ఉన్నాను. ఏదైనా ప్రశ్న అడగండి లేదా ఫోటోను జతచేయండి."
            AppLanguage.ES -> "¡Bienvenido a AeroFast AI Co-Pilot! Me especializo en metalurgia de sujetadores aeroespaciales, protocolos de deshidrogenado Poka-Yoke, matrices de forja en frío y cálculo de diámetros de pre-laminado. Haga una pregunta o adjunte un plano."
            AppLanguage.DE -> "Willkommen beim AeroFast KI-Co-Pilot! Ich bin spezialisiert auf Luft- und Raumfahrt-Verbindungselemente, Poka-Yoke-Wasserstoffversprödungs-Backprotokolle, Kaltumformwerkzeuge und Vorwalzdurchmesser-Berechnung. Stellen Sie eine Frage oder hängen Sie eine Zeichnung an."
            AppLanguage.FR -> "Bienvenue sur AeroFast IA Co-Pilote ! Je suis spécialisé dans la métallurgie des fixations aérospatiales, les protocoles anti-fragilisation par l'hydrogène Poka-Yoke, l'outillage de frappe à froid et le calcul des ébauches de roulage. Posez une question ou joignez un plan."
            AppLanguage.RU -> "Добро пожаловать в AeroFast ИИ Ко-Пилот! Я специализируюсь на металлургии крепежа аэрокосмического класса, протоколах дегидрогенизации Poka-Yoke, штампах холодной высадки и расчете диаметров заготовок под накатку резьбы. Задайте вопрос или прикрепите чертеж."
            AppLanguage.ZH -> "欢迎使用 AeroFast 航空航天紧固件 AI 副驾驶！我专注于高强度航空紧固件冶金、防错防呆除氢烘烤工艺、冷镦模具以及精密滚丝前毛坯外径计算。请随时提问或上传工程图纸。"
            AppLanguage.JA -> "AeroFast AI副操縦士へようこそ！航空宇宙ファスナー冶金、ポカヨケ水素脆化ベーキングプロトコル、冷間圧造ダイス、転造前ブランク径の精密計算をサポートします。質問を入力するか、図面写真を添付してください。"
            AppLanguage.EN -> "Welcome to AeroFast AI Co-Pilot! I specialize in aerospace fastener metallurgy, Poka-Yoke hydrogen embrittlement baking protocols, ASP30/GT55 cold heading dies, and pre-roll blank calculation. Ask a question, speak via microphone, or attach a photo."
        }
    }
}
