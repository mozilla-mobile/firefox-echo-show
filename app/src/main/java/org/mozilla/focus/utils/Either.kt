/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

/**
 * A container type that represents a value of either one type or another. We could pull
 * in a library to provide this functionality but it seems like overkill.
 *
 * impl via https://discuss.kotlinlang.org/t/implementing-kotlin-either/8730
 */
sealed class Either<out A, out B> {
    class Left<A>(val value: A) : Either<A, Nothing>()
    class Right<B>(val value: B) : Either<Nothing, B>()

    /**
     * Calls the [leftBranch] when the Either is of type Left, otherwise calls the [rightBranch],
     * and returns a value of a type that both functions share.
     *
     * For example:
     *   val eitherTextOrException: Either<String, InvalidParameterException> = getDisplayText(someString)
     *   val displayText = result.fold(
     *           { displayText -> displayText },
     *           { exception -> "Error: exception occurred!" })
     */
    fun <C> fold(leftBranch: (A) -> C, rightBranch: (B) -> C): C = when (this) {
        is Either.Left -> leftBranch(value)
        is Either.Right -> rightBranch(value)
    }

    val leftOrThrow: A
        get() = if (this is Either.Left) {
            value
        } else {
            throw IllegalStateException("Either: attempted to get left but value was right")
        }

    val rightOrThrow: B
        get() = if (this is Either.Right) {
            value
        } else {
            throw java.lang.IllegalStateException("Either: attempted to get right but value was left")
        }
}
