package no.tornado.tornadofx.idea;

import com.intellij.openapi.module.ModuleType;
import no.tornado.tornadofx.idea.icons.PluginIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class TornadoFXModuleType extends ModuleType<TornadoFXModuleBuilder> {
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
		return PluginIcons.ACTION;
	}

	public Icon getNodeIcon(@Deprecated boolean isOpened) {
		return PluginIcons.ACTION;
	}

	public static TornadoFXModuleType getInstance() {
		return INSTANCE;
	}

	@NotNull
	public TornadoFXModuleBuilder createModuleBuilder() {
		return new TornadoFXModuleBuilder();
	}
}
