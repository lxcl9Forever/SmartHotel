package com.smarthotel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "smarthotel.alipay")
public class AlipayConfig {
    private boolean enabled;
    private String gateway;
    private String appId;
    private String privateKeyFile;
    private String publicKeyFile;
    private String notifyUrl;
    private String sellerId;
}
