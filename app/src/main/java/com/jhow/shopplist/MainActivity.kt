package com.jhow.shopplist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jhow.shopplist.data.icon.DictionaryLoader
import com.jhow.shopplist.domain.icon.DefaultIconMatcher
import com.jhow.shopplist.navigation.Routes
import com.jhow.shopplist.presentation.caldavconfig.CalDavConfigRoute
import com.jhow.shopplist.presentation.icon.IconResolver
import com.jhow.shopplist.presentation.shoppinglist.ShoppingListRoute
import com.jhow.shopplist.ui.theme.JhowShoppListTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var iconResolver: IconResolver

    @Inject
    lateinit var dictionaryLoader: DictionaryLoader

    @Inject
    lateinit var defaultIconMatcher: DefaultIconMatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadIconDictionary()
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

    private fun loadIconDictionary() {
        lifecycleScope.launch {
            val dictionary = try {
                dictionaryLoader.load()
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                Log.w(TAG, "Failed to load icon dictionary, using generic icons", error)
                return@launch
            } catch (error: SerializationException) {
                Log.w(TAG, "Failed to load icon dictionary, using generic icons", error)
                return@launch
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "Failed to load icon dictionary, using generic icons", error)
                return@launch
            }
            val dictionaryChanged = defaultIconMatcher.updateDictionary(dictionary)
            if (dictionaryChanged) {
                iconResolver.clearCache()
            }
        }
    }
}
