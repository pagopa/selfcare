package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.UOEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.*;

@Slf4j
public class DocumentToUOConverter implements Function<Document, UO> {

    @Override
    public UO apply(Document document) {
        UOEntity entity = null;
        if (document != null) {
            entity = new UOEntity();
            entity.setCodiceIpa(document.get(CODICE_IPA.toString()));
            entity.setOrigin(Origin.fromValue(document.get(ORIGIN.toString())));
            entity.setDenominazioneEnte(document.get(DENOMINAZIONE_ENTE.toString()));
            entity.setCodiceFiscaleEnte(document.get(CODICE_FISCALE_ENTE.toString()));
            entity.setCodiceFiscaleSfe(document.get(CODICE_FISCALE_SFE.toString()));
            entity.setCodiceUniAoo(document.get(CODICE_UNI_AOO.toString()));
            entity.setCodiceUniUo(document.get(CODICE_UNI_UO.toString()));
            entity.setCodiceUniUoPadre(document.get(CODICE_UNI_UO_PADRE.toString()));
            entity.setMail1(document.get(MAIL1.toString()));
            entity.setDescrizioneUo(document.get(DESCRIZIONE_UO.toString()));

            entity.setDataIstituzione(document.get(DATA_ISTITUTIONE.toString()));
            entity.setNomeResponsabile(document.get(NOME_RESPONSABILE.toString()));
            entity.setCognomeResponsabile(document.get(COGNOME_RESPONSABILE.toString()));
            entity.setMailResponsabile(document.get(MAIL_RESPONSABILE.toString()));
            entity.setTelefonoResponsabile(document.get(TELEFONO_RESPONSABILE.toString()));
            entity.setCodiceComuneISTAT(document.get(CODICE_COMUNE_ISTAT.toString()));
            entity.setCodiceCatastaleComune(document.get(CODICE_CATASTALE_COMUNE.toString()));
            entity.setCAP(document.get(CAP.toString()));
            entity.setIndirizzo(document.get(INDIRIZZO.toString()));
            entity.setTelefono(document.get(TELEFONO.toString()));
            entity.setFax(document.get(FAX.toString()));
            entity.setTipoMail1(document.get(TIPO_MAIL1.toString()));
            entity.setUrl(document.get(URL.toString()));
            entity.setDataAggiornamento(document.get(DATA_AGGIORNAMENTO.toString()));
        }
        return entity;
    }

}
