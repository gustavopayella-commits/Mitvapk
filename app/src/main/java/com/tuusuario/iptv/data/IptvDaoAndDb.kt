package com.tuusuario.iptv.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {
    @Query("SELECT * FROM channels ORDER BY isFavorite DESC, name ASC")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE isFavorite = 0")
    suspend fun deleteNonFavorites()

    @Query("UPDATE channels SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}

@Database(
    entities = [ChannelEntity::class, AppSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun iptvDao(): IptvDao

    companion object {
        @Volatile
        private var INSTANCE: IptvDatabase? = null

        fun getInstance(context: Context): IptvDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IptvDatabase::class.java,
                    "nova_iptv.db"
                ).build().also { INSTANCE = it }
            }
    }
}
