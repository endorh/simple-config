package endorh.simpleconfig.api;

/**
 * Marker interface for entry builders that produce an
 * {@code endorh.simpleconfig.core.AtomicEntry}, and thus can be used
 * as keys in map/pairList entries, and as captions for collection
 * entries/entry groups.<br><br>
 *
 * This interface is used across the Simple Config API to enforce
 * these restrictions at compile time.<br><br>
 *
 * You may need to implement this interface if you want to make your
 * own entry types usable as keys/captions.
 */
public interface AtomicEntryBuilder {}