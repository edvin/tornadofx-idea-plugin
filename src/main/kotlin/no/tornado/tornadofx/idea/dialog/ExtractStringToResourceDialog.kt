package no.tornado.tornadofx.idea.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class ExtractStringToResourceDialog(
    project: Project,
    defaultKey: String = "",
    defaultValue: String = "",
    resourcePath: String = "Unknown",
    private val okAction: (String, String) -> Unit
) : DialogWrapper(project) {

    private val resourcePathLabel = JLabel(resourcePath)
    private val keyTextField = JTextField(defaultKey)
    private val valueTextField = JTextField(defaultValue)

    private val c = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
    }

    init {
        title = "Extract String resource"
        init()
    }

    override fun createCenterPanel(): JComponent? = JPanel(GridBagLayout()).apply {
        addGrid(0, 0, JLabel("resource:"))
        addGrid(1, 0, resourcePathLabel)

        addGrid(0, 1, JLabel("key"))
        addGrid(1, 1, keyTextField)

        addGrid(0, 2, JLabel("value"))
        addGrid(1, 2, valueTextField)
    }

    override fun doOKAction() {
        okAction(keyTextField.text, valueTextField.text)
        super.doOKAction()
    }

    private fun JComponent.addGrid(x: Int, y: Int, component: JComponent) {
        c.gridx = x
        c.gridy = y
        add(component, c)
    }
}
