package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
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
            if (prop != null) return !prop.isLocal

            val param = PsiTreeUtil.getParentOfType(element, KtParameter::class.java)
            if (param != null) return true
        }

        return false
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val prop = PsiTreeUtil.getParentOfType(element, KtProperty::class.java)
        if (prop != null) {
            addForProp(prop, project, element)
        } else {
            val param = PsiTreeUtil.getParentOfType(element, KtParameter::class.java)
            if (param != null) addForParam(param, project, element)
        }
   }

    private fun addForParam(param: KtParameter, project: Project, element: PsiElement) {
        val paramName = param.name!!
        val returnType = QuickFixUtil.getDeclarationReturnType(param)

        object : WriteCommandAction.Simple<String>(project, element.containingFile) {
            override fun run() {
                val factory = KtPsiFactory(project)
                val ktClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java)!!
                var ktClassBody = PsiTreeUtil.getChildOfType(ktClass, KtClassBody::class.java)
                if (ktClassBody == null) {
                    ktClassBody = factory.createEmptyClassBody()
                    ktClassBody = ktClass.add(ktClassBody) as KtClassBody
                }

                val typeDecl = "<$returnType>"

                val declaration = factory.createProperty("var $paramName by property$typeDecl()")
                val propAccessor = factory.createFunction("fun ${paramName}Property() = getProperty(${ktClass.name}::$paramName)")

                if (ktClassBody.children.isEmpty()) {
                    ktClassBody.addAfter(factory.createNewLine(), ktClassBody)
                    ktClassBody.addAfter(propAccessor, ktClassBody.firstChild)
                    ktClassBody.addAfter(declaration, ktClassBody.firstChild)
                } else {
                    ktClassBody.addAfter(factory.createNewLine(), ktClassBody.firstChild)
                    ktClassBody.addAfter(propAccessor, ktClassBody.firstChild)
                    ktClassBody.addAfter(declaration, ktClassBody.firstChild)
                }

                addImports()

                while(param.nextSibling?.node?.text == "," || param.nextSibling is PsiWhiteSpace)
                    param.nextSibling.delete()

                while(param.prevSibling?.node?.text == "," || param.prevSibling is PsiWhiteSpace)
                    param.prevSibling.delete()

                param.delete()
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

    private fun addForProp(prop: KtProperty, project: Project, element: PsiElement) {
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
