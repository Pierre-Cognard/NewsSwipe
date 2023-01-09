package com.example.newsswipe.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.newsswipe.R
import com.example.newsswipe.database.DatabaseBookmarks
import com.example.newsswipe.database.DatabaseKeywords
import com.example.newsswipe.models.News
import com.example.newsswipe.ui.adapter.NewsAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.koushikdutta.ion.Ion
import com.yuyakaido.android.cardstackview.*
import org.json.JSONArray
import org.json.JSONObject


class NewsActivity : AppCompatActivity(), CardStackListener {

    private var mAuth = FirebaseAuth.getInstance()
    private val databaseKeywords = DatabaseKeywords(this)
    private val databaseBookmarks = DatabaseBookmarks(this)
    private val user = if(mAuth.currentUser != null){mAuth.currentUser?.email.toString()} else{"guest"}
    private lateinit var currentNews: News
    private lateinit var articles : MutableList<News>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        val settingsButton = findViewById<ImageButton>(R.id.settings_button)
        val logoutButton = findViewById<Button>(R.id.logout_button)
        val bookmarksButton = findViewById<Button>(R.id.bookmarks_button)
        val username = findViewById<TextView>(R.id.username)
        val cardStackView = findViewById<CardStackView>(R.id.card_stack_view)
        val manager = CardStackLayoutManager(this,this)

        if (mAuth.currentUser != null) username.text = mAuth.currentUser?.email
        else{
            logoutButton.text = getString(R.string.login)
            username.text = getString(R.string.guest)
        }

        val keywordsList : MutableList<String> = databaseKeywords.findKeywords(user)
        articles = newsAPI(keywordsList) // setup news list for recyclerview
        val mAdapter = NewsAdapter(articles,this)
        cardStackView.layoutManager = manager
        cardStackView.adapter = mAdapter
        init(manager,keywordsList)

        logoutButton.setOnClickListener { // binding log out button
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener { // binding settings button
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        bookmarksButton.setOnClickListener { // binding bookmarks button
            val intent = Intent(this, BookmarksActivity::class.java)
            startActivity(intent)
        }
    }

    private fun newsAPI(keywordsList: MutableList<String>): MutableList<News> {
        val listNews = mutableListOf<News>()
        if (keywordsList.isEmpty()){ // if the user has no keyword
            listNews.add(News(getString(R.string.no_keywords),"null","null","null","https://i.postimg.cc/8zJqXQqy/logo.png"))
        }
        else {
            val prefs = getSharedPreferences("Language", Context.MODE_PRIVATE)
            val language = prefs.getString("News_language",null) // get news language
            for (word in keywordsList) {
                val request = Ion.with(this)
                    .load("https://newsdata.io/api/1/news?apikey=pub_15395e81b342a68279889bacbc62c91742b75&q=$word&language=$language")
                    .setHeader("Accept", "application/json")
                    .setHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .asString()
                    .withResponse()
                    .get()

                // fetch API result
                val myJSON = JSONObject(request.result.toString())
                val listArticles = myJSON.getString("results")
                val myJSONArticles = JSONArray(listArticles)
                val nb: Int = if (myJSONArticles.length() > 5) 5 // set 5 news max per keyword
                else myJSONArticles.length()
                for (i in 0 until nb) {
                    val row = JSONObject(myJSONArticles.getJSONObject(i).toString())
                    val title = row.getString("title")
                    val author = row.getString("source_id")
                    val url = row.getString("link")
                    var image = row.getString("image_url")
                    val date = row.getString("pubDate")
                    //Log.d("API", "title = $title, author = $author, url = $url, image = $image, date = $date")
                    if (image == "null") image = getString(R.string.link_logo) // if no image, give app logo
                    listNews.add(News(title, author, url, date, image))
                }
            }
        }
        return listNews
    }

    private fun init(manager: CardStackLayoutManager,keywordsList: MutableList<String>) {
        manager.setVisibleCount(3)
        manager.setTranslationInterval(12f)
        manager.setScaleInterval(0.95f)
        manager.setSwipeThreshold(0.4f)
        manager.setMaxDegree(30.0f)
        manager.setDirections(Direction.HORIZONTAL)
        manager.setCanScrollHorizontal(true)
        manager.setCanScrollVertical(false)
        manager.setStackFrom(StackFrom.Bottom)
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
        manager.setOverlayInterpolator(LinearInterpolator())
        if (keywordsList.isEmpty()) manager.setCanScrollHorizontal(false) // Disable swipe if no keywords
    }

    override fun onCardDragging(direction: Direction, ratio: Float) {
        //Log.d("CardStackView", "onCardDragging: d = ${direction.name}, r = $ratio")
    }

    override fun onCardRewound() {
        Log.d("CardStackView","card rewound")
    }

    override fun onCardCanceled() {
        Log.d("CardStackView","card canceled")
    }

    override fun onCardSwiped(direction: Direction?) {
        if (direction.toString() == "Right"){
            databaseBookmarks.addBookmark(user,currentNews.title,currentNews.image,currentNews.url).toInt() // add bookmark
            Toast.makeText(this, getString(R.string.bookmark_add_success), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCardAppeared(view: View, position: Int) { // when a card appear
        setupShareButton(position)
        setCurrentNews(position)
    }

    override fun onCardDisappeared(view: View?, position: Int) { // when a card disappear
        if (position == articles.lastIndex) setupShareButton(-1) // if no more news, unbind the share button
    }

    private fun setupShareButton(position: Int){ // function to bind share button with the link of the current news
        val shareButton = findViewById<FloatingActionButton>(R.id.share_button)
        if (position != -1 && articles[position].url != "null") {
            shareButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TITLE, articles[position].title)
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text).plus(articles[position].url))
                intent.type = "text/plain"
                startActivity(Intent.createChooser(intent, articles[position].title))
            }
        }
        else{
            shareButton.setOnClickListener {
                Toast.makeText(this,getString(R.string.nothing_to_share), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setCurrentNews(position: Int) { // function to update currentNews with the information of the displayed news
        currentNews = articles[position]
    }
}