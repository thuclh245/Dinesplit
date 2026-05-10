package com.example.dinesplit.presentation.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dinesplit.core.ui.AppButton
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold

@Composable
fun AssistantScreen() {
    AppScaffold(title = "Assistant") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "DineSplit Assistant", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Gợi ý nhanh các thao tác như tạo bữa ăn, nhắc nợ và tóm tắt chi tiêu trong cuộc ăn uống.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    Text(text = "Prompt gợi ý", style = MaterialTheme.typography.titleMedium)
                    AssistantPrompt(
                        icon = Icons.Filled.Restaurant,
                        title = "Tạo split mới",
                        subtitle = "Tạo một hóa đơn ăn tối cho 4 người"
                    )
                    AssistantPrompt(
                        icon = Icons.Filled.Bolt,
                        title = "Nhắc thanh toán",
                        subtitle = "Gửi lời nhắc cho người còn nợ"
                    )
                    AssistantPrompt(
                        icon = Icons.Filled.ChatBubbleOutline,
                        title = "Tóm tắt nhanh",
                        subtitle = "Xem ai đã trả, ai còn thiếu"
                    )
                }
            }

            AppCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bạn muốn bắt đầu với điều gì?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Hãy chọn một hành động nhanh để tiếp tục.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                    AppButton(
                        text = "Tạo bữa ăn mới",
                        onClick = { },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantPrompt(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.fillMaxWidth(0.82f), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}