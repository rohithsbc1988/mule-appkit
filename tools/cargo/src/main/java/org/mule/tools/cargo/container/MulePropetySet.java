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

/**
 * Gathers all Mule properties
 */
public interface MulePropetySet {
    /**
     * The active Spring profile
     */
    String SPRING_PROFILE_ACTIVE = "cargo.mule.spring.profile";

    /**
     * The default HTTP port
     */
    String HTTP_PORT = "cargo.mule.http.port";

}
