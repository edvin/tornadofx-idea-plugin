package no.tornado.tornadofx.idea.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import no.tornado.tornadofx.idea.index.PropertiesIndex
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtClass

class I18nFoldingBuilder : FoldingBuilderEx() {

    private val index = FileBasedIndex.getInstance()

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("translations")
        val ret = mutableListOf<FoldingDescriptor>()

        val expressions = PsiTreeUtil.findChildrenOfType(root, KtArrayAccessExpression::class.java)

        // Get informed on changes to the index
        val dependencies = setOf(PropertiesIndex.TRACKER)
        for (expression in expressions) {
            if (!isMessageExpression(expression)) {
                continue
            }

            ret += object : FoldingDescriptor(expression.node,expression.textRange, group, dependencies) {
                override fun getPlaceholderText(): String? {
                    val key = expression.indexExpressions.first().text
                    val translation = findTranslation(getKey(expression, key.trim('"')), root.project)

                    return if (translation != null) {
                        "\"%s\"".format(translation)
                    } else {
                        "\"[$key]\""
                    }
                }
            }
        }

        return ret.toTypedArray()
    }


    override fun isCollapsedByDefault(node: ASTNode): Boolean = true


    override fun getPlaceholderText(node: ASTNode): String? = null

    /**
     * Checks if left hand side of array access is a reference to Component's messages
     */
    private fun isMessageExpression(expression: KtArrayAccessExpression): Boolean {
        val array = expression.arrayExpression ?: return false
        val fqName = array.mainReference?.resolve()?.getKotlinFqName() ?: return false

        return fqName.toString() == "tornadofx.Component.messages"
    }

    /**
     * Fetches the Components name and prepends it to the key
     */
    private fun getKey(element: PsiElement, key: String): String {
        val clazz = PsiTreeUtil.getParentOfType(element, KtClass::class.java)
        val fqn = clazz?.fqName?.toString()

        return if (fqn == null) {
            key
        } else {
            "$fqn.$key"
        }
    }

    private fun findTranslation(key: String, project: Project): String? {
        val values = index.getValues(PropertiesIndex.NAME, key, GlobalSearchScope.allScope(project))

        return values.firstOrNull()
    }
}
