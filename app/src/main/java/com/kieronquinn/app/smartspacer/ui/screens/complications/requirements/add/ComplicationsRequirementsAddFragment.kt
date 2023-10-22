package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRequirementsAddBinding
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add.ComplicationsRequirementsAddViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMarginShort
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.resolveActivityCompat
import com.kieronquinn.app.smartspacer.utils.extensions.setClassLoaderToPackage
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ComplicationsRequirementsAddFragment: BoundFragment<FragmentRequirementsAddBinding>(FragmentRequirementsAddBinding::inflate), BackAvailable {

    companion object {
        const val REQUEST_KEY_REQUIREMENTS_ADD = "requirements_add"
        const val RESULT_KEY_PACKAGE_NAME = "package_name"
        const val RESULT_KEY_AUTHORITY = "authority"
        const val RESULT_KEY_ID = "id"
        const val RESULT_KEY_PAGE_TYPE = "page_type"
    }

    private var cachedRequirement: CachedRequirement? = null
    private val args by navArgs<ComplicationsRequirementsAddFragmentArgs>()

    private val configurationResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        cachedRequirement?.let { requirement ->
            cachedRequirement = null
            applyRequirement(requirement.authority, requirement.id, requirement.packageName)
        }
    }
    
    private val viewModel by viewModel<ComplicationsRequirementsAddViewModel>()

    private val adapter by lazy {
        ComplicationsRequirementsAddAdapter(
            binding.requirementsAddRecyclerview,
            viewModel::onExpandClicked,
            ::onRequirementClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupMonet()
        setupEmpty()
        setupSearch()
        setupSearchClear()
        viewModel.setup(args.complicationId, args.pageType)
    }

    private fun setupMonet() {
        binding.loading.loadingProgress.applyMonet()
        binding.includeSearch.searchBox.applyMonet()
        binding.includeSearch.searchBox.backgroundTintList = ColorStateList.valueOf(
            monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(
                requireContext()
            )
        )
    }

    private fun setupRecyclerView() = with(binding.requirementsAddRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@ComplicationsRequirementsAddFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
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
                binding.loading.root.isVisible = true
                binding.requirementsAddEmpty.isVisible = false
                binding.requirementsAddLoaded.isVisible = false
            }
            is State.Loaded -> {
                binding.loading.root.isVisible = false
                binding.requirementsAddEmpty.isVisible = state.items.isEmpty()
                binding.requirementsAddLoaded.isVisible = true
                adapter.submitList(state.items)
            }
        }
    }

    private fun setupEmpty() = with(binding.requirementsAddEmptyLabel) {
        applyBottomNavigationMarginShort()
    }

    private fun setupSearch() {
        setSearchText(viewModel.getSearchTerm())
        whenResumed {
            binding.includeSearch.searchBox.onChanged().collect {
                viewModel.setSearchTerm(it?.toString() ?: "")
            }
        }
    }

    private fun setupSearchClear() = whenResumed {
        launch {
            viewModel.showSearchClear.collect {
                binding.includeSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.includeSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.includeSearch.searchBox.run {
            this.text?.let {
                it.clear()
                it.append(text)
            } ?: setText(text)
        }
    }

    private fun onRequirementClicked(
        authority: String,
        id: String,
        packageName: String,
        setupIntent: Intent?
    ) {
        cachedRequirement = null
        if(setupIntent == null){
            applyRequirement(authority, id, packageName)
            return
        }
        setupIntent.setClassLoaderToPackage(requireContext(), packageName)
        setupIntent.putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, id)
        setupIntent.putExtra(SmartspacerConstants.EXTRA_AUTHORITY, authority)
        if(requireContext().packageManager.resolveActivityCompat(setupIntent) == null){
            applyRequirement(authority, id, packageName)
            return
        }
        cachedRequirement = CachedRequirement(authority, id, packageName)
        try {
            configurationResult.launch(setupIntent)
        }catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.requirements_add_failed_to_launch_setup,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun applyRequirement(authority: String, id: String, packageName: String) {
        setFragmentResult(REQUEST_KEY_REQUIREMENTS_ADD, bundleOf(
            RESULT_KEY_AUTHORITY to authority,
            RESULT_KEY_PACKAGE_NAME to packageName,
            RESULT_KEY_ID to id,
            RESULT_KEY_PAGE_TYPE to args.pageType
        ))
        whenResumed {
            viewModel.dismiss()
        }
    }

    override fun onDestroyView() {
        binding.requirementsAddRecyclerview.adapter = null
        super.onDestroyView()
    }

    data class CachedRequirement(val authority: String, val id: String, val packageName: String)

}