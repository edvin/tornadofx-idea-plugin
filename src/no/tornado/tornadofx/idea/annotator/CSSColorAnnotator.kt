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
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.*
import java.awt.Color
import java.util.*
import javax.swing.Icon

enum class ColorType {
    WEB_WITH_OPACITY,
    WEB_WITHOUT_OPACITY,
    RGB_DOUBLE_WITH_OPACITY,
    RGB_DOUBLE_WITHOUT_OPACITY,
    RGB_INT_WITH_OPACITY,
    RGB_INT_WITHOUT_OPACITY
}

class CSSColorAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.isValid) return
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
        if (right is KtInvokeFunctionReference && right.expression.isValid) {
            
            val args = right.expression.valueArguments
            val fxColor: javafx.scene.paint.Color
            val colorType: ColorType
            when (args.size) {
                1, 2 -> {
                    val colorCode = args[0].textReplace("\"", "")
                    try {
                        fxColor = javafx.scene.paint.Color.web(colorCode, args.getOrNull(1)?.textToDouble() ?: 1.0)
                    } catch (e: IllegalArgumentException) {
                        // No valid web color so we just show no color annotation
                        return
                    }
                    colorType = if (args.size == 1) ColorType.WEB_WITHOUT_OPACITY else ColorType.WEB_WITHOUT_OPACITY
                }
                3, 4 -> {
                    if (args[0].textContains('.')) {
                        try {
                            fxColor = args.floatToColor()
                        } catch (e: Exception) {
                            // One of the arguments is bigger the 1.0 or negative, so wie will not show an annotation.
                            return
                        }
                        colorType = if (args.size == 4) ColorType.RGB_DOUBLE_WITH_OPACITY else ColorType.RGB_DOUBLE_WITHOUT_OPACITY
                    } else {
                        try {
                            fxColor = args.intToColor()
                        } catch (ignored: Exception) {
                            // One of the arguments is is not a integer, bigger then 255 or negative, so wie will not show an annotation.
                            return
                        }
                        colorType = if (args.size == 4) ColorType.RGB_INT_WITH_OPACITY else ColorType.RGB_INT_WITHOUT_OPACITY
                    }
                }
                else -> return
            }

            try {
                val color = Color(fxColor.red.toFloat(), fxColor.green.toFloat(), fxColor.blue.toFloat(), fxColor.opacity.toFloat())
                annotation.gutterIconRenderer = PickerRenderer(element, color, colorType)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun MutableList<KtValueArgument>.floatToColor() =
            javafx.scene.paint.Color.color(this[0].textToDouble(), //
                    this[1].textToDouble(), //
                    this[2].textToDouble(), //
                    if (this.size == 4) this[3].textToDouble() else 1.0) //

    private fun MutableList<KtValueArgument>.intToColor() =
            javafx.scene.paint.Color.rgb(this[0].textToInt(), //
                    this[1].textToInt(), //
                    this[2].textToInt(), //
                    if (this.size == 4) this[3].textToDouble() else 1.0)  //

    private fun KtValueArgument.textToInt() = text.toInt()
    private fun KtValueArgument.textToDouble() = text.toDouble()
    private fun KtValueArgument.textReplace(pattern: String, replacment: String) = text.replace(pattern, replacment)


    class PickerRenderer(val element: PsiElement, val currentColor: Color, val colorType: ColorType) : GutterIconRenderer() {
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
                        ApplicationManager.getApplication()                                       //
                                .runWriteAction { setColor(element, color, colorType) } //
                    }
                }
            }
        }

        private fun setColor(element: PsiElement, color: Color, colorType: ColorType) {
            val factory = KtPsiFactory(element.project)
            if (element is KtBinaryExpression) {
                element.deleteChildInternal(element.right!!.node)
                val expression = when (colorType) {
                    ColorType.WEB_WITH_OPACITY -> if (color.alpha == 255) color.toWeb() else color.toWebWithOpacity()
                    ColorType.WEB_WITHOUT_OPACITY -> if (color.alpha == 255) color.toWeb() else color.toWebWithOpacity()
                    ColorType.RGB_DOUBLE_WITH_OPACITY -> if (color.alpha == 255) color.toRGBDoubleWithoutOpacity() else color.toRGBDoubleWithOpacity()
                    ColorType.RGB_DOUBLE_WITHOUT_OPACITY -> if (color.alpha == 255) color.toRGBDoubleWithoutOpacity() else color.toRGBDoubleWithOpacity()
                    ColorType.RGB_INT_WITH_OPACITY -> if (color.alpha == 255) color.toRGBIntWithoutOpacity() else color.toRGBIntWithOpacity()
                    ColorType.RGB_INT_WITHOUT_OPACITY -> if (color.alpha == 255) color.toRGBIntWithoutOpacity() else color.toRGBIntWithOpacity()
                }
                element.add(factory.createExpression(expression))
            }
        }

        private fun Color.toWebWithOpacity() = """c("#${Integer.toString(red, 16)}${Integer.toString(green, 16)}${Integer.toString(blue, 16)}", %.2f)""".format(Locale.US, alpha.toDouble() / 255.0)
        private fun Color.toWeb() = """c("#${Integer.toString(red, 16)}${Integer.toString(green, 16)}${Integer.toString(blue, 16)}")"""
        private fun Color.toRGBIntWithOpacity() = "c($red, $green, $blue, %.2f)".format(Locale.US, alpha.toDouble() / 255.0)
        private fun Color.toRGBIntWithoutOpacity() = "c($red, $green, $blue)"
        private fun Color.toRGBDoubleWithOpacity() = "c(%.2f, %.2f, %.2f, %.2f)".format(Locale.US, red / 255.0, green / 255.0, blue / 255.0, alpha.toDouble() / 255.0)
        private fun Color.toRGBDoubleWithoutOpacity() = "c(%.2f, %.2f, %.2f)".format(Locale.US, red / 255.0, green / 255.0, blue / 255.0)

    }
}