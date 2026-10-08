# AI APN Connect

An Android app that talks to many AI providers at once — using **the free API keys you own**.
No subscription, no credit card, no key bundled with the app.

Chat with Gemini, Groq, OpenRouter, Mistral, Cerebras and more from a single screen. When one
provider fails or hits its rate limit, the app automatically falls back to the next one, so
several small free quotas feel like one large one.

## What it does

- **Chat** with streaming answers, markdown rendering, copy / regenerate / delete.
- **Image input** on providers that support vision.
- **Automatic fallback** — a dead key or an HTTP 429 quietly moves to the next provider.
- **Key manager** — keys are encrypted with an AES-GCM key held in the Android keystore and
  are never written to disk in plain text.
- **Model discovery** — each provider's `/models` endpoint is queried, so the model list stays
  current without an app update.
- **Zero-config start** — Pollinations works without any key, so the app answers immediately.
- **Persian and English**, with full RTL support.

## Providers

| Provider | Free tier | Get a key |
| --- | --- | --- |
| Google Gemini | Permanent free tier, ~1,500 req/day on Flash | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) |
| Groq | Free plan, very fast | [console.groq.com/keys](https://console.groq.com/keys) |
| OpenRouter | 25+ models tagged `:free` | [openrouter.ai/keys](https://openrouter.ai/keys) |
| Pollinations | No key required | [pollinations.ai](https://pollinations.ai) |
| Cerebras | Free inference tier | [cloud.cerebras.ai](https://cloud.cerebras.ai) |
| Mistral | Free experimental models | [console.mistral.ai/api-keys](https://console.mistral.ai/api-keys) |
| SiliconFlow | Free models, good for Persian | [cloud.siliconflow.cn](https://cloud.siliconflow.cn/account/ak) |
| Hugging Face | Monthly free credits | [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens) |
| NVIDIA NIM | Free evaluation credits | [build.nvidia.com](https://build.nvidia.com) |
| Cohere | Trial key, ~1,000 calls/month | [dashboard.cohere.com/api-keys](https://dashboard.cohere.com/api-keys) |

Free-tier limits are set by each provider and change from time to time.

## Build

Requires JDK 17 and the Android SDK.

```bash
keytool -genkeypair -v -keystore app/apn-release.keystore -alias apn \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=AI APN Connect"

./gradlew assembleDebug assembleRelease
```

The APK lands in `app/build/outputs/apk/`.

CI does the same on every push and uploads the APKs as a build artifact.

## About the keys

The app ships **without any API key**. You create keys in each provider's own dashboard and
paste them into the app, where they are encrypted locally. Nothing is collected from other
people, no key is shared, and requests go only to the provider that key belongs to.

## License

MIT