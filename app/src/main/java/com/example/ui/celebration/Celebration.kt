package com.example.ui.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** What kind of achievement is being celebrated. */
enum class CelebrationKind {
    TASK_DONE,
    TASK_COUNT_MILESTONE,    // detail = tasks done today
    ALL_TASKS_DONE,
    TASK_TOTAL_MILESTONE,    // detail = all-time completed tasks
    TASK_DAY_STREAK,         // detail = consecutive days with at least one done task
    HABIT_DONE,
    HABIT_STREAK,            // detail = streak length in days
    ALL_HABITS_DONE,
    TIMER_SESSION,           // detail = session minutes
    TIMER_TOTAL_MILESTONE    // detail = all-time focused minutes
}

data class Celebration(
    val kind: CelebrationKind,
    val messageAr: String,
    val messageEn: String,
    val emoji: String,
    // Big celebrations get more confetti and a longer moment on screen.
    val big: Boolean,
    val id: Long = System.nanoTime()
)

/**
 * App-wide bus for joyful surprises. ViewModels publish, MainScreen hosts the overlay.
 * Kept free of Android/Compose imports so it stays unit-testable.
 */
object CelebrationCenter {

    private val _current = MutableStateFlow<Celebration?>(null)
    val current: StateFlow<Celebration?> = _current.asStateFlow()

    fun celebrate(kind: CelebrationKind, detail: Int = 0) {
        _current.value = build(kind, detail)
    }

    fun dismiss() {
        _current.value = null
    }

    private fun pick(vararg options: String) = options[Random.nextInt(options.size)]

    // A rare extra-warm message, so even an ordinary tap can surprise.
    private fun jackpot() = Random.nextInt(100) < 8

    private fun build(kind: CelebrationKind, detail: Int): Celebration = when (kind) {
        CelebrationKind.TASK_DONE -> {
            if (jackpot()) Celebration(
                kind,
                "مذهل! أنت تحوّل يومك إلى انتصارات متتالية، استمر 🏆",
                "Amazing! You're turning your day into a streak of wins, keep going 🏆",
                "🏆", big = true
            ) else Celebration(
                kind,
                pick(
                    "أحسنت! مهمة جديدة تُشطب من القائمة 👏",
                    "خطوة أخرى للأمام، أنت على الطريق الصحيح!",
                    "رائع! إنجازك يتراكم خطوة بخطوة",
                    "مهمة أقل على كاهلك الآن، تنفّس الصعداء 😌",
                    "عمل جميل! هكذا تُبنى الأيام المنتجة"
                ),
                pick(
                    "Well done! Another task checked off 👏",
                    "One more step forward, you're on the right track!",
                    "Great! Your progress adds up step by step",
                    "One less task on your shoulders, breathe easy 😌",
                    "Nice work! This is how productive days are built"
                ),
                pick("🎉", "✨", "👏", "💪", "🌟"), big = false
            )
        }

        CelebrationKind.TASK_COUNT_MILESTONE -> Celebration(
            kind,
            "أنجزت $detail مهام اليوم! رقم يستحق الاحتفال 🎊",
            "$detail tasks done today! A number worth celebrating 🎊",
            "🎊", big = true
        )

        CelebrationKind.ALL_TASKS_DONE -> Celebration(
            kind,
            "كل مهام اليوم اكتملت! أنت بطل هذا اليوم 👑",
            "All of today's tasks are done! You're today's champion 👑",
            "👑", big = true
        )

        CelebrationKind.TASK_TOTAL_MILESTONE -> Celebration(
            kind,
            "وصلت إلى $detail مهمة منجزة منذ بدأت! تاريخك من الإنجاز يكبر 🏅",
            "You've reached $detail completed tasks! Your track record keeps growing 🏅",
            "🏅", big = true
        )

        CelebrationKind.TASK_DAY_STREAK -> when {
            detail >= 100 -> Celebration(
                kind,
                "$detail يومًا متواصلًا من الإنجاز! دخلت منطقة الأساطير 👑🔥",
                "$detail straight days of getting things done! Legend territory 👑🔥",
                "👑", big = true
            )
            detail >= 30 -> Celebration(
                kind,
                "شهر كامل ($detail يومًا) من الإنجاز اليومي! قلّة من يصل إلى هنا 🌙🏆",
                "A full month ($detail days) of daily wins! Few ever get here 🌙🏆",
                "🏆", big = true
            )
            detail >= 15 -> Celebration(
                kind,
                "$detail يومًا متواصلًا من الإنجاز! نصف شهر كامل وأنت لا تتوقف ⚡",
                "$detail straight days of wins! Half a month and you won't stop ⚡",
                "⚡", big = true
            )
            detail >= 7 -> Celebration(
                kind,
                "أسبوع كامل من الإنجاز اليومي! $detail أيام متتالية 🏆",
                "A full week of daily wins! $detail days in a row 🏆",
                "🏆", big = true
            )
            else -> Celebration(
                kind,
                "$detail أيام متتالية من الإنجاز! الزخم يُبنى الآن 🚀",
                "$detail days in a row of getting things done! Momentum is building 🚀",
                "🚀", big = true
            )
        }

        CelebrationKind.HABIT_DONE -> {
            if (jackpot()) Celebration(
                kind,
                "التزامك ملهم! كل يوم تثبت أن عاداتك أقوى من أي عذر 💎",
                "Your commitment is inspiring! Every day you prove your habits beat any excuse 💎",
                "💎", big = true
            ) else Celebration(
                kind,
                pick(
                    "عادة أخرى سجلتها، الالتزام يبنيك يومًا بعد يوم 💪",
                    "مواظبة رائعة! نسخة أفضل منك تتشكل الآن",
                    "تقدم صغير كل يوم يصنع فرقًا كبيرًا 🌱",
                    "هكذا تُبنى العادات: مرة كل مرة، وأنت تفعلها!"
                ),
                pick(
                    "Another habit checked in, commitment builds you day after day 💪",
                    "Great consistency! A better you is taking shape",
                    "Small daily progress makes a big difference 🌱",
                    "This is how habits are built: one check-in at a time, and you're doing it!"
                ),
                pick("🌱", "💪", "✨", "🔆"), big = false
            )
        }

        CelebrationKind.HABIT_STREAK -> when {
            detail >= 365 -> Celebration(
                kind,
                "عام كامل ($detail يومًا) من الالتزام! إنجاز يُروى 📖👑",
                "A whole year ($detail days) of commitment! A story-worthy feat 📖👑",
                "👑", big = true
            )
            detail >= 100 -> Celebration(
                kind,
                "$detail يومًا من الالتزام المتواصل! أنت من النادرين 💎🔥",
                "$detail days of unbroken commitment! You're one of the rare few 💎🔥",
                "💎", big = true
            )
            detail >= 30 -> Celebration(
                kind,
                "شهر كامل ($detail يومًا) لم تنقطع فيه! هذه العادة صارت جزءًا منك 🌙🔥",
                "A full month ($detail days) unbroken! This habit is part of you now 🌙🔥",
                "🌙", big = true
            )
            detail >= 21 -> Celebration(
                kind,
                "$detail يومًا! يقولون العادة تتكون في ٢١ يومًا، وأنت تجاوزت ذلك 🎓🔥",
                "$detail days! They say a habit forms in 21 days, and you went past that 🎓🔥",
                "🎓", big = true
            )
            detail >= 15 -> Celebration(
                kind,
                "$detail يومًا متواصلًا من الالتزام! نصف شهر من القوة 🔥",
                "$detail straight days of commitment! Half a month of strength 🔥",
                "🔥", big = true
            )
            detail >= 7 -> Celebration(
                kind,
                "أسبوع كامل لم تنقطع فيه! $detail أيام من الالتزام 🔥",
                "A full week unbroken! $detail days of commitment 🔥",
                "🔥", big = true
            )
            else -> Celebration(
                kind,
                "$detail أيام متواصلة! بداية سلسلة جميلة، حافظ عليها 🔥",
                "$detail days in a row! A beautiful streak starting, keep it alive 🔥",
                "🔥", big = true
            )
        }

        CelebrationKind.ALL_HABITS_DONE -> Celebration(
            kind,
            "كل عادات اليوم تمت! يوم مثالي بكل معنى الكلمة ✨",
            "All of today's habits done! A perfect day in every sense ✨",
            "✨", big = true
        )

        CelebrationKind.TIMER_SESSION -> {
            if (detail >= 25) Celebration(
                kind,
                "تركيز عميق لمدة $detail دقيقة! عقلك يشكرك على هذا الإنجاز 🧠🏅",
                "$detail minutes of deep focus! Your brain thanks you for this 🧠🏅",
                "🏅", big = true
            ) else Celebration(
                kind,
                pick(
                    "جلسة تركيز مكتملة! كل دقيقة استثمار في نفسك 🧠",
                    "أنهيت جلستك! التركيز عضلة وأنت تقويها",
                    "وقت مستثمر بذكاء، أحسنت ⏳"
                ),
                pick(
                    "Focus session complete! Every minute is an investment in yourself 🧠",
                    "Session done! Focus is a muscle and you're training it",
                    "Time well invested, well done ⏳"
                ),
                pick("⏳", "🧠", "🎯"), big = false
            )
        }

        CelebrationKind.TIMER_TOTAL_MILESTONE -> Celebration(
            kind,
            "وصلت إلى $detail دقيقة تركيز إجمالاً! ساعات من العمل العميق تتراكم ⏳🏆",
            "You've reached $detail total minutes of focus! Hours of deep work adding up ⏳🏆",
            "🏆", big = true
        )
    }
}

private val confettiColors = listOf(
    Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE),
    Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF9800),
    Color(0xFF7C4DFF), Color(0xFF64FFDA)
)

private class ConfettiParticle(
    val x0: Float,         // 0..1 fraction of width at t=0
    val startY: Float,     // fraction of height at t=0 (negative = above screen)
    val vx: Float,         // fraction of width per second
    val vy: Float,         // fraction of height per second
    val size: Float,       // px
    val color: Color,
    val isCircle: Boolean,
    val spin: Float        // degrees per second
)

private const val CONFETTI_SECONDS = 3.2f

// Position at time t (seconds), including gentle gravity.
private fun ConfettiParticle.xAt(t: Float) = x0 + vx * t
private fun ConfettiParticle.yAt(t: Float) = startY + vy * t + 0.12f * t * t
private fun ConfettiParticle.angleAt(t: Float) = spin * t

/**
 * Full-screen celebration overlay: confetti rain, a bouncing emoji card with a warm
 * message, haptic tap, and auto-dismiss. Renders nothing when there is no celebration.
 */
@Composable
fun CelebrationHost(isArabic: Boolean) {
    val celebration by CelebrationCenter.current.collectAsState()
    val active = celebration ?: return

    val haptics = LocalHapticFeedback.current
    val particles = remember(active.id) {
        val count = if (active.big) 90 else 45
        List(count) {
            ConfettiParticle(
                x0 = Random.nextFloat(),
                startY = -Random.nextFloat() * 0.4f,
                vx = (Random.nextFloat() - 0.5f) * 0.15f,
                vy = 0.35f + Random.nextFloat() * 0.3f,
                size = 10f + Random.nextFloat() * 14f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                isCircle = Random.nextBoolean(),
                spin = (Random.nextFloat() - 0.5f) * 360f
            )
        }
    }
    // Drives every confetti frame; reading .value during draw redraws the Canvas.
    val fall = remember(active.id) { Animatable(0f) }

    LaunchedEffect(active.id) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        fall.animateTo(
            1f,
            animationSpec = tween(durationMillis = (CONFETTI_SECONDS * 1000).toInt(), easing = LinearEasing)
        )
    }

    LaunchedEffect(active.id) {
        delay(if (active.big) 5200 else 3800)
        CelebrationCenter.dismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { CelebrationCenter.dismiss() }
    ) {
        // Read in composition scope so each frame recomposes and re-records the Canvas.
        val confettiT = fall.value * CONFETTI_SECONDS
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = confettiT
            particles.forEach { p ->
                val px = p.xAt(t) * size.width
                val py = p.yAt(t) * size.height
                if (py < -p.size || py > size.height + p.size) return@forEach
                if (p.isCircle) {
                    drawCircle(color = p.color, radius = p.size / 2f, center = Offset(px, py))
                } else {
                    rotate(degrees = p.angleAt(t), pivot = Offset(px, py)) {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 2f, py - p.size / 4f),
                            size = Size(p.size, p.size / 2f)
                        )
                    }
                }
            }
        }

        val scale = remember(active.id) { Animatable(0f) }
        LaunchedEffect(active.id) {
            scale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = active.emoji,
                    fontSize = (52 * scale.value.coerceAtLeast(0.01f)).sp
                )
                Text(
                    text = if (isArabic) active.messageAr else active.messageEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
