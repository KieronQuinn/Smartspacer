package com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class CombinedCardsTemplateData(
    val combinedCardDataList: List<BaseTemplateData>,
    override var layoutWeight: Int = 0,
    override var primaryItem: SubItemInfo? = null,
    override var subtitleItem: SubItemInfo? = null,
    override var subtitleSupplementalItem: SubItemInfo? = null,
    override var supplementalAlarmItem: SubItemInfo? = null,
    override var supplementalLineItem: SubItemInfo? = null
): BaseTemplateData(
    6,
    layoutWeight,
    primaryItem,
    subtitleItem,
    subtitleSupplementalItem,
    supplementalAlarmItem,
    supplementalLineItem
) {

    companion object {
        private const val KEY_COMBINED_CARD_DATA_LIST_TYPE = "combined_card_data_list_type"
        private const val KEY_COMBINED_CARD_DATA_LIST = "combined_card_data_list"

        /**
         *  Since we're putting the data list into a generic bundle, we lose the type of the items,
         *  so we have to send it separately and re-create the instances via reflection. This
         *  also handles empty lists (with no type), and version differences where the class may not
         *  be found, where we lose the list contents.
         */
        private fun Bundle.getCombinedCardDataList(): List<BaseTemplateData> {
            val typeName = getString(KEY_COMBINED_CARD_DATA_LIST_TYPE) ?: return emptyList()
            val type = try {
                Class.forName(typeName) as Class<out BaseTemplateData>
            }catch (e: ClassNotFoundException){
                return emptyList()
            }
            val constructor = type.getConstructor(Bundle::class.java)
            return getParcelableArrayListCompat(KEY_COMBINED_CARD_DATA_LIST, Bundle::class.java)!!.map {
                constructor.newInstance(it)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getCombinedCardDataList(),
        bundle.getInt(KEY_LAYOUT_WEIGHT),
        bundle.getBundle(KEY_PRIMARY_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUBTITLE_SUPPLEMENTAL_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_ALARM_ITEM)?.let { SubItemInfo(it) },
        bundle.getBundle(KEY_SUPPLEMENTAL_LINE_ITEM)?.let { SubItemInfo(it) }
    )

    override fun toBundle(): Bundle {
        val bundle = super.toBundle()
        val type = combinedCardDataList.firstOrNull()?.javaClass?.name
        bundle.putString(KEY_COMBINED_CARD_DATA_LIST_TYPE, type)
        bundle.putAll(bundleOf(
            KEY_COMBINED_CARD_DATA_LIST to ArrayList(combinedCardDataList.map { it.toBundle() })
        ))
        return bundle
    }

}
