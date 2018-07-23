package no.tornado.tornadofx.idea.quickfixes.css

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Creates a quick fix for a call to <code>padding = Insets(...)</code>
 * to transform into <code>paddingAll = ...</code>
 * The following quick fix options are supported.
 *
 * padding = Insets(10.0, 10.0, 10.0, 10.0) -> paddingAll = 10.0
 * padding = Insets(10.0) -> paddingAll = 10.0
 */
class PaddingAllQuickFix(val element: KtBinaryExpression): BaseIntentionAction() {

    override fun getText(): String {
        return "Simplify statement..."
    }

    override fun getFamilyName(): String {
        return "Simplify statement"
    }

    override fun isAvailable(p0: Project, p1: Editor?, psiFile: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val factory = KtPsiFactory(element.project)
        val arg = (element.right as KtCallExpression).valueArguments[0].text
        val expression = "paddingAll = $arg"
        element.replace(factory.createExpression(expression))
    }
}
