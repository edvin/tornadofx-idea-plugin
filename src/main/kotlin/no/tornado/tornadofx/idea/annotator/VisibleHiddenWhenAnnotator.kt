package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import no.tornado.tornadofx.idea.quickfixes.InvertVisibleHiddenWhenQuickfix
import no.tornado.tornadofx.idea.util.getCalleeFQN
import org.jetbrains.kotlin.psi.KtCallExpression

class VisibleHiddenWhenAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtCallExpression) {
            return
        }

        val isVisibleWhen = element.getCalleeFQN().toString() == "tornadofx.visibleWhen"
        val isHiddenWhen = element.getCalleeFQN().toString() == "tornadofx.hiddenWhen"
        if (isVisibleWhen || isHiddenWhen) {
            val arguments = element.valueArguments
            if (arguments.size == 1 && bindsToNot(arguments.first().text)) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Invert call")
                    .range(element)
                    .withFix(InvertVisibleHiddenWhenQuickfix(element, isVisibleWhen = isVisibleWhen))
                    .create()

//                holder.createWeakWarningAnnotation(element, "Invert call")
//                    .registerFix(InvertVisibleHiddenWhenQuickfix(element, isVisibleWhen = isVisibleWhen))
            }
        }
    }

    private fun bindsToNot(argument: String): Boolean {
        if (argument.endsWith(".not()")) {
            return true
        }

        // Checks if the last line of the lambda ends with ".not()"
        return argument.dropLastWhile { it == '}' || it.isWhitespace() }.endsWith(".not()")
    }
}
