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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.AppIconButton
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.SearchTextField
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
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
                    .padding(horizontal = AppDimens.spaceXl),
            contentPadding = PaddingValues(top = AppDimens.spaceLg, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXl),
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
                    friendUids = uiState.friendUids,
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
                .padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = "Tạo nhóm mới",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.onSurface,
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.radiusSm))
                .clickable(enabled = !isLoading) { onSave() }
                .padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lưu",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLoading) colorScheme.outline else colorScheme.primary,
            )
        }
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

    AppCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spaceLg),
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
                            .size(AppDimens.space2Xl)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer)
                            .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(AppDimens.spaceLg),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            AppTextField(
                value = groupName,
                onValueChange = onNameChange,
                label = "Tên nhóm",
                placeholder = "VD: Chuyến đi Vũng Tàu",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier =
                            Modifier
                                .clip(AppShapes.full)
                                .background(if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer)
                                .clickable { onCategorySelected(category) }
                                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
    friendUids: Set<String>,
    isSearching: Boolean,
    onProfileToggle: (UserProfile) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "THÊM THÀNH VIÊN",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = AppDimens.spaceSm, bottom = AppDimens.spaceLg),
        )

        SearchTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = "Tìm theo username...",
            onClearClick = { onSearchChange("") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = AppDimens.spaceSm, bottom = AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (searchQuery.isBlank()) "Người dùng gần đây" else "Kết quả tìm kiếm",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
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

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
                profiles.forEachIndexed { index, profile ->
                    val isFriend = friendUids.contains(profile.uid)
                    CreateGroupMemberRow(
                        profile = profile,
                        isSelected = selectedMemberIds.contains(profile.uid),
                        isFriend = isFriend,
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
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
    ) {
        Text(
            text =
                if (searchQuery.isBlank()) {
                    "Chưa có người dùng nào để gợi ý."
                } else {
                    "Không tìm thấy người dùng phù hợp."
                },
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreateGroupMemberRow(
    profile: UserProfile,
    isSelected: Boolean,
    isFriend: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val displayName = profile.displayName.ifBlank { profile.username.ifBlank { profile.email } }
    val initial = displayName.firstOrNull()?.uppercase().orEmpty()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = isFriend) { onClick() }
                .padding(AppDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (!isFriend) {
                                colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else if (isSelected) {
                                colorScheme.primary
                            } else {
                                colorScheme.outlineVariant
                            }
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = if (isFriend) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.spaceLg))
            Column {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isFriend) colorScheme.onSurface else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = if (isFriend) 0.7f else 0.4f),
                    )
                    if (!isFriend) {
                        Text(
                            text = "• Chưa kết bạn",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colorScheme.error.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        if (isFriend) {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Đã chọn", tint = colorScheme.primaryContainer)
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(AppDimens.spaceXl)
                            .border(2.dp, colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm", tint = colorScheme.outlineVariant, modifier = Modifier.size(AppDimens.spaceLg))
                }
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
                .padding(horizontal = AppDimens.spaceXl, vertical = AppDimens.spaceMd)
                .navigationBarsPadding(),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimens.spaceMd, start = AppDimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(-AppDimens.spaceMd)) {
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
                            Text("+$extraCount", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(AppDimens.spaceMd))
                Text(
                    "Đã chọn $selectedCount thành viên",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colorScheme.onSurfaceVariant,
                )
            }

            PrimaryButton(
                text = "TẠO NHÓM",
                onClick = onCreateGroup,
                modifier = Modifier.fillMaxWidth(),
                enabled = canCreate && !isLoading,
                isLoading = isLoading,
            )
        }
    }
}
