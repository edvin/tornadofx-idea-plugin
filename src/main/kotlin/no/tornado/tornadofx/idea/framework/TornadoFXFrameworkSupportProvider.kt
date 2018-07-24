package no.tornado.tornadofx.idea.framework

import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProvider
import com.intellij.openapi.module.ModuleType
import no.tornado.tornadofx.idea.PluginIcons
import no.tornado.tornadofx.idea.TornadoFXModuleType
import no.tornado.tornadofx.idea.facet.TornadoFXFacetType
import javax.swing.Icon

class TornadoFXFrameworkSupportProvider : FrameworkSupportProvider("TornadoFX", TornadoFXFacetType.INSTANCE.presentableName) {
    override fun getIcon(): Icon = PluginIcons.ACTION
    override fun createConfigurable(model: FrameworkSupportModel) = TornadoFXFrameworkSupportConfigurable(model)
    override fun isEnabledForModuleType(moduleType: ModuleType<*>) = moduleType is TornadoFXModuleType
}