package com.jhow.shopplist.presentation.shoppinglist

import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingListScreenVisualsTest {
    @Test
    fun `selected pending rows use secondary container colors`() {
        assertEquals(
            PendingItemRowColorRoles(
                container = ShoppingListColorRole.SecondaryContainer,
                content = ShoppingListColorRole.OnSecondaryContainer
            ),
            pendingItemRowColorRoles(isSelected = true)
        )
    }

    @Test
    fun `unselected pending rows use surface low colors`() {
        assertEquals(
            PendingItemRowColorRoles(
                container = ShoppingListColorRole.SurfaceContainerLow,
                content = ShoppingListColorRole.OnSurface
            ),
            pendingItemRowColorRoles(isSelected = false)
        )
    }
}
