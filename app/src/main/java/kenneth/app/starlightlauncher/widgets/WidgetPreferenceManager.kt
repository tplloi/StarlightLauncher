package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.preference.ObservablePreferences
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val WIDGET_ORDER_LIST_SEPARATOR = ";"

sealed class WidgetPreferenceChanged {
    data class WidgetOrderChanged(
        val fromPosition: Int,
        val toPosition: Int,
    ) : WidgetPreferenceChanged()

    data class NewAndroidWidgetAdded(
        val addedWidget: AddedWidget.AndroidWidget,

        val appWidgetProviderInfo: AppWidgetProviderInfo,
    ) : WidgetPreferenceChanged()
}

typealias WidgetPreferenceListener = (event: WidgetPreferenceChanged) -> Unit

@Singleton
class WidgetPreferenceManager @Inject constructor(
    @ApplicationContext context: Context,
    private val extensionManager: ExtensionManager,
) : ObservablePreferences<WidgetPreferenceManager>(context) {
    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    val keys = WidgetPrefKeys(context)

    var widgetOrder =
        sharedPreferences.getString(keys.widgetOrder, null)
            ?.split(WIDGET_ORDER_LIST_SEPARATOR)
            ?: mutableSetOf<String>().apply {
                extensionManager.installedExtensions.forEach { ext ->
                    if (ext.widget != null) add(ext.name)
                }
            }
        private set

    var addedWidgets =
        sharedPreferences.getString(keys.addedWidgets, null)
            ?.let {
                Json.decodeFromString<List<AddedWidget>>(it)
            }
            ?.toMutableList()
            ?: mutableListOf<AddedWidget>().apply {
                extensionManager.installedExtensions.forEach { ext ->
                    if (ext.widget != null) add(AddedWidget.StarlightWidget(ext.name))
                }
            }

    override fun updateValue(sharedPreferences: SharedPreferences, key: String) {}

    fun orderOf(extensionName: String) = widgetOrder.indexOf(extensionName)

    fun changeWidgetOrder(fromPosition: Int, toPosition: Int, newOrder: List<String>) {
        widgetOrder = newOrder
        sharedPreferences.edit(commit = true) {
            putString(
                keys.widgetOrder,
                newOrder.joinToString(WIDGET_ORDER_LIST_SEPARATOR)
            )
        }
        setChanged()
        notifyObservers(WidgetPreferenceChanged.WidgetOrderChanged(fromPosition, toPosition))
    }

    fun addAndroidWidget(appWidgetProviderInfo: AppWidgetProviderInfo) {
        val newWidget = AddedWidget.AndroidWidget(
            appWidgetProviderInfo.provider,
        )
        addedWidgets += newWidget
        saveAddedWidgets()
        setChanged()
        notifyObservers(
            WidgetPreferenceChanged.NewAndroidWidgetAdded(
                newWidget,
                appWidgetProviderInfo
            )
        )
    }

    fun removeAndroidWidget(appWidgetProviderInfo: AppWidgetProviderInfo) {
        addedWidgets.removeIf { it is AddedWidget.AndroidWidget && it.provider == appWidgetProviderInfo.provider }
        saveAddedWidgets()
    }

    fun addOnWidgetPreferenceChangedListener(listener: WidgetPreferenceListener) {
        addObserver { o, arg ->
            if (arg is WidgetPreferenceChanged) {
                listener(arg)
            }
        }
    }

    private fun saveAddedWidgets() {
        sharedPreferences.edit(commit = true) {
            putString(
                keys.addedWidgets,
                Json.encodeToString(addedWidgets)
            )
        }
    }
}

class WidgetPrefKeys(context: Context) {
    val widgetOrder by lazy { context.getString(R.string.pref_key_widget_order) }

    val addedWidgets by lazy { context.getString(R.string.pref_key_added_widgets) }
}
