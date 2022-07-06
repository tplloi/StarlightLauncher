package kenneth.app.starlightlauncher.prefs.appearance

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.api.LauncherEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles preferences for launcher appearance.
 */
@Singleton
internal class AppearancePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val launcherEventChannel: LauncherEventChannel,
) {
    private val prefKeys = AppearancePreferenceKeys(context)

    private val defaultIconPack = DefaultIconPack(context)

    /**
     * Whether blur effect is enabled. If not set, the default value
     * is whether the launcher has read external storage permission.
     * If the launcher has permission, then blur effect is enabled by default.
     */
    val isBlurEffectEnabled
        get() = sharedPreferences.getBoolean(
            prefKeys.blurEffectEnabled,
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )

    /**
     * The icon pack to use, if user has picked any. null if user has not picked any icon pack.
     */
    var iconPack: IconPack =
        sharedPreferences.getString(prefKeys.iconPack, null)
            ?.let {
                InstalledIconPack(context, it)
            }
            ?: defaultIconPack
        private set

    /**
     * Sets whether blur effect should be enabled.
     *
     * @param enabled Whether blur effect should be enabled
     */
    fun setBlurEffectEnabled(enabled: Boolean) {
        val value =
            if (enabled)
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            else
                false

        sharedPreferences.edit(commit = true) {
            putBoolean(prefKeys.blurEffectEnabled, value)
        }
    }

    fun changeIconPack(iconPack: InstalledIconPack) {
        sharedPreferences
            .edit()
            .putString(prefKeys.iconPack, iconPack.packageName)
            .apply()

        this.iconPack = iconPack
        launcherEventChannel.add(LauncherEvent.IconPackChanged)
    }

    /**
     * Revert the applied icon pack and use default icons instead.
     */
    fun useDefaultIconPack() {
        sharedPreferences
            .edit()
            .remove(prefKeys.iconPack)
            .apply()

        iconPack = defaultIconPack
        launcherEventChannel.add(LauncherEvent.IconPackChanged)
    }
}

internal class AppearancePreferenceKeys(context: Context) {
    val iconPack = context.getString(R.string.appearance_icon_pack)

    val blurEffectEnabled = context.getString(R.string.pref_key_appearance_blur_effect_enabled)
}
