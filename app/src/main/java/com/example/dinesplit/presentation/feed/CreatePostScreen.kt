package com.example.dinesplit.presentation.feed

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.ui.AppButton
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.domain.model.Post
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@Composable
fun CreatePostScreen(
    onBack: () -> Unit = {}
) {
    var restaurantName by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Select a beautiful random placeholder food photo
    val mockImage = remember {
        listOf(
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1482049016688-2d3e1b311543?w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&auto=format&fit=crop"
        ).random()
    }

    AppScaffold(
        title = "Tạo Bài Viết",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Quay lại", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            AppCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = mockImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Text(
                        text = "Ảnh món ăn ngẫu nhiên đã chọn 📸",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            AppTextField(
                value = restaurantName,
                onValueChange = { restaurantName = it },
                label = "Tên quán ăn / Nhà hàng",
                placeholder = "Ví dụ: Phở Thìn Lò Đúc"
            )

            AppTextField(
                value = caption,
                onValueChange = { caption = it },
                label = "Cảm nghĩ của bạn",
                placeholder = "Món ăn hôm nay thế nào? Trải nghiệm ra sao?",
                singleLine = false
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceSm))

            AppButton(
                text = if (isPosting) "Đang đăng..." else "Đăng bài",
                onClick = {
                    isPosting = true
                    scope.launch {
                        try {
                            val session = AppContainer.observeSessionUseCase(application).invoke().value
                            val uid = session?.uid
                            val profile = if (uid != null) {
                                AppContainer.getCurrentUserProfileUseCase(application).invoke(uid)
                            } else {
                                null
                            }
                            
                            val newPost = Post(
                                id = UUID.randomUUID().toString(),
                                authorUid = profile?.uid ?: "",
                                authorName = profile?.displayName ?: "User",
                                authorAvatar = profile?.avatarUrl ?: "",
                                caption = caption,
                                imageUrls = listOf(mockImage),
                                location = restaurantName,
                                createdAt = Date(),
                                updatedAt = Date()
                            )
                            
                            AppContainer.feedRepository().createPost(newPost)
                            onBack()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isPosting = false
                        }
                    }
                },
                enabled = restaurantName.isNotBlank() && caption.isNotBlank() && !isPosting
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}
