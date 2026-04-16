package it.pagopa.selfcare.auth.model.otp;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Data
public class OtpDailyLimit {

    @ConfigProperty(name = "otp.daily.limit", defaultValue = "0")
    Integer dailyLimit;
}
