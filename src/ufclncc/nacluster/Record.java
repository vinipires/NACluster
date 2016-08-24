package ufclncc.nacluster;

import java.util.HashMap;

/**
 * A single data records read from the file.
 */
public class Record {
	/** Double array as data */
	private double[] data;
	/** The true catalog classifier */
	private int idCatalog = 0;
	private int idRecord;
	private int qtdClusterReal;
	private HashMap<Cluster, Double> centroidsCandidatesDistance = new HashMap<Cluster, Double>();
	 
	
	public Record(double[] data) {
		this.data = data;
	}

	public void setIdCatalog(int correctClassifier) {
		this.idCatalog = correctClassifier;
	}

	public int getCorrectClassifier() {
		return idCatalog;
	}

	public void setId(int id) {
		this.idRecord = id;
	}

	public int getId() {
		return idRecord;
	}

	public void setClusterReal(int qtdClusterReal) {
		this.qtdClusterReal = qtdClusterReal;
	}

	public int getQtdClusterReal() {
		return qtdClusterReal;
	}

	

	/**
	 * Get the data as a double array
	 * 
	 * @return data as a double array
	 */
	public double[] getData() {
		return data;
	}

	/**
	 * @return the centroidsCandidatesDistance
	 */
	public HashMap<Cluster, Double> getCentroidsCandidatesDistance() {
		return centroidsCandidatesDistance;
	}

	/**
	 * @param centroidsCandidatesDistance the centroidsCandidatesDistance to set
	 */
	public void setCentroidsCandidatesDistance(
			HashMap<Cluster, Double> centroidsCandidatesDistance) {
		this.centroidsCandidatesDistance = centroidsCandidatesDistance;
	}


}
