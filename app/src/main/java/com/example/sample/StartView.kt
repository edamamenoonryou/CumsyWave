package com.example.sample

//import android.telecom.Call
//import kotlinx.coroutines.NonCancellable.message
//import okhttp3.RequestBody.Companion.asRequestBody
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sample.api.ClumsyResult
import com.example.sample.api.FlaskApiService
import com.example.sample.api.MailRequest
import com.example.sample.api.MailResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import kotlin.math.floor
import kotlin.math.sqrt


class StartView : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE_AUDIO  = 100
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_view)

        resultTextView = findViewById(R.id.textView5)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestAudioPermission()
        val button: Button = findViewById(R.id.button_title)
        button.setOnClickListener{
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.textView6).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.editTextText).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.editTextText2).visibility = View.INVISIBLE
        findViewById<Button>(R.id.button3).visibility = View.INVISIBLE
        findViewById<CheckBox>(R.id.checkBox).visibility = View.INVISIBLE
    }

    private fun requestAudioPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // API33以上の場合はREAD_MEDIA_AUDIOパーミッションをリクエスト
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO), PERMISSION_REQUEST_CODE_AUDIO)
            } else {
                openFileDialog()
            }
        } else {
            // API33未満の場合はREAD_EXTERNAL_STORAGEパーミッションをリクエスト
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE_AUDIO)
            } else {
                openFileDialog()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE_AUDIO  && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFileDialog()
        } else {
            Toast.makeText(this, "パーミッションが拒否されました。", Toast.LENGTH_SHORT).show()
        }
    }

    // ファイル選択ダイアログ
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // ファイルが選択された場合の処理（Pythonスクリプトに渡すなど）
            handleFile(it)
        }
    }

    private fun openFileDialog() {
        // ファイル選択ダイアログを表示
        filePickerLauncher.launch("audio/*")
    }

    private fun handleFile(uri: Uri) {
        // 選択されたファイルを処理してFlaskサーバーに送信
        try {
            // UriからInputStreamを取得
            val inputStream = contentResolver.openInputStream(uri)

            // 一時ファイルを作成しデータを保存
            val tempFile = File(cacheDir, "tempFile")
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 一時ファイルからrequestBodyを作成
            val requestBody = tempFile.asRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

            // retrofitでリクエストを作成
            val retrofit = Retrofit.Builder()
                .baseUrl("http://172.20.10.2:5000/") // FlaskサーバーのURL
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(FlaskApiService::class.java)
            val call = service.uploadFile(body)

            call.enqueue(object : Callback<ClumsyResult> {
                override fun onResponse(call: retrofit2.Call<ClumsyResult>, response: retrofit2.Response<ClumsyResult>) {
                    if (response.isSuccessful) {
                        val responseText = response.body()
                        responseText?.let {
                            val mfcc = it.mfcc
                            println("MFCC: $mfcc")

                            checkCosineSimilarity(mfcc)
                        }
                    } else {
                        resultTextView.text = "声紋処理中にエラーが発生しました。もう一度試して下さい。"
                    }
                }
                override fun onFailure(call: retrofit2.Call<ClumsyResult>, t: Throwable) {
                    resultTextView.text = "送信に失敗しました。もう一度試して下さい。"
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            resultTextView.text = "ファイル処理中にエラーが発生しました。もう一度試して下さい。"
        }
    }

    fun checkCosineSimilarity(newMfcc: List<Float>) {
        val dbHelper = DatabaseHelper(this)
        val cursor = dbHelper.getAllData()

        var foundMatch = false
        var maxCos = 0.0
        var maxTarget: String? = null
        var maxMail: String? = null
        var blackFlag = false

        while (cursor.moveToNext()) {
            // 声紋リストからMFCCを取得
            val mfccValues = listOf(
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC1)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC2)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC3)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC4)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC5)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC6)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC7)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC8)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC9)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC10)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC11)),
                cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_MFCC12))
            )

            // コサイン類似度を計算
            val similarity = calculateCosineSimilarity(mfccValues, newMfcc)

            // 類似度が0.9975以上の場合一致判定
            if (similarity > 0.9975) {
                foundMatch = true
                val name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME))
                val email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))
                val check = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_BLACK))

                // 類似度の最大値を更新
                if (similarity > maxCos) {
                    blackFlag = check == 1
                    maxCos = similarity.toDouble()
                    maxTarget = name
                    maxMail = email
                }
            }
        }
        maxCos *= 100
        maxCos = floor(maxCos * 100) / 100
        if (foundMatch and !blackFlag) {
            resultTextView.text = "声紋が($maxTarget)さんと($maxCos)%一致しました。"

            val sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE)
            val name = sharedPreferences.getString("name", "誰か")

            val mailRetrofit = Retrofit.Builder()
                .baseUrl("http://172.20.10.2:5000/mail/")  // flaskサーバーのURL
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val mailService = mailRetrofit.create(FlaskApiService::class.java)

            val addRequestBody = MailRequest(name ?: "", maxMail ?: "")
            val mailCall = mailService.sendMail(addRequestBody)

            mailCall.enqueue(object : Callback<MailResult> {
                override fun onResponse(call: retrofit2.Call<MailResult>, response: retrofit2.Response<MailResult>) {
                    if (response.isSuccessful) {
                        println("メール送信成功")
                    } else {
                        println("サーバーエラー: ${response.code()}")
                    }
                }
                override fun onFailure(call: retrofit2.Call<MailResult>, t: Throwable) {
                    // エラー処理
                    println("通信エラー: ${t.message}")
                }
            })


        }else if (blackFlag) {
            resultTextView.text = "ブラックリストの声紋と一致しました。"
        }

        else {
            resultTextView.text = "一致する声紋が見つかりませんでした。"

            val editTextText: EditText = findViewById(R.id.editTextText)
            val editTextText2: EditText = findViewById(R.id.editTextText2)
            val button3: Button = findViewById(R.id.button3)
            val blackCheck = findViewById<CheckBox>(R.id.checkBox)
            findViewById<TextView>(R.id.textView6).visibility = View.VISIBLE
            editTextText.visibility = View.VISIBLE
            editTextText2.visibility = View.VISIBLE
            button3.visibility = View.VISIBLE
            blackCheck.visibility = View.VISIBLE

            val sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE)
            val mailName = sharedPreferences.getString("name", "誰か")
            val mailAddress = sharedPreferences.getString("address", "誰か")

            println("$mailAddress, $mailName")
            val familyRetrofit = Retrofit.Builder()
                .baseUrl("http://172.20.10.2:5000/family/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val mailService = familyRetrofit.create(FlaskApiService::class.java)

            val addRequestBody = MailRequest(mailName ?: "", mailAddress ?: "")
            val mailCall = mailService.sendFamily(addRequestBody)

            mailCall.enqueue(object : Callback<MailResult> {
                override fun onResponse(call: retrofit2.Call<MailResult>, response: retrofit2.Response<MailResult>) {
                    if (response.isSuccessful) {
                        println("メール送信成功")
                    } else {
                        println("サーバーエラー: ${response.code()}")
                    }
                }
                override fun onFailure(call: retrofit2.Call<MailResult>, t: Throwable) {
                    println("通信エラー: ${t.message}")
                }
            })

            // 追加ボタンが押されたらaddNewMfccを実行
            button3.setOnClickListener {
                val name = editTextText.text.toString()
                val address = editTextText2.text.toString()
                val isChecked = blackCheck.isChecked
                val check: Int = if (isChecked) 1 else 0
                addNewMfcc(newMfcc, name, address, check)
            }
        }
    }
    // コサイン類似度を計算
    private fun calculateCosineSimilarity(mfcc1: List<Float>, mfcc2: List<Float>): Float {
        val dotProduct = mfcc1.zip(mfcc2).sumOf { (x1, x2) -> (x1 * x2).toDouble() }
        val magnitude1 = sqrt(mfcc1.sumOf { (it * it).toDouble() })
        val magnitude2 = sqrt(mfcc2.sumOf { (it * it).toDouble() })
        return (dotProduct / (magnitude1 * magnitude2)).toFloat()
    }
    private fun addNewMfcc(newMfcc: List<Float>, name: String, email: String, check: Int) {
        val dbHelper = DatabaseHelper(this)
        // 声紋を追加
        val success = dbHelper.insertData(newMfcc, name, email, check)
        if (success) {
            resultTextView.text = "新しい声紋をデータベースに追加しました。"
        } else {
            resultTextView.text = "新しい声紋の追加に失敗しました。"
        }
    }
}

