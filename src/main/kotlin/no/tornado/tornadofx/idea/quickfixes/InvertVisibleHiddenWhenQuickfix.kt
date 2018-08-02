package no.tornado.tornadofx.idea.quickfixes

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Adds a quickfix for inverting calls to <code>visibleWhen</code> and <code>hiddenWhen</code>
 * if the argument is inverted.
 * This works in both cases when the argument is a property or a lambda.
 *
 * Example:
 * visibleWhen(emptyProperty.not()) -> hiddenWhen(emptyProperty)
 */
class InvertVisibleHiddenWhenQuickfix(private val element: KtCallExpression,
                                      private val isVisibleWhen: Boolean) : BaseIntentionAction() {

    private val replaceFunction = if (isVisibleWhen) "hiddenWhen" else "visibleWhen"

    override fun getFamilyName(): String = text
    override fun getText(): String = "Invert call to " + if (isVisibleWhen) "visibleWhen" else "hiddenWhen"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val argument = element.valueArguments.first()
        // Strip away the ".not()"
        val body = argument.text.substring(0, argument.text.length - ".not()".length)
        if (argument is KtLambdaArgument) {
            replaceLambda(argument)
        } else {
            val factory = KtPsiFactory(element.project)
            element.replace(factory.createExpression("$replaceFunction($body)"))
        }
    }

    private fun replaceLambda(argument: KtLambdaArgument) {
        val factory = KtPsiFactory(element.project)
        // Remove ".not()" while keeping the formatting
        val lambda = argument.text.replace(Regex("(\\.not\\(\\))(\\s*})$"), "$2")
        element.replace(factory.createExpression("$replaceFunction$lambda"))
    }
}
