package eu.zidek.augustin.bellrock.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Least-recently used cache with maximum size implementation using
 * LinkedHashMap.
 * 
 * @author Augustin Zidek
 *
 * @param <K> The key.
 * @param <V> The value.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -6504611684911533825L;
    private final int cacheSize;

    /**
     * Constructs the new LRU cache with the given cache size. The underlying
     * LinkedHashMap is initialised using the default constructor.
     * 
     * @param cacheSize The capacity of the cache. Once the capacity is reached,
     *            the cache starts dropping the eldest elements.
     */
    public LRUCache(final int cacheSize) {
        super();
        this.cacheSize = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return this.size() >= this.cacheSize;
    }
}