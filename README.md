<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/3ec9b983-8a5c-417d-8ab4-497ce3e6616d

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio) and a [Google Gemini API key](https://aistudio.google.com/)

### Setup Steps:

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project
4. Create a `.env` file in the project directory with your API key:
   ```
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
   See `.env.example` for the format.
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. Launch the app → Settings → AI Settings → Paste your API key (if not set via .env)

### Security Notes:
- **Never commit** `.env` files with real API keys to version control
- API keys are now **encrypted at rest** using EncryptedSharedPreferences
- See [SECURITY_AND_CONFIG.md](./SECURITY_AND_CONFIG.md) for detailed security information

### Production Deployment:
If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.
