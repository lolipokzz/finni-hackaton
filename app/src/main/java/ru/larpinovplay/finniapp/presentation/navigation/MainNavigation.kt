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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.larpinovplay.finniapp.presentation.components.PetHostOwner
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.screens.adult.AdultScreen
import ru.larpinovplay.finniapp.presentation.screens.adventure.AdventureScreen
import ru.larpinovplay.finniapp.presentation.screens.home.HomeScreen
import ru.larpinovplay.finniapp.presentation.screens.progress.ProgressScreen
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsScreen
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsScreen
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopScreen
import ru.larpinovplay.finniapp.presentation.screens.wardrobe.WardrobeScreen
import ru.larpinovplay.finniapp.presentation.screens.tasks.LevelPlayScreen
import ru.larpinovplay.finniapp.presentation.screens.tasks.TasksScreen

/**
 * Единственный NavDisplay приложения: здесь описан весь граф (см. [Routes.kt][Home]). Питомца рисует [PetHost] уровнем
 * выше: сюда приходит только [petHost] с его состоянием.
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
                    SideEffect { petHost.show(PetHostOwner.HOME, visible) }
                    DisposableEffect(Unit) { onDispose { petHost.show(PetHostOwner.HOME, false) } }

                    HomeScreen(
                        onOpenSection = { section -> backStack.goTo(section.toRoute()) },
                        petHost = petHost,
                    )
                }

                entry<Tasks> {
                    TasksScreen(
                        onOpenLevel = { level, challenge -> backStack.goTo(LevelPlay(level.id, challenge)) },
                        onOpenAdventure = { adventure -> backStack.goTo(AdventurePlay(adventure.id)) },
                        onBack = backStack::goBack,
                    )
                }

                entry<AdventurePlay> { route ->
                    AdventureScreen(
                        viewModel = koinViewModel { parametersOf(route.adventureId) },
                        onBack = backStack::goBack,
                    )
                }

                entry<LevelPlay> { route ->
                    LevelPlayScreen(
                        viewModel = koinViewModel { parametersOf(route.levelId, route.challenge) },
                        onBack = backStack::goBack,
                    )
                }

                entry<Shop> {
                    ShopScreen(
                        onGoToTasks = { backStack.goTo(Tasks) },
                        onGoToWardrobe = { backStack.goTo(Wardrobe) },
                        onBack = backStack::goBack,
                    )
                }

                entry<Wardrobe> {
                    // Гардероб показывает того же питомца, что и Home: вещь видна на нём сразу
                    val transition = LocalNavAnimatedContentScope.current.transition
                    val visible = transition.targetState == EnterExitState.Visible
                    SideEffect { petHost.show(PetHostOwner.WARDROBE, visible) }
                    DisposableEffect(Unit) { onDispose { petHost.show(PetHostOwner.WARDROBE, false) } }

                    WardrobeScreen(
                        petHost = petHost,
                        onGoToShop = { backStack.goTo(Shop) },
                        onBack = backStack::goBack,
                    )
                }

                entry<Savings> { SavingsScreen(onBack = backStack::goBack) }

                entry<Progress> { ProgressScreen(onBack = backStack::goBack) }

                entry<Settings> { SettingsScreen(onBack = backStack::goBack, onOpenAdult = { backStack.goTo(Adult) }) }

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
