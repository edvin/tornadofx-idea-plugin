package no.tornado.tornadofx.idea.configurations

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import no.tornado.tornadofx.idea.FXTools
import no.tornado.tornadofx.idea.PluginIcons
import no.tornado.tornadofx.idea.facet.TornadoFXFacet
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.psi.KtClass

class TornadoFXRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element is KtClass) {
            if (TornadoFXFacet.get(element.project) == null) return null

            val psiFacade = JavaPsiFacade.getInstance(element.project)
            val psiClass = psiFacade?.findClass(element.fqName.toString(), element.project.allScope()) ?: return null

            val isApp = FXTools.isApp(psiClass)
            val isView = FXTools.isUIComponent(psiClass)

            if (isApp || isView) {
                return Info(
                        PluginIcons.ACTION,
                        Function<PsiElement, String> { "Run TornadoFX ${if (isApp) "Application" else "View"}" },
                        *ExecutorAction.getActions(0)
                )
            }
        }
        return null
    }
}