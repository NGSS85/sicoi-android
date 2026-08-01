package br.com.sicoi.mobile.data.repository;

import br.com.sicoi.mobile.core.database.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class WorkOrderRepository_Factory implements Factory<WorkOrderRepository> {
  private final Provider<AppDatabase> databaseProvider;

  public WorkOrderRepository_Factory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public WorkOrderRepository get() {
    return newInstance(databaseProvider.get());
  }

  public static WorkOrderRepository_Factory create(Provider<AppDatabase> databaseProvider) {
    return new WorkOrderRepository_Factory(databaseProvider);
  }

  public static WorkOrderRepository newInstance(AppDatabase database) {
    return new WorkOrderRepository(database);
  }
}
