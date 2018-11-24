package com.phicdy.mycuration.data.repository

import android.support.test.InstrumentationRegistry.getTargetContext
import com.phicdy.mycuration.data.db.DatabaseAdapter
import com.phicdy.mycuration.data.db.DatabaseHelper
import com.phicdy.mycuration.data.rss.Feed
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class UnreadCountRepositoryTest {

    private lateinit var rssRepository: RssRepository
    private lateinit var articleRepository: ArticleRepository
    private lateinit var filterRepository: FilterRepository
    private lateinit var curationRepository: CurationRepository
    private lateinit var unreadCountRepository: UnreadCountRepository
    private lateinit var adapter: DatabaseAdapter
    private lateinit var rss: Feed

    @Before
    fun setUp() = runBlocking {
        val db = DatabaseHelper(getTargetContext()).writableDatabase
        articleRepository = ArticleRepository(db)
        curationRepository = CurationRepository(db)
        filterRepository = FilterRepository(db)
        rssRepository = RssRepository(db, articleRepository, filterRepository)
        unreadCountRepository = UnreadCountRepository(rssRepository, curationRepository)
        adapter = DatabaseAdapter.getInstance()
        adapter.deleteAll()

        rss = adapter.saveNewFeed("title", "http://www.google.com", "RSS", "http://yahoo.co.jp")
        rssRepository.updateUnreadArticleCount(rss.id, TEST_RSS_DEFAULT_UNREAD_COUNT)
        unreadCountRepository.retrieve()
    }

    @After
    fun tearDown() {
        adapter.deleteAll()
    }

    @Test
    fun whenAppendUnreadCount_ThenUnreadCountIncreases() = runBlocking {
        unreadCountRepository.appendUnreadArticleCount(rss.id, 10)
        val updatedRss = rssRepository.getFeedWithUnreadCountBy(rss.id)
        assertNotNull(updatedRss)
        assertThat(updatedRss?.unreadAriticlesCount, `is`(TEST_RSS_DEFAULT_UNREAD_COUNT + 10))
    }

    @Test
    fun whenDecreaseCount_ThenUnreadCountDecreases() = runBlocking {
        unreadCountRepository.decreaseCount(rss.id, 4)
        val updatedRss = rssRepository.getFeedWithUnreadCountBy(rss.id)
        assertNotNull(updatedRss)
        assertThat(updatedRss?.unreadAriticlesCount, `is`(1))
        assertThat(unreadCountRepository.total, `is`(TEST_RSS_DEFAULT_UNREAD_COUNT - 4))
    }

    @Test
    fun whenTooBigDecreaseCount_ThenUnreadCountWillBe0() = runBlocking {
        unreadCountRepository.decreaseCount(rss.id, TEST_RSS_DEFAULT_UNREAD_COUNT + 1)
        val updatedRss = rssRepository.getFeedWithUnreadCountBy(rss.id)
        assertNotNull(updatedRss)
        assertThat(updatedRss?.unreadAriticlesCount, `is`(0))
        assertThat(unreadCountRepository.total, `is`(0))
    }

    companion object {
        private const val TEST_RSS_DEFAULT_UNREAD_COUNT = 5
    }
}