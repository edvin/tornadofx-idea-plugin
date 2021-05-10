package no.tornado.tornadofx.idea.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import javax.swing.JLabel


class ErrorDialog(project: Project, private val error: String) : DialogWrapper(project) {
    init {
        title = "Error"
        init()
    }

    override fun createCenterPanel(): JComponent = JLabel(error)
}
