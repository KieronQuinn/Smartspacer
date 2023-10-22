package com.kieronquinn.app.smartspacer.ui.screens.targets.requirements

import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.RequirementHolder
import com.kieronquinn.app.smartspacer.ui.views.LifecycleAwareRecyclerView

class TargetsRequirementsPageAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<RequirementHolder>,
    onConfigureClicked: (item: RequirementHolder) -> Unit,
    onDeleteClicked: (item: RequirementHolder) -> Unit,
    onInvertClicked: (item: RequirementHolder) -> Unit
): BaseRequirementsAdapter(recyclerView, items, onConfigureClicked, onDeleteClicked, onInvertClicked)