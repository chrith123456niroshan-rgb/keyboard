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
    
    // Define phonetic mapping of consonant combinations, vowels, etc.
    private val ruleMap = LinkedHashMap<String, String>().apply {
        // Longest keys first to prevent partial matching
        
        // Vowels (independent, at start of word or after vowel)
        put("aee", "ෑ")
        put("aae", "ෑ")
        put("oow", "ඌ")
        put("uow", "ඌ")
        put("oov", "ඌ")
        put("uov", "ඌ")
        put("aaw", "ආ")
        put("aav", "ආ")
        put("ae", "ඇ")
        put("aa", "ආ")
        put("ii", "ඊ")
        put("uu", "ඌ")
        put("ee", "ඒ")
        put("oo", "ඕ")
        put("au", "ඖ")
        put("ou", "ඖ")
        put("ei", "ඓ")
        put("ai", "ඓ")
        put("oi", "ඔයි")
        put("ui", "උයි")
        
        put("a", "අ")
        put("i", "ඉ")
        put("u", "උ")
        put("e", "එ")
        put("o", "ඔ")
        
        // 3-Letter Consonants / Nasalized Conjuncts
        put("ndh", "න්ද")
        put("ngh", "ංග")
        put("nnd", "ණ්ඩ")
        put("nnh", "ණ්හ")
        put("dhh", "ධ")
        put("thh", "ථ")
        put("nch", "ඤ")
        
        // 2-Letter Consonants / Nasalized Conjuncts
        put("kh", "ඛ")
        put("gh", "ඝ")
        put("ch", "ච")
        put("Ch", "ඡ")
        put("jh", "ඣ")
        put("th", "ත")
        put("dh", "ද")
        put("ph", "ඵ")
        put("bh", "භ")
        put("sh", "ශ")
        put("kn", "ඥ")
        put("gn", "ඥ")
        put("nd", "ඳ")
        put("ng", "ඟ")
        put("mb", "ඹ")
        put("nj", "ඤ")
        
        // 1-Letter Consonants
        put("k", "ක")
        put("g", "ග")
        put("c", "ච")
        put("j", "ජ")
        put("t", "ට") // standard Singlish: t -> ට (hard)
        put("d", "ඩ") // standard Singlish: d -> ඩ (hard)
        put("n", "න")
        put("p", "ප")
        put("b", "බ")
        put("m", "ම")
        put("y", "ය")
        put("r", "ර")
        put("l", "ල")
        put("w", "ව")
        put("v", "ව")
        put("s", "ස")
        put("h", "හ")
        put("f", "ෆ")
        
        put("N", "ණ")
        put("L", "ළ")
        put("T", "ට")
        put("D", "ඩ")
        put("S", "ෂ")
        
        // Direct modifier binds
        put("x", "ං") // x maps directly to binduva
        put("z", "ස්")
    }

    // Modifiers (dependent vowels) mapping when attached to a consonant
    private val modifierMap = LinkedHashMap<String, String>().apply {
        put("aee", "ෑ")
        put("aae", "ෑ")
        put("ae", "ැ")
        put("aa", "ා")
        put("ii", "ී")
        put("uu", "ූ")
        put("ee", "ේ")
        put("oo", "ෝ")
        put("au", "ෞ")
        put("ou", "ෞ")
        put("ei", "ෛ")
        put("ai", "ෛ")
        put("a", "") // inherent vowel
        put("i", "ි")
        put("u", "ු")
        put("e", "ෙ")
        put("o", "ො")
    }

    // List of consonants to identify when to apply modifiers or hal-kireema
    private val consonants = setOf(
        "k", "g", "c", "j", "t", "d", "n", "p", "b", "m", "y", "r", "l", "w", "v", "s", "h", "f",
        "kh", "gh", "ch", "Ch", "jh", "th", "dh", "ph", "bh", "sh", "ndh", "ngh", "nnd", "nnh", "dhh", "thh", "kn", "gn",
        "T", "D", "N", "L", "S", "nd", "ng", "mb", "nj", "z"
    )

    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""

        // Check for colloquial dictionary overrides first
        val lowerInput = input.lowercase().trim()
        val colloquialOverride = colloquialDictionary[lowerInput]
        if (colloquialOverride != null) {
            return colloquialOverride
        }

        val result = StringBuilder()
        var i = 0
        val len = input.length
        
        // Parse input into phonetic tokens
        val tokens = mutableListOf<Token>()

        while (i < len) {
            var matched = false
            for ((key, value) in ruleMap) {
                if (input.startsWith(key, i)) {
                    val isCons = consonants.contains(key)
                    tokens.add(Token(key, value, isCons))
                    i += key.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                // Keep raw non-matching characters
                val rawChar = input[i].toString()
                tokens.add(Token(rawChar, rawChar, false))
                i++
            }
        }

        // Process tokens to attach diacritics, conjuncts, and hal-kireema
        var idx = 0
        while (idx < tokens.size) {
            val current = tokens[idx]
            
            if (current.isConsonant) {
                var nextIdx = idx + 1
                var hasYansaya = false
                var hasRakaransaya = false
                
                // 1. Auto-convert 'n' before 'k' or 'g' into binduva (ං)
                if (current.englishKey == "n" && nextIdx < tokens.size) {
                    val next = tokens[nextIdx]
                    if (next.englishKey == "k" || next.englishKey == "g") {
                        result.append("ං")
                        idx++
                        continue
                    }
                }

                // 2. Check if followed by 'y' or 'r' (yansaya / ra-karansaya conjuncts)
                if (nextIdx < tokens.size) {
                    val next = tokens[nextIdx]
                    if (next.englishKey == "y") {
                        hasYansaya = true
                        nextIdx++
                    } else if (next.englishKey == "r") {
                        hasRakaransaya = true
                        nextIdx++
                    }
                }
                
                // 3. Check if followed by a vowel modifier
                var modifier = ""
                var vowelConsumed = false
                if (nextIdx < tokens.size) {
                    val next = tokens[nextIdx]
                    val mod = modifierMap[next.englishKey]
                    if (mod != null) {
                        modifier = mod
                        vowelConsumed = true
                        nextIdx++
                    }
                }
                
                // 4. Construct output for this consonant cluster
                val baseSinhala = current.sinhalaValue
                if (hasYansaya) {
                    // Yansaya: C + ් + ZWJ + ය + modifier
                    result.append(baseSinhala).append("්").append("\u200D").append("ය")
                    if (vowelConsumed) {
                        result.append(modifier)
                    }
                } else if (hasRakaransaya) {
                    // Rakaransaya: C + ් + ZWJ + ර + modifier
                    result.append(baseSinhala).append("්").append("\u200D").append("ර")
                    if (vowelConsumed) {
                        result.append(modifier)
                    }
                } else {
                    // Normal consonant
                    result.append(baseSinhala)
                    if (vowelConsumed) {
                        result.append(modifier)
                    } else {
                        // Trailing consonant gets hal-kireema
                        result.append("්")
                    }
                }
                
                idx = nextIdx
            } else {
                result.append(current.sinhalaValue)
                idx++
            }
        }

        return result.toString()
    }

    private data class Token(val englishKey: String, val sinhalaValue: String, val isConsonant: Boolean)
}
