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
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class PaddingAllAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtBinaryExpression) { // we want the assignment
            return
        }

        // Right must be a call to Insets()
        val right = element.right?.mainReference as? KtInvokeFunctionReference ?: return
        val name = right.expression.calleeExpression?.mainReference?.resolve()
        if (name?.getKotlinFqName()?.asString() != "javafx.geometry.Insets.Insets") {
            return
        }

        // Check if its either Insets(n) or Insets(n, n, n, n)
        val args = right.expression.valueArguments
        if (args.size != 1 && args.size != 4) {
            return
        }
        if (args.map { it.text }.distinct().size > 1) {
            return
        }

        // Left must be a call to setPadding
        val left = element.left as? KtNameReferenceExpression
        val setter = left?.references?.first { it is SyntheticPropertyAccessorReference }?.resolve()
        if (setter?.getKotlinFqName()?.asString() == "javafx.scene.layout.Region.setPadding") {
            holder.createWeakWarningAnnotation(element.textRange, "Can use paddingAll")
                .registerFix(PaddingAllQuickFix(element))
        }
    }
}
