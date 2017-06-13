package no.tornado.tornadofx.idea.facet

import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import javax.swing.JLabel

class TornadoFXFacetEditorTab(val editorContext: FacetEditorContext) : FacetEditorTab() {
    override fun isModified() = false
    override fun getDisplayName() = "TornadoFX"
    override fun createComponent() = JLabel("No configuration options yet.")
}