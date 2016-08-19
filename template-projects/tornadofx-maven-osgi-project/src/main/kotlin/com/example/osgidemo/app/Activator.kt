package com.example.osgidemo.app

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import tornadofx.osgi.registerApplication
import tornadofx.osgi.registerStylesheet
import tornadofx.osgi.registerView

class Activator : BundleActivator {
    override fun start(context: BundleContext) {
        // Comment out if this bundle shouldn't provide an Application to the TornadoFX OSGi Runtime
        context.registerApplication(DashboardApp::class)

        // Uncomment to provide this stylesheet to other bundles
        //context.registerStylesheet(Styles::class)

        // Uncomment to provide a View to other bundles
        //context.registerView(MusicPlayer::class, "dashboard")
    }

    override fun stop(context: BundleContext) {
    }
}