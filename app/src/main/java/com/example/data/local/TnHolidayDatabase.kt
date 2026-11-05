package com.example.data.local

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
import com.example.data.InitialLeaveSeedData
import com.example.data.TnHolidayItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "tn_holidays")
data class TnHolidayEntity(
  @PrimaryKey val id: String,
  val dateIso: String,
  val dayOfWeek: String,
  val nameEnglish: String,
  val nameTamil: String,
  val isRestrictedHoliday: Boolean,
  val monthNameTamil: String,
  val sourceOrigin: String = "TN Govt G.O. (Public Misc. Dept)",
  val syncedAtEpochMillis: Long = System.currentTimeMillis()
) {
  fun toDomainModel(): TnHolidayItem = TnHolidayItem(
    dateIso = dateIso,
    dayOfWeek = dayOfWeek,
    nameEnglish = nameEnglish,
    nameTamil = nameTamil,
    isRestrictedHoliday = isRestrictedHoliday,
    monthNameTamil = monthNameTamil
  )

  companion object {
    fun fromDomainModel(
      item: TnHolidayItem,
      sourceOrigin: String = "TN Govt G.O. (Public Misc. Dept)",
      syncedAt: Long = System.currentTimeMillis()
    ): TnHolidayEntity {
      val typePrefix = if (item.isRestrictedHoliday) "RH" else "GH"
      return TnHolidayEntity(
        id = "${typePrefix}_${item.dateIso}_${item.nameEnglish.take(16).replace(" ", "_")}",
        dateIso = item.dateIso,
        dayOfWeek = item.dayOfWeek,
        nameEnglish = item.nameEnglish,
        nameTamil = item.nameTamil,
        isRestrictedHoliday = item.isRestrictedHoliday,
        monthNameTamil = item.monthNameTamil,
        sourceOrigin = sourceOrigin,
        syncedAtEpochMillis = syncedAt
      )
    }
  }
}

@Dao
interface TnHolidayDao {
  @Query("SELECT * FROM tn_holidays ORDER BY dateIso ASC")
  fun observeAllHolidays(): Flow<List<TnHolidayEntity>>

  @Query("SELECT * FROM tn_holidays ORDER BY dateIso ASC")
  suspend fun getAllHolidaysOnce(): List<TnHolidayEntity>

  @Query("SELECT COUNT(*) FROM tn_holidays")
  suspend fun getHolidayCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHolidays(items: List<TnHolidayEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSingleHoliday(item: TnHolidayEntity)

  @Query("DELETE FROM tn_holidays WHERE id = :id")
  suspend fun deleteHolidayById(id: String)
}

@Database(
  entities = [TnHolidayEntity::class],
  version = 1,
  exportSchema = false
)
abstract class TnHolidayDatabase : RoomDatabase() {
  abstract fun holidayDao(): TnHolidayDao

  companion object {
    @Volatile
    private var INSTANCE: TnHolidayDatabase? = null

    fun getInstance(context: Context): TnHolidayDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          TnHolidayDatabase::class.java,
          "tn_medleave_holidays.db"
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}

class LocalHolidayRepository(private val dao: TnHolidayDao) {

  val allHolidaysFlow: Flow<List<TnHolidayEntity>> = dao.observeAllHolidays()

  val domainHolidaysFlow: Flow<Pair<List<TnHolidayItem>, List<TnHolidayItem>>> =
    dao.observeAllHolidays().map { entities ->
      val gh = entities.filter { !it.isRestrictedHoliday }.map { it.toDomainModel() }
      val rh = entities.filter { it.isRestrictedHoliday }.map { it.toDomainModel() }
      Pair(gh, rh)
    }

  suspend fun ensureSeededWithDefaultsIfEmpty(): Int {
    val allExisting = dao.getAllHolidaysOnce()
    val has2026 = allExisting.any { it.dateIso.startsWith("2026-") }
    val has2027 = allExisting.any { it.dateIso.startsWith("2027-") }
    if (has2026 && has2027) {
      refreshInMemoryCacheFromRoom()
      return 0
    }
    val now = System.currentTimeMillis()
    val seedEntities = buildList {
      if (!has2026) {
        InitialLeaveSeedData.defaultGovtPublicHolidays2026.forEach { gh ->
          add(
            TnHolidayEntity.fromDomainModel(
              item = gh,
              sourceOrigin = "TN Public (Misc) Dept G.O. 2026",
              syncedAt = now
            )
          )
        }
        InitialLeaveSeedData.defaultRestrictedHolidays2026.forEach { rh ->
          add(
            TnHolidayEntity.fromDomainModel(
              item = rh,
              sourceOrigin = "TN RL List 2026 Notification",
              syncedAt = now
            )
          )
        }
      }
      if (!has2027) {
        com.example.domain.TnHolidays2027Catalog.defaultGovtPublicHolidays2027.forEach { gh27 ->
          add(
            TnHolidayEntity.fromDomainModel(
              item = gh27,
              sourceOrigin = "TN Public (Misc) Dept G.O. 2027",
              syncedAt = now
            )
          )
        }
        com.example.domain.TnHolidays2027Catalog.defaultRestrictedHolidays2027.forEach { rh27 ->
          add(
            TnHolidayEntity.fromDomainModel(
              item = rh27,
              sourceOrigin = "TN RL List 2027 Notification",
              syncedAt = now
            )
          )
        }
      }
    }
    if (seedEntities.isNotEmpty()) {
      dao.upsertHolidays(seedEntities)
    }
    refreshInMemoryCacheFromRoom()
    return seedEntities.size
  }

  suspend fun upsertSyncedHolidays(items: List<TnHolidayEntity>) {
    if (items.isNotEmpty()) {
      dao.upsertHolidays(items)
      refreshInMemoryCacheFromRoom()
    }
  }

  suspend fun addCustomOrLocalHoliday(item: TnHolidayItem, sourceOrigin: String) {
    val entity = TnHolidayEntity.fromDomainModel(
      item = item,
      sourceOrigin = sourceOrigin,
      syncedAt = System.currentTimeMillis()
    )
    dao.upsertSingleHoliday(entity)
    refreshInMemoryCacheFromRoom()
  }

  suspend fun refreshInMemoryCacheFromRoom() {
    val all = dao.getAllHolidaysOnce()
    if (all.isNotEmpty()) {
      val gh2026 = all.filter { !it.isRestrictedHoliday && it.dateIso.startsWith("2026-") }.map { it.toDomainModel() }
      val rh2026 = all.filter { it.isRestrictedHoliday && it.dateIso.startsWith("2026-") }.map { it.toDomainModel() }
      if (gh2026.isNotEmpty() || rh2026.isNotEmpty()) {
        InitialLeaveSeedData.updateLiveHolidayCache(ghList = gh2026, rhList = rh2026)
      }

      val gh2027Plus = all.filter { !it.isRestrictedHoliday && !it.dateIso.startsWith("2026-") }.map { it.toDomainModel() }
      val rh2027Plus = all.filter { it.isRestrictedHoliday && !it.dateIso.startsWith("2026-") }.map { it.toDomainModel() }
      if (gh2027Plus.isNotEmpty() || rh2027Plus.isNotEmpty()) {
        com.example.domain.TnHolidays2027Catalog.updateLiveHolidayCache2027(
          ghList2027Plus = gh2027Plus,
          rhList2027Plus = rh2027Plus
        )
      }
    }
  }
}
