package org.mule.appkit.it;

import java.io.File;

import static junit.framework.Assert.assertTrue;

public class ExcludeDirectDependencyIT extends AbstractMavenIT {

    @Override
    protected String getArtifactVersion() {
        return "1.0";
    }

    @Override
    protected String getArtifactId() {
        return "exclude-direct-dependency";
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
        File zipFile = zipFileFromBuildingProject();

        String muleCoreLib = "lib/mule-core-2.2.1.jar";
        String beanutilsLib = "lib/commons-beanutils-1.7.0-osgi.jar"; // this is a transitive dependency of mule-core
        assertZipDoesNotContain(zipFile, muleCoreLib, beanutilsLib);
    }
}
