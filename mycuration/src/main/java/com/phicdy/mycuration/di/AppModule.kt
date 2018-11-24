package com.phicdy.mycuration.di

import android.database.sqlite.SQLiteDatabase
import com.phicdy.mycuration.data.db.DatabaseAdapter
import com.phicdy.mycuration.data.db.DatabaseHelper
import com.phicdy.mycuration.data.repository.ArticleRepository
import com.phicdy.mycuration.data.repository.CurationRepository
import com.phicdy.mycuration.data.repository.FilterRepository
import com.phicdy.mycuration.data.repository.RssRepository
import com.phicdy.mycuration.data.repository.UnreadCountRepository
import com.phicdy.mycuration.domain.task.NetworkTaskManager
import com.phicdy.mycuration.presentation.presenter.ArticleListPresenter
import com.phicdy.mycuration.presentation.presenter.CurationListPresenter
import com.phicdy.mycuration.presentation.presenter.RssListPresenter
import com.phicdy.mycuration.presentation.presenter.TopActivityPresenter
import com.phicdy.mycuration.presentation.view.CurationListView
import com.phicdy.mycuration.presentation.view.RssListView
import com.phicdy.mycuration.presentation.view.TopActivityView
import com.phicdy.mycuration.util.PreferenceHelper
import com.phicdy.mycuration.util.log.TimberTree
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module


val appModule = module {

    single<SQLiteDatabase> { DatabaseHelper(androidApplication()).writableDatabase }
    single { RssRepository(get(), get(), get()) }
    single { ArticleRepository(get()) }
    single { CurationRepository(get()) }
    single { FilterRepository(get()) }
    single { PreferenceHelper }
    single { NetworkTaskManager(get(), get()) }
    single { UnreadCountRepository(get(), get()) }

    single { TimberTree() }

    scope("top") { (view: TopActivityView) ->
        TopActivityPresenter(
                view = view,
                articleRepository = get(),
                rssRepository = get()
        )
    }

    scope("rss_list") { (view: RssListView) ->
        RssListPresenter(
                view = view,
                preferenceHelper = get(),
                rssRepository = get(),
                networkTaskManager = get(),
                unreadCountRepository = get()
        )
    }

    scope("article_list") { (feedId: Int, curationId: Int, query: String, action: String) ->
        ArticleListPresenter(
                feedId = feedId,
                curationId = curationId,
                adapter = DatabaseAdapter.getInstance(),
                preferenceHelper = get(),
                unreadCountRepository = get(),
                query = query,
                action = action
        )
    }

    scope("curation_list") { (view: CurationListView) ->
        CurationListPresenter(
                view = view,
                dbAdapter = DatabaseAdapter.getInstance(),
                unreadCountRepository = get()
        )
    }

}