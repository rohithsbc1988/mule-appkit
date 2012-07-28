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
import org.mule.tools.cargo.deployable.ZipApplicationDeployable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Start an embedded {@link MuleServer} using maven dependencies.
 * <br />
 * Configured {@link MuleApplicationDeployable} is deployed on startup.
 */
public class Mule3xEmbeddedLocalContainer extends AbstractEmbeddedLocalContainer {

    public static final String ID = "mule3x";
    public static final String NAME = "Mule 3.x Embedded";
    private Object server;
    private static String LOG4J_PROPERTIES = "log4j.properties";

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

    protected final Object getServer() throws Exception {
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
        final Deployable deployable = getDeployable();
        if (!(deployable instanceof MuleApplicationDeployable || deployable instanceof ZipApplicationDeployable)) {
            throw new IllegalArgumentException("Deployable type <" + deployable.getType() + "> is not supported!");
        }

        configureLog4j();

        System.setSecurityManager(new NoExitSecurityManager());

        String fakeMuleHome = createFakeMuleHomeDirectory().getAbsolutePath();
        getLogger().debug("Fake Mule Home: " + fakeMuleHome, getClass().getName());

        System.setProperty("mule.simpleLog", "true");
        System.setProperty("mule.home", fakeMuleHome);

        // start
        getServer().getClass().getMethod("start", new Class[]{boolean.class}).invoke(getServer(), false);

        // get deployment service
        Field deploymentServiceField = getServer().getClass().getDeclaredField("deploymentService");
        deploymentServiceField.setAccessible(true);
        Object deploymentService = deploymentServiceField.get(getServer());

        // get lock
        Field lockField = deploymentService.getClass().getDeclaredField("lock");
        lockField.setAccessible(true);
        Object lock = lockField.get(deploymentService);

        Method tryLock = lock.getClass().getMethod("tryLock", new Class[]{long.class, TimeUnit.class});


        try {
            tryLock.invoke(lock, 60L, TimeUnit.SECONDS);

            deploymentService.getClass().getMethod("deploy", new Class[]{URL.class}).invoke(deploymentService, new File(deployable.getFile()).toURI().toURL());
        } finally {
            lock.getClass().getMethod("unlock").invoke(lock);
        }
    }

    @Override
    protected final void waitForCompletion(final boolean waitForStarting) throws InterruptedException {
    }

    @Override
    protected void doStop() throws Exception {
        try
        {
            getServer().getClass().getMethod("shutdown").invoke(getServer());
        } catch( Exception e ) {
            // most likely NoExitException
        }
        System.setSecurityManager(null);
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