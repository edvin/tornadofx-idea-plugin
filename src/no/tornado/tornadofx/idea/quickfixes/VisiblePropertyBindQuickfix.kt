package no.tornado.tornadofx.idea.quickfixes

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory


/**
 * Creates a Quickfix to transform usage of <code>visibleProperty().bind(...)</code>
 * into <code>visibleWhen(...)</code>
 */
class VisiblePropertyBindQuickfix(private val element: KtDotQualifiedExpression,
                                  private val right: KtCallExpression) : BaseIntentionAction() {

    override fun getText(): String {
        return "Simplify statement to use visibleWhen()"
    }

    override fun getFamilyName(): String {
        return "Simplify statement"
    }

    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val factory = KtPsiFactory(element.project)
        val argument = right.valueArguments.first().text
        val expression = factory.createExpression("visibleWhen($argument)")
        element.replace(expression)
    }
}
