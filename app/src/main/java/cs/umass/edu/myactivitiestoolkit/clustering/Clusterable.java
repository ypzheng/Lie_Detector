package cs.umass.edu.myactivitiestoolkit.clustering;

/**
 * Created by snoran on 11/1/15.
 *
 * This interface defines the {@code distance} method that points must have to be valid for DBScan
 * clustering.
 *
 * @author snoran
 */
public interface Clusterable<T> {
    /** Computes the distance between two points - this is generally the Euclidean distance but it need not be */
    double distance(T other);
}
