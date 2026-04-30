package com.jhow.shopplist.presentation.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.BreakfastDining
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Soap
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tapas
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.jhow.shopplist.domain.icon.IconBucket

object BucketIcons {
    fun forBucket(bucket: IconBucket): ImageVector = when (bucket) {
        IconBucket.FRUIT -> Icons.Rounded.LocalFlorist
        IconBucket.VEGETABLE -> Icons.Rounded.Eco
        IconBucket.DAIRY -> Icons.Rounded.WaterDrop
        IconBucket.EGG -> Icons.Rounded.Egg
        IconBucket.CHEESE -> Icons.Rounded.Tapas
        IconBucket.MEAT -> Icons.Rounded.LocalDining
        IconBucket.FISH -> Icons.Rounded.SetMeal
        IconBucket.DELI_COLD_CUTS -> Icons.Rounded.LunchDining
        IconBucket.BREAD -> Icons.Rounded.BreakfastDining
        IconBucket.GRAIN -> Icons.Rounded.Agriculture
        IconBucket.PASTA -> Icons.Rounded.RamenDining
        IconBucket.BEVERAGES_COLD -> Icons.Rounded.LocalDrink
        IconBucket.BEVERAGES_HOT -> Icons.Rounded.LocalCafe
        IconBucket.ALCOHOL -> Icons.Rounded.LocalBar
        IconBucket.FROZEN -> Icons.Rounded.AcUnit
        IconBucket.SNACKS -> Icons.Rounded.Fastfood
        IconBucket.SWEETS -> Icons.Rounded.Icecream
        IconBucket.CONDIMENTS -> Icons.Rounded.SoupKitchen
        IconBucket.MERCEARIA -> Icons.Rounded.Storefront
        IconBucket.PANTRY -> Icons.Rounded.Kitchen
        IconBucket.CLEANING -> Icons.Rounded.CleaningServices
        IconBucket.PERSONAL_CARE -> Icons.Rounded.Soap
        IconBucket.PET -> Icons.Rounded.Pets
        IconBucket.BABY -> Icons.Rounded.ChildCare
        IconBucket.GENERIC -> Icons.Rounded.ShoppingBasket
    }
}
