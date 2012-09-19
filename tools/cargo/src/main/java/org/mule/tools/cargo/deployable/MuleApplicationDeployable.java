/**
 * Mule AppKit
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.cargo.deployable;

import org.codehaus.cargo.container.deployable.DeployableType;

/**
 * A mule application deployable. Matches http://www.mulesoft.org/documentation/display/MMP/Home packaging type.
 */
public class MuleApplicationDeployable extends AbstractMuleDeployable  {

    public static final DeployableType TYPE = DeployableType.toType("zip");

    public MuleApplicationDeployable(final String file) {
        super(file);
    }

    @Override
    public DeployableType getType() {
        return MuleApplicationDeployable.TYPE;
    }

}