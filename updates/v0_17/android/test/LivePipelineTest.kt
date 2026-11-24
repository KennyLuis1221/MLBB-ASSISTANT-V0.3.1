package com.mlbb.assistant

import org.junit.Assert.*
import org.junit.Test

class LivePipelineTest {
    private val allies = ObservedPlayer("Layla", "Aliado", listOf("Swift Boots"), gold = "4200")
    private val enemy = ObservedPlayer("Tigreal", "Inimigo", listOf("Blade Armor"))
    private val result = AnalysisResult(
        status = "Placar", item = "Malefic Roar", reason = "Armadura observada",
        detections = 4, source = "Base", hero = "Layla", enemies = 1, items = 1,
        visionStatus = "Placar", players = listOf(allies, enemy),
        observedAt = 5_000L, currentBoard = true, matchTime = "07:20"
    )

    @Test
    fun oneSecondBudgetAlsoPermitsTemporalConfirmation() {
        assertFalse(FrameBudget.due(5_999, 5_000))
        assertTrue(FrameBudget.due(6_000, 5_000))
        assertTrue(FrameBudget.withinDeadline(6_000, 5_000))
        assertFalse(FrameBudget.withinDeadline(6_001, 5_000))
        assertFalse(FrameBudget.withinDeadline(4_999, 5_000))
        assertTrue(Evidence14.pairFresh(5_000 + FrameBudget.INTERVAL_MS, 5_000))
        assertFalse(Evidence14.pairFresh(65_000, 5_000))
    }

    @Test
    fun remoteConsultationRequiresCurrentScoreboardAndKnownTeams() {
        assertNull(CloudEvidence.from(result.copy(currentBoard = false), 5_000))
        assertNull(CloudEvidence.from(result, 25_001))
        assertNull(CloudEvidence.from(result.copy(players = listOf(allies, enemy.copy(team = "Lado 2"))), 5_000))
        assertNotNull(CloudEvidence.from(result, 5_001))
    }

    @Test
    fun localUpdatesContinueWhileRemoteAdviceIsInFlight() {
        val initial = checkNotNull(CloudEvidence.from(result, 5_000))
        val changed = initial.copy(players = initial.players.map {
            if (it.hero == "Tigreal") it.copy(items = listOf("Antique Cuirass")) else it
        })
        val cloud = CloudAnalysis()
        val advice = CloudSupplement("Malefic Roar", "Ajuste à armadura observada de Tigreal.", listOf("Tigreal"))
        assertTrue(cloud.start(initial, 5_000))
        assertFalse(cloud.start(changed, 6_000))
        cloud.finish(initial, CloudOutcome(advice, null), true, 6_000)
        assertEquals(advice, cloud.supplement(initial, 6_000))
        assertNull(cloud.supplement(changed, 6_000))
        assertEquals("Malefic Roar", result.item)
        assertFalse(cloud.start(changed, 64_999))
        assertTrue(cloud.start(changed, 65_000))
        cloud.clear()
        cloud.finish(changed, CloudOutcome(advice, null), false, 66_000)
        assertNull(cloud.supplement(changed, 66_000))
    }

    @Test
    fun intermittentShortChangesReuseAdviceButCacheExpires() {
        val evidence = checkNotNull(CloudEvidence.from(result, 5_000))
        val cloud = CloudAnalysis()
        assertTrue(cloud.start(evidence, 5_000))
        val advice = CloudSupplement(null, "Tigreal tem armadura observada convém considerar penetração.", listOf("Tigreal"))
        cloud.finish(evidence, CloudOutcome(advice, null), true, 7_000)
        val similar = evidence.copy(minute = 8, players = evidence.players.map {
            if (it.hero == "Layla") it.copy(gold = 4300) else it
        })
        assertEquals(advice, cloud.supplement(similar, 10_000))
        assertNull(cloud.supplement(similar, 97_000))
    }

    @Test
    fun failureRetainsBaseAndAllowsRetryAfterCooldown() {
        val evidence = checkNotNull(CloudEvidence.from(result, 5_000))
        val cloud = CloudAnalysis()
        assertTrue(cloud.start(evidence, 5_000))
        cloud.finish(evidence, CloudOutcome(null, "IA indisponível"), true, 6_000)
        assertNull(cloud.supplement(evidence, 6_000))
        assertTrue(cloud.status(evidence, 6_000).contains("Base ativa"))
        assertFalse(cloud.start(evidence, 64_999))
        assertTrue(cloud.start(evidence, 65_000))
    }
}
