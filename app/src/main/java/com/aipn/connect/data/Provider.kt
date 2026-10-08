package com.aipn.connect.data

import androidx.compose.ui.graphics.Color

/** How a provider expects the chat request to be shaped. */
enum class Protocol {
    /** `POST {base}/chat/completions` with `Authorization: Bearer <key>` - Groq, OpenRouter, Mistral, ... */
    OPENAI,

    /** `POST {base}/models/{model}:streamGenerateContent` - Google AI Studio. */
    GEMINI,
}

/**
 * One AI provider that offers a usable free tier.
 *
 * Only endpoints that are free to call are listed here. The app never creates accounts,
 * never collects keys from other people, and never ships a key of its own - the keys are
 * entered by the user and stored encrypted on their device.
 */
data class Provider(
    val id: String,
    val name: String,
    val nameFa: String,
    val protocol: Protocol,
    val baseUrl: String,
    val keyUrl: String,
    val docsUrl: String,
    val freeTierEn: String,
    val freeTierFa: String,
    val defaultModel: String,
    val models: List<String> = emptyList(),
    val accent: Color = Color(0xFF4F46E5),
    val supportsVision: Boolean = false,
    val supportsStreaming: Boolean = true,
    /** Some free endpoints work anonymously, which makes the app usable before adding any key. */
    val keyOptional: Boolean = false,
    /** Free account id style credentials (Cloudflare needs the account id next to the key). */
    val needsAccountId: Boolean = false,
    /** Set for OpenRouter: keeps only models whose id ends with ":free". */
    val freeSuffixOnly: Boolean = false,
    val accountId: String = ""
) {
    val modelListUrl: String
        get() = when (protocol) {
            Protocol.OPENAI -> "$baseUrl/models"
            Protocol.GEMINI -> "$baseUrl/models"
        }

    /** The credential to send: the API key, or the account id for Cloudflare-style setups. */
    fun accessKey(vault: KeyVault): String? =
        vault.apiKey(id)?.takeIf { it.isNotBlank() } ?: accountId.takeIf { it.isNotBlank() }
}

object Providers {

    val ALL: List<Provider> = listOf(
        Provider(
            id = "gemini",
            name = "Google Gemini",
            nameFa = "گوگل جمینایی",
            protocol = Protocol.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            keyUrl = "https://aistudio.google.com/apikey",
            docsUrl = "https://ai.google.dev/gemini-api/docs",
            freeTierEn = "Free tier, no credit card. About 1,500 requests/day on Flash models.",
            freeTierFa = "لایه رایگان بدون کارت بانکی؛ حدود ۱۵۰۰ درخواست در روز برای مدل‌های Flash.",
            defaultModel = "gemini-2.5-flash",
            models = listOf(
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
            ),
            accent = Color(0xFF4285F4),
            supportsVision = true,
        ),
        Provider(
            id = "groq",
            name = "Groq",
            nameFa = "گروک",
            protocol = Protocol.OPENAI,
            baseUrl = "https://api.groq.com/openai/v1",
            keyUrl = "https://console.groq.com/keys",
            docsUrl = "https://console.groq.com/docs",
            freeTierEn = "Free plan, extremely fast. Roughly 1,000 requests/day.",
            freeTierFa = "پلن رایگان و بسیار سریع؛ حدود ۱۰۰۰ درخواست در روز.",
            defaultModel = "llama-3.3-70b-versatile",
            models = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "gemma2-9b-it",
                "qwen-qwq-32b",
            ),
            accent = Color(0xFFF55036),
            supportsVision = false,
        ),
        Provider(
            id = "openrouter",
            name = "OpenRouter",
            nameFa = "اوپن‌رووتر",
            protocol = Protocol.OPENAI,
            baseUrl = "https://openrouter.ai/api/v1",
            keyUrl = "https://openrouter.ai/keys",
            docsUrl = "https://openrouter.ai/docs",
            freeTierEn = "25+ models marked ':free'. 50 requests/day free (1,000 after $10 credits).",
            freeTierFa = "بیش از ۲۵ مدل با برچسب «:free»؛ ۵۰ درخواست رایگان در روز.",
            defaultModel = "deepseek/deepseek-chat-v3-0324:free",
            models = emptyList(),
            accent = Color(0xFF6467F2),
            supportsVision = true,
            freeSuffixOnly = true,
        ),
        Provider(
            id = "pollinations",
            name = "Pollinations",
            nameFa = "پولینیشنز",
            protocol = Protocol.OPENAI,
            baseUrl = "https://text.pollinations.ai",
            keyUrl = "https://pollinations.ai",
            docsUrl = "https://github.com/pollinations/pollinations",
            freeTierEn = "No account and no API key needed. Always available as a fallback.",
            freeTierFa = "بدون حساب و بدون کلید؛ همیشه بهعنوان گزینه پشتیبان در دسترس است.",
            defaultModel = "openai",
            models = listOf("openai", "mistral", "llama"),
            accent = Color(0xFF14B8A6),
            supportsVision = true,
            keyOptional = true,
        ),
        Provider(
            id = "cerebras",
            name = "Cerebras",
            nameFa = "سربراس",
            protocol = Protocol.OPENAI,
            baseUrl = "https://api.cerebras.ai/v1",
            keyUrl = "https://cloud.cerebras.ai",
            docsUrl = "https://inference-docs.cerebras.ai",
            freeTierEn = "Free tier on Llama and Qwen models. Roughly 30 requests/minute.",
            freeTierFa = "لایه رایگان روی مدل‌های Llama و Qwen؛ حدود ۳۰ درخواست در دقیقه.",
            defaultModel = "llama3.1-8b",
            models = listOf(
                "llama3.1-8b",
                "llama-3.3-70b",
                "qwen-3-32b",
            ),
            accent = Color(0xFFF97316),
            supportsVision = false,
        ),
        Provider(
            id = "mistral",
            name = "Mistral",
            nameFa = "میسترال",
            protocol = Protocol.OPENAI,
            baseUrl = "https://api.mistral.ai/v1",
            keyUrl = "https://console.mistral.ai/api-keys",
            docsUrl = "https://docs.mistral.ai",
            freeTierEn = "Experimental tier with free models, requires phone verification.",
            freeTierFa = "پلن آزمایشی با مدل‌های رایگان؛ نیازمند تأیید شماره تلفن.",
            defaultModel = "mistral-small-latest",
            models = listOf("mistral-small-latest", "open-mistral-nemo"),
            accent = Color(0xFFFA520F),
            supportsVision = false,
        ),
        Provider(
            id = "siliconflow",
            name = "SiliconFlow",
            nameFa = "سیلیکون‌فلو",
            protocol = Protocol.OPENAI,
            baseUrl = "https://api.siliconflow.cn/v1",
            keyUrl = "https://cloud.siliconflow.cn/account/ak",
            docsUrl = "https://docs.siliconflow.cn",
            freeTierEn = "Free models on Chinese-hosted infrastructure, good for Persian text.",
            freeTierFa = "مدل‌های رایگان روی زیرساخت چینی؛ برای متن فارسی مناسب است.",
            defaultModel = "Qwen/Qwen2.5-7B-Instruct",
            models = listOf(
                "Qwen/Qwen2.5-7B-Instruct",
                "Qwen/QwQ-32B",
                "deepseek-ai/DeepSeek-V3",
            ),
            accent = Color(0xFF6D28D9),
            supportsVision = false,
        ),
        Provider(
            id = "huggingface",
            name = "Hugging Face",
            nameFa = "هاگینگ‌فیس",
            protocol = Protocol.OPENAI,
            baseUrl = "https://router.huggingface.co/v1",
            keyUrl = "https://huggingface.co/settings/tokens",
            docsUrl = "https://huggingface.co/docs/inference-providers",
            freeTierEn = "Free Inference Providers give a small monthly credit.",
            freeTierFa = "سرویسهای رایگان Inference اعتبار ماهانه کوچکی میدهند.",
            defaultModel = "meta-llama/Llama-3.1-8B-Instruct",
            models = listOf(
                "meta-llama/Llama-3.1-8B-Instruct",
                "Qwen/Qwen2.5-72B-Instruct",
            ),
            accent = Color(0xFFFFD21E),
            supportsVision = true,
        ),
        Provider(
            id = "nvidia",
            name = "NVIDIA NIM",
            nameFa = "انویدیا",
            protocol = Protocol.OPENAI,
            baseUrl = "https://integrate.api.nvidia.com/v1",
            keyUrl = "https://build.nvidia.com",
            docsUrl = "https://docs.api.nvidia.com/nim",
            freeTierEn = "Free evaluation credits on open models.",
            freeTierFa = "اعتبار رایگان ارزیابی روی مدل‌های باز.",
            defaultModel = "meta/llama-3.1-8b-instruct",
            models = listOf(
                "meta/llama-3.1-8b-instruct",
                "mistralai/mistral-7b-instruct-v0.3",
            ),
            accent = Color(0xFF76B900),
            supportsVision = false,
        ),
        Provider(
            id = "cohere",
            name = "Cohere",
            nameFa = "کوهره",
            protocol = Protocol.OPENAI,
            baseUrl = "https://api.cohere.com/compatibility/v1",
            keyUrl = "https://dashboard.cohere.com/api-keys",
            docsUrl = "https://docs.cohere.com",
            freeTierEn = "Trial key with about 1,000 calls/month.",
            freeTierFa = "کلید آزمایشی با حدود ۱۰۰۰ فراخوانی در ماه.",
            defaultModel = "command-r7b-12-2024",
            models = listOf("command-r7b-12-2024", "command-r-plus"),
            accent = Color(0xFF39594D),
            supportsVision = false,
        ),
    )

    private val byId = ALL.associateBy { it.id }

    fun byId(id: String): Provider? = byId[id]

    /** Providers that need no key at all. */
    val zeroConfig: List<Provider> = ALL.filter { it.keyOptional }
}