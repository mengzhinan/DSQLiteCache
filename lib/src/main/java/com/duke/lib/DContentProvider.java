package com.duke.lib;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author: duke
 * @datetime: 2018-02-09 14:41
 * @deacription: 管理数据库操作，支持多线程并发、同步特点
 */
public class DContentProvider extends ContentProvider {
    private DSQLiteOpenHelper helper = null;

    @Override
    public boolean onCreate() {
        //初始化数据库工具类
        helper = new DSQLiteOpenHelper(getContext());
        DCache.setContext(getContext());
        //返回true，标记此provider成功初始化
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (getReadableDB() == null) {
            return null;
        }
        return getReadableDB().query(DSQLiteOpenHelper.DTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (getWritableDB() == null) {
            return null;
        }
        long l = getWritableDB().insert(DSQLiteOpenHelper.DTable.TABLE_NAME, null, values);
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (getWritableDB() == null) {
            return 0;
        }
        return getWritableDB().delete(DSQLiteOpenHelper.DTable.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        if (getWritableDB() == null) {
            return 0;
        }
        return getWritableDB().update(DSQLiteOpenHelper.DTable.TABLE_NAME, values, selection, selectionArgs);
    }

    private SQLiteDatabase getReadableDB() {
        if (helper == null) {
            return null;
        }
        return helper.getReadableDatabase();
    }

    private SQLiteDatabase getWritableDB() {
        if (helper == null) {
            return null;
        }
        return helper.getWritableDatabase();
    }
}
