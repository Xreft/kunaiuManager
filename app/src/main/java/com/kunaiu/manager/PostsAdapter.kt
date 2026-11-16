package com.kunaiu.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class PostsAdapter(
    private val posts: JSONArray,
    private val activity: MainActivity
) : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postText: TextView = view.findViewById(R.id.postText)
        val viewCommentsBtn: Button = view.findViewById(R.id.viewCommentsBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts.getJSONObject(position)
        val text = post.optString("post_text", "منشور بدون نص")
        holder.postText.text = text.take(100) + "..."
        val postId = post.optString("post_id")
        holder.viewCommentsBtn.setOnClickListener {
            activity.loadComments(postId)
        }
    }

    override fun getItemCount() = posts.length()
}
