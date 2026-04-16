package it.pagopa.selfcare.party.connector.ftp.client;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class FTPClientTest {

    private AnacFTPClient anacFtpClient;

    @Mock
    FTPClient ftpClient;

    @BeforeEach
    public void init(){
        anacFtpClient = new AnacFTPClient("localhost",22, "user","pass", "string");
    }

    @Test
    void getFileOk() throws IOException{
        //given
        String bytes = """
                codice_IPA,cf_gestore,cenominazione,comicilio_digitale,anac_incaricato,anac_abilitato
                aaaaaaa,tax_code,denominazione,test@pec.it,false,false
                """;
        InputStream mockInputStream = new ByteArrayInputStream(bytes.getBytes(StandardCharsets.UTF_8));
        when(ftpClient.retrieveFileStream(any())).thenReturn(mockInputStream);
        Executable executable = () ->  anacFtpClient.getFile("filename");
        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    void getFileKO() throws IOException {
        //given
        when(ftpClient.retrieveFileStream(any())).thenThrow(IOException.class);
        Optional<InputStream> resp =  anacFtpClient.getFile("filename");
        assertTrue(resp.isEmpty());
    }

}
