# AI Chat Hub - Intelligent Multi-Model Conversational Companion

**AI Chat Hub** is a modern, high-performance Android application built with **Jetpack Compose** and **Kotlin**. It brings together leading AI models—including Google Gemini (2.5 Flash, 2.0 Flash, 1.5 Flash) and OpenRouter models (DeepSeek R1, Llama 3.3 70B)—into a seamless, unified chat interface.

---

## 🌟 Key Features

### 🧠 Multi-Model AI Integration
- **Direct Gemini API Support**: Fast, low-latency integration with `gemini-2.5-flash`, `gemini-2.0-flash`, and `gemini-1.5-flash`.
- **OpenRouter Support**: Access reasoning models like `DeepSeek R1` and heavy open-weights like `Llama 3.3 70B`.
- **Custom Model Selection**: Option to specify custom OpenRouter model identifiers on the fly.

### 💾 Offline-First Architecture & Storage
- **Room SQLite Database**: All chat sessions and message histories are persisted locally on the device for fast loading and full offline access.
- **Session Management**: Effortlessly create, switch between, and clear chat sessions.

### 📊 Real-Time Analytics & Metrics
- **Token Usage Tracking**: Displays prompt and completion token consumption for every response.
- **Reasoning Tokens**: Tracks inner reasoning steps for chain-of-thought models like DeepSeek R1.
- **Latency Monitoring**: Measures round-trip API response times in milliseconds.

### 🎨 Modern Material 3 Interface
- **Jetpack Compose UI**: Smooth animations, drawer navigation, and clean edge-to-edge layout.
- **Rich Markdown & Code Rendering**: Full formatting support with code blocks, copy-to-clipboard functionality, and LaTeX math rendering.
- **Full RTL & Arabic Language Support**: Dynamic text direction alignment for seamless multilingual messaging.

### 💰 Integrated Monetization
- **AdMob Interstitial Ads**: Configured with Google Mobile Ads SDK (`ca-app-pub-1270885163968679~2866802064`).
- **Balanced Frequency**: Automatically presents interstitial ads at user-friendly intervals (e.g. session switches and message thresholds).

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Data Architecture
- **Local Database**: Room DB (SQLite) + Kotlin Flow
- **Networking**: Retrofit 2 + OkHttp 3 + Kotlinx Serialization
- **Monetization**: Google Mobile Ads SDK (AdMob)
- **Asynchronous Execution**: Kotlin Coroutines & StateFlow

---

## ⚙️ Configuration & API Setup

### 1. API Keys
Users can configure their custom keys directly in the app **Settings panel (⚙️)** or inject environment variables during development:

```env
GEMINI_API_KEY=your_gemini_api_key_here
OPENROUTER_API_KEY=your_openrouter_api_key_here
```

### 2. AdMob Setup
The app is pre-configured with the following AdMob IDs in `AndroidManifest.xml` and `AdManager.kt`:
- **AdMob App ID**: `ca-app-pub-1270885163968679~2866802064`
- **Interstitial Ad Unit ID**: `ca-app-pub-1270885163968679/9240638723`

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/ai-chat-hub.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync Gradle and build the project:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run on an Android device or emulator running **Android 7.0 (API level 24)** or higher.

---

## 📄 License
Designed and developed for high-performance AI interaction on Android.
