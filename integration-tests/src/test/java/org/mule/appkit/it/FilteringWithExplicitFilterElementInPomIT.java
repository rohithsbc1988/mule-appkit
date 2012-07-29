package org.mule.appkit.it;

import java.io.File;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class FilteringWithExplicitFilterElementInPomIT extends AbstractMavenIT {

    @Override
    protected String getArtifactVersion() {
        return "1.0";
    }

    @Override
    protected String getArtifactId() {
        return "filtering-explicit-filter-element";
    }

    @Override
    protected String getGroupId() {
        return "org.mule.appkit.it";
    }

    @Override
    protected File getRoot() {
        return new File("target/integration-tests/" + getArtifactId());
    }

    @Override
    protected void verify() throws Exception {
        File appZip = zipFileFromBuildingProject();
        String muleConfig = contentsOfMuleConfigFromZipFile(appZip);
        assertFalse(muleConfig.contains("${thePort}"));
        assertTrue(muleConfig.contains("http://localhost:8888/"));
    }
}

