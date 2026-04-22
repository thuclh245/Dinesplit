package com.example.dinesplit.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dinesplit.core.navigation.AppRoute
import com.example.dinesplit.core.navigation.BottomTab
import com.example.dinesplit.data.repository.StoredCategory
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.presentation.feed.CreatePostScreen
import com.example.dinesplit.presentation.feed.FeedScreen
import com.example.dinesplit.presentation.feed.PostDetailScreen
import com.example.dinesplit.presentation.feed.SearchScreen
import com.example.dinesplit.presentation.personal.AddTransactionScreen
import com.example.dinesplit.presentation.personal.CategoryManagementScreen
import com.example.dinesplit.presentation.personal.CategoryTypeFilter
import com.example.dinesplit.presentation.personal.HistoryScreen
import com.example.dinesplit.presentation.personal.HistoryTransactionItem
import com.example.dinesplit.presentation.personal.ManagedCategory
import com.example.dinesplit.presentation.personal.PersonalRoute
import com.example.dinesplit.presentation.personal.PersonalViewModel
import com.example.dinesplit.presentation.personal.TransactionDetailScreen
import com.example.dinesplit.presentation.personal.toRouteValue
import com.example.dinesplit.presentation.personal.transactionTypeFromRoute
import com.example.dinesplit.presentation.profile.OtherUserProfileScreen
import com.example.dinesplit.presentation.profile.ProfileScreen
import com.example.dinesplit.presentation.split.SplitScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainContainerScreen(
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    val mainNavController = rememberNavController()
    val personalViewModel: PersonalViewModel = viewModel()
    val transactions by personalViewModel.transactions.collectAsState()
    val categories by personalViewModel.categories.collectAsState()
    val categoriesById = categories.associateBy { it.id }
    val categoriesByType = categories.groupBy { it.type }
    val amountByCategoryId = transactions
        .groupBy { it.categoryId }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
    val totalAmountByType = transactions
        .groupBy { transaction -> categoriesById[transaction.categoryId]?.type ?: transaction.type }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(tab.label)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    onCreatePost = {
                        mainNavController.navigate(AppRoute.CreatePost.route)
                    },
                    onOpenPostDetail = { postId ->
                        mainNavController.navigate(AppRoute.PostDetail.createRoute(postId))
                    },
                    onOpenSearch = {
                        mainNavController.navigate(AppRoute.Search.route)
                    },
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

            composable(
                route = AppRoute.PostDetail.routeWithArg,
                arguments = listOf(
                    navArgument(AppRoute.PostDetail.ARG_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                PostDetailScreen(
                    postId = backStackEntry.arguments?.getString(AppRoute.PostDetail.ARG_ID).orEmpty(),
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(
                route = AppRoute.OtherUserProfile.routeWithArg,
                arguments = listOf(
                    navArgument(AppRoute.OtherUserProfile.ARG_USER) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                OtherUserProfileScreen(
                    userName = backStackEntry.arguments?.getString(AppRoute.OtherUserProfile.ARG_USER).orEmpty(),
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(AppRoute.Split.route) {
                SplitScreen()
            }

            composable(AppRoute.Personal.route) {
                PersonalRoute(
                    onOpenAssistant = onOpenAssistant,
                    onAddExpense = {
                        mainNavController.navigate(
                            AppRoute.AddTransaction.createRoute(TransactionType.EXPENSE.toRouteValue())
                        )
                    },
                    onAddIncome = {
                        mainNavController.navigate(
                            AppRoute.AddTransaction.createRoute(TransactionType.INCOME.toRouteValue())
                        )
                    },
                    onOpenHistory = {
                        mainNavController.navigate(AppRoute.TransactionHistory.route)
                    },
                    onOpenCategoryManagement = {
                        mainNavController.navigate(AppRoute.CategoryManagement.route)
                    },
                    viewModel = personalViewModel
                )
            }

            composable(
                route = AppRoute.AddTransaction.routeWithArg,
                arguments = listOf(
                    navArgument(AppRoute.AddTransaction.ARG_TYPE) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initialType = transactionTypeFromRoute(
                    backStackEntry.arguments?.getString(AppRoute.AddTransaction.ARG_TYPE)
                )
                AddTransactionScreen(
                    onBack = { mainNavController.navigateUp() },
                    initialType = initialType,
                    availableCategoriesByType = categoriesByType,
                    onSave = { transaction ->
                        personalViewModel.addTransaction(transaction)
                    }
                )
            }

            composable(AppRoute.TransactionHistory.route) {
                HistoryScreen(
                    onBack = { mainNavController.navigateUp() },
                    transactions = transactions.map { it.toHistoryUi(categoriesById) },
                    onTransactionClick = { item ->
                        mainNavController.navigate(AppRoute.TransactionDetail.createRoute(item.id))
                    }
                )
            }

            composable(
                route = AppRoute.TransactionDetail.routeWithArg,
                arguments = listOf(
                    navArgument(AppRoute.TransactionDetail.ARG_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString(AppRoute.TransactionDetail.ARG_ID).orEmpty()
                TransactionDetailScreen(
                    transactionId = transactionId,
                    transaction = transactions.firstOrNull { it.id == transactionId }?.resolveCategory(categoriesById),
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(AppRoute.CategoryManagement.route) {
                CategoryManagementScreen(
                    categories = categories.map {
                        it.toManagedCategory(
                            amount = amountByCategoryId[it.id] ?: 0.0,
                            totalForType = totalAmountByType[it.type] ?: 0.0
                        )
                    },
                    usedCategoryIds = transactions.map { it.categoryId }.toSet(),
                    onAddCategory = { input ->
                        personalViewModel.addCategory(
                            name = input.name,
                            description = input.description,
                            type = input.type.toDomainType(),
                            isCustom = input.isCustom
                        )
                    },
                    onUpdateCategory = { category, input ->
                        personalViewModel.updateCategory(
                            categoryId = category.id,
                            name = input.name,
                            description = input.description,
                            type = input.type.toDomainType(),
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

            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    onOpenNotifications = onOpenNotifications
                )
            }
        }
    }
}

private fun Transaction.toHistoryUi(categoriesById: Map<String, StoredCategory>): HistoryTransactionItem {
    val resolvedCategory = categoriesById[categoryId]
    return HistoryTransactionItem(
        id = id,
        categoryIcon = resolvedCategory?.icon ?: category.take(2).uppercase(Locale.US),
        category = resolvedCategory?.name ?: category,
        amount = formatSignedAmount(type = type, amount = amount),
        date = displayDate(date),
        month = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(date)),
        type = type,
        note = note
    )
}

private fun Transaction.resolveCategory(categoriesById: Map<String, StoredCategory>): Transaction {
    val resolvedCategory = categoriesById[categoryId] ?: return this
    return copy(category = resolvedCategory.name)
}

private fun StoredCategory.toManagedCategory(
    amount: Double,
    totalForType: Double
): ManagedCategory {
    val safeTotal = totalForType.takeIf { it > 0.0 } ?: 1.0
    val progressRatio = (amount / safeTotal).coerceIn(0.0, 1.0)

    return ManagedCategory(
        id = id,
        name = name,
        icon = icon,
        type = if (type == TransactionType.INCOME) {
            CategoryTypeFilter.INCOME
        } else {
            CategoryTypeFilter.EXPENSE
        },
        isCustom = isCustom,
        description = description,
        amountLabel = amount.toCurrencyLabel(),
        progress = progressRatio.toFloat(),
        isActive = isActive
    )
}

private fun Double.toCurrencyLabel(): String {
    return "$${String.format(Locale.US, "%,.2f", this)}"
}

private fun CategoryTypeFilter.toDomainType(): TransactionType {
    return if (this == CategoryTypeFilter.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
}

private fun formatSignedAmount(type: TransactionType, amount: Double): String {
    val sign = if (type == TransactionType.INCOME) "+" else "-"
    return "$sign${String.format(Locale.US, "%.2f", amount)}"
}

private fun displayDate(epochMillis: Long): String {
    val day = SimpleDateFormat("dd", Locale.getDefault()).format(Date(epochMillis)).toIntOrNull()
    val today = Date()
    val current = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(epochMillis))
    val now = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today)
    val yesterdayMillis = today.time - 24L * 60L * 60L * 1000L
    val yesterday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(yesterdayMillis))

    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(epochMillis))
    return when (current) {
        now -> "Today, $time"
        yesterday -> "Yesterday, $time"
        else -> {
            val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
            if (day != null) "$dateLabel, $time" else dateLabel
        }
    }
}

