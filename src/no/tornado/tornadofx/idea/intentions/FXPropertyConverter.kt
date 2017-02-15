package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.TornadoFXSettings
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.replace

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

    fun addForParam(param: KtParameter, project: Project, element: PsiElement) {
        val paramName = param.name!!
        val returnType = QuickFixUtil.getDeclarationReturnType(param)!!

        object : WriteCommandAction.Simple<String>(project, element.containingFile) {
            override fun run() {
                val factory = KtPsiFactory(project)
                val ktClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java)!!
                var ktClassBody = PsiTreeUtil.getChildOfType(ktClass, KtClassBody::class.java)
                if (ktClassBody == null) {
                    ktClassBody = factory.createEmptyClassBody()
                    ktClassBody = ktClass.add(ktClassBody) as KtClassBody
                }

                val importList = mutableSetOf<String>()
                val value = if (param.hasDefaultValue()) param.defaultValue?.text ?: "" else ""

                val (declaration, propAccessor) = if (TornadoFXSettings.getInstance().alternativePropertySyntax) {
                    createAlternativePropertyElements(factory, paramName, returnType, value, importList)
                } else {
                    createPropertyElements(factory, paramName, returnType, ktClass, value, importList)
                }

                if (ktClassBody.children.isEmpty()) {
                    ktClassBody.addAfter(factory.createNewLine(), ktClassBody)
                    shortenIfNeeded(ktClassBody.addAfter(propAccessor, ktClassBody.firstChild))
                    shortenIfNeeded(ktClassBody.addAfter(declaration, ktClassBody.firstChild))
                } else {
                    ktClassBody.addAfter(factory.createNewLine(), ktClassBody.firstChild)
                    shortenIfNeeded(ktClassBody.addAfter(propAccessor, ktClassBody.firstChild))
                    shortenIfNeeded(ktClassBody.addAfter(declaration, ktClassBody.firstChild))
                }

                addImports(element, project, importList)

                while(param.nextSibling?.node?.text == "," || param.nextSibling is PsiWhiteSpace)
                    param.nextSibling.delete()

                while(param.prevSibling?.node?.text == "," || param.prevSibling is PsiWhiteSpace)
                    param.prevSibling.delete()

                param.delete()
            }

        }.execute()
    }

    fun addForProp(prop: KtProperty, project: Project, element: PsiElement) {
        val propName = prop.name!!
        val returnType = QuickFixUtil.getDeclarationReturnType(prop)!!

        object : WriteCommandAction.Simple<String>(project, element.containingFile) {
            override fun run() {
                val factory = KtPsiFactory(project)
                val ktClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java)
                val ktClassBody = PsiTreeUtil.getParentOfType(element, KtClassBody::class.java)!!

                val value = if (prop.hasInitializer() && prop.initializer!!.text != "null") prop.initializer!!.text else ""

                val importList = mutableSetOf<String>()

                val (declaration, propAccessor) = if (TornadoFXSettings.getInstance().alternativePropertySyntax) {
                    createAlternativePropertyElements(factory, propName, returnType, value, importList)
                } else {
                    if (ktClass == null) {
                        HintManager.getInstance().showErrorHint(FileEditorManager.getInstance(project).selectedTextEditor!!, "Traditional property syntax not available in package level objects. Switch to alternative syntax or try with a class object.")
                        return
                    }
                    createPropertyElements(factory, propName, returnType, ktClass, value, importList)
                }

                ktClassBody.addAfter(factory.createNewLine(), prop)
                shortenIfNeeded(ktClassBody.addAfter(propAccessor, prop))
                ktClassBody.addAfter(factory.createWhiteSpace(), prop)
                shortenIfNeeded(ktClassBody.addAfter(declaration, prop))

                addImports(element, project, importList)

                prop.delete()
            }

        }.execute()
    }

    companion object {
        fun createAlternativePropertyElements(factory: KtPsiFactory, paramName: String, returnType: KotlinType, value: String, importList: MutableSet<String>): Pair<PsiElement, PsiElement> {
            val typeName = (returnType.nameIfStandardType ?: returnType.lowerIfFlexible()).toString().replace(Regex("\\?$"), "")

            val propType = when (typeName) {
                "Int" -> "SimpleIntegerProperty"
                "Long" -> "SimpleLongProperty"
                "Boolean" -> "SimpleBooleanProperty"
                "Float" -> "SimpleFloatProperty"
                "Double" -> "SimpleDoubleProperty"
                "String" -> "SimpleStringProperty"
                else -> "SimpleObjectProperty<$typeName>"
            }

            val fullPropName = "javafx.beans.property.$propType"

            importList.addAll(listOf("tornadofx.getValue", "tornadofx.setValue"))

            return factory.createProperty("val ${paramName}Property = $fullPropName($value)") to
                    factory.createProperty("var $paramName by ${paramName}Property")
        }

        fun createPropertyElements(factory: KtPsiFactory, paramName: String, returnType: KotlinType, ktClass: KtClass, value: String, importList: MutableSet<String>): Pair<PsiElement, PsiElement> {
            importList.addAll(listOf("tornadofx.property", "tornadofx.getProperty"))

            return factory.createProperty("var $paramName by property<${returnType.nameIfStandardType ?: returnType.lowerIfFlexible() ?: returnType}>($value)") to
                    factory.createFunction("fun ${paramName}Property() = getProperty(${ktClass.name}::$paramName)")
        }

        private fun addImports(element: PsiElement, project: Project, importList: Iterable<String>) {
            // ShortenReferences can't handle tornadofx.getProperty, so imports are added manually
            val importsFactory = KtImportsFactory(project)
            val ktFile = PsiTreeUtil.getParentOfType(element, KtFile::class.java)!!

            val imports = ktFile.importList!!.imports

            for (fqName in importList)
                if (imports.find { it.importedFqName.toString() == fqName } == null)
                    ktFile.importList?.add(importsFactory.createImportDirective(ImportPath(fqName)))
        }

        private fun shortenIfNeeded(declaration: PsiElement) {
            if (declaration is KtElement)
                ShortenReferences().process(declaration)
        }

    }
}
