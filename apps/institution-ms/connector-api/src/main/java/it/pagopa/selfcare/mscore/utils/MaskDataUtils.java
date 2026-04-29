package it.pagopa.selfcare.mscore.utils;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
@NoArgsConstructor(access = AccessLevel.NONE)
public class MaskDataUtils {

    private static final int MATCHER_GROUP = 2;
    private static final int MASK_STRING_END = 3;
    private static final int CHARACTER_LIMIT = 4;
    private static final String REGEX_ELENCO_CF = "(\"elencoCf\")\\s*:\\s*\\[\"(.*?)\"";
    private static final String REGEX_TAX_ID = "(\"taxId\"|\"legalTaxId\"|\"businessTaxId\"|\"vatNumber\")\\s*:\\s*\"(.*?)\"";
    private static final String REGEX_ADDRESS = "(\"description\"|\"at\"|\"address\"|\"zip\"|\"municipality\"|\"municipalityDetails\"|\"province\"|\"foreignState\"|\"codiceStato\"|\"descrizioneStato\"|\"descrizioneLocalita\"|\"denominazione\"|\"numeroCivico\"|\"digitalAddress\")\\s*:\\s*\"(.*?)\"";
    private static final String REGEX_PEC = "(\"pecProfessionista\"|\"cf\"|\"codFiscale\"|\"codiceFiscale\"|\"cognome\"|\"nome\"|\"sesso\"|\"dataNascita\")\\s*:\\s*\"(.*?)\"";
    private static final String REGEX_TOKEN = "(\"access_token\")\\s*:\\s*\"(.*?)\"";
    private static final Pattern PATTERN_ELENCO_CF = Pattern.compile(REGEX_ELENCO_CF);
    private static final Pattern PATTERN_TAX_ID = Pattern.compile(REGEX_TAX_ID);
    private static final Pattern PATTERN_ADDRESS = Pattern.compile(REGEX_ADDRESS);
    private static final Pattern PATTERN_PEC = Pattern.compile(REGEX_PEC);
    private static final Pattern PATTERN_TOKEN = Pattern.compile(REGEX_TOKEN);

    public static String maskInformation(String dataBuffered){

        dataBuffered = maskMatcher(PATTERN_ELENCO_CF, dataBuffered);
        dataBuffered = maskMatcher(PATTERN_TAX_ID, dataBuffered);
        dataBuffered = maskMatcher(PATTERN_ADDRESS, dataBuffered);
        dataBuffered = maskMatcher(PATTERN_PEC, dataBuffered);
        dataBuffered = maskMatcher(PATTERN_TOKEN, dataBuffered);

        return dataBuffered;
    }

    private static String maskMatcher(Pattern pattern, String dataBuffered){
        Matcher matcher = pattern.matcher(dataBuffered);
        while(matcher.find()){
            String toBeMasked = matcher.group(MATCHER_GROUP);
            String valueMasked = mask(toBeMasked);
            if(!toBeMasked.isBlank()){
                dataBuffered = dataBuffered.replace("\""+toBeMasked+"\"","\""+valueMasked+"\"");
            }
        }
        return dataBuffered;
    }

    private static String mask(String unmasked){
        if(unmasked.contains(",")){
            return maskAddress(unmasked);
        }
        else if(unmasked.contains("@")){
            return maskEmailAddress(unmasked);
        }
        else{
            return maskString(unmasked);
        }
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
        int end = strText.length()-MASK_STRING_END;
        String maskChar = String.valueOf('*');
        if(StringUtils.isEmpty(strText)){
            return strText;
        }
        if(strText.length() < CHARACTER_LIMIT){
            end = strText.length();
        }
        int maskLength = end - start;
        if(maskLength == 0){
            return maskChar;
        }
        String sbMaskString = maskChar.repeat(Math.max(0, maskLength));
        return strText.substring(0, start)
                + sbMaskString
                + strText.substring(start + maskLength);
    }

}
