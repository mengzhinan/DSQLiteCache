package com.duke.lib;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author: duke
 * @datetime: 2018-02-09 17:39
 * @deacription:
 */
public class DCache {

    public static final int TIME_MINUTE = 60;
    public static final int TIME_HOUR = TIME_MINUTE * 60;
    public static final int TIME_DAY = TIME_HOUR * 24;

    //如果更改authorities，记得同时更改AndroidManifest.xml中对应的值
    private static String authorities = "baofeng.dcache.com.duke.lib.authorities";
    private static Uri uri;

    private static Context context;

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

    public static DCache get() {
        if (context == null) {
            throw new IllegalArgumentException("context is not allowed to be null");
        }
        return I.i;
    }

    public static DCache get(String authorities) {
        if (context == null) {
            throw new IllegalArgumentException("context is not allowed to be null");
        }
        DCache.authorities = authorities;
        setUri(authorities);
        return I.i;
    }

    private boolean deleteAll() {
        int i = context.getContentResolver().delete(uri, null, null);
        return i > 0;
    }

    private void deleteDataById(int id) {
        context.getContentResolver().delete(uri, DSQLiteOpenHelper.DTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    private boolean deleteDataByKey(String key) {
        int i = context.getContentResolver().delete(uri,
                DSQLiteOpenHelper.DTable.COLUMN_C_KEY + " = ?",
                new String[]{String.valueOf(key)});
        return i > 0;
    }

    private DData queryData(String key) {
        if (DUtils.isEmpty(key)) {
            return null;
        }
        String selection = DSQLiteOpenHelper.DTable.COLUMN_C_KEY + " = ?";
        String[] selectionArgs = new String[]{key};
        Cursor cursor = context.getContentResolver().query(uri, null,
                selection, selectionArgs, null);
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

    private void saveOrUpdate(DData dData) {
        if (dData == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DSQLiteOpenHelper.DTable.COLUMN_C_KEY, dData.key);
        values.put(DSQLiteOpenHelper.DTable.COLUMN_C_ADDTIME, dData.addTime);
        values.put(DSQLiteOpenHelper.DTable.COLUMN_C_KEEPTIME, dData.keepTime);
        values.put(DSQLiteOpenHelper.DTable.COLUMN_C_DATA, dData.data);
        if (dData.id > 0) {
            //修改
            values.put(DSQLiteOpenHelper.DTable.COLUMN_ID, dData.id);
            context.getContentResolver().update(uri, values,
                    DSQLiteOpenHelper.DTable.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(dData.id)});
        } else {
            //添加
            context.getContentResolver().insert(uri, values);
        }
    }

    private class DData {
        int id;
        String key;//key字符串
        long addTime;//添加时间(毫秒)
        long keepTime;//有效保留时间(毫秒)
        byte[] data;//二进制数据
    }

    //==================以下是对外方法=====================

    public boolean remove(String key) {
        if (DUtils.isEmpty(key)) {
            return false;
        }
        return deleteDataByKey(key);
    }

    public boolean removeAll() {
        return deleteAll();
    }

    // =======================================
    // ============== byte 数据 读写 =========
    // =======================================

    /**
     * 保存 byte数据 到 缓存中
     *
     * @param key  保存的key
     * @param data 保存的数据
     */
    public boolean put(String key, byte[] data) {
        return put(key, data, 0L);
    }

    /**
     * 保存 byte数据 到 缓存中
     *
     * @param key      保存的key
     * @param data     保存的数据
     * @param keepTime 保存数据的有效期(毫秒)
     */
    public boolean put(String key, byte[] data, long keepTime) {
        if (DUtils.isEmpty(key) || data == null || data.length == 0) {
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
        saveOrUpdate(dData);
        return true;
    }

    /**
     * 获取 byte 数据
     *
     * @param key
     * @return byte 数据
     */
    public byte[] getAsBinary(String key) {
        if (DUtils.isEmpty(key)) {
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

    // =======================================
    // ============ String数据 读写 ==========
    // =======================================

    /**
     * 保存 String数据 到 缓存中
     *
     * @param key  保存的key
     * @param data 保存的String数据
     */
    public boolean put(String key, String data) {
        return put(key, data, 0L);
    }

    /**
     * 保存 String数据 到 缓存中
     *
     * @param key      保存的key
     * @param data     保存的String数据
     * @param keepTime 保存数据的有效期(毫秒)
     */
    public boolean put(String key, String data, long keepTime) {
        if (DUtils.isEmpty(data)) {
            return false;
        }
        return put(key, data.getBytes(), keepTime);
    }

    /**
     * 读取 String数据
     *
     * @param key
     * @return String 数据
     */
    public String getAsString(String key) {
        byte[] bytes = getAsBinary(key);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new String(bytes);
    }

    // =======================================
    // ============= JSONObject 数据 读写 ====
    // =======================================

    /**
     * 保存 JSONObject数据 到 缓存中
     *
     * @param key  保存的key
     * @param data 保存的JSON数据
     */
    public boolean put(String key, JSONObject data) {
        return put(key, data, 0L);
    }

    /**
     * 保存 JSONObject数据 到 缓存中
     *
     * @param key      保存的key
     * @param data     保存的JSONObject数据
     * @param keepTime 保存数据的有效期(毫秒)
     */
    public boolean put(String key, JSONObject data, long keepTime) {
        if (data == null) {
            return false;
        }
        return put(key, data.toString(), keepTime);
    }

    /**
     * 读取JSONObject数据
     *
     * @param key
     * @return JSONObject数据
     */
    public JSONObject getAsJSONObject(String key) throws JSONException {
        String json = getAsString(key);
        if (DUtils.isEmpty(json)) {
            return null;
        }
        return new JSONObject(json);
    }

    // =======================================
    // ============ JSONArray 数据 读写 ======
    // =======================================

    /**
     * 保存 JSONArray数据 到 缓存中
     *
     * @param key  保存的key
     * @param data 保存的JSONArray数据
     */
    public boolean put(String key, JSONArray data) {
        return put(key, data, 0L);
    }

    /**
     * 保存 JSONArray数据 到 缓存中
     *
     * @param key      保存的key
     * @param data     保存的JSONArray数据
     * @param keepTime 保存数据的有效期(毫秒)
     */
    public boolean put(String key, JSONArray data, long keepTime) {
        if (data == null || data.length() == 0) {
            return false;
        }
        return put(key, data.toString(), keepTime);
    }

    /**
     * 读取JSONArray数据
     *
     * @param key
     * @return JSONArray数据
     */
    public JSONArray getAsJSONArray(String key) throws JSONException {
        String jsonArr = getAsString(key);
        if (DUtils.isEmpty(jsonArr)) {
            return null;
        }
        return new JSONArray(jsonArr);
    }

    // =======================================
    // ============= 序列化 数据 读写 ========
    // =======================================

    /**
     * 保存 Serializable数据 到 缓存中
     *
     * @param key  保存的key
     * @param data 保存的value
     */
    public boolean put(String key, Serializable data) {
        return put(key, data, 0L);
    }

    /**
     * 保存 Serializable数据到 缓存中
     *
     * @param key      保存的key
     * @param data     保存的value
     * @param keepTime 保存数据的有效期(毫秒)
     */
    public boolean put(String key, Serializable data, long keepTime) {
        if (data == null) {
            return false;
        }
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(data);
            return put(key, baos.toByteArray(), keepTime);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 读取 Serializable数据
     *
     * @param key
     * @return Serializable 数据
     */
    public Object getAsObject(String key) {
        byte[] data = getAsBinary(key);
        if (data == null) {
            return null;
        }
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            bais = new ByteArrayInputStream(data);
            ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (bais != null)
                    bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (ois != null)
                    ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // =======================================
    // ============== bitmap 数据 读写 =======
    // =======================================

    /**
     * 保存 bitmap 到 缓存中
     *
     * @param key  保存的key
     * @param data 保存的bitmap数据
     */
    public boolean put(String key, Bitmap data) {
        return put(key, data, 0L);
    }

    /**
     * 保存 bitmap 到 缓存中
     *
     * @param key      保存的key
     * @param data     保存的 bitmap 数据
     * @param keepTime 保存数据的有效期(毫秒)
     */
    public boolean put(String key, Bitmap data, long keepTime) {
        return put(key, DUtils.Bitmap2ByteArray(data), keepTime);
    }

    /**
     * 读取 bitmap 数据
     *
     * @param key
     * @return bitmap 数据
     */
    public Bitmap getAsBitmap(String key) {
        byte[] bytes = getAsBinary(key);
        if (bytes == null) {
            return null;
        }
        return DUtils.ByteArray2Bimap(bytes);
    }

    // =======================================
    // ============= drawable 数据 读写 ======
    // =======================================

    /**
     * 保存 drawable 到 缓存中
     *
     * @param key  保存的key
     * @param data 保存的drawable数据
     */
    public boolean put(String key, Drawable data) {
        return put(key, data, 0L);
    }

    /**
     * 保存 drawable 到 缓存中
     *
     * @param key      保存的key
     * @param data     保存的 drawable 数据
     * @param keepTime 保存数据的有效期(毫秒)
     */
    public boolean put(String key, Drawable data, long keepTime) {
        return put(key, DUtils.drawable2Bitmap(data), keepTime);
    }

    /**
     * 读取 Drawable 数据
     *
     * @param key
     * @return Drawable 数据
     */
    public Drawable getAsDrawable(String key) {
        byte[] bytes = getAsBinary(key);
        if (bytes == null) {
            return null;
        }
        return DUtils.bitmap2Drawable(DUtils.ByteArray2Bimap(bytes));
    }

    //=========================================
    //===hashCode==============================
    //=========================================

    public String keyHashCode(Object object) {
        if (object == null) {
            return null;
        }
        return String.valueOf(object.hashCode());
    }
}
