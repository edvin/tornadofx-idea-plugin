package no.tornado.tornadofx.idea

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinIcons

class NewViewAction : CreateFileFromTemplateAction("New TornadoFX View", "Create a new TornadoFX View", PluginIcons.ACTION) {
    override fun getActionName(dir: PsiDirectory?, newName: String, templateName: String?) =
            "New TornadoFX View"

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
                .setTitle("New TornadoFX View")
                .addKind("Code", KotlinIcons.CLASS, "TornadoFX Code View")
                .addKind("FXML", KotlinIcons.FILE, "TornadoFX FXML View")
    }

    override fun isAvailable(dataContext: DataContext): Boolean {
        if (super.isAvailable(dataContext)) {
            val ideView = LangDataKeys.IDE_VIEW.getData(dataContext)
            val project = PlatformDataKeys.PROJECT.getData(dataContext)!!
            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex;
            ideView!!.directories.forEach { dir ->
                if (projectFileIndex.isInSourceContent(dir.virtualFile))
                    return true;
            }
        }
        return false;
    }

    override fun postProcess(createdElement: PsiFile, templateName: String, customProperties: MutableMap<String, String>?) {
        if (templateName == "TornadoFX FXML View") {
            FileTemplate
            createFileFromTemplate(createdElement.name.substringBefore(".") + ".fxml", "TornadoFX FXML ViewResource", createdElement.containingDirectory)
        }
    }
}