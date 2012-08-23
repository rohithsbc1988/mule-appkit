package org.mule.tools.cargo.container;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.spi.AbstractEmbeddedLocalContainer;
import org.mule.MuleServer;
import org.mule.tools.cargo.deployable.MuleApplicationDeployable;

import java.io.File;
import java.io.IOException;
import java.security.Permission;
import java.util.List;
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
        if (getConfiguration().getProperties().containsKey(MulePropetySet.SPRING_PROFILE_ACTIVE)) {
            System.setProperty("spring.profiles.active", getConfiguration().getProperties().get(MulePropetySet.SPRING_PROFILE_ACTIVE));
        }

        if (getConfiguration().getProperties().containsKey(MulePropetySet.HTTP_PORT)) {
            System.setProperty("http.port", getConfiguration().getProperties().get(MulePropetySet.HTTP_PORT));
        }

        // configure Log4J
        configureLog4j();

        // create a fake Mule home
        setMuleHome();

        // enable simple logging
        System.setProperty("mule.simpleLog", "true");

        SecurityManager previousSecurityManager = System.getSecurityManager();
        final SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission(final Permission permission) {
                if (permission.getName() != null && permission.getName().startsWith("exitVM")) {
                    throw new SecurityException();
                }
            }
        };
        System.setSecurityManager(securityManager);

        try {
            startContainer();
        } catch (SecurityException e) {
            // Say hi to your favorite creator of closed source software that includes System.exit() in his code.
        } finally {
            System.setSecurityManager(previousSecurityManager);
        }

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

    private void startContainer() throws Exception {
        getServer().getClass().getMethod("start", new Class[]{boolean.class}).invoke(getServer(), false);
    }

    @Override
    protected final void waitForCompletion(final boolean waitForStarting) throws InterruptedException {
    }

    @Override
    protected void doStop() throws Exception {
        try {
            SecurityManager previousSecurityManager = System.getSecurityManager();
            final SecurityManager securityManager = new SecurityManager() {
                @Override
                public void checkPermission(final Permission permission) {
                    if (permission.getName() != null && permission.getName().startsWith("exitVM")) {
                        throw new SecurityException();
                    }
                }
            };
            System.setSecurityManager(securityManager);

            try {
                getServer().getClass().getMethod("shutdown").invoke(getServer());
            } catch (SecurityException e) {
                // Say hi to your favorite creator of closed source software that includes System.exit() in his code.
            } finally {
                System.setSecurityManager(previousSecurityManager);
            }

            new File(muleHome).deleteOnExit();

        } catch (Exception e) {

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
}