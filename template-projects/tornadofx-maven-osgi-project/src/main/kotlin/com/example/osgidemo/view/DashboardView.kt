package com.example.osgidemo.view

import com.example.osgidemo.app.Styles
import tornadofx.*
import tornadofx.osgi.addViewsWhen

class DashboardView : View("Hello TornadoFX OSGi Application") {
    override val root = vbox {
        label(title) {
            addClass(Styles.heading)
        }
        addViewsWhen { it.discriminator == "dashboard" }
    }
}