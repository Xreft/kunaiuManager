package com.kunaiu.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class CommentsAdapter(
    private val comments: JSONArray,
    private val activity: MainActivity
) : RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commentText: TextView = view.findViewById(R.id.commentText)
        val hideBtn: Button = view.findViewById(R.id.hideBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments.getJSONObject(position)
        val text = comment.optString("comment_text", "تعليق بدون نص")
        holder.commentText.text = text
        val commentId = comment.optString("comment_id")
        holder.hideBtn.setOnClickListener {
            activity.hideComment(commentId)
        }
    }

    override fun getItemCount() = comments.length()
}
