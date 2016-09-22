package no.tornado.tornadofx.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import no.tornado.tornadofx.idea.FXTools
import no.tornado.tornadofx.idea.FXTools.Companion.isTornadoFXType
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil.getDeclarationReturnType
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

class GenerateViewModel : PsiElementBaseIntentionAction() {
    override fun getText() = "Generate ViewModel"
    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        if (element.isWritable && element.language == KotlinLanguage.INSTANCE) {
            val ktClass = if (element is KtClass) element else PsiTreeUtil.getParentOfType(element, KtClass::class.java)

            if (ktClass != null) {
                val psiFacade = JavaPsiFacade.getInstance(project)
                val psiClass = psiFacade.findClass(ktClass.fqName.toString(), project.allScope())
                return psiClass != null && !isTornadoFXType(psiClass)
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
        val sourceClass = if (element is KtClass) element else PsiTreeUtil.getParentOfType(element, KtClass::class.java)!!
        val sourceVal = sourceClass.name?.toLowerCase() ?: "source"

        object : WriteCommandAction.Simple<String>(project, element.containingFile) {
            override fun run() {
                val factory = KtPsiFactory(project)
                val ktClass = element.containingFile.addAfter(factory.createClass("class ${sourceClass.name}Model(val $sourceVal: ${sourceClass.name}) : tornadofx.ViewModel()"), sourceClass) as KtElement
                ShortenReferences().process(ktClass)

                val ktClassBody = ktClass.add(factory.createEmptyClassBody())
                ktClassBody.addAfter(factory.createNewLine(), ktClassBody)

                val constructorParams = sourceClass.getPrimaryConstructorParameters()
                        .filter { it.hasValOrVar() && !it.isVarArg && it.name != null }
                        .map { PropDesc(it) }

                val properties = sourceClass.getBody()?.properties?.filterNot { it.name == null }?.map { PropDesc(it) } ?: emptyList()

                val fxPropertyFunctions = sourceClass.getBody()?.declarations
                        ?.filter { it is KtNamedFunction }
                        ?.filter { it.name?.endsWith("Property") ?: false }
                        ?.map { PropDesc(it as KtNamedFunction) }
                        ?: emptyList()

                val fxPropertyFnNames = fxPropertyFunctions.map { it.name.replace(Regex("Property$"), "") }
                val propertiesWithoutFunctionOverlaps = properties.filterNot { fxPropertyFnNames.contains(it.name) }

                (propertiesWithoutFunctionOverlaps.reversed() + fxPropertyFunctions.reversed() + constructorParams.reversed()).forEach { param ->
                    val s = StringBuilder("val ${param.name} = bind { ")

                    if (FXTools.isJavaFXProperty(param.type)) {
                        s.append("$sourceVal.${param.accessor} }")
                    } else {
                        val typeName = param.type?.nameIfStandardType?.toString()
                        val propType = when (typeName) {
                            "Int" -> "Integer"
                            "Long" -> "Long"
                            "Boolean" -> "Boolean"
                            "Float" -> "Float"
                            "String" -> "String"
                            else -> "Object"
                        }
                        s.append("javafx.beans.property.Simple${propType}Property($sourceVal.${param.name}) }");
                    }

                    val declaration = ktClassBody.addAfter(factory.createProperty(s.toString()), ktClassBody.firstChild) as KtElement

                    ShortenReferences().process(declaration)
                }
            }
        }.execute()


    }

}