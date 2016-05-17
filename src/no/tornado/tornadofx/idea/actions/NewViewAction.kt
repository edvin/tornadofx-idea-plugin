package no.tornado.tornadofx.idea.actions

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.psi.KtFile

class NewViewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = NewViewDialog(e.project!!)
        dialog.show()
        if (dialog.isOK) {
            val templateName = dialog.myKindCombo.selectedName
            println(templateName)
        }
    }


    fun postProcess(createdElement: PsiFile, templateName: String, customProperties: MutableMap<String, String>?) {
        // Create FXML companion to the source file
        if (templateName == "TornadoFX FXML View") {
            val project = createdElement.project
            val template = FileTemplateManager.getInstance(project).getInternalTemplate("TornadoFX FXML ViewResource")

            // Prefer resources folder as target for FXML file
            val rootModel = ModuleRootManager.getInstance(project.allModules().first()).modifiableModel
            val resourcesRoot = rootModel.sourceRoots.find { it.name == "resources" }

            val targetDir: PsiDirectory

            if (resourcesRoot != null) {
                // Split package components into list
                val pkgNames = (createdElement as KtFile).packageFqName.asString().split(".")

                // Make sure we have created all subfolders
                var virtualDirectory: VirtualFile = resourcesRoot
                pkgNames.forEach { pkgName ->
                    val existing = virtualDirectory.findChild(pkgName)
                    virtualDirectory = existing ?: virtualDirectory.createChildDirectory(null, pkgName)
                }

                // Convert target to a PSIDirectory
                targetDir = PsiManager.getInstance(project).findDirectory(virtualDirectory)!!
            } else {
                // Revert to creating the FXML file in same dir as the source file
                targetDir = createdElement.containingDirectory
            }

//            createFileFromTemplate(createdElement.name.substringBefore(".") + ".fxml", template, targetDir)
        }
    }
}