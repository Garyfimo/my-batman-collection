package com.garyfimo.batmancollection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.garyfimo.batmancollection.navigation.BatmanNavDisplay
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BatmanCollectionTheme {
                BatmanNavDisplay()
            }
        }
    }
}
