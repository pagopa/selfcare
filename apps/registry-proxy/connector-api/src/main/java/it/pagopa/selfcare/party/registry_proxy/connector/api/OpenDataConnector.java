package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.*;

import java.util.List;

public interface OpenDataConnector<I extends Institution, C extends Category, A extends AOO, U extends UO> {

    List<I> getInstitutions();

    List<C> getCategories();

    List<A> getAOOs();

    List<U> getUOs();

}
