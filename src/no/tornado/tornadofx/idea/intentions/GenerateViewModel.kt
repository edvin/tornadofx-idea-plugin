package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.FXTools
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil.getDeclarationReturnType
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.KotlinType

class GenerateViewModel : PsiElementBaseIntentionAction() {
    override fun getText() = "Generate ViewModel"
    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        if (element.isWritable && element.language == KotlinLanguage.INSTANCE) {
            val ktClass = element as? KtClass ?: PsiTreeUtil.getParentOfType(element, KtClass::class.java)

            if (ktClass != null) {
                val psiFacade = JavaPsiFacade.getInstance(project)
                val psiClass = psiFacade.findClass(ktClass.fqName.toString(), project.allScope())
                return psiClass != null
            }
        }

        return false
    }

    private class PropDesc(val name: String, val accessor: String, val type: KotlinType?) {
        constructor(param: KtParameter) : this(param.name!!, param.name!!, getDeclarationReturnType(param))
        constructor(prop: KtProperty) : this(prop.name!!, prop.name!!, getDeclarationReturnType(prop))
        constructor(method: KtNamedFunction) : this(method.name!!.replace(Regex("Property$"), ""), "${method.name}()", getDeclarationReturnType(method))
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val sourceClass = element as? KtClass ?: PsiTreeUtil.getParentOfType(element, KtClass::class.java)!!

        object : WriteCommandAction.Simple<String>(project, element.containingFile) {
            override fun run() {
                val factory = KtPsiFactory(project)
                val ktClass = element.containingFile.addAfter(factory.createClass("class ${sourceClass.name}Model : tornadofx.ItemViewModel<${sourceClass.name}>()"), sourceClass) as KtElement
                ShortenReferences().process(ktClass)

                val ktClassBody = ktClass.add(factory.createEmptyClassBody())
                ktClassBody.addAfter(factory.createNewLine(), ktClassBody)

                val constructorParams = sourceClass.getPrimaryConstructorParameters()
                        .filter { it.hasValOrVar() && !it.isVarArg && it.name != null }
                        .map(::PropDesc)

                val properties = sourceClass.getBody()?.properties?.filterNot { it.name == null }?.filterNot { it.hasDelegate() }?.map(::PropDesc) ?: emptyList()

                val fxPropertyFunctions = sourceClass.getBody()?.declarations
                        ?.filter { it is KtNamedFunction }
                        ?.filter { it.name?.endsWith("Property") ?: false }
                        ?.map { PropDesc(it as KtNamedFunction) }
                        ?: emptyList()

                val fxPropertyFnNames = fxPropertyFunctions.map { it.name.replace(Regex("Property$"), "") }
                val propertiesWithoutFunctionOverlaps = properties
                        .filterNot { fxPropertyFnNames.contains(it.name) }

                (propertiesWithoutFunctionOverlaps.reversed() + fxPropertyFunctions.reversed() + constructorParams.reversed()).forEach { param ->
                    val paramName = param.name.replace(Regex("Property$"), "")
                    val s = StringBuilder("val $paramName = bind { ")

                    if (FXTools.isJavaFXProperty(param.type)) {
                        s.append("item?.${param.accessor} }")
                    } else {
                        val typeName = param.type?.nameIfStandardType?.toString()
                        val propType = when (typeName) {
                            "Int" -> "Integer"
                            "Long" -> "Long"
                            "Boolean" -> "Boolean"
                            "Float" -> "Float"
                            "Double" -> "Double"
                            "String" -> "String"
                            else -> "Object"
                        }
                        s.append("if (item == null) javafx.beans.property.Simple${propType}Property() else javafx.beans.property.Simple${propType}Property(item!!.${param.name}) }")
                    }

                    val declaration = ktClassBody.addAfter(factory.createProperty(s.toString()), ktClassBody.firstChild) as KtElement

                    ShortenReferences().process(declaration)
                }
            }
        }.execute()


    }

}