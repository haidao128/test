package com.mobileplatform.creator.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mobileplatform.creator.model.AppCategory;
import com.mobileplatform.creator.model.Category;
import com.mobileplatform.creator.model.LogEntry;
import com.mobileplatform.creator.util.Converters;

// TODO: 添加其他实体类到 entities 数组中，例如 AppInfo.class, Category.class
// TODO: 增加数据库版本号 (version) 当你修改了表结构时

/**
 * 应用的 Room 数据库主类。
 * 定义了数据库包含的表以及提供了 DAO 的访问方法。
 */
@Database(entities = {Category.class, LogEntry.class, AppCategory.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CategoryDao categoryDao();
    public abstract LogEntryDao logEntryDao();
    public abstract AppCategoryDao appCategoryDao();
    // TODO: 添加其他 DAO 的抽象方法，例如 appInfoDao(), categoryDao()

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "app_database")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration() // 临时：如果迁移失败，销毁并重建数据库（会丢失数据！）
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 数据库从版本1迁移到版本2的迁移规则
     * 添加应用与分类关联表
     */
    static final androidx.room.migration.Migration MIGRATION_1_2 = new androidx.room.migration.Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 创建应用与分类关联表
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_categories` (" +
                "`package_name` TEXT NOT NULL, " +
                "`category_id` TEXT NOT NULL, " +
                "`add_time` INTEGER NOT NULL, " +
                "PRIMARY KEY(`package_name`, `category_id`), " +
                "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON DELETE CASCADE " +
                ")"
            );
            
            // 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_app_categories_package_name` ON `app_categories` (`package_name`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_app_categories_category_id` ON `app_categories` (`category_id`)");
        }
    };
} 