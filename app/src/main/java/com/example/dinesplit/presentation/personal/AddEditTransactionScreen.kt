package com.example.dinesplit.presentation.personal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.data.ocr.MlKitReceiptTextRecognizer
import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionSource
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.receipt.ReceiptCategoryOption
import com.example.dinesplit.domain.receipt.ReceiptOcrParser
import com.example.dinesplit.domain.receipt.ReceiptOcrResult
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToLong

data class AddEditTransactionInput(
    val id: String = UUID.randomUUID().toString(),
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: String = "",
    val categoryName: String = "",
    val note: String = "",
    val receiptImageUrl: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val walletId: String = "",
    val walletName: String = "",
)

private val AddEditTransactionInputSaver =
    mapSaver(
        save = {
            mapOf(
                "id" to it.id,
                "amount" to it.amount,
                "type" to it.type.name,
                "categoryId" to it.categoryId,
                "categoryName" to it.categoryName,
                "note" to it.note,
                "receiptImageUrl" to it.receiptImageUrl,
                "dateMillis" to it.dateMillis,
                "walletId" to it.walletId,
                "walletName" to it.walletName,
            )
        },
        restore = {
            AddEditTransactionInput(
                id = it["id"] as String,
                amount = it["amount"] as String,
                type = TransactionType.valueOf(it["type"] as String),
                categoryId = it["categoryId"] as String,
                categoryName = it["categoryName"] as String,
                note = it["note"] as String,
                receiptImageUrl = it["receiptImageUrl"] as String,
                dateMillis = it["dateMillis"] as Long,
                walletId = it["walletId"] as? String ?: "",
                walletName = it["walletName"] as? String ?: "",
            )
        },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onBack: () -> Unit,
    transactionId: String? = null,
    initialTransaction: Transaction? = null,
    availableCategories: List<StoredCategory> = emptyList(),
    availableWallets: List<PersonalWallet> = emptyList(),
    transactions: List<Transaction> = emptyList(),
    goals: List<PersonalGoal> = emptyList(),
    spendingReminders: List<SpendingReminder> = emptyList(),
    recurringRules: List<RecurringRule> = emptyList(),
    onSave: (Transaction) -> Unit = {},
) {
    var input by rememberSaveable(stateSaver = AddEditTransactionInputSaver) {
        mutableStateOf(
            if (initialTransaction != null) {
                val matchingWallet = availableWallets.firstOrNull { it.id == initialTransaction.walletId }
                AddEditTransactionInput(
                    id = initialTransaction.id,
                    amount = initialTransaction.amount.toString(),
                    type = initialTransaction.type,
                    categoryId = initialTransaction.categoryId,
                    categoryName = initialTransaction.category.orEmpty(),
                    note = initialTransaction.note.orEmpty(),
                    receiptImageUrl = initialTransaction.receiptImageUrl.orEmpty(),
                    dateMillis = initialTransaction.date,
                    walletId = initialTransaction.walletId.orEmpty(),
                    walletName = matchingWallet?.name.orEmpty(),
                )
            } else {
                val defaultWallet = availableWallets.firstOrNull()
                AddEditTransactionInput(
                    walletId = defaultWallet?.id.orEmpty(),
                    walletName = defaultWallet?.name.orEmpty(),
                )
            },
        )
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showWalletDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var receiptOcrStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraReceiptUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val receiptTextRecognizer =
        remember(context) {
            MlKitReceiptTextRecognizer(context.applicationContext)
        }

    DisposableEffect(receiptTextRecognizer) {
        onDispose {
            receiptTextRecognizer.close()
        }
    }

    fun applyReceiptImage(selectedUri: Uri) {
        input = input.copy(receiptImageUrl = selectedUri.toString())
        receiptOcrStatus = null
        coroutineScope.launch {
            isScanningReceipt = true
            receiptOcrStatus = "Đang đọc hóa đơn..."
            runCatching {
                val rawText = receiptTextRecognizer.recognize(selectedUri)
                val result =
                    ReceiptOcrParser.parse(
                        rawText = rawText,
                        categories = availableCategories.toReceiptCategoryOptions(),
                    )
                val amountApplied = result.amount != null && input.amount.isBlank()
                val categoryApplied = result.category != null && input.categoryId.isBlank()
                input =
                    input.applyReceiptOcrResult(
                        result = result,
                        amountApplied = amountApplied,
                        categoryApplied = categoryApplied,
                    )
                receiptOcrStatus =
                    result.toReceiptOcrStatus(
                        amountApplied = amountApplied,
                        categoryApplied = categoryApplied,
                    )
            }.onFailure {
                receiptOcrStatus = "Không thể đọc hóa đơn. Nhập số tiền/danh mục theo cách thủ công."
            }
            isScanningReceipt = false
        }
    }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            applyReceiptImage(selectedUri)
         }
    }

    val receiptDocumentPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let { selectedUri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        selectedUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                applyReceiptImage(selectedUri)
            }
        }

    val receiptCameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { captured ->
            val capturedUri = pendingCameraReceiptUri
            if (captured && capturedUri != null) {
                applyReceiptImage(capturedUri)
            } else {
                receiptOcrStatus = "Chưa chụp được ảnh hóa đơn."
            }
        }

    val categoriesForType =
        availableCategories
            .filter { it.type == input.type }
            .sortedBy { it.name }
    val impactPreview =
        remember(input, transactions, goals, spendingReminders, recurringRules, availableCategories) {
            buildTransactionImpactPreview(
                input = input,
                transactions = transactions,
                categories = availableCategories,
                goals = goals,
                reminders = spendingReminders,
                recurringRules = recurringRules,
            )
        }

    AppScaffold(
        title = if (transactionId == null) "Thêm giao dịch" else "Chỉnh sửa giao dịch",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimens.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            Text(
                text = if (transactionId == null) "Mục mới." else "Cập nhật mục.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
            )

            // Type selector - Bộ chọn loại
             AppCard {
                 Column(
                     modifier = Modifier.padding(AppDimens.spaceMd),
                     verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                 ) {
                     Text("Loại", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                    ) {
                        TransactionType.entries.forEach { type ->
                            FilterChip(
                                selected = input.type == type,
                                onClick = { input = input.copy(type = type, categoryId = "", categoryName = "") },
                                label = { Text(type.displayLabel()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Amount - Số tiền
             AppCard {
                 AppTextField(
                     value = input.amount,
                     onValueChange = { input = input.copy(amount = it) },
                     label = "Số tiền (VND)",
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(AppDimens.spaceMd)
                 )
             }

            // Category - Danh mục
             AppCard {
                 Column(
                     modifier = Modifier.padding(AppDimens.spaceMd),
                     verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                 ) {
                     Text("Danh mục", style = MaterialTheme.typography.labelSmall)
                     Surface(
                         modifier = Modifier.fillMaxWidth(),
                         color = MaterialTheme.colorScheme.surface,
                         shape = MaterialTheme.shapes.medium
                     ) {
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .padding(AppDimens.spaceMd),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(
                                 text = input.categoryName.ifEmpty { "Chọn danh mục" },
                                 style = MaterialTheme.typography.bodyMedium
                             )
                             TextButton(onClick = { showCategoryDropdown = !showCategoryDropdown }) {
                                 Text("Thay đổi")
                             }
                         }
                     }
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        categoriesForType.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    input =
                                        input.copy(
                                            categoryId = category.id,
                                            categoryName = category.name,
                                        )
                                    showCategoryDropdown = false
                                },
                            )
                        }
                    }
                }
            }

            // Wallet - Tài khoản / Ví
            AppCard {
                Column(
                    modifier = Modifier.padding(AppDimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    Text("Tài khoản / Ví", style = MaterialTheme.typography.labelSmall)
                    if (availableWallets.isEmpty()) {
                        Text(
                            text = "Chưa có ví nào khả dụng. Vui lòng tạo ví trong mục Lập kế hoạch trước.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimens.spaceMd),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = input.walletName.ifEmpty { "Chọn ví thanh toán" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = { showWalletDropdown = !showWalletDropdown }) {
                                    Text("Thay đổi")
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = showWalletDropdown,
                            onDismissRequest = { showWalletDropdown = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            availableWallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text(wallet.name) },
                                    onClick = {
                                        input = input.copy(
                                            walletId = wallet.id,
                                            walletName = wallet.name,
                                        )
                                        showWalletDropdown = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Date - Ngày
             AppCard {
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(AppDimens.spaceMd),
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text(
                         text = "Ngày: ${SimpleDateFormat("dd MMM yyyy", Locale("vi", "VN")).format(Date(input.dateMillis))}",
                         style = MaterialTheme.typography.bodyMedium
                     )
                     IconButton(onClick = { showDatePicker = true }) {
                         Icon(Icons.Default.DateRange, contentDescription = "Chọn ngày")
                     }
                 }
             }

             // Date picker dialog - Hộp thoại chọn ngày
             if (showDatePicker) {
                 val datePickerState = rememberDatePickerState(initialSelectedDateMillis = input.dateMillis)
                 DatePickerDialog(
                     onDismissRequest = { showDatePicker = false },
                     confirmButton = {
                          TextButton(onClick = {
                              datePickerState.selectedDateMillis?.let { selectedDate ->
                                  input = input.copy(dateMillis = selectedDate)
                              }
                              showDatePicker = false
                          }) {
                              Text("Xác Nhận")
                          }
                     },
                     dismissButton = {
                         TextButton(onClick = { showDatePicker = false }) {
                             Text("Hủy")
                         }
                     }
                 ) {
                     DatePicker(state = datePickerState)
                 }
             }

            // Note - Ghi chú
             AppCard {
                 AppTextField(
                     value = input.note,
                     onValueChange = { input = input.copy(note = it) },
                     label = "Ghi chú (tùy chọn)",
                     singleLine = false,
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(AppDimens.spaceMd)
                 )
             }

            AppCard {
                 Column(
                     modifier = Modifier.padding(AppDimens.spaceMd),
                     verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                 ) {
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Row(
                             horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Icon(
                                 imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                 contentDescription = null,
                                 tint = MaterialTheme.colorScheme.primary
                             )
                             Text("Hóa đơn", style = MaterialTheme.typography.titleMedium)
                         }
                         Column(horizontalAlignment = Alignment.End) {
                             TextButton(
                                 enabled = !isScanningReceipt,
                                 onClick = {
                                     receiptPickerLauncher.launch(
                                         PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                     )
                                 }
                             ) {
                                 Text(if (input.receiptImageUrl.isBlank()) "Chọn từ thư viện" else "Thay ảnh")
                             }
                             TextButton(
                                 enabled = !isScanningReceipt,
                                 onClick = {
                                     val cameraUri = createReceiptCameraUri(context)
                                     pendingCameraReceiptUri = cameraUri
                                     receiptCameraLauncher.launch(cameraUri)
                                 }
                             ) {
                                 Text("Chụp hóa đơn")
                             }
                             TextButton(
                                 enabled = !isScanningReceipt,
                                 onClick = {
                                     receiptDocumentPickerLauncher.launch(arrayOf("image/*"))
                                 }
                             ) {
                                 Text("Tệp / Drive")
                             }
                         }
                     }

                     if (isScanningReceipt) {
                         LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                     }

                     Text(
                         text = receiptOcrStatus ?: if (input.receiptImageUrl.isBlank()) {
                             "Đính kèm ảnh hóa đơn trước khi lưu."
                         } else {
                             "Hóa đơn được đính kèm. Số tiền và danh mục có thể chỉnh sửa trước khi lưu."
                         },
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )

                     if (input.receiptImageUrl.isNotBlank()) {
                         TextButton(
                             enabled = !isScanningReceipt,
                             onClick = {
                                 input = input.copy(receiptImageUrl = "")
                                 receiptOcrStatus = null
                             }
                         ) {
                             Text("Xóa hóa đơn")
                         }
                     }
                 }
             }

            impactPreview?.let { preview ->
                TransactionImpactPreviewCard(preview = preview)
            }

            validationError?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(AppDimens.spaceMd),
                    )
                }
            }

            PrimaryButton(
                 text = if (transactionId == null) "Tạo giao dịch" else "Cập nhật giao dịch",
                 onClick = {
                     try {
                         require(input.amount.isNotBlank()) { "Số tiền là bắt buộc" }
                         require(input.amount.toDoubleOrNull() != null) { "Số tiền phải là số hợp lệ" }
                         require(input.amount.toDouble() > 0) { "Số tiền phải > 0" }
                         require(input.categoryId.isNotBlank()) { "Danh mục là bắt buộc" }
                         require(input.walletId.isNotBlank()) { "Tài khoản/Ví là bắt buộc" }

                        val transaction =
                            Transaction(
                                id = input.id,
                                userId = "",
                                amount = input.amount.toDouble(),
                                type = input.type,
                                categoryId = input.categoryId,
                                category = input.categoryName,
                                note = input.note.takeIf { it.isNotBlank() },
                                receiptImageUrl = input.receiptImageUrl.takeIf { it.isNotBlank() },
                                source =
                                    if (input.receiptImageUrl.isBlank()) {
                                        TransactionSource.MANUAL
                                    } else {
                                        TransactionSource.RECEIPT
                                    },
                                date = input.dateMillis,
                                createdAt = System.currentTimeMillis(),
                                walletId = input.walletId.takeIf { it.isNotBlank() },
                            )
                        validationError = null
                        onSave(transaction)
                    } catch (e: Exception) {
                        validationError = e.message ?: "Dữ liệu không hợp lệ"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class ImpactTone {
    POSITIVE,
    INFO,
    WARNING,
    DANGER,
}

private data class TransactionImpactPreview(
    val headline: String,
    val summary: String,
    val tone: ImpactTone,
    val categoryName: String,
    val categoryLine: String,
    val safeToSpendLine: String,
    val goalLine: String?,
    val goalProgress: Float?,
    val reminderLine: String?,
    val reminderProgress: Float?,
)

@Composable
private fun TransactionImpactPreviewCard(preview: TransactionImpactPreview) {
    val toneColor = impactToneColor(preview.tone)

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preview.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = toneColor,
                    )
                    Text(
                        text = preview.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = toneColor.copy(alpha = 0.12f),
                    contentColor = toneColor,
                ) {
                    Text(
                        text = preview.categoryName,
                        modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }

            ImpactLine(label = "Danh mục", value = preview.categoryLine)
            ImpactLine(label = "Safe-to-spend", value = preview.safeToSpendLine)

            preview.goalLine?.let { line ->
                ImpactLine(label = "Mục tiêu", value = line)
                preview.goalProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = toneColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            preview.reminderLine?.let { line ->
                ImpactLine(label = "Nhắc nhở", value = line)
                preview.reminderProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = toneColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImpactLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.36f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.64f),
        )
    }
}

@Composable
private fun impactToneColor(tone: ImpactTone): Color {
    return when (tone) {
        ImpactTone.POSITIVE -> MaterialTheme.colorScheme.secondary
        ImpactTone.INFO -> MaterialTheme.colorScheme.primary
        ImpactTone.WARNING -> MaterialTheme.colorScheme.tertiary
        ImpactTone.DANGER -> MaterialTheme.colorScheme.error
    }
}

private fun buildTransactionImpactPreview(
    input: AddEditTransactionInput,
    transactions: List<Transaction>,
    categories: List<StoredCategory>,
    goals: List<PersonalGoal>,
    reminders: List<SpendingReminder>,
    recurringRules: List<RecurringRule>,
): TransactionImpactPreview? {
    val amount = input.amount.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    val categoryId = input.categoryId.takeIf { it.isNotBlank() } ?: return null
    val category = categories.firstOrNull { it.id == categoryId }
    val categoryName = input.categoryName.ifBlank { category?.name ?: "Danh mục" }
    val baseTransactions = transactions.filterNot { it.id == input.id }
    val simulatedTransaction =
        Transaction(
            id = input.id,
            userId = "",
            amount = amount,
            type = input.type,
            categoryId = categoryId,
            category = categoryName,
            note = input.note.takeIf { it.isNotBlank() },
            receiptImageUrl = input.receiptImageUrl.takeIf { it.isNotBlank() },
            source = if (input.receiptImageUrl.isBlank()) TransactionSource.MANUAL else TransactionSource.RECEIPT,
            date = input.dateMillis,
            createdAt = System.currentTimeMillis(),
            walletId = input.walletId.takeIf { it.isNotBlank() },
        )

    val monthStart = startOfMonth(input.dateMillis)
    val monthEnd = endOfMonth(input.dateMillis)
    val categoryCurrent =
        baseTransactions
            .filter { transaction ->
                transaction.categoryId == categoryId &&
                    transaction.type == input.type &&
                    transaction.date in monthStart..monthEnd
            }
            .sumOf { it.amount }
    val categoryAfter = categoryCurrent + amount
    val linkedGoal = goals.firstOrNull { it.categoryId == categoryId && it.status != GoalStatus.PAUSED }
    val goalImpact = linkedGoal?.let { goal ->
        buildGoalImpactLine(
            goal = goal,
            categoryType = category?.type ?: input.type,
            amount = amount,
            transactions = baseTransactions,
            transactionDate = input.dateMillis,
            categoryId = categoryId,
        )
    }
    val reminderImpact =
        if (input.type == TransactionType.EXPENSE) {
            val reminder =
                reminders
                    .filter { it.isEnabled }
                    .filter { it.categoryId == categoryId || it.categoryId == null }
                    .sortedByDescending { it.categoryId == categoryId }
                    .firstOrNull()
            reminder?.let {
                buildReminderImpactLine(
                    reminder = it,
                    transactions = baseTransactions,
                    simulatedTransaction = simulatedTransaction,
                )
            }
        } else {
            null
        }

    val recurringReserve =
        recurringRules
            .filter { it.isEnabled && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    val categoryTypesById = categories.associate { it.id to it.type }
    val goalReserveBefore = goals.toGoalReserve(categoryTypesById)
    val goalsAfter =
        if (linkedGoal != null && goalImpact != null) {
            goals.map { goal ->
                if (goal.id == linkedGoal.id) goal.copy(currentAmount = goalImpact.afterAmount) else goal
            }
        } else {
            goals
        }
    val goalReserveAfter = goalsAfter.toGoalReserve(categoryTypesById)
    val safeBefore =
        baseTransactions.toSafeToSpendForecast(
            referenceMillis = input.dateMillis,
            upcomingRecurringExpense = recurringReserve,
            savingsGoal = goalReserveBefore,
        )
    val safeAfter =
        (baseTransactions + simulatedTransaction).toSafeToSpendForecast(
            referenceMillis = input.dateMillis,
            upcomingRecurringExpense = recurringReserve,
            savingsGoal = goalReserveAfter,
        )

    val tone =
        when {
            reminderImpact?.isOverBudget == true -> ImpactTone.DANGER
            goalImpact?.isOverLimit == true -> ImpactTone.DANGER
            safeAfter.status == SafeToSpendStatus.OVER -> ImpactTone.DANGER
            reminderImpact?.isOverThreshold == true -> ImpactTone.WARNING
            goalImpact?.progress?.let { it >= 0.85f } == true -> ImpactTone.WARNING
            safeAfter.status == SafeToSpendStatus.WATCH -> ImpactTone.WARNING
            safeAfter.dailyAmount >= safeBefore.dailyAmount -> ImpactTone.POSITIVE
            else -> ImpactTone.INFO
        }
    val headline =
        when (tone) {
            ImpactTone.POSITIVE -> "Tác động tích cực"
            ImpactTone.INFO -> "Tác động nằm trong kế hoạch"
            ImpactTone.WARNING -> "Sắp chạm ngưỡng"
            ImpactTone.DANGER -> "Cần xem lại trước khi lưu"
        }
    val summary =
        if (input.type == TransactionType.INCOME) {
            "Khoản thu này sẽ tăng vùng an toàn còn lại của tháng."
        } else {
            "Khoản chi này được giả lập trước khi ghi vào sổ."
        }

    return TransactionImpactPreview(
        headline = headline,
        summary = summary,
        tone = tone,
        categoryName = categoryName,
        categoryLine = "${formatImpactMoney(categoryCurrent)} -> ${formatImpactMoney(categoryAfter)} tháng này",
        safeToSpendLine = "${formatImpactMoney(safeBefore.dailyAmount)}/ngày -> ${formatImpactMoney(safeAfter.dailyAmount)}/ngày",
        goalLine = goalImpact?.line,
        goalProgress = goalImpact?.progress,
        reminderLine = reminderImpact?.line,
        reminderProgress = reminderImpact?.progress,
    )
}

private data class GoalImpact(
    val line: String,
    val progress: Float,
    val afterAmount: Double,
    val isOverLimit: Boolean,
)

private fun buildGoalImpactLine(
    goal: PersonalGoal,
    categoryType: TransactionType,
    amount: Double,
    transactions: List<Transaction>,
    transactionDate: Long,
    categoryId: String,
): GoalImpact {
    val referenceDate =
        when {
            goal.deadlineAt > 0L -> goal.deadlineAt
            goal.createdAt > 0L -> goal.createdAt
            else -> transactionDate
        }
    val goalMonthStart = startOfMonth(referenceDate)
    val goalMonthEnd = endOfMonth(referenceDate)
    val affectsGoalMonth = transactionDate in goalMonthStart..goalMonthEnd
    val currentAmount =
        transactions
            .filter { transaction -> transaction.categoryId == categoryId && transaction.date in goalMonthStart..goalMonthEnd }
            .sumOf { it.amount }
            .takeIf { it > 0.0 || affectsGoalMonth }
            ?: goal.currentAmount
    val afterAmount = currentAmount + if (affectsGoalMonth) amount else 0.0
    val progress = if (goal.targetAmount > 0.0) (afterAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = goal.targetAmount - afterAmount
    val line =
        if (categoryType == TransactionType.EXPENSE) {
            if (remaining >= 0.0) {
                "${goal.title}: còn ${formatImpactMoney(remaining)} trước giới hạn"
            } else {
                "${goal.title}: vượt ${formatImpactMoney(kotlin.math.abs(remaining))}"
            }
        } else {
            if (remaining <= 0.0) {
                "${goal.title}: đạt mục tiêu"
            } else {
                "${goal.title}: còn ${formatImpactMoney(remaining)} để đạt"
            }
        }

    return GoalImpact(
        line = line,
        progress = progress,
        afterAmount = afterAmount,
        isOverLimit = categoryType == TransactionType.EXPENSE && afterAmount > goal.targetAmount,
    )
}

private data class ReminderImpact(
    val line: String,
    val progress: Float,
    val isOverThreshold: Boolean,
    val isOverBudget: Boolean,
)

private fun buildReminderImpactLine(
    reminder: SpendingReminder,
    transactions: List<Transaction>,
    simulatedTransaction: Transaction,
): ReminderImpact {
    val effectiveThreshold =
        if (reminder.reminderType == ReminderType.MILESTONE) {
            1f
        } else {
            reminder.threshold
        }
    val currentSpent =
        transactions
            .filter { transaction ->
                transaction.type == TransactionType.EXPENSE &&
                    transaction.isInsideReminderWindow(reminder, simulatedTransaction.date) &&
                    (reminder.categoryId == null || transaction.categoryId == reminder.categoryId)
            }
            .sumOf { it.amount }
    val affectsReminder =
        simulatedTransaction.type == TransactionType.EXPENSE &&
            simulatedTransaction.isInsideReminderWindow(reminder, simulatedTransaction.date) &&
            (reminder.categoryId == null || simulatedTransaction.categoryId == reminder.categoryId)
    val afterSpent = currentSpent + if (affectsReminder) simulatedTransaction.amount else 0.0
    val thresholdAmount = reminder.budgetAmount * effectiveThreshold
    val isOverThreshold = afterSpent >= thresholdAmount
    val isOverBudget = afterSpent >= reminder.budgetAmount
    val line =
        when {
            !affectsReminder -> "${reminder.categoryName}: không nằm trong kỳ nhắc nhở"
            isOverBudget -> "${reminder.categoryName}: vượt ngân sách ${formatImpactMoney(reminder.budgetAmount)}"
            isOverThreshold -> "${reminder.categoryName}: chạm ngưỡng ${formatImpactMoney(thresholdAmount)}"
            else -> "${reminder.categoryName}: ${formatImpactMoney(afterSpent)} / ${formatImpactMoney(thresholdAmount)}"
        }

    return ReminderImpact(
        line = line,
        progress = if (reminder.budgetAmount > 0.0) (afterSpent / reminder.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f,
        isOverThreshold = isOverThreshold,
        isOverBudget = isOverBudget,
    )
}

private fun List<PersonalGoal>.toGoalReserve(categoryTypesById: Map<String, TransactionType>): Double {
    return filter { it.status == GoalStatus.ACTIVE }
        .filter { goal ->
            goal.categoryId == null ||
                categoryTypesById[goal.categoryId] == TransactionType.INCOME
        }
        .sumOf { goal -> (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0) }
        .coerceAtMost(5_000_000.0)
}

private fun Transaction.isInsideReminderWindow(
    reminder: SpendingReminder,
    referenceMillis: Long,
): Boolean {
    val transactionCalendar = java.util.Calendar.getInstance().apply { timeInMillis = date }
    val referenceCalendar = java.util.Calendar.getInstance().apply { timeInMillis = referenceMillis }

    return when (reminder.reminderType) {
        ReminderType.DAILY ->
            transactionCalendar.get(java.util.Calendar.YEAR) == referenceCalendar.get(java.util.Calendar.YEAR) &&
                transactionCalendar.get(java.util.Calendar.DAY_OF_YEAR) == referenceCalendar.get(java.util.Calendar.DAY_OF_YEAR)
        ReminderType.WEEKLY ->
            transactionCalendar.get(java.util.Calendar.YEAR) == referenceCalendar.get(java.util.Calendar.YEAR) &&
                transactionCalendar.get(java.util.Calendar.WEEK_OF_YEAR) == referenceCalendar.get(java.util.Calendar.WEEK_OF_YEAR)
        ReminderType.MONTHLY ->
            transactionCalendar.get(java.util.Calendar.YEAR) == referenceCalendar.get(java.util.Calendar.YEAR) &&
                transactionCalendar.get(java.util.Calendar.MONTH) == referenceCalendar.get(java.util.Calendar.MONTH)
        ReminderType.MILESTONE -> date >= reminder.createdAt
    }
}

private fun startOfMonth(referenceMillis: Long): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun endOfMonth(referenceMillis: Long): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun formatImpactMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}

private fun createReceiptCameraUri(context: Context): Uri {
    val receiptDirectory = File(context.cacheDir, "receipts").apply { mkdirs() }
    val receiptFile = File(receiptDirectory, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        receiptFile,
    )
}

private fun List<StoredCategory>.toReceiptCategoryOptions(): List<ReceiptCategoryOption> {
    return map { category ->
        ReceiptCategoryOption(
            id = category.id,
            name = category.name,
            type = category.type,
        )
    }
}

private fun AddEditTransactionInput.applyReceiptOcrResult(
    result: ReceiptOcrResult,
    amountApplied: Boolean,
    categoryApplied: Boolean,
): AddEditTransactionInput {
    val detectedCategory = result.category
    return copy(
        amount =
            if (amountApplied && result.amount != null) {
                formatReceiptAmountInput(result.amount)
            } else {
                amount
            },
        type = if (categoryApplied && detectedCategory != null) detectedCategory.type else type,
        categoryId = if (categoryApplied && detectedCategory != null) detectedCategory.id else categoryId,
        categoryName = if (categoryApplied && detectedCategory != null) detectedCategory.name else categoryName,
        note =
            if (note.isBlank() && !result.merchantName.isNullOrBlank()) {
                result.merchantName
            } else {
                note
            },
    )
}

private fun ReceiptOcrResult.toReceiptOcrStatus(
     amountApplied: Boolean,
     categoryApplied: Boolean
 ): String {
     if (rawText.isBlank()) return "Không tìm thấy văn bản có thể đọc được. Nhập số tiền/danh mục theo cách thủ công."

     val detectedParts = listOfNotNull(
         amount?.let { formatReceiptAmountLabel(it) },
         category?.name
     )
     val detectedText = detectedParts.joinToString(" - ")

     return when {
         detectedText.isBlank() -> "Hóa đơn được đính kèm. Không phát hiện tổng/danh mục."
         amountApplied || categoryApplied -> "Phát hiện $detectedText"
         else -> "Phát hiện $detectedText. Các trường hiện có được giữ lại."
     }
 }

private fun formatReceiptAmountInput(amount: Double): String {
    val rounded = amount.roundToLong()
    return if (abs(amount - rounded) < 0.01) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.2f", amount)
    }
}

private fun formatReceiptAmountLabel(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.roundToLong())} VND"
}
