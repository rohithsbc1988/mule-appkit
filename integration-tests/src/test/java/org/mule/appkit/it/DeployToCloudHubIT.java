package org.mule.appkit.it;

import org.junit.Ignore;

import java.io.File;

import static junit.framework.Assert.assertFalse;

@Ignore("Use only to check that CloudHub deploy works. Not for CI.")
public class DeployToCloudHubIT extends AbstractMavenIT {

    @Override
    protected String getArtifactVersion() {
        return "1.0";
    }

    @Override
    protected String getArtifactId() {
        return "deploy-to-cloudhub";
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
        String classesFolder = String.format("target/integration-tests/%1s/target/classes", getArtifactId());
        assertFalse((new File(classesFolder)).getAbsoluteFile().exists());
    }
}

