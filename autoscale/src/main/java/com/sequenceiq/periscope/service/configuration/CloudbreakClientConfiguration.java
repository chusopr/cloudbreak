package com.sequenceiq.periscope.service.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.CloudbreakClient.CloudbreakClientBuilder;

@Configuration
public class CloudbreakClientConfiguration {

    @Autowired
    @Qualifier("cloudbreakUrl")
    private String cloudbreakUrl;

    @Value("${cb.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Autowired
    @Qualifier("TODO")
    private String caasProtocol;

    @Value("${TODO}")
    private String caasAddress;

    @Bean
    public CloudbreakClient cloudbreakClient() {
        return new CloudbreakClientBuilder(cloudbreakUrl + cbRootContextPath, caasAddress, caasProtocol).build();
    }
}
