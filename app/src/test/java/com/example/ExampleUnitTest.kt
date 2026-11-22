package com.example

import com.example.audio.BookAudioPlayer
import com.example.data.model.BookItem
import com.example.data.model.BookshelfFilter
import com.example.data.model.BookshelfSort
import com.example.data.model.WorldwideLanguages
import com.example.data.pdf.PdfMetadataExtractor
import com.example.data.repository.BookshelfRepository
import com.example.ui.viewmodel.BookshelfViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun `pdf metadata extractor parses Info dictionary and XMP fallback`() {
        val rawPdf = """
            %PDF-1.4
            1 0 obj
            << /Title (The Pragmatic Programmer) /Author (David Thomas & Andrew Hunt) >>
            endobj
        """.trimIndent().toByteArray()

        val extracted = PdfMetadataExtractor.extractFromBytes(rawPdf, "pragmatic_book.pdf")
        assertEquals("The Pragmatic Programmer", extracted.title)
        assertEquals("David Thomas & Andrew Hunt", extracted.author)

        val xmpPdf = """
            %PDF-1.6
            <x:xmpmeta><rdf:RDF>
              <dc:title><rdf:Alt><rdf:li xml:lang="x-default">Designing Data-Intensive Applications</rdf:li></rdf:Alt></dc:title>
              <dc:creator><rdf:Seq><rdf:li>Martin Kleppmann</rdf:li></rdf:Seq></dc:creator>
            </rdf:RDF></x:xmpmeta>
        """.trimIndent().toByteArray()

        val xmpExtracted = PdfMetadataExtractor.extractFromBytes(xmpPdf, "ddia.pdf")
        assertEquals("Designing Data-Intensive Applications", xmpExtracted.title)
        assertEquals("Martin Kleppmann", xmpExtracted.author)

        val fallback = PdfMetadataExtractor.extractFromBytes("%PDF-1.4".toByteArray(), "clean_code_guide.pdf")
        assertEquals("clean code guide", fallback.title)
        assertEquals("Unknown Author", fallback.author)
    }

    @Test
    fun `bookshelf filter, search, and sort return expected books`() {
        val books = listOf(
            BookItem(id = "1", title = "Dune", author = "Frank Herbert", pageCount = 100, currentPage = 40, favorite = true),
            BookItem(id = "2", title = "Clean Code", author = "Robert Martin", pageCount = 50, currentPage = 49, completed = true),
            BookItem(id = "3", title = "Unread Essay", author = "Ada Lovelace", pageCount = 20, currentPage = 0),
            BookItem(id = "4", title = "Archived Classic", author = "Leo Tolstoy", pageCount = 300, currentPage = 299, completed = true, archived = true)
        )

        val favs = BookshelfViewModel.applyFilterAndSort(books, "", BookshelfFilter.FAVORITES, BookshelfSort.TITLE)
        assertEquals(listOf("1"), favs.map { it.id })

        val searchAuthor = BookshelfViewModel.applyFilterAndSort(books, "herbert", BookshelfFilter.ALL, BookshelfSort.RECENT)
        assertEquals(listOf("1"), searchAuthor.map { it.id })

        val completed = BookshelfViewModel.applyFilterAndSort(books, "", BookshelfFilter.COMPLETED, BookshelfSort.RECENT)
        assertEquals(listOf("2"), completed.map { it.id })

        val unread = BookshelfViewModel.applyFilterAndSort(books, "", BookshelfFilter.UNREAD, BookshelfSort.TITLE)
        assertEquals(listOf("3"), unread.map { it.id })

        val archived = BookshelfViewModel.applyFilterAndSort(books, "", BookshelfFilter.ARCHIVED, BookshelfSort.TITLE)
        assertEquals(listOf("4"), archived.map { it.id })

        val byTitle = BookshelfViewModel.applyFilterAndSort(books, "", BookshelfFilter.ALL, BookshelfSort.TITLE)
        assertEquals(listOf("2", "1", "3"), byTitle.map { it.id })
    }

    @Test
    fun `book progress fraction and percent clamp accurately`() {
        val reading = BookItem(pageCount = 100, currentPage = 49)
        assertEquals(50, reading.progressPercent)

        val finished = BookItem(pageCount = 100, currentPage = 99, completed = true)
        assertEquals(100, finished.progressPercent)

        val unread = BookItem(pageCount = 100, currentPage = 0, completed = false, lastReadAt = null)
        assertEquals(0, unread.progressPercent)
    }

    @Test
    fun `reading streak increments on consecutive day and resets after gap`() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 22) }
        val today = BookshelfRepository.todayDateKey(now)
        val yesterdayCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = BookshelfRepository.todayDateKey(yesterdayCal)

        assertEquals(4L, BookshelfRepository.computeUpdatedStreak(yesterday, 3L, today, now))
        assertEquals(3L, BookshelfRepository.computeUpdatedStreak(today, 3L, today, now))
        assertEquals(1L, BookshelfRepository.computeUpdatedStreak("2026-11-10", 5L, today, now))
    }

    @Test
    fun `worldwide languages search and lookup support global locales`() {
        assertTrue(WorldwideLanguages.allLanguages.size >= 36)
        val tamil = WorldwideLanguages.findByTag("ta")
        assertEquals("Tamil", tamil.displayName)
        assertEquals("தமிழ்", tamil.nativeName)

        val arabicMatches = WorldwideLanguages.search("العربية")
        assertTrue(arabicMatches.any { it.tag == "ar" })

        val japaneseMatches = WorldwideLanguages.search("japanese")
        assertTrue(japaneseMatches.any { it.tag == "ja" })
    }

    @Test
    fun `audio player splits prose into crisp sentences`() {
        val sentences = BookAudioPlayer.splitIntoSentences(
            "Welcome to Favbooks. Enjoy reading or listening! Ready to start?",
            Locale.ENGLISH
        )
        assertEquals(3, sentences.size)
        assertEquals("Welcome to Favbooks.", sentences[0])
        assertEquals("Enjoy reading or listening!", sentences[1])
        assertEquals("Ready to start?", sentences[2])
    }
}
