# SprachBrücke – AI-Powered Translator App 🌍

SprachBrücke is a modern Android translation application designed for seamless face-to-face communication. It leverages Google's Gemini AI for high-quality translations and Android's On-Device Speech Recognition for fast, reliable input!

## ✨ Features

- **Real-time Translation:** Powered by Google Gemini (1.5 Flash / 2.0 Flash).
- **Offline Speech Recognition:** Supports downloaded Android language packs for use without stable internet.
- **Multilingual UI:** Available in German, English, French, and Spanish.
- **Innovative Layout:**
    - Split-screen view for two participants.
    - **180° Text Rotation:** Flip the top text field so your conversation partner can read it easily.
- **Customizable UI:** Change font sizes, colors, and switch between Light/Dark modes.
- **Always-On Display:** Screen stays active during conversation (with a 5-minute inactivity timeout).
- **Synchronized Scrolling:** Both language panels scroll proportionally to keep context aligned.

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- A Google Gemini API Key from [Google AI Studio](https://aistudio.google.com/)

### Setup
1. Clone the repository.
2. Copy `local.properties.example` to `local.properties`.
3. Open `local.properties` and enter your `GEMINI_API_KEY`.
4. Open the project in Android Studio.
5. Build and run on your Android device (optimized for Pixel 9).

## 🛠 Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (ViewModel, StateFlow)
- **Local Storage:** Room Database (Conversation history), DataStore (Settings)
- **AI Integration:** Google Generative AI SDK (Gemini)
- **Speech:** Android SpeechRecognizer API

## 🔒 Privacy & Security
- **API Keys:** Sensitive keys are stored in `local.properties` and are excluded from version control via `.gitignore`.
- **Data:** Conversations are stored locally in a Room database.

## 📝 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Developed with ❤️ for better global communication.*
