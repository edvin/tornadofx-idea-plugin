package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import no.tornado.tornadofx.idea.quickfixes.MissingTranslationQuickfix
import no.tornado.tornadofx.idea.translation.TranslationManager
import org.jetbrains.kotlin.psi.KtArrayAccessExpression

class MissingTranslationAnnotator : Annotator {
    private val translationManager = TranslationManager()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtArrayAccessExpression) {
            return
        }

        if (!translationManager.isMessageExpression(element)) {
            return
        }

        val translation = translationManager.findTranslation(element)

        if (translation == null) {
            holder.createWeakWarningAnnotation(element.textRange, "Missing translation")
                .registerFix(MissingTranslationQuickfix(element))
        }
    }
}
