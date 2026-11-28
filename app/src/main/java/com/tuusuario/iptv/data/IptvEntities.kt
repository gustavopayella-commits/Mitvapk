package com.tuusuario.iptv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val groupName: String = "General",
    val tvgId: String? = null,
    val logo: String? = null,
    val isFavorite: Boolean = false
)

@Entity(tableName = "settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val lastPlaylistUrl: String = "",
    val lastEpgUrl: String = ""
)

data class EpgProgram(
    val channelId: String,
    val title: String,
    val startEpochMs: Long,
    val stopEpochMs: Long
)
