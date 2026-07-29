package com.example.readproplus.data

import android.content.Context
import android.content.SharedPreferences
import com.example.readproplus.model.ReadingStats
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReadingStatsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getStats(): ReadingStats = ReadingStats(
        totalReadingTimeSeconds = prefs.getLong(KEY_TIME_SEC, 0L),
        totalPagesRead = prefs.getInt(KEY_PAGES_READ, 0),
        booksFinished = prefs.getInt(KEY_BOOKS_FINISHED, 0),
        currentStreakDays = prefs.getInt(KEY_STREAK, 0),
        lastReadTimestamp = prefs.getLong(KEY_LAST_READ, 0L),
    )

    @Synchronized
    fun startSession(bookId: String, now: Long = System.currentTimeMillis()) {
        if (prefs.getLong(KEY_SESSION_START, 0L) == 0L) {
            prefs.edit()
                .putString(KEY_SESSION_BOOK, bookId)
                .putLong(KEY_SESSION_START, now)
                .apply()
        }
    }

    @Synchronized
    fun finishSession(now: Long = System.currentTimeMillis()) {
        val startedAt = prefs.getLong(KEY_SESSION_START, 0L)
        if (startedAt > 0L) {
            val seconds = TimeUnit.MILLISECONDS.toSeconds((now - startedAt).coerceAtLeast(0L))
            if (seconds > 0L) {
                prefs.edit().putLong(KEY_TIME_SEC, getStats().totalReadingTimeSeconds + seconds).apply()
            }
            prefs.edit().remove(KEY_SESSION_START).remove(KEY_SESSION_BOOK).apply()
        }
    }

    /** Records a page once per book/page pair and marks completion once. */
    @Synchronized
    fun recordPageRead(
        bookId: String,
        pageNumber: Int,
        totalPages: Int,
        now: Long = System.currentTimeMillis(),
    ) {
        if (pageNumber < 1) return
        val seenPages = prefs.getStringSet(KEY_SEEN_PAGES, emptySet()).orEmpty().toMutableSet()
        val pageKey = "$bookId:$pageNumber"
        val isNewPage = seenPages.add(pageKey)
        val editor = prefs.edit()
            .putLong(KEY_LAST_READ, now)
            .putInt(KEY_STREAK, calculateStreakDays(getStats().lastReadTimestamp, getStats().currentStreakDays, now))
            .putStringSet(KEY_SEEN_PAGES, seenPages)
        if (isNewPage) editor.putInt(KEY_PAGES_READ, getStats().totalPagesRead + 1)

        if (totalPages > 0 && pageNumber >= totalPages) {
            val finishedBooks = prefs.getStringSet(KEY_FINISHED_BOOKS, emptySet()).orEmpty().toMutableSet()
            if (finishedBooks.add(bookId)) {
                editor.putStringSet(KEY_FINISHED_BOOKS, finishedBooks)
                    .putInt(KEY_BOOKS_FINISHED, getStats().booksFinished + 1)
            }
        }
        editor.apply()
    }

    @Synchronized
    fun addReadingTime(seconds: Long) {
        if (seconds <= 0L) return
        prefs.edit()
            .putLong(KEY_TIME_SEC, getStats().totalReadingTimeSeconds + seconds)
            .putLong(KEY_LAST_READ, System.currentTimeMillis())
            .apply()
    }

    @Synchronized
    fun incrementPagesRead(pages: Int = 1) {
        if (pages > 0) prefs.edit().putInt(KEY_PAGES_READ, getStats().totalPagesRead + pages).apply()
    }

    @Synchronized
    fun markBookFinished() {
        prefs.edit().putInt(KEY_BOOKS_FINISHED, getStats().booksFinished + 1).apply()
    }

    companion object {
        private const val PREFS_NAME = "readproplus_reading_stats"
        private const val KEY_TIME_SEC = "total_time_sec"
        private const val KEY_PAGES_READ = "total_pages_read"
        private const val KEY_BOOKS_FINISHED = "books_finished"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_READ = "last_read"
        private const val KEY_SEEN_PAGES = "seen_pages"
        private const val KEY_FINISHED_BOOKS = "finished_books"
        private const val KEY_SESSION_START = "session_start"
        private const val KEY_SESSION_BOOK = "session_book"

        internal fun calculateStreakDays(lastReadTimestamp: Long, currentStreak: Int, now: Long): Int {
            if (lastReadTimestamp == 0L) return 1
            val lastDay = dayNumber(lastReadTimestamp)
            val currentDay = dayNumber(now)
            return when (currentDay - lastDay) {
                0L -> currentStreak.coerceAtLeast(1)
                1L -> currentStreak.coerceAtLeast(1) + 1
                else -> 1
            }
        }

        private fun dayNumber(timestamp: Long): Long {
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            return calendar.get(Calendar.YEAR) * 366L + calendar.get(Calendar.DAY_OF_YEAR)
        }
    }
}
