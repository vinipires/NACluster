package ufclncc.nacluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This is a data reader for reading the catalogs shaked.
 */
public class BDataReader {
	/** file name */
	private String fileName = "catalogsShaked.txt";
	/** number of attributes */
	private int attributes = 2;
	/** separator */
	private static final String separator = ";";
	/** index of Attributes to be used */
	private int[] attributesUsed = { 1, 2 };
	
	private Catalog catalog;

	/** change the name of the filanem */
	public BDataReader(String fileName) {
		this.fileName = fileName;
	}

	public void setAttributesUsed(int[] attributesUsed) {
		this.attributesUsed = attributesUsed;
		this.attributes = attributesUsed.length;
	}

	/**
	 * Read the data from the given file
	 * 
	 * @return the data from the file
	 */
	@SuppressWarnings("resource")
	public Data read() {
		File file = new File(fileName);

		BufferedReader in = null;
		ArrayList<Record> records = new ArrayList<Record>();
		ArrayList<Catalog> catalogs = new ArrayList<Catalog>();
		if (file.exists()) {
			try {
				in = new BufferedReader(new FileReader(file));

				String line = in.readLine();
				while (line != null) {
					// System.out.println(line);
					readRecord(records, catalogs, line);
					line = null;
					line = in.readLine();
				}
				line = null;

			} catch (FileNotFoundException ignored) {
			} catch (IOException e) {
				System.out.println("Error occurred while reading file: " + file
						+ " " + e.getMessage());
			}
		} else {
			return null;
		}

		return new Data(records, catalogs);
	}

	/**
	 * Read a single records and store it in records
	 * 
	 * @param records
	 *            records list
	 * @param incompleteRecords
	 *            incomplete record list
	 * @param line
	 *            string to extract the record fom
	 */
	private void readRecord(ArrayList<Record> records,
			ArrayList<Catalog> catalogs, String line) {
		String[] strRecord = line.split(separator);
		double[] record = new double[attributes];
		for (int i = 0; i < attributesUsed.length; i++) {
			try {
				record[i] = Double.parseDouble(strRecord[attributesUsed[i]]
						.trim());
			} catch (NumberFormatException e) {
				System.err.println(e);
				return;
			}
		}
		Record r = new Record(record);
		try {
			r.setId(Integer.parseInt(strRecord[0]));
			r.setClusterReal(Integer.parseInt(strRecord[strRecord.length - 1]));
		} catch (NumberFormatException e) {
			System.err.println(e);
			return;
		}
		try {
			r.setIdCatalog(Integer
					.parseInt(strRecord[strRecord.length - 2]));
			boolean exist = false;
			
			int catalogsSize = catalogs.size();
			for (int i = 0; i < catalogsSize; i++) {
				catalog = catalogs.get(i);
				if (catalogs.get(i).getId() == r.getCorrectClassifier()) {
					exist = true;
					catalog.addRecord(r);
			//		size = catalogs.get(i).getSize();
			//		catalogs.get(i).setSize(size + 1);
				}
			}
			if (!exist) {

				catalog = new Catalog(r.getCorrectClassifier());
				catalogs.add(catalog);
				catalog.addRecord(r);
			}
			catalog = null;

		} catch (NumberFormatException e) {
			System.err.println(e);
			return;
		}
		records.add(r);
		strRecord = null;
		records = null;
		catalogs = null;
		line = null;
		
	}
}
