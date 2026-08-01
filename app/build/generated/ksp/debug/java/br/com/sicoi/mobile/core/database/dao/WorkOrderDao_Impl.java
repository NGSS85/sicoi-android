package br.com.sicoi.mobile.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import br.com.sicoi.mobile.core.database.entity.WorkOrderEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WorkOrderDao_Impl implements WorkOrderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkOrderEntity> __insertionAdapterOfWorkOrderEntity;

  private final SharedSQLiteStatement __preparedStmtOfFinalizeOffline;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfClearFinalized;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatus;

  public WorkOrderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkOrderEntity = new EntityInsertionAdapter<WorkOrderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `work_orders_offline` (`id`,`numero_os`,`data_abertura`,`equipamento`,`setor`,`descricao`,`prioridade`,`status`,`tecnico`,`solicitante`,`solucao_aplicada`,`pecas_utilizadas`,`tempo_gasto`,`assinatura_url`,`foto_antes_url`,`foto_depois_url`,`sync_pending`,`updated_at_local`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkOrderEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getNumeroOs() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getNumeroOs());
        }
        if (entity.getDataAbertura() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDataAbertura());
        }
        if (entity.getEquipamento() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getEquipamento());
        }
        if (entity.getSetor() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSetor());
        }
        if (entity.getDescricaoProblema() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDescricaoProblema());
        }
        if (entity.getPrioridade() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPrioridade());
        }
        statement.bindString(8, entity.getStatus());
        if (entity.getTecnicoResponsavel() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTecnicoResponsavel());
        }
        if (entity.getSolicitante() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getSolicitante());
        }
        if (entity.getSolucaoAplicada() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getSolucaoAplicada());
        }
        if (entity.getPecasUtilizadas() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getPecasUtilizadas());
        }
        if (entity.getTempoGasto() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getTempoGasto());
        }
        if (entity.getAssinaturaUrl() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getAssinaturaUrl());
        }
        if (entity.getFotoAntesUrl() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getFotoAntesUrl());
        }
        if (entity.getFotoDepoisUrl() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getFotoDepoisUrl());
        }
        final int _tmp = entity.getSyncPending() ? 1 : 0;
        statement.bindLong(17, _tmp);
        statement.bindLong(18, entity.getUpdatedAtLocal());
      }
    };
    this.__preparedStmtOfFinalizeOffline = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE work_orders_offline\n"
                + "        SET status = 'Finalizada',\n"
                + "            solucao_aplicada = ?,\n"
                + "            pecas_utilizadas = ?,\n"
                + "            tempo_gasto = ?,\n"
                + "            assinatura_url = ?,\n"
                + "            foto_antes_url = ?,\n"
                + "            foto_depois_url = ?,\n"
                + "            sync_pending = 1,\n"
                + "            updated_at_local = ?\n"
                + "        WHERE id = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE work_orders_offline SET sync_pending = 0 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearFinalized = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM work_orders_offline WHERE status = 'Finalizada' AND sync_pending = 0";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE work_orders_offline\n"
                + "        SET status = ?,\n"
                + "            sync_pending = ?,\n"
                + "            updated_at_local = ?\n"
                + "        WHERE id = ?\n"
                + "    ";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<WorkOrderEntity> orders,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWorkOrderEntity.insert(orders);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final WorkOrderEntity order, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWorkOrderEntity.insert(order);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object finalizeOffline(final String id, final String solucao, final String pecas,
      final String tempo, final String assinatura, final String fotoAntes, final String fotoDepois,
      final long timestamp, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfFinalizeOffline.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, solucao);
        _argIndex = 2;
        _stmt.bindString(_argIndex, pecas);
        _argIndex = 3;
        _stmt.bindString(_argIndex, tempo);
        _argIndex = 4;
        if (assinatura == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, assinatura);
        }
        _argIndex = 5;
        if (fotoAntes == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, fotoAntes);
        }
        _argIndex = 6;
        if (fotoDepois == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, fotoDepois);
        }
        _argIndex = 7;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 8;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfFinalizeOffline.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearFinalized(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearFinalized.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearFinalized.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStatus(final String id, final String status, final int syncPending,
      final long timestamp, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, syncPending);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 4;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WorkOrderEntity>> getOpenOrders() {
    final String _sql = "SELECT * FROM work_orders_offline WHERE status = 'Em Aberto' ORDER BY updated_at_local DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"work_orders_offline"}, new Callable<List<WorkOrderEntity>>() {
      @Override
      @NonNull
      public List<WorkOrderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNumeroOs = CursorUtil.getColumnIndexOrThrow(_cursor, "numero_os");
          final int _cursorIndexOfDataAbertura = CursorUtil.getColumnIndexOrThrow(_cursor, "data_abertura");
          final int _cursorIndexOfEquipamento = CursorUtil.getColumnIndexOrThrow(_cursor, "equipamento");
          final int _cursorIndexOfSetor = CursorUtil.getColumnIndexOrThrow(_cursor, "setor");
          final int _cursorIndexOfDescricaoProblema = CursorUtil.getColumnIndexOrThrow(_cursor, "descricao");
          final int _cursorIndexOfPrioridade = CursorUtil.getColumnIndexOrThrow(_cursor, "prioridade");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfTecnicoResponsavel = CursorUtil.getColumnIndexOrThrow(_cursor, "tecnico");
          final int _cursorIndexOfSolicitante = CursorUtil.getColumnIndexOrThrow(_cursor, "solicitante");
          final int _cursorIndexOfSolucaoAplicada = CursorUtil.getColumnIndexOrThrow(_cursor, "solucao_aplicada");
          final int _cursorIndexOfPecasUtilizadas = CursorUtil.getColumnIndexOrThrow(_cursor, "pecas_utilizadas");
          final int _cursorIndexOfTempoGasto = CursorUtil.getColumnIndexOrThrow(_cursor, "tempo_gasto");
          final int _cursorIndexOfAssinaturaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "assinatura_url");
          final int _cursorIndexOfFotoAntesUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "foto_antes_url");
          final int _cursorIndexOfFotoDepoisUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "foto_depois_url");
          final int _cursorIndexOfSyncPending = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_pending");
          final int _cursorIndexOfUpdatedAtLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at_local");
          final List<WorkOrderEntity> _result = new ArrayList<WorkOrderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkOrderEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNumeroOs;
            if (_cursor.isNull(_cursorIndexOfNumeroOs)) {
              _tmpNumeroOs = null;
            } else {
              _tmpNumeroOs = _cursor.getString(_cursorIndexOfNumeroOs);
            }
            final String _tmpDataAbertura;
            if (_cursor.isNull(_cursorIndexOfDataAbertura)) {
              _tmpDataAbertura = null;
            } else {
              _tmpDataAbertura = _cursor.getString(_cursorIndexOfDataAbertura);
            }
            final String _tmpEquipamento;
            if (_cursor.isNull(_cursorIndexOfEquipamento)) {
              _tmpEquipamento = null;
            } else {
              _tmpEquipamento = _cursor.getString(_cursorIndexOfEquipamento);
            }
            final String _tmpSetor;
            if (_cursor.isNull(_cursorIndexOfSetor)) {
              _tmpSetor = null;
            } else {
              _tmpSetor = _cursor.getString(_cursorIndexOfSetor);
            }
            final String _tmpDescricaoProblema;
            if (_cursor.isNull(_cursorIndexOfDescricaoProblema)) {
              _tmpDescricaoProblema = null;
            } else {
              _tmpDescricaoProblema = _cursor.getString(_cursorIndexOfDescricaoProblema);
            }
            final String _tmpPrioridade;
            if (_cursor.isNull(_cursorIndexOfPrioridade)) {
              _tmpPrioridade = null;
            } else {
              _tmpPrioridade = _cursor.getString(_cursorIndexOfPrioridade);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpTecnicoResponsavel;
            if (_cursor.isNull(_cursorIndexOfTecnicoResponsavel)) {
              _tmpTecnicoResponsavel = null;
            } else {
              _tmpTecnicoResponsavel = _cursor.getString(_cursorIndexOfTecnicoResponsavel);
            }
            final String _tmpSolicitante;
            if (_cursor.isNull(_cursorIndexOfSolicitante)) {
              _tmpSolicitante = null;
            } else {
              _tmpSolicitante = _cursor.getString(_cursorIndexOfSolicitante);
            }
            final String _tmpSolucaoAplicada;
            if (_cursor.isNull(_cursorIndexOfSolucaoAplicada)) {
              _tmpSolucaoAplicada = null;
            } else {
              _tmpSolucaoAplicada = _cursor.getString(_cursorIndexOfSolucaoAplicada);
            }
            final String _tmpPecasUtilizadas;
            if (_cursor.isNull(_cursorIndexOfPecasUtilizadas)) {
              _tmpPecasUtilizadas = null;
            } else {
              _tmpPecasUtilizadas = _cursor.getString(_cursorIndexOfPecasUtilizadas);
            }
            final String _tmpTempoGasto;
            if (_cursor.isNull(_cursorIndexOfTempoGasto)) {
              _tmpTempoGasto = null;
            } else {
              _tmpTempoGasto = _cursor.getString(_cursorIndexOfTempoGasto);
            }
            final String _tmpAssinaturaUrl;
            if (_cursor.isNull(_cursorIndexOfAssinaturaUrl)) {
              _tmpAssinaturaUrl = null;
            } else {
              _tmpAssinaturaUrl = _cursor.getString(_cursorIndexOfAssinaturaUrl);
            }
            final String _tmpFotoAntesUrl;
            if (_cursor.isNull(_cursorIndexOfFotoAntesUrl)) {
              _tmpFotoAntesUrl = null;
            } else {
              _tmpFotoAntesUrl = _cursor.getString(_cursorIndexOfFotoAntesUrl);
            }
            final String _tmpFotoDepoisUrl;
            if (_cursor.isNull(_cursorIndexOfFotoDepoisUrl)) {
              _tmpFotoDepoisUrl = null;
            } else {
              _tmpFotoDepoisUrl = _cursor.getString(_cursorIndexOfFotoDepoisUrl);
            }
            final boolean _tmpSyncPending;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSyncPending);
            _tmpSyncPending = _tmp != 0;
            final long _tmpUpdatedAtLocal;
            _tmpUpdatedAtLocal = _cursor.getLong(_cursorIndexOfUpdatedAtLocal);
            _item = new WorkOrderEntity(_tmpId,_tmpNumeroOs,_tmpDataAbertura,_tmpEquipamento,_tmpSetor,_tmpDescricaoProblema,_tmpPrioridade,_tmpStatus,_tmpTecnicoResponsavel,_tmpSolicitante,_tmpSolucaoAplicada,_tmpPecasUtilizadas,_tmpTempoGasto,_tmpAssinaturaUrl,_tmpFotoAntesUrl,_tmpFotoDepoisUrl,_tmpSyncPending,_tmpUpdatedAtLocal);
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
  public Object getPendingSync(final Continuation<? super List<WorkOrderEntity>> $completion) {
    final String _sql = "SELECT * FROM work_orders_offline WHERE sync_pending = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkOrderEntity>>() {
      @Override
      @NonNull
      public List<WorkOrderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNumeroOs = CursorUtil.getColumnIndexOrThrow(_cursor, "numero_os");
          final int _cursorIndexOfDataAbertura = CursorUtil.getColumnIndexOrThrow(_cursor, "data_abertura");
          final int _cursorIndexOfEquipamento = CursorUtil.getColumnIndexOrThrow(_cursor, "equipamento");
          final int _cursorIndexOfSetor = CursorUtil.getColumnIndexOrThrow(_cursor, "setor");
          final int _cursorIndexOfDescricaoProblema = CursorUtil.getColumnIndexOrThrow(_cursor, "descricao");
          final int _cursorIndexOfPrioridade = CursorUtil.getColumnIndexOrThrow(_cursor, "prioridade");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfTecnicoResponsavel = CursorUtil.getColumnIndexOrThrow(_cursor, "tecnico");
          final int _cursorIndexOfSolicitante = CursorUtil.getColumnIndexOrThrow(_cursor, "solicitante");
          final int _cursorIndexOfSolucaoAplicada = CursorUtil.getColumnIndexOrThrow(_cursor, "solucao_aplicada");
          final int _cursorIndexOfPecasUtilizadas = CursorUtil.getColumnIndexOrThrow(_cursor, "pecas_utilizadas");
          final int _cursorIndexOfTempoGasto = CursorUtil.getColumnIndexOrThrow(_cursor, "tempo_gasto");
          final int _cursorIndexOfAssinaturaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "assinatura_url");
          final int _cursorIndexOfFotoAntesUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "foto_antes_url");
          final int _cursorIndexOfFotoDepoisUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "foto_depois_url");
          final int _cursorIndexOfSyncPending = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_pending");
          final int _cursorIndexOfUpdatedAtLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at_local");
          final List<WorkOrderEntity> _result = new ArrayList<WorkOrderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkOrderEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNumeroOs;
            if (_cursor.isNull(_cursorIndexOfNumeroOs)) {
              _tmpNumeroOs = null;
            } else {
              _tmpNumeroOs = _cursor.getString(_cursorIndexOfNumeroOs);
            }
            final String _tmpDataAbertura;
            if (_cursor.isNull(_cursorIndexOfDataAbertura)) {
              _tmpDataAbertura = null;
            } else {
              _tmpDataAbertura = _cursor.getString(_cursorIndexOfDataAbertura);
            }
            final String _tmpEquipamento;
            if (_cursor.isNull(_cursorIndexOfEquipamento)) {
              _tmpEquipamento = null;
            } else {
              _tmpEquipamento = _cursor.getString(_cursorIndexOfEquipamento);
            }
            final String _tmpSetor;
            if (_cursor.isNull(_cursorIndexOfSetor)) {
              _tmpSetor = null;
            } else {
              _tmpSetor = _cursor.getString(_cursorIndexOfSetor);
            }
            final String _tmpDescricaoProblema;
            if (_cursor.isNull(_cursorIndexOfDescricaoProblema)) {
              _tmpDescricaoProblema = null;
            } else {
              _tmpDescricaoProblema = _cursor.getString(_cursorIndexOfDescricaoProblema);
            }
            final String _tmpPrioridade;
            if (_cursor.isNull(_cursorIndexOfPrioridade)) {
              _tmpPrioridade = null;
            } else {
              _tmpPrioridade = _cursor.getString(_cursorIndexOfPrioridade);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpTecnicoResponsavel;
            if (_cursor.isNull(_cursorIndexOfTecnicoResponsavel)) {
              _tmpTecnicoResponsavel = null;
            } else {
              _tmpTecnicoResponsavel = _cursor.getString(_cursorIndexOfTecnicoResponsavel);
            }
            final String _tmpSolicitante;
            if (_cursor.isNull(_cursorIndexOfSolicitante)) {
              _tmpSolicitante = null;
            } else {
              _tmpSolicitante = _cursor.getString(_cursorIndexOfSolicitante);
            }
            final String _tmpSolucaoAplicada;
            if (_cursor.isNull(_cursorIndexOfSolucaoAplicada)) {
              _tmpSolucaoAplicada = null;
            } else {
              _tmpSolucaoAplicada = _cursor.getString(_cursorIndexOfSolucaoAplicada);
            }
            final String _tmpPecasUtilizadas;
            if (_cursor.isNull(_cursorIndexOfPecasUtilizadas)) {
              _tmpPecasUtilizadas = null;
            } else {
              _tmpPecasUtilizadas = _cursor.getString(_cursorIndexOfPecasUtilizadas);
            }
            final String _tmpTempoGasto;
            if (_cursor.isNull(_cursorIndexOfTempoGasto)) {
              _tmpTempoGasto = null;
            } else {
              _tmpTempoGasto = _cursor.getString(_cursorIndexOfTempoGasto);
            }
            final String _tmpAssinaturaUrl;
            if (_cursor.isNull(_cursorIndexOfAssinaturaUrl)) {
              _tmpAssinaturaUrl = null;
            } else {
              _tmpAssinaturaUrl = _cursor.getString(_cursorIndexOfAssinaturaUrl);
            }
            final String _tmpFotoAntesUrl;
            if (_cursor.isNull(_cursorIndexOfFotoAntesUrl)) {
              _tmpFotoAntesUrl = null;
            } else {
              _tmpFotoAntesUrl = _cursor.getString(_cursorIndexOfFotoAntesUrl);
            }
            final String _tmpFotoDepoisUrl;
            if (_cursor.isNull(_cursorIndexOfFotoDepoisUrl)) {
              _tmpFotoDepoisUrl = null;
            } else {
              _tmpFotoDepoisUrl = _cursor.getString(_cursorIndexOfFotoDepoisUrl);
            }
            final boolean _tmpSyncPending;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSyncPending);
            _tmpSyncPending = _tmp != 0;
            final long _tmpUpdatedAtLocal;
            _tmpUpdatedAtLocal = _cursor.getLong(_cursorIndexOfUpdatedAtLocal);
            _item = new WorkOrderEntity(_tmpId,_tmpNumeroOs,_tmpDataAbertura,_tmpEquipamento,_tmpSetor,_tmpDescricaoProblema,_tmpPrioridade,_tmpStatus,_tmpTecnicoResponsavel,_tmpSolicitante,_tmpSolucaoAplicada,_tmpPecasUtilizadas,_tmpTempoGasto,_tmpAssinaturaUrl,_tmpFotoAntesUrl,_tmpFotoDepoisUrl,_tmpSyncPending,_tmpUpdatedAtLocal);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
