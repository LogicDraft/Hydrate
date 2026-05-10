package com.gowtham.hydrate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.collectAsState
import com.gowtham.hydrate.ui.HydrateViewModel
import com.gowtham.hydrate.ui.onboarding.OnboardingScreen
import com.gowtham.hydrate.ui.schedule.ScheduleScreen
import com.gowtham.hydrate.ui.settings.AboutScreen
import com.gowtham.hydrate.ui.settings.SettingsScreen
import com.gowtham.hydrate.ui.today.TodayScreen

object HydrateRoutes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Today = "today"
    const val Schedule = "schedule"
    const val Settings = "settings"
    const val About = "about"
}

@Composable
fun HydrateNavGraph(
    navController: NavHostController,
    viewModel: HydrateViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    NavHost(navController = navController, startDestination = HydrateRoutes.Splash) {
        composable(HydrateRoutes.Splash) {
            LaunchedEffect(uiState.needsOnboarding) {
                navController.navigate(if (uiState.needsOnboarding) HydrateRoutes.Onboarding else HydrateRoutes.Today) {
                    popUpTo(HydrateRoutes.Splash) { inclusive = true }
                }
            }
        }
        composable(HydrateRoutes.Onboarding) {
            OnboardingScreen(
                initialPreferences = uiState.preferences,
                onSave = { preferences ->
                    viewModel.savePreferences(preferences)
                    navController.navigate(HydrateRoutes.Today) {
                        popUpTo(HydrateRoutes.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable(HydrateRoutes.Today) {
            TodayScreen(
                uiState = uiState,
                onQuickAdd = viewModel::quickAdd,
                onUndoLastLog = viewModel::undoLastLog,
                showTabTips = uiState.showTabTips,
                shouldCelebrateGoal = uiState.shouldCelebrateGoal,
                errorMessage = errorMessage,
                onClearErrorMessage = viewModel::clearErrorMessage,
                onDismissTabTips = viewModel::dismissTabTips,
                onCelebrationDisplayed = viewModel::acknowledgeGoalCelebrationShown,
                onOpenSchedule = { navController.navigate(HydrateRoutes.Schedule) },
                onOpenSettings = { navController.navigate(HydrateRoutes.Settings) },
            )
        }
        composable(HydrateRoutes.Schedule) {
            ScheduleScreen(
                uiState = uiState,
                onSnooze = viewModel::snoozeSlot,
                onSkip = viewModel::skipSlot,
                onBack = { navController.popBackStack() },
            )
        }
        composable(HydrateRoutes.Settings) {
            SettingsScreen(
                uiState = uiState,
                onSave = viewModel::savePreferences,
                onResetToday = viewModel::resetToday,
                onEraseData = viewModel::eraseAllData,
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(HydrateRoutes.About) },
            )
        }
        composable(HydrateRoutes.About) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
