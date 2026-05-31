package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.UserProfile

@Composable
fun CreateGroupScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val viewModel =
        remember {
            CreateGroupViewModel(
                repository = AppContainer.splitRepository(context),
                profileRepository = AppContainer.profileRepository(context),
                currentUserId = FirebaseProviders.auth.currentUser?.uid,
            )
        }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val categories = listOf("Ăn uống", "Du lịch", "Nhà ở", "Khác")

    LaunchedEffect(uiState.isCreated) {
        if (uiState.isCreated) onBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            CreateGroupTopBar(
                onBack = onBack,
                onSave = viewModel::createGroup,
                isLoading = uiState.isLoading,
            )
        },
        bottomBar = {
            CreateGroupBottomAction(
                selectedCount = uiState.totalMemberCount,
                isLoading = uiState.isLoading,
                canCreate = uiState.groupName.isNotBlank(),
                onCreateGroup = viewModel::createGroup,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                CreateGroupInfoCard(
                    groupName = uiState.groupName,
                    onNameChange = viewModel::onGroupNameChange,
                    categories = categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected,
                )
            }

            item {
                CreateGroupMembersSection(
                    searchQuery = uiState.searchQuery,
                    onSearchChange = viewModel::onSearchQueryChange,
                    profiles = uiState.searchResults,
                    selectedMemberIds = uiState.selectedMemberIds,
                    isSearching = uiState.isSearching,
                    onProfileToggle = viewModel::onProfileToggled,
                )
            }
        }
    }
}

@Composable
private fun CreateGroupTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.98f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = "Tạo nhóm mới",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
        )

        Text(
            text = "Lưu",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) colorScheme.outline else colorScheme.primary,
            modifier = Modifier.clickable(enabled = !isLoading) { onSave() },
        )
    }
}

@Composable
private fun CreateGroupInfoCard(
    groupName: String,
    onNameChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier =
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.outline, modifier = Modifier.size(38.dp))
                }
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer)
                            .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                if (groupName.isEmpty()) {
                    Text("Tên nhóm (VD: Chuyến đi Vũng Tàu)", color = colorScheme.outline, fontSize = 14.sp)
                }
                BasicTextField(
                    value = groupName,
                    onValueChange = onNameChange,
                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer)
                                .clickable { onCategorySelected(category) }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupMembersSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    profiles: List<UserProfile>,
    selectedMemberIds: Set<String>,
    isSearching: Boolean,
    onProfileToggle: (UserProfile) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "THÊM THÀNH VIÊN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLowest, RoundedCornerShape(50))
                    .border(1.dp, colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.outline, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text("Tìm theo username...", color = colorScheme.outline, fontSize = 14.sp)
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (searchQuery.isBlank()) "Người dùng gần đây" else "Kết quả tìm kiếm",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
        }

        if (profiles.isEmpty() && !isSearching) {
            EmptyProfileSearchCard(searchQuery = searchQuery)
            return
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column {
                profiles.forEachIndexed { index, profile ->
                    CreateGroupMemberRow(
                        profile = profile,
                        isSelected = selectedMemberIds.contains(profile.uid),
                        onClick = { onProfileToggle(profile) },
                    )
                    if (index < profiles.size - 1) {
                        HorizontalDivider(color = colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfileSearchCard(searchQuery: String) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text =
                if (searchQuery.isBlank()) {
                    "Chưa có người dùng nào để gợi ý."
                } else {
                    "Không tìm thấy người dùng phù hợp."
                },
            modifier = Modifier.padding(18.dp),
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreateGroupMemberRow(
    profile: UserProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val displayName = profile.displayName.ifBlank { profile.username.ifBlank { profile.email } }
    val initial = displayName.firstOrNull()?.uppercase().orEmpty()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colorScheme.primary else colorScheme.outlineVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colorScheme.onSurface)
                Text(
                    text = "@${profile.username}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Đã chọn", tint = colorScheme.primaryContainer)
        } else {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .border(2.dp, colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm", tint = colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CreateGroupBottomAction(
    selectedCount: Int,
    isLoading: Boolean,
    canCreate: Boolean,
    onCreateGroup: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.96f))
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .navigationBarsPadding(),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                    val avatarColors = listOf(colorScheme.primary, colorScheme.secondary, colorScheme.tertiary)
                    repeat(minOf(3, selectedCount)) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(avatarColors[index % avatarColors.size])
                                    .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                        )
                    }

                    val extraCount = selectedCount - 3
                    if (extraCount > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceContainer)
                                    .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+$extraCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Đã chọn $selectedCount thành viên",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = onCreateGroup,
                enabled = canCreate && !isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(50),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(brush = Brush.verticalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colorScheme.surfaceContainerLowest,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "TẠO NHÓM",
                            color = colorScheme.surfaceContainerLowest,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
        }
    }
}
