package no.tornado.tornadofx.idea

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "TornadoFXSettings", storages = arrayOf(Storage("other.xml")))
class TornadoFXSettings : PersistentStateComponent<TornadoFXSettings> {
    var alternativePropertySyntax = true

    override fun loadState(state: TornadoFXSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun getState() = this

    companion object {
        fun getInstance(): TornadoFXSettings = ServiceManager.getService(TornadoFXSettings::class.java)
    }
}