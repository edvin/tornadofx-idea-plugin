package no.tornado.tornadofx.idea.framework

import com.intellij.facet.FacetManager
import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.FacetsProvider
import no.tornado.tornadofx.idea.PluginIcons
import no.tornado.tornadofx.idea.facet.TornadoFXFacet
import no.tornado.tornadofx.idea.facet.TornadoFXFacetConfiguration
import no.tornado.tornadofx.idea.facet.TornadoFXFacetType
import javax.swing.Icon


class TornadoFXFrameworkType : FrameworkTypeEx("TornadoFX") {
    override fun getIcon(): Icon = PluginIcons.ACTION

    override fun getPresentableName() = id

    override fun createProvider() = object : FrameworkSupportInModuleProvider() {
        override fun getFrameworkType() = this@TornadoFXFrameworkType

        override fun isEnabledForModuleType(moduleType: ModuleType<*>): Boolean {
            return true
        }

        override fun isSupportAlreadyAdded(module: Module, facetsProvider: FacetsProvider) =
            facetsProvider.getAllFacets(module).find { it is TornadoFXFacet } != null

        override fun canAddSupport(module: Module, facetsProvider: FacetsProvider) =
            !isSupportAlreadyAdded(module, facetsProvider)

        override fun createConfigurable(model: FrameworkSupportModel) = object : FrameworkSupportInModuleConfigurable() {
            override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider) {
                val facetManager = FacetManager.getInstance(module)
                val facetModel = facetManager.createModifiableModel()
                val facet = facetManager.addFacet<TornadoFXFacet, TornadoFXFacetConfiguration>(TornadoFXFacetType.INSTANCE, "TornadoFX", null)
                facetModel.addFacet(facet)
                facetModel.commit()
            }

            override fun createComponent() = null
        }
    }
}