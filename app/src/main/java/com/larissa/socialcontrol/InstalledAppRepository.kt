package com.larissa.socialcontrol

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
)

interface InstalledAppLookup {
    fun isPackageInstalled(packageName: String): Boolean
}

class InstalledAppRepository(
    private val context: Context,
) : InstalledAppLookup {
    private val packageManager: PackageManager
        get() = context.packageManager

    fun getLaunchableApps(): List<InstalledAppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(launcherIntent, 0)
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                if (activityInfo.packageName == context.packageName) {
                    return@mapNotNull null
                }
                InstalledAppInfo(
                    packageName = activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager)?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: activityInfo.packageName,
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    override fun isPackageInstalled(packageName: String): Boolean {
        return getApp(packageName) != null
    }

    fun getApp(packageName: String): InstalledAppInfo? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val resolveInfo = packageManager.resolveActivity(launchIntent, 0)
        if (resolveInfo?.activityInfo == null) {
            return null
        }

        val label = resolveInfo.loadLabel(packageManager)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: packageName

        return InstalledAppInfo(
            packageName = packageName,
            label = label,
        )
    }

    fun getLaunchIntent(packageName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }
}
