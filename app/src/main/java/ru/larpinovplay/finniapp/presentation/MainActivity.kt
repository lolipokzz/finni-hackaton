package ru.larpinovplay.finniapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.presentation.components.LocalRoom
import ru.larpinovplay.finniapp.presentation.components.RoomLook
import ru.larpinovplay.finniapp.presentation.components.PetHost
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.pet.PetWarmUpSpec
import ru.larpinovplay.finniapp.presentation.screens.petcreation.PetCreationScreen
import ru.larpinovplay.finniapp.presentation.theme.FinniAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinniAppTheme {
                // Достигнутые цели копилки (кроватка, велосипед, ремонт, поездка) видны в комнате на всех экранах с фоном-комнатой
                val game = koinInject<GameRepository>()
                val snapshot by game.snapshot.collectAsState()
                val state = snapshot?.state
                val completedGoals = state?.completedGoals
                val tripGoal = state?.currentTrip?.goalId
                val room = remember(completedGoals, tripGoal) {
                    RoomLook(goals = completedGoals?.mapTo(mutableSetOf()) { it.id }.orEmpty(), tripGoal = tripGoal)
                }
                CompositionLocalProvider(LocalFeedback provides koinInject<Feedback>(), LocalRoom provides room) {
                    // Питомец рисуется поверх всего и создаётся заранее: см. PetHost
                    val petHost = remember { PetHostState(warmUp = PetWarmUpSpec) }
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(Modifier.fillMaxSize().padding(innerPadding)) {
                            PetCreationScreen(petHost = petHost)
                            PetHost(petHost)
                        }
                    }
                }
            }
        }
    }
}