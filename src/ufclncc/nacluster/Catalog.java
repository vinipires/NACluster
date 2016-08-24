/**
 * 
 */
package ufclncc.nacluster;

import java.util.ArrayList;

/**
 * @author Vinicius Pires de Moura Freire
 * 
 */
public class Catalog {
	/** Catalog name */
	private String name;
	/** Catalog records */
	private ArrayList<Record> records;
	/** Catalog id */
	private int id;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void addRecord(Record record){
		records.add(record);
	}

	public ArrayList<Record> getRecords() {
		return records;
	}

	public int getId() {
		return id;
	}

	public long getSize() {
		return records.size();
	}

	public Catalog(int id) {
		this.id = id;
		records = new ArrayList<Record>();

	}

}
