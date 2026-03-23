package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.StationEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field.*;

@Slf4j
public class DocumentToStationConverter implements Function<Document, Station> {

    @Override
    public Station apply(Document document) {
        StationEntity station = null;
        if (document != null) {
            station = new StationEntity();
            station.setId(document.get(ID.toString()));
            station.setOriginId(document.get(ORIGIN_ID.toString()));
            station.setTaxCode(document.get(TAX_CODE.toString()));
            station.setDescription(document.get(DESCRIPTION.toString()));
            station.setDigitalAddress(document.get(DIGITAL_ADDRESS.toString()));
            station.setOrigin(Origin.ANAC);
        }
        return station;
    }

}
