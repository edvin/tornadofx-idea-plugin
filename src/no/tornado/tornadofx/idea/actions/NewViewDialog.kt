package no.tornado.tornadofx.idea.actions

import com.intellij.execution.ui.ClassBrowser
import com.intellij.ide.actions.TemplateKindCombo
import com.intellij.ide.util.ClassFilter.ClassFilterWithScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.EditorTextFieldWithBrowseButton
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.HORIZONTAL
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class NewViewDialog(val project: Project) : DialogWrapper(project) {
    val myPanel = JPanel(GridBagLayout())
    val myKindCombo = TemplateKindCombo()
    val myNameField = JTextField()
    val myUpDownHint = JLabel(PlatformIcons.UP_DOWN_ARROWS)
    val visibilityChecker = JavaCodeFragment.VisibilityChecker { declaration, place ->
        if (declaration is PsiClass && isParentClass(declaration))
            JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE
        else
            JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE
    }
    val rootType = EditorTextFieldWithBrowseButton(project, true, visibilityChecker)

    init {
        title = "New TornadoFX View"

        myPanel.preferredSize = Dimension(350, -1)

        myPanel.add(JLabel("Name"), GridBagConstraints().apply { gridx = 0; gridy = 0; insets = Insets(3, 3, 3, 10); fill = HORIZONTAL })
        myPanel.add(myNameField, GridBagConstraints().apply { gridx = 1; gridy = 0; insets = Insets(3, 3, 3, 10); weightx = 2.0; fill = HORIZONTAL })

        myUpDownHint.toolTipText = "Pressing Up or Down arrows while in editor changes the kind"
        myPanel.add(myUpDownHint, GridBagConstraints().apply { gridx = 2; gridy = 0; insets = Insets(3, 3, 3, 0); })

        myPanel.add(JLabel("Kind"), GridBagConstraints().apply { gridx = 0; gridy = 1; insets = Insets(3, 3, 3, 10); fill = HORIZONTAL })

        myKindCombo.registerUpDownHint(myNameField)
        myKindCombo.addItem("Code", KotlinIcons.CLASS, "TornadoFX Code View")
        myKindCombo.addItem("FXML", KotlinIcons.FILE, "TornadoFX FXML View")

        myPanel.add(myKindCombo, GridBagConstraints().apply { gridx = 1; gridy = 1; weightx = 2.0; insets = Insets(3, 3, 3, 0); fill = HORIZONTAL; gridwidth = 2 })

        myPanel.add(JLabel("Root"), GridBagConstraints().apply { gridx = 0; gridy = 2; insets = Insets(3, 3, 3, 10); fill = HORIZONTAL })
        myPanel.add(rootType, GridBagConstraints().apply { gridx = 1; gridy = 2; weightx = 2.0; insets = Insets(3, 3, 3, 0); fill = HORIZONTAL; gridwidth = 2 })
        rootType.text = "javafx.scene.layout.BorderPane"

        RootClassBrowser()

        init()
    }

    override fun getPreferredFocusedComponent() = myNameField

    override fun createCenterPanel() = myPanel

    fun isParentClass(psiClass: PsiClass): Boolean {
        if (psiClass.qualifiedName?.startsWith("com.sun.") ?: false) return false

        for (supa in psiClass.supers) {
            if ("javafx.scene.Parent" == supa.qualifiedName && !(supa.qualifiedName?.startsWith("com.sun.") ?: false))
                return true

            val superIs = isParentClass(supa)
            if (superIs) return true
        }
        return false
    }

    inner class RootClassBrowser() : ClassBrowser(project, "Select Root Class") {
        init {
            setField(rootType)
        }

        override fun findClass(className: String) = JavaPsiFacade.getInstance(project).findClass(className, project.projectScope())
        override fun getField() = rootType
        override fun getFilter(): ClassFilterWithScope = object : ClassFilterWithScope {
            override fun getScope() = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(project.allModules().first())
            override fun isAccepted(psiClass: PsiClass) = isParentClass(psiClass)
        }
    }

}