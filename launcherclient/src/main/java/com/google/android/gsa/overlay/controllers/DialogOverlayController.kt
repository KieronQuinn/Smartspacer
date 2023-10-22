package com.google.android.gsa.overlay.controllers

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build.VERSION
import android.os.Bundle
import android.view.*
import android.view.accessibility.AccessibilityEvent
import com.google.android.gsa.overlay.base.DialogListeners

open class DialogOverlayController(context: Context?, theme: Int, dialogTheme: Int) :
    ContextThemeWrapper(context, theme), Window.Callback, DialogListeners {
    var windowManager: WindowManager? = null
    public val window: Window?
    private val dialogs = HashSet<DialogInterface>()
    var windowView: View? = null
    open fun onBackPressed() {}
    override fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.keyCode != 4 || keyEvent.action != 1 || keyEvent.isCanceled) {
            return window!!.superDispatchKeyEvent(keyEvent)
        }
        onBackPressed()
        return true
    }

    override fun dispatchKeyShortcutEvent(keyEvent: KeyEvent): Boolean {
        return window!!.superDispatchKeyShortcutEvent(keyEvent)
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        return window!!.superDispatchTouchEvent(motionEvent)
    }

    override fun dispatchTrackballEvent(motionEvent: MotionEvent): Boolean {
        return window!!.superDispatchTrackballEvent(motionEvent)
    }

    override fun dispatchGenericMotionEvent(motionEvent: MotionEvent): Boolean {
        return window!!.superDispatchGenericMotionEvent(motionEvent)
    }

    override fun dispatchPopulateAccessibilityEvent(accessibilityEvent: AccessibilityEvent): Boolean {
        return false
    }

    override fun onCreatePanelView(i: Int): View? {
        return null
    }

    override fun onCreatePanelMenu(i: Int, menu: Menu): Boolean {
        return false
    }

    override fun onPreparePanel(p0: Int, p1: View?, p2: Menu): Boolean {
        return false
    }

    override fun onMenuOpened(i: Int, menu: Menu): Boolean {
        return true
    }

    override fun onMenuItemSelected(i: Int, menuItem: MenuItem): Boolean {
        return false
    }

    override fun onWindowAttributesChanged(layoutParams: WindowManager.LayoutParams) {
        if (windowView != null) {
            windowManager!!.updateViewLayout(windowView, layoutParams)
        }
    }

    override fun onContentChanged() {}
    override fun onWindowFocusChanged(z: Boolean) {}
    override fun onAttachedToWindow() {}
    override fun onDetachedFromWindow() {}
    override fun onPanelClosed(i: Int, menu: Menu) {}
    override fun onSearchRequested(): Boolean {
        return false
    }

    override fun onSearchRequested(searchEvent: SearchEvent): Boolean {
        return false
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback): ActionMode? {
        return null
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback, i: Int): ActionMode? {
        return null
    }

    override fun onActionModeStarted(actionMode: ActionMode) {}
    override fun onActionModeFinished(actionMode: ActionMode) {}
    override fun startActivity(intent: Intent) {
        super.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun startActivity(intent: Intent, bundle: Bundle?) {
        super.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), bundle)
    }

    override fun onShow(dialogInterface: DialogInterface) {
        dialogs.add(dialogInterface)
    }

    override fun onDismiss(dialogInterface: DialogInterface) {
        dialogs.remove(dialogInterface)
    }

    fun cnB() {
        if (!dialogs.isEmpty()) {
            val dialogArr = dialogs.toTypedArray()
            dialogs.clear()
            for (dismiss in dialogArr) {
                dismiss.dismiss()
            }
        }
    }

    init {
        val dialog = Dialog(context!!, dialogTheme)
        window = dialog.window
        window!!.callback = this
        val window = window
        if (VERSION.SDK_INT >= 21) {
            //window.setStatusBarColor(0);
            //window.setNavigationBarColor(0);
            window.addFlags(Int.MIN_VALUE)
        } else if (VERSION.SDK_INT >= 19) {
            window.addFlags(201326592)
        }
    }
}