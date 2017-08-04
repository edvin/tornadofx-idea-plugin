package no.tornado.tornadofx.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

class FXTools {
    companion object {
        fun isComponent(psiClass: PsiClass) = isType("tornadofx.Component", psiClass)
        fun isFragment(psiClass: PsiClass) = isType("tornadofx.Fragment", psiClass)
        fun isUIComponent(psiClass: PsiClass) = isType("tornadofx.UIComponent", psiClass)
        fun isApp(psiClass: PsiClass) = isType("tornadofx.App", psiClass)
        fun isView(psiClass: PsiClass) = isType("tornadofx.View", psiClass)
        fun isStylesheet(psiClass: PsiClass) = isType("tornadofx.Stylesheet", psiClass)

        fun isType(type: String, psiClass: PsiClass): Boolean {
            for (supa in psiClass.supers)
                if (type == supa.qualifiedName) {
                    return true
                } else {
                    val superIs = isType(type, supa)
                    if (superIs) return true
                }

            return false
        }

        fun isTornadoFXType(psiClass: PsiClass): Boolean {
            for (supa in psiClass.supers)
                if (supa.qualifiedName?.startsWith("tornadofx.") ?: false) {
                    return true
                } else {
                    val superIs = isTornadoFXType(supa)
                    if (superIs) return true
                }

            return false
        }

        fun containsTornadoFXImports(file: KtFile) = file.importList?.imports?.find {
            it.text.contains("tornadofx")
        } != null

        fun psiClass(className: String, project: Project) =
                JavaPsiFacade.getInstance(project).findClass(className, project.projectScope())

        fun isJavaFXProperty(type: KotlinType?) = type?.supertypes()?.find { it.getJetTypeFqName(false) == "javafx.beans.property.Property" } != null
    }

}


/**
 * A list of all source roots from all modules
 */
fun Project.allRoots(): List<VirtualFile> = allModules()
        .map { ModuleRootManager.getInstance(it).modifiableModel }
        .flatMap { it.sourceRoots.toList() }

fun Project.firstModuleWithTornadoFXLib() = allModules().filter {
    JavaPsiFacade.getInstance(this).findClass("tornadofx.App", it.moduleWithLibrariesScope) != null
}.firstOrNull()