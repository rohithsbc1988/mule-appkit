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

import java.io.File;
import org.codehaus.cargo.container.spi.deployable.AbstractDeployable;

public abstract class AbstractMuleDeployable extends AbstractDeployable {

    public AbstractMuleDeployable(final String file) {
        super(file);
    }

    /**
     * @return name of this application extracted from file name
     */
    public final String getApplicationName() {
        final String fileName = getFile();
        return fileName.substring(fileName.lastIndexOf(File.separator)+1, fileName.lastIndexOf("."));
    }

}