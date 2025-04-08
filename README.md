# Robocall - Voice-Controlled Call and Text App

An Android application that allows users to make calls and send text messages using voice commands.

## Features

- Voice command recognition for calls and texts
- Natural language processing for command interpretation
- Contact integration with fuzzy name matching
- Material 3 design with modern UI components
- Real-time status feedback
- Permission handling for required Android features

## Voice Commands

The app supports the following voice command patterns:

- Making calls:
  - "Call [contact name]"
  - "Phone [contact name]"
  - "Dial [contact name]"

- Sending texts:
  - "Text [contact name] saying [message]"
  - "Send message to [contact name] [message]"
  - "Message [contact name] saying [message]"

## Required Permissions

- `RECORD_AUDIO` - For voice command recognition
- `CALL_PHONE` - For making phone calls
- `SEND_SMS` - For sending text messages
- `READ_CONTACTS` - For contact name lookup

## Technical Details

- Built with Kotlin and Jetpack Compose
- Uses Android's SpeechRecognizer for voice input
- Implements MVVM architecture
- Material 3 theming and components
- Kotlin Coroutines and Flow for async operations

## Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/tvictori/robocall.git
   ```

2. Open in Android Studio

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

## License

This project is licensed under the MIT License - see the LICENSE file for details. 