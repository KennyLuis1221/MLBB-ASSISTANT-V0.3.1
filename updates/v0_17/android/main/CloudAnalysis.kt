package com.mlbb.assistant

internal data class CloudPlayer(
    val hero: String, val team: String, val items: List<String>,
    val gold: Int?, val level: Int?
)

internal data class CloudEvidence(
    val hero: String,
    val players: List<CloudPlayer>,
    val candidates: List<String>,
    val minute: Int?
) {
    // Cache identity changes when the inputs that can materially alter the advice change.
    fun key() = copy(players = players.map { it.copy(gold = it.gold?.div(1000)) }, minute = minute?.div(3))

    companion object {
        fun from(result: AnalysisResult, now: Long): CloudEvidence? {
            if (!result.currentBoard || !Evidence14.pairFresh(now, result.observedAt)) return null
            val hero = result.hero ?: return null
            val players = result.players.mapNotNull {
                val team = when (it.team) {
                    "Aliado" -> "ally"
                    "Inimigo" -> "enemy"
                    else -> return@mapNotNull null
                }
                CloudPlayer(it.hero, team, it.items.sorted(), it.gold?.toIntOrNull(), it.level)
            }.sortedBy { it.hero }
            if (players.none { it.hero == hero && it.team == "ally" } || players.none { it.team == "enemy" }) return null
            val candidates = (listOfNotNull(result.item) + result.alternatives + result.buildPlan).distinct().take(8)
            if (candidates.isEmpty()) return null
            return CloudEvidence(hero, players, candidates, result.matchTime?.substringBefore(':')?.toIntOrNull())
        }
    }
}

data class CloudSupplement(val item: String?, val advice: String, val evidenceHeroes: List<String>)
internal data class CloudOutcome(val supplement: CloudSupplement?, val error: String?)

internal class CloudAnalysis {
    private var requested: CloudEvidence? = null
    private var cached: Pair<CloudEvidence, CloudSupplement>? = null
    private var cachedAt = 0L
    private var nextRequestAt = 0L
    private var inFlight = false
    private var failure: String? = null

    fun start(evidence: CloudEvidence, now: Long): Boolean {
        if (inFlight || now < nextRequestAt || requested == evidence.key() && failure == null && now - cachedAt < 90_000L) return false
        requested = evidence.key()
        inFlight = true
        nextRequestAt = now + 60_000L
        failure = null
        return true
    }

    fun finish(evidence: CloudEvidence, outcome: CloudOutcome, currentSession: Boolean, now: Long) {
        inFlight = false
        if (!currentSession) return
        failure = outcome.error
        cached = outcome.supplement?.let { evidence.key() to it }
        cachedAt = now
    }

    fun unavailable(message: String) {
        failure = message
    }

    fun supplement(evidence: CloudEvidence?, now: Long) = cached?.takeIf { it.first == evidence?.key() && now - cachedAt < 90_000L }?.second

    fun status(evidence: CloudEvidence?, now: Long): String = when {
        supplement(evidence, now) != null -> "IA disponível para esta leitura"
        inFlight -> "IA consultando • Base ativa"
        failure != null -> "$failure • Base ativa"
        evidence == null -> "IA aguardando placar confirmado • Base ativa"
        else -> "IA aguardando nova consulta • Base ativa"
    }

    fun clear() {
        requested = null
        cached = null
    }
}
