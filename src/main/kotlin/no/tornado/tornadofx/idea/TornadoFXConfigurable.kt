package no.tornado.tornadofx.idea

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.HyperlinkLabel
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class TornadoFXConfigurable : SearchableConfigurable {
    private val id = "preferences.TornadoFX"
    private val displayName = "TornadoFX"
    private val settings by lazy { TornadoFXSettings.getInstance() }
    private var ui: ConfigUI? = null

    override fun createComponent(): JComponent? = ui?.mainPanel ?: ConfigUI().also { ui = it }.mainPanel

    override fun isModified(): Boolean = ui?.isModified(settings) ?: false

    override fun apply() {
       ui?.apply(settings)
    }

    override fun getDisplayName(): String = displayName

    override fun getId(): String = id

    override fun getHelpTopic(): String? = id

    override fun disposeUIResources() {
        ui = null
    }

    override fun reset() {
        ui?.reset(settings)
    }

    class ConfigUI {
        internal var mainPanel: JPanel? = null
        private var alternativePropertySyntaxCheckbox: JCheckBox? = null
        private var propertySyntaxLink: HyperlinkLabel? = null

        fun reset(settings: TornadoFXSettings) {
            alternativePropertySyntaxCheckbox!!.isSelected = settings.alternativePropertySyntax
        }

        fun apply(settings: TornadoFXSettings) {
            settings.alternativePropertySyntax = alternativePropertySyntaxCheckbox!!.isSelected
        }

        fun isModified(settings: TornadoFXSettings): Boolean {
            return settings.alternativePropertySyntax != alternativePropertySyntaxCheckbox!!.isSelected
        }

        init {
            propertySyntaxLink!!.setHyperlinkText("More")
            propertySyntaxLink!!.addHyperlinkListener(object : HyperlinkAdapter() {
                override fun hyperlinkActivated(e: HyperlinkEvent) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://edvin.gitbooks.io/tornadofx-guide/content/part2/Property%20Delegates.html#alternative-property-syntax"))
                    } catch (e1: IOException) {
                        e1.printStackTrace()
                    }
                }
            })
        }
    }
}