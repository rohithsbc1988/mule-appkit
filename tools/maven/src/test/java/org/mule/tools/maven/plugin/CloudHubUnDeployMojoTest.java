/**
 * Mule AppKit
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.maven.plugin;

import com.mulesoft.cloudhub.client.Connection;
import com.mulesoft.cloudhub.client.DomainConnection;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.mule.tools.maven.plugin.cloudhub.CloudHubAdapter;

import static org.apache.maven.plugin.testing.ArtifactStubFactory.setVariableValueToObject;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudHubUnDeployMojoTest {

    private CloudHubUnDeployMojo mojo;

    @Before
    public void setUp() throws Exception {

        mojo = new CloudHubUnDeployMojo();

        Connection connection = mock(Connection.class);

        DomainConnection domainConnection = mock(DomainConnection.class);

        when(connection.on(any(String.class))).thenReturn(domainConnection);

        String cloudHubUrl = "https://cloudhub.io/";

        Settings settings = mock(Settings.class);

        Server server = mock(Server.class);

        when(settings.getServer(any(String.class))).thenReturn(server);

        CloudHubAdapter cloudHubAdapter = mock(CloudHubAdapter.class);

        setVariableValueToObject(mojo, "settings", settings);
        setVariableValueToObject(mojo, "cloudHubUrl", cloudHubUrl);
        setVariableValueToObject(mojo, "cloudHubAdapter", cloudHubAdapter);
    }

    @Test
    public void testExecute() throws Exception {
        mojo.execute();
    }
}
