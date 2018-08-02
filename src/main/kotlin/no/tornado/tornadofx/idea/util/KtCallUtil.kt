package no.tornado.tornadofx.idea.util

import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression

fun KtCallExpression.getCalleeFQN() = calleeExpression?.mainReference?.resolve()?.getKotlinFqName()
