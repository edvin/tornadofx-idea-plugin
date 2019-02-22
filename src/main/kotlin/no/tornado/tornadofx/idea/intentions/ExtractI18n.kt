package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.dialog.ErrorDialog
import no.tornado.tornadofx.idea.dialog.ExtractStringToResourceDialog
import no.tornado.tornadofx.idea.translation.TranslationManager
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 * Intention on strings to extract string under cursor to a resource file based
 * on the Component's name.
 */
class ExtractI18n : PsiElementBaseIntentionAction() {

    private val translationManager = TranslationManager()

    override fun getText(): String = "Extract String into resources"

    override fun getFamilyName(): String = "Internationalization"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.node.elementType == KtTokens.REGULAR_STRING_PART
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val defaultValue = element.node.chars.toString() // Content of string
        val defaultKey = defaultValue
            .replace(Regex("([^A-Za-z]+)"), "_")
            .replace(Regex("_$"), "") // do not end keys with underscore
            .toLowerCaseAsciiOnly()

        val dialog: DialogWrapper = try {
            val clazz = getClass(element) ?: throw TranslationManager.FetchResourceFileException("Cannot find class.")
            val resourcePaths = translationManager.getResourceFiles(clazz)
            val resourcePathStrings = Array(resourcePaths.size) {
                translationManager.getResourcePath(project, resourcePaths[it])
            }
            ExtractStringToResourceDialog(project, defaultKey, defaultValue, resourcePaths, resourcePathStrings) { key, value, resourceFile ->
                translationManager.addProperty(resourceFile, key, value)
                WriteCommandAction.runWriteCommandAction(project) {
                    val factory = KtPsiFactory(project)
                    val stringPsi = PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java) ?: element
                    stringPsi.replace(factory.createExpression("messages[\"$key\"]"))
                }
            }
        } catch (e: TranslationManager.FetchResourceFileException) {
            ErrorDialog(project, e.message)
        }
        ApplicationManager.getApplication().invokeLater {
            dialog.show()
        }
    }

    private fun getClass(element: PsiElement): KtClass? {
        return PsiTreeUtil.getParentOfType(element, KtClass::class.java)
    }
}
