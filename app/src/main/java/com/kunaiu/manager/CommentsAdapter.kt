package com.kunaiu.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class CommentsAdapter(
    private val comments: JSONArray,
    private val activity: MainActivity
) : RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {

    // قائمة لتخزين التعليقات المحددة (comment_id)
    private val selectedComments = mutableSetOf<String>()
    private var allSelected = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commentText: TextView = view.findViewById(R.id.commentText)
        val hideBtn: Button = view.findViewById(R.id.hideBtn)
        val checkBox: CheckBox = view.findViewById(R.id.commentCheckBox)
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

        // إعداد الـ CheckBox
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedComments.contains(commentId)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedComments.add(commentId)
            } else {
                selectedComments.remove(commentId)
            }
            activity.updateHideButtonState(selectedComments.size > 0)
        }

        // إخفاء التعليق (بشكل فردي)
        holder.hideBtn.setOnClickListener {
            activity.hideComment(commentId)
        }
    }

    override fun getItemCount() = comments.length()

    // دالة لتحديد كل التعليقات
    fun selectAll() {
        if (allSelected) {
            selectedComments.clear()
            allSelected = false
            activity.updateSelectAllButtonText(false)
        } else {
            for (i in 0 until comments.length()) {
                selectedComments.add(comments.getJSONObject(i).optString("comment_id"))
            }
            allSelected = true
            activity.updateSelectAllButtonText(true)
        }
        notifyDataSetChanged()
        activity.updateHideButtonState(selectedComments.size > 0)
    }

    // دالة لإخفاء التعليقات المحددة
    fun getSelectedComments(): List<String> {
        return selectedComments.toList()
    }

    // دالة لمسح التحديد
    fun clearSelection() {
        selectedComments.clear()
        allSelected = false
        notifyDataSetChanged()
        activity.updateHideButtonState(false)
    }
}
