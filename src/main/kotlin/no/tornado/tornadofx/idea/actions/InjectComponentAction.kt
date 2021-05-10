package no.tornado.tornadofx.idea.actions

import com.intellij.execution.ui.ClassBrowser
import com.intellij.ide.util.ClassFilter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorTextField
import com.intellij.ui.EditorTextFieldWithBrowseButton
import no.tornado.tornadofx.idea.FXTools
import no.tornado.tornadofx.idea.FXTools.isComponent
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory

class InjectComponentAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val browser = ComponentBrowser(e.project!!)
        val componentClass = browser.requestComponentClass()
        if (componentClass != null) {
            val element = getElementAtCaret(e)!!
            WriteCommandAction.writeCommandAction(e.project!!, element.containingFile).run<Throwable> {
                val factory = KtPsiFactory(e.project!!)
                val propName = componentClass.substringAfterLast(".").let {
                    it.first().toLowerCase() + it.substring(1)
                }
                val componentPsiClass = FXTools.psiClass(componentClass, e.project!!)!!
                val injectType = if (FXTools.isFragment(componentPsiClass)) "fragment" else "inject"
                val prop = factory.createProperty("val $propName: $componentClass by $injectType()")
                val ktClassBody = PsiTreeUtil.getParentOfType(element, KtClassBody::class.java)!!
                val added = ktClassBody.addAfter(prop, ktClassBody.firstChild) as KtElement
                ShortenReferences().process(added)
            }
        }
    }

    private fun getElementAtCaret(e: AnActionEvent): PsiElement? {
        val psiFile = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)

        if (psiFile == null || editor == null) {
            e.presentation.isEnabled = false
            return null
        }

        val offset = editor.caretModel.offset
        return psiFile.findElementAt(offset)
    }

    override fun update(e: AnActionEvent) {
        val elementAt = getElementAtCaret(e)
        if (elementAt == null) {
            e.presentation.isEnabled = false
            return
        }
        val ktClass = PsiTreeUtil.getParentOfType(elementAt, KtClass::class.java)

        if (ktClass == null) {
            e.presentation.isEnabled = false
            return
        }

        val psiFacade = JavaPsiFacade.getInstance(e.project!!)
        val psiClass = psiFacade.findClass(ktClass.fqName.toString(), e.project!!.allScope())!!

        e.presentation.isEnabled = isComponent(psiClass)
    }

    class ComponentBrowser(project: Project) : ClassBrowser<EditorTextField>(project, "Select Component to Inject") {
        init {
            // Inject a fake field, we need it because myField.text is called while browsing
            setField(EditorTextFieldWithBrowseButton(project, true, null))
        }

        override fun findClass(className: String) = JavaPsiFacade.getInstance(project).findClass(className, project.projectScope())
        override fun getFilter(): ClassFilter.ClassFilterWithScope = object : ClassFilter.ClassFilterWithScope {
            override fun getScope() = GlobalSearchScope.allScope(project)
            override fun isAccepted(psiClass: PsiClass) = isComponent(psiClass)
        }

        fun requestComponentClass(): String? = showDialog()
    }

}