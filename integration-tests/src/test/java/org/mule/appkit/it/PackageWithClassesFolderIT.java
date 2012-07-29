package org.mule.appkit.it;

import java.io.File;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PackageWithClassesFolderIT extends AbstractMavenIT {

    @Override
    protected String getArtifactVersion() {
        return "1.0";
    }

    @Override
    protected String getArtifactId() {
        return "project-with-plain-classes";
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
        String pathToClasses = String.format("target/integration-tests/%1s/target/classes", getArtifactId());
        File classesFolder = (new File(pathToClasses)).getAbsoluteFile();
        assertFileExists(classesFolder);
        assertTrue(classesFolder.isDirectory());
    }
}

