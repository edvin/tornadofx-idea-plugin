package no.tornado.tornadofx.idea.actions

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.fileTemplates.actions.CreateFromTemplateActionBase
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbModePermission
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.IncorrectOperationException
import org.apache.velocity.runtime.parser.ParseException
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.psi.KtFile

class NewViewAction : AnAction() {
    val log = Logger.getInstance("#no.tornado.tornadofx.idea.actions.NewViewAction")

    override fun actionPerformed(e: AnActionEvent) {
        val dialog = NewViewDialog(e.project!!)
        dialog.show()
        if (dialog.isOK) {
            val templateName = dialog.myKindCombo.selectedName
            val fileName = dialog.myNameField.text
            val rootType = dialog.rootType.text
            val viewType = dialog.myTypeCombo.selectedItem as String

            val template = FileTemplateManager.getInstance(e.project!!).getInternalTemplate(templateName)
            val view = LangDataKeys.IDE_VIEW.getData(e.dataContext)!!

            val createdElement = createFileFromTemplate(fileName, template, view.orChooseDirectory!!, rootType, viewType)
            if (createdElement != null)
                postProcess(createdElement, templateName, rootType, viewType)
        }
    }


    fun postProcess(createdElement: PsiFile, templateName: String, rootType: String, viewType: String) {
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

            createFileFromTemplate(createdElement.name.substringBefore(".") + ".fxml", template, targetDir, rootType, viewType)
        }
    }

    fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory, rootType: String, viewType: String): PsiFile? {
        var file: PsiFile? = null

        DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, Runnable {
            val element: PsiElement
            val project = dir.project
            try {
                val props = FileTemplateManager.getInstance(dir.project).defaultProperties
                props.put("fqRootType", rootType)
                props.put("rootType", rootType.substringAfterLast("."))
                props.put("viewType", viewType)
                element = FileTemplateUtil.createFromTemplate(template, name, props, dir)
                val psiFile = element.containingFile

                val virtualFile = psiFile.virtualFile
                if (virtualFile != null) {
                    if (template.isLiveTemplateEnabled) {
                        CreateFromTemplateActionBase.startLiveTemplate(psiFile)
                    } else {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    }
                    file = psiFile
                }
            } catch (e: ParseException) {
                Messages.showErrorDialog(project, "Error parsing Velocity template: " + e.message, "Create File from Template")
            } catch (e: IncorrectOperationException) {
                throw e
            } catch (e: Exception) {
                log.error(e)
            }
        })

        return file
    }

}