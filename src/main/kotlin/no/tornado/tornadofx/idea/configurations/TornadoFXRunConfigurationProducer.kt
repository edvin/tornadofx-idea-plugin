package no.tornado.tornadofx.idea.configurations

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import no.tornado.tornadofx.idea.FXTools.isApp
import no.tornado.tornadofx.idea.configurations.TornadoFXConfiguration.RunType.App
import no.tornado.tornadofx.idea.configurations.TornadoFXConfiguration.RunType.View
import no.tornado.tornadofx.idea.firstModuleWithTornadoFXLib
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.psi.KtClass

class TornadoFXRunConfigurationProducer : LazyRunConfigurationProducer<TornadoFXConfiguration>() {
    private val tornadoFXConfigurationType by lazy { runConfigurationType<TornadoFXConfigurationType>() }


    override fun getConfigurationFactory(): ConfigurationFactory {
        return tornadoFXConfigurationType.configurationFactories[0]
    }

    override fun setupConfigurationFromContext(configuration: TornadoFXConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val ktClass = sourceElement.get() as? KtClass ?: return false
        val psiFacade = JavaPsiFacade.getInstance(ktClass.project)
        val psiClass = psiFacade.findClass(ktClass.fqName.toString(), ktClass.project.allScope()) ?: return false

        configuration.name = psiClass.name ?: ""

        configuration.setModule(context.project.firstModuleWithTornadoFXLib())

        if (isApp(psiClass)) {
            configuration.RUN_TYPE = App
            configuration.mainClassName = psiClass.qualifiedName
        } else {
            configuration.RUN_TYPE = View
            configuration.mainClassName = "tornadofx.App"
            configuration.viewClassName = psiClass.qualifiedClassNameForRendering()
        }
        return true
    }

    override fun isConfigurationFromContext(configuration: TornadoFXConfiguration, context: ConfigurationContext): Boolean {
        val element = context.location!!.psiElement
        if (element is KtClass) {
            val psiFacade = JavaPsiFacade.getInstance(element.project)
            val psiClass = psiFacade.findClass(element.fqName.toString(), element.project.allScope())!!

            if (configuration.name == psiClass.name) {
                val correctRunType = if (isApp(psiClass)) App else View
                return correctRunType == configuration.RUN_TYPE
            }
        }
        return false
    }
}
