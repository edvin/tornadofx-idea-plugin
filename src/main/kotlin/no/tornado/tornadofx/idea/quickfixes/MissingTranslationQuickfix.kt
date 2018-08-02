package no.tornado.tornadofx.idea.quickfixes

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.dialog.ExtractStringToResourceDialog
import no.tornado.tornadofx.idea.translation.TranslationManager
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtClass

/**
 * Creates a quickfix that openes a dialog for adding a translation to resources
 * Looks for <code>messages["key"]</code>, where there is no translation for given key
 */
class MissingTranslationQuickfix(private val expression: KtArrayAccessExpression) : BaseIntentionAction() {

    private val translationManager = TranslationManager()

    override fun getFamilyName(): String = "Missing translation"
    override fun getText(): String = "Add missing translation"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val defaultKey = expression.indexExpressions.first().text.trim('"')
        val clazz = PsiTreeUtil.getParentOfType(expression, KtClass::class.java) ?: throw TranslationManager.FetchResourceFileException("Cannot find class")
        val resourceFile = translationManager.getResourceFile(clazz)
        val resourcePath = translationManager.getResourcePath(project, resourceFile)
        val dialog =
            ExtractStringToResourceDialog(project, defaultKey, resourcePath = resourcePath) { key, value ->
                translationManager.addProperty(resourceFile, key, value)
            }

        ApplicationManager.getApplication().invokeLater {
            dialog.show()
        }
    }
}
