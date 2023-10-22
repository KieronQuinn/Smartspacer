package com.kieronquinn.app.smartspacer.ui.screens.root

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.setupWithNavigation
import com.kieronquinn.app.smartspacer.databinding.FragmentRootBinding
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.activities.MainActivityViewModel
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf

class RootFragment: BoundFragment<FragmentRootBinding>(FragmentRootBinding::inflate) {

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_root) as NavHostFragment
    }

    private val navigation by inject<RootNavigation>()

    private val activityViewModel by activityViewModel<MainActivityViewModel> {
        val skipSplash = requireActivity()
            .intent.getBooleanExtra(MainActivity.EXTRA_SKIP_SPLASH, false)
        parametersOf(skipSplash)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        whenCreated {
            setupStartDestination(
                activityViewModel.startDestination.firstNotNull(), savedInstanceState
            )
        }
    }

    private fun setupStartDestination(id: Int, savedInstanceState: Bundle?) {
        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph_root)
        graph.setStartDestination(id)
        navHostFragment.navController.setGraph(graph, savedInstanceState)
    }

    private fun setupNavigation() = whenCreated {
        navHostFragment.setupWithNavigation(navigation)
    }

}