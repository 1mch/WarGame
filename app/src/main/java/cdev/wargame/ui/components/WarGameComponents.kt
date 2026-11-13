package cdev.wargame.ui.components

import android.graphics.Paint as NativePaint
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdev.wargame.R
import cdev.wargame.ui.theme.WarGameColors

/**
 * Primary pill-shaped CTA button with pulsing neon-gold outer glow and breathing scale animation.
 */
@Composable
fun PrimaryGlowPillButton(
    text: String,
    @DrawableRes iconRes: Int = R.drawable.ic_play,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 68.dp,
    fontSize: TextUnit = 20.sp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pillPulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFraction"
    )

    val scale = 1.0f + 0.05f * pulseFraction

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .drawBehind {
                val radius = size.height / 2f
                val blurPx = (16.dp + (12.dp * pulseFraction)).toPx()
                val glowAlpha = (145 + (75 * pulseFraction).toInt()).coerceIn(0, 255)
                val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                    style = NativePaint.Style.FILL
                    color = WarGameColors.Gold.toArgb()
                    setShadowLayer(
                        blurPx,
                        0f,
                        0f,
                        android.graphics.Color.argb(glowAlpha, 255, 198, 41)
                    )
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        radius,
                        radius,
                        paint
                    )
                }
            }
            .clip(CircleShape)
            .background(WarGameColors.Gold, CircleShape)
            //.border(1.5.dp, WarGameColors.GoldHighlight, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = WarGameColors.GoldDarkText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = WarGameColors.GoldDarkText,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp
            )
        }
    }
}

/**
 * Secondary dark tactical button with optional monochrome Lucide icon.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    textColor: Color = WarGameColors.TextPrimary,
    backgroundColor: Color = WarGameColors.ButtonSecondaryBg,
    borderColor: Color = WarGameColors.ButtonSecondaryBorder,
    height: Dp = 42.dp
) {
    Surface(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * Circular icon button used in Top HUD (e.g. Settings).
 */
@Composable
fun IconCircleButton(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = Color(0xFFCCCCCC)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(WarGameColors.PillBg, CircleShape)
            .border(1.dp, WarGameColors.CardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * In-Game HUD action button (Home, Restart, Pause).
 */
@Composable
fun HudIconButton(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x8C1E293B), RoundedCornerShape(8.dp))
            .border(1.2.dp, Color(0xB4475569), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Standard Dark Cyber Card template.
 */
@Composable
fun WarGameCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = WarGameColors.CardBg,
    borderColor: Color = WarGameColors.CardBorder,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickMod),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/**
 * Top HUD Currency Pill displaying Credits and Gems with Lucide icons.
 */
@Composable
fun CurrencyPill(
    creditsText: String,
    gemsText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(WarGameColors.PillBg, CircleShape)
            .border(1.dp, WarGameColors.CardBorder, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_coins),
            contentDescription = null,
            tint = WarGameColors.Gold,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = creditsText,
            color = WarGameColors.Gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(Color(0xFF333333))
        )
        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            painter = painterResource(R.drawable.ic_gem),
            contentDescription = null,
            tint = WarGameColors.Cyan,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = gemsText,
            color = WarGameColors.Cyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Pulsing Rank Badge pill for the Main Menu header.
 */
@Composable
fun PulsingRankBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rankBadgeDot")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF11161F), CircleShape)
            .border(1.dp, Color(0xFF1E293B), CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(1.0f + 0.25f * dotPulse)
                .background(WarGameColors.Cyan.copy(alpha = 0.6f + 0.4f * dotPulse), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = WarGameColors.Cyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * Bottom Navigation Tabs Bar (MISSION, TECH, ARMORY).
 */
@Composable
fun BottomNavBar(
    activeTab: Int,
    titles: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = listOf(
        R.drawable.ic_crosshair,
        R.drawable.ic_zap,
        R.drawable.ic_shield
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(WarGameColors.BottomBarBg)
    ) {
        for (i in 0 until 3) {
            val isActive = activeTab == i
            val tint = if (isActive) WarGameColors.Gold else WarGameColors.TextMuted

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isActive) WarGameColors.Gold.copy(alpha = 0.07f) else Color.Transparent)
                    .clickable { onTabSelected(i) }
            ) {
                // Top border line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WarGameColors.CardBorder)
                        .align(Alignment.TopCenter)
                )

                // Active gold top indicator
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(WarGameColors.Gold)
                            .align(Alignment.TopCenter)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = if (isActive) (-2).dp else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(icons[i]),
                        contentDescription = titles.getOrElse(i) { "" },
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = titles.getOrElse(i) { "" },
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

