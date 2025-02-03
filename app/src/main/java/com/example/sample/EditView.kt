package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EditView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //RecyclerViewの取得
        val recyclerView = findViewById<RecyclerView>(R.id.rvList)

        //Adapterの設定
        val adapter = ItemAdapter(this)
        recyclerView.adapter = adapter

        //LayoutManagerの設定
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val button: Button = findViewById(R.id.button_title)
        button.setOnClickListener{
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }

        val button2: Button = findViewById(R.id.button_change)
        button2.setOnClickListener{
            val editTextText: EditText = findViewById(R.id.editTextText5)
            val editTextText2: EditText = findViewById(R.id.editTextText4)

            val name = editTextText.text.toString()
            val address = editTextText2.text.toString()

            val sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("name", name)
            editor.putString("address", address)
            editor.apply()
            Toast.makeText(this, "ユーザー情報が登録されました。", Toast.LENGTH_SHORT).show()
        }
    }
}