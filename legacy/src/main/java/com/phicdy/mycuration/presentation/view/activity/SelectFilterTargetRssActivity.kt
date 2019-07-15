package com.phicdy.mycuration.presentation.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.phicdy.mycuration.entity.Feed
import com.phicdy.mycuration.feature.util.changeTheme
import com.phicdy.mycuration.legacy.R
import com.phicdy.mycuration.presentation.presenter.SelectFilterTargetRssPresenter
import com.phicdy.mycuration.presentation.view.SelectTargetRssView
import com.phicdy.mycuration.presentation.view.fragment.SelectFilterTargetRssFragment

class SelectFilterTargetRssActivity : AppCompatActivity(), SelectTargetRssView {

    companion object {
        const val TARGET_RSS = "targetRss"
    }

    private lateinit var presenter: SelectFilterTargetRssPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_filter_target_rss)
        title = getString(R.string.title_select_filter_rss)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        presenter = SelectFilterTargetRssPresenter(this)
        presenter.create()
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        val rssFragment = fragment as? SelectFilterTargetRssFragment // maybe Glide's fragment
        val selectedList = intent.getParcelableArrayListExtra<Feed>(TARGET_RSS)
        rssFragment?.updateSelected(selectedList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_select_filter_rss, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // For arrow button on toolbar
            android.R.id.home -> finish()
            else -> presenter.optionItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        changeTheme()
    }

    override fun finishSelect() {
        val data = Intent()
        val bundle = Bundle()
        val manager = supportFragmentManager
        val fragment = manager.findFragmentById(R.id.f_select_target) as SelectFilterTargetRssFragment
        bundle.putParcelableArrayList(RegisterFilterActivity.KEY_SELECTED_FEED, fragment.list())
        data.putExtras(bundle)
        setResult(RESULT_OK, data)
        finish()
    }
}
