package org.mule.appkit.it;

import java.io.File;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PackageWithArchivedClassesIT extends AbstractMavenIT {

    @Override
    protected String getArtifactVersion() {
        return "1.0";
    }

    @Override
    protected String getArtifactId() {
        return "project-with-archived-classes";
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
        String classesArchive = String.format("target/integration-tests/%1s/target/%2s-1.0-SNAPSHOT.jar",
                getArtifactId(), getArtifactId());
        assertFileExists((new File(classesArchive)).getAbsoluteFile());
    }
}

