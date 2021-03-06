package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.OPENSTACK;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.YARN;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.CUMULUS_YARN;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class TemplateToTemplateResponseConverter extends AbstractConversionServiceAwareConverter<Template, TemplateResponse> {
    @Override
    public TemplateResponse convert(Template source) {
        TemplateResponse templateJson = new TemplateResponse();
        templateJson.setId(source.getId());
        templateJson.setName(source.getName());
        templateJson.setVolumeCount(source.getVolumeCount());
        templateJson.setVolumeSize(source.getVolumeSize());
        templateJson.setInstanceType(source.getInstanceType());
        templateJson.setVolumeType(source.getVolumeType());
        templateJson.setRootVolumeSize(source.getRootVolumeSize());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            Map<String, Object> atributesMap = attributes.getMap();
            templateJson.setParameters(atributesMap);

            Optional.ofNullable(source.getSecretAttributes()).ifPresent(attr -> atributesMap.putAll(attr.getMap()));
            Map<String, Object> map = atributesMap;

            setParameterByPlatform(templateJson, atributesMap, map);
        }
        templateJson.setCloudPlatform(source.cloudPlatform());
        templateJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        if (source.getTopology() != null) {
            templateJson.setTopologyId(source.getTopology().getId());
        }
        return templateJson;
    }

    private void setParameterByPlatform(TemplateResponse templateJson, Map<String, Object> atributesMap, Map<String, Object> map) {
        Object platformType = map.get(BaseTemplateParameter.PLATFORM_TYPE);
        if (platformType != null) {
            switch (platformType.toString()) {
                case AWS:
                    templateJson.setAwsParameters(getConversionService().convert(atributesMap, AwsParameters.class));
                    break;
                case OPENSTACK:
                    templateJson.setOpenStackParameters(getConversionService().convert(atributesMap, OpenStackParameters.class));
                    break;
                case AZURE:
                    templateJson.setAzureParameters(getConversionService().convert(atributesMap, AzureParameters.class));
                    break;
                case GCP:
                    templateJson.setGcpParameters(getConversionService().convert(atributesMap, GcpParameters.class));
                    break;
                case YARN:
                case CUMULUS_YARN:
                    templateJson.setYarnParameters(getConversionService().convert(atributesMap, YarnParameters.class));
                    break;
                default:
            }
        }
    }
}
