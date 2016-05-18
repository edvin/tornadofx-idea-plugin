package no.tornado.tornadofx.idea

import com.intellij.psi.PsiClass

class FXTools {
    companion object {
        fun isComponent(psiClass: PsiClass) = isType("tornadofx.Component", psiClass)
        fun isUIComponent(psiClass: PsiClass) = isType("tornadofx.UIComponent", psiClass)

        fun isType(type: String, psiClass: PsiClass): Boolean {
            for (supa in psiClass.supers)
                if (type == supa.qualifiedName) {
                    return true
                } else {
                    val superIs = isType(type, supa)
                    if (superIs) return true
                }

            return false
        }
    }
}