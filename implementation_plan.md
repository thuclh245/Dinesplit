# Implementation Plan: Documenting and Explaining Personal, Notification, Chat, and Chatbot Modules

This plan details how we will review, comment, and explain the functions and architecture of the Personal Finance, Notification, Chat, and Chatbot (Assistant) modules in the DineSplit application.

## User Review Required

> [!IMPORTANT]
> **Scope of Comments**: There are approximately 38 files across these four modules. 
> To ensure the work is manageable, high-quality, and does not hit model context or API invocation limits, we propose:
> 1. **Core Business Logic (Primary Focus)**: Add detailed KDoc comments to all functions in the **Repositories**, **ViewModels**, **Models**, and **Core Clients** (e.g., `WebRtcVoiceCallClient`, `NotificationTriggerIntegration`).
> 2. **UI Screens (Secondary Focus/Structural Summary)**: For Composable files (e.g., `PersonalScreen.kt`, `ChatScreens.kt`, `AssistantScreen.kt`), we will document the main screen entry point Composables and explain their overall component structure in the final documentation instead of commenting on every small helper sub-Composable (which are mostly standard layout/styling boilerplate).
> 
> Please let me know if you would prefer us to comment on *every single* sub-composable layout helper as well.

---

## Open Questions

> [!NOTE]
> 1. **Comment Language**: The code comments will be written in Vietnamese (Tiếng Việt) as requested. Do you have any preferences on format (e.g. standard Kotlin KDoc with `@param`, `@return`, `@throws` tags)?
> 2. **Chatbot (Assistant) Details**: The chatbot is implemented using keyword matching and repository data queries inside `AssistantViewModel` in `AssistantScreen.kt`. We will document how the search engine/token processing logic works. Are there any other hidden chatbot integrations (like OpenAI or Gemini APIs) you want us to look for?

---

## Proposed Changes

We will group the modifications by module. For each file, we will add detailed, professional, and clear documentation (KDoc/code comments) for every function.

### 📊 1. Personal Finance Module
This module handles personal transactions, wallets, goals, recurring rules, category management, and financial insights (charts, safe to spend, anomaly detection).

#### [MODIFY] [PersonalRepository.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/domain/repository/PersonalRepository.kt)
- Interface for Personal Finance operations.

#### [MODIFY] [FirebasePersonalRepository.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/data/repository/FirebasePersonalRepository.kt)
- Firestore implementation. We will document CRUD methods for Transactions, Categories, Reminders, Goals, Wallets, and recurring rules.

#### [MODIFY] [PersonalViewModel.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/presentation/personal/PersonalViewModel.kt)
- Handles UI state calculation, chart data structure creation (slices, daily bars), budget threshold checking, and recurring rule triggers.

#### [MODIFY] [PersonalPresentationMappers.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/presentation/personal/PersonalPresentationMappers.kt)
- Mapping extension functions for chart states.

---

### 🔔 2. Notification Module
This module manages FCM setup, local/remote notification saving, and triggering notifications based on application events.

#### [MODIFY] [NotificationRepository.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/domain/repository/NotificationRepository.kt)
- Interface for notification observations and read-state updates.

#### [MODIFY] [FirebaseNotificationRepository.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/data/repository/FirebaseNotificationRepository.kt)
- Firebase/Firestore database management for user notifications.

#### [MODIFY] [NotificationViewModel.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/presentation/notification/NotificationViewModel.kt)
- ViewModel that coordinates fetching, marking read/unread, and UI states.

#### [MODIFY] [NotificationTriggerIntegration.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/presentation/notification/NotificationTriggerIntegration.kt)
- Integration point that links system triggers (split bills, transaction alerts) to notification generation.

---

### 💬 3. Chat and Voice Call Module
Manages one-on-one text messages, media/sticker exchanges, and WebRTC-based voice calls.

#### [MODIFY] [ChatRepository.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/domain/repository/ChatRepository.kt)
- Interface for message exchanges, typing status, and WebRTC signaling (call status, mute status).

#### [MODIFY] [FirebaseChatRepository.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/data/repository/FirebaseChatRepository.kt)
- Database persistence for messages, channels/threads, and signaling data.

#### [MODIFY] [ChatViewModels.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/presentation/chat/ChatViewModels.kt)
- `ChatListViewModel` (lists active threads), `ChatDetailViewModel` (handles messages/reactions/typing/signaling setup), and `ChatCallViewModel` (handles accept/decline/mute audio calls).

#### [MODIFY] [WebRtcVoiceCallClient.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/presentation/chat/WebRtcVoiceCallClient.kt)
- Native WebRTC client logic for audio streams.

---

### 🤖 4. Chatbot (Assistant) Module
Handles user queries about application entities (posts, bills, debts, personal budgets).

#### [MODIFY] [AssistantScreen.kt](file:///d:/projects/Dinesplit/app/src/main/java/com/example/dinesplit/presentation/assistant/AssistantScreen.kt)
- Document the `AssistantViewModel` class nested inside this file and its main processing methods (`submit`, `answer`, `helpAnswer`, and all the intent parser functions like `answerGroups`, `answerSplit`, etc.).

---

## Verification Plan

### Automated Tests
- Build check: Compile the project using Gradle (`./gradlew assembleDebug` or equivalent) to ensure no syntax errors were introduced during comment editing.
- We will monitor output errors using the `build_check.log` if needed.

### Manual Verification
- We will present a highly detailed architectural walkthrough explaining the data models and execution flow diagrams for the user to verify.
