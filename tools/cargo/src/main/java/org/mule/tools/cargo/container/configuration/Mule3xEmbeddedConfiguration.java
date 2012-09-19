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

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.spi.configuration.AbstractLocalConfiguration;

/**
 * Encapsulates Mule 3.x specific configuration details.
 */
public class Mule3xEmbeddedConfiguration extends AbstractLocalConfiguration {

    public Mule3xEmbeddedConfiguration(final String home) {
        super(home);
    }

    @Override
    protected void doConfigure(final LocalContainer container) throws Exception {
    }

    @Override
    public ConfigurationCapability getCapability() {
        return new MuleConfigurationCapability();
    }

    @Override
    public ConfigurationType getType() {
        return ConfigurationType.STANDALONE;
    }

}