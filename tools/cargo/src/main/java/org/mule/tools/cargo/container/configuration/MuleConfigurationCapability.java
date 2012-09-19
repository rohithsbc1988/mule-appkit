/**
 * Mule AppKit
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.cargo.container.configuration;

import java.util.HashMap;
import java.util.Map;
import org.codehaus.cargo.container.spi.configuration.AbstractConfigurationCapability;
import org.mule.tools.cargo.container.MulePropetySet;

public class MuleConfigurationCapability extends AbstractConfigurationCapability {

    private Map<String, Boolean> propertyMap = new HashMap<String, Boolean>();

    public MuleConfigurationCapability() {
        propertyMap.put(MulePropetySet.SPRING_PROFILE_ACTIVE, Boolean.TRUE);
        propertyMap.put(MulePropetySet.HTTP_PORT, Boolean.TRUE);
    }

    @Override
    protected Map<String, Boolean> getPropertySupportMap() {
        return propertyMap;
    }

}