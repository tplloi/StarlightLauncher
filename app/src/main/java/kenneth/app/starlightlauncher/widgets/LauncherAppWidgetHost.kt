package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.BindingRegister
import kenneth.app.starlightlauncher.R
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LauncherAppWidgetHostProvider {
    @Singleton
    @Binds
    abstract fun bindAppWidgetHost(impl: LauncherAppWidgetHost): AppWidgetHost
}

internal class LauncherAppWidgetHost @Inject constructor(
    @ApplicationContext context: Context,
    private val bindingRegister: BindingRegister,
) :
    AppWidgetHost(context, R.id.app_widget_host_id) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = LauncherAppWidgetHostView(context, bindingRegister)
}