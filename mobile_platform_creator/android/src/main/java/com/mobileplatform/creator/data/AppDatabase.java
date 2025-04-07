package com.mobileplatform.creator.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mobileplatform.creator.data.dao.LogDao;
import com.mobileplatform.creator.data.entity.LogEntry;

// TODO: 添加其他实体类到 entities 数组中，例如 AppInfo.class, Category.class
// TODO: 增加数据库版本号 (version) 当你修改了表结构时

/**
 * 应用的 Room 数据库主类。
 * 定义了数据库包含的表以及提供了 DAO 的访问方法。
 */
@Database(entities = {LogEntry.class /*, AppInfo.class, Category.class */ }, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract LogDao logDao();
    // TODO: 添加其他 DAO 的抽象方法，例如 appInfoDao(), categoryDao()

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "mobile_platform_db")
                            // TODO: 添加数据库迁移策略 (Migrations) 如果需要的话
                            // .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration() // 临时：如果迁移失败，销毁并重建数据库（会丢失数据！）
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // TODO: 定义数据库迁移 (Migration) 对象
    /*
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 执行数据库结构变更的 SQL 语句
            database.execSQL("ALTER TABLE install_logs ADD COLUMN user_id TEXT");
        }
    };
    */
} 