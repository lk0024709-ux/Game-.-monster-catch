package com.example.rann.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rann.model.LevelGrade
import com.example.ui.theme.Rann
import com.example.ui.theme.RannColors
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme

// ======================================================
// SHARED UI KIT — premium, muted, generous spacing.
// ======================================================

val CardShape = RoundedCornerShape(20.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(999.dp)

@Composable
fun RannCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color? = null,
    borderColor: Color? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable () -> Unit,
) {
    val c = RannTheme.colors
    val base = modifier
        .shadow(
            elevation = 12.dp,
            shape = CardShape,
            ambientColor = Color.Black.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.10f),
        )
        .clip(CardShape)
        .background(color ?: c.surface)
        .border(1.dp, borderColor ?: c.border, CardShape)
    val clickable = if (onClick != null) base.clickable { onClick() } else base
    Box(modifier = clickable.padding(contentPadding)) {
        content()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = RannTheme.colors
    val alpha by animateFloatAsState(if (enabled) 1f else 0.45f, tween(200), label = "btn")
    Box(
        modifier = modifier
            .height(52.dp)
            .alpha(alpha)
            .shadow(12.dp, ButtonShape, ambientColor = Rann.Primary.copy(alpha = 0.25f))
            .clip(ButtonShape)
            .background(Brush.verticalGradient(listOf(Rann.Primary, Rann.PrimaryDark)))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = RannText.button, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = RannTheme.colors
    Box(
        modifier = modifier
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ButtonShape)
            .background(c.surfaceAlt)
            .border(1.dp, c.border, ButtonShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = RannText.button, color = c.textPrimary)
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    onClick: () -> Unit,
) {
    val c = RannTheme.colors
    Row(
        modifier = modifier
            .clip(PillShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Text(icon, fontSize = 15.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = RannText.buttonSmall, color = c.textMuted)
    }
}

@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier) {
    val c = RannTheme.colors
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(c.surfaceAlt)
            .border(1.dp, c.border, PillShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatCoins(coins),
            style = RannText.buttonSmall,
            color = c.textPrimary,
        )
    }
}

@Composable
fun StatChip(icon: String, text: String, tint: Color? = null, modifier: Modifier = Modifier) {
    val c = RannTheme.colors
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(c.surfaceAlt)
            .border(1.dp, c.border, PillShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Text(text, style = RannText.bodySmall, color = tint ?: c.textMuted)
    }
}

@Composable
fun GradeBadge(grade: LevelGrade, size: Dp = 34.dp, modifier: Modifier = Modifier) {
    val color = gradeColor(grade)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = if (grade == LevelGrade.NONE) 0.18f else 0.16f))
            .border(1.5.dp, color.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = grade.letter,
            fontSize = (size.value / 2.6f).sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

fun gradeColor(grade: LevelGrade): Color = when (grade) {
    LevelGrade.SS -> Rann.GradeSS
    LevelGrade.S -> Rann.GradeS
    LevelGrade.A -> Rann.GradeA
    LevelGrade.B -> Rann.GradeB
    LevelGrade.C -> Rann.GradeC
    LevelGrade.D -> Rann.GradeD
    LevelGrade.NONE -> Rann.GradeNone
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val c = RannTheme.colors
    Column(modifier) {
        Text(title, style = RannText.h2, color = c.textPrimary)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = RannText.bodySmall, color = c.textMuted)
        }
    }
}

@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = RannTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.surfaceAlt)
                    .border(1.dp, c.border, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("←", fontSize = 18.sp, color = c.textPrimary)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = RannText.h1, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = RannText.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val c = RannTheme.colors
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(c.surfaceAlt)
            .border(1.dp, c.border, PillShape)
            .padding(4.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(if (active) c.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = RannText.buttonSmall,
                    color = if (active) c.textPrimary else c.textMuted,
                )
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 7.dp,
    color: Color = Rann.Primary,
    track: Color = Color(0x22000000),
    content: @Composable (() -> Unit)? = null,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(sw / 2f, sw / 2f),
                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(sw / 2f, sw / 2f),
                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        content?.invoke()
    }
}

@Composable
fun AnimatedCoinCounter(
    target: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = RannText.h2,
    color: Color = Rann.PrimaryDark,
) {
    var display by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) {
        val steps = 24
        val from = display
        for (i in 1..steps) {
            display = from + ((target - from) * i) / steps
            kotlinx.coroutines.delay(16)
        }
        display = target
    }
    Text("🪙 $display", style = style, color = color, modifier = modifier)
}

/** Big animated grade letter used by the result modal. */
@Composable
fun GradeReveal(grade: LevelGrade, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(grade) {
        scale.animateTo(1.15f, tween(320))
        scale.animateTo(1f, tween(200))
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = grade.letter,
            fontSize = 118.sp,
            fontWeight = FontWeight.ExtraBold,
            color = gradeColor(grade),
            modifier = Modifier.scale(scale.value),
        )
        Text(
            text = grade.label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = gradeColor(grade).copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 132.dp),
        )
    }
}

@Composable
fun ModalScrim(
    visible: Boolean,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
        exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.98f, animationSpec = tween(160)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(enabled = onDismiss != null) { onDismiss?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.padding(20.dp)) { content() }
        }
    }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    val c = RannTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RannText.bodySmall, color = c.textMuted, modifier = Modifier.weight(1f))
        Text(value, style = RannText.bodySmall, color = valueColor ?: c.textPrimary, textAlign = TextAlign.End)
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit,
) {
    val c = RannTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = RannText.h3, color = c.textPrimary)
            Text(subtitle, style = RannText.bodySmall, color = c.textMuted)
        }
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 26.dp)
                .clip(PillShape)
                .background(if (checked) Rann.Primary else c.border),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

fun formatCoins(value: Int): String {
    return if (value >= 1_000_000) {
        "%.1fM".format(value / 1_000_000f)
    } else if (value >= 10_000) {
        "%.1fK".format(value / 1_000f)
    } else {
        value.toString()
    }
}
