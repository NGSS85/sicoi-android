package br.com.sicoi.mobile.core.sync;

import android.content.Context;
import androidx.work.WorkerParameters;
import br.com.sicoi.mobile.core.database.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class OfflineSyncWorker_Factory {
  private final Provider<AppDatabase> databaseProvider;

  public OfflineSyncWorker_Factory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  public OfflineSyncWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, databaseProvider.get());
  }

  public static OfflineSyncWorker_Factory create(Provider<AppDatabase> databaseProvider) {
    return new OfflineSyncWorker_Factory(databaseProvider);
  }

  public static OfflineSyncWorker newInstance(Context context, WorkerParameters workerParams,
      AppDatabase database) {
    return new OfflineSyncWorker(context, workerParams, database);
  }
}
