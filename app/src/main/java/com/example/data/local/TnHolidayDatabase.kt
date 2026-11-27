package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.InitialLeaveSeedData
import com.example.data.TnHolidayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class TnHolidayEntity(
  val id: String,
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

interface TnHolidayDao {
  fun observeAllHolidays(): Flow<List<TnHolidayEntity>>
  suspend fun getAllHolidaysOnce(): List<TnHolidayEntity>
  suspend fun getHolidayCount(): Int
  suspend fun upsertHolidays(items: List<TnHolidayEntity>)
  suspend fun upsertSingleHoliday(item: TnHolidayEntity)
  suspend fun deleteHolidayById(id: String)
}

/**
 * Lightweight, zero-KSP native Android SQLite cache preserving the exact `tn_medleave_holidays.db`
 * file and `tn_holidays` table schema for seamless local caching of synced TN Govt holidays.
 */
class TnHolidayDatabase private constructor(context: Context) {

  private val openHelper = HolidaySqliteOpenHelper(context.applicationContext)
  private val daoInstance: TnHolidayDao = SqliteTnHolidayDao(openHelper)

  fun holidayDao(): TnHolidayDao = daoInstance

  private class HolidaySqliteOpenHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
      db.execSQL(CREATE_TABLE_SQL)
    }

    override fun onOpen(db: SQLiteDatabase) {
      super.onOpen(db)
      if (!db.isReadOnly) {
        db.execSQL(CREATE_TABLE_SQL)
      }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
      onCreate(db)
    }
  }

  private class SqliteTnHolidayDao(
    private val helper: HolidaySqliteOpenHelper
  ) : TnHolidayDao {

    private val lock = Any()
    private val holidaysState = MutableStateFlow<List<TnHolidayEntity>>(emptyList())
    @Volatile
    private var initializedFromDisk = false

    private fun readAllSortedSync(): List<TnHolidayEntity> {
      synchronized(lock) {
        val db = helper.readableDatabase
        val result = mutableListOf<TnHolidayEntity>()
        db.query(
          TABLE_NAME,
          arrayOf(
            "id",
            "dateIso",
            "dayOfWeek",
            "nameEnglish",
            "nameTamil",
            "isRestrictedHoliday",
            "monthNameTamil",
            "sourceOrigin",
            "syncedAtEpochMillis"
          ),
          null,
          null,
          null,
          null,
          "dateIso ASC"
        ).use { cursor ->
          while (cursor.moveToNext()) {
            result.add(
              TnHolidayEntity(
                id = cursor.getString(0),
                dateIso = cursor.getString(1),
                dayOfWeek = cursor.getString(2),
                nameEnglish = cursor.getString(3),
                nameTamil = cursor.getString(4),
                isRestrictedHoliday = cursor.getInt(5) != 0,
                monthNameTamil = cursor.getString(6),
                sourceOrigin = cursor.getString(7),
                syncedAtEpochMillis = cursor.getLong(8)
              )
            )
          }
        }
        initializedFromDisk = true
        return result
      }
    }

    private fun refreshStateSync(): List<TnHolidayEntity> {
      val rows = readAllSortedSync()
      holidaysState.value = rows
      return rows
    }

    override fun observeAllHolidays(): Flow<List<TnHolidayEntity>> {
      if (!initializedFromDisk) {
        runCatching { refreshStateSync() }
      }
      return holidaysState.asStateFlow()
    }

    override suspend fun getAllHolidaysOnce(): List<TnHolidayEntity> = withContext(Dispatchers.IO) {
      refreshStateSync()
    }

    override suspend fun getHolidayCount(): Int = withContext(Dispatchers.IO) {
      synchronized(lock) {
        val db = helper.readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null).use { cursor ->
          if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
      }
    }

    override suspend fun upsertHolidays(items: List<TnHolidayEntity>) = withContext(Dispatchers.IO) {
      if (items.isEmpty()) return@withContext
      synchronized(lock) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
          items.forEach { item ->
            db.insertWithOnConflict(
              TABLE_NAME,
              null,
              item.toContentValues(),
              SQLiteDatabase.CONFLICT_REPLACE
            )
          }
          db.setTransactionSuccessful()
        } finally {
          db.endTransaction()
        }
      }
      refreshStateSync()
      Unit
    }

    override suspend fun upsertSingleHoliday(item: TnHolidayEntity) = withContext(Dispatchers.IO) {
      synchronized(lock) {
        val db = helper.writableDatabase
        db.insertWithOnConflict(
          TABLE_NAME,
          null,
          item.toContentValues(),
          SQLiteDatabase.CONFLICT_REPLACE
        )
      }
      refreshStateSync()
      Unit
    }

    override suspend fun deleteHolidayById(id: String) = withContext(Dispatchers.IO) {
      synchronized(lock) {
        val db = helper.writableDatabase
        db.delete(TABLE_NAME, "id = ?", arrayOf(id))
      }
      refreshStateSync()
      Unit
    }

    private fun TnHolidayEntity.toContentValues(): ContentValues = ContentValues().apply {
      put("id", id)
      put("dateIso", dateIso)
      put("dayOfWeek", dayOfWeek)
      put("nameEnglish", nameEnglish)
      put("nameTamil", nameTamil)
      put("isRestrictedHoliday", if (isRestrictedHoliday) 1 else 0)
      put("monthNameTamil", monthNameTamil)
      put("sourceOrigin", sourceOrigin)
      put("syncedAtEpochMillis", syncedAtEpochMillis)
    }
  }

  companion object {
    private const val DB_NAME = "tn_medleave_holidays.db"
    private const val DB_VERSION = 1
    private const val TABLE_NAME = "tn_holidays"

    private const val CREATE_TABLE_SQL = """
      CREATE TABLE IF NOT EXISTS tn_holidays (
        id TEXT NOT NULL PRIMARY KEY,
        dateIso TEXT NOT NULL,
        dayOfWeek TEXT NOT NULL,
        nameEnglish TEXT NOT NULL,
        nameTamil TEXT NOT NULL,
        isRestrictedHoliday INTEGER NOT NULL,
        monthNameTamil TEXT NOT NULL,
        sourceOrigin TEXT NOT NULL,
        syncedAtEpochMillis INTEGER NOT NULL
      )
    """

    @Volatile
    private var INSTANCE: TnHolidayDatabase? = null

    fun getInstance(context: Context): TnHolidayDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: TnHolidayDatabase(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}

class LocalHolidayRepository(private val dao: TnHolidayDao) {

  val allHolidaysFlow: Flow<List<TnHolidayEntity>> = dao.observeAllHolidays()

  val domainHolidaysFlow: Flow<Pair<List<TnHolidayItem>, List<TnHolidayItem>>> =
    dao.observeAllHolidays().map { entities ->
      syncEntitiesIntoInMemoryKnowledgeBase(entities)
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
    syncEntitiesIntoInMemoryKnowledgeBase(all)
  }

  private fun syncEntitiesIntoInMemoryKnowledgeBase(all: List<TnHolidayEntity>) {
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
