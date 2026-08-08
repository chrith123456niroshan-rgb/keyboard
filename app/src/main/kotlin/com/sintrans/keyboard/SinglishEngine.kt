package com.sintrans.keyboard

object SinglishEngine {
    
    // Define phonetic mapping of consonant consonant combinations, vowels, etc.
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
        
        // Special character modifications / conjuncts
        put("nch", "ඤ")
        put("ndh", "ඳ")
        put("ndj", "ඬ")
        put("ndw", "ඬ්ව")
        
        // Consonants
        put("kh", "ඛ")
        put("gh", "ඝ")
        put("ch", "ඡ")
        put("jh", "ඣ")
        put("th", "ත")
        put("dh", "ද")
        put("ph", "ඵ")
        put("bh", "භ")
        put("sh", "ශ")
        put("ss", "ෂ")
        
        put("kn", "ඥ")
        put("gn", "ඥ")
        
        put("k", "ක")
        put("g", "ග")
        put("c", "ච")
        put("j", "ජ")
        put("T", "ට") // Capital T for hard T
        put("D", "ඩ") // Capital D for hard D
        put("t", "ත")
        put("d", "ද")
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
        put("N", "ණ") // Capital N for hard N
        put("L", "ළ") // Capital L for hard L
        put("f", "ෆ")
        
        // Non-consonant characters
        put("x", "ක්ස්")
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
        "kh", "gh", "ch", "jh", "th", "dh", "ph", "bh", "sh", "ss", "nch", "ndh", "ndj", "ndw", "kn", "gn",
        "T", "D", "N", "L", "x", "z"
    )

    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""

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
                // Keep raw non-matching characters (numbers, punctuation, spaces)
                val rawChar = input[i].toString()
                tokens.add(Token(rawChar, rawChar, false))
                i++
            }
        }

        // Process tokens to attach diacritics and hal-kireema
        var idx = 0
        while (idx < tokens.size) {
            val current = tokens[idx]
            
            if (current.isConsonant) {
                // If followed by a vowel modifier, apply it
                if (idx + 1 < tokens.size) {
                    val next = tokens[idx + 1]
                    val modifier = modifierMap[next.englishKey]
                    if (modifier != null) {
                        result.append(current.sinhalaValue).append(modifier)
                        idx += 2
                        continue
                    }
                }
                // Otherwise, it is a trailing consonant, attach hal-kireema (්)
                result.append(current.sinhalaValue).append("්")
                idx++
            } else {
                result.append(current.sinhalaValue)
                idx++
            }
        }

        return result.toString()
    }

    private data class Token(val englishKey: String, val sinhalaValue: String, val isConsonant: Boolean)
}
