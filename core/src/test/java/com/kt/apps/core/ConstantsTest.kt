package com.kt.apps.core

import com.kt.apps.core.utils.removeAllSpecialChars
import org.junit.Test
import java.text.Normalizer

class ConstantsTest {

    @Test
    fun normalize() {
        println(Normalizer.normalize("bình dương", Normalizer.Form.NFD)
            .replace("[^\\p{ASCII}]", "")
        )
        println(Normalizer.normalize("bình dương", Normalizer.Form.NFC))
        println(Normalizer.normalize("bình dương", Normalizer.Form.NFKC))
        println(Normalizer.normalize("bình dương", Normalizer.Form.NFKD))
    }

    @Test
    fun test() {
        Constants.mapChannel.forEach { t, u ->
            println(
                "\"${
                    t.lowercase().removeSuffix("hd")
                        .replace(" ", "")
                        .removeAllSpecialChars()
                        .replace(".", "")
                }\" to \"$u\","
            )
        }
    }
}