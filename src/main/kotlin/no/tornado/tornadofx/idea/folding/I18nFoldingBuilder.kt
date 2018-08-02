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
import no.tornado.tornadofx.idea.translation.TranslationManager
import org.jetbrains.kotlin.psi.KtArrayAccessExpression

class I18nFoldingBuilder : FoldingBuilderEx() {

    private val translationManager = TranslationManager()

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("translations")
        val ret = mutableListOf<FoldingDescriptor>()

        val expressions = PsiTreeUtil.findChildrenOfType(root, KtArrayAccessExpression::class.java)

        // Get informed on changes to the index
        val dependencies = setOf(PropertiesIndex.TRACKER)
        for (expression in expressions) {
            if (!translationManager.isMessageExpression(expression)) {
                continue
            }

            ret += object : FoldingDescriptor(expression.node,expression.textRange, group, dependencies) {
                override fun getPlaceholderText(): String? {
                    val key = expression.indexExpressions.first().text
                    val translation = translationManager.findTranslation(expression)

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
}
