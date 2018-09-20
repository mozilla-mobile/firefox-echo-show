/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.focus.helpers.Assert.failUnexpectedEitherBranch

private const val EXPECTED_LEFT = "a"
private const val EXPECTED_RIGHT = 27

private val leftReturnedEither: Either<String, Int> = Either.Left(EXPECTED_LEFT)
private val rightReturnedEither: Either<String, Int> = Either.Right(EXPECTED_RIGHT)

class EitherTest {

    @Test
    fun `given left is returned then it provides the value it is given`() {
        when (leftReturnedEither) {
            is Either.Left -> assertEquals(EXPECTED_LEFT, leftReturnedEither.value)
            is Either.Right -> failUnexpectedEitherBranch(leftReturnedEither)
        }
    }

    @Test
    fun `given right is returned then it provides the value it is given`() {
        when (rightReturnedEither) {
            is Either.Left -> failUnexpectedEitherBranch(rightReturnedEither)
            is Either.Right -> assertEquals(EXPECTED_RIGHT, rightReturnedEither.value)
        }
    }

    @Test
    fun `given left is returned when leftOrThrow is called then it provides the value`() {
        assertEquals(EXPECTED_LEFT, leftReturnedEither.leftOrThrow)
    }

    @Test(expected = IllegalStateException::class)
    fun `given left is returned when rightOrThrow is called then it throws`() {
        leftReturnedEither.rightOrThrow
    }

    @Test
    fun `given right is returned when rightOrThrow is called then it provides the value`() {
        assertEquals(EXPECTED_RIGHT, rightReturnedEither.rightOrThrow)
    }

    @Test(expected = IllegalStateException::class)
    fun `given right is returned when leftOrThrow is called then it throws`() {
        rightReturnedEither.leftOrThrow
    }

    @Test
    fun `given left is returned when fold is called then the value from the left branch is returned`() {
        val expected = 4
        val actual: Int = leftReturnedEither.fold({ expected }, { failUnexpectedEitherBranch(it) })
        assertEquals(expected, actual)
    }

    @Test
    fun `given right is returned when fold is called then the value from the right branch is returned`() {
        val expected = "lol"
        val actual: String = rightReturnedEither.fold({ failUnexpectedEitherBranch(it) }, { expected })
        assertEquals(expected, actual)
    }

    @Test
    fun `given left is returned when fold is called then the value from Either Left is passed in`() {
        leftReturnedEither.fold({ assertEquals(EXPECTED_LEFT, it) }, { failUnexpectedEitherBranch(it) })
    }

    @Test
    fun `given right is returned when fold is called then the value from Either Right is passed in`() {
        rightReturnedEither.fold({ failUnexpectedEitherBranch(it) }, { assertEquals(EXPECTED_RIGHT, it) })
    }
}
