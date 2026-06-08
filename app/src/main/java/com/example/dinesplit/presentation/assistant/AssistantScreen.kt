package com.example.dinesplit.presentation.assistant

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.Story
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.usecase.SplitCalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Một dòng hội thoại trong Assistant.
 *
 * @property id ID ổn định để LazyColumn diff item.
 * @property text Nội dung tin nhắn.
 * @property fromUser true nếu message do người dùng gửi, false nếu là phản hồi của Assistant.
 */
data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromUser: Boolean,
)

/**
 * State của màn hình Assistant.
 *
 * @property input Nội dung hiện tại trong ô nhập.
 * @property messages Danh sách hội thoại đang hiển thị.
 * @property isLoading true khi Assistant đang đọc dữ liệu repository và tạo câu trả lời.
 */
data class AssistantUiState(
    val input: String = "",
    val messages: List<AssistantMessage> =
        listOf(
            AssistantMessage(
                text =
                    "Mình có thể trả lời về bài đăng, bạn bè, người dùng, địa điểm, bill/split, " +
                        "nợ cần trả, chi tiêu, ví, goal, reminder và profile trong DineSplit.",
                fromUser = false,
            ),
        ),
    val isLoading: Boolean = false,
)

/**
 * Kết quả gợi ý địa điểm dùng trong câu trả lời về place/restaurant.
 *
 * @property name Tên địa điểm.
 * @property category Mô tả/category ngắn.
 * @property count Số lần xuất hiện hoặc điểm ưu tiên để sắp xếp.
 * @property imageHint URL ảnh gợi ý nếu lấy được từ bài đăng.
 */
private data class PlaceAnswer(
    val name: String,
    val category: String,
    val count: Int,
    val imageHint: String = "",
)

/**
 * Ngữ cảnh bill đã được gắn thêm tên nhóm và map tên thành viên để format câu trả lời split.
 *
 * @property groupName Tên nhóm chứa bill.
 * @property bill Bill cần phân tích.
 * @property memberNames Map từ memberId sang tên hiển thị.
 */
private data class BillContext(
    val groupName: String,
    val bill: Bill,
    val memberNames: Map<String, String>,
)

/**
 * ViewModel cho Assistant nội bộ của DineSplit.
 *
 * Assistant này không gọi LLM bên ngoài; nó dùng intent keyword matching, normalize tiếng Việt
 * không dấu và truy vấn các repository sẵn có để trả lời về feed, story, profile, split bill,
 * tài chính cá nhân, ví, goal và reminder.
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val feedRepository = AppContainer.feedRepository()
    private val profileRepository = AppContainer.profileRepository(application)
    private val personalRepository = AppContainer.personalRepository(application)
    private val splitRepository = AppContainer.splitRepository(application)
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    /**
     * Cập nhật text đang nhập trong ô hỏi Assistant.
     *
     * @param value Nội dung mới.
     */
    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    /**
     * Gửi câu hỏi hiện tại, thêm message người dùng vào hội thoại và tạo câu trả lời bất đồng bộ.
     *
     * @param text Nội dung câu hỏi; mặc định lấy từ [AssistantUiState.input].
     */
    fun submit(text: String = _uiState.value.input) {
        val question = text.trim()
        if (question.isBlank() || _uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                input = "",
                isLoading = true,
                messages = it.messages + AssistantMessage(text = question, fromUser = true),
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val answer =
                runCatching { answer(question) }
                    .getOrElse { throwable ->
                        "Mình chưa xử lý được câu này: ${FirebaseErrorMapper.toUserMessage(throwable)}"
                    }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    messages = it.messages + AssistantMessage(text = answer, fromUser = false),
                )
            }
        }
    }

    /**
     * Router intent chính của Assistant.
     *
     * Hàm normalize câu hỏi thành search key không dấu, tách token, lấy UID hiện tại rồi chuyển
     * sang nhóm trả lời phù hợp theo thứ tự ưu tiên: help, group, split, personal, story, place,
     * post, people, profile, general search.
     *
     * @param question Câu hỏi gốc của người dùng.
     * @return Chuỗi phản hồi đã format để hiển thị trong bubble Assistant.
     */
    private suspend fun answer(question: String): String {
        val key = question.toSearchKey()
        val tokens = key.tokens()
        val uid = observeSessionUseCase().value?.uid.orEmpty()

        return when {
            key.hasAny("tro giup", "lam duoc gi", "huong dan", "help") -> helpAnswer()
            key.isGroupQuestion() -> answerGroups(uid, key)
            key.isSplitQuestion(tokens) -> answerSplit(uid, key)
            key.isPersonalQuestion() -> answerPersonal(uid, key)
            key.isStoryQuestion(tokens) -> answerStories(key, uid)
            key.isPlaceQuestion() -> answerPlaces(question, key)
            key.isPostQuestion() -> answerPosts(question, key, uid)
            key.isPeopleQuestion() -> answerPeople(question, key, uid)
            key.hasAny("profile", "ho so", "tai khoan cua toi", "toi la ai") -> answerProfile(uid)
            else -> answerGeneralSearch(question, uid)
        }
    }

    /**
     * Trả lời hướng dẫn các nhóm câu hỏi Assistant đang hỗ trợ.
     *
     * @return Nội dung help nhiều dòng.
     */
    private fun helpAnswer(): String {
        return listOf(
            "Bạn có thể hỏi theo các nhóm này:",
            "1. Bài đăng: \"tìm bài về pizza\", \"bài đã lưu của tôi\".",
            "2. Bạn bè/người dùng: \"tìm Minh\", \"ai là bạn bè của tôi\".",
            "3. Địa điểm: \"địa điểm nổi bật\", \"tìm quán phở\".",
            "4. Bill/split: \"tôi nợ ai\", \"ai nợ tôi\", \"tóm tắt bill\".",
            "5. Ví cá nhân: \"chi tiêu tháng này\", \"goal của tôi\", \"reminder đang bật\", \"số dư ví\".",
            "6. Profile: \"profile của tôi thế nào\".",
            "7. Tin/story: \"tin moi\", \"tin cua toi\", \"tin da thich\".",
        ).joinToString("\n")
    }

    /**
     * Trả lời câu hỏi liên quan đến bài đăng.
     *
     * Hiện tại hàm chuyển sang [answerPostsFlexible] để dùng logic tìm kiếm rộng hơn; phần code
     * phía sau được giữ lại như fallback/legacy nếu cần khôi phục cách search cũ.
     *
     * @param question Câu hỏi gốc.
     * @param key Câu hỏi đã normalize.
     * @param uid UID người hiện tại, có thể rỗng nếu chưa đăng nhập.
     * @return Câu trả lời về bài đăng hoặc bài đã lưu.
     */
    private suspend fun answerPosts(
        question: String,
        key: String,
        uid: String,
    ): String {
        return answerPostsFlexible(question, key, uid)

        if (key.hasAny("da luu", "bai luu", "saved", "save")) {
            if (uid.isBlank()) return "Bạn cần đăng nhập để xem bài đã lưu."
            val savedPosts = feedRepository.getSavedPosts(uid).first().take(6)
            if (savedPosts.isEmpty()) return "Bạn chưa lưu bài viết nào."
            return "Các bài bạn đã lưu:\n" + savedPosts.joinToString("\n") { post ->
                "- ${post.authorName}: ${post.caption.previewText()}${post.location?.let { " tại $it" }.orEmpty()}"
            }
        }

        val term = question.intentTerm(
            "bài đăng",
            "bài viết",
            "post",
            "tìm",
            "kiếm",
            "về",
            "co",
            "có",
        )
        val posts =
            if (term.isBlank()) {
                feedRepository.getFeedPostsBatch(10, null)
            } else {
                feedRepository.searchPosts(term).ifEmpty { feedRepository.getFeedPostsBatch(20, null).filterByPostTerm(term) }
            }.take(6)

        if (posts.isEmpty()) return "Mình chưa tìm thấy bài đăng phù hợp với \"$question\"."

        return "Mình tìm thấy ${posts.size} bài liên quan:\n" +
            posts.joinToString("\n") { post ->
                "- ${post.authorName}: ${post.caption.previewText()}${post.location?.let { " tại $it" }.orEmpty()}"
            }
    }

    /**
     * Tìm và format bài đăng theo nhiều kiểu intent hơn cách search cơ bản.
     *
     * Hàm nhận diện bài đã lưu, bài của tôi, feed mới hoặc term tự do rồi lọc trên caption,
     * tên tác giả và địa điểm.
     *
     * @param question Câu hỏi gốc.
     * @param key Câu hỏi đã normalize.
     * @param uid UID người hiện tại.
     * @return Câu trả lời về tối đa 6 bài đăng phù hợp.
     */
    private suspend fun answerPostsFlexible(
        question: String,
        key: String,
        uid: String,
    ): String {
        if (key.hasAny("da luu", "bai luu", "saved", "bookmark")) {
            if (uid.isBlank()) return "Bạn cần đăng nhập để mình đọc danh sách bài đã lưu."
            val savedPosts = feedRepository.getSavedPosts(uid).first().take(6)
            if (savedPosts.isEmpty()) {
                return "Bạn chưa lưu bài nào. Khi bạn bấm bookmark ở feed, mình sẽ tóm tắt lại được ngay."
            }
            return formatPostAnswer("Các bài bạn đã lưu", savedPosts, uid)
        }

        val allPosts = feedRepository.getFeedPostsBatch(80, null)
        val term = key.intentKeyWithout(
            "bai",
            "dang",
            "viet",
            "post",
            "feed",
            "tim",
            "kiem",
            "ve",
            "co",
            "cua",
            "toi",
            "minh",
            "hien",
            "tai",
            "moi",
            "nhat",
            "gan",
            "day",
            "xem",
            "cho",
            "hoi",
        )

        val posts =
            when {
                key.hasAny("cua toi", "toi dang", "minh dang", "bai toi", "bai cua minh") && uid.isNotBlank() ->
                    allPosts.filter { it.authorUid == uid }
                term.isBlank() ->
                    allPosts
                else ->
                    allPosts.filterByPostTerm(term)
            }.take(6)

        if (posts.isEmpty()) {
            return if (term.isBlank()) {
                "Mình chưa thấy bài đăng nào trong feed hiện tại."
            } else {
                "Mình chưa thấy bài nào khớp \"$term\". Bạn thử hỏi theo tên món, địa điểm hoặc tên người đăng nhé."
            }
        }

        val title =
            when {
                key.hasAny("cua toi", "toi dang", "minh dang", "bai toi", "bai cua minh") -> "Bài đăng của bạn"
                term.isBlank() -> "Bài đăng mới trong feed"
                else -> "Bài đăng khớp \"$term\""
            }
        return formatPostAnswer(title, posts, uid)
    }

    /**
     * Format danh sách bài đăng thành câu trả lời nhiều dòng.
     *
     * @param title Tiêu đề nhóm kết quả.
     * @param posts Danh sách bài đăng cần hiển thị.
     * @param uid UID người hiện tại để đổi tên tác giả thành "Bạn" nếu phù hợp.
     * @return Chuỗi phản hồi đã format.
     */
    private fun formatPostAnswer(
        title: String,
        posts: List<Post>,
        uid: String,
    ): String {
        val lines = posts.joinToString("\n") { post ->
            val owner = if (post.authorUid == uid) "Bạn" else post.authorName.ifBlank { "Người dùng" }
            val place = post.location?.takeIf { it.isNotBlank() }?.let { " tại $it" }.orEmpty()
            val stats = "${post.likesCount} tim, ${post.commentsCount} bình luận"
            "- $owner$place: ${post.caption.previewText()} ($stats)"
        }
        return "$title:\n$lines"
    }

    /**
     * Trả lời câu hỏi về story/tin đang hoạt động.
     *
     * Hàm hỗ trợ story của tôi, story đã thích, story mới và search story theo caption/tác giả/địa điểm.
     *
     * @param key Câu hỏi đã normalize.
     * @param uid UID người hiện tại.
     * @return Câu trả lời về story.
     */
    private suspend fun answerStories(
        key: String,
        uid: String,
    ): String {
        val wantsOwnStories = key.hasAny("cua toi", "toi dang", "minh dang", "tin toi", "tin cua minh")
        val wantsLikedStories = key.hasAny("da thich", "liked", "thich", "tha tim")

        if ((wantsOwnStories || wantsLikedStories) && uid.isBlank()) {
            return "Ban can dang nhap de minh doc danh sach tin cua ban."
        }

        val allStories = feedRepository.getActiveStories().first()
        val term = key.intentKeyWithout(
            "tin",
            "story",
            "stories",
            "feed",
            "tim",
            "kiem",
            "ve",
            "co",
            "cua",
            "toi",
            "minh",
            "hien",
            "tai",
            "moi",
            "nhat",
            "gan",
            "day",
            "xem",
            "cho",
            "hoi",
            "da",
            "dang",
            "thich",
            "tha",
            "tim",
            "24h",
            "24",
            "h",
        )

        val stories =
            when {
                wantsOwnStories ->
                    allStories.filter { it.authorUid == uid }
                wantsLikedStories ->
                    allStories.filter { uid in it.likedBy }
                term.isBlank() ->
                    allStories
                else ->
                    allStories.filterByStoryTerm(term)
            }.take(6)

        if (stories.isEmpty()) {
            return if (term.isBlank()) {
                "Minh chua thay tin nao dang hoat dong."
            } else {
                "Minh chua thay tin nao khop \"$term\". Ban thu hoi theo caption, dia diem hoac ten nguoi dang nhe."
            }
        }

        val title =
            when {
                wantsOwnStories -> "Tin dang hoat dong cua ban"
                wantsLikedStories -> "Tin ban da thich"
                term.isBlank() -> "Tin moi dang hoat dong"
                else -> "Tin khop \"$term\""
            }
        return formatStoryAnswer(title, stories, uid)
    }

    /**
     * Format danh sách story thành câu trả lời nhiều dòng.
     *
     * @param title Tiêu đề nhóm kết quả.
     * @param stories Danh sách story cần hiển thị.
     * @param uid UID người hiện tại để đổi tên tác giả thành "Bạn" nếu phù hợp.
     * @return Chuỗi phản hồi đã format.
     */
    private fun formatStoryAnswer(
        title: String,
        stories: List<Story>,
        uid: String,
    ): String {
        val lines = stories.joinToString("\n") { story ->
            val owner = if (story.authorUid == uid) "Ban" else story.authorName.ifBlank { "Nguoi dung" }
            val place = story.location?.takeIf { it.isNotBlank() }?.let { " tai $it" }.orEmpty()
            val expires = story.expiresAt?.time?.let { expiresAt ->
                val remainingMillis = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
                val remainingHours = remainingMillis / (60L * 60L * 1000L)
                if (remainingHours == 0L) ", con duoi 1h" else ", con ${remainingHours}h"
            }.orEmpty()
            "- $owner$place: ${story.caption.previewText()} (${story.likesCount} tim$expires)"
        }
        return "$title:\n$lines"
    }

    /**
     * Trả lời câu hỏi về bạn bè, người đang theo dõi hoặc tìm người dùng.
     *
     * @param question Câu hỏi gốc để lấy search term có dấu nếu cần.
     * @param key Câu hỏi đã normalize.
     * @param uid UID người hiện tại.
     * @return Câu trả lời về quan hệ follow/friend hoặc kết quả profile search.
     */
    private suspend fun answerPeople(
        question: String,
        key: String,
        uid: String,
    ): String {
        if (key.hasAny("ban be", "friend")) {
            if (uid.isBlank()) return "Bạn cần đăng nhập để xem danh sách bạn bè."
            val following = profileRepository.getFollowing(uid).getOrDefault(emptyList())
            val followers = profileRepository.getFollowers(uid).getOrDefault(emptyList())
            val followerIds = followers.map { it.uid }.toSet()
            val friends = following.filter { it.uid in followerIds }
            if (friends.isEmpty()) {
                return "Bạn chưa có bạn bè hai chiều. Đang theo dõi ${following.size} người và có ${followers.size} người theo dõi."
            }
            return "Bạn có ${friends.size} bạn bè hai chiều:\n" +
                friends.take(8).joinToString("\n") { profile ->
                    "- ${profile.displayName.ifBlank { profile.username }} (@${profile.username.ifBlank { profile.uid.take(6) }})"
                }
        }

        if (key.hasAny("dang theo doi", "following", "theo doi")) {
            if (uid.isBlank()) return "Bạn cần đăng nhập để xem danh sách đang theo dõi."
            val following = profileRepository.getFollowing(uid).getOrDefault(emptyList())
            if (following.isEmpty()) return "Bạn chưa theo dõi ai."
            return "Bạn đang theo dõi ${following.size} người:\n" +
                following.take(8).joinToString("\n") { profile ->
                    "- ${profile.displayName.ifBlank { profile.username }}"
                }
        }

        val term = question.intentTerm("tìm", "kiếm", "người", "user", "profile", "bạn")
        val results = profileRepository.searchProfiles(term, limit = 8).getOrDefault(emptyList())
            .filter { profile -> profile.uid != uid }

        if (results.isEmpty()) return "Mình chưa tìm thấy người dùng phù hợp với \"$question\"."

        return "Người dùng phù hợp:\n" +
            results.joinToString("\n") { profile ->
                val relation =
                    when {
                        uid.isNotBlank() && uid in profile.followerIds && uid in profile.followingIds -> "bạn bè"
                        uid.isNotBlank() && uid in profile.followerIds -> "bạn đang theo dõi"
                        uid.isNotBlank() && uid in profile.followingIds -> "theo dõi bạn"
                        else -> "${profile.postsCount} bài viết"
                    }
                "- ${profile.displayName.ifBlank { profile.username }} (@${profile.username.ifBlank { profile.uid.take(6) }}) - $relation"
            }
    }

    /**
     * Trả lời câu hỏi về địa điểm/quán ăn.
     *
     * Nguồn dữ liệu chính là location trong các bài đăng feed; nếu thiếu dữ liệu thì trộn thêm
     * danh sách gợi ý tĩnh để Assistant vẫn có phản hồi hữu ích.
     *
     * @param question Câu hỏi gốc.
     * @param key Câu hỏi đã normalize.
     * @return Câu trả lời về tối đa 6 địa điểm phù hợp.
     */
    private suspend fun answerPlaces(
        question: String,
        key: String,
    ): String {
        val term = question.intentTerm("địa điểm", "dia diem", "nổi bật", "noi bat", "quán", "quan", "nhà hàng", "nha hang", "tìm", "kiếm")
        val posts = feedRepository.getFeedPostsBatch(100, null)
        val placesFromPosts =
            posts
                .mapNotNull { post ->
                    val location = post.location?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    location to post
                }
                .groupBy { (location, _) -> location.toSearchKey() }
                .values
                .map { rows ->
                    val location = rows.first().first
                    PlaceAnswer(
                        name = location,
                        category = "Có ${rows.size} bài viết trong DineSplit",
                        count = rows.size,
                        imageHint = rows.firstNotNullOfOrNull { it.second.imageUrls.firstOrNull() }.orEmpty(),
                    )
                }

        val fallback = suggestedPlaces()
        val allPlaces = (placesFromPosts + fallback)
            .distinctBy { it.name.toSearchKey() }
            .let { places ->
                if (term.isBlank() || key.hasAny("noi bat", "goi y", "hot", "trending")) {
                    places.sortedByDescending { it.count }
                } else {
                    places.filter {
                        it.name.toSearchKey().contains(term.toSearchKey()) ||
                            it.category.toSearchKey().contains(term.toSearchKey())
                    }.sortedByDescending { it.count }
                }
            }
            .take(6)

        if (allPlaces.isEmpty()) return "Mình chưa tìm thấy địa điểm phù hợp với \"$question\"."

        return "Địa điểm phù hợp:\n" +
            allPlaces.joinToString("\n") { place ->
                "- ${place.name} - ${place.category}"
            }
    }

    /**
     * Trả lời câu hỏi về các nhóm chia tiền hiện tại của người dùng.
     *
     * Với mỗi nhóm, hàm tải bills, tính số tiền người dùng cần trả/được nợ và số bill chưa settled.
     *
     * @param uid UID người hiện tại.
     * @param key Câu hỏi đã normalize, dùng để lọc theo tên nhóm nếu có term.
     * @return Tóm tắt nhóm hoặc thông báo không có nhóm phù hợp.
     */
    private suspend fun answerGroups(
        uid: String,
        key: String,
    ): String {
        if (uid.isBlank()) return "Bạn cần đăng nhập để mình đọc các nhóm hiện tại."

        val groups = splitRepository.getGroups().first()
        if (groups.isEmpty()) {
            return "Bạn chưa tham gia nhóm nào. Khi tạo hoặc được thêm vào nhóm chia tiền, mình sẽ tóm tắt nhóm ở đây."
        }

        val term = key.intentKeyWithout(
            "nhom",
            "group",
            "hien",
            "tai",
            "cua",
            "toi",
            "minh",
            "dang",
            "co",
            "xem",
            "tom",
            "tat",
            "hoi",
        )

        val visibleGroups =
            if (term.isBlank()) {
                groups
            } else {
                groups.filter { group -> group.name.toSearchKey().contains(term) }
            }.take(6)

        if (visibleGroups.isEmpty()) {
            return "Mình chưa thấy nhóm nào khớp \"$term\" trong danh sách nhóm hiện tại của bạn."
        }

        val groupLines =
            visibleGroups.map { group ->
                val bills = runCatching { splitRepository.getBills(group.id).first() }.getOrDefault(emptyList())
                val summary = SplitCalculationEngine.calculateUserBalance(bills, uid)
                val memberCount = group.memberIds.size.takeIf { it > 0 } ?: group.memberCount
                val openBills = bills.count { bill -> bill.status.name != "SETTLED" }
                "- ${group.name}: $memberCount thành viên, ${bills.size} bill ($openBills chưa xong), bạn cần trả ${formatMoney(summary.amountYouOwe)}, người khác còn nợ bạn ${formatMoney(summary.amountYouAreOwed)}"
            }
        val lines = groupLines.joinToString("\n")

        return "Nhóm hiện tại của bạn:\n$lines"
    }

    /**
     * Trả lời câu hỏi về split bill và các khoản nợ.
     *
     * Hàm tính tổng balance bằng [SplitCalculationEngine], sau đó tách hai hướng:
     * người dùng còn nợ người khác và người khác còn nợ người dùng.
     *
     * @param uid UID người hiện tại.
     * @param key Câu hỏi đã normalize để phân biệt "tôi nợ ai" và "ai nợ tôi".
     * @return Tóm tắt split hoặc danh sách khoản cần trả/được nhận.
     */
    private suspend fun answerSplit(
        uid: String,
        key: String,
    ): String {
        if (uid.isBlank()) return "Bạn cần đăng nhập để xem thông tin bill và khoản nợ."

        val billContexts = loadBillContexts()
        val bills = billContexts.map { it.bill }
        if (bills.isEmpty()) return "Bạn chưa có bill nào trong các nhóm hiện tại."

        val summary = SplitCalculationEngine.calculateUserBalance(bills, uid)
        val owes = billContexts.filter { context ->
            context.bill.payerId != uid &&
                uid !in context.bill.paidMemberIds &&
                (context.bill.shares[uid] ?: 0.0) > 0.0
        }
        val owedToMe = billContexts.flatMap { context ->
            if (context.bill.payerId != uid) {
                emptyList()
            } else {
                context.bill.shares
                    .filter { (memberId, amount) ->
                        memberId != uid && memberId !in context.bill.paidMemberIds && amount > 0.0
                    }
                    .map { (memberId, amount) -> context to (memberId to amount) }
            }
        }

        return when {
            key.hasAny("toi no", "minh no", "can tra", "phai tra") -> {
                if (owes.isEmpty()) {
                    "Bạn không còn khoản split nào cần trả."
                } else {
                    "Bạn đang cần trả ${formatMoney(summary.amountYouOwe)}:\n" +
                        owes.take(8).joinToString("\n") { context ->
                            val bill = context.bill
                            "- ${bill.name} (${context.groupName}): ${formatMoney(bill.shares[uid] ?: 0.0)} cho ${context.nameOf(bill.payerId)}"
                        }
                }
            }
            key.hasAny("ai no toi", "no toi", "nguoi no", "chua tra cho toi") -> {
                if (owedToMe.isEmpty()) {
                    "Hiện không có ai đang nợ bạn trong các bill mở."
                } else {
                    "Bạn đang được nợ ${formatMoney(summary.amountYouAreOwed)}:\n" +
                        owedToMe.take(10).joinToString("\n") { (context, debtor) ->
                            val (memberId, amount) = debtor
                            "- ${context.nameOf(memberId)} nợ ${formatMoney(amount)} trong ${context.bill.name} (${context.groupName})"
                        }
                }
            }
            else -> {
                val openBills = bills.count { bill -> bill.status.name == "OPEN" }
                listOf(
                    "Tóm tắt Split:",
                    "- Bạn cần trả: ${formatMoney(summary.amountYouOwe)}",
                    "- Người khác còn nợ bạn: ${formatMoney(summary.amountYouAreOwed)}",
                    "- Số bill còn mở: $openBills/${bills.size}",
                    if (owes.isNotEmpty()) "Khoản gần nhất bạn cần trả: ${owes.first().bill.name} - ${formatMoney(owes.first().bill.shares[uid] ?: 0.0)}" else "Bạn không có khoản cần trả ngay.",
                ).joinToString("\n")
            }
        }
    }

    /**
     * Trả lời câu hỏi về tài chính cá nhân: giao dịch, ví, goal, reminder và recurring rule.
     *
     * Nếu intent nhắm vào một nhóm cụ thể, hàm trả lời nhóm đó. Nếu không, hàm tổng hợp thu/chi
     * trong tháng hiện tại và các category chi tiêu lớn nhất.
     *
     * @param uid UID người hiện tại.
     * @param key Câu hỏi đã normalize.
     * @return Câu trả lời tài chính cá nhân.
     */
    private suspend fun answerPersonal(
        uid: String,
        key: String,
    ): String {
        if (uid.isBlank()) return "Bạn cần đăng nhập để xem Ví, goal và reminder."

        val transactions = personalRepository.getAllTransactions()
        val wallets = personalRepository.getWallets()
        val goals = personalRepository.getGoals()
        val reminders = personalRepository.getSpendingReminders()
        val recurringRules = personalRepository.getRecurringRules()

        if (key.hasAny("goal", "muc tieu", "mục tiêu")) {
            if (goals.isEmpty()) return "Bạn chưa tạo goal nào."
            return "Goal của bạn:\n" +
                goals.take(8).joinToString("\n") { goal ->
                    "- ${goal.title}: ${formatMoney(goal.currentAmount)}/${formatMoney(goal.targetAmount)} (${goal.progressPercent()}%, ${goal.status.toVietnamese()})"
                }
        }

        if (key.hasAny("reminder", "nhac", "nhắc", "canh bao", "cảnh báo")) {
            if (reminders.isEmpty()) return "Bạn chưa tạo reminder chi tiêu nào."
            return "Reminder đang có:\n" +
                reminders.take(8).joinToString("\n") { reminder ->
                    "- ${reminder.categoryName}: ngưỡng ${formatMoney(reminder.budgetAmount)} (${reminder.reminderType.toVietnamese()}, ${if (reminder.isEnabled) "đang bật" else "đã tắt"})"
                }
        }

        if (key.hasAny("wallet", "so du", "số dư", "vi", "ví")) {
            if (wallets.isEmpty()) return "Bạn chưa tạo ví/tài khoản nào."
            return "Số dư ví:\n" +
                wallets.take(8).joinToString("\n") { wallet ->
                    "- ${wallet.name}: ${formatMoney(wallet.balance)} (${wallet.type.name})"
                }
        }

        if (key.hasAny("dinh ky", "định kỳ", "recurring", "lap lai", "lặp lại")) {
            if (recurringRules.isEmpty()) return "Bạn chưa có giao dịch định kỳ."
            return "Giao dịch định kỳ:\n" +
                recurringRules.take(8).joinToString("\n") { rule ->
                    "- ${rule.name}: ${formatMoney(rule.amount)} ${if (rule.type == TransactionType.EXPENSE) "chi" else "thu"} ${rule.cadence.name.lowercase()}"
                }
        }

        val monthTransactions = transactions.filterCurrentMonth()
        val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val topCategories =
            monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, rows) -> rows.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
                .take(3)

        return buildString {
            appendLine("Tóm tắt Ví tháng này:")
            appendLine("- Thu nhập: ${formatMoney(income)}")
            appendLine("- Chi tiêu: ${formatMoney(expense)}")
            appendLine("- Chênh lệch: ${formatMoney(income - expense)}")
            if (topCategories.isNotEmpty()) {
                appendLine("Nhóm chi nhiều nhất:")
                topCategories.forEach { entry ->
                    appendLine("- ${entry.key}: ${formatMoney(entry.value)}")
                }
            }
            append("- Goal: ${goals.count { it.status == GoalStatus.ACTIVE }} đang hoạt động, reminder: ${reminders.count { it.isEnabled }} đang bật.")
        }
    }

    /**
     * Trả lời câu hỏi về profile của người dùng hiện tại.
     *
     * @param uid UID người hiện tại.
     * @return Tóm tắt tên, username, số bài viết, follow và bio.
     */
    private suspend fun answerProfile(uid: String): String {
        if (uid.isBlank()) return "Bạn cần đăng nhập để xem profile."
        val profile = profileRepository.getProfile(uid) ?: return "Mình chưa tìm thấy profile của bạn."
        return listOf(
            "Profile của bạn:",
            "- Tên: ${profile.displayName.ifBlank { "Chưa đặt" }}",
            "- Username: @${profile.username.ifBlank { "chưa đặt" }}",
            "- Bài viết: ${profile.postsCount}",
            "- Theo dõi: ${profile.followingCount}, người theo dõi: ${profile.followersCount}",
            "- Bio: ${profile.bio.ifBlank { "Chưa có bio" }}",
        ).joinToString("\n")
    }

    /**
     * Fallback khi Assistant chưa xác định chắc intent.
     *
     * Hàm thử tìm theo nhiều miền dữ liệu: post, story, people và places rồi trả về một cụm
     * kết quả liên quan.
     *
     * @param question Câu hỏi gốc.
     * @param uid UID người hiện tại.
     * @return Câu trả lời tổng hợp.
     */
    private suspend fun answerGeneralSearch(
        question: String,
        uid: String,
    ): String {
        val posts = feedRepository.searchPosts(question).take(3)
        val stories = feedRepository.getActiveStories().first().filterByStoryTerm(question).take(3)
        val people = profileRepository.searchProfiles(question, limit = 3).getOrDefault(emptyList())
            .filter { it.uid != uid }
        val places = answerPlaces(question, question.toSearchKey())

        return buildString {
            appendLine("Mình chưa chắc bạn muốn hỏi nhóm nào, nhưng có vài kết quả liên quan:")
            if (posts.isNotEmpty()) {
                appendLine("Bài đăng:")
                posts.forEach { appendLine("- ${it.authorName}: ${it.caption.previewText()}") }
            }
            if (stories.isNotEmpty()) {
                appendLine("Tin/story:")
                stories.forEach { appendLine("- ${it.authorName}: ${it.caption.previewText()}") }
            }
            if (people.isNotEmpty()) {
                appendLine("Người dùng:")
                people.forEach { profile ->
                    appendLine("- ${profile.displayName.ifBlank { profile.username }} (@${profile.username})")
                }
            }
            append(places)
        }
    }

    /**
     * Tải toàn bộ bill hiện có kèm tên nhóm và tên thành viên để trả lời split.
     *
     * @return Danh sách [BillContext] từ tất cả nhóm người dùng tham gia.
     */
    private suspend fun loadBillContexts(): List<BillContext> {
        val groups = splitRepository.getGroups().first()
        return groups.flatMap { group ->
            val bills = splitRepository.getBills(group.id).first()
            val memberNames =
                splitRepository.getGroupMembers(group.id)
                    .first()
                    .associate { member -> member.id to member.name }
            bills.map { bill ->
                BillContext(
                    groupName = group.name,
                    bill = bill,
                    memberNames = memberNames,
                )
            }
        }
    }

    /**
     * Lấy tên hiển thị của một thành viên trong context bill.
     *
     * @param memberId ID thành viên.
     * @return Tên hiển thị hoặc 8 ký tự đầu của ID nếu thiếu tên.
     */
    private fun BillContext.nameOf(memberId: String): String {
        return memberNames[memberId]?.takeIf { it.isNotBlank() } ?: memberId.take(8)
    }

    /**
     * Danh sách gợi ý địa điểm fallback khi dữ liệu feed chưa đủ.
     *
     * @return Danh sách [PlaceAnswer] tĩnh.
     */
    private fun suggestedPlaces(): List<PlaceAnswer> {
        return listOf(
            PlaceAnswer("Pizza 4P's Tràng Tiền", "Pizza & Italian", 0),
            PlaceAnswer("Phở Thìn Lò Đúc", "Món Việt truyền thống", 0),
            PlaceAnswer("Bún Chả Hương Liên", "Bún chả Hà Nội", 0),
            PlaceAnswer("Urban Greens", "Healthy bowls", 0),
            PlaceAnswer("The Rustic Spoons", "Artisanal Italian", 0),
        )
    }

    /**
     * Factory tạo [AssistantViewModel] với [Application] từ Compose.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AssistantViewModel(application) as T
        }
    }
}

/**
 * Entry point UI của màn hình Assistant.
 *
 * Composable này dựng layout hội thoại, quick prompt và input bar; toàn bộ xử lý câu hỏi nằm trong
 * [AssistantViewModel].
 *
 * @param onBack Callback điều hướng quay lại.
 */
@Composable
fun AssistantScreen(onBack: () -> Unit = {}) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AssistantViewModel = viewModel(factory = AssistantViewModel.Factory(application))
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(
        title = "Assistant",
        navigationIcon = { BackNavigationButton(onClick = onBack) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding(),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentPadding = PaddingValues(AppDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    AssistantBubble(message = message)
                }
                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Row(
                                    modifier = Modifier.padding(AppDimens.spaceMd),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                                    Text("Đang đọc dữ liệu DineSplit...")
                                }
                            }
                        }
                    }
                }
            }

            AssistantQuickPrompts(
                enabled = !uiState.isLoading,
                onPromptClick = viewModel::submit,
            )

            AssistantInputBar(
                input = uiState.input,
                enabled = !uiState.isLoading,
                onInputChange = viewModel::onInputChange,
                onSubmit = { viewModel.submit() },
            )
        }
    }
}

/**
 * Bubble hiển thị một message người dùng hoặc Assistant.
 *
 * @param message Message cần render.
 */
@Composable
private fun AssistantBubble(message: AssistantMessage) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.fromUser) 18.dp else 4.dp,
                bottomEnd = if (message.fromUser) 4.dp else 18.dp,
            ),
            color = if (message.fromUser) colorScheme.primary else colorScheme.surfaceVariant,
            contentColor = if (message.fromUser) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(AppDimens.spaceMd),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Danh sách prompt nhanh để người dùng gửi một câu hỏi mẫu.
 *
 * @param enabled true nếu Assistant không đang xử lý câu trước đó.
 * @param onPromptClick Callback gửi prompt được chọn.
 */
@Composable
private fun AssistantQuickPrompts(
    enabled: Boolean,
    onPromptClick: (String) -> Unit,
) {
    val prompts =
        listOf(
            "Ai nợ tôi?",
            "Chi tiêu tháng này",
            "Địa điểm nổi bật",
            "Bài đã lưu",
            "Tin moi",
            "Goal của tôi",
            "Bạn bè của tôi",
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        prompts.forEach { prompt ->
            Card(
                modifier =
                    Modifier.clickable(enabled = enabled) {
                        onPromptClick(prompt)
                    },
                shape = RoundedCornerShape(100.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceXs))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Thanh nhập câu hỏi ở cuối màn hình Assistant.
 *
 * @param input Nội dung đang nhập.
 * @param enabled true nếu cho phép nhập/gửi.
 * @param onInputChange Callback khi text thay đổi.
 * @param onSubmit Callback gửi câu hỏi.
 */
@Composable
private fun AssistantInputBar(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hỏi về bill, bài đăng, bạn bè, ví...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            )
            IconButton(
                onClick = onSubmit,
                enabled = enabled && input.isNotBlank(),
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi",
                    tint = Color.White,
                )
            }
        }
    }
}

/**
 * Nhận diện câu hỏi thuộc miền split/bill/debt.
 *
 * @param tokens Danh sách token đã normalize.
 * @return true nếu câu hỏi nói về hóa đơn, thanh toán hoặc nợ.
 */
private fun String.isSplitQuestion(tokens: List<String>): Boolean {
    return hasAny("bill", "hoa don", "split", "thanh toan", "qr", "can tra", "phai tra", "ai no", "no toi") ||
        "no" in tokens ||
        "tra" in tokens
}

/**
 * Nhận diện câu hỏi về nhóm chia tiền.
 *
 * @return true nếu chuỗi chứa keyword nhóm/group.
 */
private fun String.isGroupQuestion(): Boolean {
    return hasAny("nhom", "group", "nhom hien tai", "nhom cua toi", "group hien tai", "cac nhom")
}

/**
 * Nhận diện câu hỏi về tài chính cá nhân.
 *
 * @return true nếu chuỗi chứa keyword ví, chi tiêu, goal, reminder hoặc recurring.
 */
private fun String.isPersonalQuestion(): Boolean {
    return hasAny(
        "vi",
        "wallet",
        "chi tieu",
        "thu nhap",
        "giao dich",
        "goal",
        "muc tieu",
        "reminder",
        "nhac",
        "ngan sach",
        "dinh ky",
        "so du",
    )
}

/**
 * Nhận diện câu hỏi về địa điểm/quán ăn.
 *
 * @return true nếu chuỗi chứa keyword place/restaurant.
 */
private fun String.isPlaceQuestion(): Boolean {
    return hasAny("dia diem", "noi bat", "quan", "nha hang", "cafe", "restaurant", "place", "an o dau")
}

/**
 * Nhận diện câu hỏi về story mà không nhầm với cụm "thông tin".
 *
 * @param tokens Danh sách token đã normalize.
 * @return true nếu câu hỏi nhắm vào story/tin.
 */
private fun String.isStoryQuestion(tokens: List<String>): Boolean {
    if (hasAny("story", "stories")) return true
    if ("tin" !in tokens) return false
    if (hasAny("thong tin")) return false

    return tokens.size == 1 ||
        hasAny(
            "cac tin",
            "tin moi",
            "tin gan day",
            "tin hien tai",
            "tin dang",
            "tin cua",
            "tin ve",
            "tin toi",
            "tin minh",
            "tin 24h",
            "tin 24 h",
            "xem tin",
            "tim tin",
            "kiem tin",
            "hoi ve tin",
            "ve tin",
            "da dang tin",
            "dang tin",
            "thich tin",
            "tha tim tin",
            "tin khong phai bai viet",
        )
}

/**
 * Nhận diện câu hỏi về bài đăng/feed.
 *
 * @return true nếu chuỗi chứa keyword post/caption/saved.
 */
private fun String.isPostQuestion(): Boolean {
    return hasAny("bai dang", "bai viet", "post", "caption", "bai luu", "da luu", "saved")
}

/**
 * Nhận diện câu hỏi về bạn bè/người dùng/follow.
 *
 * @return true nếu chuỗi chứa keyword people/user/follow.
 */
private fun String.isPeopleQuestion(): Boolean {
    return hasAny("ban be", "nguoi dung", "tim nguoi", "user", "follow", "theo doi", "followers", "following")
}

/**
 * Kiểm tra chuỗi đã normalize có chứa bất kỳ keyword nào không.
 *
 * @param needles Danh sách keyword có thể có dấu hoặc không dấu.
 * @return true nếu có ít nhất một keyword xuất hiện.
 */
private fun String.hasAny(vararg needles: String): Boolean {
    return needles.any { contains(it.toSearchKey()) }
}

/**
 * Tách search key thành token theo khoảng trắng.
 *
 * @return Danh sách token khác rỗng.
 */
private fun String.tokens(): List<String> {
    return split(" ").filter { it.isNotBlank() }
}

/**
 * Loại bỏ các stop word khỏi search key để lấy term intent còn lại.
 *
 * @param stopWords Các từ/cụm từ cần bỏ qua.
 * @return Search key đã loại stop word.
 */
private fun String.intentKeyWithout(vararg stopWords: String): String {
    val stops = stopWords.flatMap { it.toSearchKey().tokens() }.toSet()
    return tokens()
        .filterNot { token -> token in stops }
        .joinToString(" ")
        .trim()
}

/**
 * Normalize text để search không phân biệt hoa/thường, dấu tiếng Việt và ký tự đặc biệt.
 *
 * @return Chuỗi lowercase, không dấu, chỉ còn chữ/số/khoảng trắng đơn.
 */
private fun String.toSearchKey(): String {
    val decomposed = Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
    return decomposed
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .replace('đ', 'd')
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

/**
 * Lấy cụm từ tìm kiếm từ câu gốc bằng cách bỏ stop word nhưng giữ lại chữ có dấu nếu có.
 *
 * @param stopWords Các từ/cụm từ cần bỏ qua.
 * @return Term còn lại để truyền vào repository search.
 */
private fun String.intentTerm(vararg stopWords: String): String {
    val normalizedStops = stopWords.map { it.toSearchKey() }.toSet()
    val words = trim().split(Regex("\\s+"))
    return words
        .filterNot { word -> word.toSearchKey() in normalizedStops }
        .joinToString(" ")
        .trim()
}

/**
 * Lọc bài đăng theo caption, tên tác giả hoặc location.
 *
 * @param term Cụm từ tìm kiếm.
 * @return Danh sách [Post] khớp term.
 */
private fun List<Post>.filterByPostTerm(term: String): List<Post> {
    val key = term.toSearchKey()
    return filter { post ->
        post.caption.toSearchKey().contains(key) ||
            post.authorName.toSearchKey().contains(key) ||
            post.location.orEmpty().toSearchKey().contains(key)
    }
}

/**
 * Lọc story theo caption, tên tác giả hoặc location.
 *
 * @param term Cụm từ tìm kiếm.
 * @return Danh sách [Story] khớp term.
 */
private fun List<Story>.filterByStoryTerm(term: String): List<Story> {
    val key = term.toSearchKey()
    if (key.isBlank()) return this
    return filter { story ->
        story.caption.toSearchKey().contains(key) ||
            story.authorName.toSearchKey().contains(key) ||
            story.location.orEmpty().toSearchKey().contains(key)
    }
}

/**
 * Rút gọn text để hiển thị trong câu trả lời Assistant.
 *
 * @param maxLength Độ dài tối đa.
 * @return Text một dòng, có dấu `...` nếu bị cắt.
 */
private fun String.previewText(maxLength: Int = 72): String {
    val clean = replace("\n", " ").trim()
    return when {
        clean.isBlank() -> "Không có caption"
        clean.length <= maxLength -> clean
        else -> clean.take(maxLength).trimEnd() + "..."
    }
}

/**
 * Lọc giao dịch thuộc tháng hiện tại theo thời gian thiết bị.
 *
 * @return Danh sách giao dịch trong cùng năm/tháng hiện tại.
 */
private fun List<Transaction>.filterCurrentMonth(): List<Transaction> {
    val now = Calendar.getInstance()
    return filter { transaction ->
        val calendar = Calendar.getInstance().apply { timeInMillis = transaction.date }
        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
}

/**
 * Format số tiền theo locale Việt Nam.
 *
 * @param amount Số tiền cần format.
 * @return Chuỗi dạng `x VND`.
 */
private fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.roundToLong())} VND"
}

/**
 * Chuyển [GoalStatus] sang nhãn tiếng Việt.
 *
 * @return Nhãn trạng thái goal.
 */
private fun GoalStatus.toVietnamese(): String {
    return when (this) {
        GoalStatus.ACTIVE -> "đang chạy"
        GoalStatus.COMPLETED -> "hoàn thành"
        GoalStatus.PAUSED -> "tạm dừng"
    }
}

/**
 * Chuyển [ReminderType] sang nhãn tiếng Việt.
 *
 * @return Nhãn loại reminder.
 */
private fun ReminderType.toVietnamese(): String {
    return when (this) {
        ReminderType.DAILY -> "hằng ngày"
        ReminderType.WEEKLY -> "hằng tuần"
        ReminderType.MONTHLY -> "hằng tháng"
        ReminderType.MILESTONE -> "mốc chi tiêu"
    }
}

/**
 * Tính phần trăm hoàn thành mục tiêu tiết kiệm.
 *
 * @return Phần trăm nguyên, giới hạn từ 0 đến 999.
 */
private fun com.example.dinesplit.domain.model.PersonalGoal.progressPercent(): Int {
    if (targetAmount <= 0.0) return 0
    return ((currentAmount / targetAmount) * 100).roundToInt().coerceIn(0, 999)
}

/**
 * Format epoch millis thành ngày `dd/MM/yyyy`.
 *
 * @param epochMillis Thời điểm cần format.
 * @return Chuỗi ngày hoặc thông báo chưa đặt nếu giá trị không hợp lệ.
 */
@Suppress("unused")
private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Chưa đặt"
    return SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(epochMillis)
}
