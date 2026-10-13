package com.mulkatz.lumacare

import android.content.Context
import androidx.room.Room
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.local.SharedPreferencesAppStateStore
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.data.seed.SeedMeta
import com.mulkatz.lumacare.data.seed.SeedParser
import com.mulkatz.lumacare.domain.AppStateStore
import com.mulkatz.lumacare.domain.ShiftClock

/** Manuelle DI: Datenbank, Zustandsspeicher, Seed-Import und ShiftClock entstehen bei Bedarf. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: LumaDatabase by lazy {
        Room.databaseBuilder(appContext, LumaDatabase::class.java, LumaDatabase.NAME).build()
    }

    val stateStore: AppStateStore by lazy {
        SharedPreferencesAppStateStore(
            appContext.getSharedPreferences(SharedPreferencesAppStateStore.PREFS_NAME, Context.MODE_PRIVATE),
        )
    }

    /** Meta des Seeds (Einrichtung, Schicht, Seed-Zeit), einmal aus dem Asset gelesen. */
    val seedMeta: SeedMeta by lazy { SeedParser.parse(readSeedAsset()).meta }

    val seedImporter: SeedImporter by lazy { SeedImporter(database, stateStore, ::readSeedAsset) }

    val shiftClock: ShiftClock by lazy {
        ShiftClock(
            seedNow = SeedImporter.parseDateTime(seedMeta.now, "meta.now"),
            shiftStart = SeedImporter.parseDateTime(seedMeta.shift.start, "meta.shift.start"),
            shiftEnd = SeedImporter.parseDateTime(seedMeta.shift.end, "meta.shift.end"),
            anchorStore = stateStore,
            currentTimeMillis = System::currentTimeMillis,
        )
    }

    private fun readSeedAsset(): String =
        appContext.assets.open(SeedParser.ASSET_NAME).bufferedReader().use { it.readText() }
}
