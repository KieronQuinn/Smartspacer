package com.kieronquinn.app.smartspacer.sdksample.plugin

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.kieronquinn.app.smartspacer.sdksample.R

class SampleSetupActivity: Activity() {

    private val providerId
        get() = intent.getStringExtra(com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_setup)
        findViewById<TextView>(R.id.sample_setup_id).text = "Provider ID: $providerId"
        findViewById<Button>(R.id.sample_setup_add).setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
        findViewById<Button>(R.id.sample_setup_cancel).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

}