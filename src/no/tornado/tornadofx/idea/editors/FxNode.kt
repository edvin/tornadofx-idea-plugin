package no.tornado.tornadofx.idea.editors

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import tornadofx.*
import com.intellij.openapi.application.ApplicationManager
import javafx.application.Platform


class FxNode(nodeName: String, ktElement: KtElement, children: ObservableList<FxNode>? = null) {

    val nodeNameProperty = SimpleStringProperty(nodeName)
    var nodeName by nodeNameProperty

    val ktElementProperty = SimpleObjectProperty<KtElement>(ktElement)
    var ktElement by ktElementProperty

    val childrenProperty = SimpleObjectProperty<ObservableList<FxNode>>(children)
    var children: ObservableList<FxNode>
        get() {
            if (childrenProperty.value == null) {
                childrenProperty.value = FXCollections.observableArrayList()
            }
            return childrenProperty.value
        }
        set(children) {
            childrenProperty.value = children
        }
}

class HierarchyModel : ViewModel() {
    val nodeController: NodeController by inject()

    val fxNodeProperty = SimpleObjectProperty<FxNode>()
    var fxNode by fxNodeProperty

    fun computeNodeHirchay(rootProperty: KtProperty) {
        runIDEA({ nodeController.computeNodeHirachy(rootProperty) }, { fxNode = it })
//        runAsync {
//            val t = ApplicationManager.getApplication().runReadAction<FxNode?> {
//                nodeController.computeNodeHirachy(rootProperty)
//            }
//            t
//        } ui {fxNode = it}
    }
}

fun <T> runIDEA(func: () -> T, ui: (T) -> Unit) {
    ApplicationManager.getApplication()
            .invokeLater {
                val result = ApplicationManager.getApplication()
                        .runReadAction<T>(func)
                Platform.runLater { ui(result) }
            }
}

class NodeController : Controller() {

    fun computeNodeHirachy(rootProperty: KtProperty): FxNode? {
        val rootFxNode = FxNode("root", rootProperty)
        rootProperty.children
                .filterIsInstance<KtCallExpression>()
                .forEach { handelCallExpression(rootFxNode, it) }
        return rootFxNode
    }

    private fun handelCallExpression(parent: FxNode, expression: KtCallExpression) {
        // Check if we have a tornadofx builder function here.
        // And if the receiver of the extensions function is EvenTarget
        // since we only care about the top level builder functions
        // and not about addClass ...
        expression.reference?.element?.firstChild?.references?.forEach {
            if (it is KtSimpleNameReference) {
                val call = it.extractReturnTypeClassNameOrNull()
                if (call != null) {

                    val child = FxNode(call, expression)
                    parent.children.add(child)
//                    println("Class: $call")

                    // Visit all expressions in the lambda call of the builder method
                    // if there are any available

                    // We are only infested in the last lambda argument since all builders
                    // have a lambda expression as there last argument for creating the builder
                    val lambdaArgument = expression.lambdaArguments.lastOrNull()

//        val b = expression.referenceExpression()
//        println("Method-Name: ${b?.text}")

                    if (lambdaArgument != null) {
                        val lExpression = lambdaArgument.getLambdaExpression()
                        val blockExpr = lExpression.bodyExpression
                        blockExpr?.children?.forEach {
                            if (it is KtCallExpression) handelCallExpression(child, it)
                        }
                    }
                }
            }
        }
    }

    private fun KtSimpleNameReference.extractReturnTypeClassNameOrNull(): String? {
        val declration = this.resolve() as KtNamedDeclaration

        if (declration.containingFile !is KtFile) return null

        val descriptor = declration.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableDescriptor ?: return null
        val receiverFqName = descriptor.extensionReceiverParameter?.returnType?.getJetTypeFqName(false)
        val receiver = receiverFqName?.split(".")?.last()
        if (receiver == "EventTarget") {
            val type1 = descriptor.returnType;
            val i = type1?.getJetTypeFqName(false)
            return i?.split(".")?.last()
        }
        return null
    }
}
