package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@RunWith(MockitoJUnitRunner.class)
public class SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverterTest {

    private SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter();
    }

    @Test
    public void convertWithExampleDataSource() {
        SmartSenseSubscription source = mock(SmartSenseSubscription.class);

        Long id = Long.MAX_VALUE;
        String subscriptionId = "A-99900000-C-00000000";

        when(source.getId()).thenReturn(id);
        when(source.getSubscriptionId()).thenReturn(subscriptionId);

        SmartSenseSubscriptionJson json = underTest.convert(source);

        assertNotNull("The returning SmartSenseSubscriptionJson should not be null.", json);
        assertEquals("The output ID from the json is not match for the expected.", id, json.getId());
        assertEquals("The output subscription ID from the json is not match for the expected.", subscriptionId, json.getSubscriptionId());
        assertTrue("The autoGenerated value from the json is not match for the expected.", json.isAutoGenerated());
    }

}
