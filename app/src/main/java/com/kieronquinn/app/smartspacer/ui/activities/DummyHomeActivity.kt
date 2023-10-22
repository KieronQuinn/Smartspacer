package com.kieronquinn.app.smartspacer.ui.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.kieronquinn.app.smartspacer.databinding.ActivityDummyHomeBinding
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet

class DummyHomeActivity: BoundActivity<ActivityDummyHomeBinding>(ActivityDummyHomeBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.dummyHomeSettings.applyMonet()
        whenResumed {
            binding.dummyHomeSettings.onClicked().collect {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        }
    }

}