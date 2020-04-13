package eu.zidek.augustin.bellrock.server;

import java.util.List;

import eu.zidek.augustin.bellrock.identification.UID;

/**
 * Observations of AIDs made by a certain observer. This holds all observations
 * made by the client since its last synchronisation with the server.
 * 
 * @author Augustin Zidek
 *
 */
public class Observations {
    private final UID observer;
    private final List<Observation> observations;

    /**
     * @param observer The UID of the observer (client) that made these
     *            observations.
     * @param observations The observations in chronological order.
     */
    public Observations(final UID observer,
            final List<Observation> observations) {
        this.observer = observer;
        this.observations = observations;
    }

    /**
     * @return The observer.
     */
    public UID getObserver() {
        return this.observer;
    }

    /**
     * @return The observations.
     */
    public List<Observation> getObservations() {
        return this.observations;
    }

    /**
     * @return The first observation from these observations.
     */
    public Observation getFirstObservation() {
        return this.observations.get(0);
    }

    /**
     * @return The last observation from these observations.
     */
    public Observation getLastObservation() {
        return this.observations.get(this.observations.size() - 1);
    }

    /**
     * @param observation The observation to be added into the list of
     *            observations.
     */
    public void addObservation(final Observation observation) {
        this.observations.add(observation);
    }

}
