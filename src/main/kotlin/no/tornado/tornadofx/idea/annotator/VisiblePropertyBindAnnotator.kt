package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import no.tornado.tornadofx.idea.quickfixes.VisiblePropertyBindQuickfix
import no.tornado.tornadofx.idea.util.getCalleeFQN
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class VisiblePropertyBindAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtDotQualifiedExpression) {
            return
        }

        if (element.children.size == 2 && element.children.all { it is KtCallExpression}) {
            val left = element.children[0] as KtCallExpression // visibleProperty()
            val right = element.children[1] as KtCallExpression // bind()
            if (left.getCalleeFQN().toString() != "javafx.scene.Node.visibleProperty") {
                return
            }

            if (right.getCalleeFQN().toString() != "javafx.beans.property.Property.bind") {
                return
            }


            holder.newAnnotation(HighlightSeverity.WARNING, "Can use visibleWhen()")
                .range(element.textRange)
                .newFix(VisiblePropertyBindQuickfix(element, right))
                .registerFix()
                .create()
        }
    }

}
