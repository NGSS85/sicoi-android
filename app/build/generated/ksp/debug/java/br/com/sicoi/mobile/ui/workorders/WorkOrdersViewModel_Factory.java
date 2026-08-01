package br.com.sicoi.mobile.ui.workorders;

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
public final class WorkOrdersViewModel_Factory implements Factory<WorkOrdersViewModel> {
  private final Provider<WorkOrderRepository> repositoryProvider;

  public WorkOrdersViewModel_Factory(Provider<WorkOrderRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public WorkOrdersViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static WorkOrdersViewModel_Factory create(
      Provider<WorkOrderRepository> repositoryProvider) {
    return new WorkOrdersViewModel_Factory(repositoryProvider);
  }

  public static WorkOrdersViewModel newInstance(WorkOrderRepository repository) {
    return new WorkOrdersViewModel(repository);
  }
}
