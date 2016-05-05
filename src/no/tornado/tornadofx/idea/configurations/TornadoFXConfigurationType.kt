package no.tornado.tornadofx.idea.configurations

import com.intellij.execution.application.ApplicationConfigurationType
import no.tornado.tornadofx.idea.PluginIcons

class TornadoFXConfigurationType : ApplicationConfigurationType() {
    override fun getIcon() = PluginIcons.ACTION_ICON

    override fun getConfigurationTypeDescription() = displayName

    override fun getId() = "TORNADOFX_RUNCONFIGURATION"

    override fun getDisplayName() = "TornadoFX"

    override fun getConfigurationFactories() = arrayOf(TornadoFXConfigurationFactory(this))
}