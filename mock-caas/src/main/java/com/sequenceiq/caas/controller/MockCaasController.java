package com.sequenceiq.caas.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.client.CaasUser;
import com.sequenceiq.cloudbreak.client.IntrospectResponse;

@RestController
public class MockCaasController {

    @GetMapping("/oidc/userinfo")
    public CaasUser getUserInfo() {
        return null;
    }

    @PostMapping("/oidc/introspect")
    public IntrospectResponse introSpect(String token) {
        return null;
    }

    @GetMapping("/auth/in")
    public void auth(HttpServletResponse httpServletResponse, @RequestParam("redirect_uri") String requestUri) {
        httpServletResponse.setHeader("Location", requestUri);
        httpServletResponse.setStatus(302);
    }

}
