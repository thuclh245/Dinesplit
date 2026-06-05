package com.example.dinesplit.core.navigation

import android.net.Uri

sealed class AppRoute(val route: String) {
    data object Splash : AppRoute("splash")

    data object Login : AppRoute("login")

    data object Register : AppRoute("register")

    data object CompleteProfile : AppRoute("complete_profile") {
        const val ARG_DISPLAY_NAME = "displayName"
        val routeWithArg = "$route?$ARG_DISPLAY_NAME={$ARG_DISPLAY_NAME}"

        fun createRoute(displayName: String): String {
            return "$route?$ARG_DISPLAY_NAME=${Uri.encode(displayName)}"
        }
    }

    data object MainContainer : AppRoute("main") {
        const val ARG_TAB = "tab"
        const val ARG_TARGET = "target"
        const val ARG_RETURN_TO_NOTIFICATIONS = "returnToNotifications"
        val routeWithArgs =
            "$route?$ARG_TAB={$ARG_TAB}&$ARG_TARGET={$ARG_TARGET}&$ARG_RETURN_TO_NOTIFICATIONS={$ARG_RETURN_TO_NOTIFICATIONS}"

        fun createRoute(
            tab: String? = null,
            target: String? = null,
            returnToNotifications: Boolean = false,
        ): String {
            val args =
                buildList {
                    if (!tab.isNullOrBlank()) add("$ARG_TAB=${Uri.encode(tab)}")
                    if (!target.isNullOrBlank()) add("$ARG_TARGET=${Uri.encode(target)}")
                    if (returnToNotifications) add("$ARG_RETURN_TO_NOTIFICATIONS=true")
                }
            return if (args.isEmpty()) route else "$route?${args.joinToString("&")}"
        }
    }

    data object Feed : AppRoute("feed")

    data object CreatePost : AppRoute("create_post")

    data object EditPost : AppRoute("edit_post") {
        const val ARG_ID = "postId"
        val routeWithArg = "$route/{$ARG_ID}"

        fun createRoute(postId: String): String {
            return "$route/${Uri.encode(postId)}"
        }
    }

    data object CreateBill : AppRoute("create_bill") {
        const val ARG_GROUP_ID = "groupId"
        const val ARG_BILL_ID = "billId"
        val routeWithArg = "$route/{$ARG_GROUP_ID}?$ARG_BILL_ID={$ARG_BILL_ID}"

        fun createRoute(
            groupId: String,
            billId: String? = null,
        ): String {
            val baseRoute = "$route/${Uri.encode(groupId)}"
            return if (billId.isNullOrBlank()) {
                baseRoute
            } else {
                "$baseRoute?$ARG_BILL_ID=${Uri.encode(billId)}"
            }
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
            return "$route/${Uri.encode(userName)}"
        }
    }

    data object FollowList : AppRoute("follow_list") {
        const val ARG_USER_ID = "userId"
        const val ARG_INITIAL_TAB = "initialTab"
        val routeWithArg = "$route/{$ARG_USER_ID}?$ARG_INITIAL_TAB={$ARG_INITIAL_TAB}"

        fun createRoute(userId: String, initialTab: Int = 0): String {
            return "$route/$userId?$ARG_INITIAL_TAB=$initialTab"
        }
    }

    data object Split : AppRoute("split")

    data object GroupList : AppRoute("group_list")

    data object CreateGroup : AppRoute("create_group")

    data object GroupDetail : AppRoute("group_detail") {
        const val ARG_ID = "groupId"
        val routeWithArg = "$route/{$ARG_ID}"

        fun createRoute(groupId: String): String {
            return "$route/${Uri.encode(groupId)}"
        }
    }

    data object SettleSummary : AppRoute("settle_summary") {
        const val ARG_ID = "groupId"
        val routeWithArg = "$route/{$ARG_ID}"

        fun createRoute(groupId: String): String {
            return "$route/${Uri.encode(groupId)}"
        }
    }

    data object BillDetail : AppRoute("bill_detail") {
        const val ARG_GROUP_ID = "groupId"
        const val ARG_BILL_ID = "billId"
        val routeWithArg = "$route/{$ARG_GROUP_ID}/{$ARG_BILL_ID}"

        fun createRoute(
            groupId: String,
            billId: String,
        ): String {
            return "$route/${Uri.encode(groupId)}/${Uri.encode(billId)}"
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
            return "$route/${Uri.encode(transactionId)}"
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
