package com.jhow.shopplist.presentation.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BreakfastDining
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.jhow.shopplist.domain.icon.IconBucket

object BucketIcons {
    fun forBucket(bucket: IconBucket): ImageVector = when (bucket) {
        IconBucket.DAIRY -> Icons.Rounded.WaterDrop
        IconBucket.FRUIT -> Icons.Rounded.Spa
        IconBucket.BREAD -> Icons.Rounded.BreakfastDining
        IconBucket.PANTRY_CANNED -> Icons.Rounded.Kitchen
        IconBucket.GENERIC -> Icons.Rounded.ShoppingBasket
    }
}
