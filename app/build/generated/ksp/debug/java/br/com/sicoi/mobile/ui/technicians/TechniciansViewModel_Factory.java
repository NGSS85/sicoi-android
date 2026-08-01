package br.com.sicoi.mobile.ui.technicians;

import br.com.sicoi.mobile.data.repository.WorkOrderRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TechniciansViewModel_Factory implements Factory<TechniciansViewModel> {
  private final Provider<WorkOrderRepository> repositoryProvider;

  public TechniciansViewModel_Factory(Provider<WorkOrderRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TechniciansViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TechniciansViewModel_Factory create(
      Provider<WorkOrderRepository> repositoryProvider) {
    return new TechniciansViewModel_Factory(repositoryProvider);
  }

  public static TechniciansViewModel newInstance(WorkOrderRepository repository) {
    return new TechniciansViewModel(repository);
  }
}
