package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RoomDataRepository
import com.example.data.local.LifeManagerDatabase
import com.example.model.Checklist
import com.example.model.ChecklistItem
import com.example.model.Folder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChecklistRepositoryTest {

    private lateinit var db: LifeManagerDatabase
    private lateinit var repo: RoomDataRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = LifeManagerDatabase.getInMemoryDatabase(context)
        repo = RoomDataRepository(db.folderDao(), db.textCardDao(), db.syncTombstoneDao(), db.checklistDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `items keep done state, note and rating after a save and reload`() {
        runBlocking {
            val list = Checklist(
                id = "l-1",
                title = "Movies",
                items = listOf(
                    ChecklistItem(id = "i-1", text = "Inception", isDone = true, note = "Great ending\nsecond line", rating = 5, completedAt = 123L),
                    ChecklistItem(id = "i-2", text = "Dune")
                )
            )
            repo.addChecklist(list)

            val loaded = repo.getChecklists().first().single()
            assertEquals("Movies", loaded.title)
            assertEquals(2, loaded.items.size)
            val first = loaded.items[0]
            assertTrue(first.isDone)
            assertEquals("Great ending\nsecond line", first.note)
            assertEquals(5, first.rating)
            assertEquals(123L, first.completedAt)
            assertFalse(loaded.items[1].isDone)
            assertEquals(null, loaded.items[1].completedAt)
            assertEquals(1, loaded.doneCount)
        }
    }

    @Test
    fun `update favorite and move work`() {
        runBlocking {
            repo.addFolder(Folder(id = "f-1", name = "Watch list"))
            repo.addChecklist(Checklist(id = "l-1", title = "Shopping"))

            repo.updateChecklist(Checklist(id = "l-1", title = "Groceries", items = listOf(ChecklistItem(text = "Milk"))))
            repo.toggleChecklistFavorite("l-1")
            repo.moveChecklistToFolder("l-1", "f-1")

            val loaded = repo.getChecklists().first().single()
            assertEquals("Groceries", loaded.title)
            assertEquals(1, loaded.items.size)
            assertTrue(loaded.isFavorite)
            assertEquals("f-1", loaded.folderId)
        }
    }

    @Test
    fun `deleting a checklist leaves a sync tombstone`() {
        runBlocking {
            repo.addChecklist(Checklist(id = "l-1", title = "Books"))
            repo.deleteChecklist("l-1")

            assertTrue(repo.getChecklists().first().isEmpty())
            val tombstones = db.syncTombstoneDao().getAllTombstones()
            assertTrue(tombstones.any { it.entityType == "CHECKLIST" && it.entityId == "l-1" })
        }
    }

    @Test
    fun `deleting a folder removes its checklists and tombstones each one`() {
        runBlocking {
            repo.addFolder(Folder(id = "f-1", name = "Media"))
            repo.addChecklist(Checklist(id = "l-1", title = "Series", folderId = "f-1"))
            repo.addChecklist(Checklist(id = "l-2", title = "Unfiled"))

            repo.deleteFolder("f-1")

            val remaining = repo.getChecklists().first()
            assertEquals(listOf("l-2"), remaining.map { it.id })
            val tombstones = db.syncTombstoneDao().getAllTombstones()
            assertTrue(tombstones.any { it.entityType == "CHECKLIST" && it.entityId == "l-1" })
        }
    }
}
