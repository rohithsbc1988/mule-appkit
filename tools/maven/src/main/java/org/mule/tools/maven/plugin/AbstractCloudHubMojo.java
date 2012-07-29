package org.mule.tools.maven.plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mulesoft.cloudhub.client.Connection;
import com.mulesoft.cloudhub.client.DomainConnection;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/**
 * Base class for CloudHub Mojos
 */
public abstract class AbstractCloudHubMojo extends AbstractMojo {

    private static final String URL_LAYOUT = ".*://(.*)(/+)";

    /**
     * @parameter expression="${cloudhub.url}" default-value="https://cloudhub.io/"
     * @required
     */
    protected String cloudHubUrl;

    /**
     * @parameter expression="${cloudhub.domain}"
     * @required
     */
    protected String domain;

    /**
     * @parameter expression="${cloudhub.username}"
     */
    protected String username;

    /**
     * @parameter expression="${cloudhub.password}"
     */
    protected String password;

    /**
     * @parameter default-value="${settings}"
     * @readonly
     */
    private Settings settings;

    protected final Server getServer() throws MojoExecutionException {
        return this.settings.getServer(normalize(this.cloudHubUrl));
    }

    protected final String normalize(final String url) throws MojoExecutionException {
        final Pattern pattern = Pattern.compile(AbstractCloudHubMojo.URL_LAYOUT);
        final Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            throw new MojoExecutionException("Invalid URL <"+url+">");
        }
        return matcher.group(1);
    }

    protected final String getUsername() throws MojoExecutionException {
        if (this.username != null) {
            return this.username;
        }

        final Server server = getServer();
        if (server == null) {
            throw new MojoExecutionException("Failed to extract username from server settings");
        }
        return server.getUsername();
    }

    protected final String getPassword() throws MojoExecutionException {
        if (this.password != null) {
            return this.password;
        }

        final Server server = getServer();
        if (server == null) {
            throw new MojoExecutionException("Failed to extract password from server settings");
        }
        return server.getPassword();
    }

    protected final Connection createConnection() throws MojoExecutionException {
        return new Connection(this.cloudHubUrl, getUsername(), getPassword());
    }

    protected final DomainConnection createDomainConnection() throws MojoExecutionException {
        return createConnection().on(this.domain);
    }

}
