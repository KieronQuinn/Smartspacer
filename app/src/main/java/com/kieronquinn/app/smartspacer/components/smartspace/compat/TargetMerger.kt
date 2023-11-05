package com.kieronquinn.app.smartspacer.components.smartspace.compat

import android.content.ComponentName
import android.os.Bundle
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.model.smartspace.ActionHolder
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspaceActionHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_ABOUT_INTENT
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_FEEDBACK_INTENT
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_HIDE_SUBTITLE_ON_AOD
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_HIDE_TITLE_ON_AOD
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction.Companion.KEY_EXTRA_SHOW_ON_LOCKSCREEN
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BasicTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.weather.WeatherData
import com.kieronquinn.app.smartspacer.utils.extensions.cloneWithUniqneness
import com.kieronquinn.app.smartspacer.utils.extensions.popOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.reformatBullet
import java.util.LinkedList
import java.util.UUID

/**
 *  Default merger:
 *  - Base Targets & Actions have unique IDs
 *  - Runs through Targets, adding available actions to any free spaces
 *  - Adds any remaining Actions to blank targets
 */
abstract class TargetMerger {

    companion object {
        /**
         *  Useful undocumented extras in Native Smartspace, which must be applied to the base
         *  action. Smartspacer will strip them from the given base and copy them from the header,
         *  since the base is often a different complication
         */
        private val UNDOCUMENTED_EXTRAS = arrayOf(
            KEY_EXTRA_SHOW_ON_LOCKSCREEN,
            KEY_EXTRA_HIDE_TITLE_ON_AOD,
            KEY_EXTRA_HIDE_SUBTITLE_ON_AOD,
            KEY_EXTRA_ABOUT_INTENT,
            KEY_EXTRA_FEEDBACK_INTENT
        )

        /**
         *  Prefix applied to blank (padding) targets. This allows extraction in Expanded Smartspace
         *  to use the grid layout if needed instead. The exclamation mark enforces no third party
         *  app could use this prefix since it is disallowed in package names.
         */
        const val BLANK_TARGET_PREFIX = "!blank"

        @Synchronized
        protected fun Bundle.stripUndocumentedExtras() {
            UNDOCUMENTED_EXTRAS.forEach { remove(it) }
        }
    }

    fun mergeTargetsAndActions(
        targets: List<TargetHolder>,
        actions: List<ActionHolder>,
        openMode: ExpandedOpenMode
    ): List<SmartspacePageHolder> {
        val uniqueTargets = makeTargetsUnique(targets)
        val uniqueActions = makeActionsUnique(actions)
        //Queue all the available actions, then add them to available base actions...
        val actionQueue = LinkedList(uniqueActions)
        return getSplitTargets(actionQueue) + uniqueTargets.mapNotNull {
            val pageActions = ArrayList<Action>()
            var target = it.target
            val headerAction = if(target.canTakeHeaderAction(actionQueue.peek())){
                actionQueue.pop()
            }else null
            if(headerAction != null){
                target = convertIfNeeded(target, headerAction.action)
                //Retain the title - which is only set if forcing two complications
                target.headerAction = headerAction.action.copy(
                    title = target.headerAction?.title ?: ""
                )
                target.templateData?.subtitleItem = headerAction.action.subItemInfo
                pageActions.add(headerAction.parent)
            }
            val baseAction = if(target.canTakeBaseAction(actionQueue.peek())) {
                actionQueue.pop()
            }else null
            if(baseAction != null) {
                target = convertIfNeeded(target, baseAction.action)
                target.baseAction = baseAction.action.copyWithTargetExtras(target)
                target.templateData?.subtitleSupplementalItem = baseAction.action.subItemInfo
                pageActions.add(baseAction.parent)
            }
            //If the target has no actions and has asked to be hidden with no actions, return null
            if(target.hideIfNoComplications && target.hasNoActions()){
                return@mapNotNull null
            }
            //Copy undocumented extras from header to base, stripping it of any erroneously included
            target.reformatBulletIfNeeded(pageActions.size)
            SmartspacePageHolder(
                target,
                it.parent,
                pageActions
            )
        }.toMutableList().also {
            //...and add padding targets with the remaining ones
            it.addRemainingActions(actionQueue, openMode)
        }.map {
            it.convert()
        }
    }

    /**
     *  Moves a Target's extras to this Action if available. This allows keeping dismiss intents,
     *  feedback intents, special behaviour etc. when overwriting the base action.
     */
    private fun SmartspaceAction.copyWithTargetExtras(target: SmartspaceTarget): SmartspaceAction {
        return copy().apply {
            target.baseAction?.extras?.let {
                extras = Bundle(extras).apply {
                    putAll(it)
                }
            }
        }
    }

    private fun SmartspaceTarget.hasNoActions(): Boolean {
        if(!headerAction?.subtitle.isNullOrEmpty()) return false
        if(!baseAction?.subtitle.isNullOrEmpty()) return false
        if(!templateData?.subtitleItem?.text?.text.isNullOrEmpty()) return false
        if(!templateData?.subtitleSupplementalItem?.text?.text.isNullOrEmpty()) return false
        return true
    }

    private fun SmartspaceTarget.generateTemplateData(): BaseTemplateData {
        return BasicTemplateData(
            subtitleItem = headerAction?.generateSubItemInfo(),
            subtitleSupplementalItem = baseAction?.generateSubItemInfo()
        )
    }

    private fun makeTargetsUnique(
        targets: List<TargetHolder>
    ): List<SmartspaceRepository.SmartspaceTargetHolder> {
        return targets.mapNotNull {
            //Inject the parent to prefix the ID, enforcing uniqueness
            it.targets?.map { target ->
                SmartspaceRepository.SmartspaceTargetHolder(
                    target.cloneWithUniqneness(it.parent),
                    it.parent
                )
            }
        }.flatten()
    }

    private fun makeActionsUnique(
        smallActions: List<ActionHolder>
    ): List<SmartspaceActionHolder> {
        return smallActions.mapNotNull {
            //Inject the parent to prefix the ID, enforcing uniqueness
            it.actions?.map { action ->
                SmartspaceActionHolder(
                    action.cloneWithUniqneness(it.parent),
                    it.parent
                )
            }
        }.flatten()
    }

    private fun SmartspaceTarget.canTakeHeaderAction(holder: SmartspaceActionHolder?): Boolean {
        if(holder == null) return false //End of list
        //Override specified by target
        if(canTakeTwoComplications && featureType == SmartspaceTarget.FEATURE_UNDEFINED) return true
        //Weather target
        if(featureType == SmartspaceTarget.FEATURE_WEATHER){
            return headerAction?.subtitle.isNullOrEmpty() && templateData?.subtitleItem == null
        }
        return false
    }

    private fun SmartspaceTarget.canTakeBaseAction(holder: SmartspaceActionHolder?): Boolean {
        val action = holder?.action ?: return false //End of list
        //Override specified by target
        if(canTakeTwoComplications && featureType == SmartspaceTarget.FEATURE_UNDEFINED) return true
        val hasSubItemInfo = action.subItemInfo != null
        val hasNoSubtitle = action.subtitle.isNullOrEmpty()
        val targetHasNoTemplate = templateData == null
        val targetHasNoSubtitleSupplementalItem =
            templateData?.subtitleSupplementalItem?.text?.text.isNullOrEmpty()
        if(hasSubItemInfo && hasNoSubtitle && targetHasNoTemplate){
            //If the action is UI template only, and the target doesn't have template data, only
            //the FEATURE_WEATHER target can be converted, otherwise assume incompatible and abort
            return featureType == SmartspaceTarget.FEATURE_WEATHER
        }
        //Otherwise, return if there's no sub action set
        return baseAction?.id.isNullOrEmpty() && targetHasNoSubtitleSupplementalItem
    }

    private fun SmartspaceTarget.reformatBulletIfNeeded(actionsCount: Int) {
        headerAction = headerAction?.reformatBullet(actionsCount == 1)
        templateData?.subtitleItem =
            templateData?.subtitleItem?.reformatBullet(actionsCount == 1)
    }

    private fun MutableList<SmartspacePageHolder>.addRemainingActions(
        actionQueue: LinkedList<SmartspaceActionHolder>, openMode: ExpandedOpenMode
    ) {
        while(true){
            val action = actionQueue.popOrNull() ?: break
            val secondAction = actionQueue.popOrNull()
            val page = SmartspacePageHolder(
                createBlankTarget(action.action, secondAction?.action),
                null,
                listOfNotNull(action.parent, secondAction?.parent)
            )
            add(page)
        }
    }

    private fun convertIfNeeded(
        target: SmartspaceTarget,
        action: SmartspaceAction
    ): SmartspaceTarget = with(target) {
        //Action must have subItemInfo but no legacy subtitle
        val hasSubItemInfo = action.subItemInfo != null
        val hasNoSubtitle = action.subtitle.isNullOrEmpty()
        //Target must have no UI template set
        val targetHasNoTemplate = templateData == null
        //Target must be blank to guarantee convertibility
        val isWeather = featureType == SmartspaceTarget.FEATURE_WEATHER
        //Copy the base extras from the existing action (if it exists) to the new one
        val existingExtras = target.baseAction?.extras
        //Strip out the undocumented extras so a complication can't inject them
        action.extras.stripUndocumentedExtras()
        if(existingExtras?.isEmpty == false) {
            val extras = Bundle(action.extras)
            //Remove any existing weather data extras as they will be stale
            WeatherData.clearExtras(extras)
            extras.putAll(existingExtras)
            action.extras = extras
        }
        return if(hasSubItemInfo && hasNoSubtitle && targetHasNoTemplate && isWeather){
            copy(templateData = generateTemplateData())
        }else this
    }

    open fun getSplitTargets(actions: LinkedList<SmartspaceActionHolder>): List<SmartspacePageHolder> {
        return emptyList()
    }

    open fun SmartspacePageHolder.convert(): SmartspacePageHolder {
        return this
    }

    protected open val blankFeatureType = SmartspaceTarget.FEATURE_WEATHER

    /**
     *  Creates a blank target, with one or two [SmartspaceAction]s. If [base] is not specified,
     *  a blank base will be used. The launcher will surround these actions with the default
     *  date text.
     */
    protected fun createBlankTarget(
        header: SmartspaceAction,
        base: SmartspaceAction?,
        templateData: BasicTemplateData? = null
    ): SmartspaceTarget {
        //Strip undocumented extras from the complication so it can't inject them into the target
        base?.extras?.stripUndocumentedExtras()
        return SmartspaceTarget(
            smartspaceTargetId = "${BLANK_TARGET_PREFIX}_${UUID.randomUUID()}",
            headerAction = header.reformatBullet(base == null),
            baseAction = base ?: SmartspaceAction(
                id = "",
                title = ""
            ),
            featureType = blankFeatureType,
            componentName = ComponentName("package_name", "class_name"),
            templateData = BasicTemplateData(
                subtitleItem = header.subItemInfo ?: templateData?.subtitleItem
                    ?: header.generateSubItemInfo().reformatBullet(base == null),
                subtitleSupplementalItem = base?.subItemInfo
                    ?: templateData?.subtitleSupplementalItem
                    ?: base?.generateSubItemInfo()
            ),
            canBeDismissed = false
        )
    }

}