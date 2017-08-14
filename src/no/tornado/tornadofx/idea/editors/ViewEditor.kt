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
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.util.PsiTreeUtil
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.control.TreeItem
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import tornadofx.*
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ViewEditor(val project: Project, val file: VirtualFile) : FileEditor {
    val hierarchyModel: HierarchyModel by inject()
    val panel = JFXPanel()

    init {
        Platform.setImplicitExit(false)
        val view = find(ViewBuilder::class)
        panel.scene = Scene(view.root)

        view.properties["tornadofx.scene"] = panel.scene
        FX.applyStylesheetsTo(panel.scene)
        FX.initialized.value = true

        val rootProperty = rootPropertyOrNull()
        rootProperty?.let { hierarchyModel.computeNodeHirchay(it) }

        PsiManager.getInstance(project).addPsiTreeChangeListener(object: PsiTreeChangeListener {
            override fun beforePropertyChange(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun childReplaced(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun childrenChanged(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun beforeChildAddition(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun beforeChildReplacement(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun beforeChildrenChange(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun childMoved(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun childRemoved(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun beforeChildMovement(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun childAdded(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun beforeChildRemoval(p0: PsiTreeChangeEvent) {updateHirachy()}
            override fun propertyChanged(p0: PsiTreeChangeEvent) {updateHirachy()}
        })
    }

    inline fun <reified T> inject(overrideScope: Scope = DefaultScope, params: Map<String, Any?>? = null): ReadOnlyProperty<FileEditor, T>
            where T : Component, T : ScopedInstance = object : ReadOnlyProperty<FileEditor, T> {
        override fun getValue(thisRef: FileEditor, property: KProperty<*>) = find(T::class, overrideScope, params)
    }


    private fun updateHirachy() {
        val rootProperty = rootPropertyOrNull()
        rootProperty?.let { hierarchyModel.computeNodeHirchay(it) }
    }

    private fun rootPropertyOrNull(): KtProperty? {
        val psiFile = file.toPsiFile(project)!!
        val ktClass = PsiTreeUtil.findChildOfType(psiFile, KtClass::class.java)!!
        // TODO: check if it is a View Class
        val psiFacade = JavaPsiFacade.getInstance(project)

        val root = ktClass.getProperties().find { it.name == "root" }
//        val rootType = QuickFixUtil.getDeclarationReturnType(root)!!
        return root
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

class ViewBuilder: View() {
    val hirachyModel: HierarchyModel by inject()

    override val root = borderpane {
        left {
            vbox {
                squeezebox {
                    fold("Properties: Button") { }
                    fold("Layout: Button") { }
                }

                treeview<FxNode> {
                    useMaxHeight = true
                    isShowRoot = false
                    cellFormat { text = it.nodeName }

                    hirachyModel.fxNodeProperty.onChange {
                        if (it != null) {
                            // Set-up root
                            root = TreeItem(it)
                            root.isExpanded = true

                            populate(itemFactory = {
                                val item = TreeItem(it)
                                item.isExpanded = true
                                item
                            }){
                                if (it.value.children.isNotEmpty()) it.value.children
                                else null
                            }
                        }
                    }
                }
            }
        }
        center {
            stackpane {

            }
        }
        right {
            vbox {
                hbox {
                    label("Inspector:")
                    textfield()
                }
                squeezebox {
                    fold("Properties: Button") { }
                    fold("Layout: Button") { }
                }
            }
        }
    }
}
