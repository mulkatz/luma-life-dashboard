package com.mulkatz.lumacare.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.seed.SeedParser

/** In-Memory-Datenbank für Robolectric-Tests. */
fun inMemoryDatabase(): LumaDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LumaDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** Der echte Seed, gelesen aus dem generierten Asset (Quelle: docs/case/luma-seed.json). */
fun realSeedJson(): String =
    ApplicationProvider.getApplicationContext<Context>().assets.open(SeedParser.ASSET_NAME)
        .bufferedReader().use { it.readText() }
