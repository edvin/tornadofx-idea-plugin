package no.tornado.tornadofx.idea;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;

public class TornadoFXModuleBuilder extends ModuleBuilder implements PluginIcons {

	public void setupRootModel(ModifiableRootModel model) throws ConfigurationException {
	}

	public ModuleType getModuleType() {
		return TornadoFXModuleType.INSTANCE;
	}

	public boolean isTemplateBased() {
		return true;
	}

	public boolean isTemplate() {
		return false;
	}

}
