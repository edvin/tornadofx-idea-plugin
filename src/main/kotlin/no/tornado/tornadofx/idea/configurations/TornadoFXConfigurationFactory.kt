package no.tornado.tornadofx.idea.configurations

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

/*
com.intellij.util.DeprecatedMethodException: The default implementation of ConfigurationFactory.getId is deprecated,
 you need to override it in no.tornado.tornadofx.idea.configurations.TornadoFXConfigurationFactory.
 The default implementation delegates to 'getName' which may be localized but return value of this method must not depend on current localization.
 */
class TornadoFXConfigurationFactory(type: ConfigurationType) : SimpleConfigurationType(
    type.id, type.displayName, icon = NotNullLazyValue.createConstantValue(type.icon)
) {
    companion object {
        val FACTORY_NAME = "TornadoFX Configuration Factory"
    }

    override fun createTemplateConfiguration(project: Project) = TornadoFXConfiguration(project, this, name).apply {
        if (project.allModules().isNotEmpty())
            setModule(project.allModules().first())
    }

    /*
    override fun getName() = FACTORY_NAME
    override fun getId(): String = FACTORY_NAME
     */
}