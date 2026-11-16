package com.kunaiu.manager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var tokenInput: EditText
    private lateinit var saveTokenBtn: Button
    private lateinit var postsRecycler: RecyclerView
    private lateinit var commentsRecycler: RecyclerView
    private lateinit var statusText: TextView

    private var token: String = ""
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenInput = findViewById(R.id.tokenInput)
        saveTokenBtn = findViewById(R.id.saveTokenBtn)
        postsRecycler = findViewById(R.id.postsRecycler)
        commentsRecycler = findViewById(R.id.commentsRecycler)
        statusText = findViewById(R.id.statusText)

        // تحميل التوكن المحفوظ
        token = getSharedPreferences("prefs", MODE_PRIVATE).getString("token", "") ?: ""
        if (token.isNotEmpty()) {
            tokenInput.setText(token)
            statusText.text = "التوكن محفوظ. جاري تحميل المنشورات..."
            loadPosts()
        } else {
            statusText.text = "أدخل التوكن أولاً."
        }

        saveTokenBtn.setOnClickListener {
            val newToken = tokenInput.text.toString().trim()
            if (newToken.isNotEmpty()) {
                token = newToken
                getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("token", token).apply()
                statusText.text = "تم حفظ التوكن. جاري تحميل المنشورات..."
                loadPosts()
            } else {
                Toast.makeText(this, "التوكن فارغ", Toast.LENGTH_SHORT).show()
            }
        }

        postsRecycler.layoutManager = LinearLayoutManager(this)
        commentsRecycler.layoutManager = LinearLayoutManager(this)
    }

    private fun loadPosts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://api.kunaiu.com/anime-social/api/posts?_page_number=1&user_feed=&user_id=&hashtag=&hashtag_id=&post_scope=&anime_id=&manga_id=&character_id=&guild_id=&collection_id=")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Client-Id", "kunaiu-android")
                    .addHeader("Client-Secret", "b7022a16e0c896575g2d82c33a8h2d9d36781g33")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val dataArray = json.getJSONObject("data").getJSONArray("data")

                withContext(Dispatchers.Main) {
                    val adapter = PostsAdapter(dataArray, this@MainActivity)
                    postsRecycler.adapter = adapter
                    statusText.text = "تم تحميل ${dataArray.length()} منشور."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "خطأ: ${e.message}"
                }
            }
        }
    }

    fun loadComments(postId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://api.kunaiu.com/anime-social/api/comments?reference_id=$postId&_page_number=1&_order_by=latest_first&comment_type=post&comment_parent_id=&comment_id=")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Client-Id", "kunaiu-android")
                    .addHeader("Client-Secret", "b7022a16e0c896575g2d82c33a8h2d9d36781g33")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val dataArray = json.getJSONObject("data").getJSONArray("data")

                withContext(Dispatchers.Main) {
                    val adapter = CommentsAdapter(dataArray, this@MainActivity)
                    commentsRecycler.adapter = adapter
                    statusText.text = "تم تحميل ${dataArray.length()} تعليق."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "خطأ في التعليقات: ${e.message}"
                }
            }
        }
    }

    fun hideComment(commentId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestBody = "comment_id=$commentId".toRequestBody()
                val request = Request.Builder()
                    .url("https://api.kunaiu.com/anime-social/api/posts/comment/hide")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Client-Id", "kunaiu-android")
                    .addHeader("Client-Secret", "b7022a16e0c896575g2d82c33a8h2d9d36781g33")
                    .addHeader("Accept", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "تم إخفاء التعليق", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "فشل الإخفاء", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
