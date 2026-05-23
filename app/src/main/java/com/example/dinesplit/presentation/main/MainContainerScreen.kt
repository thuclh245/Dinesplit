package com.example.dinesplit.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dinesplit.core.navigation.AppRoute
import com.example.dinesplit.core.navigation.BottomTab
import com.example.dinesplit.presentation.feed.CreatePostScreen
import com.example.dinesplit.presentation.feed.FeedScreen
import com.example.dinesplit.presentation.feed.PostDetailScreen
import com.example.dinesplit.presentation.feed.SearchScreen
import com.example.dinesplit.presentation.personal.AddEditTransactionScreen
import com.example.dinesplit.presentation.personal.CategoryManagementScreen
import com.example.dinesplit.presentation.personal.HistoryScreen
import com.example.dinesplit.presentation.personal.PersonalScreen
import com.example.dinesplit.presentation.personal.PersonalPlansScreen
import com.example.dinesplit.presentation.personal.PersonalViewModel
import com.example.dinesplit.presentation.personal.SpendingReminderScreen
import com.example.dinesplit.presentation.personal.TransactionDetailScreen
import com.example.dinesplit.presentation.personal.toHistoryItems
import com.example.dinesplit.presentation.personal.toManagedCategories
import com.example.dinesplit.presentation.personal.toTransactionType
import com.example.dinesplit.presentation.split.BillDetailScreen
import com.example.dinesplit.presentation.profile.EditProfileScreen
import com.example.dinesplit.presentation.profile.OtherUserProfileScreen
import com.example.dinesplit.presentation.profile.ProfileScreen
import com.example.dinesplit.presentation.profile.ProfileUiEffect
import com.example.dinesplit.presentation.profile.ProfileViewModel
import com.example.dinesplit.presentation.split.CreateBillScreen
import com.example.dinesplit.presentation.split.CreateGroupScreen
import com.example.dinesplit.presentation.split.GroupDetailScreen
import com.example.dinesplit.presentation.split.GroupListScreen
import com.example.dinesplit.presentation.split.SplitScreen
import com.example.dinesplit.ui.theme.DineSplitTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainContainerScreen(
    initialTabRoute: String = AppRoute.Feed.route,
    pendingRoute: String? = null,
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit,
    onLogout: () -> Unit
) {
    val mainNavController = rememberNavController()
    val profileViewModel: ProfileViewModel = viewModel()
    val personalViewModel: PersonalViewModel = viewModel()
    val profileUiState by profileViewModel.profileUiState.collectAsState()
    val editProfileUiState by profileViewModel.editUiState.collectAsState()
    val personalUiState by personalViewModel.uiState.collectAsState()
    val personalChartState by personalViewModel.chartState.collectAsState()
    val personalReminders by personalViewModel.reminders.collectAsState()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = BottomTab.items.any { tab ->
        currentDestination
            ?.hierarchy
            ?.any { it.route == tab.route } == true
    }

    LaunchedEffect(profileViewModel) {
        profileViewModel.effect.collectLatest { effect ->
            when (effect) {
                ProfileUiEffect.LogoutSuccess -> onLogout()
                ProfileUiEffect.SaveSuccess -> mainNavController.navigateUp()
            }
        }
    }

    LaunchedEffect(initialTabRoute, pendingRoute) {
        if (initialTabRoute != AppRoute.Feed.route) {
            mainNavController.navigate(initialTabRoute) {
                popUpTo(AppRoute.Feed.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        if (!pendingRoute.isNullOrBlank()) {
            mainNavController.navigate(pendingRoute) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(
                    isTabSelected = { tab ->
                        currentDestination
                            ?.hierarchy
                            ?.any { it.route == tab.route } == true
                    },
                    onTabSelected = { tab ->
                        mainNavController.navigate(tab.route) {
                            popUpTo(mainNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = AppRoute.Feed.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Fix lỗi lint và đảm bảo đúng layout
        ) {
            composable(AppRoute.Feed.route) {
                FeedScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onSettleUp = { /* Handle settle up */ }
                )
            }
            composable(AppRoute.Split.route) {
                SplitScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onNewGroup = { mainNavController.navigate(AppRoute.GroupList.route) },
                    onNewExpense = { mainNavController.navigate(AppRoute.GroupList.route) },
                    onViewAllGroups = { mainNavController.navigate(AppRoute.GroupList.route) },
                    onGroupClick = { groupId ->
                        mainNavController.navigate(AppRoute.GroupDetail.createRoute(groupId))
                    },
                    onBillClick = { groupId, billId ->
                        mainNavController.navigate(AppRoute.BillDetail.createRoute(groupId, billId))
                    }
                )
            }

            composable(AppRoute.GroupList.route) {
                GroupListScreen(
                    onNavigateToGroupDetail = { groupId ->
                        mainNavController.navigate(AppRoute.GroupDetail.createRoute(groupId))
                    },
                    onNavigateToCreateGroup = { mainNavController.navigate(AppRoute.CreateGroup.route) },
                    onNavigateToAllGroups = { /* already here */ }
                )
            }

            composable(AppRoute.CreateGroup.route) {
                CreateGroupScreen(onBack = { mainNavController.navigateUp() })
            }

            composable(AppRoute.GroupDetail.routeWithArg) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString(AppRoute.GroupDetail.ARG_ID).orEmpty()
                GroupDetailScreen(
                    groupId = groupId,
                    onBack = { mainNavController.navigateUp() },
                    onNavigateToCreateBill = {
                        mainNavController.navigate(AppRoute.CreateBill.createRoute(groupId))
                    },
                    onNavigateToBillDetail = { billId ->
                        mainNavController.navigate(AppRoute.BillDetail.createRoute(groupId, billId))
                    },
                    onNavigateToSettleSummary = { /* TODO */ }
                )
            }
            composable(AppRoute.Personal.route) {
                PersonalScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    uiState = personalUiState,
                    chartState = personalChartState,
                    reminderCount = personalReminders.size,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onAddTransaction = { mainNavController.navigate(AppRoute.AddTransaction.route) },
                    onOpenHistory = { mainNavController.navigate(AppRoute.TransactionHistory.route) },
                    onOpenCategories = { mainNavController.navigate(AppRoute.CategoryManagement.route) },
                    onOpenReminders = { mainNavController.navigate(AppRoute.SpendingReminders.route) },
                    onOpenPlans = { mainNavController.navigate(AppRoute.PersonalPlans.route) },
                    onRefresh = personalViewModel::refreshState
                )
            }
            composable(AppRoute.AddTransaction.route) {
                AddEditTransactionScreen(
                    onBack = { mainNavController.navigateUp() },
                    transactionId = null,
                    initialTransaction = null,
                    availableCategories = personalUiState.categories,
                    onSave = { transaction ->
                        personalViewModel.addTransaction(transaction)
                        mainNavController.navigateUp()
                    }
                )
            }
            composable(AppRoute.TransactionHistory.route) {
                HistoryScreen(
                    onBack = { mainNavController.navigateUp() },
                    transactions = personalUiState.transactions.toHistoryItems(personalUiState.categories),
                    onTransactionClick = { item ->
                        mainNavController.navigate(AppRoute.TransactionDetail.createRoute(item.id))
                    }
                )
            }
            composable(AppRoute.TransactionDetail.routeWithArg) { backStackEntry ->
                val transactionId = backStackEntry.arguments
                    ?.getString(AppRoute.TransactionDetail.ARG_ID)
                    .orEmpty()
                TransactionDetailScreen(
                    transactionId = transactionId,
                    transaction = personalUiState.transactions.firstOrNull { it.id == transactionId },
                    onBack = { mainNavController.navigateUp() }
                )
            }
            composable(AppRoute.CategoryManagement.route) {
                CategoryManagementScreen(
                    categories = personalUiState.categories.toManagedCategories(personalUiState.transactions),
                    usedCategoryIds = personalUiState.transactions.map { it.categoryId }.toSet(),
                    onAddCategory = { input ->
                        personalViewModel.addCategory(
                            name = input.name,
                            description = input.description,
                            type = input.type.toTransactionType(),
                            isCustom = input.isCustom
                        )
                    },
                    onUpdateCategory = { category, input ->
                        personalViewModel.updateCategory(
                            categoryId = category.id,
                            name = input.name,
                            description = input.description,
                            type = input.type.toTransactionType(),
                            isCustom = input.isCustom,
                            isActive = category.isActive
                        )
                    },
                    onDeleteCategory = { category ->
                        personalViewModel.deleteCategory(category.id)
                    },
                    onBack = { mainNavController.navigateUp() }
                )
            }
            composable(AppRoute.SpendingReminders.route) {
                SpendingReminderScreen(
                    onBack = { mainNavController.navigateUp() },
                    reminders = personalReminders,
                    categories = personalUiState.categories,
                    errorMessage = personalUiState.errorMessage,
                    onCreateReminder = { categoryId, categoryName, budget, threshold, type ->
                        personalViewModel.addSpendingReminder(
                            categoryId = categoryId,
                            categoryName = categoryName,
                            budgetAmount = budget,
                            threshold = threshold,
                            reminderType = type
                        )
                    },
                    onDeleteReminder = personalViewModel::deleteSpendingReminder
                )
            }
            composable(AppRoute.PersonalPlans.route) {
                PersonalPlansScreen(
                    onBack = { mainNavController.navigateUp() },
                    categories = personalUiState.categories,
                    recurringRules = personalUiState.recurringRules,
                    goals = personalUiState.goals,
                    wallets = personalUiState.wallets,
                    onAddRecurring = personalViewModel::addRecurringRule,
                    onDeleteRecurring = personalViewModel::deleteRecurringRule,
                    onAddGoal = personalViewModel::addGoal,
                    onDeleteGoal = personalViewModel::deleteGoal,
                    onAddWallet = personalViewModel::addWallet,
                    onDeleteWallet = personalViewModel::deleteWallet
                )
            }
            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    userName = profileUiState.profile?.displayName ?: "User",
                    userHandle = profileUiState.profile?.username?.let { "@$it" }.orEmpty(),
                    userBio = profileUiState.profile?.bio.orEmpty(),
                    onEditProfile = { mainNavController.navigate(AppRoute.EditProfile.route) },
                    onOpenSettings = { /* TODO: Implement settings */ },
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) }
                )
            }
            composable(AppRoute.EditProfile.route) {
                EditProfileScreen(
                    uiState = editProfileUiState,
                    onDisplayNameChange = profileViewModel::onDisplayNameChange,
                    onUsernameChange = profileViewModel::onUsernameChange,
                    onBioChange = profileViewModel::onBioChange,
                    onAvatarChange = profileViewModel::onAvatarSelected,
                    onAvatarClear = profileViewModel::onAvatarCleared,
                    onSave = profileViewModel::saveProfile,
                    onBack = { mainNavController.navigateUp() }
                )
            }
            composable(AppRoute.Search.route) {
                SearchScreen(onBack = { mainNavController.navigateUp() })
            }
            composable(AppRoute.CreatePost.route) {
                CreatePostScreen(onBack = { mainNavController.navigateUp() })
            }
            composable(AppRoute.CreateBill.routeWithArg) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString(AppRoute.CreateBill.ARG_GROUP_ID).orEmpty()
                CreateBillScreen(
                    groupId = groupId,
                    onBack = { mainNavController.navigateUp() },
                    onBillSavedForPersonal = personalViewModel::addSplitBillTransaction
                )
            }
            composable(AppRoute.BillDetail.routeWithArg) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString(AppRoute.BillDetail.ARG_GROUP_ID).orEmpty()
                val billId = backStackEntry.arguments?.getString(AppRoute.BillDetail.ARG_BILL_ID).orEmpty()
                BillDetailScreen(
                    groupId = groupId,
                    billId = billId,
                    onBack = { mainNavController.navigateUp() }
                )
            }
            composable(AppRoute.PostDetail.routeWithArg) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString(AppRoute.PostDetail.ARG_ID) ?: ""
                PostDetailScreen(postId = postId, onBack = { mainNavController.navigateUp() })
            }
            composable(AppRoute.OtherUserProfile.routeWithArg) { backStackEntry ->
                val userName = backStackEntry.arguments?.getString(AppRoute.OtherUserProfile.ARG_USER) ?: ""
                OtherUserProfileScreen(userName = userName, onBack = { mainNavController.navigateUp() })
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    isTabSelected: (BottomTab) -> Boolean,
    onTabSelected: (BottomTab) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = colorScheme.surfaceContainerLowest,
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.35f), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomTab.items.forEach { tab ->
                    val selected = isTabSelected(tab)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (selected) colorScheme.primary else colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Main Bottom Bar")
@Composable
private fun MainBottomBarPreview() {
    DineSplitTheme {
        MainBottomBar(
            isTabSelected = { tab -> tab.route == AppRoute.Feed.route },
            onTabSelected = {}
        )
    }
}
