/**
 * Mule AppKit
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.cargo.deployer;

import org.apache.commons.io.FilenameUtils;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.spi.deployer.AbstractEmbeddedLocalDeployer;
import org.mule.module.launcher.DeploymentListener;
import org.mule.tools.cargo.container.Mule3xEmbeddedLocalContainer;
import org.mule.tools.cargo.deployable.AbstractMuleDeployable;
import org.mule.tools.cargo.deployable.MuleApplicationDeployable;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class EmbeddedDeployer extends AbstractEmbeddedLocalDeployer {
    public EmbeddedDeployer(EmbeddedLocalContainer container) {
        super(container);
    }

    public void deploy(Deployable deployable) {
        getLogger().info("Deploying deployable", getClass().getName());

        if (!((Mule3xEmbeddedLocalContainer) getContainer()).hasBeenStarted()) {
            throw new ContainerException("The container has not been started, cannot deploy. Add the start goal to your config.");
        }

        if (!(deployable instanceof MuleApplicationDeployable)) {
            throw new IllegalArgumentException("Deployable type <" + deployable.getType() + "> is not supported!");
        }

        try {
            final CauseHolder applicationFailCause = new CauseHolder();

            // application name
            final String applicationName = ((AbstractMuleDeployable) deployable).getApplicationName();

            // copy deployable
            File targetCopy = new File(((Mule3xEmbeddedLocalContainer) getContainer()).getMuleHome(), FilenameUtils.getName(deployable.getFile()));
            FileUtils.getFileUtils().copyFile(new File(deployable.getFile()), targetCopy);

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
                            if (appName.equals(applicationName)) {
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
            } catch (Exception nee) {
                if (applicationFailCause.getCause() != null && applicationFailCause.getCause() instanceof Exception) {
                    throw (Exception) applicationFailCause.getCause();
                }
            } finally {
                lock.getClass().getMethod("unlock").invoke(lock);
            }
        } catch (Exception e) {
            throw new ContainerException("Unable to deploy", e);
        }
    }

    public void start(Deployable deployable) {
        //throw new ContainerException("Not supported");
        getLogger().info("Starting deployable", getClass().getName());
    }

    public void stop(Deployable deployable) {
        //throw new ContainerException("Not supported");
        getLogger().info("Stopping deployable", getClass().getName());
    }

    public void undeploy(Deployable deployable) {
        //throw new ContainerException("Not supported");
        try {
            // application name
            final String applicationName = ((AbstractMuleDeployable) deployable).getApplicationName();

            // get deployment service
            Object deploymentService = getDeploymentService();

            // get lock
            Object lock = getDeploymentLock(deploymentService);

            Method tryLock = lock.getClass().getMethod("tryLock", new Class[]{long.class, TimeUnit.class});

            try {
                tryLock.invoke(lock, 60L, TimeUnit.SECONDS);

                deploymentService.getClass().getMethod("undeploy", new Class[]{String.class}).invoke(deploymentService, applicationName);
            } catch (Exception nee) {
                throw new ContainerException("Unable to undeploy", nee);
            } finally {
                lock.getClass().getMethod("unlock").invoke(lock);
            }

        } catch (Exception e) {
            throw new ContainerException("Unable to undeploy", e);
        }
    }

    private Object getDeploymentStatusTracker(Object deploymentService) throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object deploymentListener = getCompositeDeploymentListener(deploymentService);

        // get list of deployment listeners
        Field deploymentListenersField = deploymentListener.getClass().getDeclaredField("deploymentListeners");
        deploymentListenersField.setAccessible(true);
        Object deploymentListeners = deploymentListenersField.get(deploymentListener);

        // get size of deployment listeners
        int size = (Integer) deploymentListeners.getClass().getMethod("size").invoke(deploymentListeners);

        // for one by one until we find the one we want
        for (int i = 0; i < size; i++) {
            Object innerDeploymentListener = deploymentListeners.getClass().getMethod("get", new Class[]{int.class}).invoke(deploymentListeners, i);
            if (innerDeploymentListener.getClass().getName().equals("org.mule.module.launcher.DeploymentStatusTracker")) {
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
        Field deploymentServiceField = ((Mule3xEmbeddedLocalContainer) getContainer()).getServer().getClass().getDeclaredField("deploymentService");
        deploymentServiceField.setAccessible(true);
        return deploymentServiceField.get(((Mule3xEmbeddedLocalContainer) getContainer()).getServer());
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
}
