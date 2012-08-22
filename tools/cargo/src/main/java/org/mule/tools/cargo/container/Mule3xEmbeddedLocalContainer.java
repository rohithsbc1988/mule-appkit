package org.mule.tools.cargo.container;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.spi.AbstractEmbeddedLocalContainer;
import org.mule.MuleServer;
import org.mule.module.launcher.DeploymentListener;
import org.mule.tools.cargo.deployable.AbstractMuleDeployable;
import org.mule.tools.cargo.deployable.MuleApplicationDeployable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Start an embedded {@link MuleServer} using maven dependencies.
 * <br />
 * Configured {@link MuleApplicationDeployable} is deployed on startup.
 */
public class Mule3xEmbeddedLocalContainer extends AbstractEmbeddedLocalContainer {

    public static final String ID = "mule3x";
    public static final String NAME = "Mule 3.x Embedded";
    private Object server;
    private String muleHome;
    private static String LOG4J_PROPERTIES = "log4j.properties";
    private AtomicBoolean started = new AtomicBoolean(false);
    private SecurityManager oldSecurityManager;

    public Mule3xEmbeddedLocalContainer(final LocalConfiguration configuration) {
        super(configuration);
    }

    @Override
    public final String getId() {
        return Mule3xEmbeddedLocalContainer.ID;
    }

    @Override
    public final String getName() {
        return Mule3xEmbeddedLocalContainer.NAME;
    }

    @Override
    public final ContainerCapability getCapability() {
        return new MuleContainerCapability();
    }

    public final Object getServer() throws Exception {
        if (this.server == null) {
            createServerObject();
        }
        return this.server;
    }

    /**
     * @return defined {@link Deployable}
     */
    protected final Deployable getDeployable() {
        final List<Deployable> deployables = getConfiguration().getDeployables();
        if (deployables.isEmpty()) {
            throw new IllegalArgumentException("No " + Deployable.class.getSimpleName() + " defined");
        }
        if (deployables.size() != 1) {
            throw new IllegalArgumentException("Only supports a single " + Deployable.class.getSimpleName());
        }
        return deployables.get(0);
    }

    protected final void configureLog4j() {
        final String log4jProperties = getConfiguration().getPropertyValue(Mule3xEmbeddedLocalContainer.LOG4J_PROPERTIES);
        if (log4jProperties == null) {
            final Logger root = Logger.getRootLogger();
            root.setLevel(Level.INFO);
            root.addAppender(new ConsoleAppender(new PatternLayout("[%p] %m%n")));
        } else {
            PropertyConfigurator.configure(log4jProperties);
        }
    }

    @Override
    protected void doStart() throws Exception {
        if( getConfiguration().getProperties().containsKey(MulePropetySet.SPRING_PROFILE_ACTIVE) ) {
            System.setProperty("spring.profiles.active", getConfiguration().getProperties().get(MulePropetySet.SPRING_PROFILE_ACTIVE));
        }

        if( getConfiguration().getProperties().containsKey(MulePropetySet.HTTP_PORT) ) {
            System.setProperty("http.port", getConfiguration().getProperties().get(MulePropetySet.HTTP_PORT));
        }

        // configure Log4J
        configureLog4j();

        // avoid Mule calling System.exit(0)
        setNoExitSecurityManager();

        // create a fake Mule home
        setMuleHome();

        // enable simple logging
        System.setProperty("mule.simpleLog", "true");

        // start
        startContainer();

        restoreSecurityManager();

        started.set(true);
    }

    public String getMuleHome() {
        return muleHome;
    }

    public boolean hasBeenStarted() {
        return started.get();
    }

    private void setMuleHome() throws IOException {
        String fakeMuleHome = createFakeMuleHomeDirectory().getAbsolutePath();
        getLogger().debug("Fake Mule Home: " + fakeMuleHome, getClass().getName());
        System.setProperty("mule.home", fakeMuleHome);

        this.muleHome = fakeMuleHome;
    }

    private void setNoExitSecurityManager() {
        oldSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
    }

    private void startContainer() throws Exception {
        getServer().getClass().getMethod("start", new Class[]{boolean.class}).invoke(getServer(), false);
    }

    @Override
    protected final void waitForCompletion(final boolean waitForStarting) throws InterruptedException {
    }

    private void restoreSecurityManager() {
        System.setSecurityManager(oldSecurityManager);
    }

    @Override
    protected void doStop() throws Exception {
        try
        {
            // avoid Mule calling System.exit(0)
            setNoExitSecurityManager();

            getServer().getClass().getMethod("shutdown").invoke(getServer());

            restoreSecurityManager();

            new File(muleHome).deleteOnExit();
        } catch( Exception e ) {
            // most likely ExitException
        } finally {
            started.set(false);
        }
    }

    /**
     * Create a Mule Server Object.
     *
     * @throws Exception in case of error
     */
    protected synchronized void createServerObject() throws Exception {
        if (this.server == null) {
            try {
                this.server = getClassLoader().loadClass("org.mule.module.launcher.MuleContainer").newInstance();
            } catch (Exception e) {
                throw new ContainerException("Failed to create Mule container", e);
            }
        }
    }

    public File createFakeMuleHomeDirectory()
            throws IOException {
        File fakeMuleHome = File.createTempFile("fakeMuleHome", Long.toString(System.nanoTime()));
        File fakeAppsDir = new File(fakeMuleHome, "apps");
        File fakeLibDir = new File(fakeMuleHome, "lib/shared/default");

        if (!(fakeMuleHome.delete()) || !(fakeMuleHome.mkdir())) {
            throw new IOException("Could not create fake mule home: " + fakeMuleHome.getAbsolutePath());
        }

        if (!(fakeAppsDir.mkdir())) {
            throw new IOException("Could not create fake apps home: " + fakeAppsDir.getAbsolutePath());
        }

        if (!(fakeLibDir.mkdirs())) {
            throw new IOException("Could not create fake lib dir: " + fakeLibDir.getAbsolutePath());
        }

        return fakeMuleHome;
    }

    protected static class ExitException extends SecurityException
    {
        public final int status;
        public ExitException(int status)
        {
            super("There is no escape!");
            this.status = status;
        }
    }

    private static class NoExitSecurityManager extends SecurityManager
    {
        @Override
        public void checkPermission(Permission perm)
        {
            // allow anything.
        }
        @Override
        public void checkPermission(Permission perm, Object context)
        {
            // allow anything.
        }
        @Override
        public void checkExit(int status)
        {
            super.checkExit(status);
            throw new ExitException(status);
        }
    }
}