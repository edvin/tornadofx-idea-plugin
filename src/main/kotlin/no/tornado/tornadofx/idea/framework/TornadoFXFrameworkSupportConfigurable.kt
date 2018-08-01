package no.tornado.tornadofx.idea.framework

import com.intellij.facet.FacetManager
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.libraries.Library
import no.tornado.tornadofx.idea.facet.TornadoFXFacet
import no.tornado.tornadofx.idea.facet.TornadoFXFacetConfiguration
import no.tornado.tornadofx.idea.facet.TornadoFXFacetType
import javax.swing.JLabel

class TornadoFXFrameworkSupportConfigurable(model: FrameworkSupportModel) : FrameworkSupportConfigurable() {
    override fun getComponent() = JLabel("TornadoFX Framework Support Configuration")

    override fun addSupport(module: Module, modifiableRootModel: ModifiableRootModel, library: Library?) {
        val facetManager = FacetManager.getInstance(module)
        val facetModel = facetManager.createModifiableModel()
        val facet = FacetManager.getInstance(modifiableRootModel.module).addFacet<TornadoFXFacet, TornadoFXFacetConfiguration>(TornadoFXFacetType.INSTANCE, "TornadoFX", null)
        facetModel.addFacet(facet)
        facetModel.commit()
    }

    init {
        model.setFrameworkComponentEnabled("TornadoFX", true)
    }
}