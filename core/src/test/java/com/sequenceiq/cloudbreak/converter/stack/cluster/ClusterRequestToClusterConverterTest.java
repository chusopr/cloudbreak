package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

public class ClusterRequestToClusterConverterTest extends AbstractJsonConverterTest<ClusterRequest> {

    @InjectMocks
    private ClusterRequestToClusterConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new ClusterRequestToClusterConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(GatewayJson.class), eq(Gateway.class))).willReturn(new Gateway());
        // WHEN
        Cluster result = underTest.convert(getRequest("cluster.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "kerberosConfig", "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText"));
    }

    @Test
    public void testConvertWithFileSystemDetails() {
        // GIVEN
        Gateway gateway = new Gateway();
        given(conversionService.convert(any(GatewayJson.class), eq(Gateway.class))).willReturn(gateway);
        given(conversionService.convert(any(KerberosRequest.class), eq(KerberosConfig.class))).willReturn(new KerberosConfig());
        given(conversionService.convert(any(FileSystemRequest.class), eq(FileSystem.class))).willReturn(new FileSystem());
        // WHEN
        Cluster result = underTest.convert(getRequest("cluster-with-file-system.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText"));
    }

    @Test
    public void testNoGateway() {
        // GIVEN
        // WHEN
        ClusterRequest clusterRequest = getRequest("cluster-no-gateway.json");
        Cluster result = underTest.convert(clusterRequest);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "kerberosConfig", "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText", "gateway"));
        assertNull(result.getGateway());
    }

    @Test
    public void testGatewayPathConversionWhenNoPathInGatewayJson() {
        Gateway gateway = new Gateway();
        given(conversionService.convert(any(GatewayJson.class), eq(Gateway.class))).willReturn(gateway);
        ClusterRequest clusterRequest = getRequest("cluster-with-file-system.json");

        Cluster result = underTest.convert(clusterRequest);

        assertEquals(result.getName(), result.getGateway().getPath());
        assertEquals('/' + result.getGateway().getPath() + "/sso/api/v1/websso", result.getGateway().getSsoProvider());
    }

    @Test
    public void testGatewayPathConversionWhenPathIsInGatewayJson() {
        String gatewayPath = "gatewayPath";
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setPath(gatewayPath);
        given(conversionService.convert(any(GatewayJson.class), eq(Gateway.class))).willReturn(new Gateway());
        ClusterRequest clusterRequest = getRequest("cluster-with-file-system.json");
        clusterRequest.setGateway(gatewayJson);

        Cluster result = underTest.convert(clusterRequest);

        assertEquals(gatewayPath, result.getGateway().getPath());
        assertEquals('/' + result.getGateway().getPath() + "/sso/api/v1/websso", result.getGateway().getSsoProvider());
    }

    @Override
    public Class<ClusterRequest> getRequestClass() {
        return ClusterRequest.class;
    }
}