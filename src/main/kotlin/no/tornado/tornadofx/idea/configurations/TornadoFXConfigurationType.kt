package no.tornado.tornadofx.idea.configurations

import com.intellij.execution.configurations.ConfigurationTypeBase
import no.tornado.tornadofx.idea.icons.PluginIcons

class TornadoFXConfigurationType : ConfigurationTypeBase("TORNADOFX_RUNCONFIGURATION", "TornadoFX", "TornadoFX", PluginIcons.ACTION) {
    override fun getConfigurationFactories() = arrayOf(TornadoFXConfigurationFactory(this))
}