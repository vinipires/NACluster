package ufclncc.nacluster;

import java.util.ArrayList;

import ch.ethz.globis.pht.BitTools;

/**
 * Represents a calculated cluster. A cluster contains the centroid and the data
 * points.
 */
public class Cluster {
	/* cluster centroid. */
	private double[] centroid;
	private long[] centroidLongBits;
	//private 



	/* records belonging to this cluster */
	private ArrayList<Record> records = new ArrayList<Record>();

	/* *
	 * Obs: Não passar record.data como parâmetro, senão dá erro no cálculo do
	 * centroid
	 */
	public Cluster(int attributesLenght) {
		this.centroid = new double[attributesLenght];
		this.centroidLongBits = new long[attributesLenght];
	}

	public double[] getCentroid() {
		return centroid;
	}

	public long[] getCentroidLongBits() {
		return centroidLongBits;
	}

	public ArrayList<Record> getRecords() {
		return records;
	}

	/**
	 * Add record in this cluster. ATTENTION: Do NOT forget to call
	 * calculateCentroid() function after calling addRecord function.
	 * 
	 * @param record
	 */
	public void addRecord(Record record) {
		records.add(record);
	}

	/**
	 * Remove record from this cluster. ATTENTION: Do NOT forget to call
	 * calculateCentroid() function after calling removeRecord function.
	 * 
	 * @param record
	 */
	public void removeRecord(Record record) {
		records.remove(record);
	}

	/**
	 * 
	 * @param catalogNumber
	 * @return
	 */
	public boolean containsCatalogPoint(int catalogNumber) {

		for (int i = 0; i < records.size(); i++) {
			if (records.get(i).getCorrectClassifier() == catalogNumber)
				return true;
		}
		return false;
	}

	/**
	 * Return a record belongs to the specified catalog if exist
	 * 
	 * @param catalogNumber
	 *            number of the specified catalog
	 * @return a record of the specified catalog if it exist
	 */
	public Record getCatalogPoint(int catalogNumber) {

		int size = records.size();
		for (int i = 0; i < size; i++) {
			if (records.get(i).getCorrectClassifier() == catalogNumber)
				return records.get(i);
		}
		return null;
	}

	/**
	 * Calculate the new centroid from the data points. Mean of the data points
	 * belonging to this cluster is the new centroid.
	 */
	public void calculateCentroid() {
		if (records.size() == 0) {
			throw new IllegalArgumentException("The calculated cluster" + " "
					+ centroid[0] + " " + centroid[1] + " should be non empty");
		}

		int size = records.get(0).getData().length; // size = numero de
													// atributos

		// reset centroid
		for (int j = 0; j < size; j++) {
			centroid[j] = 0; //
		}

		for (Record record : records) {

			double[] r = record.getData(); // recuperando um registro do cluster

			for (int j = 0; j < size; j++) {
				centroid[j] += r[j]; // atribui ao atribuito do centroide a soma
										// do atribuito correspondente de cada
										// registro do cluster.
			}
		}

		for (int i = 0; i < size; i++) {
			centroid[i] /= records.size(); // divide o atribuito pelo nº de
			centroidLongBits[i] = BitTools.toSortableLong(centroid[i]);								// registros do cluster
		}
	}

	/**
	 * Clears the data records
	 */
	public void resetRecords() {
		// now clear the records
		records.clear();
	}

	/**
	 * Get data as a long bits array
	 * 
	 * @return data as a long bits array
	 */
	public long convertDoubleToLong(double value) {
		long raw = Double.doubleToRawLongBits(value);
		if (value < 0.0) {
			return raw ^ 0x7FFFFFFFFFFFFFFFL;
		}
		return raw;
	}

	/**
	 * Get data as a double array
	 * 
	 * @return data as a double array
	 */
	public double convertLongToDouble(long value) {
		double raw = Double.longBitsToDouble(value);
		if (value < 0) {
			return Double.longBitsToDouble(value ^ 0x7FFFFFFFFFFFFFFFL);
		}
		return raw;
	}

}
