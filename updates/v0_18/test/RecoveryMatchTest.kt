package com.mlbb.assistant

import org.junit.Assert.*
import org.junit.Test

class RecoveryMatchTest {
    private fun item(index: Int = 0, name: String = "Dagger") =
        SlotObservation(index, SlotState.ITEM_CANDIDATE, name)

    private fun row(hero: String? = "Layla", slots: List<SlotObservation> = listOf(item()), side: Int = 0, row: Int = 0) =
        RowReading(side, row, hero, slots)

    @Test fun replacement_is_confirmed_once_without_removing_old_item_early() {
        val tracker = MatchTracker("test")
        for (now in listOf(1000L, 2000L)) tracker.observe(listOf(row()), null, null, now)
        val new = row(slots = listOf(item(name = "Swift Boots")))
        val first = tracker.observe(listOf(new), null, null, 3000)
        assertEquals("Dagger", first.players.single().slots[0].item)
        assertEquals("Swift Boots", first.players.single().slots[0].candidate)
        assertTrue(first.changes.isEmpty())
        val second = tracker.observe(listOf(new), null, null, 4000)
        assertEquals("Swift Boots", second.players.single().slots[0].item)
        assertTrue(second.changes.single().contains("Dagger → Swift Boots"))
        assertTrue(tracker.observe(listOf(new), null, null, 5000).changes.isEmpty())
    }

    @Test fun unreadable_candidate_drop_and_confirmed_empty_have_distinct_meanings() {
        val tracker = MatchTracker("test")
        fun set(time: Long, slot: SlotObservation) =
            tracker.observe(listOf(row(slots = listOf(slot))), null, null, time)
        set(1000, item()); set(2000, item())
        val unknown = SlotObservation(0, SlotState.UNREADABLE)
        assertEquals("Dagger", set(3000, unknown).players.single().slots[0].item)
        assertTrue(set(4000, item()).changes.isEmpty())
        set(5000, item(name = "Knife"))
        assertTrue(set(6000, item()).changes.isEmpty())
        set(7000, unknown); set(8000, item(name = "Knife"))
        assertTrue(set(9000, item(name = "Knife")).changes.single().contains("Dagger → Knife"))
        val empty = SlotObservation(0, SlotState.EMPTY_CONFIRMED)
        assertEquals("Knife", set(10000, empty).players.single().slots[0].item)
        assertEquals(SlotState.EMPTY_CONFIRMED, set(11000, empty).players.single().slots[0].state)
    }

    @Test fun a_row_keeps_items_without_portrait_and_duplicate_heroes_stay_separate() {
        val tracker = MatchTracker("test")
        val rows = listOf(row(hero = null), row(side = 0, row = 1), row(side = 1))
        tracker.observe(rows, null, null, 1000)
        val confirmed = tracker.observe(rows, null, "Layla", 2000)
        assertEquals(3, confirmed.players.size)
        assertNull(confirmed.players.first().hero)
        assertEquals("Dagger", confirmed.players.first().slots.first().item)
        assertEquals(2, confirmed.players.count { it.hero?.value == "Layla" })
        assertNull(tracker.observe(rows, null, "Layla", 3000).own)
        tracker.observe(rows, 0 to 1, "Layla", 4000)
        assertEquals(0, tracker.observe(rows, 0 to 1, "Layla", 5000).own?.side)
    }

    @Test fun reordering_preserves_player_and_a_partial_frame_preserves_older_rows() {
        val tracker = MatchTracker("test")
        val a = row()
        val b = row("Tigreal", listOf(item(name = "Blade Armor")), row = 1)
        tracker.observe(listOf(a,b), null, null, 1000)
        val first = tracker.observe(listOf(a,b), null, null, 2000)
        val moved = tracker.observe(listOf(a.copy(row = 1), b.copy(row = 0)), null, null, 3000)
        assertEquals(first.players.first().id, moved.players.last().id)
        assertEquals("Dagger", moved.players.last().slots.first().item)
        assertEquals(2, tracker.observe(listOf(a.copy(row = 1)), null, null, 4000).players.size)
        assertEquals(0, tracker.snapshot(50_001).players.size)
    }

    @Test fun clocks_expire_and_a_confirmed_rollback_starts_new_match() {
        val tracker = MatchTracker("test")
        tracker.observe(listOf(row()), null, null, 1000)
        tracker.observeClock(180, 1000)
        tracker.observeClock(181, 2000)
        val before = tracker.snapshot(3000)
        assertEquals("03:02", before.clock)
        assertTrue(before.estimatedClock)
        assertNull(tracker.snapshot(18000).clock)
        tracker.observeClock(3, 3000)
        tracker.observeClock(4, 4000)
        val after = tracker.snapshot(4000)
        assertNotEquals(before.matchId, after.matchId)
        assertTrue(after.players.isEmpty())
    }

    @Test fun ocr_regions_are_disjoint_and_scheduling_recovers_phase() {
        val centres = listOf(100, 140, 180)
        assertFalse(RowBounds.includes(centres, 0, 120, 32))
        assertTrue(RowBounds.includes(centres, 1, 120, 32))
        assertEquals(3000L, FrameBudget.nextDue(2020L, 2000L))
        assertEquals(4300L, FrameBudget.nextDue(3300L, 2000L))
    }
}
