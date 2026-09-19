package ru.larpinovplay.finniapp.presentation.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.screens.home.HomeScreen
import ru.larpinovplay.finniapp.presentation.screens.home.HomeSection
import ru.larpinovplay.finniapp.presentation.screens.adult.AdultScreen
import ru.larpinovplay.finniapp.presentation.screens.progress.ProgressScreen
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsScreen
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsScreen
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopScreen
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskPlayScreen
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskResultCard
import ru.larpinovplay.finniapp.presentation.screens.tasks.TasksScreen

/**
 * Единственный NavDisplay приложения: здесь описан весь граф (см. [Routes.kt][Home]).
 * Питомца рисует [PetHost] уровнем выше: сюда приходит только [petHost] с его состоянием.
 * Экран сам берёт свой ViewModel и ничего не знает о соседях; куда идти дальше, решается здесь.
 */
@Composable
fun MainNavigation(petHost: PetHostState, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)
    Box(modifier) {
        NavDisplay(
            backStack = backStack,
            onBack = backStack::goBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),   // ViewModel экрана живёт, пока экран в стеке
            ),
            sceneStrategies = listOf(remember { DialogSceneStrategy<NavKey>() }),
            // Только затухание, без сдвига: SurfaceView питомца не следует за анимацией Compose, а стандартные
            // 0.7 с перехода держат кнопки неактивными и заставляют ждать питомца
            transitionSpec = { FadeTransition },
            popTransitionSpec = { FadeTransition },
            predictivePopTransitionSpec = { FadeTransition },
            entryProvider = entryProvider {
                entry<Home> {
                    // Питомец виден, пока Home наверху или становится верхним; уход с Home скрывает его сразу
                    val transition = LocalNavAnimatedContentScope.current.transition
                    val visible = transition.targetState == EnterExitState.Visible
                    SideEffect { petHost.shown = visible }
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
                    AdultScreen(onBack = backStack::goBack)
                }
            },
        )
    }
}

private const val FADE_MILLIS = 150

private val FadeTransition = fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS))

/** Открыть [route]; повторный тап по той же кнопке не кладёт в стек второй такой же экран. */
private fun NavBackStack<NavKey>.goTo(route: NavKey) {
    if (lastOrNull() != route) add(route)
}

/** Назад на один экран. Корень ([Home]) не снимаем: пустой стек нечем отобразить. */
private fun NavBackStack<NavKey>.goBack() {
    if (size > 1) removeAt(lastIndex)
}
