package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.KtFunctionPsiElementCellRenderer
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JList

class AddTableViewColumns : PsiElementBaseIntentionAction() {
    override fun getText() = "Add TableView Columns..."

    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) =
            getFunctionDescriptor(element)?.returnType?.getJetTypeFqName(false) == "javafx.scene.control.TableView"

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val declaration = PsiTreeUtil.getParentOfType(element, KtCallableDeclaration::class.java)!!
        val modelPsiClass = getModelPsiClass(element, project) ?: return

        val dialog = ColumnsDialog(modelPsiClass)
        dialog.show()

        if (dialog.isOK) {
            object : WriteCommandAction.Simple<String>(project, element.containingFile) {
                override fun run() {
                    val factory = KtPsiFactory(project)
                    val target = PsiTreeUtil.findChildrenOfType(declaration, KtBlockExpression::class.java).last()
                    val modelName = modelPsiClass.name
                    val properties = dialog.fields.items

                    properties.forEach {
                        val colName = niceColumnName(it)
                        val expr = factory.createExpression("column(\"$colName\", $modelName::${it.name})")
                        if (target.children.isNotEmpty()) target.add(factory.createNewLine())
                        target.add(expr)
                    }

                    addImports(element, modelPsiClass)
                }

                private fun niceColumnName(it: PsiMethod): String {
                    val stripped = it.name.let {
                        it.first().toUpperCase() + it.substring(1).replace(Regex("Property$"), "")
                    }
                    return stripped.replace(Regex("([A-Z]){1}"), " $1").trim()
                }

            }.execute()

        }

    }

    private fun getModelPsiClass(element: PsiElement, project: Project): PsiClass? {
        val descriptor = getFunctionDescriptor(element)!!
        val returnType = descriptor.returnType!!
        val modelTypeProjection = returnType.arguments[0]!!
        val modelTypeFq = modelTypeProjection.type.getJetTypeFqName(false)
        return getPsiClass(project, modelTypeFq)
    }

    // ShortenReferences can't handle tornadofx.column, so imports are added manually
    private fun addImports(element: PsiElement, psiClass: PsiClass) {
        val importsFactory = KtImportsFactory(element.project)
        val ktFile = PsiTreeUtil.getParentOfType(element, KtFile::class.java)!!

        val imports = ktFile.importList!!.imports

        for (fqName in listOf("tornadofx.column", psiClass.qualifiedName!!))
            if (imports.find { it.importedFqName.toString() == fqName } == null)
                ktFile.importList?.add(importsFactory.createImportDirective(ImportPath(fqName)))
    }

    private fun getPsiClass(project: Project, modelTypeFq: String): PsiClass? =
            JavaPsiFacade.getInstance(project).findClass(modelTypeFq, project.projectScope())

    private fun getFunctionDescriptor(element: PsiElement): FunctionDescriptor? {
        val declaration = PsiTreeUtil.getParentOfType(element, KtCallableDeclaration::class.java)
        val descriptor = declaration?.resolveToDescriptor()
        return if (descriptor is FunctionDescriptor) descriptor else null
    }

    inner class ColumnsDialog(psiClass: PsiClass) : DialogWrapper(psiClass.project) {
        val fieldList: JList<PsiMethod>
        val component: JComponent
        val fields: CollectionListModel<PsiMethod>

        init {
            title = "Add TableView Columns from ${psiClass.name}"

            val properties = getJavaFXProperties(psiClass)
            fields = CollectionListModel(properties)
            fieldList = JList(fields)
            fieldList.cellRenderer = KtFunctionPsiElementCellRenderer()
            val decorator = ToolbarDecorator.createDecorator(fieldList)
            decorator.disableAddAction()

            val panel = decorator.createPanel()
            component = LabeledComponent.create(panel, "Choose columns to add from ${psiClass.name}")
            fieldList.requestFocus()
            init()
        }

        override fun createCenterPanel(): JComponent? {
            return component
        }

        override fun getOKAction(): Action {
            return super.getOKAction()
        }
    }

    private fun getJavaFXProperties(psiClass: PsiClass): List<PsiMethod> = psiClass.allMethods
            .filter { it.name.endsWith("Property") }
            .filter { it.returnType?.canonicalText?.contains("javafx.beans.property") ?: false }
}
