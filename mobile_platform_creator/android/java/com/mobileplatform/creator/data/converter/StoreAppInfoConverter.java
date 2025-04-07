package com.mobileplatform.creator.data.converter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mobileplatform.creator.data.model.StoreAppInfo;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;

/**
 * StoreAppInfo对象与JSON字符串之间的转换器
 * 用于Room数据库存储
 */
public class StoreAppInfoConverter {
    private static final String TAG = "StoreAppInfoConverter";
    
    // 创建自定义的Gson实例，用于处理Bitmap的序列化和反序列化
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Bitmap.class, new BitmapTypeAdapter())
            .create();
    
    /**
     * 将StoreAppInfo对象转换为JSON字符串
     * 
     * @param appInfo StoreAppInfo对象
     * @return JSON字符串
     */
    @TypeConverter
    public static String fromStoreAppInfo(StoreAppInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        
        try {
            return gson.toJson(appInfo);
        } catch (Exception e) {
            Log.e(TAG, "序列化StoreAppInfo失败", e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为StoreAppInfo对象
     * 
     * @param json JSON字符串
     * @return StoreAppInfo对象
     */
    @TypeConverter
    public static StoreAppInfo toStoreAppInfo(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return gson.fromJson(json, StoreAppInfo.class);
        } catch (Exception e) {
            Log.e(TAG, "反序列化StoreAppInfo失败", e);
            return null;
        }
    }
    
    /**
     * Bitmap类型适配器
     * 处理Bitmap在JSON中的序列化和反序列化
     */
    private static class BitmapTypeAdapter implements JsonSerializer<Bitmap>, JsonDeserializer<Bitmap> {
        
        @Override
        public JsonElement serialize(Bitmap bitmap, Type typeOfSrc, JsonSerializationContext context) {
            if (bitmap == null) {
                return null;
            }
            
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                return new JsonPrimitive(base64);
            } catch (Exception e) {
                Log.e(TAG, "序列化Bitmap失败", e);
                return null;
            }
        }
        
        @Override
        public Bitmap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            
            try {
                String base64 = json.getAsString();
                byte[] byteArray = Base64.decode(base64, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            } catch (Exception e) {
                Log.e(TAG, "反序列化Bitmap失败", e);
                return null;
            }
        }
    }
} 