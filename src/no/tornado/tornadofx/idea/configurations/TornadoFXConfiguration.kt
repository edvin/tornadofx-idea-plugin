package no.tornado.tornadofx.idea.configurations

import com.intellij.diagnostic.logging.LogConfigurationPanel
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element

class TornadoFXConfiguration(project: Project, factory: ConfigurationFactory, name: String?) : ApplicationConfiguration(name, project, factory) {
    enum class RunType { App, View }

    @JvmField
    var RUN_TYPE = RunType.App
    @JvmField
    var VIEW_CLASS_NAME: String? = null

    override fun getState(executor: Executor, environment: ExecutionEnvironment) = ViewCommandLineState(environment)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        val group = SettingsEditorGroup<TornadoFXConfiguration>()
        group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), TornadoFXSettingsEditor(project))
        JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group)
        group.addEditor(ExecutionBundle.message("logs.tab.title"), LogConfigurationPanel<TornadoFXConfiguration>())
        return group
    }

    override fun checkConfiguration() {
        JavaParametersUtil.checkAlternativeJRE(this)

        if (RUN_TYPE == RunType.App) {
            if (MAIN_CLASS_NAME.isNullOrBlank()) {
                throw RuntimeConfigurationWarning("No App Class specified")
            } else {
                val psiClass = configurationModule.checkModuleAndClassName(MAIN_CLASS_NAME, "No App Class specified!")
                if (!TornadoFXSettingsEditor.isAppClass(psiClass))
                    throw RuntimeConfigurationWarning("Specified App Class does not inherit from tornadofx.App")
            }
        } else {
            val psiClass = configurationModule.checkModuleAndClassName(VIEW_CLASS_NAME, "No View Class specified!")
            if (!TornadoFXSettingsEditor.isViewClass(psiClass))
                throw RuntimeConfigurationWarning("Specified View Class does not inherit from tornadofx.View")
        }

        ProgramParametersUtil.checkWorkingDirectoryExist(this, project, configurationModule.module)
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this)
    }

    inner class ViewCommandLineState(environment: ExecutionEnvironment) : ApplicationConfiguration.JavaApplicationCommandLineState<TornadoFXConfiguration>(this, environment) {
        override fun createJavaParameters(): JavaParameters? {
            val params = super.createJavaParameters()!!

            if (RUN_TYPE == RunType.View)
                params.programParametersList.add("--view-class=$VIEW_CLASS_NAME")

            return params
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        VIEW_CLASS_NAME = element.getAttributeValue("view-class")
        RUN_TYPE = RunType.valueOf(element.getAttributeValue("run-type"))
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("run-type", RUN_TYPE.toString())

        if (VIEW_CLASS_NAME != null)
            element.setAttribute("view-class", VIEW_CLASS_NAME)
    }

}