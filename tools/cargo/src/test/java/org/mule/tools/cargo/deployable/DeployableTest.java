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

import org.junit.Assert;
import org.junit.Test;

public class DeployableTest  {

    @Test
    public void nameShouldNotContainExtension() {
        final String name = "name";
        final String fullZipName = name+".zip";
        Assert.assertEquals(new MuleApplicationDeployable(fullZipName).getApplicationName(), name);
        Assert.assertEquals(new MuleApplicationDeployable(fullZipName).getFile(), fullZipName);
    }

}