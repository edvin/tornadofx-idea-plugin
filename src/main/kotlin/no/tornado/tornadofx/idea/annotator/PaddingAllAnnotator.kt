package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import no.tornado.tornadofx.idea.quickfixes.css.PaddingAllQuickFix
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument

class PaddingAllAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtBinaryExpression) { // we want the assignment
            return
        }

        // Right must be a call to Insets()
        val right = element.right?.mainReference as? KtInvokeFunctionReference ?: return
        if (!checkRight(right)) {
            return
        }

        // Left must be a call to setPadding
        val left = element.left as? KtNameReferenceExpression ?: return
        if (checkLeft(left)) {
            holder.createWeakWarningAnnotation(element.textRange, "Can use paddingAll")
                .registerFix(PaddingAllQuickFix(element))
        }
    }

    private fun checkLeft(left: KtNameReferenceExpression): Boolean {
        val setter = left.references.first { it is SyntheticPropertyAccessorReference }?.resolve()

        return setter?.getKotlinFqName()?.asString() == "javafx.scene.layout.Region.setPadding"
    }

    private fun checkRight(right: KtInvokeFunctionReference): Boolean {
        // Right must be a call to Insets()
        val name = right.expression.calleeExpression?.mainReference?.resolve() ?: return false
        if (name.getKotlinFqName()?.asString() != "javafx.geometry.Insets.Insets") {
            return false
        }

        // Check if its either Insets(n) or Insets(n, n, n, n)
        val args: List<KtValueArgument> = right.expression.valueArguments
        if (args.size == 1) {
            return true
        }

        val argsExpr = args.asSequence()
            .map { it.children } // Get the underlying expression
            .filter { it.size == 1 } // No complex expressions
            .map { it[0] } // List only has one element
            .filter { it is KtConstantExpression || it is KtNameReferenceExpression } // only constants or variables, no functions
            .toList()
        return argsExpr.size == 4 && argsExpr.map { it.text }.distinct().size == 1
    }
}
