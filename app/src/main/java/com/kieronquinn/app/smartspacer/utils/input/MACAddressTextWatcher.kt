package com.kieronquinn.app.smartspacer.utils.input

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher

//Based on https://gist.github.com/hleinone/5b445e5475ca9f8a3bdc6a44998f4edd
class MACAddressTextWatcher : TextWatcher {

    companion object {
        //Colons should be filtered out as they are re-inserted
        private val ACCEPTABLE_CHARS = "abcdefABCDEF0123456789".toCharArray()
    }

    private var current = ""

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        //No-op
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        //No-op
    }

    override fun afterTextChanged(s: Editable) {
        if (s.toString() != current) {
            val userInput = s.toString().filter { ACCEPTABLE_CHARS.contains(it) }
            if (userInput.length <= 12) {
                current = userInput.chunked(2).joinToString(":")
                s.filters = arrayOfNulls<InputFilter>(0)
            }
            s.replace(0, s.length, current, 0, current.length)
        }
    }

}