/*
 * Copyright (C) 2022-2023 NotEnoughUpdates contributors
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
package io.github.moulberry.notenoughupdates.miscfeatures

import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.autosubscribe.NEUAutoSubscribe
import net.minecraft.client.Minecraft
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/*
   CountdownCalculator.kt
   A Kotlin class by:
   — Erymanthus / RayDeeUx (code base, math, timestamp wrangling, adaptation into overall NEU codebase)
   — nea89 (enum, regex wrangling, ItemTooltipEvent integration, cleanup)
   
   Intended to detect countdowns within item tooltips and then adding that result
   to the user's current system time in Unix epoch form converted into a
   human-readable timestamp relative to the system timezone.
   
   Formerly a feature from SkyBlockCatia, then later attempted to be ported into Skytils.
   Now has a comfy home in the NotEnoughUpdates codebase.
 */

@NEUAutoSubscribe
class CountdownCalculator {

    private val regex =
        "(?:(?<years>\\d+)y )?(?:(?<days>\\d+)d)? ?(?:(?<hours>\\d+)h)? ?(?:(?<minutes>\\d+)m)? ?(?:(?<seconds>\\d+)s)?\\b".toRegex()

    @Suppress("unused")
    private enum class CountdownTypes(
        val match: String,
        val label: String,
        val isRelative: Boolean = false,
    ) {
        STARTING("Starting in:", "Starts at"),
        STARTS("Starts in:", "Starts at"),
        INTEREST("Interest in:", "Interest at"),
        UNTILINTEREST("Until interest:", "Interest at"),
        ENDS("Ends in:", "Ends at"),
        REMAINING("Remaining:", "Ends at"),
        DURATION("Duration:", "Finishes at"),
        TIMELEFT("Time left:", "Ends at"),
        EVENTTIMELEFT("Event lasts for", "Ends at", isRelative = true),
        SHENSUCKS("Auction ends in:", "Auction ends at"),
        TAMINGSIXTYWASAMISTAKE("Ends:", "Finishes at"), // There would be a more specific message here seeing as this is for pet XP, but knowing Hypixel it's probably safer to leave it like this in case they use the "Ends:" prefix elsewhere besides the pet training menus.
        CALENDARDETAILS(" (§e", "Starts at"), // calendar details
        COMMUNITYPROJECTSSUCK("Contribute again", "Come back at"); // nopo i just woke up i really hope this works
    }

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        var formatterAsString = when (NotEnoughUpdates.INSTANCE.config.misc.showWhenCountdownEnds) {
            1 -> "EEEE, MMM d h:mm a"
            2 -> "EEEE, MMM d HH:mm"
            else -> return
        }
        if (event.itemStack != null && Minecraft.getMinecraft().thePlayer?.openContainer != null) {
            var i = -1
            var lastTimer: ZonedDateTime? = null
            while (++i < event.toolTip.size) {
                val tooltipLine = event.toolTip[i]
                val countdownKind = CountdownTypes.values().find { it.match in tooltipLine } ?: continue
                val match = regex.findAll(tooltipLine).maxByOrNull { it.value.length } ?: continue
                if (countdownKind == CountdownTypes.CALENDARDETAILS && !event.itemStack.displayName.startsWith("§aDay ")) return
                val years = match.groups["years"]?.value?.toLong() ?: 0L
                val days = match.groups["days"]?.value?.toLong() ?: 0L
                val hours = match.groups["hours"]?.value?.toLong() ?: 0L
                val minutes = match.groups["minutes"]?.value?.toLong() ?: 0L
                val seconds = match.groups["seconds"]?.value?.toLong() ?: 0L
                val totalSeconds = (years * 31_536_000L) + (days * 86_400L) + (hours * 3_600L) + (minutes * 60L) + seconds
                if (totalSeconds == 0L) continue
                if (years != 0L) formatterAsString = "${formatterAsString} yyyy"
                val useFormatter = DateTimeFormatter.ofPattern(formatterAsString)!!
                val countdownTarget = if (countdownKind.isRelative) {
                    if (lastTimer == null) {
                        event.toolTip.add(
                            ++i,
                            "§r§cThe above countdown is relative, but I can't find another countdown. [NEU]"
                        )
                        continue
                    } else lastTimer.addTime(years, days, hours, minutes, seconds)
                } else ZonedDateTime.now().addTime(years, days, hours, minutes, seconds)
                val countdownTargetFormatted = useFormatter.format(countdownTarget)
                event.toolTip.add(
                    ++i,
                    "§r§b${countdownKind.label}: $countdownTargetFormatted"
                )
                lastTimer = countdownTarget
            }
        }
    }

    private fun ZonedDateTime.addTime(years: Long, days: Long, hours: Long, minutes: Long, seconds: Long): ZonedDateTime {
        return this.plusYears(years).plusDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds)
    }
}

