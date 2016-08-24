package ufclncc.nacluster;

import java.util.ArrayList;

/**
 * Represent the read data.
 */
public class Data {
	/** Data records */
	private ArrayList<Record> records;
	/** Catalogs list */
	private ArrayList<Catalog> catalogs;

	/**
	 * 
	 * @param records
	 * @param catalogs
	 */
	public Data(ArrayList<Record> records, ArrayList<Catalog> catalogs) {
		this.records = records;
		this.catalogs = catalogs;
	}

	/**
	 * Get all records retrieved from the file
	 * 
	 * @return records
	 */
	public ArrayList<Record> getRecords() {
		return records;
	}

	/**
	 * Return records belonging to the same catalog (records that contains the
	 * same correctClassifier field)
	 * 
	 * @param catalogNumber
	 *            = correctClassifier contained in a record
	 * @return catalog records
	 */
	public ArrayList<Record> getCatalogRecords(int catalogNumber) {
		ArrayList<Record> catalogRecords = new ArrayList<Record>();
		for (int i = 0; i < records.size(); i++) {
			if (records.get(i).getCorrectClassifier() == catalogNumber) {
				catalogRecords.add(records.get(i));
			}
		}
		return catalogRecords;
	}

	/**
	 * 
	 * @return a list of the catalogs in the dataset
	 */
	public ArrayList<Catalog> getCatalogs() {
		return catalogs;
	}

}
