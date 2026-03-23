package it.pagopa.selfcare.party.connector.ftp.client;

import com.jcraft.jsch.*;
import it.pagopa.selfcare.party.registry_proxy.connector.api.FTPConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@Slf4j
@PropertySource("classpath:config/ftp-config.properties")
public class AnacFTPClient implements FTPConnector {

    private final String address;
    private final String user;
    private final String password;
    private final Integer port;
    private final String knownHost;

    public AnacFTPClient(@Value("${anac.ftp.ip}") String address,
                         @Value("${anac.ftp.port}") Integer port,
                         @Value("${anac.ftp.user}") String user,
                         @Value("${anac.ftp.password}") String password,
                         @Value("${anac.ftp.known-host}") String knownHost) {

        this.address = address;
        this.password = password;
        this.user = user;
        this.port = port;
        this.knownHost = knownHost;

    }

    @Override
    public Optional<InputStream> getFile(String fileName) {
        log.info("START retrieve {} from ftp", fileName);
        Optional<InputStream> optionalInputStream = Optional.empty();
        ChannelSftp channelSftp = new ChannelSftp();
        try {
            channelSftp = setupJsch();
            optionalInputStream = connectAndRetrieveFile(channelSftp, fileName);
        } catch (JSchException | SftpException | IOException e) {
            log.error("Error during get file from ANAC ftp: {}", e.getMessage());
            log.trace("Error during get file from ANAC ftp", e);
        } finally {
            channelSftp.exit();
        }
        log.info("END retrieve {} from ftp", fileName);
        return optionalInputStream;
    }

    private Optional<InputStream> connectAndRetrieveFile(ChannelSftp channelSftp, String fileName) throws JSchException, SftpException, IOException {
        channelSftp.connect(30000);
        InputStream inputStream = channelSftp.get(fileName);
        InputStream finalInputStream = new ByteArrayInputStream(inputStream.readAllBytes());
        return Optional.of(finalInputStream);
    }

    private ChannelSftp setupJsch() throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts(new ByteArrayInputStream(knownHost.getBytes(StandardCharsets.UTF_8)));
        Session jschSession = jsch.getSession(user, address, port);
        jschSession.setPassword(password);
        jschSession.connect();
        return (ChannelSftp) jschSession.openChannel("sftp");

    }
}