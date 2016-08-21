package no.tornado.tornadofx.idea.framework

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import no.tornado.tornadofx.idea.PluginIcons
import javax.swing.JComponent
import javax.swing.JPanel

class TornadoFXFramework : FrameworkTypeEx("TornadoFX") {
    override fun getIcon() = PluginIcons.ACTION

    override fun getPresentableName() = id

    override fun createProvider() = object : FrameworkSupportInModuleProvider() {
        override fun getFrameworkType() = this@TornadoFXFramework

        override fun isEnabledForModuleType(moduleType: ModuleType<*>): Boolean {
            println("Should we enable TornadoFX Framework for module type $moduleType?")
            return true
        }

        override fun createConfigurable(model: FrameworkSupportModel) = object : FrameworkSupportInModuleConfigurable() {
            override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider) {
                println("Ready to add TornadoFX Support to $module")
            }

            override fun createComponent(): JComponent {
                return JPanel()
            }

        }
    }
}