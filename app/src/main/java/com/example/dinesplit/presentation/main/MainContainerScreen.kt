package com.example.dinesplit.presentation.main

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dinesplit.core.navigation.AppRoute
import com.example.dinesplit.core.navigation.BottomTab
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.presentation.feed.CreatePostScreen
import com.example.dinesplit.presentation.feed.CreatePostViewModel
import com.example.dinesplit.presentation.feed.FeedRoute
import com.example.dinesplit.presentation.feed.PostDetailScreen
import com.example.dinesplit.presentation.feed.SearchScreen
import com.example.dinesplit.presentation.personal.AddEditTransactionScreen
import com.example.dinesplit.presentation.personal.CategoryManagementScreen
import com.example.dinesplit.presentation.personal.HistoryScreen
import com.example.dinesplit.presentation.personal.MonthlySummaryScreen
import com.example.dinesplit.presentation.personal.PersonalIntelligenceScreen
import com.example.dinesplit.presentation.personal.PersonalPlanFocus
import com.example.dinesplit.presentation.personal.PersonalPlansScreen
import com.example.dinesplit.presentation.personal.PersonalScreen
import com.example.dinesplit.presentation.personal.PersonalViewModel
import com.example.dinesplit.presentation.personal.SpendingReminderScreen
import com.example.dinesplit.presentation.personal.TransactionDetailScreen
import com.example.dinesplit.presentation.personal.toHistoryItems
import com.example.dinesplit.presentation.personal.toManagedCategories
import com.example.dinesplit.presentation.personal.toTransactionType
import com.example.dinesplit.presentation.profile.EditProfileScreen
import com.example.dinesplit.presentation.profile.FollowListScreen
import com.example.dinesplit.presentation.profile.OtherUserProfileScreen
import com.example.dinesplit.presentation.profile.ProfileScreen
import com.example.dinesplit.presentation.profile.ProfileUiEffect
import com.example.dinesplit.presentation.profile.ProfileViewModel
import com.example.dinesplit.presentation.split.BillDetailScreen
import com.example.dinesplit.presentation.split.CreateBillScreen
import com.example.dinesplit.presentation.split.CreateGroupScreen
import com.example.dinesplit.presentation.split.GroupDetailScreen
import com.example.dinesplit.presentation.split.GroupListScreen
import com.example.dinesplit.presentation.split.SplitScreen
import com.example.dinesplit.ui.theme.DineSplitTheme
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Composable
fun MainContainerScreen(
    initialTabRoute: String = AppRoute.Feed.route,
    pendingRoute: String? = null,
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit,
    onLogout: () -> Unit,
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
    
    val currentRoute = currentDestination?.route
    val selectedBottomTab = currentRoute?.let(::bottomTabForRoute)
    val showBottomBar = selectedBottomTab != null

    val context = androidx.compose.ui.platform.LocalContext.current
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

    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density)

    val topBarHeightPx = remember(statusBarHeightPx) { with(density) { 64.dp.toPx() } + statusBarHeightPx }
    val bottomBarHeightPx = remember(navigationBarHeightPx) { with(density) { 72.dp.toPx() } + navigationBarHeightPx }

    var topBarOffsetHeightPx by remember { mutableStateOf(0f) }
    var bottomBarOffsetHeightPx by remember { mutableStateOf(0f) }

    val isFeedScreen = currentRoute == AppRoute.Feed.route

    val nestedScrollConnection = remember(currentRoute, topBarHeightPx, bottomBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!showBottomBar || !isFeedScreen) {
                    topBarOffsetHeightPx = 0f
                    bottomBarOffsetHeightPx = 0f
                    return Offset.Zero
                }
                val delta = available.y
                val newTopOffset = topBarOffsetHeightPx + delta
                topBarOffsetHeightPx = newTopOffset.coerceIn(-topBarHeightPx, 0f)

                val newBottomOffset = bottomBarOffsetHeightPx - delta
                bottomBarOffsetHeightPx = newBottomOffset.coerceIn(0f, bottomBarHeightPx)

                return Offset.Zero
            }
        }
    }

    LaunchedEffect(currentRoute) {
        topBarOffsetHeightPx = 0f
        bottomBarOffsetHeightPx = 0f
    }

    val bottomBarOffsetHeightDp = with(density) { bottomBarOffsetHeightPx.toDp() }
    val dynamicBottomPadding = remember(bottomBarOffsetHeightDp) { maxOf(0.dp, 80.dp - bottomBarOffsetHeightDp) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Không áp dụng innerPadding vào NavHost toàn cục để thanh công cụ trượt đè Glassmorphic mượt mà
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = mainNavController,
                    startDestination = AppRoute.Feed.route,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // TÍCH HỢP LUỒNG KHÉP KÍN TRANG CHỦ TUẦN 4 & 5 CHUẨN MVVM ROUTING
                    composable(AppRoute.Feed.route) {
                        FeedRoute(
                            userAvatarUrl = profileUiState.profile?.avatarUrl,
                            onOpenNotifications = onOpenNotifications,
                            onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                            onNavigateToCreatePost = { mainNavController.navigate(AppRoute.CreatePost.route) },
                            onNavigateToPostDetail = { postId ->
                                mainNavController.navigate(AppRoute.PostDetail.createRoute(postId))
                            },
                            onNavigateToUserProfile = { userId ->
                                mainNavController.navigate(AppRoute.OtherUserProfile.createRoute(userId))
                            },
                            onNavigateToEditPost = { postId ->
                                mainNavController.navigate(AppRoute.EditPost.createRoute(postId))
                            },
                            onSettleUp = { groupId, billId ->
                                mainNavController.navigate(AppRoute.BillDetail.createRoute(groupId, billId))
                            }
                        )
                    }
                    
                    composable(AppRoute.Split.route) {
                        SplitScreen(
                            userAvatarUrl = profileUiState.profile?.avatarUrl,
                            bottomPadding = dynamicBottomPadding,
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
                            },
                        )
                    }

                    composable(AppRoute.GroupList.route) {
                        GroupListScreen(
                            onNavigateToGroupDetail = { groupId ->
                                mainNavController.navigate(AppRoute.GroupDetail.createRoute(groupId))
                            },
                            onNavigateToCreateGroup = { mainNavController.navigate(AppRoute.CreateGroup.route) },
                            onNavigateToAllGroups = { /* already here */ },
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
                        )
                    }
                    
                    composable(AppRoute.Personal.route) {
                        PersonalScreen(
                            userAvatarUrl = profileUiState.profile?.avatarUrl,
                            uiState = personalUiState,
                            chartState = personalChartState,
                            reminderCount = personalReminders.size,
                            bottomPadding = dynamicBottomPadding,
                            onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                            onAddTransaction = { mainNavController.navigate(AppRoute.AddTransaction.route) },
                            onOpenHistory = { mainNavController.navigate(AppRoute.TransactionHistory.route) },
                            onOpenMonthlySummary = { mainNavController.navigate(AppRoute.MonthlySummary.route) },
                            onOpenCategories = { mainNavController.navigate(AppRoute.CategoryManagement.route) },
                            onOpenReminders = { mainNavController.navigate(AppRoute.SpendingReminders.route) },
                            onOpenInsights = { mainNavController.navigate(AppRoute.PersonalInsights.route) },
                            onOpenPlans = { mainNavController.navigate(AppRoute.PersonalPlans.route) },
                            onOpenRecurringPlans = {
                                mainNavController.navigate(AppRoute.PersonalPlans.createRoute(AppRoute.PersonalPlans.FOCUS_RECURRING))
                            },
                            onOpenGoalPlans = {
                                mainNavController.navigate(AppRoute.PersonalPlans.createRoute(AppRoute.PersonalPlans.FOCUS_GOALS))
                            },
                            onOpenWalletPlans = {
                                mainNavController.navigate(AppRoute.PersonalPlans.createRoute(AppRoute.PersonalPlans.FOCUS_WALLETS))
                            },
                            onRefresh = personalViewModel::refreshState,
                        )
                    }
                    
                    composable(AppRoute.AddTransaction.route) {
                        AddEditTransactionScreen(
                            onBack = { mainNavController.navigateUp() },
                            transactionId = null,
                            initialTransaction = null,
                            availableCategories = personalUiState.categories,
                            availableWallets = personalUiState.wallets,
                            onSave = { transaction ->
                                personalViewModel.addTransaction(transaction)
                                mainNavController.navigateUp()
                            },
                        )
                    }
                    
                    composable(AppRoute.TransactionHistory.route) {
                        HistoryScreen(
                            onBack = { mainNavController.navigateUp() },
                            transactions = personalUiState.transactions.toHistoryItems(personalUiState.categories),
                            onTransactionClick = { item ->
                                mainNavController.navigate(AppRoute.TransactionDetail.createRoute(item.id))
                            },
                        )
                    }
                    
                    composable(AppRoute.MonthlySummary.route) {
                        MonthlySummaryScreen(
                            onBack = { mainNavController.navigateUp() },
                            summary = personalChartState.monthlySummary,
                            categorySpending = personalChartState.pieSlices,
                        )
                    }
                    
                    composable(AppRoute.TransactionDetail.routeWithArg) { backStackEntry ->
                        val transactionId = backStackEntry.arguments?.getString(AppRoute.TransactionDetail.ARG_ID).orEmpty()
                        TransactionDetailScreen(
                            transactionId = transactionId,
                            transaction = personalUiState.transactions.firstOrNull { it.id == transactionId },
                            onBack = { mainNavController.navigateUp() },
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
                                    isCustom = input.isCustom,
                                )
                            },
                            onUpdateCategory = { category, input ->
                                personalViewModel.updateCategory(
                                    categoryId = category.id,
                                    name = input.name,
                                    description = input.description,
                                    type = input.type.toTransactionType(),
                                    isCustom = input.isCustom,
                                    isActive = category.isActive,
                                )
                            },
                            onDeleteCategory = { category ->
                                personalViewModel.deleteCategory(category.id)
                            },
                            onBack = { mainNavController.navigateUp() },
                        )
                    }
                    
                    composable(AppRoute.SpendingReminders.route) {
                        SpendingReminderScreen(
                            reminders = personalReminders,
                            categories = personalUiState.categories,
                            errorMessage = personalUiState.errorMessage,
                            onCreateReminder = { categoryId, categoryName, budgetAmount, threshold, type ->
                                personalViewModel.addSpendingReminder(categoryId, categoryName, budgetAmount, threshold, type)
                            },
                            onDeleteReminder = { reminderId ->
                                personalViewModel.deleteSpendingReminder(reminderId)
                            },
                            onBack = { mainNavController.navigateUp() },
                        )
                    }
                    
                    composable(AppRoute.PersonalInsights.route) {
                        PersonalIntelligenceScreen(
                            onBack = { mainNavController.navigateUp() },
                            uiState = personalUiState,
                            chartState = personalChartState,
                            reminderCount = personalReminders.size,
                            onOpenHistory = { mainNavController.navigate(AppRoute.TransactionHistory.route) },
                            onOpenReminders = { mainNavController.navigate(AppRoute.SpendingReminders.route) },
                            onOpenPlans = { mainNavController.navigate(AppRoute.PersonalPlans.route) },
                        )
                    }
                    
                    composable(
                        route = AppRoute.PersonalPlans.routeWithFocus,
                        arguments = listOf(
                            navArgument(AppRoute.PersonalPlans.ARG_FOCUS) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { backStackEntry ->
                        PersonalPlansScreen(
                            onBack = { mainNavController.navigateUp() },
                            initialFocus = PersonalPlanFocus.fromRouteValue(
                                backStackEntry.arguments?.getString(AppRoute.PersonalPlans.ARG_FOCUS),
                            ),
                            categories = personalUiState.categories,
                            recurringRules = personalUiState.recurringRules,
                            goals = personalUiState.goals,
                            wallets = personalUiState.wallets,
                            onAddRecurring = personalViewModel::addRecurringRule,
                            onDeleteRecurring = personalViewModel::deleteRecurringRule,
                            onAddGoal = personalViewModel::addGoal,
                            onDeleteGoal = personalViewModel::deleteGoal,
                            onAddWallet = personalViewModel::addWallet,
                            onDeleteWallet = personalViewModel::deleteWallet,
                        )
                    }
                    
                    composable(AppRoute.Profile.route) {
                        when {
                            profileUiState.isLoading -> {
                                Box(modifier = Modifier.fillMaxSize().padding(AppDimens.spaceLg), contentAlignment = Alignment.Center) {
                                    LoadingBlock(message = "Loading profile...")
                                }
                            }
                            profileUiState.errorMessage != null -> {
                                Box(modifier = Modifier.fillMaxSize().padding(AppDimens.spaceLg), contentAlignment = Alignment.Center) {
                                    ErrorStateBlock(
                                        title = "Không thể tải hồ sơ",
                                        subtitle = profileUiState.errorMessage ?: "Vui lòng kiểm tra mạng và thử lại.",
                                        retryText = "Thử lại",
                                        onRetryClick = { profileViewModel.loadProfile() },
                                    )
                                }
                            }
                            else -> {
                                ProfileScreen(
                                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                                    userName = profileUiState.profile?.displayName ?: "User",
                                    userHandle = profileUiState.profile?.username?.let { "@$it" }.orEmpty(),
                                    userBio = profileUiState.profile?.bio.orEmpty(),
                                    posts = profileUiState.posts,
                                    followersCount = profileUiState.profile?.followersCount ?: 0,
                                    followingCount = profileUiState.profile?.followingCount ?: 0,
                                    savedPosts = profileUiState.savedPosts,
                                    taggedBills = profileUiState.taggedBills,
                                    isLoggingOut = profileUiState.isLoggingOut,
                                    isPublic = profileUiState.profile?.isPublic ?: true,
                                    isSettingsDialogOpen = profileUiState.isSettingsDialogOpen,
                                    onCloseSettings = { profileViewModel.setSettingsDialogOpen(false) },
                                    bottomPadding = dynamicBottomPadding,
                                    onEditProfile = { mainNavController.navigate(AppRoute.EditProfile.route) },
                                    onOpenSettings = { profileViewModel.setSettingsDialogOpen(true) },
                                    onPrivacyChange = profileViewModel::setAccountPrivacy,
                                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                                    onLogout = profileViewModel::logout,
                                    onOpenPostDetail = { postId ->
                                        mainNavController.navigate(AppRoute.PostDetail.createRoute(postId))
                                    },
                                    onBillClick = { groupId, billId ->
                                        mainNavController.navigate(AppRoute.BillDetail.createRoute(groupId, billId))
                                    },
                                    onNavigateToFollowList = { tabIndex ->
                                        val myUid = profileUiState.profile?.uid.orEmpty()
                                        if (myUid.isNotEmpty()) {
                                            mainNavController.navigate(AppRoute.FollowList.createRoute(myUid, tabIndex))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    composable(AppRoute.EditProfile.route) {
                        EditProfileScreen(
                            uiState = editProfileUiState,
                            onDisplayNameChange = profileViewModel::onDisplayNameChange,
                            onUsernameChange = profileViewModel::onUsernameChange,
                            onBioChange = profileViewModel::onBioChange,
                            onAvatarChange = profileViewModel::onAvatarSelected,
                            onAvatarClear = profileViewModel::onAvatarCleared,
                            onToggleDiningStyle = profileViewModel::toggleDiningStyle,
                            onSave = profileViewModel::saveProfile,
                            onBack = { mainNavController.navigateUp() },
                        )
                    }
                    
                    composable(AppRoute.Search.route) {
                        SearchScreen(
                            onBack = { mainNavController.navigateUp() },
                            onOpenPostDetail = { postId ->
                                mainNavController.navigate(AppRoute.PostDetail.createRoute(postId))
                            },
                            onOpenUserProfile = { userId ->
                                mainNavController.navigate(AppRoute.OtherUserProfile.createRoute(userId))
                            }
                        )
                    }
                    
                    composable(AppRoute.CreatePost.route) {
                        val createPostViewModel: CreatePostViewModel = viewModel()
                        CreatePostScreen(
                            viewModel = createPostViewModel,
                            onBack = { mainNavController.navigateUp() }
                        )
                    }
                    
                    composable(AppRoute.EditPost.routeWithArg) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString(AppRoute.EditPost.ARG_ID) ?: ""
                        val createPostViewModel: CreatePostViewModel = viewModel()
                        CreatePostScreen(
                            viewModel = createPostViewModel,
                            postId = postId, 
                            onBack = { mainNavController.navigateUp() }
                        )
                    }
                    
                    composable(AppRoute.CreateBill.routeWithArg) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString(AppRoute.CreateBill.ARG_GROUP_ID).orEmpty()
                        CreateBillScreen(
                            groupId = groupId,
                            onBack = { mainNavController.navigateUp() },
                            onBillSavedForPersonal = personalViewModel::addSplitBillTransaction,
                        )
                    }
                    
                    composable(AppRoute.BillDetail.routeWithArg) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString(AppRoute.BillDetail.ARG_GROUP_ID).orEmpty()
                        val billId = backStackEntry.arguments?.getString(AppRoute.BillDetail.ARG_BILL_ID).orEmpty()
                        BillDetailScreen(
                            groupId = groupId,
                            billId = billId,
                            onBack = { mainNavController.navigateUp() },
                        )
                    }
                    
                    composable(AppRoute.PostDetail.routeWithArg) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString(AppRoute.PostDetail.ARG_ID) ?: ""
                        PostDetailScreen(postId = postId, onBack = { mainNavController.navigateUp() })
                    }
                    
                    composable(AppRoute.OtherUserProfile.routeWithArg) { backStackEntry ->
                        val userName = backStackEntry.arguments?.getString(AppRoute.OtherUserProfile.ARG_USER) ?: ""
                        OtherUserProfileScreen(
                            userName = userName,
                            onBack = { mainNavController.navigateUp() },
                            onNavigateToFollowList = { uid, tabIndex ->
                                mainNavController.navigate(AppRoute.FollowList.createRoute(uid, tabIndex))
                            }
                        )
                    }

                    composable(
                        route = AppRoute.FollowList.routeWithArg,
                        arguments = listOf(
                            navArgument(AppRoute.FollowList.ARG_INITIAL_TAB) {
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString(AppRoute.FollowList.ARG_USER_ID).orEmpty()
                        val initialTab = backStackEntry.arguments?.getInt(AppRoute.FollowList.ARG_INITIAL_TAB) ?: 0
                        FollowListScreen(
                            userId = userId,
                            initialTab = initialTab,
                            onBack = { mainNavController.navigateUp() },
                            onUserClick = { clickedUserId ->
                                if (clickedUserId == profileUiState.profile?.uid) {
                                    mainNavController.navigate(AppRoute.Profile.route) {
                                        popUpTo(AppRoute.Feed.route)
                                    }
                                } else {
                                    mainNavController.navigate(AppRoute.OtherUserProfile.createRoute(clickedUserId))
                                }
                            }
                        )
                    }
                }
            }

            if (showBottomBar) {
                val title = when {
                    currentRoute?.contains(AppRoute.Feed.route) == true -> "DineSplit"
                    currentRoute?.contains(AppRoute.Split.route) == true -> "Split Bill"
                    currentRoute?.contains(AppRoute.Personal.route) == true -> "Ví cá nhân"
                    currentRoute?.contains(AppRoute.Profile.route) == true -> profileUiState.profile?.displayName ?: "Profile"
                    else -> "DineSplit"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .offset { IntOffset(0, topBarOffsetHeightPx.roundToInt()) },
                ) {
                    HomeTopBar(
                        userAvatarUrl = profileUiState.profile?.avatarUrl,
                        title = title,
                        onAvatarClick = {
                            if (currentRoute?.contains(AppRoute.Feed.route) == true) {
                                mainNavController.navigate(AppRoute.CreatePost.route)
                            } else {
                                val isAlreadyOnProfile = currentRoute?.contains(AppRoute.Profile.route) == true
                                if (!isAlreadyOnProfile) {
                                    mainNavController.navigate(AppRoute.Profile.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                        onOpenSearch = if (currentRoute?.contains(AppRoute.Profile.route) == true) null else { { mainNavController.navigate(AppRoute.Search.route) } },
                        onOpenNotifications = if (currentRoute?.contains(AppRoute.Profile.route) == true) null else onOpenNotifications,
                        onOpenSettings = if (currentRoute?.contains(AppRoute.Profile.route) == true) { { profileViewModel.setSettingsDialogOpen(true) } } else null,
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .offset { IntOffset(0, bottomBarOffsetHeightPx.roundToInt()) },
                ) {
                    MainBottomBar(
                        selectedTab = selectedBottomTab,
                        onTabSelected = { tab ->
                            mainNavController.navigate(tab.route) {
                                popUpTo(mainNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun bottomTabForRoute(route: String): BottomTab? {
    return when (route) {
        AppRoute.Feed.route -> BottomTab.Feed
        AppRoute.Split.route,
        AppRoute.GroupList.route -> BottomTab.Split
        AppRoute.Personal.route -> BottomTab.Personal
        AppRoute.Profile.route -> BottomTab.Profile
        else -> null
    }
}

@Composable
private fun MainBottomBar(
    selectedTab: BottomTab?,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val tabs = remember {
        listOf(
            BottomTab.Feed,
            BottomTab.Split,
            BottomTab.Personal,
            BottomTab.Profile,
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = colorScheme.surfaceContainerLowest,
        tonalElevation = 8.dp,
    ) {
        Column {
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.35f), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                tabs.forEach { tab ->
                    val selected = selectedTab == tab

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                            ),
                            color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
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
            selectedTab = BottomTab.Feed,
            onTabSelected = {},
        )
    }
}
