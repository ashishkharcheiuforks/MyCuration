package com.phicdy.mycuration.data.repository

import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.phicdy.mycuration.data.filter.Filter
import com.phicdy.mycuration.data.filter.FilterFeedRegistration
import com.phicdy.mycuration.data.rss.Feed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

class FilterRepository(private val db: SQLiteDatabase) {

    /**
     * Helper method to retrieve all of the filters.
     *
     * @return all of the filters in the database
     */
    suspend fun getAllFilters(): ArrayList<Filter> = coroutineScope {
        return@coroutineScope withContext(Dispatchers.IO) {
            val filters = ArrayList<Filter>()
            val columns = arrayOf(
                    Filter.TABLE_NAME + "." + Filter.ID,
                    Filter.TABLE_NAME + "." + Filter.TITLE,
                    Filter.TABLE_NAME + "." + Filter.KEYWORD,
                    Filter.TABLE_NAME + "." + Filter.URL,
                    Filter.TABLE_NAME + "." + Filter.ENABLED,
                    Feed.TABLE_NAME + "." + Feed.ID,
                    Feed.TABLE_NAME + "." + Feed.TITLE
            )
            val selection = Filter.TABLE_NAME + "." + Filter.ID + "=" +
                    FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FILTER_ID + " and " +
                    FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FEED_ID + "=" +
                    Feed.TABLE_NAME + "." + Feed.ID
            val table = Filter.TABLE_NAME + " inner join " +
                    FilterFeedRegistration.TABLE_NAME + " inner join " + Feed.TABLE_NAME
            var cursor: Cursor? = null
            try {
                db.beginTransaction()
                cursor = db.query(table, columns, selection, null, null, null, null)
                if (cursor != null && cursor.count > 0) {
                    cursor.moveToFirst()
                    var filter: Filter
                    var rssList = ArrayList<Feed>()
                    var filterId = cursor.getInt(0)
                    var title = cursor.getString(1)
                    var keyword = cursor.getString(2)
                    var url = cursor.getString(3)
                    var enabled = cursor.getInt(4)
                    var rssId = cursor.getInt(5)
                    var rssTitle = cursor.getString(6)
                    rssList.add(Feed(rssId, rssTitle, "", Feed.DEDAULT_ICON_PATH, "", 0, ""))
                    while (cursor.moveToNext()) {
                        val cursorFilterId = cursor.getInt(0)
                        if (filterId != cursorFilterId) {
                            // Next filter starts, add to filter list and init RSS list for next filter
                            filter = Filter(filterId, title, keyword, url, rssList, -1, enabled)
                            filters.add(filter)
                            filterId = cursorFilterId
                            rssList = ArrayList()
                        }
                        title = cursor.getString(1)
                        keyword = cursor.getString(2)
                        url = cursor.getString(3)
                        enabled = cursor.getInt(4)
                        rssId = cursor.getInt(5)
                        rssTitle = cursor.getString(6)
                        rssList.add(Feed(rssId, rssTitle, "", Feed.DEDAULT_ICON_PATH, "", 0, ""))
                    }
                    filter = Filter(filterId, title, keyword, url, rssList, -1, enabled)
                    filters.add(filter)
                    cursor.close()
                }
                db.setTransactionSuccessful()
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
                cursor?.close()
            }
            return@withContext filters
        }
    }

    suspend fun getEnabledFiltersOfFeed(feedId: Int): ArrayList<Filter> = withContext(Dispatchers.IO) {
        val filterList = arrayListOf<Filter>()
        var cur: Cursor? = null
        try {
            // Get all filters which feed ID is "feedId"
            val columns = arrayOf(
                    Filter.TABLE_NAME + "." + Filter.ID,
                    Filter.TABLE_NAME + "." + Filter.TITLE,
                    Filter.TABLE_NAME + "." + Filter.KEYWORD,
                    Filter.TABLE_NAME + "." + Filter.URL,
                    Filter.TABLE_NAME + "." + Filter.ENABLED
            )
            val condition = FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FEED_ID + " = " + feedId + " and " +
                    FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FILTER_ID + " = " + Filter.TABLE_NAME + "." + Filter.ID + " and " +
                    Filter.TABLE_NAME + "." + Filter.ENABLED + " = " + Filter.TRUE
            db.beginTransaction()
            cur = db.query(Filter.TABLE_NAME + " inner join " + FilterFeedRegistration.TABLE_NAME, columns, condition, null, null, null, null)
            // Change to ArrayList
            while (cur.moveToNext()) {
                val id = cur.getInt(0)
                val title = cur.getString(1)
                val keyword = cur.getString(2)
                val url = cur.getString(3)
                val enabled = cur.getInt(4)
                filterList.add(Filter(id, title, keyword, url, ArrayList(), -1, enabled))
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            cur?.close()
            db.endTransaction()
        }

        return@withContext filterList
    }

    suspend fun getFilterById(filterId: Int): Filter? = withContext(Dispatchers.IO) {
        var filter: Filter? = null
        val columns = arrayOf(
                Filter.TABLE_NAME + "." + Filter.ID,
                Filter.TABLE_NAME + "." + Filter.KEYWORD,
                Filter.TABLE_NAME + "." + Filter.URL,
                Filter.TABLE_NAME + "." + Filter.TITLE,
                Filter.TABLE_NAME + "." + Filter.ENABLED,
                Feed.TABLE_NAME + "." + Feed.ID,
                Feed.TABLE_NAME + "." + Feed.TITLE
        )
        val condition = Filter.TABLE_NAME + "." + Filter.ID + " = " + filterId + " and " +
                FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FILTER_ID + " = " + filterId + " and " +
                FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FEED_ID + " = " + Feed.TABLE_NAME + "." + Feed.ID
        val table = Filter.TABLE_NAME + " inner join " +
                FilterFeedRegistration.TABLE_NAME + " inner join " + Feed.TABLE_NAME
        var cursor: Cursor? = null
        try {
            db.beginTransaction()
            cursor = db.query(table, columns, condition, null, null, null, null)
            if (cursor == null || cursor.count < 1) return@withContext null

            val feeds = arrayListOf<Feed>()
            var id = 0
            var keyword = ""
            var url = ""
            var title = ""
            var enabled = 0
            while (cursor.moveToNext()) {
                id = cursor.getInt(0)
                keyword = cursor.getString(1)
                url = cursor.getString(2)
                title = cursor.getString(3)
                enabled = cursor.getInt(4)
                val feedId = cursor.getInt(5)
                val feedTitle = cursor.getString(6)
                val feed = Feed(feedId, feedTitle, "", Feed.DEDAULT_ICON_PATH, "", 0, "")
                feeds.add(feed)
            }
            db.setTransactionSuccessful()
            filter = Filter(id, title, keyword, url, feeds, enabled)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            cursor?.close()
            db.endTransaction()
        }

        return@withContext filter
    }

    /**
     * Delete method for specified filter
     *
     * @param filterId Filter ID to delete
     */
    suspend fun deleteFilter(filterId: Int) = withContext(Dispatchers.IO) {
        try {
            db.beginTransaction()
            val relationWhere = FilterFeedRegistration.FILTER_ID + " = " + filterId
            db.delete(FilterFeedRegistration.TABLE_NAME, relationWhere, null)
            val filterWhere = Filter.ID + " = " + filterId
            db.delete(Filter.TABLE_NAME, filterWhere, null)
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Timber.e(e)
        } finally {
            db.endTransaction()
        }
    }
}
