package com.duke.lib;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author: duke
 * @datetime: 2018-02-09 14:39
 * @deacription:
 */
public class DSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String dbName = "DCache.db";
    private static final int dbVersion = 1;

    public DSQLiteOpenHelper(Context context) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DTable.createTableSQL());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalArgumentException("not need upgrade");
    }

    /**
     * @author: duke
     * @datetime: 2018-02-09 15:08
     * @deacription: 表结构
     */
    public static class DTable {
        public static final String TABLE_NAME = "tblDCache";//表名
        public static final String COLUMN_ID = "_id";//id，自动增长
        public static final String COLUMN_C_KEY = "cKey";//数据记录key,唯一键
        public static final String COLUMN_C_ADDTIME = "cAddTime";//数据记录添加时间(毫秒)
        public static final String COLUMN_C_KEEPTIME = "cKeepTime";//数据记录保存期限时间(毫秒)
        public static final String COLUMN_C_DATA = "cData";//数据记录内容(二进制形式)

        public static String createTableSQL() {
            return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_C_KEY + " VARCHAR UNIQUE, "
                    + COLUMN_C_ADDTIME + " INTEGER, "
                    + COLUMN_C_KEEPTIME + " INTEGER, "
                    + COLUMN_C_DATA + " BLOB)";
        }

        public static String alterTableSQL() {
            return "ALTER TABLE " + TABLE_NAME + " ADD COLUMN xxx varchar";
        }
    }
}
