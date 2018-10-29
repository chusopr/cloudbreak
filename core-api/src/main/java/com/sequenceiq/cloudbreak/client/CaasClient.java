package com.sequenceiq.cloudbreak.client;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;

public class CaasClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaasClient.class);

    private final String caasProtocol;

    private final String caasDomain;

    private final ConfigKey configKey;

    public CaasClient(String caasProtocol, String caasDomain, ConfigKey configKey) {
        this.caasProtocol = caasProtocol;
        this.caasDomain = caasDomain;
        this.configKey = configKey;
    }

    @Cacheable(cacheNames = "caasUserCache")
    public CaasUser getUserInfo(String dpsJwtToken) {
        WebTarget caasWebTarget = getCaasWebTarget();
        WebTarget userInfoWebTarget = caasWebTarget.path("/oidc/userinfo");
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Cookie", "dps-jwt=" + dpsJwtToken);
        return userInfoWebTarget.request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .headers(headers)
                .get(CaasUser.class);
    }

    public IntrospectResponse introSpect(String dpsJwtToken) {
        WebTarget caasWebTarget = getCaasWebTarget();
        WebTarget introspectWebTarget = caasWebTarget.path("/oidc/introspect");
        return introspectWebTarget.request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new IntrospectRequest(dpsJwtToken)), IntrospectResponse.class);
    }

    public String getToken(String user, String password) {
        WebTarget webTarget = getCaasWebTarget();
        WebTarget authWebTarget = webTarget.path("auth/in?redirect_uri=" + caasProtocol + "://" + caasDomain + "/oidc/userinfo");
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Authentication", getBasicAuthentication(user, password));
        return authWebTarget.request()
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE)
                .headers(headers)
                .get()
                .getCookies()
                .get("dps-jwt")
                .toString();
    }

    private String getBasicAuthentication(String user, String password) {
        String token = user + ":" + password;
        try {
            return "Basic " + Base64.getEncoder().encode(token.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Cannot encode with UTF-8", ex);
        }
    }

    private WebTarget getCaasWebTarget() {
        if (StringUtils.isNotEmpty(caasDomain)) {
            return RestClientUtil.get(configKey).target(caasProtocol + "://" + caasDomain);
        } else {
            LOGGER.warn("CAAS isn't configured");
            throw new InvalidTokenException("CAAS isn't configured");
        }
    }
}