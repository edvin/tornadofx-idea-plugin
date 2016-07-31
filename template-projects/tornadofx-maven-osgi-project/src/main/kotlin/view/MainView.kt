package view

import app.Styles.Companion.heading
import javafx.scene.layout.HBox
import tornadofx.View
import tornadofx.addClass
import tornadofx.label

class MainView : View() {
    override val root = HBox()

    init {
        title = "Hello TornadoFX OSGi Application"

        with (root) {
            label(title) {
                addClass(heading)
            }
        }
    }
}