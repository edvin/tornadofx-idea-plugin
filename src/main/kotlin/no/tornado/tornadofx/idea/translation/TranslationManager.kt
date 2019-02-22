package no.tornado.tornadofx.idea.translation

import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import no.tornado.tornadofx.idea.index.PropertiesIndex
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtClass
import kotlin.math.max

class TranslationManager {
    class FetchResourceFileException(override val message: String) : RuntimeException()

    private val index = FileBasedIndex.getInstance()

    /**
     * Checks if left hand side of array access is a reference to Component's messages
     */
    fun isMessageExpression(expression: KtArrayAccessExpression): Boolean {
        val array = expression.arrayExpression ?: return false
        val fqName = array.mainReference?.resolve()?.getKotlinFqName() ?: return false

        return fqName.toString() == "tornadofx.Component.messages"
    }

    /**
     * Fetches the Components name and prepends it to the key
     * expression must be a message expression, checked with [isMessageExpression]
     */
    fun getKey(expression: KtArrayAccessExpression): String {
        val key = expression.indexExpressions.first().text.trim('"')
        val clazz = PsiTreeUtil.getParentOfType(expression, KtClass::class.java) ?: return key
        val fqn = clazz.fqName?.toString()

        return if (fqn == null) {
            key
        } else {
            "$fqn.$key"
        }
    }

    /**
     * Finds a translation, based on the [PropertiesIndex] key
     */
    fun findTranslation(key: String, project: Project): String? {
        val values = index.getValues(PropertiesIndex.NAME, key, GlobalSearchScope.allScope(project))

        return values.firstOrNull() ?: findGlobalTranslation(key, project)
    }

    /**
     * Finds a translation for given `message["key"]` expression.
     * Expression must be a message expression, checked with [isMessageExpression]
     */
    fun findTranslation(expression: KtArrayAccessExpression): String? {
        val key = getKey(expression)
        return findTranslation(key, expression.project)
    }

    private fun findGlobalTranslation(key: String, project: Project): String? {
        val globalKey = "Messages." + key.substringAfterLast('.')
        return index.getValues(PropertiesIndex.NAME, globalKey, GlobalSearchScope.allScope(project)).firstOrNull()
    }


    fun getResourcePath(project: Project, file: PsiFile): String {
        return file.virtualFile.path.substring(project.basePath?.length ?: 0)
    }

    fun getResourceFiles(clazz: KtClass): List<PsiFile> {
        val project = clazz.project
        val module = clazz.containingFile.module!!
        val psiManager = PsiManager.getInstance(project)
        val moduleRootManager = ModuleRootManager.getInstance(module)
        val propertiesPath = getFileSubPath(clazz)!! + ".properties"

        // check whether the class is a test source
        val isTestSource = TestSourcesFilter.isTestSources(clazz.containingFile.virtualFile, project)
        val resourceType = if (isTestSource) JavaResourceRootType.TEST_RESOURCE else JavaResourceRootType.RESOURCE


        val ret = mutableListOf<PsiFile>()
        // get matching resource root(s)
        val resourceFolders = moduleRootManager.contentEntries
                .flatMap { it.getSourceFolders(resourceType) }
        val existingFiles = resourceFolders
                .asSequence()
                .mapNotNull { it.file?.findFileByRelativePath(propertiesPath) }
                .mapNotNull { psiManager.findFile(it) }
        ret.addAll(existingFiles.toList())

        if (ret.isEmpty()) {
            // Create file
            val mainResourceDirectory = resourceFolders.firstOrNull()?.file
                    ?: throw FetchResourceFileException("No resource folder found")

            ret += PsiFileFactory.getInstance(project)
                    .createFileFromText(
                            propertiesPath.substringAfterLast('/'),
                            PropertiesFileType.INSTANCE.language, ""
                    ).let { newFile ->
                        // create parent directories
                        var parentDirectory = psiManager.findDirectory(mainResourceDirectory)
                                ?: throw FetchResourceFileException("Cannot fetch directory '$mainResourceDirectory'")
                        for (part in propertiesPath.substringBeforeLast("/").split('/')) {
                            parentDirectory = parentDirectory.findSubdirectory(part)
                                    ?: parentDirectory.createSubdirectory(part)
                        }

                        // Must return the added file as it contains the full path
                        parentDirectory.add(newFile) as PsiFile
                    }
        }

        // find global files
        ret += resourceFolders
                .asSequence()
                .mapNotNull { it.file?.findFileByRelativePath("Messages.properties") }
                .mapNotNull { psiManager.findFile(it) }

        return ret
    }


    private fun getFileSubPath(clazz: KtClass): String? {
        return clazz.fqName?.toString()?.replace(".", "/")
    }

    fun addProperty(psiFile: PsiFile, key: String, value: String) {
        WriteCommandAction.runWriteCommandAction(psiFile.project) {
            val manager = FileEditorManager.getInstance(psiFile.project)
            manager.openFile(psiFile.virtualFile, true)
            val editor = manager.selectedTextEditor
            val document = editor?.document ?: return@runWriteCommandAction
            val prefix = editor.caretModel.primaryCaret.run { // Don't add unnecessary newlines
                val selectionStart = max(document.textLength - 1, 0)
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

