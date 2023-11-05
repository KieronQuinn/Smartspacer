package com.kieronquinn.app.smartspacer.ui.screens.native.reconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.databinding.FragmentNativeReconnectBinding
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class NativeReconnectFragment: BoundFragment<FragmentNativeReconnectBinding>(FragmentNativeReconnectBinding::inflate), BackAvailable {

    companion object {
        private const val EXTRA_IS_FEEDBACK = "is_feedback"
        private const val EXTRA_JUST_RECONNECT = "just_reconnect"

        fun createIntent(
            context: Context,
            isFeedback: Boolean,
            justReconnect: Boolean = false
        ): Intent {
            return ConfigurationActivity.createIntent(
                context, NavGraphMapping.NATIVE_RECONNECT
            ).apply {
                putExtra(EXTRA_IS_FEEDBACK, isFeedback)
                putExtra(EXTRA_JUST_RECONNECT, justReconnect)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
    }

    private val viewModel by viewModel<NativeReconnectViewModel>()
    private val notificationRepository by inject<NotificationRepository>()

    override val backIcon: Int = R.drawable.ic_close

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationRepository.cancelNotification(NotificationId.RECONNECT_PROMPT)
        val isFeedback = requireActivity().intent
            .getBooleanExtra(EXTRA_IS_FEEDBACK, false)
        val justReconnect = requireActivity().intent
            .getBooleanExtra(EXTRA_JUST_RECONNECT, false)
        setupContent(isFeedback)
        setupReconnect()
        if(justReconnect) {
            binding.nativeReconnectButton.isVisible = false
            viewModel.onReconnectClicked()
        }
    }

    private fun setupContent(isFeedback: Boolean) = with(binding.nativeReconnectContent) {
        val content = if(isFeedback) {
            R.string.native_restart_feedback_loop_content
        }else{
            R.string.native_restart_disconnect_content
        }
        setText(content)
    }

    private fun setupReconnect() = with(binding.nativeReconnectButton) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onReconnectClicked()
            }
        }
    }

}