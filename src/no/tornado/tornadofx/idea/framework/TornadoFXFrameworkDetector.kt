package no.tornado.tornadofx.idea.framework

import com.intellij.framework.detection.DetectedFrameworkDescription
import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.intellij.framework.detection.FrameworkDetectionContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileContent
import no.tornado.tornadofx.idea.FXTools
import no.tornado.tornadofx.idea.facet.TornadoFXFacet
import no.tornado.tornadofx.idea.facet.TornadoFXFacetConfiguration
import no.tornado.tornadofx.idea.facet.TornadoFXFacetType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

class TornadoFXFrameworkDetector : FacetBasedFrameworkDetector<TornadoFXFacet, TornadoFXFacetConfiguration>("TornadoFX", 1) {
    override fun getFacetType() = TornadoFXFacetType.INSTANCE

    override fun createSuitableFilePattern(): ElementPattern<FileContent> {
        return FileContentPattern.fileContent()
    }

    override fun getFileType(): KotlinFileType = KotlinFileType.INSTANCE

    override fun detect(newFiles: MutableCollection<VirtualFile>, context: FrameworkDetectionContext): MutableList<out DetectedFrameworkDescription> {
        val project = context.project ?: return arrayListOf()
        val psiManager = PsiManager.getInstance(project)

        for (file in newFiles) {
            val psiFile: PsiFile? = psiManager.findFile(file)
            if (psiFile is KtFile) {
                for (clazz in psiFile.classes) {
                    if (clazz != null && FXTools.isTornadoFXType(clazz))
                        return super.detect(mutableListOf(file), context)
                }
            }
        }
        return arrayListOf()
    }
}