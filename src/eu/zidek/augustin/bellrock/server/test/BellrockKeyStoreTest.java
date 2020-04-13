package eu.zidek.augustin.bellrock.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.Test;

import eu.zidek.augustin.bellrock.server.BellrockDBKeyStore;
import eu.zidek.augustin.bellrock.server.BellrockUser;
import eu.zidek.augustin.bellrock.server.UserManager;

/**
 * Tester for the Bellrock Key Store.
 * 
 * @author Augustin Zidek
 *
 */
public class BellrockKeyStoreTest {

    /**
     * Test the Bellrock Key Store.
     */
    @Test
    public void testBellrockKeyStore() {
        // Bellrock Key Store
        final BellrockDBKeyStore bks;
        try {
            bks = BellrockDBKeyStore.getInstance();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            fail();
            return;
        }
        bks.clearKeyStore();

        // User Manager
        final UserManager um;
        try {
            um = UserManager.getInstance();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            fail();
            return;
        }

        // Create a new user
        final BellrockUser user1 = um.newUser();
        assertNotNull(user1);

        // Key successfully added into the Bellrock Key Store
        assertTrue(bks.addSecretKey(user1.getUID(), user1.getKey()));

        // Key successfully retrieved
        assertEquals(bks.getSecretKey(user1.getUID()), user1.getKey());

        final BellrockUser user2 = um.newUser();
        assertNotNull(user1);

        // Key successfully added into the Bellrock Key Store
        assertTrue(bks.addSecretKey(user2.getUID(), user2.getKey()));

        // Key successfully retrieved
        assertEquals(bks.getSecretKey(user2.getUID()), user2.getKey());

        // Key successfully removed
        assertTrue(bks.deleteSecretKey(user2.getUID()));

        // Key is not any more in the store
        assertNull(bks.getSecretKey(user2.getUID()));
    }
}
