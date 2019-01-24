package com.phicdy.mycuration.presentation.presenter

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.phicdy.mycuration.data.repository.ArticleRepository
import com.phicdy.mycuration.data.repository.RssRepository
import com.phicdy.mycuration.presentation.view.TopActivityView
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test


class TopActivityPresenterTest {

    private lateinit var mockView: TopActivityView
    private lateinit var mockArticleRepository: ArticleRepository
    private lateinit var mockRssRepository: RssRepository
    private lateinit var presenter: TopActivityPresenter

    @Before
    fun setup() {
        mockView = mock()
        mockArticleRepository = mock()
        mockRssRepository = mock()
        presenter = TopActivityPresenter(mockView, mockArticleRepository, mockRssRepository, mock())
    }

    @Test
    fun `initViewPager is called when onCreate`() {
        presenter.create()
        verify(mockView, times(1)).initViewPager()
    }

    @Test
    fun `initToolbar is called when onCreate`() {
        presenter.create()
        verify(mockView, times(1)).initToolbar()
    }

    @Test
    fun `setAlarmManager is called when onCreate`() {
        presenter.create()
        verify(mockView, times(1)).setAlarmManager()
    }

    @Test
    fun `not go to artcile search result when query is null`() {
        presenter.queryTextSubmit(null)
        verify(mockView, times(0)).goToArticleSearchResult(null.toString())
    }

    @Test
    fun `go to artcile search result when query is not null`() {
        presenter.queryTextSubmit("query")
        verify(mockView, times(1)).goToArticleSearchResult("query")
    }

    @Test
    fun `when curation fab is clicked then close add fab`() = runBlocking {
        whenever(mockRssRepository.getNumOfRss()).thenReturn(1)
        presenter.fabCurationClicked()
        verify(mockView, times(1)).closeAddFab()
    }

    @Test
    fun `when curation fab is clicked and RSS is empty then open RSS search view`() = runBlocking {
        whenever(mockRssRepository.getNumOfRss()).thenReturn(0)
        presenter.fabCurationClicked()
        verify(mockView, times(1)).goToFeedSearch()
    }

    @Test
    fun `when curation fab is clicked and RSS is not empty then open add curation view`() = runBlocking {
        whenever(mockRssRepository.getNumOfRss()).thenReturn(1)
        presenter.fabCurationClicked()
        verify(mockView, times(0)).goToFeedSearch()
        verify(mockView, times(1)).goToAddCuration()
    }

    @Test
    fun `when filter fab is clicked then close add fab`() = runBlocking {
        whenever(mockRssRepository.getNumOfRss()).thenReturn(1)
        presenter.fabFilterClicked()
        verify(mockView, times(1)).closeAddFab()
    }

    @Test
    fun `when filter fab is clicked and RSS is empty then open RSS search view`() = runBlocking {
        whenever(mockRssRepository.getNumOfRss()).thenReturn(0)
        presenter.fabFilterClicked()
        verify(mockView, times(1)).goToFeedSearch()
    }

    @Test
    fun `when filter fab is clicked and RSS is not empty then open add filter view`() = runBlocking {
        whenever(mockRssRepository.getNumOfRss()).thenReturn(1)
        presenter.fabFilterClicked()
        verify(mockView, times(0)).goToFeedSearch()
        verify(mockView, times(1)).goToAddFilter()
    }
}