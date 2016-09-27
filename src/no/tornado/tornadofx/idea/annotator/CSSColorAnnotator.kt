package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.ColorIcon
import no.tornado.tornadofx.idea.FXTools
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.*
import java.awt.Color
import javax.swing.Icon

class CSSColorAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val ktClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java)
        if (ktClass != null) {
            val psiFacade = JavaPsiFacade.getInstance(element.project)
            val psiClass = psiFacade.findClass(ktClass.fqName.toString(), element.project.allScope())
            if (psiClass != null && FXTools.isType("tornadofx.Stylesheet", psiClass)) {
                if (element is KtBinaryExpression) {
                    val left = element.left
                    if (left is KtNameReferenceExpression) {
                        val prop = left.mainReference.resolve()
                        if (prop is KtProperty) {
                            val returnType = QuickFixUtil.getDeclarationReturnType(prop)
                            if (returnType?.getJetTypeFqName(false) == "javafx.scene.paint.Paint") {
                                annotateColor(element, holder)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun annotateColor(element: KtBinaryExpression, holder: AnnotationHolder) {
        val annotation = holder.createInfoAnnotation(element, null)
        val value = element.right?.mainReference
        if (value is KtInvokeFunctionReference) {
            val colorCode = value.expression.valueArguments.first().text.replace("\"", "")
            val fxColor = javafx.scene.paint.Color.web(colorCode)
            try {
                val color = Color(fxColor.red.toFloat(), fxColor.green.toFloat(), fxColor.blue.toFloat())
                annotation.gutterIconRenderer = PickerRenderer(element, color)
            } catch (ignored: Exception) {
                ignored.printStackTrace() // for now
            }
        }
   }

    class PickerRenderer(val element: PsiElement, val color: Color) : GutterIconRenderer() {
        override fun getIcon(): Icon {
            return ColorIcon(16, color)
        }

        override fun hashCode(): Int {
            var result = element.hashCode()
            //result = 31 * result + if (myColor != null) myColor.hashCode() else 0
            return result
        }

        override fun equals(other: Any?): Boolean {
            return element.equals(other)
        }
    }
}