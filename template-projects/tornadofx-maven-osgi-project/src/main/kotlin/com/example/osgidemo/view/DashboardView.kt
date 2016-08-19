package com.example.osgidemo.view

import com.example.osgidemo.app.Styles.Companion.heading
import tornadofx.View
import tornadofx.addClass
import tornadofx.label
import tornadofx.osgi.addViewsWhen
import tornadofx.vbox

class DashboardView : View("Hello TornadoFX OSGi Application") {
    override val root = vbox {
        label(title) {
            addClass(heading)
        }
        addViewsWhen { it.discriminator == "dashboard" }
    }
}