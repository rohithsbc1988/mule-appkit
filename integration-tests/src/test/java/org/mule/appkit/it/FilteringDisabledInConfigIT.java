package org.mule.appkit.it;

import java.io.File;

import static junit.framework.Assert.assertTrue;

public class FilteringDisabledInConfigIT extends AbstractMavenIT {

    @Override
    protected String getArtifactVersion() {
        return "1.0";
    }

    @Override
    protected String getArtifactId() {
        return "filtering-disabled-in-config";
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
        assertTrue(muleConfig.contains("${thePort}"));
    }
}
