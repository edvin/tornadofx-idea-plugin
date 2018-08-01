package no.tornado.tornadofx.idea.editors

import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.FXTools
import org.jdom.Element
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.psi.KtClass

class ViewEditorProvider : ApplicationComponent, FileEditorProvider {
    override fun createEditor(project: Project, file: VirtualFile) = ViewEditor(project, file)

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
    }

    override fun getPolicy() = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState {
        return FileEditorState { _, _ -> true }
    }

    override fun getEditorTypeId() = "tornadofx-views"

    override fun accept(project: Project, file: VirtualFile): Boolean {
        val psiFile = file.toPsiFile(project)
        if (psiFile != null) {
            val ktClass = PsiTreeUtil.findChildOfType(psiFile, KtClass::class.java)
            if (ktClass != null) {
                val psiFacade = JavaPsiFacade.getInstance(project)
                val psiClass = psiFacade.findClass(ktClass.fqName!!.asString(), project.projectScope())
                if (psiClass != null) return FXTools.isUIComponent(psiClass)
            }
        }
        return false
    }

    override fun disposeComponent() {
    }

    override fun initComponent() {
    }

    override fun getComponentName() = "Live View"
}