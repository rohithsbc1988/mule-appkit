/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven.plugin.cloudhub;

import com.mulesoft.cloudhub.client.Connection;
import com.mulesoft.cloudhub.client.DomainConnection;

import java.io.File;
import java.util.Map;

public class DefaultCloudHubAdapter implements CloudHubAdapter {

    private DomainConnection connectionDomain;

    public DefaultCloudHubAdapter() {
    }

    @Override
    public void create(String cloudHubUrl, String username, String password, String domain) {
        connectionDomain = new Connection(cloudHubUrl,username,password).on(domain);
    }

    @Override
    public void deploy(File file, String muleVersion, int workers, long maxWaitTime, Map<String, String> properties) {
        connectionDomain.deploy(file,muleVersion,workers,maxWaitTime,properties);
    }

}
