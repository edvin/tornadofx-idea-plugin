package no.tornado.tornadofx.idea.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBTextField
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ExtractStringToResourceDialog(
        project: Project,
        defaultKey: String,
        defaultValue: String,
        private val resourcePaths: List<PsiFile>,
        resourcePathStrings: Array<String>,
        private val okAction: (String, String, PsiFile) -> Unit
) : DialogWrapper(project) {

    private val resourcePathComboBox = JComboBox<String>(resourcePathStrings).apply {
        resourcePathStrings.maxBy { it.length }?.let {
            prototypeDisplayValue = it
        }
    }
    private val keyTextField = JBTextField(defaultKey)
    private val valueTextField = JBTextField(defaultValue)

    private val c = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
    }

    init {
        title = "Extract String resource"
        init()
    }

    override fun createCenterPanel(): JComponent? = JPanel(GridBagLayout()).apply {
        addGrid(0, 0, JLabel("resource:"))
        addGrid(1, 0, resourcePathComboBox)

        addGrid(0, 1, JLabel("key"))
        addGrid(1, 1, keyTextField)

        addGrid(0, 2, JLabel("value"))
        addGrid(1, 2, valueTextField)
    }

    override fun doOKAction() {
        okAction(keyTextField.text, valueTextField.text, resourcePaths[resourcePathComboBox.selectedIndex])
        super.doOKAction()
    }

    private fun JComponent.addGrid(x: Int, y: Int, component: JComponent) {
        c.gridx = x
        c.gridy = y
        c.insets = Insets(1, 5, 1, 5)
        add(component, c)
    }
}
