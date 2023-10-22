package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils.TruncateAt
import android.text.style.StrikethroughSpan
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.*
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.TruncateLocation
import com.google.geo.sidekick.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.TapAction.ActionType
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.providers.SmartspacerOemIconProvider
import com.kieronquinn.app.smartspacer.receivers.DummyReceiver
import com.kieronquinn.app.smartspacer.receivers.SmartspacerOemClickReceiver
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.TrampolineActivity
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedFragment
import com.kieronquinn.app.smartspacer.utils.extensions.isWeather
import com.kieronquinn.app.smartspacer.utils.extensions.shouldTintIcon
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction as SmartspaceTapAction


class OemSmartspacerSession(
    private val context: Context,
    config: SmartspaceConfig,
    override val sessionId: SmartspaceSessionId,
    private val collectIntoExt: suspend (SmartspaceSessionId, List<SmartspaceCard?>) -> Unit
) : BaseSmartspacerSession<SmartspaceCard?, SmartspaceSessionId>(context, config, sessionId) {

    companion object {
        /*
         *  Cards are serialized as protobuf by launchers/SystemUI. In the event the user
         *  uninstalls Smartspacer, the last cards will stay. This duration (1 day) strikes a
         *  balance between cards potentially disappearing on devices that are left for a period of
         *  time, vs. the last cards clearing automatically if the user is unable to reset before
         *  uninstalling.
         */
        private val oneDayDuration = Duration.ofDays(1)
    }

    private val oemSmartspacerRepository by inject<OemSmartspacerRepository>()
    private val resendBus = MutableStateFlow(System.currentTimeMillis())

    override val targetCount = flowOf(10)

    override suspend fun collectInto(id: SmartspaceSessionId, targets: List<SmartspaceCard?>) {
        collectIntoExt(id, targets)
    }

    fun resendLastCards() = whenResumed {
        resendBus.emit(System.currentTimeMillis())
    }

    override fun toSmartspacerSessionId(id: SmartspaceSessionId) = id

    override fun convert(
        pages: Flow<List<SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<SmartspaceCard?>> {
        return combine(pages, uiSurface, resendBus) { it, surface, _ ->
            it.toSmartspaceCards(surface)
        }.flowOn(Dispatchers.IO)
    }

    override fun filterTargets(targets: Flow<List<TargetHolder>>): Flow<List<TargetHolder>> {
        return combine(
            targets,
            settings.oemHideIncompatible.asFlow()
        ) { t, h ->
            if(h){
                t.filterIncompatible()
            }else t
        }
    }

    private fun List<TargetHolder>.filterIncompatible(): List<TargetHolder> {
        return map {
            it.copy(targets = it.targets?.filterNot { target ->
                TargetTemplate.FEATURE_DENYLIST_BASIC.contains(target.featureType)
            })
        }.filterNot { it.targets.isNullOrEmpty() }
    }

    override fun onDestroy() {
        //Clear the cards from the client as they're stored in its cache
        runBlocking {
            collectIntoExt(sessionId, emptyList())
        }
        super.onDestroy()
    }

    private fun List<SmartspacePageHolder>.toSmartspaceCards(
        surface: UiSurface
    ): List<SmartspaceCard?> {
        val titlePage = firstOrNull { it.target != null }
        val actionPage = firstOrNull { it.actions.isNotEmpty() }
        return listOf(
            titlePage?.page?.toPrimarySmartspaceCard(surface),
            actionPage?.page?.toSecondarySmartspaceCard(surface)
        )
    }

    private fun SmartspaceTarget.toPrimarySmartspaceCard(surface: UiSurface): SmartspaceCard {
        val message = createPrimaryMessage()
        val actionId = templateData?.primaryItem?.tapAction?.id?.toString()
            ?: templateData?.subtitleItem?.tapAction?.id?.toString()
            ?: headerAction?.id
        return newBuilder()
            .setShouldDiscard(message == null)
            .setCardId(UUID.randomUUID().toString().hashCode())
            .setTapAction(createTargetTapAction(actionId))
            .setCardPriority(CardPriority.PRIMARY)
            .setCardType(CardType.CALENDAR)
            .setMessage(message)
            .setIcon(createPrimaryImage(surface))
            .setExpiryCriteria(createExpiryCriteria())
            .setEventTimeMillis(System.currentTimeMillis())
            .setEventDurationMillis(getDurationMillis())
            .build()
    }

    private fun SmartspaceTarget.toSecondarySmartspaceCard(surface: UiSurface): SmartspaceCard? {
        val message = createSecondaryMessage() ?: return null
        return newBuilder()
            .setShouldDiscard(false)
            .setCardId(UUID.randomUUID().toString().hashCode())
            .setTapAction(createActionTapAction())
            .setCardPriority(CardPriority.SECONDARY)
            .setCardType(CardType.WEATHER)
            .setMessage(message)
            .setImage(createSecondaryImage(surface))
            .setExpiryCriteria(createOneDayCriteria())
            .setEventTimeMillis(System.currentTimeMillis())
            .setEventDurationMillis(oneDayDuration.toMillis())
            .build()
    }

    private fun SmartspaceTarget.createTargetTapAction(actionId: String?): TapAction {
        val tapAction = templateData?.primaryItem?.tapAction
            ?: templateData?.subtitleItem?.tapAction
        val intent = tapAction?.intent ?: headerAction?.intent
        return when {
            templateData != null -> {
                ExpandedFragment.createOpenTargetUriCompatibleIntent(intent)?.let {
                    return it.toTapAction()
                }
                tapAction?.createTargetTapAction(this, tapAction.id.toString())
                    ?: createTargetClickTapAction(smartspaceTargetId, tapAction?.id?.toString())
            }
            headerAction != null -> {
                ExpandedFragment.createOpenTargetUriCompatibleIntent(intent)?.let {
                    return it.toTapAction()
                }
                headerAction?.createTargetTapAction(smartspaceTargetId)
                    ?: createTargetClickTapAction(smartspaceTargetId, headerAction?.id)
            }
            else -> {
                createTargetClickTapAction(smartspaceTargetId, actionId)
            }
        }
    }

    private fun SmartspaceTarget.createActionTapAction(): TapAction {
        return when {
            templateData != null -> {
                val tapAction = templateData?.primaryItem?.tapAction
                    ?: templateData?.subtitleItem?.tapAction
                tapAction?.createActionTapAction(tapAction.id.toString())
            }
            baseAction != null -> {
                baseAction?.createActionTapAction()
            }
            else -> null
        } ?: createBlankTapAction()
    }

    private fun SmartspaceTarget.createPrimaryMessage(): Message? {
        //Weather targets should not have a primary message as they show the dynamic date
        if(isWeather()) return null
        val title = templateData?.primaryItem?.text?.text ?: headerAction?.title
        val titleTruncation = templateData?.primaryItem?.text?.truncateAtType
        if(title.isNullOrEmpty()) return null
        val subtitle = templateData?.subtitleItem?.text?.text ?: headerAction?.subtitle
        val subtitleTruncation = templateData?.subtitleItem?.text?.truncateAtType
        return createMessage(title, titleTruncation, subtitle, subtitleTruncation)
    }

    private fun SmartspaceTarget.createSecondaryMessage(): Message? {
        //Weather targets use the subtitle item as their secondary as supplemental is the 2nd item
        val isWeather = isWeather()
        val title = if(!isWeather) {
            templateData?.subtitleSupplementalItem?.text?.text ?: baseAction?.subtitle
        }else{
            templateData?.subtitleItem?.text?.text ?: headerAction?.subtitle
        }
        if(title.isNullOrEmpty()) return null
        val titleTruncation = if(!isWeather){
            templateData?.subtitleSupplementalItem?.text?.truncateAtType
        }else{
            templateData?.subtitleItem?.text?.truncateAtType
        }
        return createMessage(title, titleTruncation, null, null)
    }

    private fun createMessage(
        title: CharSequence?,
        titleTruncateAt: TruncateAt?,
        subtitle: CharSequence?,
        subtitleTruncateAt: TruncateAt?,
    ): Message {
        val message = Message.newBuilder()
        title?.let {
            message.setTitle(createFormattedText(it, titleTruncateAt ?: TruncateAt.END))
        }
        subtitle?.let {
            message.setSubtitle(createFormattedText(it, subtitleTruncateAt ?: TruncateAt.END))
        }
        return message.build()
    }

    private fun createFormattedText(text: CharSequence, truncateAt: TruncateAt): FormattedText {
        return FormattedText.newBuilder()
            .setText(text.flattenToString())
            .setTruncateLocation(truncateAt.toTruncateLocation())
            .build()
    }

    /**
     *  Flattens a CharSequence to a string, if it contains [StrikethroughSpan]s, these will be
     *  converted into unicode-based strikethroughs since String doesn't support spans. Other
     *  Spannable types are not supported.
     */
    private fun CharSequence.flattenToString(): String {
        if(this !is Spannable) return this.toString()
        val spannableString = SpannableString(this)
        val out = StringBuilder()
        var next: Int
        var i = 0
        while (i < spannableString.length) {
            next = spannableString.nextSpanTransition(i, length, StrikethroughSpan::class.java)
            val isStrikethrough = spannableString.getSpans(i, next, StrikethroughSpan::class.java)
                .isNotEmpty()
            val chars = if(isStrikethrough){
                substring(i, next).toCharArray().joinToString("̶") + "̶"
            }else{
                substring(i, next)
            }
            out.append(chars)
            i = next
        }
        return out.toString()
    }

    private fun TruncateAt?.toTruncateLocation(): TruncateLocation {
        return when(this) {
            TruncateAt.START -> TruncateLocation.START
            TruncateAt.MIDDLE -> TruncateLocation.MIDDLE
            TruncateAt.END -> TruncateLocation.END
            TruncateAt.MARQUEE -> TruncateLocation.UNSPECIFIED
            null -> TruncateLocation.END
        }
    }

    private fun SmartspaceAction.createTargetTapAction(targetId: String?): TapAction {
        val launchIntent = TrampolineActivity.createUriTrampolineIntent(
            context,
            oemSmartspacerRepository.token,
            actionId = id,
            targetId = targetId
        )
        return TapAction.newBuilder()
            .setIntent(launchIntent.toUri(0))
            .setActionType(ActionType.START_ACTIVITY)
            .build()
    }

    private fun SmartspaceAction.createActionTapAction(): TapAction {
        val launchIntent = TrampolineActivity.createUriTrampolineIntent(
            context,
            oemSmartspacerRepository.token,
            actionId = id
        )
        return TapAction.newBuilder()
            .setIntent(launchIntent.toUri(0))
            .setActionType(ActionType.START_ACTIVITY)
            .build()
    }

    private fun SmartspaceTapAction.createTargetTapAction(
        target: SmartspaceTarget,
        actionId: String
    ): TapAction {
        val launchIntent = TrampolineActivity.createUriTrampolineIntent(
            context,
            oemSmartspacerRepository.token,
            targetId = target.smartspaceTargetId,
            actionId = actionId
        )
        return launchIntent.toTapAction()
    }

    private fun SmartspaceTapAction.createActionTapAction(
        actionId: String
    ): TapAction {
        val launchIntent = TrampolineActivity.createUriTrampolineIntent(
            context,
            oemSmartspacerRepository.token,
            targetId = null,
            actionId = actionId
        )
        return launchIntent.toTapAction()
    }

    private fun Intent.toTapAction(): TapAction {
        return TapAction.newBuilder()
            .setIntent(this.toUri(0))
            .setActionType(ActionType.START_ACTIVITY)
            .build()
    }

    private fun createBlankTapAction(): TapAction {
        return TapAction.newBuilder()
            .setIntent(Intent(context, DummyReceiver::class.java).toUri(0))
            .setActionType(ActionType.BROADCAST)
            .build()
    }

    private fun createTargetClickTapAction(targetId: String, actionId: String? = null): TapAction {
        val widgetClickIntent = Intent(
            context, SmartspacerOemClickReceiver::class.java
        ).apply {
            putExtra(SmartspacerOemClickReceiver.WIDGET_CLICK_KEY_TARGET_ID, targetId)
            actionId?.let {
                putExtra(SmartspacerOemClickReceiver.WIDGET_CLICK_KEY_ACTION_ID, it)
            }
        }.toUri(0)
        val launchIntent = TrampolineActivity.createUriTrampolineIntent(
            context,
            oemSmartspacerRepository.token,
            uriIntent = widgetClickIntent
        ).toUri(0)
        return TapAction.newBuilder()
            .setIntent(launchIntent)
            .setActionType(ActionType.BROADCAST)
            .build()
    }

    private fun createOneDayCriteria(): ExpiryCriteria {
        val expireAt = ZonedDateTime.now().plus(oneDayDuration).toInstant().toEpochMilli()
        return ExpiryCriteria.newBuilder()
            .setExpirationTimeMillis(expireAt)
            .setMaxImpressions(Integer.MAX_VALUE)
            .build()
    }

    private fun SmartspaceTarget.createExpiryCriteria(): ExpiryCriteria {
        val expireAt = ZonedDateTime.now().plus(oneDayDuration).toInstant().toEpochMilli()
        return ExpiryCriteria.newBuilder()
            .setExpirationTimeMillis(expireAt)
            .setMaxImpressions(Integer.MAX_VALUE)
            .build()
    }

    private fun SmartspaceTarget.getDurationMillis(): Long {
        return oneDayDuration.toMillis()
    }

    private fun SmartspaceTarget.createPrimaryImage(surface: UiSurface): Image {
        val actionId = headerAction?.id ?: templateData?.primaryItem?.tapAction?.id?.toString()
        val uri = SmartspacerOemIconProvider.createTargetUri(
            smartspaceTargetId,
            actionId,
            surface,
            templateData?.primaryItem?.icon?.shouldTint
                ?: templateData?.subtitleItem?.icon?.shouldTint
                ?: headerAction?.shouldTintIcon() ?:false
        )
        return createImage(uri)
    }

    private fun SmartspaceTarget.createSecondaryImage(surface: UiSurface): Image? {
        //Weather targets use the subtitle item as their secondary as supplemental is the 2nd item
        val action = if(isWeather()){
            headerAction
        }else{
            baseAction
        }  ?: return null
        val uri = SmartspacerOemIconProvider.createActionUri(
            action.id, surface, !isWeather() && action.shouldTintIcon()
        )
        return createImage(uri)
    }

    private fun createImage(uri: Uri): Image {
        /*
            Hack: Some third party reverse engineered launchers have Uri and Gsa Resource Name in
            the wrong order. Since the Uri field is loaded first, setting them both to the Uri is
            enough to cover this issue, since we never use the Gsa Resource Name field for its
            intended purpose.
         */
        return Image.newBuilder()
            .setUri(uri.toString())
            .setGsaResourceName(uri.toString())
            .build()
    }

    private fun Builder.setImage(image: Image?) = apply {
        if(image != null) {
            icon = image
        }
    }

    private fun Builder.setMessage(message: Message?) = apply {
        if(message != null){
            preEvent = message
            duringEvent = message
            postEvent = message
        }
    }

    init {
        onCreate()
    }

}