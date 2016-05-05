package no.tornado.tornadofx.idea;

import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TornadoFXModuleType extends ModuleType<TornadoFXModuleBuilder> implements PluginIcons {
	public static final TornadoFXModuleType INSTANCE = new TornadoFXModuleType("TornadoFX");

	protected TornadoFXModuleType(@NotNull @NonNls String id) {
		super(id);
	}

	@NotNull
	public String getName() {
		return "TornadoFX";
	}

	@NotNull
	public String getDescription() {
		return "TornadoFX Module";
	}

	public Icon getBigIcon() {
		return ACTION_ICON;
	}

	public Icon getNodeIcon(@Deprecated boolean isOpened) {
		return ACTION_ICON;
	}


	@NotNull
	public TornadoFXModuleBuilder createModuleBuilder() {
		return new TornadoFXModuleBuilder();
	}
}
