Markdown
# 🚀 SinTrans Keyboard - AI-Powered Multilingual Android IME

**SinTrans Keyboard** is a custom, feature-packed Android Input Method (IME) and Notification Translation service designed to break language barriers effortlessly. Built with Kotlin, it bridges communication gaps during university collaborations, group projects, and daily multilingual chats.

---

## 🌟 Key Features

1. **Helakuru-Standard Singlish Engine:**
   - Ultra-smooth, natural phonetic typing with advanced longest-prefix matching for consonants, vowels, and diacritics.
2. **Real-Time Sentence-Level Multi-Language Translation:**
   - Automatically translates typed Sinhala sentences into English (`en`) or Tamil (`ta`) instantly when pressing Space or Enter using the Google Translation API.
3. **Direct English Mode:**
   - Instantly toggle between translation mode and clean, direct English typing using the dedicated Globe key (`btn_globe`).
4. **Multi-App Real-Time Notification Translation:**
   - Intercepts incoming messages from **WhatsApp, Telegram, Facebook Messenger, and Viber** in the background.
   - Robust fallback text extraction handling (`EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_TEXT_LINES`) to capture rich or grouped messages safely without crashing.
5. **Advanced Shift & Caps Lock Logic:**
   - Single tap for a capital letter or double-tap for continuous Caps Lock.

---

## 🔔 Latest Update: Enhanced Notification Translation Engine

We have recently upgraded the background notification listener service to provide seamless multi-app messaging support:
- **Multi-App Integration:** Explicitly handles package name filtering for WhatsApp, Telegram, Messenger, and Viber.
- **Smart Fallback Extraction:** Prevents notification parsing crashes by intelligently checking `EXTRA_BIG_TEXT` and `EXTRA_TEXT_LINES` when standard `EXTRA_TEXT` is missing.
- **Background Dispatch:** Broadcasts translated alerts instantly via `LocalBroadcastManager` for smooth keyboard banners and overlays.

---

## 🏗️ Project Architecture & Services

- **`SinTransKeyboardService`**: Manages the custom keyboard layout, IME lifecycle, character buffering, Singlish transliteration, and direct English switching.
- **`NotificationTranslationService`**: Extends `NotificationListenerService` to intercept messages from supported chat apps in real-time, extracts text safely using fallback keys, translates via API, and broadcasts updates.
- **`GoogleTranslationRepository`**: Handles asynchronous sentence and notification translation via Google Cloud Translation API.

---

## 📱 Supported Apps for Notification Translation
- 🟢 WhatsApp (`com.whatsapp`)
- ✈️ Telegram (`org.telegram.messenger`)
- 🔵 Facebook Messenger (`com.facebook.orca`)
- 🟣 Viber (`com.viber.voip`)

---

## 🛠️ Tech Stack & Libraries
- **Language:** Kotlin
- **Architecture:** Coroutines, Flow, LocalBroadcastManager
- **Android Components:** InputMethodService, NotificationListenerService, SharedPreferences
- **API:** Google Cloud Translation API

---

## ⚙️ Setup & Installation

1. Clone the repository:
   ```bash
   git clone [https://github.com/your-username/SinTrans-Keyboard.git](https://github.com/your-username/SinTrans-Keyboard.git)
Open the project in Android Studio.

Add your Google Translation API Key in your local properties or configuration files.

Build and run the app on your Android device (ensure Notification Access permission is enabled in device settings for notification translation).

💡 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the issues page or submit a pull request.

📄 License
This project is open-source and available under the MIT License.
