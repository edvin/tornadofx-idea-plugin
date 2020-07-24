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
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class ViewEditor(val project: Project, val myFile: VirtualFile) : FileEditor {
    val panel = JFXPanel()
    val wrapper = StackPane()

    override fun getFile() = myFile

    init {
//        val psiFile = file.toPsiFile(project)!!
//        val ktClass = PsiTreeUtil.findChildOfType(psiFile, KtClass::class.java)!!
//        val psiFacade = JavaPsiFacade.getInstance(project)
        Platform.setImplicitExit(false)
        createHierarchy()
        panel.scene = Scene(wrapper)
    }

    private fun createHierarchy() {
        val psiFile = file.toPsiFile(project)!!
        val ktClass = PsiTreeUtil.findChildOfType(psiFile, KtClass::class.java)!!
        //val psiFacade = JavaPsiFacade.getInstance(project)

        val root = ktClass.getProperties().find { it.name == "root" }
        val rootType = QuickFixUtil.getDeclarationReturnType(root)!!

        println(rootType)

        for (child in root!!.children) {
            println("Child: " + child)

            if (child is KtCallExpression) {
                println("Reference element: " + child.reference?.element?.firstChild)
                println("Args: " + child.lambdaArguments)
            }
        }
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
        return FileEditorState { _, _ -> true }
    }

    override fun selectNotify() {
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return null;
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    }
}