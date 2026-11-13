package cdev.wargame.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cdev.wargame.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_black, FontWeight.Black)
)

object WarGameColors {
    val BgBlack = Color(0xFF101010)
    val BottomBarBg = Color(0xFF0B0B0B)
    val PillBg = Color(0xFF0A0A0A)
    val CardBg = Color(0xFF121212)
    val CardAltBg = Color(0xFF141414)
    val ButtonSecondaryBg = Color(0xFF1A1A1A)
    val CardBorder = Color(0xFF222222)
    val ButtonSecondaryBorder = Color(0xFF2A2A2A)
    val Divider = Color(0xFF1E1E1E)

    val Gold = Color(0xFFFFC629)
    val GoldHighlight = Color(0xFFFFE57F)
    val GoldDarkText = Color(0xFF0A0401)
    val Cyan = Color(0xFF4DF1FF)
    val CyanBright = Color(0xFF00D2FF)
    val Green = Color(0xFF4ADE80)
    val Danger = Color(0xFFFF3366)
    val Violet = Color(0xFFA855F7)

    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF666666)
    val TextSubtle = Color(0xFF555555)
}

private val BaseTypography = Typography()

val WarGameTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Black),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Black),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Black),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Black),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp)
)

private val DarkColors = darkColorScheme(
    primary = WarGameColors.Gold,
    onPrimary = WarGameColors.GoldDarkText,
    secondary = WarGameColors.Cyan,
    background = WarGameColors.BgBlack,
    surface = WarGameColors.CardBg,
    onSurface = WarGameColors.TextPrimary
)

@Composable
fun WarGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = WarGameTypography
    ) {
        ProvideTextStyle(
            value = LocalTextStyle.current.merge(
                TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = WarGameColors.TextPrimary
                )
            ),
            content = content
        )
    }
}

