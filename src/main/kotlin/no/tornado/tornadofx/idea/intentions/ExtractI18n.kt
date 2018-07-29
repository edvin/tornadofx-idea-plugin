package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.dialog.ErrorDialog
import no.tornado.tornadofx.idea.dialog.ExtractStringToResourceDialog
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

    class FetchResourceFileException(override val message: String) : RuntimeException()

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
            val clazz = getClass(element) ?: throw FetchResourceFileException("Cannot find class.")
            val file = getFile(clazz)
            val resourcePath = file.virtualFile.path.substring(project.basePath?.length ?: 0)
            ExtractStringToResourceDialog(project, defaultKey, defaultValue, resourcePath) { key, value ->
                addProperty(file, key, value)
                WriteCommandAction.runWriteCommandAction(project) {
                    val factory = KtPsiFactory(project)
                    val stringPsi = PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java) ?: element
                    stringPsi.replace(factory.createExpression("messages[\"$key\"]"))
                }
            }
        } catch (e: FetchResourceFileException) {
            ErrorDialog(project, e.message)
        }
        ApplicationManager.getApplication().invokeLater {
            dialog.show()
        }
    }

    private fun getFolder(clazz: KtClass): String? {
        return clazz.fqName?.toString()?.replace(".", "/")
    }

    private fun getClass(element: PsiElement): KtClass? {
        return PsiTreeUtil.getParentOfType(element, KtClass::class.java)
    }

    private fun getFile(clazz: KtClass): PsiFile {
        val project = clazz.project
        val className = clazz.fqName?.shortName() ?: throw FetchResourceFileException("Cannot fetch classname")
        val folder = getFolder(clazz)!!

        val file = FilenameIndex.getFilesByName(project, "$className.properties", GlobalSearchScope.allScope(project))
            .firstOrNull { it.containingDirectory.toString().endsWith(folder.substring(0, folder.lastIndexOf("/"))) }
        return file ?: throw FetchResourceFileException("Cannot find file $className.properties")
    }

    private fun addProperty(psiFile: PsiFile, key: String, value: String) {
        WriteCommandAction.runWriteCommandAction(psiFile.project) {
            val manager = FileEditorManager.getInstance(psiFile.project)
            manager.openFile(psiFile.virtualFile, true)
            val editor = manager.selectedTextEditor
            val document = editor?.document ?: return@runWriteCommandAction
            val prefix = editor.caretModel.primaryCaret.run { // Don't add unnecessary newlines
                val selectionStart = if (document.textLength == 0) {
                    0
                } else {
                    document.textLength - 1
                }
                setSelection(selectionStart, document.textLength)
                if (selectedText == "\n" || document.textLength == 0) {
                    ""
                } else {
                    "\n"
                }
            }
            document.insertString(document.textLength, "$prefix$key = $value")
            manager.closeFile(psiFile.virtualFile)
        }
    }
}
