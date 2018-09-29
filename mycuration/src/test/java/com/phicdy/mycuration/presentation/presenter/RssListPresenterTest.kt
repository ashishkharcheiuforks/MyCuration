package com.phicdy.mycuration.presentation.presenter


import android.content.Context
import android.content.SharedPreferences
import com.phicdy.mycuration.data.db.DatabaseAdapter
import com.phicdy.mycuration.data.rss.Feed
import com.phicdy.mycuration.domain.rss.UnreadCountManager
import com.phicdy.mycuration.domain.task.NetworkTaskManager
import com.phicdy.mycuration.presentation.view.RssItemView
import com.phicdy.mycuration.presentation.view.RssListView
import com.phicdy.mycuration.presentation.view.fragment.RssListFragment
import com.phicdy.mycuration.util.PreferenceHelper
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class RssListPresenterTest {

    private lateinit var presenter: RssListPresenter
    private val view = Mockito.mock(RssListView::class.java)
    private val adapter = Mockito.mock(DatabaseAdapter::class.java)
    private val mockPref = Mockito.mock(SharedPreferences::class.java)

    @Before
    fun setup() {
        DatabaseAdapter.inject(adapter)

        // Mock two RSS returns
        val firstRss = Feed(FIRST_RSS_ID, FIRST_RSS_TITLE, "", Feed.DEDAULT_ICON_PATH, "", FIRST_RSS_UNREAD_COUNT, "")
        val secondRss = Feed(SECOND_RSS_ID, SECOND_RSS_TITLE, "", SECOND_RSS_ICON_PATH, "", SECOND_RSS_UNREAD_COUNT, "")
        val allFeeds = arrayListOf(firstRss, secondRss)
        `when`(adapter.allFeedsWithNumOfUnreadArticles).thenReturn(allFeeds)
        `when`(adapter.numOfFeeds).thenReturn(2)

        // Set up mock PreferenceHelper
        val mockContext = Mockito.mock(Context::class.java)
        `when`(mockContext.getSharedPreferences("FilterPref", Context.MODE_PRIVATE)).thenReturn(mockPref)
        PreferenceHelper.setUp(mockContext)
        val mockEdit = mock(SharedPreferences.Editor::class.java)
        `when`(mockPref.edit()).thenReturn(mockEdit)
        `when`(mockEdit.putLong(anyString(), anyLong())).thenReturn(mockEdit)

        UnreadCountManager.addFeed(firstRss)
        UnreadCountManager.addFeed(secondRss)
        presenter = RssListPresenter(view, PreferenceHelper, adapter, NetworkTaskManager, UnreadCountManager)

        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun tearDown() {
        UnreadCountManager.clear()
        RxAndroidPlugins.reset()
    }

    // For coverage
    @Test
    fun testOnCreate() {
        presenter.create()
    }

    @Test
    fun `when onResume and RSS doesn't exist then hide all unread view`() {
        `when`(adapter.numOfFeeds).thenReturn(0)
        presenter.resume()
        verify(view, times(1)).hideAllUnreadView()
    }

    @Test
    fun `when onResume and RSS doesn't exist then hide recyclerview`() {
        `when`(adapter.numOfFeeds).thenReturn(0)
        presenter.resume()
        verify(view, times(1)).hideRecyclerView()
    }

    @Test
    fun `when onResume and RSS doesn't exist then show empty view`() {
        `when`(adapter.numOfFeeds).thenReturn(0)
        presenter.resume()
        verify(view, times(1)).showEmptyView()
    }

    @Test
    fun `when onResume and RSS exist then show all unread view`() {
        presenter.resume()
        verify(view, times(1)).showAllUnreadView()
    }

    @Test
    fun `when onResume and RSS exist then show recyclerview`() {
        presenter.resume()
        verify(view, times(1)).showRecyclerView()
    }

    @Test
    fun `when onResume and RSS exist then hide empty view`() {
        presenter.resume()
        verify(view, times(1)).hideEmptyView()
    }

    @Test
    fun `when onResume and RSS exist then set num of unread count`() {
        presenter.resume()
        verify(view, times(1)).setTotalUnreadCount(1)
    }

    @Test
    fun `when onResume and RSS exist then fetch RSS from database`() {
        presenter.resume()
        verify(adapter, times(1)).allFeedsWithNumOfUnreadArticles
    }

    @Test
    fun `when onResume and RSS exist then init with hidden list`() {
        presenter.resume()
        verify(view, times(1)).init(presenter.unreadOnlyFeeds)
    }

    @Test
    fun `when onResume and show all RSS and onResume then init with all list`() {
        presenter.resume()
        presenter.onRssFooterClicked() // call init(allFeeds)
        presenter.pause()
        presenter.resume()
        verify(view, times(2)).init(presenter.allFeeds)
    }

    @Test
    fun `when onResume and RSS exist and auto update in main UI is enabled and after interval then show refreshing view`() {
        `when`(mockPref.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        `when`(mockPref.getLong(anyString(), anyLong())).thenReturn(System.currentTimeMillis()-1000*60)
        presenter.resume()
        verify(view, times(1)).setRefreshing(true)
    }

    @Test
    fun `when onResume and RSS exist and auto update in main UI is enabled and before interval then show refreshing view`() {
        `when`(mockPref.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        `when`(mockPref.getLong(anyString(), anyLong())).thenReturn(System.currentTimeMillis())
        presenter.resume()
        verify(view, times(0)).setRefreshing(true)
    }

    @Test
    fun `when onResume and RSS exist and auto update in main UI is disabled then not show refreshing view`() {
        `when`(mockPref.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        presenter.resume()
        verify(view, times(0)).setRefreshing(true)
    }

    @Test
    fun `when first RSS is hidden then first RSS title will be second RSS`() {
        presenter.resume()
        assertThat(presenter.unreadOnlyFeeds.size, `is`(1))
        assertThat(presenter.unreadOnlyFeeds[0].title, `is`(SECOND_RSS_TITLE))
    }

    @Test
    fun `when all of articles were read then show all of RSS`() {
        val firstRss = Feed(FIRST_RSS_ID, FIRST_RSS_TITLE, "", "", "", 0, "")
        val secondRss = Feed(SECOND_RSS_ID, SECOND_RSS_TITLE, "", "", "", 0, "")
        val alreadyReadRss = arrayListOf(firstRss, secondRss)
        `when`(adapter.allFeedsWithNumOfUnreadArticles).thenReturn(alreadyReadRss)
        presenter.resume()
        assertThat(presenter.unreadOnlyFeeds.size, `is`(2))
        assertThat(presenter.unreadOnlyFeeds[0].title, `is`(FIRST_RSS_TITLE))
        assertThat(presenter.unreadOnlyFeeds[1].title, `is`(SECOND_RSS_TITLE))
    }

    @Test
    fun `when delete menu is clicked then show alert dialog`() {
        presenter.onDeleteFeedMenuClicked(0)
        verify(view, times(1)).showDeleteFeedAlertDialog(0)
    }

    @Test
    fun `when first RSS is hidden then first edit title will be second RSS`() {
        // Default hidden option is enaled
        presenter.resume()
        presenter.onEditFeedMenuClicked(FIRST_RSS_POSITION)
        verify(view, times(1)).showEditTitleDialog(FIRST_RSS_POSITION, SECOND_RSS_TITLE)
    }

    @Test
    fun `when RSS is not hidden then first edit title will be first RSS`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        presenter.onEditFeedMenuClicked(FIRST_RSS_POSITION)
        verify(view, times(1)).showEditTitleDialog(FIRST_RSS_POSITION, FIRST_RSS_TITLE)
    }

    @Test
    fun `when edit ok button is clicked and new title is empty then show error toast`() {
        presenter.onEditFeedOkButtonClicked("", 0)
        verify(view, times(1)).showEditFeedTitleEmptyErrorToast()
    }

    @Test
    fun `when edit ok button is clicked and new title is blank then show error toast`() {
        presenter.onEditFeedOkButtonClicked("   ", 0)
        verify(view, times(1)).showEditFeedTitleEmptyErrorToast()
    }

    @Test
    fun `when edit ok button is clicked and succeeds then show success toast`() {
        `when`(adapter.saveNewTitle(anyInt(), anyString())).thenReturn(1)
        presenter.onEditFeedOkButtonClicked("newTitle", 0)
        verify(view, times(1)).showEditFeedSuccessToast()
    }

    @Test
    fun `when edit ok button is clicked and succeeds then refresh the list`() {
        `when`(adapter.saveNewTitle(anyInt(), anyString())).thenReturn(1)
        presenter.onEditFeedOkButtonClicked("newTitle", 0)
        verify(view, times(1)).notifyDataSetChanged()
    }

    @Test
    fun `when edit ok button is clicked and succeeds then the title will be updated`() {
        `when`(adapter.saveNewTitle(anyInt(), anyString())).thenReturn(1)
        presenter.resume() // init list
        presenter.onEditFeedOkButtonClicked("newTitle", 0)
        // Current status is hidden, first position RSS is first one in hidden RSS list and second one in all RSS list
        assertThat(presenter.unreadOnlyFeeds[0].title, `is`("newTitle"))
        assertThat(presenter.allFeeds[1].title, `is`("newTitle"))
    }

    @Test
    fun `when edit ok button is clicked and fails then show error toast`() {
        `when`(adapter.saveNewTitle(anyInt(), anyString())).thenReturn(0)
        presenter.onEditFeedOkButtonClicked("newTitle", 0)
        verify(view, times(1)).showEditFeedFailToast()
    }

    @Test
    fun `when delete ok button is clicked and fails then show error toast`() {
        `when`(adapter.deleteFeed(anyInt())).thenReturn(false)
        presenter.onDeleteOkButtonClicked(0)
        verify(view, times(1)).showDeleteFailToast()
    }

    @Test
    fun `when delete ok button is clicked and succeeds then show success toast`() {
        `when`(adapter.deleteFeed(anyInt())).thenReturn(true)
        presenter.resume() // init list
        presenter.onDeleteOkButtonClicked(0)
        verify(view, times(1)).showDeleteSuccessToast()
    }

    @Test
    fun `when delete ok button is clicked in hidden status and succeeds then delete the RSS`() {
        `when`(adapter.deleteFeed(anyInt())).thenReturn(true)
        presenter.resume() // init list
        presenter.onDeleteOkButtonClicked(0)
        // Current status is hidden and size is 1, so hidden list becomes all RSS list after refresh
        assertThat(presenter.unreadOnlyFeeds[0].id, `is`(FIRST_RSS_ID))
        assertThat(presenter.unreadOnlyFeeds.size, `is`(1))
        assertThat(presenter.allFeeds[0].id, `is`(FIRST_RSS_ID))
        assertThat(presenter.allFeeds.size, `is`(1))
    }

    @Test
    fun `when delete ok button is clicked in all of RSS and succeeds then delete the RSS`() {
        `when`(adapter.deleteFeed(anyInt())).thenReturn(true)
        presenter.resume() // init list
        presenter.onRssFooterClicked() // Change to all RSS
        presenter.onDeleteOkButtonClicked(0)
        // Current status is all and first RSS status is read, so hidden list has no update
        assertThat(presenter.unreadOnlyFeeds[0].id, `is`(SECOND_RSS_ID))
        assertThat(presenter.unreadOnlyFeeds.size, `is`(1))
        assertThat(presenter.allFeeds[0].id, `is`(SECOND_RSS_ID))
        assertThat(presenter.allFeeds.size, `is`(1))
    }

    @Test
    fun `when delete all of RSS then show empty view`() {
        `when`(adapter.deleteFeed(anyInt())).thenReturn(true)
        presenter.resume() // init list
        presenter.onRssFooterClicked() // Change to all RSS
        presenter.onDeleteOkButtonClicked(0)
        presenter.onDeleteOkButtonClicked(0)
        verify(view, times(1)).showEmptyView()
    }

    @Test
    fun `when RSS is clicked then callback is called`() {
        val listner = mock(RssListFragment.OnFeedListFragmentListener::class.java)
        presenter.resume() // init list
        presenter.onRssItemClicked(0, listner)
        verify(listner, times(1)).onListClicked(SECOND_RSS_ID)
    }

    @Test
    fun `when invalid RSS is clicked then callback is not called`() {
        val listner = mock(RssListFragment.OnFeedListFragmentListener::class.java)
        presenter.resume() // init list
        presenter.onRssItemClicked(9999, listner)
        verify(listner, times(0)).onListClicked(SECOND_RSS_ID)
    }

    @Test
    fun `when RSS is clicked and listener is null then not crashed`() {
        presenter.resume() // init list
        presenter.onRssItemClicked(0, null)
        assertTrue(true)
    }

    @Test
    fun `when refresh and RSS is empty then finish refresh`() {
        `when`(adapter.allFeedsWithNumOfUnreadArticles).thenReturn(arrayListOf())
        presenter.resume()
        presenter.onRefresh()
        verify(view, times(1)).onRefreshCompleted()
    }

    @Test
    fun `when refresh and RSS exist then start to update`() {
        presenter.resume()
        presenter.onRefresh()
        verify(view, times(0)).onRefreshCompleted()
    }

    @Test
    fun `when finish refresh then hide refresh view`() {
        presenter.onFinishUpdate()
        verify(view, times(1)).onRefreshCompleted()
    }

    @Test
    fun `when finish refresh then fetch RSS`() {
        presenter.onFinishUpdate()
        verify(adapter, times(1)).allFeedsWithNumOfUnreadArticles
    }

    @Test
    fun `when finish refresh then reload RSS list`() {
        presenter.onFinishUpdate()
        verify(view, times(1)).init(presenter.unreadOnlyFeeds)
    }

    @Test
    fun `when finish refresh then last update time will be updated`() {
        presenter.onFinishUpdate()
        verify(mockPref.edit(), times(1)).putLong(anyString(), anyLong())
    }

    @Test
    fun `when get item count in RecyclerView in hide status then return num of unread RSS + 1 for footer`() {
        presenter.resume()
        assertThat(presenter.getItemCount(), `is`(2))
    }

    @Test
    fun `when get item count in RecyclerView in all status then return num of unread RSS + 1 for footer`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        assertThat(presenter.getItemCount(), `is`(3))
    }

    @Test
    fun `when bind default icon RSS then show default icon`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        val mockRssItemView = mock(RssItemView.Content::class.java)
        presenter.onBindRssViewHolder(0, mockRssItemView)
        verify(mockRssItemView, times(1)).showDefaultIcon()

    }

    @Test
    fun `when bind not default icon RSS then show the icon`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        val mockRssItemView = mock(RssItemView.Content::class.java)
        presenter.onBindRssViewHolder(1, mockRssItemView)
        verify(mockRssItemView, times(1)).showIcon(SECOND_RSS_ICON_PATH)

    }

    @Test
    fun `when bind not default icon RSS and fails to show the icon then update the path to default`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        val mockRssItemView = mock(RssItemView.Content::class.java)
        `when`(mockRssItemView.showIcon(SECOND_RSS_ICON_PATH)).thenReturn(false)
        presenter.onBindRssViewHolder(1, mockRssItemView)
        verify(adapter, times(1)).saveIconPath(anyString(), eq(Feed.DEDAULT_ICON_PATH))

    }

    @Test
    fun `when bind RSS then update the title`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        val mockRssItemView = mock(RssItemView.Content::class.java)
        presenter.onBindRssViewHolder(0, mockRssItemView)
        verify(mockRssItemView, times(1)).updateTitle(FIRST_RSS_TITLE)

    }

    @Test
    fun `when bind RSS in all status then update unread count`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        val mockRssItemView = mock(RssItemView.Content::class.java)
        presenter.onBindRssViewHolder(0, mockRssItemView)
        verify(mockRssItemView, times(1)).updateUnreadCount(FIRST_RSS_UNREAD_COUNT.toString())

    }

    @Test
    fun `when bind RSS in hidden status then update unread count`() {
        presenter.resume()
        val mockRssItemView = mock(RssItemView.Content::class.java)
        presenter.onBindRssViewHolder(0, mockRssItemView)
        verify(mockRssItemView, times(1)).updateUnreadCount(SECOND_RSS_UNREAD_COUNT.toString())

    }

    @Test
    fun `when bind footer in all status then show hide view`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        val mockRssFooterView = mock(RssItemView.Footer::class.java)
        presenter.onBindRssFooterViewHolder(mockRssFooterView)
        verify(mockRssFooterView, times(1)).showHideView()

    }

    @Test
    fun `when bind footer in hidden status then show all view`() {
        presenter.resume()
        val mockRssFooterView = mock(RssItemView.Footer::class.java)
        presenter.onBindRssFooterViewHolder(mockRssFooterView)
        verify(mockRssFooterView, times(1)).showAllView()
    }

    @Test
    fun `when get item view type in hide status and position is same with size then rturn footer`() {
        presenter.resume()
        assertThat(presenter.onGetItemViewType(1), `is`(RssListFragment.VIEW_TYPE_FOOTER))
    }

    @Test
    fun `when get item view type in hide status and position is not same with size then rturn footer`() {
        presenter.resume()
        assertThat(presenter.onGetItemViewType(0), `is`(RssListFragment.VIEW_TYPE_RSS))
    }

    @Test
    fun `when get item view type in all status and position is same with size then rturn footer`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        assertThat(presenter.onGetItemViewType(2), `is`(RssListFragment.VIEW_TYPE_FOOTER))
    }

    @Test
    fun `when get item view type in all status and position is not same with size then rturn footer`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        assertThat(presenter.onGetItemViewType(0), `is`(RssListFragment.VIEW_TYPE_RSS))
    }

    @Test
    fun `when click footer twice then go back to hidden status`() {
        presenter.resume()
        presenter.onRssFooterClicked()
        presenter.onRssFooterClicked()
        assertThat(presenter.getItemCount(), `is`(2))
    }

    companion object {

        private const val FIRST_RSS_TITLE = "rss1"
        private const val SECOND_RSS_TITLE = "rss2"
        private const val FIRST_RSS_ID = 0
        private const val SECOND_RSS_ID = 1
        private const val FIRST_RSS_POSITION = 0
        private const val SECOND_RSS_ICON_PATH = "/data/data/com.phicdy.mycuration/somewhere"
        private const val FIRST_RSS_UNREAD_COUNT = 0
        private const val SECOND_RSS_UNREAD_COUNT = 1
    }
}