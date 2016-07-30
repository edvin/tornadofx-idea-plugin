package app

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import tornadofx.osgi.registerApplication
import tornadofx.osgi.registerStylesheet
import tornadofx.osgi.registerView

class Activator : BundleActivator {
    override fun start(context: BundleContext) {
        // Comment out if this bundle shouldn't provide an Application to the TornadoFX Runtime Bundle
        context.registerApplication(MyApp::class)

        // Uncomment to provide this stylesheet to other bundles
        //context.registerStylesheet(Styles::class)

        // Uncomment to provide this View to other bundles
        //context.registerView(MusicPlayer::class, "dashboard")
    }

    override fun stop(context: BundleContext) {
    }
}