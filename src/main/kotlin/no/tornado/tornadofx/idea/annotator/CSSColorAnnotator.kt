package no.tornado.tornadofx.idea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.ui.ColorPicker
import com.intellij.util.ui.ColorIcon
import no.tornado.tornadofx.idea.facet.TornadoFXFacet
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.KotlinType
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
        // TODO: Is this the proper way of checking if this is an element in file of a tfx project?
        if (TornadoFXFacet.get(element.project) == null) {
            return
        }
        when (element) {
            is KtBinaryExpression -> {
                val left = element.left
                if (left is KtNameReferenceExpression) {
                    val prop = left.mainReference.resolve()
                    val right = element.right
                    if (prop is KtProperty && right != null) {
                        handelProperty(prop, right, holder)
                    }
                }
            }
            is KtProperty -> {
                val expr = element.children.lastOrNull() as? KtExpression ?: return
                handelProperty(element, expr, holder)
            }
        }
    }

    private fun handelProperty(property: KtProperty, expr: KtExpression, holder: AnnotationHolder, ref: PsiElement? = null)  {
        val returnType= property.declarationReturnType()
        when {
            expr is KtCallExpression && expr.text.startsWith("c(") -> annotateTFXColor(expr, ref, holder)
            expr is KtCallExpression && returnType.isColorMulti() -> annotateMulti(expr.valueArguments, ref, holder)
            expr is KtDotQualifiedExpression && expr.text.startsWith("Color.") ->
                annotateFXColor(expr, ref, holder) { property.replaceColor(it) }
            expr is KtReferenceExpression -> annotateReference(expr, holder)
        }
    }


    /**
     * Annotates Color.* expressions
     */
    private fun annotateFXColor(element: KtDotQualifiedExpression, ref: PsiElement? = null, holder: AnnotationHolder, transformer: (String) -> Unit) {
        val annotation = holder.createInfoAnnotation(ref ?: element, null)
        val fxColor = element.text.toColor()
        fxColor?.let {
            val color = Color(//
                    fxColor.red.toFloat(), //
                    fxColor.green.toFloat(), //
                    fxColor.blue.toFloat(), //
                    fxColor.opacity.toFloat()//
            ) //
            annotation.gutterIconRenderer = ColorRenderer(element.project, color, transformer)
        }
    }

    /**
     * Annotates multi(c() | Color.*, ...) expressions
     */
    private fun annotateMulti(elements: List<KtValueArgument>, ref: PsiElement? = null, holder: AnnotationHolder) {
        for (element in elements) {
            val child = element.firstChild

            when {
                child is KtCallExpression && child.text.startsWith("c(") ->
                    annotateTFXColor(child, ref, holder)
                child is KtDotQualifiedExpression && child.text.startsWith("Color.") ->
                    annotateFXColor(child, ref, holder, { child.replaceColor(element, it) })
            }
        }
    }

    /**
     * Annotates c(...) expressions
     */
    private fun annotateTFXColor(element: KtCallExpression, ref: PsiElement? = null, holder: AnnotationHolder) {
        // TODO: Do we need to check if the expression is valid?
        val annotation = holder.createInfoAnnotation(ref ?: element, null)
        val args = element.valueArguments
        val (fxColor, colorType) = args.toColorType() ?: return
        val color = Color(//
                fxColor.red.toFloat(), //
                fxColor.green.toFloat(), //
                fxColor.blue.toFloat(), //
                fxColor.opacity.toFloat() //
        ) //
        annotation.gutterIconRenderer = PickerRenderer(color, colorType) {
            val factory = KtPsiFactory(element.project)
            element.replace(factory.createExpression(it))
        }
    }

    private fun annotateReference(element: KtReferenceExpression, holder: AnnotationHolder) {
        val resolvedRef = element.mainReference.resolve()

        if( resolvedRef is KtProperty) {
            val expr = resolvedRef.children.last();
            if (expr is KtExpression) {
                handelProperty(
                        resolvedRef,
                        expr,
                        holder,
                        element
                )
            }
        }

    }

    private fun KotlinType?.isColorMulti(): Boolean {
        val fqName = this?.getJetTypeFqName(true)
        return fqName == "tornadofx.MultiValue<javafx.scene.paint.Paint>" || fqName == "tornadofx.MultiValue<javafx.scene.paint.Color>"
    }

    private fun KtNamedDeclaration.declarationReturnType() = QuickFixUtil.getDeclarationReturnType(this)

    private fun KotlinType?.isFxColor(): Boolean {
        val fqName = this?.getJetTypeFqName(false)
        return fqName == "javafx.scene.paint.Color" || fqName == "javafx.scene.paint.Paint"
    }

    private fun List<KtValueArgument>.toColorType(): Pair<javafx.scene.paint.Color, ColorType>? {
        when (size) {
            1, 2 -> {
                this[0]
                val colorCode = this[0].textReplace("\"", "")
                val fxColor: javafx.scene.paint.Color
                try {
                    fxColor = javafx.scene.paint.Color.web(colorCode, this.getOrNull(1)?.textToDouble() ?: 1.0)
                } catch (e: IllegalArgumentException) {
                    // No valid web color so we just show no color annotation
                    return null
                }
                val colorType = if (this.size == 1) ColorType.WEB_WITHOUT_OPACITY else ColorType.WEB_WITHOUT_OPACITY
                return fxColor to colorType
            }
            3, 4 -> {
                if (this[0].textContains('.')) {
                    val fxColor: javafx.scene.paint.Color
                    try {
                        fxColor = this.floatToColor()
                    } catch (e: Exception) {
                        // One of the arguments is bigger the 1.0 or negative, so wie will not show an annotation.
                        return null
                    }
                    val colorType = if (this.size == 4) ColorType.RGB_DOUBLE_WITH_OPACITY else ColorType.RGB_DOUBLE_WITHOUT_OPACITY
                    return fxColor to colorType
                } else {
                    val fxColor: javafx.scene.paint.Color
                    try {
                        fxColor = this.intToColor()
                    } catch (ignored: Exception) {
                        // One of the arguments is is not a integer, bigger then 255 or negative, so wie will not show an annotation.
                        return null
                    }
                    val colorType = if (this.size == 4) ColorType.RGB_INT_WITH_OPACITY else ColorType.RGB_INT_WITHOUT_OPACITY
                    return fxColor to colorType
                }
            }
            else -> return null
        }
    }

    private fun List<KtValueArgument>.floatToColor() =
            javafx.scene.paint.Color.color(this[0].textToDouble(), //
                    this[1].textToDouble(), //
                    this[2].textToDouble(), //
                    if (this.size == 4) this[3].textToDouble() else 1.0) //

    private fun List<KtValueArgument>.intToColor() =
            javafx.scene.paint.Color.rgb(this[0].textToInt(), //
                    this[1].textToInt(), //
                    this[2].textToInt(), //
                    if (this.size == 4) this[3].textToDouble() else 1.0)  //

    private fun KtValueArgument.textToInt() = text.toInt()
    private fun KtValueArgument.textToDouble() = text.toDouble()
    private fun KtValueArgument.textReplace(pattern: String, replacment: String) = text.replace(pattern, replacment)

    class ColorRenderer(private val project: Project, val currentColor: Color, val transformer: (String) -> Unit) : GutterIconRenderer() {

        override fun getIcon(): Icon = ColorIcon(8, currentColor)

        override fun getClickAction(): AnAction? {
            return object : AnAction() {
                override fun actionPerformed(e: AnActionEvent) {
                    val editor = CommonDataKeys.EDITOR.getData(e.dataContext)

                    JBPopupFactory.getInstance()
                            .createListPopup(object : BaseListPopupStep<String>(
                                    "Choose-Color", defaultFXColorNames, defaultFXColors) {
                                override fun getTextFor(value: String) = value

                                override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                                    selectedValue?.let {
                                        ApplicationManager.getApplication().invokeLater {
                                            WriteCommandAction.runWriteCommandAction(project) {
                                                transformer("Color.$it")
                                            }
                                        }
                                    }
                                    return super.onChosen(selectedValue, finalChoice)
                                }

                            }).showInBestPositionFor(editor!!)
                }

            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ColorRenderer

            if (currentColor != other.currentColor) return false

            return true
        }

        override fun hashCode(): Int {
            return 31 * currentColor.hashCode()
        }


    }

    class PickerRenderer(val currentColor: Color, val colorType: ColorType, val transformer: (String) -> Unit) : GutterIconRenderer() {

        override fun getIcon(): Icon = ColorIcon(8, currentColor)

        override fun getClickAction() = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val editor = CommonDataKeys.EDITOR.getData(e.dataContext)
                if (editor != null) {
                    val color = ColorPicker.showDialog(editor.component, "Choose Color",
                            currentColor, true, null, false)

                    color?.let {
                        val colorExpression = it.transform(colorType)
                        ApplicationManager.getApplication()
                                .runWriteAction { transformer(colorExpression) }
                    }
                }
            }
        }

        private fun Color.transform(colorType: ColorType) = when (colorType) {
            ColorType.WEB_WITH_OPACITY -> if (alpha == 255) toWeb() else toWebWithOpacity()
            ColorType.WEB_WITHOUT_OPACITY -> if (alpha == 255) toWeb() else toWebWithOpacity()
            ColorType.RGB_DOUBLE_WITH_OPACITY -> if (alpha == 255) toRGBDoubleWithoutOpacity() else toRGBDoubleWithOpacity()
            ColorType.RGB_DOUBLE_WITHOUT_OPACITY -> if (alpha == 255) toRGBDoubleWithoutOpacity() else toRGBDoubleWithOpacity()
            ColorType.RGB_INT_WITH_OPACITY -> if (alpha == 255) toRGBIntWithoutOpacity() else toRGBIntWithOpacity()
            ColorType.RGB_INT_WITHOUT_OPACITY -> if (alpha == 255) toRGBIntWithoutOpacity() else toRGBIntWithOpacity()
        }

        private fun Color.toWebWithOpacity() = """c("#${Integer.toString(red, 16)}${Integer.toString(green, 16)}${Integer.toString(blue, 16)}", %.2f)""".format(Locale.US, alpha.toDouble() / 255.0)
        private fun Color.toWeb() = """c("#${Integer.toString(red, 16)}${Integer.toString(green, 16)}${Integer.toString(blue, 16)}")"""
        private fun Color.toRGBIntWithOpacity() = "c($red, $green, $blue, %.2f)".format(Locale.US, alpha.toDouble() / 255.0)
        private fun Color.toRGBIntWithoutOpacity() = "c($red, $green, $blue)"
        private fun Color.toRGBDoubleWithOpacity() = "c(%.2f, %.2f, %.2f, %.2f)".format(Locale.US, red / 255.0, green / 255.0, blue / 255.0, alpha.toDouble() / 255.0)
        private fun Color.toRGBDoubleWithoutOpacity() = "c(%.2f, %.2f, %.2f)".format(Locale.US, red / 255.0, green / 255.0, blue / 255.0)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PickerRenderer

            if (currentColor != other.currentColor) return false
            if (colorType != other.colorType) return false
            if (transformer != other.transformer) return false

            return true
        }

        override fun hashCode(): Int {
            var result = 31 * currentColor.hashCode()
            result = 31 * result + colorType.hashCode()
            result = 31 * result + transformer.hashCode()
            return result
        }

    }

    private fun KtDotQualifiedExpression.replaceColor(parent: PsiElement, color: String) {
        val factory = KtPsiFactory(project)
        deleteChildInternal(this.node)
        parent.add(factory.createExpression(color))
    }

    private fun KtProperty.replaceColor(color: String) {
        val factory = KtPsiFactory(project)
        deleteChildInternal(children.last().node)
        add(factory.createExpression(color))
    }

}
