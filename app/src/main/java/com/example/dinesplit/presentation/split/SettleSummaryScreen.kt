package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders

private enum class SettlementRelation {
    YouPay,
    YouReceive,
    GroupTransfer,
}

private data class SettlementDisplayItem(
    val fromName: String,
    val fromInitial: String,
    val toName: String,
    val toInitial: String,
    val amount: Double,
    val relation: SettlementRelation,
)

@Composable
fun SettleSummaryScreen(
    groupId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = remember(groupId) {
        GroupDetailViewModel(
            repository = AppContainer.splitRepository(context),
            profileRepository = AppContainer.profileRepository(context),
            groupId = groupId,
            currentUserId = FirebaseProviders.auth.currentUser?.uid
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = uiState.currentUserId
    val settlements =
        uiState.settlements.map { suggestion ->
            val fromMember = uiState.members.firstOrNull { it.id == suggestion.fromMemberId }
            val toMember = uiState.members.firstOrNull { it.id == suggestion.toMemberId }
            val relation =
                when {
                    suggestion.fromMemberId == currentUserId -> SettlementRelation.YouPay
                    suggestion.toMemberId == currentUserId -> SettlementRelation.YouReceive
                    else -> SettlementRelation.GroupTransfer
                }
            SettlementDisplayItem(
                fromName = if (suggestion.fromMemberId == currentUserId) "Bạn" else suggestion.fromName,
                fromInitial =
                    if (suggestion.fromMemberId == currentUserId) {
                        "B"
                    } else {
                        fromMember?.initial ?: suggestion.fromName.firstOrNull()?.uppercase().orEmpty()
                    },
                toName = if (suggestion.toMemberId == currentUserId) "Bạn" else suggestion.toName,
                toInitial =
                    if (suggestion.toMemberId == currentUserId) {
                        "B"
                    } else {
                        toMember?.initial ?: suggestion.toName.firstOrNull()?.uppercase().orEmpty()
                    },
                amount = suggestion.amount,
                relation = relation,
            )
        }
    val totalYouPay =
        settlements
            .filter { it.relation == SettlementRelation.YouPay }
            .sumOf { it.amount }
    val totalYouReceive =
        settlements
            .filter { it.relation == SettlementRelation.YouReceive }
            .sumOf { it.amount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettleTopBar(
                groupName = uiState.group?.name,
                onBack = onBack,
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                SettleMessage(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 24.dp),
                    title = "Không thể tải số dư",
                    message = uiState.error.orEmpty(),
                )
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        SettleHeroSection(settlementCount = settlements.size)
                    }
                    item { Spacer(modifier = Modifier.height(28.dp)) }
                    item {
                        SettleDebtList(debts = settlements)
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                    item {
                        SettleStatsCard(
                            totalYouPay = totalYouPay,
                            totalYouReceive = totalYouReceive,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                    item {
                        Text(
                            text = "Số dư được tính từ các hóa đơn chưa được đánh dấu thanh toán.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettleTopBar(
    groupName: String?,
    onBack: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainerLowest)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colorScheme.primary,
            )
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Chốt sổ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )
            if (!groupName.isNullOrBlank()) {
                Text(
                    text = groupName,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun SettleHeroSection(settlementCount: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val title = if (settlementCount == 0) "Đã cân bằng!" else "Các khoản cần đối soát"
    val subtitle =
        if (settlementCount == 0) {
            "Hiện tại nhóm không còn khoản nợ chưa thanh toán."
        } else {
            "Còn $settlementCount giao dịch để cân bằng số dư của nhóm."
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettleDebtList(debts: List<SettlementDisplayItem>) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CHI TIẾT ĐỐI SOÁT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        )

        if (debts.isEmpty()) {
            SettleMessage(
                modifier = Modifier.fillMaxWidth(),
                title = "Không có khoản cần thanh toán",
                message = "Mọi khoản chia tiền trong nhóm đã được cân bằng.",
            )
            return
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column {
                debts.forEachIndexed { index, debt ->
                    SettlementRow(debt = debt)
                    if (index < debts.lastIndex) {
                        HorizontalDivider(color = colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementRow(debt: SettlementDisplayItem) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor =
        when (debt.relation) {
            SettlementRelation.YouPay -> colorScheme.primary
            SettlementRelation.YouReceive -> colorScheme.secondary
            SettlementRelation.GroupTransfer -> colorScheme.onSurfaceVariant
        }
    val relationLabel =
        when (debt.relation) {
            SettlementRelation.YouPay -> "Bạn cần trả"
            SettlementRelation.YouReceive -> "Bạn sẽ nhận"
            SettlementRelation.GroupTransfer -> "Đối soát trong nhóm"
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettlementMember(
                modifier = Modifier.weight(1f),
                name = debt.fromName,
                initial = debt.fromInitial,
                isCurrentUser = debt.relation == SettlementRelation.YouPay,
            )
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${formatSettleAmount(debt.amount)} đ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                )
                HorizontalDivider(
                    modifier = Modifier.width(54.dp),
                    color = colorScheme.surfaceContainerHigh,
                    thickness = 2.dp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier =
                        Modifier
                            .offset(y = (-10).dp)
                            .size(18.dp)
                            .background(colorScheme.surfaceContainerLowest),
                )
            }
            SettlementMember(
                modifier = Modifier.weight(1f),
                name = debt.toName,
                initial = debt.toInitial,
                isCurrentUser = debt.relation == SettlementRelation.YouReceive,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = accentColor.copy(alpha = 0.10f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = relationLabel,
                modifier = Modifier.padding(vertical = 9.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

@Composable
private fun SettlementMember(
    modifier: Modifier,
    name: String,
    initial: String,
    isCurrentUser: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentUser) colorScheme.primary else colorScheme.outlineVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = colorScheme.surfaceContainerLowest,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettleStatsCard(
    totalYouPay: Double,
    totalYouReceive: Double,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettleStat(
            modifier = Modifier.weight(1f),
            title = "TỔNG BẠN TRẢ",
            amount = totalYouPay,
            containerColor = colorScheme.errorContainer.copy(alpha = 0.35f),
            contentColor = colorScheme.primary,
        )
        SettleStat(
            modifier = Modifier.weight(1f),
            title = "TỔNG NHẬN VỀ",
            amount = totalYouReceive,
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.secondary,
        )
    }
}

@Composable
private fun SettleStat(
    modifier: Modifier,
    title: String,
    amount: Double,
    containerColor: Color,
    contentColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatSettleAmount(amount)} đ",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SettleMessage(
    modifier: Modifier,
    title: String,
    message: String,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun formatSettleAmount(amount: Double): String {
    return "%,.0f".format(amount).replace(",", ".")
}
