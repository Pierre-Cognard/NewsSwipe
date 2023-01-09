package com.example.newsswipe.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsswipe.R
import com.example.newsswipe.database.DatabaseBookmarks
import com.example.newsswipe.models.News
import com.example.newsswipe.ui.adapter.BookmarksAdapter
import com.google.firebase.auth.FirebaseAuth

class BookmarksActivity : AppCompatActivity() {

    private val databaseBookmarks = DatabaseBookmarks(this)
    private var mAuth = FirebaseAuth.getInstance()
    private val user = if(mAuth.currentUser != null){mAuth.currentUser?.email.toString()} else{"guest"}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        val backButton = findViewById<Button>(R.id.back_button)
        val noBookmarks = findViewById<TextView>(R.id.no_bookmarks)

        val bookmarksList = databaseBookmarks.findBookmarks(user) // setup bookmarks list for recyclerview
        val recyclerView = findViewById<View>(R.id.bookmarks_recycler_view) as RecyclerView
        val mAdapter = BookmarksAdapter(bookmarksList,this)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = mAdapter

        if (bookmarksList.isEmpty())noBookmarks.visibility = View.VISIBLE // if no bookmarks, display text

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletedNews: News = bookmarksList[viewHolder.adapterPosition] // get deleted news
                val check = databaseBookmarks.deleteBookmark(deletedNews.url,user) // delete bookmark from database
                if (check == 1) Toast.makeText(applicationContext, getString(R.string.bookmark_delete_success), Toast.LENGTH_SHORT).show()
                else Toast.makeText(applicationContext, getString(R.string.bookmark_delete_error), Toast.LENGTH_SHORT).show()

                bookmarksList.removeAt(viewHolder.adapterPosition) // delete bookmark from list
                mAdapter.notifyItemRemoved(viewHolder.adapterPosition) // notify adapter
                if (bookmarksList.isEmpty())noBookmarks.visibility = View.VISIBLE // if no bookmarks, display text
            }
        }).attachToRecyclerView(recyclerView)

        backButton.setOnClickListener{ // binding back button
            val intent = Intent(this, NewsActivity::class.java)
            startActivity(intent)
        }
    }
}