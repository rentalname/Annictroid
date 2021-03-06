package me.kirimin.annictroid.work

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_work.*

import me.kirimin.annictroid.R
import me.kirimin.annictroid._common.networks.RetrofitClient
import me.kirimin.annictroid._common.networks.apis.AnnictService
import me.kirimin.annictroid._common.preferences.AppPreferences
import me.kirimin.annictroid.episode.EpisodeActivity
import rx.Observable
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class WorkActivity : AppCompatActivity() {

    private val subscriptions = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)
        setSupportActionBar(toolbar)
        supportActionBar?.title = intent.getStringExtra(WORK_TITLE)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val worksObservable = RetrofitClient.default().build().create(AnnictService::class.java)
                .works(token = AppPreferences.getToken(),
                        workIds = intent.getStringExtra(WORK_ID))
        val episodesObservable = RetrofitClient.default().build().create(AnnictService::class.java)
                .episodes(token = AppPreferences.getToken(),
                        workId = intent.getStringExtra(WORK_ID),
                        sortNumber = "desc")
        subscriptions.add(Single.zip(worksObservable, episodesObservable, { works, episodes -> Pair(works, episodes) })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    layoutParent.visibility = View.VISIBLE
                    val work = it.first.works[0]
                    val episodes = it.second.episodes
                    textViewSeason.text = getString(R.string.work_season, work.season_name_text)
                    textViewWatchersCount.text = getString(R.string.work_watchers_count, work.watchers_count)
                    textViewEpisodeCount.text = getString(R.string.work_episode_count, work.episodes_count)
                    textViewHashTag.text = getString(R.string.work_hashtag, work.twitter_hashtag ?: "")
                    work.official_site_url?.let {
                        buttonSite.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(work.official_site_url))) }
                    }
                    work.wikipedia_url?.let {
                        buttonWikipedia.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(work.wikipedia_url))) }
                    }
                    work.twitter_username?.let {
                        buttonTwitter.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + work.twitter_username))) }
                    }
                    episodes.forEach { episode ->
                        val episodeView = layoutInflater.inflate(R.layout.view_episode, null)
                        episodeView.setOnClickListener {
                            val intent = Intent(this, EpisodeActivity::class.java)
                            intent.putExtras(EpisodeActivity.getBundle(episode.id, episode.work.title))
                            startActivity(intent)
                        }
                        (episodeView.findViewById(R.id.viewEpisodeTextViewNumber) as TextView).text = episode.number_text
                        (episodeView.findViewById(R.id.viewEpisodeTextViewEpisode) as TextView).text = episode.title ?: ""
                        layoutEpisodeList.addView(episodeView)
                    }
                }, {
                    Toast.makeText(this, R.string.common_network_error, Toast.LENGTH_SHORT).show()
                }))
    }

    override fun onDestroy() {
        subscriptions.unsubscribe()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (android.R.id.home == item.itemId) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        val WORK_ID = "workId"
        val WORK_TITLE = "workTitle"

        fun getBundle(workId: String, workTitle: String = ""): Bundle {
            val bundle = Bundle()
            bundle.putString(WORK_ID, workId)
            bundle.putString(WORK_TITLE, workTitle)
            return bundle
        }
    }
}
