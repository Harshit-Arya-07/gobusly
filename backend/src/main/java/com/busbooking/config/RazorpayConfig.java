package com.busbooking.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures a singleton {@link RazorpayClient} bean.
 * <p>
 * Previously, a new {@code RazorpayClient} was created on every payment
 * order request. This bean ensures the client is instantiated once and
 * reused, avoiding unnecessary object creation and connection overhead.
 * </p>
 */
@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${app.payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${app.payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        if (razorpayKeyId == null || razorpayKeyId.isBlank()
                || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            log.warn("Razorpay keys are not configured. Payment features will be unavailable.");
            // Return a client with empty keys; the service layer validates before use.
            return new RazorpayClient("placeholder_key", "placeholder_secret");
        }

        log.info("Initializing RazorpayClient with key-id: {}...", razorpayKeyId.substring(0, Math.min(12, razorpayKeyId.length())));
        return new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }
}
