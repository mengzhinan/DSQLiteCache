package com.duke.lib;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;

/**
 * @author: duke
 * @datetime: 2018-02-09 17:39
 * @deacription:
 */
public class DCache {
    private static Context context;
    //如果更改authorities，记得同时更改AndroidManifest.xml中对应的值
    private static String authorities = "baofeng.dcache.com.duke.lib.authorities";
    private static Uri uri;

    static void setContext(Context context) {
        DCache.context = context;
    }

    private static void setUri(String authorities) {
        uri = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + authorities);
    }

    private DCache() {
        setUri(authorities);
    }

    private static class I {
        private static DCache i = new DCache();
    }

    public static DCache getInstance() {
        if (context == null) {
            throw new IllegalArgumentException("context is not allowed to be null");
        }
        return I.i;
    }

    public static DCache getInstance(String authorities) {
        if (context == null) {
            throw new IllegalArgumentException("context is not allowed to be null");
        }
        DCache.authorities = authorities;
        setUri(authorities);
        return I.i;
    }

    public boolean set(String key, byte[] data) {
        return set(key, data, 0);
    }

    public boolean set(String key, byte[] data, long keepTime) {
        if (isEmpty(key) || data == null || data.length == 0) {
            return false;
        }
        if (keepTime < 0) {
            keepTime = 0L;
        }
        DData dData = queryData(key);
        if (dData == null) {
            //新增
            dData = new DData();
            dData.key = key;
            dData.addTime = System.currentTimeMillis();
            dData.keepTime = keepTime;
            dData.data = data;
        } else {
            //更新数据
            dData.data = data;
            if (keepTime > 0) {
                //重新更新保存时间
                dData.addTime = System.currentTimeMillis();
                dData.keepTime = keepTime;
            }
        }
        // TODO: 2018/2/9 添加或者修改  ？？？？？？？
        return true;
    }

    public byte[] get(String key) {
        if (isEmpty(key)) {
            return null;
        }
        DData dData = queryData(key);
        if (dData == null) {
            return null;
        }
        //有时间期限，计算是否过期
        if (dData.keepTime > 0
                && System.currentTimeMillis() > dData.keepTime + dData.addTime) {
            deleteDataByKey(dData.key);
            return null;
        }
        //无时间期限，或未过期
        return dData.data;
    }

    public boolean remove(String key) {
        if (isEmpty(key)) {
            return false;
        }
        return deleteDataByKey(key);
    }

    public boolean removeAll() {
        return deleteAll();
    }

    private boolean deleteAll() {
        int i = context.getContentResolver().delete(uri, null, null);
        return i > 0;
    }

    private void deleteDataById(int id) {
        context.getContentResolver().delete(uri, DSQLiteOpenHelper.DTable.COLUMN_ID + "=" + id, new String[]{String.valueOf(id)});
    }

    private boolean deleteDataByKey(String key) {
        int i = context.getContentResolver().delete(uri, DSQLiteOpenHelper.DTable.COLUMN_C_KEY + "=" + key, new String[]{String.valueOf(key)});
        return i > 0;
    }

    private DData queryData(String key) {
        if (isEmpty(key)) {
            return null;
        }
        String selection = DSQLiteOpenHelper.DTable.COLUMN_C_KEY + " = ?";
        String[] selectionArgs = new String[]{key};
        Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null);
        if (cursor == null) {
            return null;
        }
        ArrayList<DData> list = new ArrayList<>();
        DData dData = null;
        while (cursor.moveToNext()) {
            dData = new DData();
            dData.id = cursor.getInt(cursor.getColumnIndex(DSQLiteOpenHelper.DTable.COLUMN_ID));
            dData.key = cursor.getString(cursor.getColumnIndex(DSQLiteOpenHelper.DTable.COLUMN_C_KEY));
            dData.addTime = cursor.getLong(cursor.getColumnIndex(DSQLiteOpenHelper.DTable.COLUMN_C_ADDTIME));
            dData.keepTime = cursor.getLong(cursor.getColumnIndex(DSQLiteOpenHelper.DTable.COLUMN_C_KEEPTIME));
            dData.data = cursor.getBlob(cursor.getColumnIndex(DSQLiteOpenHelper.DTable.COLUMN_C_DATA));
            list.add(dData);
        }
        cursor.close();
        if (list.size() == 0) {
            list = null;
            return null;
        } else if (list.size() == 1) {
            dData = list.get(0);
            list.clear();
            list = null;
            return dData;
        } else {
            dData = list.get(0);
            int size = list.size();
            for (int i = 1; i < size; i++) {
                deleteDataById(list.get(i).id);
            }
            list.clear();
            list = null;
            return dData;
        }
    }

    private boolean isEmpty(String key) {
        return key == null || "".equals(key.trim()) || key.trim().length() == 0;
    }

    private class DData {
        int id;
        String key;//key字符串
        long addTime;//添加时间(毫秒)
        long keepTime;//有效保留时间(毫秒)
        byte[] data;//二进制数据
    }
}
