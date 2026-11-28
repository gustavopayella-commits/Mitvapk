package com.tuusuario.iptv.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(entities = [ChannelEntity::class, AppSettingsEntity::class, ServerEntity::class], version = 2, exportSchema = false)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun iptvDao(): IptvDao
    abstract fun serverDao(): ServerDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS servers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, url TEXT NOT NULL, format TEXT NOT NULL DEFAULT 'AUTO', enabled INTEGER NOT NULL DEFAULT 1)")
                db.execSQL("ALTER TABLE channels ADD COLUMN serverId INTEGER")
            }
        }
        @Volatile private var INSTANCE: IptvDatabase? = null
        fun getInstance(context: Context): IptvDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, IptvDatabase::class.java, "nova_iptv.db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
