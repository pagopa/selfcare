package it.pagopa.selfcare.party.registry_proxy.connector.rest.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MaskDataUtils {
    private static final Pattern elencoCf = Pattern.compile("(\"elencoCf\")\\s*:\\s*\\[\"(.*?)\"");
    private static final Pattern patternTaxId = Pattern.compile("(\"taxId\"|\"legalTaxId\"|\"businessTaxId\"|\"vatNumber\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern patternAddress = Pattern.compile("(\"description\"|\"at\"|\"address\"|\"zip\"|\"municipality\"|\"municipalityDetails\"|\"province\"|\"foreignState\"|\"codiceStato\"|\"descrizioneStato\"|\"descrizioneLocalita\"|\"denominazione\"|\"numeroCivico\"|\"digitalAddress\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern patternIdentity = Pattern.compile("(\"pecProfessionista\"|\"cf\"|\"codFiscale\"|\"codiceFiscale\"|\"cognome\"|\"nome\"|\"sesso\"|\"dataNascita\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern patternAccessToken = Pattern.compile("(\"access_token\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern patternTaxIdQueryParams = Pattern.compile("(taxId|vatNumber)=(.{0,16})");

    private MaskDataUtils(){}

    public static String maskInformation(String dataBuffered){

        dataBuffered = maskMatcher(elencoCf, dataBuffered);
        dataBuffered = maskMatcher(patternTaxId, dataBuffered);
        dataBuffered = maskMatcher(patternAddress, dataBuffered);
        dataBuffered = maskMatcher(patternIdentity, dataBuffered);
        dataBuffered = maskMatcher(patternAccessToken, dataBuffered);
        dataBuffered = maskMatcher(patternTaxIdQueryParams, dataBuffered);

        return dataBuffered;
    }

    private static String maskMatcher(Pattern pattern, String dataBuffered){
        Matcher matcher = pattern.matcher(dataBuffered);
        while(matcher.find()){
            String toBeMasked = matcher.group(2);
            String valueMasked = mask(toBeMasked);
            if(!toBeMasked.isBlank()){
                dataBuffered = dataBuffered.replace(toBeMasked, valueMasked);
            }
        }
        return dataBuffered;
    }

    private static String mask(String unmasked){
        if(unmasked.contains(","))
            return maskAddress(unmasked);
        else if(unmasked.contains("@"))
            return maskEmailAddress(unmasked);
        else
            return maskString(unmasked);

    }


    private static String maskAddress(String strAddress){
        String[] parts = strAddress.split(",");
        StringBuilder masked = new StringBuilder();
        for (String part : parts) {
            masked.append(maskString(part)).append(",");
        }
        return masked.substring(0,masked.length()-1);
    }

    private static String maskEmailAddress(String strEmail) {
        String[] parts = strEmail.split("@");
        String strId = maskString(parts[0]);
        return strId + "@" + parts[1];
    }

    public static String maskString(String strText) {
        int start = 1;
        int end = strText.length()-3;
        String maskChar = String.valueOf('*');

        if(strText.equals("")){
            return "";
        }
        if(strText.length() < 4){
            end = strText.length();
        }
        int maskLength = end - start;
        if(maskLength == 0) {
            return maskChar;
        }
        String sbMaskString = maskChar.repeat(Math.max(0, maskLength));
        return strText.substring(0, start)
                + sbMaskString
                + strText.substring(start + maskLength);
    }

}
