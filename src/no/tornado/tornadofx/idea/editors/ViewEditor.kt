package no.tornado.tornadofx.idea.editors

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ViewEditor(val project: Project, val file: VirtualFile) : FileEditor {
    val panel = JPanel()

    init {
        val psiFile = file.toPsiFile(project)!!
        val ktClass = PsiTreeUtil.findChildOfType(psiFile, KtClass::class.java)!!
        val psiFacade = JavaPsiFacade.getInstance(project)
        panel.add(JLabel("View Editor"))
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        return null
    }

    override fun dispose() {
    }

    override fun getName(): String {
        return "View Editor"
    }

    override fun getStructureViewBuilder(): StructureViewBuilder? {
        return null
    }

    override fun setState(state: FileEditorState) {
    }

    override fun getComponent(): JComponent {
        return panel;
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null;
    }

    override fun deselectNotify() {
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null;
    }

    override fun isValid(): Boolean {
        return true;
    }

    override fun isModified(): Boolean {
        return false;
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState {
        return FileEditorState { otherState, level -> true; }
    }

    override fun selectNotify() {
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return null;
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    }
}