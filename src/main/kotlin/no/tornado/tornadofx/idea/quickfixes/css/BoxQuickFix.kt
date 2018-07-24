package no.tornado.tornadofx.idea.quickfixes.css

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Creates a quick fix for an [CssBox] element that can be simplified.
 * The following quick fix options are supported.
 *
 * box(10.0.px, 10.0.px, 10.0.px, 10.0.px) -> box(10.px)
 * box(5.px, 10.px, 5.px, 10.px) -> box(5.px, 10.px)
 * Created by amish on 6/14/17.
 */

class BoxQuickFix private constructor(val element: KtBinaryExpression, val isSimplifyAll: Boolean): BaseIntentionAction() {

    var all: Pair<Double, String>? = null
    var vertical: Pair<Double, String>? = null
    var horizontal: Pair<Double, String>? = null

    constructor(element: KtBinaryExpression, all: Pair<Double, String>): this(element, true) {
        this.all = all
    }

    constructor(element: KtBinaryExpression, vertical: Pair<Double, String>, horizontal: Pair<Double, String>): this(element, false) {
        this.vertical = vertical
        this.horizontal = horizontal
    }

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
        element.deleteChildInternal(element.right!!.node)
        val expression = if (all != null) {
            "box(${all!!.first}.${all!!.second})"
        } else {
            "box(${vertical!!.first}.${vertical!!.second}, ${horizontal!!.first}.${horizontal!!.second})"
        }
        element.add(factory.createExpression(expression))
    }
}
