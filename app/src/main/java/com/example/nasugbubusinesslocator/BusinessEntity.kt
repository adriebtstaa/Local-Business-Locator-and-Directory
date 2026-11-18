package com.example.nasugbubusinesslocator

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// Database TABLE!
@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val ownerName: String,
    val type: String,
    val imageName: String,
    val latitude: Double,
    val longitude: Double,
    val photos: String,
    val extraInfo: String = "",
    val address: String = "",
    val phone: String = ""
)

//
@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY id ASC")
    suspend fun getAllBusinesses(): List<BusinessEntity>

    @Query("SELECT * FROM businesses WHERE type = :type ORDER BY id ASC")
    suspend fun getBusinessesByType(type: String): List<BusinessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(businesses: List<BusinessEntity>)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: Int)

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun getCount(): Int
}

// DATABASE------------------------------
@Database(entities = [BusinessEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nasugbu_business_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}