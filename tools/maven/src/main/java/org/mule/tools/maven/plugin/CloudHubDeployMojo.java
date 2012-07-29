package org.mule.tools.maven.plugin;

import java.io.File;
import java.util.Map;

import com.mulesoft.cloudhub.client.DomainConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public class CloudHubDeployMojo extends AbstractCloudHubMojo {

    private static final String MULE_TYPE = "mule";

    /**
     * @parameter expression="${cloudhub.workers}" default-value="1"
     */
    protected int workers;

    /**
     * @parameter expression="${cloudhub.muleVersion}" default-value="3.3.0"
     */
    protected String muleVersion;

    /**
     * @parameter expression="${cloudhub.maxWaitTime}" default-value="120000"
     */
    protected long maxWaitTime;

    /**
     * @parameter
     */
    protected Map<String, String> properties;
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        final String type = this.project.getArtifact().getType();
        if (!MULE_TYPE.equals(type)) {
            throw new IllegalArgumentException("Only supports mule packaging type, not <"+type+">.");
        }

        if (this.project.getAttachedArtifacts().isEmpty()) {
            throw new IllegalArgumentException("No Mule application attached. This probably means `package` phase has not been executed.");
        }

        /*
        final File file = this.project.getAttachedArtifacts().get(0).getFile();
        getLog().info("Deploying <"+file+">");

        final DomainConnection domainConnection = createDomainConnection();
        domainConnection.deploy(file, this.muleVersion, this.workers, this.maxWaitTime, this.properties);
        */
    }
}