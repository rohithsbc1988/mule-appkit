package org.mule.tools.cargo.container.configuration;

import java.util.HashMap;
import java.util.Map;
import org.codehaus.cargo.container.spi.configuration.AbstractConfigurationCapability;
import org.mule.tools.cargo.container.MulePropetySet;

public class MuleConfigurationCapability extends AbstractConfigurationCapability {

    private Map<String, Boolean> propertyMap = new HashMap<String, Boolean>();

    public MuleConfigurationCapability() {
        propertyMap.put(MulePropetySet.SPRING_PROFILE_ACTIVE, Boolean.TRUE);
    }

    @Override
    protected Map<String, Boolean> getPropertySupportMap() {
        return propertyMap;
    }

}