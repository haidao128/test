package com.mobileplatform.creator.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mobileplatform.creator.data.dao.DownloadTaskDao;
import com.mobileplatform.creator.data.db.InstallLogDao;
import com.mobileplatform.creator.data.db.InstallLogEntity;
import com.mobileplatform.creator.data.entity.DownloadTaskEntity;

/**
 * 应用数据库
 * 使用Room框架管理SQLite数据库
 */
@Database(entities = {DownloadTaskEntity.class, InstallLogEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    // 数据库名称
    private static final String DATABASE_NAME = "mobile_platform.db";
    
    // 单例实例
    private static volatile AppDatabase instance;
    
    /**
     * 获取下载任务DAO
     * 
     * @return 下载任务DAO
     */
    public abstract DownloadTaskDao downloadTaskDao();
    
    /**
     * 获取安装日志DAO
     * 
     * @return 安装日志DAO
     */
    public abstract InstallLogDao installLogDao();
    
    /**
     * 获取数据库实例
     * 
     * @param context 上下文
     * @return 数据库实例
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration() // 迁移失败时重建数据库
                            .addMigrations(MIGRATION_1_2) // 添加1到2的迁移规则
                            .build();
                }
            }
        }
        return instance;
    }
    
    /**
     * 数据库从版本1迁移到版本2的迁移规则
     * 添加安装日志表
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 创建安装日志表
            database.execSQL("CREATE TABLE IF NOT EXISTS `install_logs` ("
                    + "`id` TEXT NOT NULL, "
                    + "`appId` TEXT, "
                    + "`appName` TEXT, "
                    + "`packageName` TEXT, "
                    + "`version` TEXT, "
                    + "`operationType` INTEGER NOT NULL DEFAULT 0, "
                    + "`operationTime` INTEGER NOT NULL DEFAULT 0, "
                    + "`success` INTEGER NOT NULL DEFAULT 0, "
                    + "`errorMessage` TEXT, "
                    + "`additionalInfo` TEXT, "
                    + "PRIMARY KEY(`id`))");
        }
    };
} 