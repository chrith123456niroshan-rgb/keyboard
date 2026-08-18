package com.sintrans.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class SinglishEngineTest {

    @Test
    fun testLongestPrefixMatchingAndConsonants() {
        // th -> ත
        assertEquals("ත්", SinglishEngine.transliterate("th"))
        assertEquals("ත", SinglishEngine.transliterate("tha"))
        assertEquals("තා", SinglishEngine.transliterate("thaa"))
        assertEquals("තා", SinglishEngine.transliterate("th aa"))

        // sh -> ෂ
        assertEquals("ෂ්", SinglishEngine.transliterate("sh"))
        assertEquals("ෂ", SinglishEngine.transliterate("sha"))

        // S -> ශ
        assertEquals("ශ", SinglishEngine.transliterate("Sa"))

        // t -> ත, d -> ද (dental by default)
        assertEquals("ත", SinglishEngine.transliterate("ta"))
        assertEquals("ද", SinglishEngine.transliterate("da"))

        // T -> ට, D -> ඩ (retroflex)
        assertEquals("ට", SinglishEngine.transliterate("Ta"))
        assertEquals("ඩ", SinglishEngine.transliterate("Da"))
    }

    @Test
    fun testVowelsAndModifiers() {
        // oo -> ූ (modifier) or ඌ (independent)
        assertEquals("කූ", SinglishEngine.transliterate("koo"))
        assertEquals("ඌ", SinglishEngine.transliterate("oo"))

        // ii -> ී (modifier) or ඊ (independent)
        assertEquals("කී", SinglishEngine.transliterate("kii"))
        assertEquals("ඊ", SinglishEngine.transliterate("ii"))

        // ee -> ේ (modifier) or ඒ (independent)
        assertEquals("කේ", SinglishEngine.transliterate("kee"))
        assertEquals("ඒ", SinglishEngine.transliterate("ee"))
    }

    @Test
    fun testNasalAndDoubleConsonants() {
        // nna -> න්න
        assertEquals("න්න", SinglishEngine.transliterate("nna"))

        // nka -> ංක
        assertEquals("ංක", SinglishEngine.transliterate("nka"))

        // nda -> න්ද
        assertEquals("න්ද", SinglishEngine.transliterate("nda"))

        // Double consonants (e.g. kka -> ක්ක)
        assertEquals("ක්ක", SinglishEngine.transliterate("kka"))
    }

    @Test
    fun testSanyakaLetters() {
        // zga -> ඟ
        assertEquals("ඟ", SinglishEngine.transliterate("zga"))

        // zdha -> ඳ
        assertEquals("ඳ", SinglishEngine.transliterate("zdha"))

        // zqa -> ඳ
        assertEquals("ඳ", SinglishEngine.transliterate("zqa"))

        // zda -> ඬ
        assertEquals("ඬ", SinglishEngine.transliterate("zda"))
    }

    @Test
    fun testExplicitKillers() {
        // Explicit killer \ -> ්
        assertEquals("ක්", SinglishEngine.transliterate("k\\"))
        assertEquals("ක්", SinglishEngine.transliterate("ka\\"))
        assertEquals("ක්ක්", SinglishEngine.transliterate("kka\\")) // wait, kka\ -> "ක්ක" + \ -> "ක්ක්"? No: k -> ක්, k -> ක්, a -> ක, \ -> explicit killer -> ක්ක්? Wait! Let's check kka\ :
        // tokens are k, k, a, \.
        // k is consonant. Followed by k. Gets ක්.
        // k is consonant. Followed by a. Gets ක.
        // \ is processed. Appends ් to ක. Result becomes ක්ක්!
        // Wait, is that correct? Yes, because k + k + a + \ = ක + ් + ක + ් = ක්ක්.
        // Let's verify: ka\ -> ක්.
        assertEquals("ක්", SinglishEngine.transliterate("ka\\"))
    }

    @Test
    fun testColloquialDictionary() {
        assertEquals("වැඩ", SinglishEngine.transliterate("weda"))
        assertEquals("බෑ", SinglishEngine.transliterate("be"))
        assertEquals("නෑ", SinglishEngine.transliterate("na"))
    }
}
