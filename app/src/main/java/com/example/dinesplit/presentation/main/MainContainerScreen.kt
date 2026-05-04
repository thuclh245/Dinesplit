package com.example.dinesplit.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dinesplit.core.navigation.AppRoute
import com.example.dinesplit.core.navigation.BottomTab
import com.example.dinesplit.data.repository.LocalAuthRepository
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.presentation.feed.CreatePostScreen
import com.example.dinesplit.presentation.feed.FeedScreen
import com.example.dinesplit.presentation.feed.PostDetailScreen
import com.example.dinesplit.presentation.feed.SearchScreen
import com.example.dinesplit.presentation.personal.AddTransactionScreen
import com.example.dinesplit.presentation.personal.CategoryManagementRoute
import com.example.dinesplit.presentation.personal.HistoryRoute
import com.example.dinesplit.presentation.personal.PersonalRoute
import com.example.dinesplit.presentation.personal.PersonalViewModel
import com.example.dinesplit.presentation.personal.TransactionDetailScreen
import com.example.dinesplit.presentation.personal.toRouteValue
import com.example.dinesplit.presentation.personal.transactionTypeFromRoute
import com.example.dinesplit.presentation.profile.EditProfileScreen
import com.example.dinesplit.presentation.profile.OtherUserProfileScreen
import com.example.dinesplit.presentation.profile.ProfileScreen
import com.example.dinesplit.presentation.profile.ProfileViewModel
import com.example.dinesplit.presentation.split.SplitScreen

@Composable
fun MainContainerScreen(
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit,
    onLogout: () -> Unit
) {
    val mainNavController = rememberNavController()
    val profileViewModel: ProfileViewModel = viewModel()
    val profileUiState by profileViewModel.profileUiState.collectAsState()
    val editProfileUiState by profileViewModel.editUiState.collectAsState()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val context = LocalContext.current
    val authRepo = LocalAuthRepository.getInstance(context)
    val currentSession by authRepo.sessionFlow.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomTab.items.forEach { tab ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == tab.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            mainNavController.navigate(tab.route) {
                                popUpTo(mainNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(tab.label.take(1)) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = AppRoute.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Feed.route) {
                FeedScreen(
                    onOpenNotifications = onOpenNotifications,
                    onOpenAssistant = onOpenAssistant,
                    onCreatePost = { mainNavController.navigate(AppRoute.CreatePost.route) },
                    onOpenPostDetail = { postId ->
                        mainNavController.navigate(AppRoute.PostDetail.createRoute(postId))
                    },
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onOpenOtherUserProfile = { userName ->
                        mainNavController.navigate(AppRoute.OtherUserProfile.createRoute(userName))
                    }
                )
            }

            composable(AppRoute.CreatePost.route) {
                CreatePostScreen(onBack = { mainNavController.navigateUp() })
            }

            composable(AppRoute.Search.route) {
                SearchScreen(onBack = { mainNavController.navigateUp() })
            }

            composable(AppRoute.PostDetail.routeWithArg) { backStackEntry ->
                PostDetailScreen(
                    postId = backStackEntry.arguments?.getString(AppRoute.PostDetail.ARG_ID).orEmpty(),
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(AppRoute.OtherUserProfile.routeWithArg) { backStackEntry ->
                OtherUserProfileScreen(
                    userName = backStackEntry.arguments?.getString(AppRoute.OtherUserProfile.ARG_USER).orEmpty(),
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(AppRoute.Split.route) {
                SplitScreen()
            }

            composable(AppRoute.Personal.route) { backStackEntry ->
                val personalNavController = rememberNavController()
                
                // ✅ Scope PersonalViewModel to the Personal tab's root entry
                val sharedViewModel: PersonalViewModel = viewModel(viewModelStoreOwner = backStackEntry)
                
                // ✅ Fetch real categories from ViewModel
                val allCategories by sharedViewModel.categories.collectAsState()
                val categoriesByType = remember(allCategories) {
                    allCategories.groupBy { it.type }
                }

                // ✅ Sync User ID
                LaunchedEffect(currentSession) {
                    currentSession?.let { sharedViewModel.setCurrentUserId(it.uid) }
                }

                NavHost(
                    navController = personalNavController,
                    startDestination = "personal_dashboard"
                ) {
                    composable("personal_dashboard") {
                        PersonalRoute(
                            onOpenAssistant = onOpenAssistant,
                            onAddExpense = { personalNavController.navigate("add_transaction/${TransactionType.EXPENSE.toRouteValue()}") },
                            onAddIncome = { personalNavController.navigate("add_transaction/${TransactionType.INCOME.toRouteValue()}") },
                            onOpenHistory = { personalNavController.navigate("history") },
                            onOpenCategoryManagement = { personalNavController.navigate("management") },
                            viewModel = sharedViewModel
                        )
                    }
                    
                    composable("add_transaction/{type}") { subBackStackEntry ->
                        val transactionType = transactionTypeFromRoute(
                            subBackStackEntry.arguments?.getString("type")
                        ) ?: TransactionType.EXPENSE
                        AddTransactionScreen(
                            onBack = { personalNavController.navigateUp() },
                            initialType = transactionType,
                            availableCategoriesByType = categoriesByType, // ✅ Pass REAL categories
                            currentUserId = currentSession?.uid ?: "",
                            onSave = { sharedViewModel.addTransaction(it) }
                        )
                    }
                    
                    composable("history") {
                        HistoryRoute(
                            onBack = { personalNavController.navigateUp() },
                            onTransactionClick = { transactionId ->
                                personalNavController.navigate("transaction_detail/$transactionId")
                            },
                            viewModel = sharedViewModel
                        )
                    }
                    
                    composable("transaction_detail/{transactionId}") { subBackStackEntry ->
                        val transactionId = subBackStackEntry.arguments?.getString("transactionId") ?: ""
                        val allTransactions by sharedViewModel.transactions.collectAsState()

                        val transaction = allTransactions.firstOrNull { it.id == transactionId }

                        TransactionDetailScreen(
                            transactionId = transactionId,
                            transaction = transaction,
                            onBack = { personalNavController.navigateUp() }
                        )
                    }
                    
                    composable("management") {
                        CategoryManagementRoute(
                            onBack = { personalNavController.navigateUp() },
                            viewModel = sharedViewModel
                        )
                    }
                }
            }

            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    uiState = profileUiState,
                    onEditProfile = { mainNavController.navigate(AppRoute.EditProfile.route) },
                    onLogout = onLogout,
                    onOpenNotifications = onOpenNotifications
                )
            }

            composable(AppRoute.EditProfile.route) {
                EditProfileScreen(
                    uiState = editProfileUiState,
                    onDisplayNameChange = profileViewModel::onDisplayNameChange,
                    onUsernameChange = profileViewModel::onUsernameChange,
                    onBioChange = profileViewModel::onBioChange,
                    onSave = profileViewModel::saveProfile,
                    onBack = { mainNavController.navigateUp() }
                )
            }
        }
    }
}
