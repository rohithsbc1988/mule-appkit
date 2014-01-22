/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven.plugin;

import static java.lang.String.format;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.Logger;

/**
 * Build a Mule domain archive.
 *
 * @phase package
 * @goal mule-domain
 * @requiresDependencyResolution runtime
 */
public class MuleDomainMojo extends AbstractMuleMojo
{

    /**
     * If set to <code>true</code> attempt to copy the Mule application zip to $MULE_HOME/apps
     *
     * @parameter alias="bundleApps" expression="${bundleApps}" default-value="false"
     * @required
     */
    protected boolean bundleApps;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * Whether a JAR file will be created for the classes in the app. Using this optional
     * configuration parameter will make the generated classes to be archived into a jar file
     * and the classes directory will then be excluded from the app.
     *
     * @parameter expression="${archiveClasses}" default-value="false"
     */
    private boolean archiveClasses;

    /**
     * List of exclusion elements (having groupId and artifactId children) to exclude from the
     * application archive.
     *
     * @parameter
     * @since 1.2
     */
    private List<Exclusion> exclusions;

    /**
     * List of inclusion elements (having groupId and artifactId children) to exclude from the
     * application archive.
     *
     * @parameter
     * @since 1.5
     */
    private List<Inclusion> inclusions;

    /**
     * Exclude all artifacts with Mule groupIds. Default is <code>true</code>.
     *
     * @parameter default-value="true"
     * @since 1.4
     */
    private boolean excludeMuleDependencies;

    /**
     * @parameter default-value="false"
     * @since 1.7
     */
    private boolean filterAppDirectory;

    /**
     * @parameter default-value="false"
     * @since 1.8
     */
    private boolean prependGroupId;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        File domain = getMuleZipFile();
        try
        {
            createMuleDomain(domain);
        }
        catch (ArchiverException e)
        {
            throw new MojoExecutionException("Exception creating the Mule App", e);
        }

        this.project.setFile(domain);
        this.project.getArtifact().setFile(domain);
        this.projectHelper.attachArtifact(this.project, "zip", domain);
    }

    protected Logger createLogger()
    {
        //TODO see how to fix this
        return new Logger()
        {
            public void debug(String s)
            {
            }

            public void debug(String s, Throwable throwable)
            {
            }

            public boolean isDebugEnabled()
            {
                return false;
            }

            public void info(String s)
            {
            }

            public void info(String s, Throwable throwable)
            {
            }

            public boolean isInfoEnabled()
            {
                return false;
            }

            public void warn(String s)
            {
            }

            public void warn(String s, Throwable throwable)
            {
            }

            public boolean isWarnEnabled()
            {
                return false;
            }

            public void error(String s)
            {
            }

            public void error(String s, Throwable throwable)
            {
            }

            public boolean isErrorEnabled()
            {
                return false;
            }

            public void fatalError(String s)
            {
            }

            public void fatalError(String s, Throwable throwable)
            {
            }

            public boolean isFatalErrorEnabled()
            {
                return false;
            }

            public Logger getChildLogger(String s)
            {
                return null;
            }

            public int getThreshold()
            {
                return 0;
            }

            public String getName()
            {
                return null;
            }
        };
    }

    protected void createMuleDomain(final File domain) throws MojoExecutionException, ArchiverException
    {
        //PLG - not for now
        //validateProject();

        MuleArchiver archiver = new MuleArchiver(prependGroupId);
        //addAppDirectory(archiver);
        //addCompiledClasses(archiver);
        addDependencies(archiver);
        addResourcesFile(archiver);
        addDomainFile(archiver);
        //addMappingsDirectory(archiver);
        archiver.setDestFile(domain);

        try
        {
            domain.delete();
            archiver.createArchive();
        }
        catch (IOException e)
        {
            getLog().error("Cannot create archive", e);
        }
    }

    protected void addAppsZipFilesToArchive(ZipArchiver archiver, File appsFolder) throws MojoExecutionException
    {
        if (appsFolder.exists())
        {
            File[] directories = appsFolder.listFiles();
            for (File file : directories)
            {
                if (file.isDirectory())
                {
                    File targetFolder = new File(file, "target");
                    if (!targetFolder.exists())
                    {
                        throw new MojoExecutionException("Cannot bound app " + file.getName() + ". Seems it was not build");
                    }
                    File[] zipFiles = targetFolder.listFiles(new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.endsWith(".zip");
                        }
                    });
                    if (zipFiles.length == 0)
                    {
                        throw new MojoExecutionException("No application zip in project " + file.getName());
                    }
                    File muleApp = zipFiles[0];
                    try
                    {
                        archiver.addFile(muleApp, "apps" + File.separator + muleApp.getName());
                    }
                    catch (ArchiverException e)
                    {
                        throw new MojoExecutionException("Error adding domain application " + muleApp.getName(),e);
                    }
                }
            }
        }
        else
        {
            throw new RuntimeException(appsFolder.getAbsolutePath() + " does not exists");
        }
    }

    private void addDomainFile(MuleArchiver archiver) throws ArchiverException
    {
        archiver.addResources(domainDirectory);
    }

    private void addResourcesFile(MuleArchiver archiver) throws ArchiverException
    {
        archiver.addResources(resourcesDirectory);
    }

    private void validateProject() throws MojoExecutionException
    {
        File muleConfig = new File(appDirectory, "mule-config.xml");
        File deploymentDescriptor = new File(appDirectory, "mule-deploy.properties");

        if ((muleConfig.exists() == false) && (deploymentDescriptor.exists() == false))
        {
            String message = format("No mule-config.xml or mule-deploy.properties in %1s",
                                    this.project.getBasedir());

            getLog().error(message);
            throw new MojoExecutionException(message);
        }
    }

    private void addAppDirectory(MuleArchiver archiver) throws ArchiverException
    {
        if (filterAppDirectory)
        {
            archiver.addResources(getFilteredAppDirectory());
        }
        else
        {
            archiver.addResources(appDirectory);
        }
    }

    private void addCompiledClasses(MuleArchiver archiver) throws ArchiverException, MojoExecutionException
    {
        if (this.archiveClasses == false)
        {
            addClassesFolder(archiver);
        }
        else
        {
            addArchivedClasses(archiver);
        }
    }

    private void addClassesFolder(MuleArchiver archiver) throws ArchiverException
    {
        if (this.classesDirectory.exists())
        {
            getLog().info("Copying classes directly");
            archiver.addClasses(this.classesDirectory, null, null);
        }
        else
        {
            getLog().info(this.classesDirectory + " does not exist, skipping");
        }
    }

    private void addMappingsDirectory(MuleArchiver archiver) throws ArchiverException
    {
        if (this.mappingsDirectory.exists())
        {
            getLog().info("Copying mappings");
            archiver.addResources(this.mappingsDirectory);
        }
        else
        {
            getLog().info(this.mappingsDirectory + " does not exist, skipping");
        }
    }

    private void addArchivedClasses(MuleArchiver archiver) throws ArchiverException, MojoExecutionException
    {
        if (this.classesDirectory.exists() == false)
        {
            getLog().info(this.classesDirectory + " does not exist, skipping");
            return;
        }

        getLog().info("Copying classes as a jar");

        final JarArchiver jarArchiver = new JarArchiver();
        jarArchiver.addDirectory(this.classesDirectory, null, null);
        final File jar = new File(this.outputDirectory, this.finalName + ".jar");
        jarArchiver.setDestFile(jar);
        try
        {
            jarArchiver.createArchive();
            archiver.addLib(jar);
        }
        catch (IOException e)
        {
            final String message = "Cannot create project jar";
            getLog().error(message, e);
            throw new MojoExecutionException(message, e);
        }
    }

    private void addDependencies(MuleArchiver archiver) throws ArchiverException
    {
        for (Artifact artifact : getArtifactsToArchive())
        {
            String message = format("Adding <%1s> as a lib", artifact.getId());
            getLog().info(message);
            archiver.addLibraryArtifact(artifact);
        }
    }

    private Set<Artifact> getArtifactsToArchive()
    {
        ArtifactFilter filter = new ArtifactFilter(this.project, this.inclusions,
            this.exclusions, this.excludeMuleDependencies);
        return filter.getArtifactsToArchive();
    }
}
