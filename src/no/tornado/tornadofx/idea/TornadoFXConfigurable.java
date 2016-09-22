package no.tornado.tornadofx.idea;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import no.tornado.tornadofx.idea.TornadoFXSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TornadoFXConfigurable implements SearchableConfigurable {
    private TornadoFXSettings settings;
    private ConfigUI ui;

    public TornadoFXConfigurable(TornadoFXSettings settings) {
        this.settings = settings;
    }

    @NotNull
    @Override
    public String getId() {
        return getHelpTopic();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "TornadoFX";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "preferences.TornadoFX";
    }

    @Override
    public boolean isModified() {
        return ui.isModified(settings);
    }

    @Override
    public void disposeUIResources() {
        ui = null;
    }

    @Override
    public void apply() throws ConfigurationException {
        ui.apply(settings);
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ui = new ConfigUI();
        return ui.mainPanel;
    }

    @Override
    public void reset() {
        ui.reset(settings);
    }

    public static class ConfigUI {
        JPanel mainPanel;
        JCheckBox alternativePropertySyntaxCheckbox;

        void reset(TornadoFXSettings settings) {
            alternativePropertySyntaxCheckbox.setSelected(settings.getAlternativePropertySyntax());
        }

        void apply(TornadoFXSettings settings) {
            settings.setAlternativePropertySyntax(alternativePropertySyntaxCheckbox.isSelected());
        }

        boolean isModified(TornadoFXSettings settings) {
            return settings.getAlternativePropertySyntax() != alternativePropertySyntaxCheckbox.isSelected();
        }

    }
}
