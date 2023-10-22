package com.kieronquinn.app.smartspacer.utils.spans

import android.text.style.ForegroundColorSpan

/**
 *  This span type is used for one thing: Changing Target's foreground colours to adapt to the
 *  wallpaper. Since we need to clean up old spans so the Target size doesn't balloon, this is
 *  how we identify it - but when parceled it will become a regular [ForegroundColorSpan].
 */
class SmartspacerForegroundColorSpan(color: Int) : ForegroundColorSpan(color)