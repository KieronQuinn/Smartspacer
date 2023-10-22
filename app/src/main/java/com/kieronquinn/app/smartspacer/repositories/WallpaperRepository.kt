package com.kieronquinn.app.smartspacer.repositories

import android.app.WallpaperManager
import android.content.Context
import com.kieronquinn.app.smartspacer.utils.extensions.homescreenWallpaperSupportsDarkText
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenWallpaperSupportsDarkText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow

interface WallpaperRepository {

    val lockscreenWallpaperDarkTextColour: StateFlow<Boolean>
    val homescreenWallpaperDarkTextColour: StateFlow<Boolean>

}

class WallpaperRepositoryImpl(
    context: Context,
    scope: CoroutineScope = MainScope()
): WallpaperRepository {

    private val wallpaperManager =
        context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager

    override val lockscreenWallpaperDarkTextColour = wallpaperManager
        .lockscreenWallpaperSupportsDarkText(scope)

    override val homescreenWallpaperDarkTextColour = wallpaperManager
        .homescreenWallpaperSupportsDarkText(scope)

}