package com.example.dinesplit.presentation.feed

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.Post
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    postId: String? = null,
    onBack: () -> Unit = {},
) {
    var restaurantName by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoadingExistingPost by remember { mutableStateOf(false) }
    var existingPost by remember { mutableStateOf<Post?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Select a beautiful random placeholder food photo as initial fallback
    val mockImage =
        remember {
            listOf(
                "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1482049016688-2d3e1b311543?w=800&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=800&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&auto=format&fit=crop",
            ).random()
        }

    // Load existing post if in edit mode
    LaunchedEffect(postId) {
        if (!postId.isNullOrBlank()) {
            isLoadingExistingPost = true
            try {
                val posts = AppContainer.feedRepository().getFeedPosts().first()
                val post = posts.firstOrNull { it.id == postId }
                if (post != null) {
                    existingPost = post
                    restaurantName = post.location.orEmpty()
                    caption = post.caption
                    if (post.imageUrls.isNotEmpty()) {
                        selectedImageUri = Uri.parse(post.imageUrls.first())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingExistingPost = false
            }
        }
    }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (!postId.isNullOrBlank()) "Chỉnh sửa bài viết" else "Đăng bài viết mới",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { padding ->
        if (isLoadingExistingPost) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Elegant Image Picker Block
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.33f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp),
                            )
                            .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selectedImageUri != null) {
                        // Show custom chosen image from gallery or Firestore
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        // Glassmorphic change indicator pill at top right
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    "Đổi ảnh thư viện",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    } else {
                        // Fallback to visual preview of default random food image but styled to encourage changing
                        AsyncImage(
                            model = mockImage,
                            contentDescription = "Mock image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        // Overlay tint
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

                        // Call to Action
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .background(Color.White.copy(0.2f), CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Text(
                                text = "Nhấp để chọn ảnh từ gallery của bạn 📸",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Hoặc sử dụng ảnh món ăn ngẫu nhiên có sẵn",
                                color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Input Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Thông tin ẩm thực",
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                    )

                    OutlinedTextField(
                        value = restaurantName,
                        onValueChange = { restaurantName = it },
                        label = { Text("Tên quán ăn / Nhà hàng", style = MaterialTheme.typography.bodyMedium) },
                        placeholder = { Text("Ví dụ: Phở Thìn Lò Đúc, Pizza 4P's...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Cảm nghĩ của bạn về bữa ăn", style = MaterialTheme.typography.bodyMedium) },
                        placeholder = {
                            Text(
                                "Hôm nay bạn ăn gì? Trải nghiệm hương vị ra sao? Hãy chia sẻ cho cộng đồng nhé!",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        singleLine = false,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        isPosting = true
                        scope.launch {
                            try {
                                val session = AppContainer.observeSessionUseCase(application).invoke().value
                                val uid = session?.uid
                                val profile =
                                    if (uid != null) {
                                        AppContainer.getCurrentUserProfileUseCase(application).invoke(uid)
                                    } else {
                                        null
                                    }

                                val targetPostId = existingPost?.id ?: UUID.randomUUID().toString()
                                var finalImageUrl = mockImage
                                if (selectedImageUri != null) {
                                    val uriStr = selectedImageUri!!.toString()
                                    if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                                        // Upload local picked gallery photo to Firebase Storage
                                        finalImageUrl = AppContainer.feedRepository().uploadPostImage(targetPostId, selectedImageUri!!)
                                    } else {
                                        finalImageUrl = uriStr
                                    }
                                }

                                if (existingPost != null) {
                                    // Update existing post
                                    val updatedPost =
                                        existingPost!!.copy(
                                            caption = caption.trim(),
                                            imageUrls = listOf(finalImageUrl),
                                            location = restaurantName.trim(),
                                            updatedAt = Date(),
                                        )
                                    AppContainer.feedRepository().updatePost(updatedPost)
                                } else {
                                    // Create brand new post
                                    val newPost =
                                        Post(
                                            id = targetPostId,
                                            authorUid = profile?.uid ?: "",
                                            authorName = profile?.displayName ?: "User",
                                            authorAvatar = profile?.avatarUrl ?: "",
                                            caption = caption.trim(),
                                            imageUrls = listOf(finalImageUrl),
                                            location = restaurantName.trim(),
                                            createdAt = Date(),
                                            updatedAt = Date(),
                                        )
                                    AppContainer.feedRepository().createPost(newPost)
                                }
                                onBack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isPosting = false
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                        ),
                    enabled = restaurantName.isNotBlank() && caption.isNotBlank() && !isPosting,
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
                    } else {
                        Text(
                            if (existingPost != null) "Cập nhật bài viết" else "Đăng bài viết",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
