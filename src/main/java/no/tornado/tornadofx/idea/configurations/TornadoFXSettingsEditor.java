/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.tornado.tornadofx.idea.configurations;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ui.*;
import com.intellij.execution.util.JreVersionDetector;
import com.intellij.ide.util.ClassFilter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.EditorTextFieldWithBrowseButton;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static no.tornado.tornadofx.idea.configurations.TornadoFXConfiguration.RunType.App;
import static no.tornado.tornadofx.idea.configurations.TornadoFXConfiguration.RunType.View;

@SuppressWarnings("unchecked")
public class TornadoFXSettingsEditor extends SettingsEditor<TornadoFXConfiguration> implements PanelWithAnchor {
	private CommonJavaParametersPanel myCommonProgramParameters;
	private LabeledComponent<EditorTextFieldWithBrowseButton> myViewClass;
	private LabeledComponent<EditorTextFieldWithBrowseButton> myAppClass;
	private LabeledComponent<ModulesComboBox> myModule;
	private JPanel myWholePanel;

	private final ConfigurationModuleSelector myModuleSelector;
	private JrePathEditor myJrePathEditor;
	private JCheckBox myShowSwingInspectorCheckbox;
	private LabeledComponent typeWrapper;
	private LabeledComponent myDevOptions;
	private final JreVersionDetector myVersionDetector;
	private final Project myProject;
	private JComponent myAnchor;
	private JBRadioButton appButton;
	private JBRadioButton viewButton;
	private JBCheckBox liveViewsButton;
	private JBCheckBox liveStylesheetsButton;
	private JBCheckBox dumpStylesheetsButton;

	public TornadoFXSettingsEditor(final Project project) {
		myProject = project;
		myModuleSelector = new ConfigurationModuleSelector(project, myModule.getComponent());
		myJrePathEditor.setDefaultJreSelector(DefaultJreSelector.fromSourceRootsDependencies(myModule.getComponent(), getViewClassField()));
		myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
		myModule.getComponent().addActionListener(e -> myCommonProgramParameters.setModuleContext(myModuleSelector.getModule()));
		createClassBrowser(project, myModuleSelector, View).setField(getViewClassField());
		createClassBrowser(project, myModuleSelector, App).setField(getAppClassField());
		myVersionDetector = new JreVersionDetector();

		myAnchor = UIUtil.mergeComponentsWithAnchor(myViewClass, myAppClass, myDevOptions, typeWrapper, myCommonProgramParameters, myJrePathEditor, myModule);
	}

	public void applyEditorTo(final TornadoFXConfiguration configuration) throws ConfigurationException {
		myCommonProgramParameters.applyTo(configuration);
		myModuleSelector.applyTo(configuration);
		configuration.RUN_TYPE = appButton.isSelected() ? App : View;

		if (configuration.RUN_TYPE == App) {
			configuration.setViewClassName(null);
			configuration.setMainClassName(getAppClassField().getText());
		} else {
			configuration.setViewClassName(getViewClassField().getText());
			configuration.setMainClassName("tornadofx.App");
		}
		configuration.LIVE_STYLESHEETS = liveStylesheetsButton.isSelected();
		configuration.DUMP_STYLESHEETS = dumpStylesheetsButton.isSelected();
		configuration.LIVE_VIEWS = liveViewsButton.isSelected();
		configuration.ALTERNATIVE_JRE_PATH = myJrePathEditor.getJrePathOrName();
		configuration.ALTERNATIVE_JRE_PATH_ENABLED = myJrePathEditor.isAlternativeJreSelected();
		configuration.ENABLE_SWING_INSPECTOR = (myVersionDetector.isJre50Configured(configuration) || myVersionDetector.isModuleJre50Configured(configuration)) && myShowSwingInspectorCheckbox.isSelected();
		updateShowSwingInspector(configuration);
	}

	public void resetEditorFrom(final TornadoFXConfiguration configuration) {
		myCommonProgramParameters.reset(configuration);
		myModuleSelector.reset(configuration);
		if (configuration.RUN_TYPE == App) {
			appButton.setSelected(true);
			appButton.getActionListeners()[0].actionPerformed(null);
			getViewClassField().setText(null);
			getAppClassField().setText(configuration.getMainClassName() != null ? configuration.getMainClassName().replaceAll("\\$", "\\.") : "");
		} else {
			viewButton.setSelected(true);
			viewButton.getActionListeners()[0].actionPerformed(null);
			getAppClassField().setText(null);
			getViewClassField().setText(configuration.getViewClassName() != null ? configuration.getViewClassName().replaceAll("\\$", "\\.") : "");
		}
		liveStylesheetsButton.setSelected(configuration.LIVE_STYLESHEETS);
		dumpStylesheetsButton.setSelected(configuration.DUMP_STYLESHEETS);
		liveViewsButton.setSelected(configuration.LIVE_VIEWS);
		myJrePathEditor.setPathOrName(configuration.ALTERNATIVE_JRE_PATH, configuration.ALTERNATIVE_JRE_PATH_ENABLED);
		updateShowSwingInspector(configuration);
	}

	private void updateShowSwingInspector(final TornadoFXConfiguration configuration) {
		if (myVersionDetector.isJre50Configured(configuration) || myVersionDetector.isModuleJre50Configured(configuration)) {
			myShowSwingInspectorCheckbox.setEnabled(true);
			myShowSwingInspectorCheckbox.setSelected(configuration.ENABLE_SWING_INSPECTOR);
			myShowSwingInspectorCheckbox.setText(ExecutionBundle.message("show.swing.inspector"));
		} else {
			myShowSwingInspectorCheckbox.setEnabled(false);
			myShowSwingInspectorCheckbox.setSelected(false);
			myShowSwingInspectorCheckbox.setText(ExecutionBundle.message("show.swing.inspector.disabled"));
		}
	}

	public EditorTextFieldWithBrowseButton getViewClassField() {
		return myViewClass.getComponent();
	}

	public EditorTextFieldWithBrowseButton getAppClassField() {
		return myAppClass.getComponent();
	}

	public CommonJavaParametersPanel getCommonProgramParameters() {
		return myCommonProgramParameters;
	}

	@NotNull
	public JComponent createEditor() {
		return myWholePanel;
	}

	private void createUIComponents() {
		myViewClass = new LabeledComponent<>();
		myViewClass.setComponent(new EditorTextFieldWithBrowseButton(myProject, true, new JavaCodeFragment.VisibilityChecker() {
			public Visibility isDeclarationVisible(PsiElement declaration, PsiElement place) {
				return (declaration instanceof PsiClass && isViewClass((PsiClass) declaration))
					? Visibility.VISIBLE : Visibility.NOT_VISIBLE;
			}
		}));

		myAppClass = new LabeledComponent<>();
		myAppClass.setComponent(new EditorTextFieldWithBrowseButton(myProject, true, new JavaCodeFragment.VisibilityChecker() {
			public Visibility isDeclarationVisible(PsiElement declaration, PsiElement place) {
				return (declaration instanceof PsiClass && isAppClass((PsiClass) declaration))
					? Visibility.VISIBLE : Visibility.NOT_VISIBLE;
			}
		}));

		typeWrapper = new LabeledComponent<EditorTextFieldWithBrowseButton>();
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		ButtonGroup typeGroup = new ButtonGroup();
		appButton = new JBRadioButton("Application");
		appButton.addActionListener(e -> {
			myAppClass.setVisible(true);
			myViewClass.setVisible(false);
			fireEditorStateChanged();
		});
		viewButton = new JBRadioButton("View");
		viewButton.addActionListener(e -> {
			myAppClass.setVisible(false);
			myViewClass.setVisible(true);
			fireEditorStateChanged();
		});
		typeGroup.add(appButton);
		typeGroup.add(viewButton);
		panel.add(appButton);
		panel.add(viewButton);
		typeWrapper.setComponent(panel);

		liveViewsButton = new JBCheckBox("Live Views");
		liveStylesheetsButton = new JBCheckBox("Live Stylesheets");
		dumpStylesheetsButton = new JBCheckBox("Dump Stylesheets");
		JPanel devPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		devPanel.add(liveViewsButton);
		devPanel.add(liveStylesheetsButton);
		devPanel.add(dumpStylesheetsButton);
		myDevOptions = new LabeledComponent();
		myDevOptions.setComponent(devPanel);
	}

	public static boolean isViewClass(PsiClass psiClass) {
		for (PsiClass supa : psiClass.getSupers())
			if ("tornadofx.View".equals(supa.getQualifiedName())) {
				return true;
			} else {
				boolean superIs = isViewClass(supa);
				if (superIs) return true;
			}

		return false;
	}

	public static boolean isAppClass(PsiClass psiClass) {
		for (PsiClass supa : psiClass.getSupers())
			if ("tornadofx.App".equals(supa.getQualifiedName())) {
				return true;
			} else {
				boolean superIs = isAppClass(supa);
				if (superIs) return true;
			}

		return false;
	}

	@Override
	public JComponent getAnchor() {
		return myAnchor;
	}

	@Override
	public void setAnchor(@Nullable JComponent anchor) {
		this.myAnchor = anchor;
		myViewClass.setAnchor(anchor);
		myCommonProgramParameters.setAnchor(anchor);
		myJrePathEditor.setAnchor(anchor);
		myModule.setAnchor(anchor);
	}

	private ClassBrowser createClassBrowser(final Project project,
	                                        final ConfigurationModuleSelector moduleSelector,
	                                        final TornadoFXConfiguration.RunType runType) {
		final ClassFilter classFilter = aClass -> runType == TornadoFXConfiguration.RunType.View ? isViewClass(aClass) : isAppClass(aClass);
		return new MyClassBrowser(project, moduleSelector, "Choose " + runType + " Class") {
			protected ClassFilter createFilter(final Module module) {
				return classFilter;
			}
		};
	}

	private abstract static class MyClassBrowser extends ClassBrowser {
		final Project myProject;
		private final ConfigurationModuleSelector myModuleSelector;

		MyClassBrowser(final Project project,
		               final ConfigurationModuleSelector moduleSelector,
		               final String title) {
			super(project, title);
			myProject = project;
			myModuleSelector = moduleSelector;
		}

		protected PsiClass findClass(final String className) {
			return myModuleSelector.findClass(className);
		}

		protected ClassFilter.ClassFilterWithScope getFilter() throws NoFilterException {
			final Module module = myModuleSelector.getModule();
			final GlobalSearchScope scope;
			if (module == null) {
				scope = GlobalSearchScope.allScope(myProject);
			} else {
				scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
			}
			final ClassFilter filter = createFilter(module);
			return new ClassFilter.ClassFilterWithScope() {
				public GlobalSearchScope getScope() {
					return scope;
				}
				public boolean isAccepted(final PsiClass aClass) {
					return filter == null || filter.isAccepted(aClass);
				}
			};
		}

		protected ClassFilter createFilter(final Module module) {
			return null;
		}
	}

}
