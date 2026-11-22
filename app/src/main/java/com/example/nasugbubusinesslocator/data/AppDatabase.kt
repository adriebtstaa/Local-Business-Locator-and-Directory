package com.example.nasugbubusinesslocator.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val ownerName: String,
    val address: String,
    val barangay: String,
    val category: String,
    val phone: String,
    val imageName: String,
    val latitude: Double,
    val longitude: Double,
    val photosString: String = "",
    val hasBeachAccess: Boolean = false,
    val entranceFee: Double = 0.0,
    val cuisineType: String = "",
    val openingHours: String = "",
    val specialty: String = "",
    val hasWifi: Boolean = false,
    val is24Hours: Boolean = false
)

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId")]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Int,
    val guestName: String,
    val commentText: String,
    val rating: Int,
    val date: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val password: String,
    val role: String,
    val businessNameOwned: String = ""
)

@Entity(tableName = "owner_logs")
data class OwnerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerEmail: String,
    val action: String,
    val timestamp: String
)

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses")
    suspend fun getAllBusinesses(): List<BusinessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinesses(businesses: List<BusinessEntity>)

    @Query("SELECT * FROM businesses WHERE category = :category")
    suspend fun getBusinessesByCategory(category: String): List<BusinessEntity>

    @Query("SELECT * FROM businesses WHERE name LIKE '%' || :query || '%' OR barangay LIKE '%' || :query || '%'")
    suspend fun searchBusinesses(query: String): List<BusinessEntity>
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE businessId = :businessId")
    suspend fun getReviewsForBusiness(businessId: Int): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUser(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Database(entities = [BusinessEntity::class, ReviewEntity::class, UserEntity::class, OwnerLogEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun reviewDao(): ReviewDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nasugbu_business_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
