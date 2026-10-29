# AeroFast Pro - Security & Configuration Guide

## Security Improvements

### 🔒 Encrypted Storage
- **API Keys**: All API keys are now stored using Android's `EncryptedSharedPreferences` with AES-256-GCM encryption
- **Cache Data**: Blueprint extraction cache is encrypted at rest
- **Master Key**: Uses hardware-backed keystore when available for maximum security

### 🛡️ API Security
- **No Hardcoded Keys**: All API keys have been removed from source code and build configs
- **Rate Limiting**: API calls are throttled to 1 request per 500ms to prevent quota abuse
- **Timeout Protection**: 
  - Connection: 30s
  - Read/Write: 60s
  - Total call: 90s
- **Retry Logic**: Automatic retry on connection failures
- **User-Agent Headers**: All API requests include proper user-agent identification

### 📊 Data Protection
- **Cache Expiration**: Cached data automatically expires after 30 days
- **Input Validation**: All API responses are validated before parsing
- **Error Handling**: Detailed error messages prevent sensitive information leakage

---

## Setup Guide

### 1. Get a Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Click **"Get API Key"** in the left sidebar
3. Create a new API key in your Google Cloud project
4. Copy the key (keep it safe!)

### 2. Configure the App

#### For Local Development:
```bash
# Create .env file in project root
GEMINI_API_KEY=your_actual_api_key_here
```

#### For APK Builds:
```bash
# Set as environment variable before building
export GEMINI_API_KEY=your_actual_api_key_here
./gradlew assembleDebug
```

#### For CI/CD (GitHub Actions):
Add to repository secrets:
```
GEMINI_API_KEY: your_actual_api_key_here
```

Then use in workflow:
```yaml
env:
  GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
```

### 3. Configure API Key in App

After first launch:
1. Tap **Settings** ⚙️
2. Tap **AI Settings** or **Gemini API Key**
3. Paste your API key
4. Tap **Save**

The key is encrypted and stored securely.

---

## Configuration Options

### Environment Variables
- `GEMINI_API_KEY`: Your Google Gemini API key (required for AI features)
- `FIREBASE_DB_URL`: Firebase Realtime Database URL (configured in AppConfig.kt)

### AppConfig.kt Settings
- API timeouts and connection parameters
- Rate limiting intervals
- Cache validity periods
- Image processing quality

---

## Security Best Practices

### For Users
✅ **Do:**
- Keep your API key private and secure
- Rotate your key periodically
- Monitor API usage in Google Cloud Console
- Enable rate limiting in Google Cloud if needed

❌ **Don't:**
- Share your API key with others
- Commit your API key to version control
- Use the same API key across multiple apps
- Enable billing without spending limits

### For Developers
✅ **Do:**
- Store API keys in environment variables
- Use GitHub Secrets for CI/CD
- Rotate keys regularly
- Monitor logs for unauthorized access attempts

❌ **Don't:**
- Hardcode API keys in source files
- Commit .env files with real keys
- Use personal API keys for production builds
- Disable encryption for convenience

---

## Troubleshooting

### "API Key Not Configured"
- Launch the app and go to Settings → AI Settings
- Paste your API key
- Restart the app

### "Invalid or Expired API Key"
- Check your key in [Google AI Studio](https://aistudio.google.com/)
- Ensure billing is enabled for your project
- Try creating a new API key

### "Quota Exceeded (429)"
- Check your usage in Google Cloud Console
- Upgrade your API plan if needed
- Wait and retry after a few minutes

### "Network Timeout"
- Check your internet connection
- Try again in a few moments
- The app falls back to local calculations if available

---

## Data Retention Policy

- **Blueprint Cache**: Automatically deleted after 30 days of inactivity
- **Chat History**: Stored locally until user deletes (Firebase Realtime)
- **User Preferences**: Persistent until user resets
- **API Keys**: Encrypted and never transmitted except to Gemini API

---

## Compliance

- ✅ No sensitive data in logs
- ✅ GDPR-ready (local-first storage)
- ✅ Hardware-backed encryption when available
- ✅ No analytics or tracking
- ✅ Open source security practices

For more security details, see `GeminiAiService.kt` and `SecurityUtils.kt`.
