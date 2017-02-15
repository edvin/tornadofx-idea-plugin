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
        CodeStyleSettingsManager settingsManager = CodeStyleSettingsManager.getInstance();
        KotlinCodeStyleSettings settings = settingsManager.getCurrentSettings().getCustomSettings(KotlinCodeStyleSettings.class);
        if (!settings.PACKAGES_TO_USE_STAR_IMPORTS.contains("tornadofx")) {
            settings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new PackageEntry(false, "tornadofx", false));
        }
    }
}
