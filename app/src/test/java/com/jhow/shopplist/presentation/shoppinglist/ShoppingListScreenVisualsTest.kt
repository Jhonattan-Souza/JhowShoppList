package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingListScreenVisualsTest {
    @Test
    fun `selected pending rows use primary container colors`() {
        assertEquals(
            PendingItemRowColorRoles(
                container = ShoppingListColorRole.PrimaryContainer,
                content = ShoppingListColorRole.OnPrimaryContainer
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

    @Test
    fun `wide shopping surfaces use pill compatible shape`() {
        assertEquals(
            RoundedCornerShape(percent = 50),
            shoppingListWideSurfaceShape
        )
    }

    @Test
    fun `list bottom padding follows measured composer height`() {
        assertEquals(148.dp, shoppingListBottomContentPadding(inputBarHeight = 148.dp))
    }

    @Test
    fun `strikethrough y position follows rendered glyph center`() {
        assertEquals(
            18f,
            shoppingListStrikethroughCenterY(glyphTopPx = 10f, glyphBottomPx = 26f)
        )
    }
}
