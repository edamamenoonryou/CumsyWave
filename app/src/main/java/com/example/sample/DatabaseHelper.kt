package com.example.sample

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "ClumsyWave.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "mfccList"
        const val COLUMN_ID = "id"
        const val COLUMN_MFCC1 = "mfcc1"
        const val COLUMN_MFCC2 = "mfcc2"
        const val COLUMN_MFCC3 = "mfcc3"
        const val COLUMN_MFCC4 = "mfcc4"
        const val COLUMN_MFCC5 = "mfcc5"
        const val COLUMN_MFCC6 = "mfcc6"
        const val COLUMN_MFCC7 = "mfcc7"
        const val COLUMN_MFCC8 = "mfcc8"
        const val COLUMN_MFCC9 = "mfcc9"
        const val COLUMN_MFCC10 = "mfcc10"
        const val COLUMN_MFCC11 = "mfcc11"
        const val COLUMN_MFCC12 = "mfcc12"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_BLACK = "black"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_MFCC1 REAL, $COLUMN_MFCC2 REAL, $COLUMN_MFCC3 REAL, $COLUMN_MFCC4 REAL, " +
                "$COLUMN_MFCC5 REAL, $COLUMN_MFCC6 REAL, $COLUMN_MFCC7 REAL, $COLUMN_MFCC8 REAL, " +
                "$COLUMN_MFCC9 REAL, $COLUMN_MFCC10 REAL, $COLUMN_MFCC11 REAL, $COLUMN_MFCC12 REAL, " +
                "$COLUMN_NAME TEXT, $COLUMN_EMAIL TEXT, $COLUMN_BLACK INTEGER DEFAULT 0)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // データの挿入
    fun insertData(mfcc: List<Float>, name: String, email: String, check: Int): Boolean {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_MFCC1, mfcc[0])
            put(COLUMN_MFCC2, mfcc[1])
            put(COLUMN_MFCC3, mfcc[2])
            put(COLUMN_MFCC4, mfcc[3])
            put(COLUMN_MFCC5, mfcc[4])
            put(COLUMN_MFCC6, mfcc[5])
            put(COLUMN_MFCC7, mfcc[6])
            put(COLUMN_MFCC8, mfcc[7])
            put(COLUMN_MFCC9, mfcc[8])
            put(COLUMN_MFCC10, mfcc[9])
            put(COLUMN_MFCC11, mfcc[10])
            put(COLUMN_MFCC12, mfcc[11])
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_BLACK, check)
        }

        val result = db.insert(TABLE_NAME, null, contentValues)
        return result != -1L
    }

    // すべてのデータを取得
    fun getAllData(): Cursor {
        val db = readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    fun deleteData(id: Int): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return result > 0
    }
}
