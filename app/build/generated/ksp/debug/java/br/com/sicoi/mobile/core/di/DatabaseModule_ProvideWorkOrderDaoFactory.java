package br.com.sicoi.mobile.core.di;

import br.com.sicoi.mobile.core.database.AppDatabase;
import br.com.sicoi.mobile.core.database.dao.WorkOrderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideWorkOrderDaoFactory implements Factory<WorkOrderDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideWorkOrderDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public WorkOrderDao get() {
    return provideWorkOrderDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideWorkOrderDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideWorkOrderDaoFactory(databaseProvider);
  }

  public static WorkOrderDao provideWorkOrderDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWorkOrderDao(database));
  }
}
