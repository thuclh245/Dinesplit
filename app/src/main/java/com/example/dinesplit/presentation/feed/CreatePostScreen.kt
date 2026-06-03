package com.example.dinesplit.presentation.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.AppButton
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.LoadingBlock

@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    postId: String? = null, // Hỗ trợ Edit Mode từ file dự án chính của nhóm
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val restaurantName by viewModel.restaurantName.collectAsState()
    val caption by viewModel.caption.collectAsState()
    val selectedImageUri by viewModel.imageUri.collectAsState()
    val visibility by viewModel.visibility.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val isLoadingExistingPost by viewModel.isLoadingExistingPost.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // KHỞI TẠO LUỒNG ĐĂNG/SỬA: Nếu có postId, ra lệnh cho ViewModel nạp dữ liệu cũ về
    LaunchedEffect(postId) {
        viewModel.initializePostMode(postId)
    }

    // SIDE-EFFECTS CONTROL: Đảm bảo tác vụ điều hướng/thông báo lỗi mạng chạy chuẩn xác
    LaunchedEffect(uiState) {
        if (uiState is CreatePostUiState.Success) {
            onBack()
            viewModel.resetUiState() // Trả state về Idle để tránh bẫy loop khi quay lại backstack
        } else if (uiState is CreatePostUiState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = (uiState as CreatePostUiState.Error).message,
                actionLabel = "Thử lại"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.submitPost()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.updateImageUri(uri) }

    AppScaffold(
        title = if (!postId.isNullOrBlank()) "Chỉnh sửa bài viết" else "Đăng bài viết mới",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Hủy", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoadingExistingPost) {
                // Trạng thái chờ tải bài viết cũ trong Edit Mode
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingBlock(message = "Đang tải bài viết cũ...")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Cụm Chọn Ảnh Ẩm Thực Cao Cấp (Hỗ trợ cả fallback ảnh mồi ngẫu nhiên)
                    AppCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Food Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Nhãn thủy tinh báo đổi ảnh
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("Đổi ảnh", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Text("Nhấp chọn ảnh món ăn từ máy của bạn 📸", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    // Các ô nhập liệu đồng bộ Design System của nhóm
                    AppTextField(
                        value = restaurantName,
                        onValueChange = { viewModel.updateRestaurantName(it) },
                        label = "Tên quán ăn / Nhà hàng",
                        placeholder = "Ví dụ: Phở Thìn Lò Đúc, Pizza 4P's..."
                    )

                    AppTextField(
                        value = caption,
                        onValueChange = { viewModel.updateCaption(it) },
                        label = "Cảm nghĩ của bạn về bữa ăn",
                        placeholder = "Hôm nay bạn ăn gì? Trải nghiệm ra sao?",
                        singleLine = false
                    )

                    // Phân Vùng Chọn Quyền Hiển Thị (Public / Followers Only) từ file nhóm
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Chế độ hiển thị bài đăng",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isPublic = visibility == "public"
                            Surface(
                                modifier = Modifier.weight(1f).clickable { viewModel.updateVisibility("public") },
                                color = if (isPublic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, if (isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = if (isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Column {
                                        Text("Công khai", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                        Text("Mọi người xem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            val isFollowers = visibility == "followers_only"
                            Surface(
                                modifier = Modifier.weight(1f).clickable { viewModel.updateVisibility("followers_only") },
                                color = if (isFollowers) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, if (isFollowers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = if (isFollowers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Column {
                                        Text("Bạn bè", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                        Text("Người theo dõi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chống click spam khi đang tải lên đám mây
                    if (uiState is CreatePostUiState.Loading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        AppButton(
                            text = if (postId.isNullOrBlank()) "Đăng bài viết" else "Cập nhật bài viết",
                            onClick = { viewModel.submitPost() },
                            enabled = isFormValid,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Đưa SnackbarHost ra làm bộ hiển thị overlay ở đáy màn hình
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            )
        }
    }
}