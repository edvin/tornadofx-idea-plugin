package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath

class FXPropertyConverter : PsiElementBaseIntentionAction() {
    override fun getText() = "Convert to TornadoFX Property"

    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element.isWritable && element.language == KotlinLanguage.INSTANCE) {
            val prop = PsiTreeUtil.getParentOfType(element, KtProperty::class.java)
            return prop != null && !prop.isLocal
        }

        return false;
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        var prop = PsiTreeUtil.getParentOfType(element, KtProperty::class.java)!!
        val propName = prop.name!!
        val returnType = QuickFixUtil.getDeclarationReturnType(prop)

        object : WriteCommandAction.Simple<String>(project, element.containingFile) {
            override fun run() {
                val factory = KtPsiFactory(project)
                val ktClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java)!!
                val ktClassBody = PsiTreeUtil.getParentOfType(element, KtClassBody::class.java)!!

                val value = if (prop.hasInitializer() && prop.initializer!!.text != "null") prop.initializer!!.text else ""
                val typeDecl = if (value.isEmpty()) "<$returnType>" else ""

                val declaration = factory.createProperty("var $propName by property$typeDecl($value)")
                val propAccessor = factory.createFunction("fun ${propName}Property() = getProperty(${ktClass.name}::$propName)")

                ktClassBody.addAfter(propAccessor, prop)
                ktClassBody.addAfter(declaration, prop)

                addImports()

                prop.delete()
            }

            private fun addImports() {
                // ShortenReferences can't handle tornadofx.getProperty, so imports are added manually
                val importsFactory = KtImportsFactory(project)
                val ktFile = PsiTreeUtil.getParentOfType(element, KtFile::class.java)!!

                val imports = ktFile.importList!!.imports

                for (fqName in listOf("tornadofx.property", "tornadofx.getProperty"))
                    if (imports.find { it.importedFqName.toString() == fqName } == null)
                        ktFile.importList?.add(importsFactory.createImportDirective(ImportPath(fqName)))
            }
        }.execute()
    }

}
