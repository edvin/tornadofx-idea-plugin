package no.tornado.tornadofx.idea.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import no.tornado.tornadofx.idea.PluginIcons
import javax.swing.Icon

class TornadoFXFacetType : FacetType<TornadoFXFacet, TornadoFXFacetConfiguration>(ID, "TornadoFX", "TornadoFX") {

    override fun createFacet(module: Module, name: String, configuration: TornadoFXFacetConfiguration, underlyingFacet: Facet<*>?) =
            TornadoFXFacet(this, module, name, configuration, underlyingFacet)

    override fun createDefaultConfiguration() = TornadoFXFacetConfiguration()

    override fun isSuitableModuleType(moduleType: ModuleType<*>?) = true

    override fun getIcon(): Icon = PluginIcons.ACTION

    companion object {
        val ID = FacetTypeId<TornadoFXFacet>("TornadoFX")
        val INSTANCE = TornadoFXFacetType()
    }

}