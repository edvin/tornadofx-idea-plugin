package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.facet.FacetManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.FXTools
import no.tornado.tornadofx.idea.FXTools.Companion.isJavaFXProperty
import no.tornado.tornadofx.idea.facet.TornadoFXFacet
import no.tornado.tornadofx.idea.facet.TornadoFXFacetType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil.getDeclarationReturnType
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class ConvertAllPropertiesToFX: PsiElementBaseIntentionAction(), LowPriorityAction {
    override fun getText() = "Convert all properties to TornadoFX properties"
    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        if (element.isWritable && element.language == KotlinLanguage.INSTANCE) {
            TornadoFXFacet.get(project) ?: return false

            val ktClass = if (element is KtClass) element else PsiTreeUtil.getParentOfType(element, KtClass::class.java)

            if (ktClass != null) {
                val psiFacade = JavaPsiFacade.getInstance(project)
                val psiClass = psiFacade.findClass(ktClass.fqName.toString(), project.allScope())
                return psiClass != null && !FXTools.isTornadoFXType(psiClass)
            }
        }

        return false

    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val ktClass = element as? KtClass ?: PsiTreeUtil.getParentOfType(element, KtClass::class.java)!!
        val converter = FXPropertyConverter()

        // Find all val or vars that are not JavaFX Properties
        val params = ktClass.getPrimaryConstructorParameters()
                .filter { it.hasValOrVar() && !it.isVarArg && it.name != null && !isJavaFXProperty(getDeclarationReturnType(it)) }

        // Filter properties that are not JavaFX Properties and not Property Delegates
        val props = ktClass.getBody()?.properties
                ?.filter { it.name != null && !isJavaFXProperty(getDeclarationReturnType(it)) }
                ?.filter { it.children.find { it is KtPropertyDelegate } == null}

        params.forEach { converter.addForParam(it, project, it) }
        props?.forEach { converter.addForProp(it, project, it) }
    }
}
