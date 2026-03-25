package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Station.Field.*;

@Slf4j
public class StationToDocumentConverter implements Function<Station, Document> {

    @Override
    public Document apply(Station station) {
        Document doc = null;
        if (station != null) {
            doc = new Document();
            doc.add(new StringField(Entity.ENTITY_TYPE.toString(), Entity.STATION.toString(), Field.Store.YES));
            doc.add(new StringField(ID.toString(), station.getId(), Field.Store.YES));
            doc.add(new StoredField(ORIGIN.toString(), station.getOrigin().toString()));
            doc.add(new SortedDocValuesField(DESCRIPTION.toString(), new BytesRef(station.getDescription())));
            doc.add(new TextField(DESCRIPTION.toString(), station.getDescription(), Field.Store.YES));
            doc.add(new StoredField(TAX_CODE.toString(), station.getTaxCode()));
            doc.add(new StoredField(DIGITAL_ADDRESS.toString(), station.getDigitalAddress()));
            doc.add(new StoredField(ANAC_ENABLED.toString(), station.getAnacEnabled()));
            doc.add(new StoredField(ANAC_ENGAGED.toString(), station.getAnacEngaged()));
        }
        return doc;
    }

}
