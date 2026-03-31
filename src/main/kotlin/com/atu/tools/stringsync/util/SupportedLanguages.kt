package com.atu.tools.stringsync.util

import com.atu.tools.stringsync.model.LanguageOption

object SupportedLanguages {
    val all: List<LanguageOption> = listOf(
        LanguageOption("af", "Afrikaans"),
        LanguageOption("sq", "Albanian"),
        LanguageOption("am", "Amharic"),
        LanguageOption("ar", "Arabic"),
        LanguageOption("hy", "Armenian"),
        LanguageOption("as", "Assamese"),
        LanguageOption("ay", "Aymara"),
        LanguageOption("az", "Azerbaijani"),
        LanguageOption("bm", "Bambara"),
        LanguageOption("eu", "Basque"),
        LanguageOption("brx", "Bodo"),
        LanguageOption("be", "Belarusian"),
        LanguageOption("bn", "Bengali"),
        LanguageOption("bho", "Bhojpuri"),
        LanguageOption("bs", "Bosnian"),
        LanguageOption("bg", "Bulgarian"),
        LanguageOption("ca", "Catalan"),
        LanguageOption("ceb", "Cebuano"),
        LanguageOption("zh", "Chinese"),
        LanguageOption("zh-CN", "Chinese (Simplified)", setOf("zh-rCN", "zh-hans")),
        LanguageOption("zh-TW", "Chinese (Traditional)", setOf("zh-rTW", "zh-hant")),
        LanguageOption("co", "Corsican"),
        LanguageOption("hr", "Croatian"),
        LanguageOption("cs", "Czech"),
        LanguageOption("da", "Danish"),
        LanguageOption("dv", "Divehi"),
        LanguageOption("doi", "Dogri"),
        LanguageOption("nl", "Dutch"),
        LanguageOption("en", "English"),
        LanguageOption("eo", "Esperanto"),
        LanguageOption("et", "Estonian"),
        LanguageOption("ee", "Ewe"),
        LanguageOption("fil", "Filipino", setOf("tl")),
        LanguageOption("fi", "Finnish"),
        LanguageOption("fr", "French"),
        LanguageOption("fy", "Frisian"),
        LanguageOption("gl", "Galician"),
        LanguageOption("ka", "Georgian"),
        LanguageOption("de", "German"),
        LanguageOption("el", "Greek"),
        LanguageOption("gn", "Guarani"),
        LanguageOption("gu", "Gujarati"),
        LanguageOption("ht", "Haitian Creole"),
        LanguageOption("ha", "Hausa"),
        LanguageOption("haw", "Hawaiian"),
        LanguageOption("he", "Hebrew", setOf("iw")),
        LanguageOption("iw", "Hebrew (legacy)", setOf("he")),
        LanguageOption("hi", "Hindi"),
        LanguageOption("hmn", "Hmong"),
        LanguageOption("hu", "Hungarian"),
        LanguageOption("is", "Icelandic"),
        LanguageOption("ig", "Igbo"),
        LanguageOption("ilo", "Ilocano"),
        LanguageOption("id", "Indonesian", setOf("in")),
        LanguageOption("in", "Indonesian (legacy)", setOf("id")),
        LanguageOption("ga", "Irish"),
        LanguageOption("it", "Italian"),
        LanguageOption("ja", "Japanese"),
        LanguageOption("jv", "Javanese"),
        LanguageOption("kn", "Kannada"),
        LanguageOption("kk", "Kazakh"),
        LanguageOption("km", "Khmer"),
        LanguageOption("rw", "Kinyarwanda"),
        LanguageOption("gom", "Konkani"),
        LanguageOption("ko", "Korean"),
        LanguageOption("kri", "Krio"),
        LanguageOption("ku", "Kurdish (Kurmanji)"),
        LanguageOption("ckb", "Kurdish (Sorani)"),
        LanguageOption("ky", "Kyrgyz"),
        LanguageOption("lo", "Lao"),
        LanguageOption("la", "Latin"),
        LanguageOption("lv", "Latvian"),
        LanguageOption("ln", "Lingala"),
        LanguageOption("lt", "Lithuanian"),
        LanguageOption("lg", "Luganda"),
        LanguageOption("lb", "Luxembourgish"),
        LanguageOption("mk", "Macedonian"),
        LanguageOption("mai", "Maithili"),
        LanguageOption("mg", "Malagasy"),
        LanguageOption("ms", "Malay"),
        LanguageOption("ml", "Malayalam"),
        LanguageOption("mt", "Maltese"),
        LanguageOption("mi", "Maori"),
        LanguageOption("mr", "Marathi"),
        LanguageOption("mni-Mtei", "Meitei"),
        LanguageOption("lus", "Mizo"),
        LanguageOption("mn", "Mongolian"),
        LanguageOption("my", "Myanmar (Burmese)", setOf("bur")),
        LanguageOption("ne", "Nepali"),
        LanguageOption("no", "Norwegian", setOf("nb")),
        LanguageOption("nb", "Norwegian Bokmal", setOf("no")),
        LanguageOption("ny", "Nyanja (Chichewa)"),
        LanguageOption("or", "Odia (Oriya)"),
        LanguageOption("om", "Oromo"),
        LanguageOption("ps", "Pashto"),
        LanguageOption("fa", "Persian"),
        LanguageOption("pl", "Polish"),
        LanguageOption("pt", "Portuguese"),
        LanguageOption("pa", "Punjabi"),
        LanguageOption("qu", "Quechua"),
        LanguageOption("ro", "Romanian"),
        LanguageOption("ru", "Russian"),
        LanguageOption("sm", "Samoan"),
        LanguageOption("sa", "Sanskrit"),
        LanguageOption("gd", "Scots Gaelic"),
        LanguageOption("nso", "Sepedi"),
        LanguageOption("sr", "Serbian"),
        LanguageOption("st", "Sesotho"),
        LanguageOption("sn", "Shona"),
        LanguageOption("sd", "Sindhi"),
        LanguageOption("si", "Sinhala"),
        LanguageOption("sk", "Slovak"),
        LanguageOption("sl", "Slovenian"),
        LanguageOption("so", "Somali"),
        LanguageOption("es", "Spanish"),
        LanguageOption("su", "Sundanese"),
        LanguageOption("sw", "Swahili"),
        LanguageOption("sv", "Swedish"),
        LanguageOption("tg", "Tajik"),
        LanguageOption("ta", "Tamil"),
        LanguageOption("tt", "Tatar"),
        LanguageOption("te", "Telugu"),
        LanguageOption("th", "Thai"),
        LanguageOption("ti", "Tigrinya"),
        LanguageOption("ts", "Tsonga"),
        LanguageOption("tr", "Turkish"),
        LanguageOption("tk", "Turkmen"),
        LanguageOption("ak", "Twi (Akan)"),
        LanguageOption("uk", "Ukrainian"),
        LanguageOption("ur", "Urdu"),
        LanguageOption("ug", "Uyghur"),
        LanguageOption("uz", "Uzbek"),
        LanguageOption("vi", "Vietnamese"),
        LanguageOption("cy", "Welsh"),
        LanguageOption("xh", "Xhosa"),
        LanguageOption("yi", "Yiddish"),
        LanguageOption("yo", "Yoruba"),
        LanguageOption("zu", "Zulu")
    )

    fun defaultSelectedCodes(existingLocales: Set<String>): Set<String> {
        return mapInputsToCodes(existingLocales)
    }

    fun selectedCodesFromSheetLocales(sheetLocales: Set<String>): Set<String> {
        return mapInputsToCodes(sheetLocales)
    }

    private fun mapInputsToCodes(inputs: Set<String>): Set<String> {
        val selected = linkedSetOf<String>()
        for (raw in inputs) {
            val normalized = normalizeLocale(raw)
            val primary = normalized.substringBefore('-')

            val exact = all.filter { option ->
                optionFullKeys(option).contains(normalized)
            }
            if (exact.isNotEmpty()) {
                selected += pickBest(exact, normalized, primary).code
                continue
            }

            val byPrimary = all.filter { option ->
                optionPrimaryKeys(option).contains(primary)
            }
            if (byPrimary.isNotEmpty()) {
                selected += pickBest(byPrimary, normalized, primary).code
            }
        }
        return selected
    }

    private fun pickBest(candidates: List<LanguageOption>, normalizedInput: String, primaryInput: String): LanguageOption {
        return candidates.sortedWith(
            compareBy<LanguageOption>(
                { if (normalizeLocale(it.code) == normalizedInput) 0 else 1 },
                { if (normalizeLocale(it.code) == primaryInput) 0 else 1 },
                { if (it.name.contains("(legacy)", ignoreCase = true)) 1 else 0 },
                { it.code.length }
            )
        ).first()
    }

    private fun optionFullKeys(option: LanguageOption): Set<String> {
        val keys = linkedSetOf<String>()
        keys += normalizeLocale(option.code)
        option.aliases.forEach { keys += normalizeLocale(it) }
        return keys
    }

    private fun optionPrimaryKeys(option: LanguageOption): Set<String> {
        val keys = linkedSetOf<String>()
        keys += normalizeLocale(option.code).substringBefore('-')
        option.aliases.forEach { keys += normalizeLocale(it).substringBefore('-') }
        return keys
    }

    fun normalizeLocale(input: String): String {
        val value = input.trim().replace('_', '-').lowercase()
        return value
            .replace("-r", "-")
            .removePrefix("b+")
            .replace("+", "-")
    }
}
