package com.mobileplatform.creator.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.mobileplatform.creator.data.entity.LogEntry;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class LogDao_Impl implements LogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LogEntry> __insertionAdapterOfLogEntry;

  private final EntityDeletionOrUpdateAdapter<LogEntry> __deletionAdapterOfLogEntry;

  private final EntityDeletionOrUpdateAdapter<LogEntry> __updateAdapterOfLogEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllLogs;

  public LogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLogEntry = new EntityInsertionAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `install_logs` (`id`,`packageName`,`appName`,`versionName`,`versionCode`,`timestamp`,`operationType`,`status`,`details`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final LogEntry entity) {
        statement.bindLong(1, entity.id);
        if (entity.packageName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.packageName);
        }
        if (entity.appName == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.appName);
        }
        if (entity.versionName == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.versionName);
        }
        statement.bindLong(5, entity.versionCode);
        statement.bindLong(6, entity.timestamp);
        if (entity.operationType == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.operationType);
        }
        if (entity.status == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.status);
        }
        if (entity.details == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.details);
        }
      }
    };
    this.__deletionAdapterOfLogEntry = new EntityDeletionOrUpdateAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `install_logs` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final LogEntry entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__updateAdapterOfLogEntry = new EntityDeletionOrUpdateAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `install_logs` SET `id` = ?,`packageName` = ?,`appName` = ?,`versionName` = ?,`versionCode` = ?,`timestamp` = ?,`operationType` = ?,`status` = ?,`details` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final LogEntry entity) {
        statement.bindLong(1, entity.id);
        if (entity.packageName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.packageName);
        }
        if (entity.appName == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.appName);
        }
        if (entity.versionName == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.versionName);
        }
        statement.bindLong(5, entity.versionCode);
        statement.bindLong(6, entity.timestamp);
        if (entity.operationType == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.operationType);
        }
        if (entity.status == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.status);
        }
        if (entity.details == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.details);
        }
        statement.bindLong(10, entity.id);
      }
    };
    this.__preparedStmtOfDeleteAllLogs = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM install_logs";
        return _query;
      }
    };
  }

  @Override
  public void insert(final LogEntry logEntry) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLogEntry.insert(logEntry);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertAll(final LogEntry... logEntries) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLogEntry.insert(logEntries);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final LogEntry logEntry) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfLogEntry.handle(logEntry);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final LogEntry logEntry) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfLogEntry.handle(logEntry);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAllLogs() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllLogs.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAllLogs.release(_stmt);
    }
  }

  @Override
  public LiveData<List<LogEntry>> getAllLogs() {
    final String _sql = "SELECT * FROM install_logs ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"install_logs"}, false, new Callable<List<LogEntry>>() {
      @Override
      @Nullable
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfVersionName = CursorUtil.getColumnIndexOrThrow(_cursor, "versionName");
          final int _cursorIndexOfVersionCode = CursorUtil.getColumnIndexOrThrow(_cursor, "versionCode");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDetails = CursorUtil.getColumnIndexOrThrow(_cursor, "details");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            _item = new LogEntry();
            _item.id = _cursor.getInt(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _item.packageName = null;
            } else {
              _item.packageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            if (_cursor.isNull(_cursorIndexOfAppName)) {
              _item.appName = null;
            } else {
              _item.appName = _cursor.getString(_cursorIndexOfAppName);
            }
            if (_cursor.isNull(_cursorIndexOfVersionName)) {
              _item.versionName = null;
            } else {
              _item.versionName = _cursor.getString(_cursorIndexOfVersionName);
            }
            _item.versionCode = _cursor.getInt(_cursorIndexOfVersionCode);
            _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            if (_cursor.isNull(_cursorIndexOfOperationType)) {
              _item.operationType = null;
            } else {
              _item.operationType = _cursor.getString(_cursorIndexOfOperationType);
            }
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _item.status = null;
            } else {
              _item.status = _cursor.getString(_cursorIndexOfStatus);
            }
            if (_cursor.isNull(_cursorIndexOfDetails)) {
              _item.details = null;
            } else {
              _item.details = _cursor.getString(_cursorIndexOfDetails);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<LogEntry>> getLogsByOperationType(final String operationType) {
    final String _sql = "SELECT * FROM install_logs WHERE operationType = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (operationType == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, operationType);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"install_logs"}, false, new Callable<List<LogEntry>>() {
      @Override
      @Nullable
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfVersionName = CursorUtil.getColumnIndexOrThrow(_cursor, "versionName");
          final int _cursorIndexOfVersionCode = CursorUtil.getColumnIndexOrThrow(_cursor, "versionCode");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDetails = CursorUtil.getColumnIndexOrThrow(_cursor, "details");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            _item = new LogEntry();
            _item.id = _cursor.getInt(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _item.packageName = null;
            } else {
              _item.packageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            if (_cursor.isNull(_cursorIndexOfAppName)) {
              _item.appName = null;
            } else {
              _item.appName = _cursor.getString(_cursorIndexOfAppName);
            }
            if (_cursor.isNull(_cursorIndexOfVersionName)) {
              _item.versionName = null;
            } else {
              _item.versionName = _cursor.getString(_cursorIndexOfVersionName);
            }
            _item.versionCode = _cursor.getInt(_cursorIndexOfVersionCode);
            _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            if (_cursor.isNull(_cursorIndexOfOperationType)) {
              _item.operationType = null;
            } else {
              _item.operationType = _cursor.getString(_cursorIndexOfOperationType);
            }
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _item.status = null;
            } else {
              _item.status = _cursor.getString(_cursorIndexOfStatus);
            }
            if (_cursor.isNull(_cursorIndexOfDetails)) {
              _item.details = null;
            } else {
              _item.details = _cursor.getString(_cursorIndexOfDetails);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<LogEntry>> getLogsByStatus(final String status) {
    final String _sql = "SELECT * FROM install_logs WHERE status = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (status == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, status);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"install_logs"}, false, new Callable<List<LogEntry>>() {
      @Override
      @Nullable
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfVersionName = CursorUtil.getColumnIndexOrThrow(_cursor, "versionName");
          final int _cursorIndexOfVersionCode = CursorUtil.getColumnIndexOrThrow(_cursor, "versionCode");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDetails = CursorUtil.getColumnIndexOrThrow(_cursor, "details");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            _item = new LogEntry();
            _item.id = _cursor.getInt(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _item.packageName = null;
            } else {
              _item.packageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            if (_cursor.isNull(_cursorIndexOfAppName)) {
              _item.appName = null;
            } else {
              _item.appName = _cursor.getString(_cursorIndexOfAppName);
            }
            if (_cursor.isNull(_cursorIndexOfVersionName)) {
              _item.versionName = null;
            } else {
              _item.versionName = _cursor.getString(_cursorIndexOfVersionName);
            }
            _item.versionCode = _cursor.getInt(_cursorIndexOfVersionCode);
            _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            if (_cursor.isNull(_cursorIndexOfOperationType)) {
              _item.operationType = null;
            } else {
              _item.operationType = _cursor.getString(_cursorIndexOfOperationType);
            }
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _item.status = null;
            } else {
              _item.status = _cursor.getString(_cursorIndexOfStatus);
            }
            if (_cursor.isNull(_cursorIndexOfDetails)) {
              _item.details = null;
            } else {
              _item.details = _cursor.getString(_cursorIndexOfDetails);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<LogEntry>> getLogsByPackageName(final String packageName) {
    final String _sql = "SELECT * FROM install_logs WHERE packageName = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (packageName == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, packageName);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"install_logs"}, false, new Callable<List<LogEntry>>() {
      @Override
      @Nullable
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfVersionName = CursorUtil.getColumnIndexOrThrow(_cursor, "versionName");
          final int _cursorIndexOfVersionCode = CursorUtil.getColumnIndexOrThrow(_cursor, "versionCode");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDetails = CursorUtil.getColumnIndexOrThrow(_cursor, "details");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            _item = new LogEntry();
            _item.id = _cursor.getInt(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _item.packageName = null;
            } else {
              _item.packageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            if (_cursor.isNull(_cursorIndexOfAppName)) {
              _item.appName = null;
            } else {
              _item.appName = _cursor.getString(_cursorIndexOfAppName);
            }
            if (_cursor.isNull(_cursorIndexOfVersionName)) {
              _item.versionName = null;
            } else {
              _item.versionName = _cursor.getString(_cursorIndexOfVersionName);
            }
            _item.versionCode = _cursor.getInt(_cursorIndexOfVersionCode);
            _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            if (_cursor.isNull(_cursorIndexOfOperationType)) {
              _item.operationType = null;
            } else {
              _item.operationType = _cursor.getString(_cursorIndexOfOperationType);
            }
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _item.status = null;
            } else {
              _item.status = _cursor.getString(_cursorIndexOfStatus);
            }
            if (_cursor.isNull(_cursorIndexOfDetails)) {
              _item.details = null;
            } else {
              _item.details = _cursor.getString(_cursorIndexOfDetails);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<LogEntry> getLogById(final int logId) {
    final String _sql = "SELECT * FROM install_logs WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, logId);
    return __db.getInvalidationTracker().createLiveData(new String[] {"install_logs"}, false, new Callable<LogEntry>() {
      @Override
      @Nullable
      public LogEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfVersionName = CursorUtil.getColumnIndexOrThrow(_cursor, "versionName");
          final int _cursorIndexOfVersionCode = CursorUtil.getColumnIndexOrThrow(_cursor, "versionCode");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDetails = CursorUtil.getColumnIndexOrThrow(_cursor, "details");
          final LogEntry _result;
          if (_cursor.moveToFirst()) {
            _result = new LogEntry();
            _result.id = _cursor.getInt(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _result.packageName = null;
            } else {
              _result.packageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            if (_cursor.isNull(_cursorIndexOfAppName)) {
              _result.appName = null;
            } else {
              _result.appName = _cursor.getString(_cursorIndexOfAppName);
            }
            if (_cursor.isNull(_cursorIndexOfVersionName)) {
              _result.versionName = null;
            } else {
              _result.versionName = _cursor.getString(_cursorIndexOfVersionName);
            }
            _result.versionCode = _cursor.getInt(_cursorIndexOfVersionCode);
            _result.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            if (_cursor.isNull(_cursorIndexOfOperationType)) {
              _result.operationType = null;
            } else {
              _result.operationType = _cursor.getString(_cursorIndexOfOperationType);
            }
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _result.status = null;
            } else {
              _result.status = _cursor.getString(_cursorIndexOfStatus);
            }
            if (_cursor.isNull(_cursorIndexOfDetails)) {
              _result.details = null;
            } else {
              _result.details = _cursor.getString(_cursorIndexOfDetails);
            }
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
