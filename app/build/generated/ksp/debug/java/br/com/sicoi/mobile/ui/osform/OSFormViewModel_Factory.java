package br.com.sicoi.mobile.ui.osform;

import android.content.Context;
import br.com.sicoi.mobile.data.repository.WorkOrderRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class OSFormViewModel_Factory implements Factory<OSFormViewModel> {
  private final Provider<WorkOrderRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public OSFormViewModel_Factory(Provider<WorkOrderRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public OSFormViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static OSFormViewModel_Factory create(Provider<WorkOrderRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new OSFormViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static OSFormViewModel newInstance(WorkOrderRepository repository, Context context) {
    return new OSFormViewModel(repository, context);
  }
}
