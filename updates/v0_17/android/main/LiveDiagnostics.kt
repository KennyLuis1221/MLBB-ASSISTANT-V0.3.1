package com.mlbb.assistant

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Stores only selected diagnostic fields. No raw OCR, frame pixels, player IDs or credentials. */
internal class LiveDiagnostics(context: Context) {
    private val saved = context.getSharedPreferences("capture_diagnostics", Context.MODE_PRIVATE)
    private val fields = JSONObject()
    private var lastWrite = 0L
    var mode = "Base"
        private set

    @Synchronized
    fun start(value: String) {
        mode = value
        fields.put("apk", BuildConfig.VERSION_NAME)
            .put("modo", mode)
            .put("captura", "Autorizada pelo Android")
            .put("servico_ativo", true)
            .put("quadros_recebidos", 0)
            .put("quadros_processados", 0)
            .put("quadros_fora_do_prazo", 0)
            .put("prazo_local_ms", FrameBudget.DEADLINE_MS)
        persist(true)
    }

    @Synchronized
    fun resolution(width: Int, height: Int) {
        fields.put("resolucao", "${width}×${height}")
        persist(true)
    }

    @Synchronized
    fun received(count: Long, now: Long) {
        fields.put("quadros_recebidos", count)
        if (now - lastWrite >= 1_000) {
            fields.put("ultimo_quadro_utc", Instant.now().toString())
            persist()
        }
    }

    @Synchronized
    fun analyzed(result: AnalysisResult, duration: Long) {
        fields.put("captura", "Captura ativa").put("quadros_processados", result.frameCount)
            .put("ultima_analise_utc", Instant.now().toString())
            .put("latencia_local_ms", duration)
            .put("estado", result.status)
            .put("ocr", result.ocrSummary)
            .put("base", result.item ?: "Aguardando dados")
            .put("heroi_proprio", result.hero ?: JSONObject.NULL)
            .put("tempo_partida", result.matchTime ?: JSONObject.NULL)
        fields.put("jogadores", playerRows(result))
            .put("latencia_maxima_ms", maxOf(fields.optLong("latencia_maxima_ms"), duration))
            .put("placar_atual", result.currentBoard)
            .put("justificativa_base", result.reason)
        if (result.observedAt > 0) fields.put("idade_observacao_ms",
            (SystemClock.elapsedRealtime() - result.observedAt).coerceAtLeast(0))
        else fields.remove("idade_observacao_ms")
        result.localError?.let { fields.put("ultimo_erro", it) }
        persist(true)
    }

    @Synchronized
    fun started(gap: Long?) {
        fields.put("quadros_analisados_tentativas", fields.optLong("quadros_analisados_tentativas") + 1)
        if (gap != null) {
            fields.put("ultimo_intervalo_local_ms", gap)
                .put("maior_intervalo_local_ms", maxOf(fields.optLong("maior_intervalo_local_ms"), gap))
        }
        persist()
    }

    @Synchronized
    fun deadlineExceeded(duration: Long) {
        fields.put("quadros_fora_do_prazo", fields.optInt("quadros_fora_do_prazo") + 1)
            .put("ultima_latencia_fora_do_prazo_ms", duration)
            .put("ultimo_erro", "Quadro excedeu o prazo de 1 s")
        persist(true)
    }

    @Synchronized
    fun state(status: String, error: String? = null) {
        fields.put("captura", status)
        if (error != null) fields.put("ultimo_erro", error)
        persist(true)
    }

    @Synchronized
    fun error(message: String) {
        fields.put("ultimo_erro", message)
        persist(true)
    }

    @Synchronized
    fun cloud(status: String) {
        if (fields.optString("ia") == status) return
        fields.put("ia", status)
        persist(true)
    }

    @Synchronized
    fun ended() {
        fields.put("servico_ativo", false)
        if (fields.optString("captura").startsWith("Captura ativa") || fields.optString("captura").isBlank())
            fields.put("captura", "Encerrada")
        persist(true)
    }

    companion object {
        fun playerRows(result: AnalysisResult): JSONArray {
            val rows = JSONArray()
            for (player in result.players) {
                val slots = JSONArray()
                for (slot in player.visualSlots) {
                    slots.put(JSONObject().put("slot", slot.index + 1).put("item", slot.name)
                        .put("distancia", slot.distance).put("margem", slot.margin))
                }
                rows.put(JSONObject().put("heroi", player.hero).put("lado", player.team)
                    .put("ouro", player.gold ?: JSONObject.NULL).put("kda", player.kda ?: JSONObject.NULL)
                    .put("nivel", player.level ?: JSONObject.NULL)
                    .put("lado_indice", player.side ?: JSONObject.NULL).put("linha_indice", player.row ?: JSONObject.NULL)
                    .put("distancia_retrato", player.heroDistance ?: JSONObject.NULL)
                    .put("margem_retrato", player.heroMargin ?: JSONObject.NULL).put("slots", slots))
            }
            return rows
        }
    }

    private fun persist(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastWrite < 1_000) return
        lastWrite = now
        saved.edit().putString("ultimo", fields.toString()).apply()
    }
}
