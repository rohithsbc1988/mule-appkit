/**
 * Mule AppKit
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.cargo.container;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.mule.tools.cargo.deployable.MuleApplicationDeployable;

/**
 * {@link ContainerCapability} supporting {@link org.mule.tools.cargo.deployable.MuleApplicationDeployable}.
 */
public class MuleContainerCapability implements ContainerCapability {

    @Override
    public boolean supportsDeployableType(final DeployableType type) {
        return MuleApplicationDeployable.TYPE.equals(type);
    }

}