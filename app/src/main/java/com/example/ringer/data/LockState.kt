package com.example.ringer.data

data class LockState(
    val packageName: String,
    val unlockedUntil: Long = 0L // timestamp in millis when lock expires
) {
    fun isUnlocked(): Boolean = System.currentTimeMillis() < unlockedUntil
}