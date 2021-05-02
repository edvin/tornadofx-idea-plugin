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
//import com.intellij.openapi.project.DumbModePermission
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.IncorrectOperationException
import no.tornado.tornadofx.idea.allRoots
import org.apache.velocity.runtime.parser.ParseException
import org.jetbrains.kotlin.psi.KtFile

class NewViewAction : AnAction() {
    val log: Logger = Logger.getInstance("#no.tornado.tornadofx.idea.actions.NewViewAction")

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
            val resourcesRoot = project.allRoots().find { it.name == "resources" }

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



//        val dumbService = DumbService.getInstance(dir.project)
//        object : DumbModeTask() {}
//        dumbService.queueTask()
//        dumbService.completeJustSubmittedTasks()

        //DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, Runnable {
        val element: PsiElement
        val project = dir.project
        try {
            val props = FileTemplateManager.getInstance(dir.project).defaultProperties
            props["fqRootType"] = rootType
            props["rootType"] = rootType.substringAfterLast(".")
            props["initType"] = if(hasBuilder(project, rootType)) "builder" else "construct"
            props["viewType"] = viewType
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
        //})

        return file
    }

    private fun hasBuilder(project: Project, rootType: String): Boolean {
        val builderName = rootType.substringAfterLast(".").toLowerCase()
        val layouts = JavaPsiFacade.getInstance(project).findClass("tornadofx.LayoutsKt", GlobalSearchScope.allScope(project))
        if (layouts == null) {
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder("TornadoFX was not found on your classpath. You must add it to reliably use the TornadoFX plugin.", MessageType.WARNING, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.atRight)
            return true
        } else {
            return layouts.findMethodsByName(builderName, true).isNotEmpty()
        }
    }

}