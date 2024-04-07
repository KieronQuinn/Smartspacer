package com.kieronquinn.app.smartspacer.sdk.client.views.features

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.kieronquinn.app.smartspacer.sdk.client.R
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageFeatureDoorbellBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.isLoadable
import com.kieronquinn.app.smartspacer.sdk.client.utils.setAspectRatio
import com.kieronquinn.app.smartspacer.sdk.client.utils.whenResumed
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.DoorbellState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SmartspacerDoorbellFeaturePageView(context: Context): SmartspacerBaseFeaturePageView<SmartspacePageFeatureDoorbellBinding>(
    context,
    SmartspacePageFeatureDoorbellBinding::inflate
) {

    override val title by lazy {
        binding.smartspacePageFeatureBasicTitle
    }

    override val subtitle by lazy {
        SubtitleBinding.SubtitleOnly(binding.smartspacePageFeatureBasicSubtitle)
    }

    private var uris: List<Uri>? = null
    private var delay: Long? = null
    private var loopJob: Job? = null

    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int,
        applyShadow: Boolean
    ) = with(binding) {
        super.setTarget(target, interactionListener, tintColour, applyShadow)
        loopJob?.cancel()
        val state = DoorbellState.fromTarget(target) ?: return
        val tint = ColorStateList.valueOf(tintColour)
        smartspacePageDoorbell.displayedChild = state.index
        when(state){
            is DoorbellState.LoadingIndeterminate -> {
                smartspacePageDoorbellLoadingIndeterminate.setSize(state.width, state.height)
                smartspacePageDoorbellLoadingIndeterminateContainer.setAspectRatio(
                    R.id.smartspace_page_doorbell_loading_indeterminate,
                    aspectWidth = state.ratioWidth,
                    aspectHeight = state.ratioHeight
                )
            }
            is DoorbellState.Loading -> {
                smartspacePageDoorbellLoadingImage.setSize(state.width, state.height)
                smartspacePageDoorbellLoadingProgress.setSize(state.width, state.height)
                smartspacePageDoorbellLoadingContainer.setAspectRatio(
                    R.id.smartspace_page_doorbell_loading_progress,
                    aspectWidth = state.ratioWidth,
                    aspectHeight = state.ratioHeight
                )
                smartspacePageDoorbellLoadingContainer.setAspectRatio(
                    R.id.smartspace_page_doorbell_loading_image,
                    aspectWidth = state.ratioWidth,
                    aspectHeight = state.ratioHeight
                )
                val icon = Icon.createWithBitmap(state.icon).apply {
                    setTintList(if(state.tint) tint else null)
                }
                smartspacePageDoorbellLoadingImage.setImageIcon(icon)
                smartspacePageDoorbellLoadingProgress.isVisible = state.showProgressBar
            }
            is DoorbellState.Videocam -> {
                smartspacePageDoorbellVideocam.setSize(state.width, state.height)
                smartspacePageDoorbellVideocamContainer.setAspectRatio(
                    R.id.smartspace_page_doorbell_videocam,
                    aspectWidth = state.ratioWidth,
                    aspectHeight = state.ratioHeight
                )
            }
            is DoorbellState.VideocamOff -> {
                smartspacePageDoorbellVideocamOff.setSize(state.width, state.height)
                smartspacePageDoorbellVideocamOffContainer.setAspectRatio(
                    R.id.smartspace_page_doorbell_videocam_off,
                    aspectWidth = state.ratioWidth,
                    aspectHeight = state.ratioHeight
                )
            }
            is DoorbellState.ImageBitmap -> {
                smartspacePageDoorbellImageBitmap.setSize(state.imageWidth, state.imageHeight)
                smartspacePageDoorbellImageBitmap.scaleType = state.imageScaleType
                    ?: ImageView.ScaleType.FIT_CENTER
                smartspacePageDoorbellImageBitmap.setImageBitmap(state.bitmap)
            }
            is DoorbellState.ImageUri -> {
                uris = state.imageUris
                delay = state.frameDurationMs.toLong()
                loopImages()
            }
        }
    }

    private fun View.setSize(width: Int?, height: Int?) {
        updateLayoutParams<ConstraintLayout.LayoutParams> {
            this.width = width ?: 0
            this.height = height ?: 0
        }
    }

    override fun onResume() {
        super.onResume()
        loopImages()
    }

    override fun onPause() {
        super.onPause()
        loopJob?.cancel()
    }

    @Synchronized
    private fun loopImages() {
        loopJob?.cancel()
        whenResumed {
            val uris = uris ?: return@whenResumed
            val delay = delay ?: return@whenResumed
            while(true){
                uris.forEach {
                    if(it.isLoadable()) {
                        binding.smartspacePageDoorbell.isVisible = true
                        binding.smartspacePageDoorbellImageUri.setImageURI(it)
                    }else{
                        binding.smartspacePageDoorbell.isVisible = false
                    }
                    delay(delay)
                }
            }
        }.also {
            loopJob = it
        }
    }

}