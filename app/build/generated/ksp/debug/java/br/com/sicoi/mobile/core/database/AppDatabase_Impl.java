package br.com.sicoi.mobile.core.database;

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
import br.com.sicoi.mobile.core.database.dao.WorkOrderDao;
import br.com.sicoi.mobile.core.database.dao.WorkOrderDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile WorkOrderDao _workOrderDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `work_orders_offline` (`id` TEXT NOT NULL, `numero_os` TEXT, `data_abertura` TEXT, `equipamento` TEXT, `setor` TEXT, `descricao` TEXT, `prioridade` TEXT, `status` TEXT NOT NULL, `tecnico` TEXT, `solicitante` TEXT, `solucao_aplicada` TEXT, `pecas_utilizadas` TEXT, `tempo_gasto` TEXT, `assinatura_url` TEXT, `foto_antes_url` TEXT, `foto_depois_url` TEXT, `sync_pending` INTEGER NOT NULL, `updated_at_local` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5df6012b80f14e58b199bc49c37b1abf')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `work_orders_offline`");
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
        final HashMap<String, TableInfo.Column> _columnsWorkOrdersOffline = new HashMap<String, TableInfo.Column>(18);
        _columnsWorkOrdersOffline.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("numero_os", new TableInfo.Column("numero_os", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("data_abertura", new TableInfo.Column("data_abertura", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("equipamento", new TableInfo.Column("equipamento", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("setor", new TableInfo.Column("setor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("descricao", new TableInfo.Column("descricao", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("prioridade", new TableInfo.Column("prioridade", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("tecnico", new TableInfo.Column("tecnico", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("solicitante", new TableInfo.Column("solicitante", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("solucao_aplicada", new TableInfo.Column("solucao_aplicada", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("pecas_utilizadas", new TableInfo.Column("pecas_utilizadas", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("tempo_gasto", new TableInfo.Column("tempo_gasto", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("assinatura_url", new TableInfo.Column("assinatura_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("foto_antes_url", new TableInfo.Column("foto_antes_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("foto_depois_url", new TableInfo.Column("foto_depois_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("sync_pending", new TableInfo.Column("sync_pending", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkOrdersOffline.put("updated_at_local", new TableInfo.Column("updated_at_local", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkOrdersOffline = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWorkOrdersOffline = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWorkOrdersOffline = new TableInfo("work_orders_offline", _columnsWorkOrdersOffline, _foreignKeysWorkOrdersOffline, _indicesWorkOrdersOffline);
        final TableInfo _existingWorkOrdersOffline = TableInfo.read(db, "work_orders_offline");
        if (!_infoWorkOrdersOffline.equals(_existingWorkOrdersOffline)) {
          return new RoomOpenHelper.ValidationResult(false, "work_orders_offline(br.com.sicoi.mobile.core.database.entity.WorkOrderEntity).\n"
                  + " Expected:\n" + _infoWorkOrdersOffline + "\n"
                  + " Found:\n" + _existingWorkOrdersOffline);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "5df6012b80f14e58b199bc49c37b1abf", "78d66a432bb5ebfc6e9570d64c212fad");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "work_orders_offline");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `work_orders_offline`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(WorkOrderDao.class, WorkOrderDao_Impl.getRequiredConverters());
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
  public WorkOrderDao workOrderDao() {
    if (_workOrderDao != null) {
      return _workOrderDao;
    } else {
      synchronized(this) {
        if(_workOrderDao == null) {
          _workOrderDao = new WorkOrderDao_Impl(this);
        }
        return _workOrderDao;
      }
    }
  }
}
