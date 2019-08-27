package com.phicdy.mycuration.presentation.presenter

import com.phicdy.mycuration.data.preference.PreferenceHelper
import com.phicdy.mycuration.data.repository.RssRepository
import com.phicdy.mycuration.domain.task.NetworkTaskManager
import com.phicdy.mycuration.entity.Feed
import com.phicdy.mycuration.presentation.view.RssItemView
import com.phicdy.mycuration.presentation.view.RssListView
import com.phicdy.mycuration.presentation.view.fragment.RssListFragment
import kotlinx.coroutines.coroutineScope
import java.util.ArrayList

class RssListPresenter(private val view: RssListView,
                       private val preferenceHelper: PreferenceHelper,
                       private val rssRepository: RssRepository,
                       private val networkTaskManager: NetworkTaskManager) {

    var unreadOnlyFeeds = arrayListOf<Feed>()
        private set
    var allFeeds = arrayListOf<Feed>()
        private set

    // Manage hide feed status
    private var isHided = true

    private val isAfterInterval: Boolean
        get() = System.currentTimeMillis() - preferenceHelper.lastUpdateDate >= 1000 * 60

    fun create() {}

    suspend fun resume() = coroutineScope {
        if (rssRepository.getNumOfRss() == 0) {
            updateViewForEmpty()
        } else {
            view.showAllUnreadView()
            view.showRecyclerView()
            view.hideEmptyView()
            fetchAllRss()
            refreshList()
            if (preferenceHelper.autoUpdateInMainUi && isAfterInterval) {
                view.setRefreshing(true)
                updateAllRss()
            }
        }
    }

    private fun generateHidedFeedList() {
        if (allFeeds.isEmpty()) return
        unreadOnlyFeeds = allFeeds.filter { it.unreadAriticlesCount > 0 } as ArrayList<Feed>
        if (unreadOnlyFeeds.isEmpty()) {
            unreadOnlyFeeds = allFeeds
        }
    }

    private fun refreshList() {
        generateHidedFeedList()
        if (isHided) {
            view.init(unreadOnlyFeeds)
        } else {
            view.init(allFeeds)
        }
        view.setTotalUnreadCount(allFeeds.sumBy { it.unreadAriticlesCount })
    }

    fun pause() {}

    fun onDeleteFeedMenuClicked(position: Int) {
        view.showDeleteFeedAlertDialog(getFeedIdAtPosition(position), position)
    }

    fun onEditFeedMenuClicked(position: Int) {
        view.showEditTitleDialog(
                rssId = getFeedIdAtPosition(position),
                feedTitle = getFeedTitleAtPosition(position)
        )
    }

    private fun getFeedTitleAtPosition(position: Int): String {
        if (position < 0) return ""
        return if (isHided) {
            if (position > unreadOnlyFeeds.size - 1) return ""
            unreadOnlyFeeds[position].title
        } else {
            if (position > allFeeds.size - 1) return ""
            allFeeds[position].title
        }
    }

    fun updateFeedTitle(feedId: Int, newTitle: String) {
        for (feed in allFeeds) {
            if (feed.id == feedId) {
                feed.title = newTitle
                break
            }
        }
        for (feed in unreadOnlyFeeds) {
            if (feed.id == feedId) {
                feed.title = newTitle
                break
            }
        }
        view.notifyDataSetChanged()
    }

    private fun getFeedIdAtPosition(position: Int): Int {
        if (position < 0) return -1

        if (isHided) {
            return if (position > unreadOnlyFeeds.size - 1) {
                -1
            } else unreadOnlyFeeds[position].id
        } else {
            if (position > allFeeds.size - 1) {
                return -1
            }
        }
        return allFeeds[position].id
    }

    suspend fun deleteFeedAtPosition(position: Int) = coroutineScope {
        fun deleteAtPosition(currentList: ArrayList<Feed>, oppositeList: ArrayList<Feed>) {
            if (currentList.size <= position) return
            val (id) = currentList[position]
            currentList.removeAt(position)
            for (i in oppositeList.indices) {
                if (oppositeList[i].id == id) {
                    oppositeList.removeAt(i)
                    break
                }
            }
        }

        if (isHided) {
            deleteAtPosition(unreadOnlyFeeds, allFeeds)
        } else {
            deleteAtPosition(allFeeds, unreadOnlyFeeds)
        }
        refreshList()
        if (allFeeds.isEmpty()) updateViewForEmpty()
    }

    private fun updateViewForEmpty() {
        view.hideAllUnreadView()
        view.hideRecyclerView()
        view.showEmptyView()
    }

    fun onRssItemClicked(position: Int, mListener: RssListFragment.OnFeedListFragmentListener?) {
        val feedId = getFeedIdAtPosition(position)
        if (feedId != -1) mListener?.onListClicked(feedId)
    }

    fun onRssFooterClicked() {
        changeHideStatus()
    }

    private fun changeHideStatus() {
        generateHidedFeedList()
        if (isHided) {
            isHided = false
            view.init(allFeeds)
        } else {
            isHided = true
            view.init(unreadOnlyFeeds)
        }
    }

    suspend fun onRefresh() = coroutineScope {
        if (allFeeds.isEmpty()) {
            onRefreshComplete()
            return@coroutineScope
        }
        updateAllRss()
    }

    private suspend fun updateAllRss() = coroutineScope {
        try {
            networkTaskManager.updateAll(allFeeds)
            onFinishUpdate()
        } catch (e: Exception) {
        }
    }

    private fun onRefreshComplete() {
        view.onRefreshCompleted()
    }

    suspend fun onFinishUpdate() = coroutineScope {
        fetchAllRss()
        refreshList()
        onRefreshComplete()
        preferenceHelper.lastUpdateDate = System.currentTimeMillis()
    }

    private suspend fun fetchAllRss() = coroutineScope {
        allFeeds = rssRepository.getAllFeedsWithNumOfUnreadArticles()
    }

    fun getItemCount(): Int {
        // Add +1 for the footer
        if (isHided) return unreadOnlyFeeds.size + 1
        return allFeeds.size + 1
    }

    fun onBindRssViewHolder(position: Int, view: RssItemView.Content) {
        val feed = if (isHided) unreadOnlyFeeds[position] else allFeeds[position]
        if (feed.iconPath.isBlank() || feed.iconPath == Feed.DEDAULT_ICON_PATH) {
            view.showDefaultIcon()
        } else {
            view.showIcon(feed.iconPath)
        }
        view.updateTitle(feed.title)
        view.updateUnreadCount(feed.unreadAriticlesCount.toString())
    }

    fun onBindRssFooterViewHolder(view: RssItemView.Footer) {
        if (isHided) {
            view.showAllView()
        } else {
            view.showHideView()
        }
    }

    fun onGetItemViewType(position: Int): Int {
        return if (isHided) {
            if (position == unreadOnlyFeeds.size) {
                RssListFragment.VIEW_TYPE_FOOTER
            } else {
                RssListFragment.VIEW_TYPE_RSS
            }
        } else {
            if (position == allFeeds.size) {
                RssListFragment.VIEW_TYPE_FOOTER
            } else {
                RssListFragment.VIEW_TYPE_RSS
            }
        }
    }
}
