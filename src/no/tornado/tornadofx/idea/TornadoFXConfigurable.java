package no.tornado.tornadofx.idea;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import no.tornado.tornadofx.idea.TornadoFXSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

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
        private JPanel mainPanel;
        private JCheckBox alternativePropertySyntaxCheckbox;
        private HyperlinkLabel propertySyntaxLink;

        public ConfigUI() {
            propertySyntaxLink.setHyperlinkText("More");
            propertySyntaxLink.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                protected void hyperlinkActivated(HyperlinkEvent e) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://edvin.gitbooks.io/tornadofx-guide/content/Appendix%20A%20-%20Supplementary%20Topics.html#alternative-property-syntax"));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }

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
