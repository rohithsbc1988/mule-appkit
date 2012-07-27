package org.mule.appkit.it;

import java.io.File;

public class FilteringDisabledByDefaultIT extends AbstractMavenIT {

    protected String getArtifactVersion() {
        return "1.0";
    }

    protected String getArtifactId() {
        return "filtering-disabled-by-default";
    }

    protected String getGroupId() {
        return "org.mule.appkit.it";
    }

    protected File getRoot() {
        return new File("target/integration-tests/" + getArtifactId());
    }
}
