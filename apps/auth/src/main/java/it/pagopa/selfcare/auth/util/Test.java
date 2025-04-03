package it.pagopa.selfcare.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Test {

    public static void main (String[] args){
        String oiClientId = "xzzUuY4LVRarzBT8_BnnFc8Jvj6lQgoLPkbrQuDJb6I";
        String oiClientSecret = "dS8BxVPFtfxdDN023zZRDRwBSJiExqQDDIMZ62SkJEQ";
        System.out.println(Base64.getEncoder()
                .encodeToString((oiClientId+":"+ oiClientSecret).getBytes(StandardCharsets.UTF_8)));
    }
}
