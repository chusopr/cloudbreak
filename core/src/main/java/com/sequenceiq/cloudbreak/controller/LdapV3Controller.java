package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotEmpty;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.LdapConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.ldap.LDAPTestRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapValidationRequest;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(LdapConfig.class)
public class LdapV3Controller extends NotificationController implements LdapConfigV3Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Override
    public Set<LdapConfigResponse> listConfigsByWorkspace(Long workspaceId, String environment, Boolean attachGlobal) {
        return ldapConfigService.findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal).stream()
                .map(ldapConfig -> conversionService.convert(ldapConfig, LdapConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public LdapConfigResponse getByNameInWorkspace(Long workspaceId, String name) {
        LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(ldapConfig, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse createInWorkspace(Long workspaceId, LdapConfigRequest request) {
        LdapConfig ldapConfig = conversionService.convert(request, LdapConfig.class);
        ldapConfig = ldapConfigService.createInEnvironment(ldapConfig, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.LDAP_CREATED);
        return conversionService.convert(ldapConfig, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse deleteInWorkspace(Long workspaceId, String name) {
        LdapConfig config = ldapConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.LDAP_DELETED);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public LdapTestResult testLdapConnection(Long workspaceId, LDAPTestRequest ldapValidationRequest) {
        String existingLDAPConfigName = ldapValidationRequest.getName();
        LdapValidationRequest validationRequest = ldapValidationRequest.getValidationRequest();
        if (existingLDAPConfigName == null && validationRequest == null) {
            throw new BadRequestException("Either an existing resource 'name' or an LDAP 'validationRequest' needs to be specified in the request. ");
        }

        LdapTestResult ldapTestResult = new LdapTestResult();
        try {
            if (existingLDAPConfigName != null) {
                LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspaceId(existingLDAPConfigName, workspaceId);
                ldapConfigValidator.validateLdapConnection(ldapConfig);
            } else {
                ldapConfigValidator.validateLdapConnection(validationRequest);
            }
            ldapTestResult.setConnectionResult("connected");
        } catch (BadRequestException e) {
            ldapTestResult.setConnectionResult(e.getMessage());
        }
        return ldapTestResult;
    }

    @Override
    public LdapConfigRequest getRequestFromName(Long workspaceId, String name) {
        LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(ldapConfig, LdapConfigRequest.class);
    }

    @Override
    public LdapConfigResponse attachToEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return ldapConfigService.attachToEnvironmentsAndConvert(name, environmentNames, workspaceId, LdapConfigResponse.class);
    }

    @Override
    public LdapConfigResponse detachFromEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return ldapConfigService.detachFromEnvironmentsAndConvert(name, environmentNames, workspaceId, LdapConfigResponse.class);
    }
}
