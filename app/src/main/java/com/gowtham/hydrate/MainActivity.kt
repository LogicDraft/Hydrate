package com.gowtham.hydrate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.gowtham.hydrate.navigation.HydrateNavGraph
import com.gowtham.hydrate.ui.HydrateViewModel
import com.gowtham.hydrate.ui.theme.HydrateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HydrateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HydrateApp(viewModel)
        }
    }
}

@Composable
private fun HydrateApp(viewModel: HydrateViewModel) {
    HydrateTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            val navController = rememberNavController()
            HydrateNavGraph(navController = navController, viewModel = viewModel)
        }
    }
}
