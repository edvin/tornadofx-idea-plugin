package no.tornado.tornadofx.idea;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.PackageEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;

public class FXPreloader extends PreloadingActivity {
    @Override
    public void preload(@NotNull ProgressIndicator progressIndicator) {
        KotlinCodeStyleSettings settings = CodeStyleSettingsManager.getInstance().getCurrentSettings().getCustomSettings(KotlinCodeStyleSettings.class);
        if (!settings.PACKAGES_TO_USE_STAR_IMPORTS.contains("tornadofx")) {
            settings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new PackageEntry(false, "tornadofx", false));
            // TODO: Persist, so we don't need to do this every time the plugin is loaded
        }
    }
}
