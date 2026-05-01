package com.example.dinesplit.presentation.split

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Đã thêm AllGroups vào danh sách quản lý Route
sealed class SplitRoutes(val route: String) {
    object GroupList : SplitRoutes("group_list")
    object CreateGroup : SplitRoutes("create_group")
    object GroupDetail : SplitRoutes("group_detail")
    object CreateBill : SplitRoutes("create_bill")
    object BillDetail : SplitRoutes("bill_detail")
    object SettleSummary : SplitRoutes("settle_summary")
    object AllGroups : SplitRoutes("all_groups") // <-- Thêm mới ở đây
}

@Composable
fun SplitScreen() {
    val navController = rememberNavController()

    // Điểm bắt đầu của tab Split sẽ là màn hình Group List
    NavHost(navController = navController, startDestination = SplitRoutes.GroupList.route) {

        composable(SplitRoutes.GroupList.route) {
            GroupListScreen(
                onNavigateToGroupDetail = { navController.navigate(SplitRoutes.GroupDetail.route) },
                onNavigateToCreateGroup = { navController.navigate(SplitRoutes.CreateGroup.route) },
                // Dùng biến từ SplitRoutes thay vì gõ cứng string "all_groups"
                onNavigateToAllGroups = { navController.navigate(SplitRoutes.AllGroups.route) }
            )
        }

        // --- KHAI BÁO MÀN HÌNH ALL GROUPS VÀO HỆ THỐNG ---
        composable(SplitRoutes.AllGroups.route) {
            AllGroupsScreen(
                onBack = { navController.popBackStack() } // Bấm mũi tên lùi lại
            )
        }

        composable(SplitRoutes.CreateGroup.route) {
            CreateGroupScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(SplitRoutes.GroupDetail.route) {
            GroupDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreateBill = { navController.navigate(SplitRoutes.CreateBill.route) },
                onNavigateToBillDetail = { navController.navigate(SplitRoutes.BillDetail.route) },
                onNavigateToSettleSummary = { navController.navigate(SplitRoutes.SettleSummary.route) }
            )
        }

        composable(SplitRoutes.CreateBill.route) {
            CreateBillScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(SplitRoutes.BillDetail.route) {
            BillDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(SplitRoutes.SettleSummary.route) {
            SettleSummaryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
