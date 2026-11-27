package com.mlbb.assistant

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inspector.WindowInspector
import android.widget.CheckBox
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.FutureTask

/** Real projection from the system consent dialog, using the test app as the foreground fixture. */
class CapturePanelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    private fun <T> main(action: () -> T): T {
        val task = FutureTask(action)
        instrumentation.runOnMainSync(task)
        return task.get()
    }

    private fun tree(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(tree(view.getChildAt(i)))
    }

    private fun overlay() = WindowInspector.getGlobalWindowViews().singleOrNull {
        (it.layoutParams as? WindowManager.LayoutParams)?.type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    }

    private fun state(): JSONObject = JSONObject(
        context.getSharedPreferences("capture_diagnostics", Context.MODE_PRIVATE).getString("ultimo", "{}")!!)

    private fun await(message: String, test: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 40_000
        while (!test() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        assertTrue(message + " • " + state().let { listOf("captura", "estado", "ocr", "quadros_processados", "ultimo_erro").map(it::optString) }, test())
    }

    @Test fun consentCapturePauseResumeAndStop() {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedPackage = prefs.getString("game_package", null)
        prefs.edit().putString("game_package", context.packageName).commit()
        device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        device.executeShellCommand("appops set ${context.packageName} GET_USAGE_STATS allow")
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            main {
                val controls = tree(activity.window.decorView).filterIsInstance<TextView>().toList()
                (controls.single { it.text.startsWith("Autorizo analisar") } as CheckBox).performClick()
                controls.single { it.text.contains("INICIAR COM APP BASE") }.performClick()
            }
            // Android 15 system dialog on the English AOSP emulator.
            val positive = By.res("android", "button1")
            assertNotNull(device.wait(Until.findObject(positive), 10_000))
            val selector = device.findObject(By.clazz("android.widget.Spinner"))
            if (selector != null) {
                selector.click()
                val entire = device.wait(Until.findObject(By.textContains("Entire screen")), 5000)
                requireNotNull(entire) { "Opção de exibição inteira ausente" }.click()
            }
            device.wait(Until.findObject(positive), 5000).click()
            await("Sobreposição do serviço") { main { overlay() != null } }
            fun resumed() = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<MainActivity>().lastOrNull()
            await("Aplicativo em primeiro plano") { main { resumed() != null } }
            val fixture = main { requireNotNull(resumed()) }
            run {
                val board = main {
                    TextView(fixture).apply {
                        text = "VICTORY"
                        textSize = 42f
                        gravity = android.view.Gravity.CENTER
                        setTextColor(android.graphics.Color.BLACK)
                        fixture.setContentView(this)
                    }
                }
                var pulse = false
                await("OCR da projeção e resultado no painel compacto") {
                    main {
                        pulse = !pulse
                        board.background = android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(if (pulse) 0xffaaaaaa.toInt() else 0xffbbbbbb.toInt(), -1))
                        state().optLong("quadros_recebidos") > 0 && state().optLong("quadros_processados") > 0 &&
                            tree(requireNotNull(overlay())).filterIsInstance<TextView>()
                                .any { it.isShown && it.text.contains("Resultado final") }
                    }
                }
            }
            main {
                val panel = requireNotNull(overlay())
                tree(panel).single { it.contentDescription == "Expandir painel" }.performClick()
                tree(panel).filterIsInstance<TextView>().single { it.text == "Pausar" }.performClick()
            }
            await("Pausa do serviço") { state().optString("captura") == "Captura pausada" }
            main {
                val controls = tree(requireNotNull(overlay())).filterIsInstance<TextView>().toList()
                controls.single { it.text == "Retomar" }.performClick()
                assertTrue(controls.any { it.text == "Aguardando quadro novo" })
                controls.single { it.text == "Encerrar" }.performClick()
            }
            await("Encerramento da captura e remoção do painel") {
                !state().optBoolean("servico_ativo") && main { overlay() == null }
            }
        } finally {
            context.stopService(Intent(context, ProjectionService::class.java))
            main { activity.finish() }
            prefs.edit().putString("game_package", savedPackage).commit()
        }
    }
}
