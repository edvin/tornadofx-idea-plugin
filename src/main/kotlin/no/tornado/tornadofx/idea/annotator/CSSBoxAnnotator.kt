package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.FXTools.isStylesheet
import no.tornado.tornadofx.idea.quickfixes.css.BoxQuickFix
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty


/**
 * Creates an annotation for CssBox statements, if they can be simplified.
 *
 * Created by amish on 6/14/17.
 */
class CSSBoxAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.isInStylesheetClass()) {
            if (element is KtBinaryExpression) {
                val left = element.left
                if (left is KtNameReferenceExpression) {
                    val prop = left.mainReference.resolve()
                    if (prop is KtProperty) {
                        val returnType = QuickFixUtil.getDeclarationReturnType(prop)
                        if (returnType?.getJetTypeFqName(false) == "tornadofx.CssBox") {
                            doAnnotation(element, holder)
                        }
                    }
                }
            }
        }
    }

    private fun doAnnotation(element: KtBinaryExpression, holder: AnnotationHolder) {
        val right = element.right?.mainReference
        if (right is KtInvokeFunctionReference) {
            val args = right.expression.valueArguments
            val values = mutableListOf<Pair<Double, String>?>()

            args.forEach { values.add(it.text.convertOrNull()) }

            if (values.isAllSimplifiable()) {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Can be simplified to box(${values[0]!!.first}${values[0]!!.second}).")
                    .range(element.right!!.textRange)
                    .withFix(BoxQuickFix(element, values[0]!!))
                    .create()
            } else if (values.isVHSimplifiable()) {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING,  "Can be simplified to box(${values[0]!!.first}${values[0]!!.second}, ${values[1]!!.first}${values[1]!!.second}).")
                    .range(element.right!!.textRange)
                    .withFix(BoxQuickFix(element, values[0]!!, values[1]!!))
                    .create()
            }
        }
    }

    private fun String.convertOrNull(): Pair<Double, String>? {
        with(this) {
            if (endsWith(".px")) return substringBefore(".px").toDouble() to "px"
            if (endsWith(".mm")) return substringBefore(".mm").toDouble() to "mm"
            if (endsWith(".cm")) return substringBefore(".cm").toDouble() to "cm"
            if (endsWith(".in")) return substringBefore(".in").toDouble() to "in"
            if (endsWith(".pt")) return substringBefore(".pt").toDouble() to "pt"
            if (endsWith(".pc")) return substringBefore(".pc").toDouble() to "pc"
            if (endsWith(".em")) return substringBefore(".em").toDouble() to "em"
            if (endsWith(".ex")) return substringBefore(".ex").toDouble() to "ex"
            if (endsWith(".percent")) return substringBefore(".percent").toDouble() to "percent"
        }
        return null
    }

    private fun MutableList<Pair<Double, String>?>.isAllSimplifiable(): Boolean {
        return this.size == 4 && this[0]?.first == this[1]?.first && this[0]?.first == this[2]?.first && this[0]?.first == this[3]?.first
                && this[0]?.second == this[1]?.second && this[0]?.second == this[2]?.second && this[0]?.second == this[3]?.second
                || size == 2 && this[0]?.first == this[1]?.first && this[0]?.second == this[1]?.second
    }

    private fun MutableList<Pair<Double, String>?>.isVHSimplifiable(): Boolean {
        //top == bottom && right == left
        return this.size == 4 && this[0]?.first == this[2]?.first && this[1]?.first == this[3]?.first
                && this[0]?.second == this[2]?.second && this[1]?.second == this[3]?.second
    }

    private fun PsiElement.isInStylesheetClass(): Boolean {
        val psiClass = this.toKtClassOrNull().toPsiClassOrNull()
        return psiClass != null && isStylesheet(psiClass)
    }

    private fun PsiElement.toKtClassOrNull(): KtClass? = PsiTreeUtil.getParentOfType(this, KtClass::class.java)
    private fun KtClass?.toPsiClassOrNull(): PsiClass? = if (this != null) {
        val psiFacade = JavaPsiFacade.getInstance(this.project)
        psiFacade.findClass(this.fqName.toString(), this.project.allScope())
    } else null
}



