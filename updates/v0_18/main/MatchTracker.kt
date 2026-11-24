package com.mlbb.assistant

import java.util.UUID
import kotlin.math.abs

enum class SlotState {
    NOT_OBSERVED, UNREADABLE, EMPTY_CONFIRMED, ITEM_CANDIDATE, ITEM_CONFIRMED, ITEM_UNKNOWN
}

data class SlotObservation(
    val index: Int,
    val state: SlotState,
    val item: String? = null,
    val distance: Double? = null,
    val margin: Double? = null,
    val observedAt: Long = 0,
    val confirmedAt: Long? = null,
    val candidate: String? = null,
    val origin: String = "placar"
)

/** Candidate frames and confirmed fields have independent timestamps. */
internal class RepeatedField<T>(private val ttlMs: Long) {
    data class Confirmed<T>(val value: T, val confirmedAt: Long, val lastSeenAt: Long)
    private var candidate: T? = null
    private var candidateAt = -1L
    private var confirmed: Confirmed<T>? = null

    fun current(now: Long) = confirmed?.takeIf { now - it.lastSeenAt in 0..ttlMs }

    fun observe(value: T?, now: Long, agrees: (T, T) -> Boolean = { old, new -> old == new }) {
        if (now <= candidateAt) return
        val old = current(now)
        val pending = candidate
        val sameAsConfirmed = old != null && value != null && agrees(old.value, value)
        val repeated = pending != null && value != null && agrees(pending, value) && now - candidateAt <= 5_000L
        confirmed = when {
            sameAsConfirmed && value != null -> old!!.copy(value = value, lastSeenAt = now)
            repeated && value != null -> Confirmed(value, now, now)
            else -> old
        }
        candidate = value
        candidateAt = now
    }
}

internal data class RowReading(
    val side: Int,
    val row: Int,
    val hero: String?,
    val slots: List<SlotObservation>,
    val gold: String? = null,
    val kda: String? = null,
    val level: Int? = null,
    val distance: Double? = null,
    val margin: Double? = null
)

internal class MatchTracker(private val prefix: String = UUID.randomUUID().toString()) {
    companion object { const val FIELD_TTL_MS = 45_000L }

    private data class SlotValue(val state: SlotState, val item: String?)
    private class Track(val id: Int, val side: Int, var row: Int) {
        val hero = RepeatedField<String>(FIELD_TTL_MS)
        val gold = RepeatedField<String>(FIELD_TTL_MS)
        val kda = RepeatedField<String>(FIELD_TTL_MS)
        val level = RepeatedField<Int>(FIELD_TTL_MS)
        val slots = List(6) { RepeatedField<SlotValue>(FIELD_TTL_MS) }
        var readings = emptyList<SlotObservation>()
        var seenAt = -1L
        var distance: Double? = null
        var margin: Double? = null
    }

    data class Player(
        val id: String, val side: Int, val row: Int, val hero: RepeatedField.Confirmed<String>?,
        val slots: List<SlotObservation>, val gold: RepeatedField.Confirmed<String>?,
        val kda: RepeatedField.Confirmed<String>?, val level: RepeatedField.Confirmed<Int>?,
        val observedAt: Long, val distance: Double?, val margin: Double?
    )

    data class Snapshot(
        val matchId: String, val version: Long, val players: List<Player>, val own: Player?,
        val clock: String?, val estimatedClock: Boolean, val changes: List<String>, val boardAt: Long
    )

    private val tracks = mutableListOf<Track>()
    private var ownId = RepeatedField<Int>(FIELD_TTL_MS)
    private var clock = RepeatedField<Int>(15_000L)
    private var rollback = RepeatedField<Int>(5_000L)
    private var serial = 0
    private var nextId = 1
    private var version = 0L
    private var boardAt = -1L
    private var priorRoster = emptySet<Pair<Int, String>>()
    private var possibleRoster = emptySet<Pair<Int, String>>()
    private var recentChanges = emptyList<String>()

    private val matchId get() = "$prefix-$serial"

    fun reset() {
        tracks.clear()
        ownId = RepeatedField(FIELD_TTL_MS)
        clock = RepeatedField(15_000L)
        rollback = RepeatedField(5_000L)
        serial++
        version = 0L
        boardAt = -1L
        priorRoster = emptySet()
        possibleRoster = emptySet()
        recentChanges = emptyList()
    }

    fun observeClock(seconds: Int?, at: Long) {
        val known = clock.current(at)
        val backwards = known != null && seconds != null && known.value - seconds >= 30
        if (backwards) {
            rollback.observe(seconds, at) { a, b -> b - a in 0..5 }
            if (rollback.current(at) == null) return
            reset()
        }
        clock.observe(seconds, at) { a, b -> b - a in 0..8 }
    }

    fun observe(rows: List<RowReading>, highlight: Pair<Int, Int>?, manualHero: String?, at: Long): Snapshot {
        if (rows.isEmpty() || at <= boardAt) return snapshot(at)
        val roster = rows.mapNotNull { r -> r.hero?.let { r.side to it } }.toSet()
        if (roster.size >= 6 && priorRoster.size >= 6 && roster.intersect(priorRoster).size <= 2) {
            if (possibleRoster.intersect(roster).size >= 5) reset()
            else {
                possibleRoster = roster
                return snapshot(at)
            }
        }
        if (roster.size >= 6) priorRoster = roster
        possibleRoster = emptySet()
        tracks.removeAll { at - it.seenAt > FIELD_TTL_MS }
        val claimed = mutableSetOf<Track>()
        val assignments = linkedMapOf<RowReading, Track>()
        val duplicateKeys = rosterKeysWithDuplicates(rows)
        // Match by confirmed hero and side before using position, so roster reordering follows the player.
        for (reading in rows) {
            val key = reading.side to reading.hero
            val found = tracks.singleOrNull {
                it.side == reading.side && it.hero.current(at)?.value == reading.hero && reading.hero != null
            }
            if (key !in duplicateKeys && found != null && claimed.add(found)) assignments[reading] = found
        }
        for (reading in rows.filter { it !in assignments }) {
            val existing = tracks.filter { it !in claimed && it.side == reading.side && it.row == reading.row }
                .filter { it.hero.current(at)?.value?.let { old -> reading.hero == null || old == reading.hero } != false }
                .maxByOrNull { it.seenAt }
            val track = existing ?: Track(nextId++, reading.side, reading.row).also(tracks::add)
            claimed.add(track)
            assignments[reading] = track
        }

        // Clear superseded positions while allowing absent rows from partial captures to age normally.
        val latestPositions = assignments.keys.map { it.side to it.row }.toSet()
        tracks.removeAll { it !in claimed && (it.side to it.row) in latestPositions }
        val changes = mutableListOf<String>()
        for ((reading, track) in assignments) {
            track.row = reading.row
            track.seenAt = at
            track.hero.observe(reading.hero, at)
            track.distance = reading.distance
            track.margin = reading.margin
            track.gold.observe(reading.gold, at) { a, b -> ObservationParser.compatibleGold(a, b) != null }
            track.kda.observe(reading.kda, at)
            track.level.observe(reading.level, at)
            track.readings = reading.slots
            for (slot in reading.slots) {
                val field = track.slots[slot.index]
                val before = field.current(at)?.value
                val value = when (slot.state) {
                    SlotState.ITEM_CANDIDATE, SlotState.ITEM_CONFIRMED -> SlotValue(SlotState.ITEM_CONFIRMED, slot.item)
                    SlotState.EMPTY_CONFIRMED -> SlotValue(SlotState.EMPTY_CONFIRMED, null)
                    else -> null
                }
                field.observe(value, at)
                val after = field.current(at)?.value
                if (before != null && after != null && before != after) {
                    val label = track.hero.current(at)?.value ?: "Lado ${track.side + 1}, linha ${track.row + 1}"
                    changes.add("$label • slot ${slot.index + 1} • ${before.item ?: "vazio"} → ${after.item ?: "vazio"}")
                }
            }
        }
        val highlighted = assignments.entries.singleOrNull {
            highlight == (it.key.side to it.key.row) && it.value.hero.current(at) != null
        }?.value
        val manual = if (manualHero == null) null else assignments.values.distinct().singleOrNull {
            it.hero.current(at)?.value == manualHero
        }
        // Visual evidence takes priority. An explicit manual selection assists only a uniquely matching row.
        ownId.observe((highlighted ?: manual)?.id, at)
        recentChanges = changes
        boardAt = at
        version++
        return snapshot(at)
    }

    fun snapshot(at: Long): Snapshot {
        val active = tracks.filter { at - it.seenAt in 0..FIELD_TTL_MS }.sortedWith(compareBy({ it.side }, { it.row }))
        val players = active.map { track ->
            val slots = track.slots.mapIndexed { index, field ->
                val confirmed = field.current(at)
                val raw = track.readings.singleOrNull { it.index == index }
                if (confirmed == null) raw?.copy(
                    state = if (raw.state == SlotState.EMPTY_CONFIRMED) SlotState.UNREADABLE else raw.state,
                    observedAt = track.seenAt
                ) ?: SlotObservation(index, SlotState.NOT_OBSERVED) else SlotObservation(
                    index, confirmed.value.state, confirmed.value.item, raw?.distance, raw?.margin,
                    confirmed.lastSeenAt, confirmed.confirmedAt, raw?.item?.takeIf { it != confirmed.value.item }
                )
            }
            Player("$matchId:${track.id}", track.side, track.row, track.hero.current(at),
                slots, track.gold.current(at), track.kda.current(at), track.level.current(at),
                track.seenAt, track.distance, track.margin)
        }
        val own = ownId.current(at)?.value?.let { id -> players.singleOrNull { it.id == "$matchId:$id" && it.hero != null } }
        val clockValue = clock.current(at)
        val elapsed = clockValue?.let { ((at - it.lastSeenAt) / 1000).toInt().coerceAtLeast(0) } ?: 0
        val seconds = clockValue?.value?.plus(elapsed)
        val display = seconds?.let { java.lang.String.format(java.util.Locale.ROOT, "%02d:%02d", it / 60, it % 60) }
        return Snapshot(matchId, version, players, own, display, elapsed > 0, recentChanges, boardAt)
    }

    private fun rosterKeysWithDuplicates(rows: List<RowReading>) = rows.map { it.side to it.hero }
        .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}

/** Adjacent row regions are partitioned at their midpoint, with a bounded outside margin. */
internal object RowBounds {
    fun includes(centres: List<Int>, row: Int, y: Int, maxMargin: Int): Boolean {
        val centre = centres[row]
        val lower = if (row == 0) centre - maxMargin else maxOf(centre - maxMargin, (centres[row - 1] + centre) / 2)
        val upper = if (row == centres.lastIndex) centre + maxMargin else minOf(centre + maxMargin, (centre + centres[row + 1]) / 2)
        return y in lower until upper && abs(y - centre) <= maxMargin
    }
}
