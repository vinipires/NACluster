package ufclncc.nacluster;

import ch.ethz.globis.pht.PhTreeVD;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementing a threshold where NACluster stop after it is getting the same
 * size of clusters.
 */
public class SameCentroidThreshold implements Threshold {
	/** current iteration number */
	private int curItr = 0;

	/** Keep track of the last partition */
	private int lastSize = 0;
	private int size = 0;

	public boolean isDone(PhTreeVD<Cluster> partitions) {
		curItr++;
		size = partitions.size();
		if (lastSize != size) {
			lastSize = size;
			return false;
		} else {
			return true;

		}
	}

	public int getCurrentIteration() {
		// TODO Auto-generated method stub
		return curItr;
	}
}
