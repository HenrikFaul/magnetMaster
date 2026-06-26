package com.magnetmaster.game.game

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistence (SharedPreferences + JSON). Mirrors the cloud
 * user_progress schema from AI_PROMPT.md §4.1 but stored on-device so the game
 * is fully playable offline. A cloud sync layer would post the same fields.
 */
class Store(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("magnet_master", Context.MODE_PRIVATE)

    var gems = 0
    var coins = 0
    var skillPoints = 0
    var highestUnlocked = 1
    val skillLevels = IntArray(4)
    var activeSkin = "default"
    val unlockedSkins = linkedSetOf("default")
    private val stars = HashMap<Int, Int>()          // level -> stars (0..3)

    // settings
    var soundOn = true
    var hapticsOn = true
    var reduceMotion = false

    // daily challenge
    var dailyDate = ""
    var dailyBestKg = 0f

    // first-time tutorial seen
    var tutorialSeen = false

    init { load() }

    private fun load() {
        gems = sp.getInt("gems", 50)
        coins = sp.getInt("coins", 0)
        skillPoints = sp.getInt("sp", 1)
        highestUnlocked = sp.getInt("hi", 1)
        activeSkin = sp.getString("skin", "default") ?: "default"
        soundOn = sp.getBoolean("sound", true)
        hapticsOn = sp.getBoolean("haptics", true)
        reduceMotion = sp.getBoolean("reduce", false)
        dailyDate = sp.getString("dailyDate", "") ?: ""
        dailyBestKg = sp.getFloat("dailyBest", 0f)
        tutorialSeen = sp.getBoolean("tut", false)

        val sl = sp.getString("skill", null)
        if (sl != null) {
            val a = JSONArray(sl)
            for (i in 0 until minOf(a.length(), 4)) skillLevels[i] = a.getInt(i)
        }
        val sk = sp.getString("skins", null)
        if (sk != null) {
            val a = JSONArray(sk)
            for (i in 0 until a.length()) unlockedSkins.add(a.getString(i))
        }
        val st = sp.getString("stars", null)
        if (st != null) {
            val o = JSONObject(st)
            for (k in o.keys()) stars[k.toInt()] = o.getInt(k)
        }
    }

    fun save() {
        val e = sp.edit()
        e.putInt("gems", gems)
        e.putInt("coins", coins)
        e.putInt("sp", skillPoints)
        e.putInt("hi", highestUnlocked)
        e.putString("skin", activeSkin)
        e.putBoolean("sound", soundOn)
        e.putBoolean("haptics", hapticsOn)
        e.putBoolean("reduce", reduceMotion)
        e.putString("dailyDate", dailyDate)
        e.putFloat("dailyBest", dailyBestKg)
        e.putBoolean("tut", tutorialSeen)
        e.putString("skill", JSONArray().apply { skillLevels.forEach { put(it) } }.toString())
        e.putString("skins", JSONArray().apply { unlockedSkins.forEach { put(it) } }.toString())
        e.putString("stars", JSONObject().apply { stars.forEach { (k, v) -> put(k.toString(), v) } }.toString())
        e.apply()
    }

    fun starsFor(level: Int): Int = stars[level] ?: 0
    fun totalStars(): Int = stars.values.sum()
    fun clearStars() { stars.clear() }

    /** Record a level result; returns skill points newly awarded. */
    fun recordResult(level: Int, starsEarned: Int, gemsEarned: Int, coinsEarned: Int): Int {
        val prev = stars[level] ?: 0
        var spAwarded = 0
        if (starsEarned > prev) {
            stars[level] = starsEarned
            // award 1 SP per new star gained
            spAwarded = starsEarned - prev
            skillPoints += spAwarded
        }
        gems += gemsEarned
        coins += coinsEarned
        if (level + 1 > highestUnlocked && level < Levels.MAX_LEVEL) highestUnlocked = level + 1
        save()
        return spAwarded
    }

    fun canUpgrade(branch: Branch): Boolean {
        val lvl = skillLevels[branch.ordinal]
        return lvl < Skills.MAX_NODE && skillPoints >= Skills.costFor(branch, lvl)
    }

    fun upgrade(branch: Branch): Boolean {
        if (!canUpgrade(branch)) return false
        val lvl = skillLevels[branch.ordinal]
        skillPoints -= Skills.costFor(branch, lvl)
        skillLevels[branch.ordinal] = lvl + 1
        save()
        return true
    }

    fun resetSkills() {
        var refund = 0
        for (i in skillLevels.indices) { refund += skillLevels[i]; skillLevels[i] = 0 }
        skillPoints += refund
        save()
    }

    fun stats(): SkillStats = SkillStats(skillLevels)

    fun buySkin(skin: Skin): Boolean {
        if (unlockedSkins.contains(skin.id)) { activeSkin = skin.id; save(); return true }
        if (gems < skin.costGems) return false
        gems -= skin.costGems
        unlockedSkins.add(skin.id)
        activeSkin = skin.id
        save()
        return true
    }
}
