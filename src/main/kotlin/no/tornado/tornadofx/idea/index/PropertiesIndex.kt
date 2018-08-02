package no.tornado.tornadofx.idea.index

import com.intellij.lang.properties.PropertiesFileType
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import no.tornado.tornadofx.idea.facet.TornadoFXFacet

/**
 * Indexes .properties files.
 * Used for fetching i18n information.
 * The key is based on the Component's FQN.
 *
 * e.g. a Component 'MainView' in the package 'com.example' has a translation for key 'hello'
 * The resulting key is: 'com.example.MainView.hello'
 */
class PropertiesIndex : FileBasedIndexExtension<String, String>() {
    companion object {
        val NAME = ID.create<String, String>("tornadofx.PropertiesIndex")

        val TRACKER = Tracker()
    }

    override fun getValueExternalizer(): DataExternalizer<String> = EnumeratorStringDescriptor()

    override fun getName(): ID<String, String> = NAME

    override fun getVersion(): Int = 0

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, String, FileContent> = PropertiesDataIndexer()

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(PropertiesFileType.INSTANCE)
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    private class PropertiesDataIndexer : DataIndexer<String, String, FileContent> {
        override fun map(content: FileContent): MutableMap<String, String> {
            if (TornadoFXFacet.get(content.project) == null) {
                // Avoid properties files that are not part of the project
                return mutableMapOf()
            }

            val filepath = content.file.path
            if (!isResourcePath(filepath)) {
                return mutableMapOf()
            }
            val path = filepathToKey(filepath)
            val properties = (content.psiFile as PropertiesFile).properties
            val ret = mutableMapOf<String, String>()
            
            for (property in properties) {
                val key = property.key ?: continue
                val value = property.value ?: continue
                ret["$path.$key"] = value
            }

            // Notify all dependencies of changes.
            TRACKER.change()

            return ret
        }

        private fun isResourcePath(filepath: String): Boolean {
            return filepath.contains("res")
        }

        /**
         * Transforms an absolute file path into a string used for setting the
         * key of the index.
         */
        private fun filepathToKey(fullpath: String): String {
            // Removes the absolute part of the full path
            // This is opinionated and assumes that resources are found in a folder called
            // "res" or "resource" or similar.
            var path = fullpath.substring(fullpath.indexOf("res"))
            // Then, remove the front of the string up to the slash.
            // This is essentially a poor mans version of /res.*\//
            // Also remove the properties file extension
            path = path.substring(path.indexOf('/') + 1, path.length - ".properties".length)

            // Lastly, turn directory separators into dots, matching the FQN of the Component
            return path.replace('/', '.')
        }
    }


    /**
     * Used as a dependency, so that dependents will be informed on changes
     * to properties files.
     */
    class Tracker : ModificationTracker {
        private var count = 0L
        override fun getModificationCount(): Long = count
        fun change() = ++count
    }
}
