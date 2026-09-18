package ru.larpinovplay.finniapp.presentation.screens.petroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.screens.home.HomeAction
import ru.larpinovplay.finniapp.presentation.screens.home.HomeScreen
import ru.larpinovplay.finniapp.presentation.screens.home.HomeSection
import ru.larpinovplay.finniapp.presentation.screens.home.GoalUi
import ru.larpinovplay.finniapp.presentation.screens.home.HomeUiState
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsScreen
import ru.larpinovplay.finniapp.presentation.screens.settings.AppSettings
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsScreen
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsUiState
import ru.larpinovplay.finniapp.presentation.screens.home.NeedUi
import ru.larpinovplay.finniapp.presentation.screens.home.PetStats
import ru.larpinovplay.finniapp.presentation.screens.home.SampleGame
import ru.larpinovplay.finniapp.presentation.screens.shop.PurchaseFeedback
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopScreen
import ru.larpinovplay.finniapp.presentation.screens.home.SectionStubScreen
import ru.larpinovplay.finniapp.presentation.screens.home.TaskUi
import ru.larpinovplay.finniapp.presentation.screens.tasks.TasksScreen
import ru.larpinovplay.finniapp.presentation.screens.home.WeekSummaryDialog
import ru.larpinovplay.finniapp.presentation.screens.progress.ProgressScreen
import ru.larpinovplay.finniapp.presentation.screens.progress.ProgressUiState

@Composable
fun PetRoomScreen(
    modifier: Modifier = Modifier,
    viewModel: PetRoomScreenViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    PetRoomScreenContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun PetRoomScreenContent(
    state: PetRoomState,
    onAction: (PetRoomScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            PetRoomState.Loading -> CircularProgressIndicator()
            is PetRoomState.Creation -> PetCreationContent(state, onAction)
            is PetRoomState.Loaded -> HomeRoute(state.pet)
        }
    }
}

@Composable
private fun PetCreationContent(
    state: PetRoomState.Creation,
    onAction: (PetRoomScreenAction) -> Unit,
) {
    when {
        state.species == null -> SpeciesStep(onAction)
        state.color == null -> ColorStep(onAction)
        else -> NameStep(state, onAction)
    }
}

@Composable
private fun SpeciesStep(onAction: (PetRoomScreenAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Выберите вид питомца")
        PetSpecies.entries.forEach { species ->
            Button(onClick = { onAction(PetRoomScreenAction.SpeciesSelected(species)) }) {
                Text(species.name)
            }
        }
    }
}

@Composable
private fun ColorStep(onAction: (PetRoomScreenAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Выберите цвет питомца")
        PetColor.entries.forEach { color ->
            Button(onClick = { onAction(PetRoomScreenAction.ColorSelected(color)) }) {
                Text(color.name)
            }
        }
    }
}

@Composable
private fun NameStep(
    state: PetRoomState.Creation,
    onAction: (PetRoomScreenAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Дайте имя питомцу")
        OutlinedTextField(
            value = state.name,
            onValueChange = { onAction(PetRoomScreenAction.NameChanged(it)) },
            enabled = !state.isCreating
        )
        Button(
            onClick = { onAction(PetRoomScreenAction.CreatePetClicked) },
            enabled = state.name.isNotBlank() && !state.isCreating
        ) {
            Text("Создать")
        }
    }
}

/**
 * Главный экран с локальной навигацией по разделам-заглушкам.
 * Когда появится NavHost (docs/07-screens.md#граф-навигации), разделы станут маршрутами.
 */
@Composable
private fun HomeRoute(pet: Pet) {
    val game = remember { SampleGame(mood = pet.mood.value) }
    var section by remember { mutableStateOf<HomeSection?>(null) }
    var weekSummary by remember { mutableStateOf<SampleGame.WeekSummary?>(null) }
    var settings by remember { mutableStateOf(AppSettings()) }   // TODO: хранить в DataStore вместе с GameState
    val current = section

    val homeState = HomeUiState.sample(pet).copy(
        balance = game.balance,
        stage = game.stage,
        activeTask = game.availableTasks.firstOrNull()?.let { TaskUi(title = it.title, reward = it.reward) },
        stats = PetStats(satiety = game.satiety, mood = game.mood),
        needs = listOf(NeedUi("Еда", covered = game.foodCovered)),
        savings = game.savings,
        goal = game.goal?.let { GoalUi(name = it.name, cost = it.cost) },
        tip = when {
            !settings.tipsEnabled -> null
            game.goal == null -> "Выбери цель в копилке!"
            else -> "Отложи немного на «${game.goal!!.name}»"
        },
        animationsEnabled = settings.animationsEnabled,
        week = game.week,
        moodExplanation = when {
            game.satiety < 30 -> "Голоден: купи еду в магазине"
            game.history.lastOrNull()?.let { it.stageAfter != it.stageBefore } == true -> "Подрос! Продолжай в том же духе"
            game.purchases.isNotEmpty() -> "Рад покупке: ${game.purchases.last().name.lowercase()}"
            else -> "Ждёт твоих решений"
        },
    )

    when (current) {
        null -> HomeScreen(
            state = homeState,
            onAction = { action ->
                when (action) {
                    is HomeAction.OpenSection -> section = action.section
                    HomeAction.FinishWeek -> weekSummary = game.finishWeek()
                    HomeAction.PetTapped -> Unit       // TODO: реакция питомца
                }
            }
        )
        HomeSection.SHOP -> ShopScreen(
            balance = game.balance,
            foodCovered = game.foodCovered,
            onBuy = { item ->
                when (val r = game.buy(item)) {
                    is SampleGame.PurchaseResult.Success -> PurchaseFeedback.Bought(item, r.balanceAfter)
                    is SampleGame.PurchaseResult.NotEnough -> PurchaseFeedback.NotEnough(item, r.missing)
                }
            },
            onGoToTasks = { section = HomeSection.TASKS },
            onBack = { section = null }
        )
        HomeSection.SETTINGS -> SettingsScreen(
            settings = settings,
            onSettingsChange = { settings = it },
            onBack = { section = null }
        )
        HomeSection.TASKS -> TasksScreen(
            statusOf = game::taskStatus,
            tasksDoneThisWeek = game.tasksDoneThisWeek,
            tasksPerWeek = 2,
            onAnswer = game::answerTask,
            onBack = { section = null }
        )
        HomeSection.PROGRESS -> ProgressScreen(
            state = ProgressUiState(
                petName = pet.name,
                week = game.week,
                stage = game.stage,
                growthPoints = game.growthPoints,
                pointsToNextStage = game.pointsToNextStage,
                goal = game.goal,
                savings = game.savings,
                completedGoals = game.completedGoals.toList(),
                taskTopics = game.taskTopics,
                lastWeek = game.history.lastOrNull(),
                weeksCompleted = game.history.size,
                ledgerThisWeek = game.ledger.filter { it.week == game.week },
            ),
            onBack = { section = null }
        )
        HomeSection.SAVINGS -> SavingsScreen(
            state = SavingsUiState(
                balance = game.balance,
                savings = game.savings,
                goal = game.goal,
                weeksToGoal = game.weeksToGoal(),
                completedGoalIds = game.completedGoals.map { it.id }.toSet(),
            ),
            onChooseGoal = game::chooseGoal,
            onDeposit = game::deposit,
            onReachGoal = game::reachGoal,
            onBack = { section = null }
        )
        else -> SectionStubScreen(section = current, onBack = { section = null })
    }
    weekSummary?.let { WeekSummaryDialog(it, onDismiss = { weekSummary = null }) }
}
