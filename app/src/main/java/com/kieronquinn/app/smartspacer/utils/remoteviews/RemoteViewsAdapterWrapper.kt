package com.kieronquinn.app.smartspacer.utils.remoteviews

import android.database.DataSetObserver
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViewsAdapter

class RemoteViewsAdapterWrapper(
    private val original: RemoteViewsAdapter
): RemoteViewsAdapter() {

    override fun getCount(): Int {
        return original.count
    }

    override fun getItem(i: Int): Any {
        return original.getItem(i)
    }

    override fun getItemId(i: Int): Long {
        return original.getItemId(i)
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
        return original.getView(i, view, viewGroup)
    }

    override fun areAllItemsEnabled(): Boolean {
        return original.areAllItemsEnabled()
    }

    override fun getAutofillOptions(): Array<CharSequence>? {
        return original.autofillOptions
    }

    override fun getViewTypeCount(): Int {
        return original.viewTypeCount
    }

    override fun hasStableIds(): Boolean {
        return original.hasStableIds()
    }

    override fun isEmpty(): Boolean {
        return original.isEmpty
    }

    override fun notifyDataSetInvalidated() {
        original.notifyDataSetInvalidated()
    }

    override fun notifyDataSetChanged() {
        original.notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return original.getItemViewType(position)
    }

    override fun equals(other: Any?): Boolean {
        return original == other
    }

    override fun isEnabled(position: Int): Boolean {
        return original.isEnabled(position)
    }

    override fun registerDataSetObserver(observer: DataSetObserver?) {
        return original.registerDataSetObserver(observer)
    }

    override fun setAutofillOptions(vararg options: CharSequence?) {
        original.setAutofillOptions(*options)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        original.unregisterDataSetObserver(observer)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return original.getDropDownView(position, convertView, parent)
    }

    override fun finalize() {
        original::class.java.getDeclaredMethod("finalize").apply {
            isAccessible = true
        }.invoke(original)
    }

}