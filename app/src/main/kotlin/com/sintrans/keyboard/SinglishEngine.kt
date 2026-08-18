package com.sintrans.keyboard

object SinglishEngine {

    // Colloquial overrides dictionary for common Romanized Sinhala words
    private val colloquialDictionary = mapOf(
        "weda" to "වැඩ",
        "wada" to "වැඩ",
        "be" to "බෑ",
        "bae" to "බෑ",
        "na" to "නෑ",
        "ne" to "නෑ",
        "ba" to "බෑ",
        "ow" to "ඔව්",
        "neda" to "නේද",
        "wage" to "වගේ",
        "denna" to "දෙන්න",
        "dhenna" to "දෙන්න",
        "hema" to "හැම",
        "hoda" to "හොඳ",
        "honda" to "හොඳ",
        "puluwan" to "පුළුවන්",
        "mokada" to "මොකද",
        "kohomada" to "කොහොමද",
        "eka" to "එක",
        "eke" to "එකේ",
        "ekak" to "එකක්",
        "yanna" to "යන්න",
        "enna" to "එන්න",
        "ganna" to "ගන්න",
        "karanna" to "කරන්න",
        "oya" to "ඔයා",
        "meya" to "මෙයා",
        "ape" to "අපේ",
        
        // Dental "d" (ද) colloquial overrides to prevent them rendering as retroflex "d" (ඩ)
        "dan" to "දැන්",
        "den" to "දැන්",
        "danna" to "දන්න",
        "dakka" to "දැක්කා",
        "dawasa" to "දවස",
        "dawasak" to "දවසක්",
        "deka" to "දෙක",
        "deke" to "දෙකේ",
        "dekam" to "දෙකම",
        "deyak" to "දෙයක්",
        "deval" to "දේවල්",
        "deela" to "දීලා",
        "dila" to "දීලා",
        "duwa" to "දුව",
        "dura" to "දුර",
        "dath" to "දත්",
        "diha" to "දිහා",
        "dala" to "දාලා",
        "dapan" to "දාපන්"
    )

    // Raw rules mapping of phonetic key combinations to Sinhala unicode characters
    private val rawRuleMap = mapOf(
        // Vowels (Independent, at start of word or after non-consonant)
        "aee" to "ෑ",
        "aae" to "ෑ",
        "ae" to "ඇ",
        "aa" to "ආ",
        "ii" to "ඊ",
        "uu" to "ඌ",
        "ee" to "ඒ",
        "oo" to "ඌ", // "oo" -> long 'u' sound (ඌ) in Helakuru
        "ow" to "ඕ",
        "au" to "ඖ",
        "ou" to "ඖ",
        "ei" to "ඓ",
        "ai" to "ඓ",
        "oi" to "ඔයි",
        "ui" to "උයි",
        "a" to "අ",
        "i" to "ඉ",
        "u" to "උ",
        "e" to "එ",
        "o" to "ඔ",
        "O" to "ඕ",
        "A" to "ඇ",
        "Aa" to "ඈ",
        "I" to "ඊ",
        "U" to "ඌ",
        "E" to "ඒ",

        // Sanyaka (Nasalized) Letters prefixed with 'z'
        "zg" to "ඟ",
        "zj" to "ඦ",
        "zd" to "ඬ",
        "zdh" to "ඳ",
        "zq" to "ඳ",
        "zk" to "ඤ",
        "zh" to "ඥ",

        // 3-Letter Consonants / Nasalized Conjuncts
        "ndh" to "න්ද",
        "ngh" to "ංග",
        "nnd" to "ණ්ඩ",
        "nnh" to "ණ්හ",
        "dhh" to "ධ",
        "thh" to "ථ",
        "nch" to "ඤ",

        // 2-Letter Consonants
        "kh" to "ඛ",
        "gh" to "ඝ",
        "ch" to "ච",
        "Ch" to "ඡ",
        "jh" to "ඣ",
        "th" to "ත", // dental
        "dh" to "ද", // dental
        "Th" to "ඨ", // retroflex aspirated
        "Dh" to "ඪ", // retroflex aspirated
        "ph" to "ඵ",
        "bh" to "භ",
        "sh" to "ෂ", // sh -> ෂ as requested
        "S" to "ශ",  // S -> ශ
        "kn" to "ඤ",
        "gn" to "ඥ",
        "mb" to "ඹ",
        "nj" to "ඤ",

        // 1-Letter Consonants
        "k" to "ක",
        "g" to "ග",
        "c" to "ච",
        "j" to "ජ",
        "t" to "ත", // Helakuru: t -> ත (dental)
        "d" to "ද", // Helakuru: d -> ද (dental)
        "T" to "ට", // Helakuru: T -> ට (retroflex)
        "D" to "ඩ", // Helakuru: D -> ඩ (retroflex)
        "n" to "න",
        "p" to "ප",
        "b" to "බ",
        "m" to "ම",
        "y" to "ය",
        "r" to "ර",
        "l" to "ල",
        "w" to "ව",
        "v" to "ව",
        "s" to "ස",
        "h" to "හ",
        "f" to "ෆ",
        "N" to "ණ",
        "L" to "ළ",
        "B" to "ඹ",

        // Special / Modifiers
        "x" to "ං", // binduva
        "z" to "ස්", // fallback for z alone
        "\\" to "්" // explicit killer
    )

    // Dependent vowel modifiers (pillam) when attached to a consonant
    private val rawModifierMap = mapOf(
        "aee" to "ෑ",
        "aae" to "ෑ",
        "ae" to "ැ",
        "aa" to "ා",
        "ii" to "ී",
        "uu" to "ූ",
        "ee" to "ේ",
        "oo" to "ූ", // "oo" -> ූ (long 'u' sound modifier) as requested
        "ow" to "ෝ",
        "au" to "ෞ",
        "ou" to "ෞ",
        "ai" to "ෛ",
        "ei" to "ෛ",
        "a" to "", // inherent vowel
        "i" to "ි",
        "u" to "ු",
        "e" to "ෙ",
        "o" to "ො",
        "O" to "ෝ",
        "A" to "ැ",
        "Aa" to "ෑ",
        "I" to "ී",
        "U" to "ූ",
        "E" to "ේ"
    )

    // Set of all keys that should be treated as consonants
    private val consonants = setOf(
        "k", "g", "c", "j", "t", "d", "n", "p", "b", "m", "y", "r", "l", "w", "v", "s", "h", "f",
        "kh", "gh", "ch", "Ch", "jh", "th", "dh", "ph", "bh", "sh", "ndh", "ngh", "nnd", "nnh", "dhh", "thh", "kn", "gn",
        "T", "D", "N", "L", "S", "mb", "nj", "z", "B", "zg", "zj", "zd", "zdh", "zq", "zk", "zh", "Th", "Dh"
    )

    // Pre-sorted rules for longest prefix matching
    private val sortedRules = rawRuleMap.entries
        .sortedByDescending { it.key.length }
        .map { Pair(it.key, it.value) }

    // Pre-sorted modifiers for longest prefix matching
    private val sortedModifiers = rawModifierMap.entries
        .sortedByDescending { it.key.length }
        .associate { it.key to it.value }

    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""

        // Check for colloquial dictionary overrides first
        val lowerInput = input.lowercase().trim()
        val colloquialOverride = colloquialDictionary[lowerInput]
        if (colloquialOverride != null) {
            return colloquialOverride
        }

        // 1. Tokenize input string using longest-prefix matching
        val tokens = ArrayList<Token>()
        var i = 0
        val len = input.length

        while (i < len) {
            val char = input[i]
            if (char.isWhitespace()) {
                tokens.add(Token(char.toString(), char.toString(), isConsonant = false, isWhitespace = true))
                i++
                continue
            }

            var matched = false
            for ((key, value) in sortedRules) {
                if (input.startsWith(key, i)) {
                    val isCons = consonants.contains(key)
                    tokens.add(Token(key, value, isConsonant = isCons, isWhitespace = false))
                    i += key.length
                    matched = true
                    break
                }
            }

            if (!matched) {
                val rawChar = char.toString()
                tokens.add(Token(rawChar, rawChar, isConsonant = false, isWhitespace = false))
                i++
            }
        }

        // 2. Process tokens sequentially to join consonants, vowels, and modifiers
        val result = StringBuilder()
        var idx = 0
        while (idx < tokens.size) {
            val current = tokens[idx]

            if (current.isConsonant) {
                var nextIdx = idx + 1

                // 2a. Auto-convert 'n' before 'k' or 'g' into binduva (ං)
                if (current.englishKey == "n" && nextIdx < tokens.size) {
                    // Skip any whitespaces to check the next consonant
                    var tempIdx = nextIdx
                    while (tempIdx < tokens.size && tokens[tempIdx].isWhitespace) {
                        tempIdx++
                    }
                    if (tempIdx < tokens.size) {
                        val nextCons = tokens[tempIdx].englishKey
                        if (nextCons == "k" || nextCons == "g") {
                            result.append("ං")
                            idx = tempIdx // Consume the 'n' and move to the target consonant
                            continue
                        }
                    }
                }

                // Skip whitespace to look for conjuncts/modifiers
                while (nextIdx < tokens.size && tokens[nextIdx].isWhitespace) {
                    nextIdx++
                }

                var hasYansaya = false
                var hasRakaransaya = false

                // 2b. Check if followed by 'y' or 'r' (yansaya / ra-karansaya conjuncts)
                if (nextIdx < tokens.size) {
                    val nextToken = tokens[nextIdx]
                    if (nextToken.englishKey == "y") {
                        hasYansaya = true
                        nextIdx++
                        while (nextIdx < tokens.size && tokens[nextIdx].isWhitespace) {
                            nextIdx++
                        }
                    } else if (nextToken.englishKey == "r") {
                        hasRakaransaya = true
                        nextIdx++
                        while (nextIdx < tokens.size && tokens[nextIdx].isWhitespace) {
                            nextIdx++
                        }
                    }
                }

                // 2c. Check if followed by a vowel modifier (ignoring spaces)
                var modifier = ""
                var modifierFound = false
                if (nextIdx < tokens.size) {
                    val nextToken = tokens[nextIdx]
                    val mod = sortedModifiers[nextToken.englishKey]
                    if (mod != null) {
                        modifier = mod
                        modifierFound = true
                        nextIdx++
                    }
                }

                // 2d. Construct output for this consonant cluster
                val baseSinhala = current.sinhalaValue
                if (hasYansaya) {
                    // Yansaya: C + ් + ZWJ + ය + modifier
                    result.append(baseSinhala).append("්").append("\u200D").append("ය")
                    if (modifierFound) {
                        result.append(modifier)
                    }
                } else if (hasRakaransaya) {
                    // Rakaransaya: C + ් + ZWJ + ර + modifier
                    result.append(baseSinhala).append("්").append("\u200D").append("ර")
                    if (modifierFound) {
                        result.append(modifier)
                    }
                } else {
                    // Normal consonant
                    result.append(baseSinhala)
                    if (modifierFound) {
                        result.append(modifier)
                    } else {
                        // Trailing or standalone consonant gets Hal Kireema
                        result.append("්")
                    }
                }

                // Advance idx
                if (hasYansaya || hasRakaransaya || modifierFound) {
                    idx = nextIdx
                } else {
                    idx++
                }

            } else if (current.englishKey == "\\") {
                // Explicit killer: Apply Hal Kireema if not already present
                if (!result.endsWith("්")) {
                    result.append("්")
                }
                idx++
            } else {
                // Non-consonant, non-killer token (e.g. space, independent vowel, symbol)
                result.append(current.sinhalaValue)
                idx++
            }
        }

        return result.toString()
    }

    private data class Token(
        val englishKey: String,
        val sinhalaValue: String,
        val isConsonant: Boolean,
        val isWhitespace: Boolean
    )
}
