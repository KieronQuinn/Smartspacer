package com.kieronquinn.app.smartspacer.ui.screens.shizuku

import androidx.lifecycle.ViewModel
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository

abstract class ShizukuOutdatedBottomSheetViewModel: ViewModel() {

    abstract fun onIgnoreClicked()

}

class ShizukuOutdatedBottomSheetViewModelImpl(
    private val shizukuServiceRepository: ShizukuServiceRepository
): ShizukuOutdatedBottomSheetViewModel() {

    override fun onIgnoreClicked() = shizukuServiceRepository.onIgnoreVersion()

}