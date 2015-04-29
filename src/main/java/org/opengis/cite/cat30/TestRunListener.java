package org.opengis.cite.cat30;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.geotoolkit.factory.Hints;
import org.geotoolkit.referencing.factory.epsg.EpsgInstaller;
import org.opengis.cite.cat30.util.TestSuiteLogger;
import org.opengis.util.FactoryException;
import org.testng.IExecutionListener;

/**
 * A listener that is invoked before and after a test run. It is often used to
 * configure a shared fixture that endures for the duration of the entire test
 * run.
 *
 * <p>
 * A shared fixture should be used with caution in order to avoid undesirable
 * test interactions. In general, it should be populated with "read-only"
 * objects that are not modified during the test run.</p>
 *
 */
public class TestRunListener implements IExecutionListener {

    /**
     * Looks up the DataSource <code>jdbc/EPSG</code> (EPSG geodetic
     * parameters).
     */
    @Override
    public void onExecutionStart() {
        lookupDataSource();
    }

    @Override
    public void onExecutionFinish() {
    }

    /**
     * Looks for a JNDI DataSource named "jdbc/EPSG" that provides access to a
     * database containing the official EPSG geodetic parameters. If one is
     * found, it is set as a
     * {@link org.geotoolkit.factory.Hints#EPSG_DATA_SOURCE hint} when
     * initializing the EPSG factory. An embedded database will be created if
     * necessary.
     */
    void lookupDataSource() {
        DataSource epsgDataSource = null;
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            epsgDataSource = (DataSource) envContext.lookup("jdbc/EPSG");
        } catch (NamingException nx) {
            TestSuiteLogger
                    .log(Level.CONFIG,
                            "JNDI DataSource 'jdbc/EPSG' not found. An embedded database will be created if necessary.");
        }
        if (null != epsgDataSource) {
            try (Connection conn = epsgDataSource.getConnection()) {
                EpsgInstaller dbInstaller = new EpsgInstaller();
                dbInstaller.setDatabase(conn);
                if (!dbInstaller.exists()) {
                    dbInstaller.call();
                }
            } catch (SQLException | FactoryException e) {
                TestSuiteLogger.log(
                        Level.CONFIG,
                        "Failed to access DataSource 'jdbc/EPSG'\n."
                        + e.getMessage());
            }
            Hints.putSystemDefault(Hints.EPSG_DATA_SOURCE, epsgDataSource);
        }
    }
}
