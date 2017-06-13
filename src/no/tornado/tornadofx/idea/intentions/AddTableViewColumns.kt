package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import no.tornado.tornadofx.idea.facet.TornadoFXFacet
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.KtFunctionPsiElementCellRenderer
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.types.KotlinType
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JList

class AddTableViewColumns : PsiElementBaseIntentionAction() {
    override fun getText() = "Add TableView Columns..."

    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) =
            getReturnType(element)?.getJetTypeFqName(false) == "javafx.scene.control.TableView"
            && TornadoFXFacet.get(project) != null

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

                private fun niceColumnName(it: PsiMember): String {
                    val stripped = it.name.let {
                        it!!.first().toUpperCase() + it.substring(1).replace(Regex("Property$"), "")
                    }
                    return stripped.replace(Regex("([A-Z]){1}"), " $1").trim()
                }

            }.execute()

        }

    }

    private fun getModelPsiClass(element: PsiElement, project: Project): PsiClass? {
        val returnType = getReturnType(element) ?: return null
        val modelTypeProjection = returnType.arguments[0]
        val modelTypeFq = modelTypeProjection.type.getJetTypeFqName(false)
        return getPsiClass(project, modelTypeFq)
    }

    // ShortenReferences can't handle tornadofx.column, so imports are added manually
    private fun addImports(element: PsiElement, psiClass: PsiClass) {
        val importsFactory = KtImportsFactory(element.project)
        val ktFile = PsiTreeUtil.getParentOfType(element, KtFile::class.java)!!

        val imports = ktFile.importList!!.imports

        // TODO: Don't add import if class is in the same package as the class we're operating on
        listOf("tornadofx.column", psiClass.qualifiedName!!)
                .filter { fqName -> imports.find { it.importedFqName.toString() == fqName } == null }
                .forEach { ktFile.importList?.add(importsFactory.createImportDirective(ImportPath(FqName(it), false))) }
    }

    private fun getPsiClass(project: Project, modelTypeFq: String): PsiClass? =
            JavaPsiFacade.getInstance(project).findClass(modelTypeFq, project.projectScope())

    private fun getReturnType(element: PsiElement): KotlinType? {
        val memberDescriptor = getMemberDescriptor(element)
        return when (memberDescriptor) {
            is PropertyDescriptor -> memberDescriptor.getter?.returnType
            is FunctionDescriptor -> memberDescriptor.returnType
            else -> null
        }
    }

    private fun getMemberDescriptor(element: PsiElement): MemberDescriptor? {
        val declaration = PsiTreeUtil.getParentOfType(element, KtCallableDeclaration::class.java)
        val descriptor = declaration?.resolveToDescriptor()
        return if (descriptor is MemberDescriptor) descriptor else null
    }

    inner class ColumnsDialog(psiClass: PsiClass) : DialogWrapper(psiClass.project) {
        val fieldList: JList<PsiMember>
        val component: JComponent
        val fields: CollectionListModel<PsiMember>

        init {
            title = "Add TableView Columns from ${psiClass.name}"

            val candidates: List<PsiMember> = getJavaFXProperties(psiClass) + getJavaFXFields(psiClass)
            fields = CollectionListModel(candidates)
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
            .filterNot { it.name.fourthLetterIsUpperCase && (it.name.startsWith("get") || it.name.startsWith("set")) }

    private fun getJavaFXFields(psiClass: PsiClass): List<PsiField> = psiClass.allFields
            .filterNot { it.name?.endsWith("\$delegate") ?: false }

    private val String.fourthLetterIsUpperCase: Boolean
        get() = length > 3 && this[3].isUpperCase()
}

