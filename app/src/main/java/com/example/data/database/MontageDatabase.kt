package com.example.data.database

import android.content.Context
import androidx.room.*
import com.example.data.model.ClipItem
import com.example.data.model.FilterStyle
import com.example.data.model.TransitionType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "montage_projects")
data class MontageProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val musicTrackId: String = "m1",
    val musicVolume: Float = 0.8f,
    val isExported: Boolean = false,
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1"
    val clips: List<ClipItem>
)

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listMyType = Types.newParameterizedType(List::class.java, ClipItem::class.java)
    private val adapter = moshi.adapter<List<ClipItem>>(listMyType)

    @TypeConverter
    fun fromString(value: String?): List<ClipItem> {
        if (value == null) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<ClipItem>?): String {
        return adapter.toJson(list ?: emptyList())
    }
}

@Dao
interface MontageProjectDao {
    @Query("SELECT * FROM montage_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<MontageProject>>

    @Query("SELECT * FROM montage_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): MontageProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: MontageProject): Long

    @Update
    suspend fun updateProject(project: MontageProject)

    @Query("DELETE FROM montage_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Database(entities = [MontageProject::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun montageDao(): MontageProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "montage_studio_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
