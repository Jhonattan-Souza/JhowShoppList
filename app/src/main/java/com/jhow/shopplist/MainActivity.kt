package com.jhow.shopplist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jhow.shopplist.navigation.Routes
import com.jhow.shopplist.presentation.caldavconfig.CalDavConfigRoute
import com.jhow.shopplist.presentation.icon.IconResolver
import com.jhow.shopplist.presentation.shoppinglist.ShoppingListRoute
import com.jhow.shopplist.ui.theme.JhowShoppListTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var iconResolver: IconResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JhowShoppListTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Routes.SHOPPING_LIST
                    ) {
                        composable(Routes.SHOPPING_LIST) {
                            ShoppingListRoute(
                                onNavigateToCalDavConfig = {
                                    navController.navigate(Routes.CALDAV_CONFIG)
                                },
                                iconResolver = iconResolver
                            )
                        }
                        composable(Routes.CALDAV_CONFIG) {
                            CalDavConfigRoute(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
