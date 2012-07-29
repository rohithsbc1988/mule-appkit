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
        if (!(deployable instanceof MuleApplicationDeployable)) {
            throw new IllegalArgumentException("Deployable type <" + deployable.getType() + "> is not supported!");
        }

        // application name
        final String applicationName = ((AbstractMuleDeployable)deployable).getApplicationName();
        final CauseHolder applicationFailCause = new CauseHolder();

        if( getConfiguration().getProperties().containsKey(MulePropetySet.SPRING_PROFILE_ACTIVE) ) {
            System.setProperty("spring.profiles.active", getConfiguration().getProperties().get(MulePropetySet.SPRING_PROFILE_ACTIVE));
        }

        // configure Log4J
        configureLog4j();

        // avoid Mule calling System.exit(0)
        setNoExitSecurityManager();

        // create a fake Mule home
        setMuleHome();

        // copy deployable
        File targetCopy = new File(muleHome, FilenameUtils.getName(deployable.getFile()));
        FileUtils.getFileUtils().copyFile(new File(deployable.getFile()), targetCopy);

        // enable simple logging
        System.setProperty("mule.simpleLog", "true");

        // start
        startContainer();

        // get deployment service
        Object deploymentService = getDeploymentService();

        // get lock
        Object lock = getDeploymentLock(deploymentService);

        // get composite deployment listener
        Object compositeDeploymentListener = getCompositeDeploymentListener(deploymentService);
        compositeDeploymentListener.getClass()
                .getMethod("addDeploymentListener", new Class[]{DeploymentListener.class})
                .invoke(compositeDeploymentListener, new DeploymentListener() {
                    @Override
                    public void onDeploymentStart(String appName) {
                    }

                    @Override
                    public void onDeploymentSuccess(String appName) {
                    }

                    @Override
                    public void onDeploymentFailure(String appName, Throwable cause) {
                        if( appName.equals(applicationName) ) {
                            applicationFailCause.setCause(cause);
                        }
                    }

                    @Override
                    public void onUndeploymentStart(String appName) {
                    }

                    @Override
                    public void onUndeploymentSuccess(String appName) {
                    }

                    @Override
                    public void onUndeploymentFailure(String appName, Throwable cause) {
                    }
                });

        Method tryLock = lock.getClass().getMethod("tryLock", new Class[]{long.class, TimeUnit.class});

        try {
            tryLock.invoke(lock, 60L, TimeUnit.SECONDS);

            deploymentService.getClass().getMethod("deploy", new Class[]{URL.class}).invoke(deploymentService, targetCopy.toURI().toURL());
        } catch( Exception nee ) {
            doStop();

            if( applicationFailCause.getCause() != null && applicationFailCause.getCause() instanceof Exception ) {
                throw (Exception)applicationFailCause.getCause();
            }
        } finally {
            lock.getClass().getMethod("unlock").invoke(lock);
        }
    }

    private void setMuleHome() throws IOException {
        String fakeMuleHome = createFakeMuleHomeDirectory().getAbsolutePath();
        getLogger().debug("Fake Mule Home: " + fakeMuleHome, getClass().getName());
        System.setProperty("mule.home", fakeMuleHome);

        this.muleHome = fakeMuleHome;
    }

    private void setNoExitSecurityManager() {
        System.setSecurityManager(new NoExitSecurityManager());
    }

    private Object getDeploymentStatusTracker(Object deploymentService) throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object deploymentListener = getCompositeDeploymentListener(deploymentService);

        // get list of deployment listeners
        Field deploymentListenersField = deploymentListener.getClass().getDeclaredField("deploymentListeners");
        deploymentListenersField.setAccessible(true);
        Object deploymentListeners = deploymentListenersField.get(deploymentListener);

        // get size of deployment listeners
        int size = (Integer)deploymentListeners.getClass().getMethod("size").invoke(deploymentListeners);

        // for one by one until we find the one we want
        for( int i = 0; i < size; i++ ) {
            Object innerDeploymentListener = deploymentListeners.getClass().getMethod("get", new Class[]{ int.class}).invoke(deploymentListeners, i);
            if( innerDeploymentListener.getClass().getName().equals("org.mule.module.launcher.DeploymentStatusTracker")) {
                return innerDeploymentListener;
            }
        }

        return null;
    }

    private Object getCompositeDeploymentListener(Object deploymentService) throws NoSuchFieldException, IllegalAccessException {
        // get deploymentlistener
        Field deploymentListenerField = deploymentService.getClass().getDeclaredField("deploymentListener");
        deploymentListenerField.setAccessible(true);
        return deploymentListenerField.get(deploymentService);
    }

    private Object getDeploymentLock(Object deploymentService) throws NoSuchFieldException, IllegalAccessException {
        Field lockField = deploymentService.getClass().getDeclaredField("lock");
        lockField.setAccessible(true);
        return lockField.get(deploymentService);
    }

    private Object getDeploymentService() throws Exception {
        Field deploymentServiceField = getServer().getClass().getDeclaredField("deploymentService");
        deploymentServiceField.setAccessible(true);
        return deploymentServiceField.get(getServer());
    }

    private void startContainer() throws Exception {
        getServer().getClass().getMethod("start", new Class[]{boolean.class}).invoke(getServer(), false);
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
            // most likely ExitException
        }
        System.setSecurityManager(null);

        new File(muleHome).deleteOnExit();
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

    private static class CauseHolder {
        private Throwable cause;

        public Throwable getCause() {
            return cause;
        }

        public void setCause(Throwable cause) {
            this.cause = cause;
        }
    }

    protected static void setEnv(Map<String, String> newenv)
    {
        try
        {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        }
        catch (NoSuchFieldException e)
        {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for(Class cl : classes) {
                    if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}