package com.example.dinesplit.core.navigation

import android.net.Uri

sealed class AppRoute(val route: String) {

    data object Splash : AppRoute("splash")
    data object Login : AppRoute("login")
    data object Register : AppRoute("register")
    data object CompleteProfile : AppRoute("complete_profile")

    data object MainContainer : AppRoute("main") {
        const val ARG_TAB = "tab"
        const val ARG_TARGET = "target"
        val routeWithArgs = "$route?$ARG_TAB={$ARG_TAB}&$ARG_TARGET={$ARG_TARGET}"

        fun createRoute(
            tab: String? = null,
            target: String? = null
        ): String {
            val args = buildList {
                if (!tab.isNullOrBlank()) add("$ARG_TAB=${Uri.encode(tab)}")
                if (!target.isNullOrBlank()) add("$ARG_TARGET=${Uri.encode(target)}")
            }
            return if (args.isEmpty()) route else "$route?${args.joinToString("&")}"
        }
    }

    data object Feed : AppRoute("feed")
    data object CreatePost : AppRoute("create_post")
    data object CreateBill : AppRoute("create_bill") {
        const val ARG_GROUP_ID = "groupId"
        val routeWithArg = "$route/{$ARG_GROUP_ID}"

        fun createRoute(groupId: String): String {
            return "$route/$groupId"
        }
    }
    data object Search : AppRoute("search")
    data object PostDetail : AppRoute("post_detail") {
        const val ARG_ID = "postId"
        val routeWithArg = "$route/{$ARG_ID}"

        fun createRoute(postId: String): String {
            return "$route/$postId"
        }
    }
    data object OtherUserProfile : AppRoute("other_user_profile") {
        const val ARG_USER = "userName"
        val routeWithArg = "$route/{$ARG_USER}"

        fun createRoute(userName: String): String {
            return "$route/$userName"
        }
    }
    data object Split : AppRoute("split")
    data object GroupList : AppRoute("group_list")
    data object CreateGroup : AppRoute("create_group")
    data object GroupDetail : AppRoute("group_detail") {
        const val ARG_ID = "groupId"
        val routeWithArg = "$route/{$ARG_ID}"

        fun createRoute(groupId: String): String {
            return "$route/$groupId"
        }
    }
    data object BillDetail : AppRoute("bill_detail") {
        const val ARG_GROUP_ID = "groupId"
        const val ARG_BILL_ID = "billId"
        val routeWithArg = "$route/{$ARG_GROUP_ID}/{$ARG_BILL_ID}"

        fun createRoute(groupId: String, billId: String): String {
            return "$route/$groupId/$billId"
        }
    }
    data object Personal : AppRoute("personal")
    data object AddTransaction : AppRoute("add_transaction") {
        const val ARG_TYPE = "type"
        val routeWithArg = "$route?$ARG_TYPE={$ARG_TYPE}"

        fun createRoute(type: String? = null): String {
            if (type.isNullOrBlank()) return route
            return "$route?$ARG_TYPE=$type"
        }
    }
    data object TransactionHistory : AppRoute("transaction_history")
    data object MonthlySummary : AppRoute("monthly_summary")
    data object TransactionDetail : AppRoute("transaction_detail") {
        const val ARG_ID = "transactionId"
        val routeWithArg = "$route/{$ARG_ID}"

        fun createRoute(transactionId: String): String {
            return "$route/$transactionId"
        }
    }
    data object CategoryManagement : AppRoute("category_management")
    data object SpendingReminders : AppRoute("spending_reminders")
    data object PersonalInsights : AppRoute("personal_insights")
    data object PersonalPlans : AppRoute("personal_plans") {
        const val ARG_FOCUS = "focus"
        const val FOCUS_RECURRING = "recurring"
        const val FOCUS_GOALS = "goals"
        const val FOCUS_WALLETS = "wallets"
        val routeWithFocus = "$route?$ARG_FOCUS={$ARG_FOCUS}"

        fun createRoute(focus: String? = null): String {
            if (focus.isNullOrBlank()) return route
            return "$route?$ARG_FOCUS=${Uri.encode(focus)}"
        }
    }
    data object Profile : AppRoute("profile")
    data object EditProfile : AppRoute("edit_profile")

    data object Notifications : AppRoute("notifications")
    data object Assistant : AppRoute("assistant")
}
