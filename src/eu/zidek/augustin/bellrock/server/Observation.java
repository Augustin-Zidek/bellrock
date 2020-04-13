package eu.zidek.augustin.bellrock.server;

import java.time.Instant;
import java.util.List;

import eu.zidek.augustin.bellrock.identification.AnonymousID;
import eu.zidek.augustin.bellrock.identification.ID;
import eu.zidek.augustin.bellrock.identification.IDDecryptor;
import eu.zidek.augustin.bellrock.identification.UID;

/**
 * Class for storing observations of the clients, i.e. the signals they heard.
 * 
 * @author Augustin Zidek
 *
 */
public class Observation {
    private final UID observerUID;
    private final AnonymousID observedAID;
    private UID resolvedUID;
    private boolean isAIDResolved = false;
    private final Instant time;
    private final Location location;

    /**
     * @param observer The observer of this observation.
     * @param anonymousID The Anonymous ID that was heard.
     * @param time The time when the AID was heard.
     * @param location The location where the AID was heard.
     */
    public Observation(final UID observer, final AnonymousID anonymousID,
            final Instant time, final Location location) {
        this.observerUID = observer;
        this.observedAID = anonymousID;
        this.time = time;
        this.location = location;
    }

    /**
     * @return The UID of the observer of this observation, i.e. the user that
     *         recorded this Anonymous ID at the given time and location.
     */
    public UID getObserverUID() {
        return this.observerUID;
    }

    /**
     * @return The Anonymous AID that was heard.
     */
    public AnonymousID getAID() {
        return this.observedAID;
    }

    /**
     * @return The time when the AID was heard.
     */
    public Instant getTime() {
        return this.time;
    }

    /**
     * @return The location where the AID was heard. Set to <code>null</code> if
     *         unknown.
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * Resolves the AID into a UID using the information about users and their
     * keys. I.e., the AID is brute force decrypted using all possible keys in
     * the system (with some heuristic to make this more efficient) and if a
     * matching UID is found, it is set as the UID which resolved this AID.
     * 
     * @param aidDecryptor The Anonymous ID decryptor.
     * @param users All Bellrock users that should be tried to decrypt this AID.
     * @return The Bellrock user who sent the AID.
     */
    public BellrockUser resolveAID(final List<BellrockUser> users) {
        // Try to resolve the AID
        final BellrockUser resolvedUser = IDDecryptor
                .decryptAnonymousIDParallel(this.observedAID, users);
        // User UID not resolved
        if (resolvedUser == null) {
            this.isAIDResolved = false;
            return null;
        }
        // User UID resolved
        this.resolvedUID = resolvedUser.getUID();
        this.isAIDResolved = true;
        return resolvedUser;
    }

    /**
     * @param resolvedUID The resolved Anonymous ID.
     */
    public void addResolvedUID(final UID resolvedUID) {
        this.resolvedUID = resolvedUID;
        this.isAIDResolved = true;
    }

    /**
     * 
     * @return The resolved Anonymous ID. Note that <code>resolveAID()</code>
     *         has to be called before or the resolved UID has to be added
     *         manually before this method is called. If the UID has not been
     *         resolved, <code>null</code> is returned.
     */
    public ID getResolvedUID() {
        return this.resolvedUID;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Observation)) {
            return false;
        }
        final Observation other = (Observation) obj;
        return this.getObserverUID().equals(other.getObserverUID())
                && this.getAID().equals(other.getAID())
                && this.getTime().equals(other.getTime())
                && this.getLocation().equals(other.getLocation());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Observer: ");
        sb.append(this.observerUID.toHexString());
        sb.append(System.lineSeparator());
        sb.append("Observee (AID): ");
        sb.append(this.observedAID.toHexString());
        sb.append(System.lineSeparator());
        if (this.isAIDResolved) {
            sb.append("Observee (UID): ");
            sb.append(this.resolvedUID.toHexString());
            sb.append(System.lineSeparator());
        }
        sb.append("Time: ");
        sb.append(this.time.toString());
        sb.append(System.lineSeparator());
        sb.append("Location: ");
        sb.append(this.location.toString());
        return sb.toString();
    }
}
