package com.kieronquinn.app.smartspacer.sdksample.plugin.ui.client

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.kieronquinn.app.smartspacer.sdk.client.views.BcSmartspaceView
import com.kieronquinn.app.smartspacer.sdksample.R

class SampleClientFragment: Fragment(R.layout.fragment_sample_client) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val page = view.findViewById<BcSmartspaceView>(R.id.bc_smartspace_view)
        val showShadowSwitch = view.findViewById<SwitchCompat>(R.id.switch_show_shadow)
        showShadowSwitch.setOnCheckedChangeListener { _, isChecked ->
            page.setApplyShadowIfRequired(isChecked)
        }
    }

}