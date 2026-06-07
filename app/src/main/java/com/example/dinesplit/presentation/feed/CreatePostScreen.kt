package com.example.dinesplit.presentation.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.dinesplit.domain.repository.SplitRepository
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Bill

@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    postId: String? = null, // Hỗ trợ Edit Mode từ file dự án chính của nhóm
    initialMode: CreatePostMode = CreatePostMode.POST,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val restaurantName by viewModel.restaurantName.collectAsState()
    val caption by viewModel.caption.collectAsState()
    val selectedImageUri by viewModel.imageUri.collectAsState()
    val visibility by viewModel.visibility.collectAsState()
    val isLoadingExistingPost by viewModel.isLoadingExistingPost.collectAsState()
    val postMode by viewModel.postMode.collectAsState()
    val isStoryMode = postMode == CreatePostMode.STORY

    val snackbarHostState = remember { SnackbarHostState() }

    // KHỞI TẠO LUỒNG ĐĂNG/SỬA: Nếu có postId, ra lệnh cho ViewModel nạp dữ liệu cũ về
    LaunchedEffect(postId, initialMode) {
        viewModel.initializePostMode(postId, initialMode)
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
        title = if (!postId.isNullOrBlank()) {
            "Chỉnh sửa bài viết"
        } else if (isStoryMode) {
            "Đăng tin 24h"
        } else {
            "Đăng bài viết mới"
        },
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
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (postId.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = postMode == CreatePostMode.POST,
                                onClick = { viewModel.updatePostMode(CreatePostMode.POST) },
                                label = { Text("Bài viết") },
                                leadingIcon = {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = postMode == CreatePostMode.STORY,
                                onClick = { viewModel.updatePostMode(CreatePostMode.STORY) },
                                label = { Text("Tin 24h") },
                                leadingIcon = {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Cụm Chọn Ảnh Ẩm Thực Cao Cấp (Hỗ trợ cả fallback ảnh mồi ngẫu nhiên)
                    AppCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (isStoryMode) "Chọn ảnh cho tin 24h" else "Chọn ảnh món ăn"
                                ) { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = if (isStoryMode) "Chọn ảnh cho tin 24h" else "Chọn ảnh món ăn",
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
                                    Text(
                                        if (isStoryMode) "Nhấp chọn ảnh cho tin 24h" else "Nhấp chọn ảnh món ăn từ máy của bạn",
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    // Các ô nhập liệu đồng bộ Design System của nhóm
                    AppTextField(
                        value = restaurantName,
                        onValueChange = { viewModel.updateRestaurantName(it) },
                        label = if (isStoryMode) "Địa điểm (Không bắt buộc)" else "Tên quán ăn / Nhà hàng",
                        placeholder = if (isStoryMode) "Ví dụ: Highlands Coffee" else "Ví dụ: Bún bò Huế O Xuân"
                    )

                    AppTextField(
                        value = caption,
                        onValueChange = { viewModel.updateCaption(it) },
                        label = "Cảm nghĩ",
                        placeholder = "Bạn thấy món ăn thế nào?",
                        singleLine = false
                    )

                    // Cụm Gắn Hóa Đơn Chia Tiền
                    if (!isStoryMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Hóa đơn liên kết (Không bắt buộc)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        
                        val selectedBillId by viewModel.selectedBillId.collectAsState()
                        val selectedBillName by viewModel.selectedBillName.collectAsState()
                        
                        if (selectedBillId != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = selectedBillName ?: "Đã gắn hóa đơn",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.selectGroup(null) }
                                    ) {
                                        Text("Hủy gắn", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            var showDialog by remember { mutableStateOf(false) }
                            val groups by viewModel.userGroups.collectAsState()
                            
                            OutlinedButton(
                                onClick = { showDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                                    Text("Gắn hóa đơn chia tiền")
                                }
                            }
                            
                            if (showDialog) {
                                SelectBillDialog(
                                    groups = groups,
                                    onSelectBill = { groupId, billId, billName ->
                                        viewModel.selectGroup(groupId)
                                        viewModel.selectBill(billId, billName)
                                        showDialog = false
                                    },
                                    onDismiss = { showDialog = false }
                                )
                            }
                        }
                    }
                    }

                    // Phân Vùng Chọn Quyền Hiển Thị (Public / Followers Only) từ file nhóm
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isStoryMode) "Ai có thể xem tin" else "Chế độ hiển thị bài đăng",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isPublic = visibility == "public"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .selectable(
                                        selected = isPublic,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.updateVisibility("public") }
                                    ),
                                color = if (isPublic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, if (isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = if (isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Column {
                                        Text("Công khai", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                        Text("Mọi người xem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            val isFollowers = visibility == "followers_only"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .selectable(
                                        selected = isFollowers,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.updateVisibility("followers_only") }
                                    ),
                                color = if (isFollowers) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, if (isFollowers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = if (isFollowers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Column {
                                        Text("Bạn bè", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                        Text("Chỉ bạn bè xem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }


                    // Chống click spam khi đang tải lên đám mây
                    if (uiState is CreatePostUiState.Loading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        AppButton(
                            text = if (!postId.isNullOrBlank()) {
                                "Cập nhật bài viết"
                            } else if (isStoryMode) {
                                "Đăng tin 24h"
                            } else {
                                "Đăng bài viết"
                            },
                            onClick = { viewModel.submitPost() },
                            enabled = uiState !is CreatePostUiState.Loading,
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

@Composable
private fun SelectBillDialog(
    groups: List<Group>,
    onSelectBill: (groupId: String, billId: String, billName: String) -> Unit,
    onDismiss: () -> Unit,
    splitRepository: SplitRepository = remember { AppContainer.splitRepository() }
) {
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var billsList by remember { mutableStateOf<List<Bill>>(emptyList()) }
    var isLoadingBills by remember { mutableStateOf(false) }

    LaunchedEffect(selectedGroup) {
        val group = selectedGroup
        if (group != null) {
            isLoadingBills = true
            splitRepository.getBills(group.id).collect { bills ->
                billsList = bills
                isLoadingBills = false
            }
        } else {
            billsList = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedGroup == null) "Chọn nhóm" else "Chọn hóa đơn trong ${selectedGroup?.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (selectedGroup == null) {
                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(AppDimens.spaceLg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Bạn chưa tham gia nhóm nào", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                        ) {
                            items(groups) { group ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedGroup = group },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(AppDimens.spaceMd),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (isLoadingBills) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(AppDimens.spaceLg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (billsList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(AppDimens.spaceLg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Không có hóa đơn nào trong nhóm này", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                                TextButton(onClick = { selectedGroup = null }) {
                                    Text("Quay lại chọn nhóm")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                        ) {
                            items(billsList) { bill ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectBill(selectedGroup!!.id, bill.id, bill.name)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(AppDimens.spaceMd)
                                    ) {
                                        Text(
                                            text = bill.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Tổng tiền: ${formatMoney(bill.totalAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedGroup != null) {
                TextButton(onClick = { selectedGroup = null }) {
                    Text("Quay lại")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

private fun formatMoney(amount: Double): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())}đ"
}
