package org.mule.appkit.it;

import org.apache.commons.io.IOUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.IOUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static junit.framework.Assert.assertNotNull;

public abstract class AbstractMavenIT {

    private static final boolean DEBUG = false;

    protected abstract String getArtifactVersion();

    protected abstract String getArtifactId();

    protected abstract String getGroupId();

    protected abstract File getRoot();

    @Before
    public void setUp() throws VerificationException, IOException {
        Verifier verifier = new Verifier(getRoot().getAbsolutePath());

        // Deleting a former created artefact from the archetype to be tested
        verifier.deleteArtifact(getGroupId(), getArtifactId(), getArtifactVersion(), null);

        // Delete the created maven project
        verifier.deleteDirectory(getArtifactId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildExecutable() throws Exception {
        try {
            Verifier verifier = new Verifier(getRoot().getAbsolutePath(), null, DEBUG, true);
            verifier.setAutoclean(true);

            setSystemProperties(verifier);

            Map<String, String> envVars = new HashMap<String, String>();
            envVars.put("MAVEN_OPTS", "-Xmx512m -XX:MaxPermSize=256m");

            verifier.executeGoal("package", envVars);

            verifier.verifyErrorFreeLog();
        } catch (IOException ioe) {
            throw new VerificationException(ioe);
        }
    }

    protected void setSystemProperties(Verifier verifier) throws IOException {
        InputStream systemPropertiesStream = null;
        try {
            systemPropertiesStream = getClass().getClassLoader().getResourceAsStream("maven.properties");
            Properties systemProperties = new Properties();
            systemProperties.load(systemPropertiesStream);
            verifier.setSystemProperties(systemProperties);
        } finally {
            IOUtils.closeQuietly(systemPropertiesStream);
        }
    }

    private String contentsOfMuleConfigFromZipFile(File muleAppZipFile) throws Exception
    {
        ZipFile zipFile = null;
        InputStream muleConfigStream = null;
        try
        {
            zipFile = new ZipFile(muleAppZipFile);

            ZipEntry muleConfigEntry = zipFile.getEntry("mule-config.xml");
            assertNotNull(muleConfigEntry);

            muleConfigStream = zipFile.getInputStream(muleConfigEntry);
            return IOUtil.toString(muleConfigStream);
        }
        finally
        {
            if (zipFile != null)
            {
                zipFile.close();
            }
            if (muleConfigStream != null)
            {
                muleConfigStream.close();
            }
        }
    }

}