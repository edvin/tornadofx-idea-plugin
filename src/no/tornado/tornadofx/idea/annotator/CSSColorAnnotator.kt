package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.ColorPicker
import com.intellij.util.ui.ColorIcon
import no.tornado.tornadofx.idea.FXTools
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.awt.Color
import java.util.*
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
        val right = element.right?.mainReference
        if (right is KtInvokeFunctionReference) {
            //val context = element.analyze(BodyResolveMode.FULL)
            //if (context.getType(right.expression)?.getJetTypeFqName(false) == "javafx.scene.paint.Color")
            val args = right.expression.valueArguments
            val fxColor: javafx.scene.paint.Color
            if (args.size == 1) {
                val colorCode = right.expression.valueArguments.first().text.replace("\"", "")
                fxColor = javafx.scene.paint.Color.web(colorCode)
            } else {
                if (args[0].textContains('.'))
                    fxColor = javafx.scene.paint.Color.color(args[0].text.toDouble(), args[1].text.toDouble(), args[2].text.toDouble(),
                            if (args.size == 4) args[3].text.toDouble() else 1.0)
                else
                    fxColor = javafx.scene.paint.Color.rgb(args[0].text.toInt(), args[1].text.toInt(), args[2].text.toInt(),
                            if (args.size == 4) args[3].text.toDouble() else 1.0)
            }

            try {
                val color = Color(fxColor.red.toFloat(), fxColor.green.toFloat(), fxColor.blue.toFloat(), fxColor.opacity.toFloat())
                annotation.gutterIconRenderer = PickerRenderer(element, color)
            } catch (ignored: Exception) {
            }
        }
   }

    class PickerRenderer(val element: PsiElement, val currentColor: Color) : GutterIconRenderer() {
        override fun getIcon(): Icon {
            return ColorIcon(16, currentColor)
        }

        override fun hashCode(): Int {
            var result = element.hashCode()
            result = 31 * result + currentColor.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            return element == other
        }

        override fun getClickAction() = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val editor = CommonDataKeys.EDITOR.getData(e.dataContext)
                if (editor != null) {
                    val color = ColorPicker.showDialog(editor.component, "Choose Color", currentColor, true, null, false)
                    if (color != null) {
                        ApplicationManager.getApplication().runWriteAction {
                            setColor(element, color)
                        }
                    }
                }
            }
        }

        private fun setColor(element: PsiElement, color: Color) {
            val factory = KtPsiFactory(element.project)
            if (element is KtBinaryExpression) {
                element.deleteChildInternal(element.right!!.node)
                element.add(factory.createExpression("c(${color.red}, ${color.green}, ${color.blue}, %.2f)".format(Locale.US, color.alpha.toDouble() / 255.0)))
            }
        }
    }
}