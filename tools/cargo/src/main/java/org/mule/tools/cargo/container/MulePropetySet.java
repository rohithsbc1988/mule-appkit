package org.mule.tools.cargo.container;

/**
 * Gathers all Mule properties
 */
public interface MulePropetySet {
    /**
     * The active Spring profile
     */
    String SPRING_PROFILE_ACTIVE = "cargo.mule.spring.profile";

    /**
     * The default HTTP port
     */
    String HTTP_PORT = "cargo.mule.http.port";

}
