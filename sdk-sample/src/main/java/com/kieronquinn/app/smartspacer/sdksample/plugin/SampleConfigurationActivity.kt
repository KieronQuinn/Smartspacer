package com.kieronquinn.app.smartspacer.sdksample.plugin

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.kieronquinn.app.smartspacer.sdksample.R

class SampleConfigurationActivity: Activity() {

    private val providerId
        get() = intent.getStringExtra(com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_configuration)
        findViewById<TextView>(R.id.sample_configuration_id).text = "Provider ID: $providerId"
    }

}