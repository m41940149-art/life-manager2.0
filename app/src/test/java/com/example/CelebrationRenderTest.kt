package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.celebration.CelebrationCenter
import com.example.ui.celebration.CelebrationHost
import com.example.ui.celebration.CelebrationKind
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CelebrationRenderTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var contentView: View

    @After fun cleanup() = CelebrationCenter.dismiss()

    private fun show(arabic: Boolean) {
        compose.setContent {
            contentView = LocalView.current
            CompositionLocalProvider(LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) { CelebrationHost(isArabic = arabic) }
                }
            }
        }
    }

    private fun screenshot(name: String) {
        val target = File("/tmp/celebration-visuals/$name.png")
        target.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = contentView.rootView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun `habit streak celebration renders in Arabic`() {
        show(arabic = true)
        CelebrationCenter.celebrate(CelebrationKind.HABIT_STREAK, 15)
        repeat(45) { compose.mainClock.advanceTimeBy(16) }
        compose.waitForIdle()
        compose.onNodeWithText("15 يومًا متواصلًا من الالتزام! نصف شهر من القوة 🔥").assertExists()
        screenshot("habit-streak-15-ar")
    }

    @Test fun `all tasks done celebration renders in English`() {
        show(arabic = false)
        CelebrationCenter.celebrate(CelebrationKind.ALL_TASKS_DONE)
        repeat(45) { compose.mainClock.advanceTimeBy(16) }
        compose.waitForIdle()
        compose.onNodeWithText("All of today's tasks are done! You're today's champion 👑").assertExists()
        screenshot("all-tasks-done-en")
    }
}
