package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.web.model.AOOResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.AOOsResource;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public class AOOMapper {

    public static AOOResource toResource(AOO aoo) {
        AOOResource aooResource = null;
        if (aoo != null) {
            aooResource = new AOOResource();
            aooResource.setId(aoo.getId());
            aooResource.setCodAoo(aoo.getCodAoo());
            aooResource.setCodiceFiscaleEnte(aoo.getCodiceFiscaleEnte());
            aooResource.setCodiceUniAoo(aoo.getCodiceUniAoo());
            aooResource.setCodiceIpa(aoo.getCodiceIpa());
            aooResource.setDenominazioneEnte(aoo.getDenominazioneEnte());
            aooResource.setDenominazioneAoo(aoo.getDenominazioneAoo());
            aooResource.setOrigin(aoo.getOrigin());

            aooResource.setMail1(aoo.getMail1());
            aooResource.setMail2(aoo.getMail2());
            aooResource.setMail3(aoo.getMail3());

            aooResource.setDataIstituzione(aoo.getDataIstituzione());
            aooResource.setNomeResponsabile(aoo.getNomeResponsabile());
            aooResource.setCognomeResponsabile(aoo.getCognomeResponsabile());
            aooResource.setMailResponsabile(aoo.getMailResponsabile());
            aooResource.setTelefonoResponsabile(aoo.getTelefonoResponsabile());
            aooResource.setCodiceComuneISTAT(aoo.getCodiceComuneISTAT());
            aooResource.setCodiceCatastaleComune(aoo.getCodiceCatastaleComune());
            aooResource.setCAP(aoo.getCAP());
            aooResource.setIndirizzo(aoo.getIndirizzo());
            aooResource.setTelefono(aoo.getTelefono());
            aooResource.setFax(aoo.getFax());

            aooResource.setTipoMail1(aoo.getTipoMail1());
            aooResource.setTipoMail2(aoo.getTipoMail2());
            aooResource.setTipoMail3(aoo.getTipoMail3());

            aooResource.setProtocolloInformatico(aoo.getProtocolloInformatico());
            aooResource.setURIProtocolloInformatico(aoo.getURIProtocolloInformatico());
            aooResource.setDataAggiornamento(aoo.getDataAggiornamento());
        }
        return aooResource;
    }

    public static AOOsResource toResource(List<AOOResource> aooList, long count) {
        AOOsResource aoosResource = null;
        if (aooList != null) {
            aoosResource = new AOOsResource();
            aoosResource.setItems(aooList);
            aoosResource.setCount(count);
        }
        return aoosResource;
    }

}
