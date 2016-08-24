package ufclncc.nacluster;

import ch.ethz.globis.pht.PhTreeVD;


/**
 * An interface for Threshold calculations. 
 */
public interface Threshold {
    int getCurrentIteration();
	boolean isDone(PhTreeVD<Cluster> phtree);
}
