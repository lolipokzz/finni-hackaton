package ru.larpinovplay.finniapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.screens.petroom.PetRoomScreen
import ru.larpinovplay.finniapp.presentation.theme.FinniAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinniAppTheme {
                CompositionLocalProvider(LocalFeedback provides koinInject<Feedback>()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        PetRoomScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}