package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "v_phones")
data class VPhone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gamePackage: String,
    val afkDaysRequested: Int,
    val hoursElapsed: Double = 0.0,
    val status: String = "ONLINE", // ONLINE, PAUSED, COMPLETED
    val farmMode: String = "GRINDING", // GRINDING, QUESTS, RAIDS, LUTE
    val xpEarned: Int = 0,
    val goldEarned: Int = 0,
    val lootCount: Int = 0,
    val ipAddress: String,
    val region: String,
    val fpsPreset: Int = 60,
    val keepScreenOn: Boolean = true,
    val autoOptimize: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
