package com.gowtham.hydrate.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gowtham.hydrate.ui.HydrateViewModel
import com.gowtham.hydrate.ui.history.HistoryScreen
import com.gowtham.hydrate.ui.onboarding.OnboardingScreen
import com.gowtham.hydrate.ui.schedule.ScheduleScreen
import com.gowtham.hydrate.ui.settings.SettingsScreen
import com.gowtham.hydrate.ui.today.TodayScreen

object HydrateRoutes {
    const val Onboarding = "onboarding"
    const val Today = "today"
    const val Schedule = "schedule"
    const val History = "history"
    const val Settings = "settings"
}

@Composable
fun HydrateNavGraph(
    navController: NavHostController,
    viewModel: HydrateViewModel,
) {
    NavHost(navController = navController, startDestination = HydrateRoutes.Onboarding) {
        composable(HydrateRoutes.Onboarding) {
            OnboardingScreen(
                onContinue = {
                    viewModel.saveOnboarding()
                    navController.navigate(HydrateRoutes.Today) {
                        popUpTo(HydrateRoutes.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable(HydrateRoutes.Today) {
            TodayScreen(
                onOpenSchedule = { navController.navigate(HydrateRoutes.Schedule) },
                onOpenHistory = { navController.navigate(HydrateRoutes.History) },
                onOpenSettings = { navController.navigate(HydrateRoutes.Settings) },
            )
        }
        composable(HydrateRoutes.Schedule) { ScheduleScreen(onBack = { navController.popBackStack() }) }
        composable(HydrateRoutes.History) { HistoryScreen(onBack = { navController.popBackStack() }) }
        composable(HydrateRoutes.Settings) { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
