package com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.maps.android.SphericalUtil
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.GeofenceRequirement.GeofenceRequirementData
import com.kieronquinn.app.smartspacer.databinding.FragmentGeofenceRequirementConfigurationBinding
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.GeofenceRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.*
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.sqrt

class GeofenceRequirementConfigurationFragment: BoundFragment<FragmentGeofenceRequirementConfigurationBinding>(FragmentGeofenceRequirementConfigurationBinding::inflate), BackAvailable, LockCollapsed {

    private val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(binding.geofenceRequirementBottomSheet)
    }

    private val toolbarColour by lazy {
        monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
    }

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onResumed()
    }

    private val id
        get() = requireActivity().intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)!!

    private val mapFragment by lazy {
        childFragmentManager
            .findFragmentById(R.id.geofence_requirement_configuration_map) as SupportMapFragment
    }

    private val viewModel by viewModel<GeofenceRequirementConfigurationViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        setupMonet()
        setupScroll()
        setupBottomSheet()
        setupBottomSheetRoundedCorners()
        setupNavBarBlocker()
        setupBottomSheetToggle()
        setupState()
        setupMap()
        setupBackPressed()
        setupGrantPermissionButton()
        setupSave()
        setupDismiss()
        viewModel.setupWithId(id)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumed()
    }

    private fun setupScroll() {
        binding.requirementGeofenceConfigurationBackgroundLocationPermission
            .isNestedScrollingEnabled = false
        binding.requirementGeofenceConfigurationLimitReached
            .isNestedScrollingEnabled = false
    }

    private fun setupMonet() {
        binding.geofenceRequirementLoading.loadingProgress.applyMonet()
        binding.requirementGeofenceConfigurationBackgroundLocationGrant.applyMonet()
    }

    private fun setupBottomSheet() {
        binding.geofenceRequirementBottomSheet.setCardBackgroundColor(
            monet.getBackgroundColor(requireContext())
        )
        binding.geofenceRequirementBottomSheet.elevation =
            resources.getDimension(R.dimen.margin_24)
        binding.root.onApplyInsets { _, insets ->
            bottomSheetBehavior.peekHeight = resources.getDimension(R.dimen.geofence_requirement_bottom_sheet_peek_size).toInt() +
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        }
    }

    private fun setupBottomSheetRoundedCorners() = with(binding.geofenceRequirementBottomSheet) {
        val radius = resources.getDimension(R.dimen.margin_16)
        shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
            setTopLeftCorner(CornerFamily.ROUNDED, radius)
            setTopRightCorner(CornerFamily.ROUNDED, radius)
        }.build()
    }

    private fun setupNavBarBlocker() = with(binding.geofenceRequirementBottomSheetNavBlocker) {
        setBackgroundColor(toolbarColour)
        binding.geofenceRequirementBottomSheet.onApplyInsets { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            viewModel.setBottomSheetInset(bottomInset.toFloat())
        }
        whenResumed {
            bottomSheetBehavior.slideOffset().collect {
                viewModel.setBottomSheetOffset(it)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setupBottomSheetToggle() {
        whenResumed {
            binding.geofenceRequirementBottomSheetToolbar.onClicked().collect {
                toggleBottomSheet()
            }
        }
        whenResumed {
            binding.geofenceRequirementBottomSheetArrow.onClicked().collect {
                toggleBottomSheet()
            }
        }
        whenResumed {
            viewModel.bottomSheetOffset.collect {
                val progress = (1f - it).coerceAtMost(1f).coerceAtLeast(0f)
                binding.geofenceRequirementBottomSheetArrow.rotation = progress * 180f
            }
        }
        binding.geofenceRequirementBottomSheetArrow.rotation =
            viewModel.bottomSheetOffset.value * 180f
    }

    @SuppressLint("RestrictedApi")
    private fun toggleBottomSheet() {
        val newState = when(bottomSheetBehavior.lastStableState){
            BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
            BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
            else -> return
        }
        bottomSheetBehavior.state = newState
    }


    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.geofenceRequirementLoading.root.isVisible = true
                binding.geofenceRequirementLoading.loadingLabel.setText(R.string.loading)
                binding.requirementGeofenceConfigurationBackgroundLocationPermission.isVisible = false
                binding.requirementGeofenceConfigurationLimitReached.isVisible = false
                binding.geofenceRequirementConfiguration.isVisible = false
            }
            is State.RequestPermission -> {
                binding.geofenceRequirementLoading.root.isVisible = false
                binding.geofenceRequirementLoading.loadingLabel.setText(R.string.loading)
                binding.requirementGeofenceConfigurationBackgroundLocationPermission.isVisible = true
                binding.requirementGeofenceConfigurationLimitReached.isVisible = false
                binding.geofenceRequirementConfiguration.isVisible = false
                permissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            is State.RequestBackgroundPermission -> {
                binding.geofenceRequirementLoading.root.isVisible = false
                binding.requirementGeofenceConfigurationBackgroundLocationPermission.isVisible = true
                binding.requirementGeofenceConfigurationLimitReached.isVisible = false
                binding.geofenceRequirementConfiguration.isVisible = false
            }
            is State.LimitReached -> {
                binding.geofenceRequirementLoading.root.isVisible = false
                binding.requirementGeofenceConfigurationBackgroundLocationPermission.isVisible = false
                binding.requirementGeofenceConfigurationLimitReached.isVisible = true
                binding.geofenceRequirementConfiguration.isVisible = false
            }
            is State.Loaded -> {
                binding.geofenceRequirementLoading.root.isVisible = false
                binding.requirementGeofenceConfigurationBackgroundLocationPermission.isVisible = false
                binding.requirementGeofenceConfigurationLimitReached.isVisible = false
                binding.geofenceRequirementConfiguration.isVisible = true
                binding.geofenceRequirementConfiguration.bringToFront()
            }
            is State.Saving -> {
                binding.geofenceRequirementLoading.root.isVisible = true
                binding.geofenceRequirementLoading.loadingLabel.setText(R.string.saving)
                binding.requirementGeofenceConfigurationBackgroundLocationPermission.isVisible = false
                binding.requirementGeofenceConfigurationLimitReached.isVisible = false
                binding.geofenceRequirementConfiguration.isVisible = false
            }
        }
    }

    private fun setupGrantPermissionButton() = with(binding.requirementGeofenceConfigurationBackgroundLocationGrant) {
        whenResumed {
            onClicked().collect {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                }.also {
                    startActivity(it)
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if(bottomSheetBehavior.lastStableState != BottomSheetBehavior.STATE_COLLAPSED){
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }else{
                requireActivity().finish()
            }
        }
    }

    private fun setupSave() = with(binding.geofenceRequirementConfigurationSave) {
        backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val bottomMargin = resources
            .getDimension(R.dimen.geofence_requirement_bottom_sheet_peek_size_with_margin).toInt()
        binding.geofenceRequirementConfigurationSave.onApplyInsets { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            updateLayoutParams<CoordinatorLayout.LayoutParams> {
                updateMargins(bottom = bottomMargin + bottomInset)
            }
        }
        whenResumed {
            onClicked().collect {
                viewModel.onSaveClicked()
            }
        }
    }

    private fun setupDismiss() = whenResumed {
        viewModel.dismissBus.collect {
            requireActivity().run {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun setupMap() = whenResumed {
        val loadedState = viewModel.state.filter { it is State.Loaded }
        val map = mapFragment.getMap().first()
        map.uiSettings.apply {
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
        }
        map.setPadding(
            0, 0, 0,
            resources.getDimension(R.dimen.geofence_requirement_map_padding).toInt()
        )
        launch {
            val data = loadedState.first() as State.Loaded //Await the map being loaded
            binding.geofenceRequirementConfigurationMap.awaitPost()
            map.setupInitialLocation(data.data)
            val marker = map.setupMarker()
            val circle = map.setupCircle()
            map.bindCircle(circle)
            map.bindMapMovement(circle, marker)
        }
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.setupInitialLocation(
        data: GeofenceRequirementData
    ) = whenResumed {
        isMyLocationEnabled = true
        bindInitialLocation(data)
    }

    private fun GoogleMap.bindInitialLocation(data: GeofenceRequirementData) {
        val northEast = SphericalUtil.computeOffset(
            data.getLatLng(),
            data.radius.toDouble() * sqrt(2.0),
            45.0
        )
        val southWest = SphericalUtil.computeOffset(
            data.getLatLng(),
            data.radius.toDouble() * sqrt(2.0),
            225.0
        )
        val bounds = LatLngBounds(southWest, northEast)
        moveCamera(
            CameraUpdateFactory
                .newLatLngBounds(bounds, resources.getDimension(R.dimen.margin_16).toInt())
        )
    }

    private fun GoogleMap.setupMarker(): Marker? {
        return MarkerOptions().apply {
            position(cameraPosition.target)
        }.let {
            addMarker(it)
        }
    }

    private fun GoogleMap.setupCircle(): Circle {
        val fillColour = if(requireContext().isDarkMode){
            monet.getAccentColor(requireContext())
        }else{
            monet.getPrimaryColor(requireContext())
        }.let {
            ColorUtils.setAlphaComponent(it, 127)
        }
        val strokeColour = if(requireContext().isDarkMode){
            monet.getPrimaryColor(requireContext())
        }else{
            monet.getAccentColor(requireContext())
        }
        return CircleOptions().apply {
            center(cameraPosition.target)
            radius(0.0)
            fillColor(fillColour)
            strokeColor(strokeColour)
        }.let {
            addCircle(it)
        }
    }

    private fun GoogleMap.bindCircle(
        circle: Circle
    ) = whenResumed {
        viewModel.radius.collect {
            circle.radius = it.toDouble()
            val northEast = SphericalUtil.computeOffset(
                cameraPosition.target,
                it * sqrt(2.0),
                45.0
            )
            val southWest = SphericalUtil.computeOffset(
                cameraPosition.target,
                it * sqrt(2.0),
                225.0
            )
            val bounds = LatLngBounds(southWest, northEast)
            moveCamera(
                CameraUpdateFactory
                    .newLatLngBounds(bounds, resources.getDimension(R.dimen.margin_16).toInt())
            )
        }
    }

    private fun GoogleMap.bindMapMovement(
        circle: Circle,
        marker: Marker?
    ) = whenResumed {
        onMapMoved().collect {
            viewModel.onLatLngChanged(it)
            marker?.position = it
            circle.center = it
        }
    }

    private fun SupportMapFragment.getMap() = callbackFlow {
        getMapAsync {
            trySend(it)
        }
        awaitClose {
            //No-op
        }
    }

    private fun GoogleMap.onMapMoved() = callbackFlow {
        setOnCameraMoveListener {
            trySend(cameraPosition.target)
        }
        trySend(cameraPosition.target)
        awaitClose {
            //No-op
        }
    }

}