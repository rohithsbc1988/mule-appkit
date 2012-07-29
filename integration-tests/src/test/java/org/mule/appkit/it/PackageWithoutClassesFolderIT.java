package org.mule.appkit.it;

import java.io.File;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PackageWithoutClassesFolderIT extends AbstractMavenIT {

    @Override
    protected String getArtifactVersion() {
        return "1.0";
    }

    @Override
    protected String getArtifactId() {
        return "project-without-classes";
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

