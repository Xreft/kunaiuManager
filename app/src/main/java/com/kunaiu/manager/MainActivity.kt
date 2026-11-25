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
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class MainActivity : AppCompatActivity() {
    private lateinit var tokenInput: EditText
    private lateinit var saveTokenBtn: Button
    private lateinit var postsRecycler: RecyclerView
    private lateinit var commentsRecycler: RecyclerView
    private lateinit var statusText: TextView
    private lateinit var hideSelectedBtn: Button
    private lateinit var selectAllBtn: Button

    // ⚠️ متغيرات جديدة
    private var currentPostId: String? = null
    private var currentCommentsAdapter: CommentsAdapter? = null
    private var token: String = ""
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ⚠️ تغيير ألوان النظام (شريط الحالة وشريط التنقل)
        window.statusBarColor = android.graphics.Color.parseColor("#1a1a1a")
        window.navigationBarColor = android.graphics.Color.parseColor("#1a1a1a")

        // ربط المتغيرات
        tokenInput = findViewById(R.id.tokenInput)
        saveTokenBtn = findViewById(R.id.saveTokenBtn)
        postsRecycler = findViewById(R.id.postsRecycler)
        commentsRecycler = findViewById(R.id.commentsRecycler)
        statusText = findViewById(R.id.statusText)
        hideSelectedBtn = findViewById(R.id.hideSelectedBtn)
        selectAllBtn = findViewById(R.id.selectAllBtn)

        // زر "تحديد الكل"
        selectAllBtn.setOnClickListener {
            currentCommentsAdapter?.selectAll()
        }

        // زر "إخفاء المحدد"
        hideSelectedBtn.setOnClickListener {
            val selected = currentCommentsAdapter?.getSelectedComments() ?: emptyList()
            if (selected.isNotEmpty()) {
                hideSelectedComments(selected)
            }
        }

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
        currentPostId = postId // ⚠️ حفظ رقم المنشور الحالي
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
                    currentCommentsAdapter = adapter // ⚠️ حفظ الأداة
                    commentsRecycler.adapter = adapter
                    statusText.text = "تم تحميل ${dataArray.length()} تعليق."
                    
                    // تفعيل أزرار التحديد بعد تحميل التعليقات
                    selectAllBtn.isEnabled = true
                    hideSelectedBtn.isEnabled = false
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
                val requestBody = "comment_id=$commentId".toRequestBody(
                    "application/x-www-form-urlencoded".toMediaTypeOrNull()
                )
                
                val request = Request.Builder()
                    .url("https://api.kunaiu.com/anime-social/api/posts/comment/hide")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Client-Id", "kunaiu-android")
                    .addHeader("Client-Secret", "b7022a16e0c896575g2d82c33a8h2d9d36781g33")
                    .addHeader("Accept", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                
                println("Hide Response: ${response.code} - $responseBody")
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "تم إخفاء التعليق", Toast.LENGTH_SHORT).show()
                        // إعادة تحميل التعليقات بعد الإخفاء
                        currentPostId?.let { loadComments(it) }
                    } else {
                        Toast.makeText(this@MainActivity, "فشل الإخفاء (${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ⚠️ دوال جديدة لتحديث حالة الأزرار وإخفاء المحدد
    fun updateHideButtonState(enabled: Boolean) {
        hideSelectedBtn.isEnabled = enabled
        if (enabled) {
            val count = currentCommentsAdapter?.getSelectedComments()?.size ?: 0
            hideSelectedBtn.text = "🗑️ إخفاء المحدد ($count)"
        } else {
            hideSelectedBtn.text = "🗑️ إخفاء المحدد"
        }
    }

    fun updateSelectAllButtonText(allSelected: Boolean) {
        selectAllBtn.text = if (allSelected) "☑️ إلغاء تحديد الكل" else "☑️ تحديد الكل"
    }

    fun hideSelectedComments(commentIds: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            for (commentId in commentIds) {
                try {
                    val requestBody = "comment_id=$commentId".toRequestBody(
                        "application/x-www-form-urlencoded".toMediaTypeOrNull()
                    )
                    val request = Request.Builder()
                        .url("https://api.kunaiu.com/anime-social/api/posts/comment/hide")
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Client-Id", "kunaiu-android")
                        .addHeader("Client-Secret", "b7022a16e0c896575g2d82c33a8h2d9d36781g33")
                        .addHeader("Accept", "application/json")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) successCount++
                } catch (e: Exception) {
                    // تجاهل الخطأ
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "تم إخفاء $successCount تعليق", Toast.LENGTH_SHORT).show()
                currentPostId?.let { loadComments(it) }
            }
        }
    }
}
