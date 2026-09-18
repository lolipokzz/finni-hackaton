package ru.larpinovplay.finniapp.presentation.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.larpinovplay.finniapp.presentation.components.PetHost
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.screens.home.HomeScreen
import ru.larpinovplay.finniapp.presentation.screens.home.HomeSection
import ru.larpinovplay.finniapp.presentation.screens.home.SectionStubScreen
import ru.larpinovplay.finniapp.presentation.screens.progress.ProgressScreen
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsScreen
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsScreen
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopScreen
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskPlayScreen
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskResultCard
import ru.larpinovplay.finniapp.presentation.screens.tasks.TasksScreen

/**
 * Единственный NavDisplay приложения: здесь описан весь граф (см. [Routes.kt][Home]).
 * Экран сам берёт свой ViewModel и ничего не знает о соседях; куда идти дальше, решается здесь.
 */
@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)
    // Питомец живёт над графом, а не внутри Home: экран уходит из композиции, а модель должна остаться
    val petHost = remember { PetHostState() }

    Box(modifier) {
        NavDisplay(
            backStack = backStack,
            onBack = backStack::goBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),   // ViewModel экрана живёт, пока экран в стеке
            ),
            sceneStrategies = listOf(remember { DialogSceneStrategy<NavKey>() }),
            entryProvider = entryProvider {
                entry<Home> {
                    // Питомца показываем, только когда Home стоит на месте: SurfaceView не следует за анимацией перехода
                    val transition = LocalNavAnimatedContentScope.current.transition
                    val settled = transition.currentState == EnterExitState.Visible &&
                        transition.targetState == EnterExitState.Visible
                    SideEffect { petHost.shown = settled }
                    DisposableEffect(Unit) { onDispose { petHost.shown = false } }

                    HomeScreen(
                        onOpenSection = { section -> backStack.goTo(section.toRoute()) },
                        petHost = petHost,
                    )
                }

                entry<Tasks> {
                    TasksScreen(
                        onOpenTask = { task -> backStack.goTo(TaskPlay(task.id)) },
                        onBack = backStack::goBack,
                    )
                }

                entry<TaskPlay> { route ->
                    TaskPlayScreen(
                        viewModel = koinViewModel { parametersOf(route.taskId) },
                        onCompleted = { outcome ->
                            backStack.goBack()   // TaskPlay → Tasks, итог покажется поверх списка
                            if (outcome != null) backStack.goTo(TaskResult(route.taskId, outcome))
                        },
                        onBack = backStack::goBack,
                    )
                }

                entry<TaskResult>(metadata = DialogSceneStrategy.dialog()) { route ->
                    TaskResultCard(result = route.outcome, onDismiss = backStack::goBack)
                }

                entry<Shop> {
                    ShopScreen(
                        onGoToTasks = { backStack.goTo(Tasks) },
                        onBack = backStack::goBack,
                    )
                }

                entry<Savings> { SavingsScreen(onBack = backStack::goBack) }

                entry<Progress> { ProgressScreen(onBack = backStack::goBack) }

                entry<Settings> { SettingsScreen(onBack = backStack::goBack) }

                entry<Adult> {
                    SectionStubScreen(section = HomeSection.ADULT, onBack = backStack::goBack)
                }
            },
        )
        PetHost(petHost)
    }
}

/** Открыть [route]; повторный тап по той же кнопке не кладёт в стек второй такой же экран. */
private fun NavBackStack<NavKey>.goTo(route: NavKey) {
    if (lastOrNull() != route) add(route)
}

/** Назад на один экран. Корень ([Home]) не снимаем: пустой стек нечем отобразить. */
private fun NavBackStack<NavKey>.goBack() {
    if (size > 1) removeAt(lastIndex)
}
