package com.example.data.model

import java.util.Locale

data class LanguageOption(
    val tag: String,
    val displayName: String,
    val nativeName: String
) {
    val label: String
        get() = if (nativeName.equals(displayName, ignoreCase = true)) {
            displayName
        } else {
            "$displayName ($nativeName)"
        }
}

object WorldwideLanguages {

    private val curatedFeatured = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("ta", "Tamil", "தமிழ்"),
        LanguageOption("hi", "Hindi", "हिन्दी"),
        LanguageOption("te", "Telugu", "తెలుగు"),
        LanguageOption("ml", "Malayalam", "മലയാളം"),
        LanguageOption("kn", "Kannada", "ಕನ್ನಡ"),
        LanguageOption("bn", "Bengali", "বাংলা"),
        LanguageOption("mr", "Marathi", "मराठी"),
        LanguageOption("gu", "Gujarati", "ગુજરાતી"),
        LanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageOption("ur", "Urdu", "اردو"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("de", "German", "Deutsch"),
        LanguageOption("it", "Italian", "Italiano"),
        LanguageOption("pt", "Portuguese", "Português"),
        LanguageOption("ar", "Arabic", "العربية"),
        LanguageOption("zh-CN", "Chinese (Simplified)", "简体中文"),
        LanguageOption("zh-TW", "Chinese (Traditional)", "繁體中文"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("ko", "Korean", "한국어"),
        LanguageOption("ru", "Russian", "Русский"),
        LanguageOption("tr", "Turkish", "Türkçe"),
        LanguageOption("id", "Indonesian", "Bahasa Indonesia"),
        LanguageOption("ms", "Malay", "Bahasa Melayu"),
        LanguageOption("th", "Thai", "ไทย"),
        LanguageOption("vi", "Vietnamese", "Tiếng Việt"),
        LanguageOption("nl", "Dutch", "Nederlands"),
        LanguageOption("pl", "Polish", "Polski"),
        LanguageOption("uk", "Ukrainian", "Українська"),
        LanguageOption("el", "Greek", "Ελληνικά"),
        LanguageOption("he", "Hebrew", "עברית"),
        LanguageOption("fa", "Persian", "فارسی"),
        LanguageOption("sw", "Swahili", "Kiswahili"),
        LanguageOption("ne", "Nepali", "नेपाली"),
        LanguageOption("si", "Sinhala", "සිංහල")
    )

    val allLanguages: List<LanguageOption> by lazy {
        val map = LinkedHashMap<String, LanguageOption>()
        curatedFeatured.forEach { map[it.tag.lowercase()] = it }

        Locale.getAvailableLocales()
            .asSequence()
            .filter { it.language.isNotBlank() }
            .map { locale ->
                val baseTag = locale.language
                val englishName = locale.getDisplayLanguage(Locale.ENGLISH).trim()
                val nativeName = locale.getDisplayLanguage(locale).trim()
                LanguageOption(
                    tag = baseTag,
                    displayName = englishName.ifBlank { baseTag },
                    nativeName = nativeName.ifBlank { englishName.ifBlank { baseTag } }
                )
            }
            .filter { it.displayName.isNotBlank() && !it.tag.lowercase().startsWith("und") }
            .sortedBy { it.displayName }
            .forEach { opt ->
                val key = opt.tag.lowercase()
                if (!map.containsKey(key) && map.keys.none { it.startsWith("$key-") }) {
                    map[key] = opt
                }
            }

        map.values.toList()
    }

    fun findByTag(tag: String?): LanguageOption {
        if (tag.isNullOrBlank()) return curatedFeatured.first()
        val normalized = tag.trim().lowercase()
        return allLanguages.firstOrNull { it.tag.lowercase() == normalized }
            ?: allLanguages.firstOrNull { it.tag.lowercase().substringBefore('-') == normalized.substringBefore('-') }
            ?: LanguageOption(tag = tag, displayName = Locale.forLanguageTag(tag).getDisplayName(Locale.ENGLISH).ifBlank { tag }, nativeName = Locale.forLanguageTag(tag).displayName.ifBlank { tag })
    }

    fun search(query: String): List<LanguageOption> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allLanguages
        return allLanguages.filter {
            it.displayName.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.tag.lowercase().contains(q)
        }
    }
}
