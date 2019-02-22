package no.tornado.tornadofx.idea.quickfixes

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.dialog.ErrorDialog
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
        val dialog = try {
            val defaultKey = expression.indexExpressions.first().text.trim('"')
            val clazz = PsiTreeUtil.getParentOfType(expression, KtClass::class.java)
                    ?: throw TranslationManager.FetchResourceFileException("Cannot find class")
            val resourcePaths = translationManager.getResourceFiles(clazz)
            val resourcePathStrings = Array(resourcePaths.size) {
                translationManager.getResourcePath(project, resourcePaths[it])
            }

            ExtractStringToResourceDialog(project, defaultKey, "", resourcePaths, resourcePathStrings) { key, value, resourceFile ->
                translationManager.addProperty(resourceFile, key, value)
            }
        } catch (e: TranslationManager.FetchResourceFileException) {
            ErrorDialog(project, e.message)
        }
        ApplicationManager.getApplication().invokeLater {
            dialog.show()
        }
    }
}
