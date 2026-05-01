package com.jhow.shopplist.presentation.shoppinglist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionControllerTest {
    private val controller = SelectionController()

    @Test
    fun `long press entry activates selection mode with pressed item selected`() {
        controller.enter("milk")

        assertTrue(controller.isActive.value)
        assertEquals(setOf("milk"), controller.selected.value)
    }

    @Test
    fun `tapping while selection mode is active toggles item membership`() {
        controller.enter("milk")

        controller.toggle("tea")
        assertEquals(setOf("milk", "tea"), controller.selected.value)

        controller.toggle("tea")
        assertEquals(setOf("milk"), controller.selected.value)
    }

    @Test
    fun `emptying the selection exits selection mode`() {
        controller.enter("milk")

        controller.toggle("milk")

        assertFalse(controller.isActive.value)
        assertEquals(emptySet<String>(), controller.selected.value)
    }

    @Test
    fun `enter is idempotent while selection mode is already active`() {
        controller.enter("milk")

        controller.enter("tea")

        assertTrue(controller.isActive.value)
        assertEquals(setOf("milk"), controller.selected.value)
    }

    @Test
    fun `exit clears a non empty selection`() {
        controller.enter("milk")
        controller.toggle("tea")

        controller.exit()

        assertFalse(controller.isActive.value)
        assertEquals(emptySet<String>(), controller.selected.value)
    }

    @Test
    fun `retaining only valid ids exits when selection becomes empty`() {
        controller.enter("milk")

        controller.retainOnly(setOf("tea"))

        assertFalse(controller.isActive.value)
        assertEquals(emptySet<String>(), controller.selected.value)
    }
}
