package it.pagopa.selfcare.registry.proxy.runner.client;

public interface DataSourceRestClient<T> {
  T retrieveDataSource();
}
