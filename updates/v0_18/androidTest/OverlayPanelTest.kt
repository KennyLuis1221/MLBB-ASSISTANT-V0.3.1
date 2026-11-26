package com.mlbb.assistant

import android.app.UiAutomation
import android.content.Context
import android.graphics.Point
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inspector.WindowInspector
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.FutureTask

class OverlayPanelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var panel: OverlayPanel
    private var pauses = 0
    private var stops = 0

    private fun <T> main(action: () -> T): T {
        val task = FutureTask(action)
        instrumentation.runOnMainSync(task)
        return task.get()
    }

    private fun views(root: View): List<View> = listOf(root) +
        if (root is ViewGroup) (0 until root.childCount).flatMap { views(root.getChildAt(it)) } else emptyList()

    private fun root(): View = WindowInspector.getGlobalWindowViews().single {
        (it.layoutParams as? WindowManager.LayoutParams)?.type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    }

    private fun label(text: String) = views(root()).filterIsInstance<TextView>().single { it.text.toString() == text }
    private fun header(): View = views(root()).filterIsInstance<TextView>().first().parent as View
    private fun scroll() = views(root()).filterIsInstance<ScrollView>().single()

    private fun tap() {
        val view = header()
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val e = MotionEvent.obtain(time, time, action, 10f, 10f, 0)
            try { view.dispatchTouchEvent(e) } finally { e.recycle() }
        }
    }

    private fun reading() = AnalysisResult("Leitura atual", "Dagger", "Compra de teste", 1,
        "Leitura local", "Layla", 1, 0, "Diagnóstico de teste", observedAt = SystemClock.elapsedRealtime())

    @Before fun open() {
        val permission = instrumentation.uiAutomation.executeShellCommand(
            "appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        ParcelFileDescriptor.AutoCloseInputStream(permission).use { it.readBytes() }
        main { panel = OverlayPanel(context, { pauses++ }, { stops++ }); panel.show() }
        instrumentation.waitForIdleSync()
    }

    @After fun close() {
        if (::panel.isInitialized) main { panel.remove() }
    }

    @Test fun accessibilityAndTouchToggleTheSameWindowAndControlsWork() = main {
        assertTrue(header().performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(View.VISIBLE, scroll().visibility)
        label("Pausar").performClick()
        label("Retomar").performClick()
        label("Encerrar").performClick()
        assertEquals(2, pauses)
        assertEquals(1, stops)
        tap()
        assertEquals(View.GONE, scroll().visibility)
    }

    @Test fun compactDraftIndicatorAndTitleFollowTheCurrentReading() = main {
        panel.showResult(reading().copy(hero = null, item = null, draftPicks = listOf("Layla"), observedAt = 0))
        label("M •")
        tap()
        label("SMART HERO PICK")
        panel.clearResult("Captura pausada")
        label("PRÓXIMA COMPRA RECOMENDADA")
        tap()
        panel.showResult(reading().copy(hero = null, item = null))
        tap()
        tap()
        label("M")
        Unit
    }

    @Test fun expiredReadingCannotReviveAdviceInCompactMode() = main {
        panel.showResult(reading())
        label("M •")
        // A future monotonic timestamp is invalid even on an emulator booted less than 45 s ago.
        val badAt = SystemClock.elapsedRealtime().let {
            if (it > Evidence14.MAX_AGE_MS + 1) it - Evidence14.MAX_AGE_MS - 1 else it + 60_000
        }
        panel.showResult(reading().copy(observedAt = badAt))
        label("M")
        tap()
        label("Dados antigos • abra o placar")
        assertFalse(views(root()).filterIsInstance<TextView>().any { it.text == "Compra de teste" })
    }

    @Test fun reattachingDoesNotDuplicateViewsOrCallbacks() {
        main { panel.remove() }
        instrumentation.waitForIdleSync()
        main {
            panel.show()
            panel.show()
            tap()
            label("Pausar").performClick()
            assertEquals(1, pauses)
        }
    }

    @Test fun opponentInventoryCannotMarkOwnBuildAsPurchased() = main {
        val enemy = ObservedPlayer("Layla", "Inimigo", listOf("Dagger"))
        panel.showResult(reading().copy(players = listOf(enemy), buildPlan = listOf("Dagger")))
        tap()
        label("PLANO CANDIDATO\n▶ Dagger")
        Unit
    }

    @Test fun expandedWindowReflowsAfterRotation() {
        val ui = instrumentation.uiAutomation
        fun size(): Point = main { Point(root().width, root().height) }
        fun awaitSize(predicate: (Point) -> Boolean): Point {
            val deadline = SystemClock.elapsedRealtime() + 7000
            var current = size()
            while (!predicate(current) && SystemClock.elapsedRealtime() < deadline) {
                SystemClock.sleep(80)
                current = size()
            }
            assertTrue("Tamanho observado: $current", predicate(current))
            return current
        }
        try {
            ui.setRotation(UiAutomation.ROTATION_FREEZE_0)
            main { tap() }
            val portrait = awaitSize { it.x > 0 && it.y > it.x }
            ui.setRotation(UiAutomation.ROTATION_FREEZE_90)
            awaitSize { it.x > portrait.x && it.y < portrait.y }
        } finally {
            ui.setRotation(UiAutomation.ROTATION_FREEZE_0)
            instrumentation.waitForIdleSync()
            ui.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
    }

    @Test fun interruptedDiagnosticsDiscardCurrentAdvice() {
        val diag = LiveDiagnostics(context)
        diag.start("IA")
        diag.analyzed(reading(), 30, 20)
        diag.cloud("IA disponível para esta leitura")
        diag.invalidate("Captura pausada", null)
        val prefs = context.getSharedPreferences("capture_diagnostics", Context.MODE_PRIVATE)
        val result = JSONObject(prefs.getString("ultimo", null)!!)
        assertEquals("Captura pausada", result.getString("estado"))
        assertTrue(result.isNull("base"))
        assertFalse(result.has("justificativa_base"))
        assertFalse(result.has("ia"))
        assertEquals(0, result.getJSONArray("jogadores").length())
    }
}
