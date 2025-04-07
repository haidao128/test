package com.mobileplatform.creator.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mobileplatform.creator.data.model.AppInfo;

import java.util.List;

/**
 * 应用内容提供者，用于在应用间共享应用列表数据
 */
public class AppContentProvider extends ContentProvider {
    private static final String TAG = "AppContentProvider";
    
    // 权限
    private static final String AUTHORITY = "com.mobileplatform.creator.provider";
    
    // 路径
    private static final String PATH_APPS = "apps";
    private static final String PATH_APP = "apps/#";
    
    // URI
    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_APPS);
    
    // 类型
    private static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobileplatform.app";
    private static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobileplatform.app";
    
    // URI匹配器
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int CODE_APPS = 1;
    private static final int CODE_APP = 2;
    
    static {
        URI_MATCHER.addURI(AUTHORITY, PATH_APPS, CODE_APPS);
        URI_MATCHER.addURI(AUTHORITY, PATH_APP, CODE_APP);
    }
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PACKAGE = "package";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_PATH = "path";
    
    @Override
    public boolean onCreate() {
        return true;
    }
    
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int match = URI_MATCHER.match(uri);
        MatrixCursor cursor = null;
        
        switch (match) {
            case CODE_APPS:
                // 查询所有应用
                cursor = getAppsCursor();
                break;
                
            case CODE_APP:
                // 查询单个应用
                String appId = uri.getLastPathSegment();
                cursor = getAppCursor(appId);
                break;
                
            default:
                throw new IllegalArgumentException("未知的URI: " + uri);
        }
        
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        
        return cursor;
    }
    
    /**
     * 获取所有应用的游标
     */
    private MatrixCursor getAppsCursor() {
        MatrixCursor cursor = createCursor();
        
        // 获取应用列表
        AppRepository.getInstance().getInstalledApps(getContext(), (apps, error) -> {
            if (apps != null) {
                addAppsToCursor(cursor, apps);
            }
        });
        
        return cursor;
    }
    
    /**
     * 获取单个应用的游标
     */
    private MatrixCursor getAppCursor(String appId) {
        MatrixCursor cursor = createCursor();
        
        // 获取应用列表
        AppRepository.getInstance().getInstalledApps(getContext(), (apps, error) -> {
            if (apps != null) {
                for (AppInfo app : apps) {
                    if (app.getId().equals(appId)) {
                        addAppToCursor(cursor, app);
                        break;
                    }
                }
            }
        });
        
        return cursor;
    }
    
    /**
     * 创建应用游标
     */
    private MatrixCursor createCursor() {
        return new MatrixCursor(new String[]{
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_PACKAGE,
                COLUMN_VERSION,
                COLUMN_SIZE,
                COLUMN_PATH
        });
    }
    
    /**
     * 添加应用列表到游标
     */
    private void addAppsToCursor(MatrixCursor cursor, List<AppInfo> apps) {
        for (AppInfo app : apps) {
            addAppToCursor(cursor, app);
        }
    }
    
    /**
     * 添加单个应用到游标
     */
    private void addAppToCursor(MatrixCursor cursor, AppInfo app) {
        cursor.addRow(new Object[]{
                app.getId(),
                app.getName(),
                app.getPackageName(),
                app.getVersion(),
                app.getSize(),
                app.getInstallPath()
        });
    }
    
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = URI_MATCHER.match(uri);
        
        switch (match) {
            case CODE_APPS:
                return CONTENT_TYPE;
            case CODE_APP:
                return CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }
    
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Log.d(TAG, "插入操作不支持");
        return null;
    }
    
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Log.d(TAG, "删除操作不支持");
        return 0;
    }
    
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        Log.d(TAG, "更新操作不支持");
        return 0;
    }
} 