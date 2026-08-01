package br.com.sicoi.mobile.core.sync;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class OfflineSyncWorker_AssistedFactory_Impl implements OfflineSyncWorker_AssistedFactory {
  private final OfflineSyncWorker_Factory delegateFactory;

  OfflineSyncWorker_AssistedFactory_Impl(OfflineSyncWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public OfflineSyncWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<OfflineSyncWorker_AssistedFactory> create(
      OfflineSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new OfflineSyncWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<OfflineSyncWorker_AssistedFactory> createFactoryProvider(
      OfflineSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new OfflineSyncWorker_AssistedFactory_Impl(delegateFactory));
  }
}
