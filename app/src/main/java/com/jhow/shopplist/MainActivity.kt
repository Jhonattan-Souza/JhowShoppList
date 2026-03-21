package com.jhow.shopplist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.jhow.shopplist.presentation.shoppinglist.ShoppingListRoute
import com.jhow.shopplist.ui.theme.JhowShoppListTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JhowShoppListTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShoppingListRoute(viewModel = hiltViewModel())
                }
            }
        }
    }
}
