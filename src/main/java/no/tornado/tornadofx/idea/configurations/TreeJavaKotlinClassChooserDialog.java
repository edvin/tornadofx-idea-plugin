package no.tornado.tornadofx.idea.configurations;

import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode;
import org.jetbrains.kotlin.psi.KtClassOrObject;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

/**
 * The [com.intellij.ide.util.TreeJavaClassChooserDialog] can only select java classes
 * We override the behaviour to fetch the [org.jetbrains.kotlin.asJava.classes.KtLightClass]
 * from the KtClass.
 */
public class TreeJavaKotlinClassChooserDialog extends TreeJavaClassChooserDialog {

    public TreeJavaKotlinClassChooserDialog(String title, Project project) {
        super(title, project);
    }

    public TreeJavaKotlinClassChooserDialog(String title, Project project, @Nullable PsiClass initialClass) {
        super(title, project, initialClass);
    }

    public TreeJavaKotlinClassChooserDialog(String title, @NotNull Project project, GlobalSearchScope scope, ClassFilter classFilter, @Nullable PsiClass initialClass) {
        super(title, project, scope, classFilter, initialClass);
    }

    public TreeJavaKotlinClassChooserDialog(String title, @NotNull Project project, GlobalSearchScope scope, @Nullable ClassFilter classFilter, PsiClass baseClass, @Nullable PsiClass initialClass, boolean isShowMembers) {
        super(title, project, scope, classFilter, baseClass, initialClass, isShowMembers);
    }

    public static TreeJavaKotlinClassChooserDialog withInnerClasses(String title,
                                                                    @NotNull Project project,
                                                                    GlobalSearchScope scope,
                                                                    final ClassFilter classFilter) {
        return new TreeJavaKotlinClassChooserDialog(title, project, scope, classFilter, null, null, true);
    }

    @Nullable
    @Override
    protected PsiClass getSelectedFromTreeUserObject(DefaultMutableTreeNode node) {
        PsiClass javaClass = super.getSelectedFromTreeUserObject(node);
        if (javaClass != null) {
            return javaClass;
        }

        Object userObject = node.getUserObject();
        if (!(userObject instanceof KtClassOrObjectTreeNode)) return null;
        KtClassOrObjectTreeNode descriptor = (KtClassOrObjectTreeNode)userObject;
        KtClassOrObject value = descriptor.getValue();

        return toLightClass(value);
    }
}
