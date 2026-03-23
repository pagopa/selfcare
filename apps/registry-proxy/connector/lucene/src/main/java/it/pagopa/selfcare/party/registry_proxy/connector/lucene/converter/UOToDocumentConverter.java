package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field.ID;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CAP;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_CATASTALE_COMUNE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_COMUNE_ISTAT;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_FISCALE_ENTE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_FISCALE_SFE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_IPA;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_UNI_AOO;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_UNI_UO;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.CODICE_UNI_UO_PADRE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.COGNOME_RESPONSABILE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.DATA_AGGIORNAMENTO;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.DATA_ISTITUTIONE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.DENOMINAZIONE_ENTE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.DESCRIZIONE_UO;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.FAX;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.INDIRIZZO;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.MAIL1;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.MAIL_RESPONSABILE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.NOME_RESPONSABILE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.ORIGIN;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.TELEFONO;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.TELEFONO_RESPONSABILE;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.TIPO_MAIL1;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field.URL;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

@Slf4j
public class UOToDocumentConverter implements Function<UO, Document> {

    @Override
    public Document apply(UO uo) {
        Document doc = null;
        if (uo != null) {
            doc = new Document();
            doc.add(new StringField(Entity.ENTITY_TYPE.toString(), Entity.UO.toString(), Field.Store.YES));
            doc.add(new StringField(ID.toString(), uo.getId() != null ? uo.getId().toUpperCase() : uo.getId(), Field.Store.YES));
            doc.add(new StringField(CODICE_IPA.toString(), uo.getCodiceIpa(), Field.Store.YES));
            doc.add(new StringField(CODICE_FISCALE_ENTE.toString(), uo.getCodiceFiscaleEnte(), Field.Store.YES));
            doc.add(new StringField(CODICE_UNI_UO.toString(), uo.getCodiceUniUo(), Field.Store.YES));
            if (Objects.nonNull(uo.getCodiceFiscaleSfe())) {
                doc.add(new StringField(CODICE_FISCALE_SFE.toString(), uo.getCodiceFiscaleSfe(), Field.Store.YES));
            }
            doc.add(new StringField(CODICE_UNI_UO_PADRE.toString(), uo.getCodiceUniUoPadre(), Field.Store.YES));
            doc.add(new StringField(CODICE_UNI_AOO.toString(), uo.getCodiceUniAoo(), Field.Store.YES));
            doc.add(new StringField(ORIGIN.toString(), uo.getOrigin().toString(), Field.Store.YES));

            doc.add(new StringField(DENOMINAZIONE_ENTE.toString(), uo.getDenominazioneEnte(), Field.Store.YES));
            doc.add(new StringField(DESCRIZIONE_UO.toString(), uo.getDescrizioneUo(), Field.Store.YES));


            doc.add(new StringField(DATA_ISTITUTIONE.toString(), uo.getDataIstituzione(), Field.Store.YES));
            doc.add(new StringField(NOME_RESPONSABILE.toString(), uo.getNomeResponsabile(), Field.Store.YES));
            doc.add(new StringField(COGNOME_RESPONSABILE.toString(), uo.getCognomeResponsabile(), Field.Store.YES));
            doc.add(new StringField(MAIL_RESPONSABILE.toString(), uo.getMailResponsabile(), Field.Store.YES));
            doc.add(new StringField(TELEFONO_RESPONSABILE.toString(), uo.getTelefonoResponsabile(), Field.Store.YES));
            doc.add(new StringField(CODICE_COMUNE_ISTAT.toString(), uo.getCodiceComuneISTAT(), Field.Store.YES));
            doc.add(new StringField(CODICE_CATASTALE_COMUNE.toString(), uo.getCodiceCatastaleComune(), Field.Store.YES));
            doc.add(new StringField(CAP.toString(), uo.getCAP(), Field.Store.YES));
            doc.add(new StringField(INDIRIZZO.toString(), uo.getIndirizzo(), Field.Store.YES));
            doc.add(new StringField(TELEFONO.toString(), uo.getTelefono(), Field.Store.YES));
            doc.add(new StringField(FAX.toString(), uo.getFax(), Field.Store.YES));
            doc.add(new StringField(URL.toString(), uo.getUrl(), Field.Store.YES));
            doc.add(new StringField(DATA_AGGIORNAMENTO.toString(), uo.getDataAggiornamento(), Field.Store.YES));

            appendContact(doc, uo);
        }
        return doc;
    }

    void appendContact(Document doc, UO uo) {

        String email = StringUtils.EMPTY;
        String typeEmail = StringUtils.EMPTY;

        if (uo.getTipoMail1().equalsIgnoreCase("Pec")) {
            email = uo.getMail1();
            typeEmail = uo.getTipoMail1();
        } else if (uo.getTipoMail2().equalsIgnoreCase("Pec")) {
            email = uo.getMail2();
            typeEmail = uo.getTipoMail2();
        } else if (uo.getTipoMail3().equalsIgnoreCase("Pec")) {
            email = uo.getMail3();
            typeEmail = uo.getTipoMail3();
        }

        doc.add(new StringField(MAIL1.toString(), email, Field.Store.YES));
        doc.add(new StringField(TIPO_MAIL1.toString(), typeEmail, Field.Store.YES));
    }

}
