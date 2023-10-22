package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.pm.ApplicationInfo
import java.util.zip.ZipFile

/**
 *  Super basic manifest reading to look for a string [contains] within a package's manifest.
 *  This does not decompile or even do anything clever to read the manifest, it just looks for the
 *  string within a raw UTF-16 Little Endian string of the manifest's bytes. It can be used to
 *  check for protected broadcast declarations, since they are not stored in the system anywhere.
 */
fun ApplicationInfo.packageManifestContains(vararg contains: String): Boolean {
    val bytes = getManifestBytes(publicSourceDir) ?: return false
    val manifest = String(bytes, 0, bytes.size, Charsets.UTF_16LE)
    return contains.any {
        manifest.contains(it)
    }
}

private fun getManifestBytes(apkPath: String): ByteArray? {
    return try {
        val apk = ZipFile(apkPath)
        val manifest = apk.getEntry("AndroidManifest.xml")
        apk.getInputStream(manifest).use {
            it.readBytes()
        }.also {
            apk.close()
        }
    }catch (e: Exception){
        null
    }
}