package com.kieronquinn.app.smartspacer.ui.activities.safemode

import android.os.Bundle
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.monetcompat.app.MonetCompatActivity

class SafeModeActivity: MonetCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        whenCreated {
            monet.awaitMonetReady()
            setContentView(R.layout.activity_safe_mode)
        }
    }

}