/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.util

import net.minecraft.util.StringUtils
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.stripControlCodes(): String = StringUtils.stripControlCodes(this)

fun String.copyToClipboard() = Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(this), null)

inline fun <T> Pattern.matchMatcher(text: String, consumer: Matcher.() -> T) =
    matcher(text).let { if (it.matches()) consumer(it) else null }
