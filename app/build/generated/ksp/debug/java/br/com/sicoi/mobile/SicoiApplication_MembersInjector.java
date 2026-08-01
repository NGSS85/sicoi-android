package br.com.sicoi.mobile;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SicoiApplication_MembersInjector implements MembersInjector<SicoiApplication> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public SicoiApplication_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<SicoiApplication> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new SicoiApplication_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(SicoiApplication instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("br.com.sicoi.mobile.SicoiApplication.workerFactory")
  public static void injectWorkerFactory(SicoiApplication instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
