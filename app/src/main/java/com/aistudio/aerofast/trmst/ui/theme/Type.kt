package com.aistudio.aerofast.trmst.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.R

enum class AppFontOption(
    val displayName: String,
    val icon: String,
    val fontResId: Int?
) {
    ROBOTO_MONO("Roboto Mono (Technical)", "🔤", R.font.roboto_mono),
    INTER("Inter (Clean Modern)", "🔡", R.font.inter),
    MONTSERRAT("Montserrat (Geometric)", "🔲", R.font.montserrat),
    OSWALD("Oswald (Condensed Eng)", "📐", R.font.oswald),
    PLAYFAIR("Playfair (Precision Serif)", "📰", R.font.playfair_display),
    OPEN_SANS("Open Sans (Clean Sans)", "💬", R.font.open_sans),
    POPPINS("Poppins (Modern Sans)", "✨", R.font.poppins),
    LATO("Lato (Humanist Sans)", "📝", R.font.lato),
    FIRA_CODE("Fira Code (Developer Mono)", "💻", R.font.fira_code),
    CINZEL("Cinzel (Classic Trajan)", "🏛️", R.font.cinzel),
    SPACE_MONO("Space Mono (Futuristic)", "🚀", R.font.space_mono),
    ROBOTO("Roboto (Standard)", "📱", R.font.roboto),
    SYSTEM_MONO("System Monospace", "⚙️", null),
    SYSTEM_SANS("System Sans-Serif", "📲", null),
    SYSTEM_SERIF("System Serif", "📖", null)
}

fun createTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp
        )
    )
}

val Typography = createTypography(FontFamily.Monospace)
