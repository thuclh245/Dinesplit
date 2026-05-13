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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private data class CreateGroupMemberOption(
    val id: String,
    val name: String,
    val initial: String,
    val phone: String,
    val avatarColor: @Composable () -> Color
)

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val viewModel = remember { CreateGroupViewModel(AppContainer.splitRepository(context)) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("Ăn uống", "Du lịch", "Nhà ở", "Khác")
    val friends = listOf(
        CreateGroupMemberOption("minh", "Minh", "M", "090 123 4567", { colorScheme.onSurfaceVariant }),
        CreateGroupMemberOption("thanh_hang", "Thanh Hằng", "T", "091 987 6543", { colorScheme.outline }),
        CreateGroupMemberOption("tuan_anh", "Tuấn Anh", "A", "098 555 1234", { colorScheme.outlineVariant })
    )
    val selectedCount = uiState.selectedMemberIds.size

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
                isLoading = uiState.isLoading
            )
        },
        bottomBar = {
            CreateGroupBottomAction(
                selectedCount = selectedCount,
                isLoading = uiState.isLoading,
                canCreate = uiState.groupName.isNotBlank(),
                onCreateGroup = viewModel::createGroup
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                CreateGroupInfoCard(
                    groupName = uiState.groupName,
                    onNameChange = viewModel::onGroupNameChange,
                    categories = categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected
                )
            }

            item {
                CreateGroupMembersSection(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    friends = friends,
                    selectedMemberIds = uiState.selectedMemberIds,
                    onMemberToggle = viewModel::onMemberToggled
                )
            }
        }
    }
}

@Composable
private fun CreateGroupTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.98f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Tạo nhóm mới",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        Text(
            text = "Lưu",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) colorScheme.outline else colorScheme.primary,
            modifier = Modifier.clickable(enabled = !isLoading) { onSave() }
        )
    }
}

@Composable
private fun CreateGroupInfoCard(
    groupName: String,
    onNameChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colorScheme.outline,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer)
                        .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (groupName.isEmpty()) {
                    Text(
                        text = "Tên nhóm (VD: Chuyến đi Vũng Tàu)",
                        color = colorScheme.outline,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = groupName,
                    onValueChange = onNameChange,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer)
                            .clickable { onCategorySelected(category) }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
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
    friends: List<CreateGroupMemberOption>,
    selectedMemberIds: Set<String>,
    onMemberToggle: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "THÊM THÀNH VIÊN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainerLowest, RoundedCornerShape(50))
                .border(1.dp, colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text("Tìm kiếm bạn bè...", color = colorScheme.outline, fontSize = 14.sp)
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gợi ý",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                friends.forEachIndexed { index, friend ->
                    CreateGroupMemberRow(
                        friend = friend,
                        isSelected = selectedMemberIds.contains(friend.id),
                        onClick = { onMemberToggle(friend.id) }
                    )
                    if (index < friends.size - 1) {
                        HorizontalDivider(color = colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupMemberRow(
    friend: CreateGroupMemberOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(friend.avatarColor()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.initial,
                    color = colorScheme.surfaceContainerLowest,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = friend.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = friend.phone,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Đã chọn",
                tint = colorScheme.primaryContainer
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm",
                    tint = colorScheme.outlineVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateGroupBottomAction(
    selectedCount: Int,
    isLoading: Boolean,
    canCreate: Boolean,
    onCreateGroup: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.96f))
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .navigationBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                    val avatarColors = listOf(
                        colorScheme.onSurfaceVariant,
                        colorScheme.outline,
                        colorScheme.outlineVariant
                    )
                    repeat(minOf(3, selectedCount)) { index ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(avatarColors[index % avatarColors.size])
                                .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape)
                        )
                    }

                    val extraCount = selectedCount - 3
                    if (extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceContainer)
                                .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$extraCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Đã chọn $selectedCount thành viên",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onCreateGroup,
                enabled = canCreate && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(50)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = Brush.verticalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colorScheme.surfaceContainerLowest,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "TẠO NHÓM",
                            color = colorScheme.surfaceContainerLowest,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
