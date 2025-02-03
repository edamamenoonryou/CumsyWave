package com.example.sample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(val context: Context): RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    private val dbHelper = DatabaseHelper(context)
    private val cursor = dbHelper.getAllData()

    private  val idList = mutableListOf<Int>()
    private val nameList = mutableListOf<String>()
    private val mailList = mutableListOf<String>()

    init {
        while (cursor.moveToNext()) {
            idList.add(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)))
            nameList.add(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)))
            mailList.add(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL)))
            println(nameList)
        }
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.textView9)
        val addressTextView: TextView = view.findViewById(R.id.textView10)
        val deleteButton: Button = view.findViewById(R.id.button_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.mfcc_layout_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.nameTextView.text = nameList[position]
        viewHolder.addressTextView.text = mailList[position]

        viewHolder.deleteButton.setOnClickListener {
            val id = idList[position]
            val success = dbHelper.deleteData(id)
            if (success) {
                idList.removeAt(position)
                nameList.removeAt(position)
                mailList.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    override fun getItemCount() = nameList.size
}