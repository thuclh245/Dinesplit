package com.example.dinesplit.presentation.personal

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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.data.ocr.MlKitReceiptTextRecognizer
import com.example.dinesplit.domain.model.PersonalWallet
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

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
             input = input.copy(receiptImageUrl = selectedUri.toString())
             receiptOcrStatus = null
             coroutineScope.launch {
                 isScanningReceipt = true
                 receiptOcrStatus = "Đang đọc hóa đơn..."
                 runCatching {
                     val rawText = receiptTextRecognizer.recognize(selectedUri)
                     val result = ReceiptOcrParser.parse(
                         rawText = rawText,
                         categories = availableCategories.toReceiptCategoryOptions()
                     )
                     val amountApplied = result.amount != null && input.amount.isBlank()
                     val categoryApplied = result.category != null && input.categoryId.isBlank()
                     input = input.applyReceiptOcrResult(
                         result = result,
                         amountApplied = amountApplied,
                         categoryApplied = categoryApplied
                     )
                     receiptOcrStatus = result.toReceiptOcrStatus(
                         amountApplied = amountApplied,
                         categoryApplied = categoryApplied
                     )
                 }.onFailure {
                     receiptOcrStatus = "Không thể đọc hóa đơn. Nhập số tiền/danh mục theo cách thủ công."
                 }
                 isScanningReceipt = false
             }
         }
    }

    val categoriesForType =
        availableCategories
            .filter { it.type == input.type }
            .sortedBy { it.name }

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
                 OutlinedTextField(
                     value = input.amount,
                     onValueChange = { input = input.copy(amount = it) },
                     label = { Text("Số tiền (VND)") },
                     singleLine = true,
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
                 OutlinedTextField(
                     value = input.note,
                     onValueChange = { input = input.copy(note = it) },
                     label = { Text("Ghi chú (tùy chọn)") },
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(AppDimens.spaceMd),
                     minLines = 3
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
                         TextButton(
                             enabled = !isScanningReceipt,
                             onClick = {
                                 receiptPickerLauncher.launch(
                                     PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                 )
                             }
                         ) {
                             Text(if (input.receiptImageUrl.isBlank()) "Quét hóa đơn" else "Thay đổi")
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
