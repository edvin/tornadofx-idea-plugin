package no.tornado.tornadofx.idea.configurations

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

class TornadoFXConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    companion object {
        val FACTORY_NAME = "TornadoFX Configuration Factory"
    }

    override fun createTemplateConfiguration(project: Project) = TornadoFXConfiguration(project, this, FACTORY_NAME)

    override fun getName() = FACTORY_NAME
}