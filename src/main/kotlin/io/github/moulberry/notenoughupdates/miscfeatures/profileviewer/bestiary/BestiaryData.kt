/*
 * Copyright (C) 2023-2024 NotEnoughUpdates contributors
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

package io.github.moulberry.notenoughupdates.miscfeatures.profileviewer.bestiary

import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.ItemUtils
import io.github.moulberry.notenoughupdates.util.Utils
import io.github.moulberry.notenoughupdates.util.roundToDecimals
import kotlin.math.min

object BestiaryData {
    private val categoriesToParse = listOf(
        "dynamic",
        "hub",
        "farming_1",
        "garden",
        "combat_1",
        "combat_3",
        "crimson_isle",
        "mining_2",
        "mining_3",
        "crystal_hollows",
        "foraging_1",
        "spooky_festival",
        "mythological_creatures",
        "jerry",
        "kuudra",
        "catacombs",
        "fishing"
    )

    /**
     * Calculates the sum of all individual tiers for this profile
     *
     * @param computedCategories List of parsed categories
     * @see BestiaryPage.parseBestiaryData
     */
    @JvmStatic
    fun calculateTotalBestiaryTiers(computedCategories: List<Category>): Int {
        var tiers = 0.0
        computedCategories.forEach {
            tiers += countTotalLevels(it)
        }
        return tiers.toInt()
    }

    /**
     * Calculate the skyblock xp awarded for the given bestiary progress
     */
    @JvmStatic
    fun calculateBestiarySkyblockXp(profileInfo: JsonObject): Int {
        val totalTiers = calculateTotalBestiaryTiers(parseBestiaryData(profileInfo))
        var skyblockXp = 0

        val slayingTask = Constants.SBLEVELS.getAsJsonObject("slaying_task") ?: return 0
        val xpPerTier = (slayingTask.get("bestiary_family_xp") ?: return 0).asInt
        val xpPerMilestone = slayingTask.get("bestiary_milestone_xp").asInt
        val maxXp = slayingTask.get("bestiary_progress").asInt

        skyblockXp += totalTiers * xpPerTier

        val milestones = (totalTiers / 100)
        skyblockXp += milestones * xpPerMilestone

        return min(skyblockXp, maxXp)
    }

    private fun countTotalLevels(category: Category): Int {
        var levels = 0
        for (mob in category.mobs) {
            levels += mob.mobLevelData.level
        }
        category.subCategories.forEach { levels += countTotalLevels(it) }
        return levels
    }

    /**
     * Checks if a user profile has migrated.
     *
     * @param profileInfo skyblock profile information
     */
    fun hasMigrated(profileInfo: JsonObject): Boolean {
        val bestiaryObject = profileInfo.getAsJsonObject("bestiary") ?: return false
        return if (bestiaryObject.has("migration")) {
            bestiaryObject.get("migration").asBoolean
        } else {
            // The account is new and therefore already uses the new format
            true
        }
    }

    /**
     * Parse the bestiary data for the profile. Categories are taken from the `constants/bestiary.json` repo file
     *
     * @param profileInfo the JsonObject containing the bestiary data
     */
    @JvmStatic
    fun parseBestiaryData(profileInfo: JsonObject): MutableList<Category> {
        if (!hasMigrated(profileInfo) || Constants.BESTIARY == null) {
            return mutableListOf()
        }

        val parsedCategories = mutableListOf<Category>()

        val apiKills = profileInfo.getAsJsonObject("bestiary")!!.getAsJsonObject("kills") ?: return mutableListOf()
        val apiDeaths = profileInfo.getAsJsonObject("bestiary").getAsJsonObject("deaths") ?: return mutableListOf()
        val killsMap: HashMap<String, Int> = HashMap()
        for (entry in apiKills.entrySet()) {
            if (entry.key == "last_killed_mob") {
                continue
            }
            killsMap[entry.key] = entry.value.asString.toIntOrNull() ?: -1
        }
        val deathsMap: HashMap<String, Int> = HashMap()
        for (entry in apiDeaths.entrySet()) {
            deathsMap[entry.key] = entry.value.asString.toIntOrNull() ?: -1
        }

        for (categoryId in categoriesToParse) {
            val categoryData = Constants.BESTIARY.getAsJsonObject(categoryId)
            if (categoryData != null) {
                parsedCategories.add(parseCategory(categoryData, categoryId, killsMap, deathsMap))
            } else {
                Utils.showOutdatedRepoNotification("bestiary.json missing or outdated")
            }
        }

        return parsedCategories
    }

    /**
     * Parse one individual category, including potential subcategories
     */
    private fun parseCategory(
        categoryData: JsonObject,
        categoryId: String,
        killsMap: HashMap<String, Int>,
        deathsMap: HashMap<String, Int>
    ): Category {
        val categoryName = categoryData["name"].asString
        val computedMobs: MutableList<Mob> = mutableListOf()
        val categoryIconData = categoryData["icon"].asJsonObject

        val categoryIcon = if (categoryIconData.has("skullOwner")) {
            Utils.createSkull(
                categoryName, categoryIconData["skullOwner"].asString, categoryIconData["texture"].asString
            )
        } else {
            ItemUtils.createItemStackFromId(categoryIconData["item"].asString, categoryName)
        }
        if (categoryData.has("hasSubcategories")) { // It must have some subcategories
            val subCategories: MutableList<Category> = mutableListOf()

            val reserved = listOf("name", "icon", "hasSubcategories")
            for (entry in categoryData.entrySet()) {
                if (!reserved.contains(entry.key)) {
                    subCategories.add(
                        parseCategory(
                            entry.value.asJsonObject,
                            "${categoryId}_${entry.key}",
                            killsMap,
                            deathsMap
                        )
                    )
                }
            }
            return Category(categoryId, categoryName, categoryIcon, emptyList(), subCategories, calculateFamilyDataOfSubcategories(subCategories))
        } else {
            val categoryMobs = categoryData["mobs"].asJsonArray.map { it.asJsonObject }

            for (mobData in categoryMobs) {
                val mobName = mobData["name"].asString
                val mobIcon = if (mobData.has("skullOwner")) {
                    Utils.createSkull(
                        mobName, mobData["skullOwner"].asString, mobData["texture"].asString
                    )
                } else {
                    ItemUtils.createItemStackFromId(mobData["item"].asString, mobName)
                }

                val cap = mobData["cap"].asDouble
                val bracket = mobData["bracket"].asInt

                var kills = 0.0
                var deaths = 0.0

                // The mobs array contains the individual names returned by the API
                val mobsArray = mobData["mobs"].asJsonArray.map { it.asString }
                for (s in mobsArray) {
                    kills += killsMap.getOrDefault(s, 0)
                    deaths += deathsMap.getOrDefault(s, 0)
                }

                val levelData = calculateLevel(bracket, kills, cap)
                computedMobs.add(Mob(mobName, mobIcon, kills, deaths, levelData))
            }
            return Category(categoryId, categoryName, categoryIcon, computedMobs, emptyList(), calculateFamilyData(computedMobs))
        }
    }

    /**
     * Calculates the level for a given mob
     *
     * @param bracket applicable bracket number
     * @param kills number of kills the player has on that mob type
     * @param cap maximum kill limit for the mob
     */
    private fun calculateLevel(bracket: Int, kills: Double, cap: Double): MobLevelData {
        val bracketData =
            Constants.BESTIARY["brackets"].asJsonObject[bracket.toString()].asJsonArray.map { it.asDouble }
        var maxLevel = false
        var progress = 0.0
        var effKills = 0.0
        var effReq = 0.0

        val effectiveKills = if (kills >= cap) {
            maxLevel = true
            cap
        } else {
            kills
        }

        val totalProgress = (effectiveKills / cap * 100).roundToDecimals(1)

        var level = 0
        for (requiredKills in bracketData) {
            if (effectiveKills >= requiredKills) {
                level++
            } else {
                val prevTierKills = if (level != 0) bracketData[(level - 1).coerceAtLeast(0)].toInt() else 0
                effKills = kills - prevTierKills
                effReq = requiredKills - prevTierKills
                progress = (effKills / effReq * 100).roundToDecimals(1)
                break
            }
        }
        return MobLevelData(level, maxLevel, progress, totalProgress, MobKillData(effKills, effReq, effectiveKills, cap))
    }

    private fun calculateFamilyData(mobs: List<Mob>): FamilyData {
        var found = 0
        var completed = 0

        for (mob in mobs) {
            if (mob.kills > 0) found++
            if (mob.mobLevelData.maxLevel) completed++
        }

        return FamilyData(found, completed, mobs.size)
    }

    private fun calculateFamilyDataOfSubcategories(subCategories: List<Category>): FamilyData {
        var found = 0
        var completed = 0
        var total = 0

        for (category in subCategories) {
            val data = category.familyData
            found += data.found
            completed += data.completed
            total += data.total
        }

        return FamilyData(found, completed, total)
    }
}
