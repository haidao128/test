package com.mobileplatform.creator.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile CategoryDao _categoryDao;

  private volatile LogEntryDao _logEntryDao;

  private volatile AppCategoryDao _appCategoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT, `description` TEXT, `app_count` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `update_time` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `log_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `app_name` TEXT, `package_name` TEXT, `operation_type` TEXT, `status` TEXT, `details` TEXT, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `app_categories` (`package_name` TEXT NOT NULL, `category_id` TEXT NOT NULL, `add_time` INTEGER NOT NULL, PRIMARY KEY(`package_name`, `category_id`), FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_categories_package_name` ON `app_categories` (`package_name`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_categories_category_id` ON `app_categories` (`category_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0c8e36c59e1e1f0bbca3d2e98cd9c6fd')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `log_entries`");
        db.execSQL("DROP TABLE IF EXISTS `app_categories`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(6);
        _columnsCategories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("app_count", new TableInfo.Column("app_count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("create_time", new TableInfo.Column("create_time", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("update_time", new TableInfo.Column("update_time", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.mobileplatform.creator.model.Category).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsLogEntries = new HashMap<String, TableInfo.Column>(7);
        _columnsLogEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("app_name", new TableInfo.Column("app_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("package_name", new TableInfo.Column("package_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("operation_type", new TableInfo.Column("operation_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("status", new TableInfo.Column("status", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("details", new TableInfo.Column("details", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLogEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLogEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLogEntries = new TableInfo("log_entries", _columnsLogEntries, _foreignKeysLogEntries, _indicesLogEntries);
        final TableInfo _existingLogEntries = TableInfo.read(db, "log_entries");
        if (!_infoLogEntries.equals(_existingLogEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "log_entries(com.mobileplatform.creator.model.LogEntry).\n"
                  + " Expected:\n" + _infoLogEntries + "\n"
                  + " Found:\n" + _existingLogEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsAppCategories = new HashMap<String, TableInfo.Column>(3);
        _columnsAppCategories.put("package_name", new TableInfo.Column("package_name", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppCategories.put("category_id", new TableInfo.Column("category_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppCategories.put("add_time", new TableInfo.Column("add_time", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppCategories = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAppCategories.add(new TableInfo.ForeignKey("categories", "CASCADE", "NO ACTION", Arrays.asList("category_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAppCategories = new HashSet<TableInfo.Index>(2);
        _indicesAppCategories.add(new TableInfo.Index("index_app_categories_package_name", false, Arrays.asList("package_name"), Arrays.asList("ASC")));
        _indicesAppCategories.add(new TableInfo.Index("index_app_categories_category_id", false, Arrays.asList("category_id"), Arrays.asList("ASC")));
        final TableInfo _infoAppCategories = new TableInfo("app_categories", _columnsAppCategories, _foreignKeysAppCategories, _indicesAppCategories);
        final TableInfo _existingAppCategories = TableInfo.read(db, "app_categories");
        if (!_infoAppCategories.equals(_existingAppCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "app_categories(com.mobileplatform.creator.model.AppCategory).\n"
                  + " Expected:\n" + _infoAppCategories + "\n"
                  + " Found:\n" + _existingAppCategories);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "0c8e36c59e1e1f0bbca3d2e98cd9c6fd", "85cc0a7ad9139cd6ee613930c584997e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "categories","log_entries","app_categories");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `log_entries`");
      _db.execSQL("DELETE FROM `app_categories`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LogEntryDao.class, LogEntryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AppCategoryDao.class, AppCategoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public LogEntryDao logEntryDao() {
    if (_logEntryDao != null) {
      return _logEntryDao;
    } else {
      synchronized(this) {
        if(_logEntryDao == null) {
          _logEntryDao = new LogEntryDao_Impl(this);
        }
        return _logEntryDao;
      }
    }
  }

  @Override
  public AppCategoryDao appCategoryDao() {
    if (_appCategoryDao != null) {
      return _appCategoryDao;
    } else {
      synchronized(this) {
        if(_appCategoryDao == null) {
          _appCategoryDao = new AppCategoryDao_Impl(this);
        }
        return _appCategoryDao;
      }
    }
  }
}
