package no.tornado.tornadofx.idea;

import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class TornadoFXModuleType extends ModuleType<TornadoFXModuleBuilder> implements PluginIcons {
	private static final TornadoFXModuleType INSTANCE = new TornadoFXModuleType();

	public TornadoFXModuleType() {
		super("TornadoFX");
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
		return ACTION;
	}

	public Icon getNodeIcon(@Deprecated boolean isOpened) {
		return ACTION;
	}

	public static TornadoFXModuleType getInstance() {
		return INSTANCE;
	}

	@NotNull
	public TornadoFXModuleBuilder createModuleBuilder() {
		return new TornadoFXModuleBuilder();
	}
}
