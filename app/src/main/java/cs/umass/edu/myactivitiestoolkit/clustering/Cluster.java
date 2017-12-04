package cs.umass.edu.myactivitiestoolkit.clustering;

import java.util.ArrayList;

/**
 * Created by snoran on 11/1/15.
 *
 * This class represents a cluster, which is simply a set of points that extend {@link Clusterable}.
 */
public class Cluster<T extends Clusterable<T>> {
    private ArrayList<T> points;
    public Cluster(){
        points = new ArrayList<T>();
    }

    /**
     * adds a point to the cluster
     * @param p the point to be added
     */
    public void addPoint(T p){
        points.add(p);
    }

    /**
     * @return a list of points
     */
    public ArrayList<T> getPoints(){
        return points;
    }

    /*
     * @return the number of points
     */
    public int size(){
        return points.size();
    }
}
