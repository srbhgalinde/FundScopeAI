package com.fundscope.ai.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey val schemeCode: String,
    val name: String,
    val amc: String,
    val category: String,
    val isin: String? = null,
    val nav: Double = 0.0,
    val navDate: String = "",
    val score: Double = 7.5,
    val valuation: Double = 7.0,
    val quality: Double = 7.5,
    val risk: String = "Moderate",
    val riskLevel: Int = 4,
    val expense: Double = 0.50,
    val returns3y: Double = 0.0,
    val returns5y: Double = 0.0,
    val note: String = "",
    val sourceName: String = "AMFI / MFAPI",
    val lastUpdatedEpoch: Long = 0L
)

@Dao
interface FundDao {
    @Query("SELECT * FROM funds ORDER BY score DESC, name ASC")
    fun observeFunds(): Flow<List<FundEntity>>

    @Query("SELECT * FROM funds WHERE schemeCode = :code LIMIT 1")
    suspend fun getFundByCode(code: String): FundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(funds: List<FundEntity>)

    @Query("DELETE FROM funds")
    suspend fun clear()
}

@Database(entities = [FundEntity::class], version = 2, exportSchema = false)
abstract class FundDatabase : RoomDatabase() {
    abstract fun fundDao(): FundDao

    companion object {
        @Volatile
        private var INSTANCE: FundDatabase? = null

        fun getInstance(context: Context): FundDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FundDatabase::class.java,
                    "fundscope_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
