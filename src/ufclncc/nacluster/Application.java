package ufclncc.nacluster;

import javax.sound.midi.MidiChannel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * A Class to run the NACluster algorithm according to given conditions.
 */
public class Application {
	public static final int DISTANCE_EUCLIDEAN = 0;
	public static final int DISTANCE_EUCLIDEAN_SQUARED = 1;
	public static final int DISTANCE_MANHATTAN = 2;
	static long initialTime;
	static long finalTime;


	/** File name to be used */
	private String fileName = "catalogsShaked.txt";
	/** file to save the incomplete data */
	@SuppressWarnings("unused")
	private String incompleteDataFile = "incomplete.data";

	/** Catalog Number to recount k value. This is the last column from data. */
	@SuppressWarnings("unused")
	private int catalogNumber = 2;

	/** Method used to check the stop condition */
	private int stopMethod = NACluster.STOP_SAME_CENTROIDS;
	/** if the ConstantEpochs method is used number of epochs */
	private int stopEpochs = 10;

	/** the distance algorithm to be used */
	private int distanceAlgorithm = DISTANCE_EUCLIDEAN;
	/** attributes to be considers */
	private int[] attributesUsed = null;

	private Data d;

	/**
	 * Main Method
	 * 
	 * @param args
	 *            filename + atrributes used + distance algorithm + Threshold
	 */
	public static void main(String[] args) {
		if (args.length < 4) {
			System.out
					.println("Usage: java -XX:+UseConcMarkSweepGC -jar NACluster.jar Filename.txt 1,2 0 1 \nargs[0] = Filename \nargs[1] = Attributes used (1, 2)\nargs[2] = Distance Algorithm (0) (0 Euclidean, 1 Euclidean Squared, 2 Manhattan)\nargs[3] = Threshold (1) (0 Constant Epochs, 1 Same Centroids)\nif args[3]==0 --> args[4] = Epochs (10) ");
			System.out
					.println("Each line in Filename.txt has: idRecord(object),ra,dec,idCatalog");
			System.out
					.println("Example: \n1,315.024512,-35.236652,1\n2,315.058928,-35.286324,2\n3,315.090892,-35.277237,3\n4,315.001386,-35.20591,1\n5,315.008551,-35.208263,2");

		} else {
			Application app = new Application();
			try {
				app.readArgs(args);
			} catch (IOException e) {
				System.out
						.println("Error occurred while reading input data.....");
			}


			initialTime = System.currentTimeMillis();
			app.run();


		}
	}

	/**
	 * Function for reading the input values from the args.
	 * 
	 * @param args
	 * 
	 * @throws IOException
	 *             if an exception occurs
	 */
	private void readArgs(String[] args) throws IOException {

		String input;

		/**
		 * "File Name (catalogsShaked.txt):");
		 */
		input = args[0];
		StringBuilder filename = new StringBuilder("File Name: ");
		if (input == null || input.trim().equals("")
				|| new File(input).exists()) {
			System.out.println(filename.append(input));
		} else {
			System.out.println("File doesn't exists");
		}

		if (input != null && !input.trim().equals("")) {
			fileName = input;
		}

		/**
		 * "Attributes used (1, 2):");
		 */
		input = args[1];
		if (args[1] == null || input.trim().equals("")) {
			System.out.println("Attributes used (1, 2):");
		} else {
			try {
				String[] attrs = input.split(",");
				if (attrs.length == 0) {
					System.out.println("Invalid input");

				}
				int[] attributes = new int[attrs.length];
				for (int i = 0; i < attrs.length; i++) {
					attributes[i] = Integer.parseInt(attrs[i].trim());
				}
				attributesUsed = attributes;
				System.out.println("Attributes used "
						+ Arrays.toString(attributesUsed));
			} catch (NumberFormatException e) {
				System.out.println("Invalid number");
			}
		}

		/**
		 * Distance Algorithm (0) (0 Euclidean, 1 Euclidean Squared, 2
		 * Manhattan):");
		 */
		input = args[2];

		if (input == null || input.trim().equals("")) {

		} else {
			try {
				int i = Integer.parseInt(input);
				if (i == 0 || i == 1 || i == 2) {
					distanceAlgorithm = i;
					System.out
							.println("Distance Algorithm ("
									+ i
									+ ") (0 Euclidean, 1 Euclidean Squared, 2 Manhattan)");

				} else {
					System.out.println("Invalid number, 0, 1 or 2 expected");
				}
			} catch (NumberFormatException e) {
				System.out.println("Invalid number");
			}
		}

		/**
		 * "Threshold (1) (0 Constant Epochs, 1 Same Centroids):");
		 */
		input = args[3];
		if (input == null || input.trim().equals("")) {
			System.out
					.println("Threshold (1) (0 Constant Epochs, 1 Same Centroids)");
		} else {
			try {
				int i = Integer.parseInt(input);
				if (i == 0 || i == 1) {
					stopMethod = i;
					System.out.println("Threshold (" + i
							+ ") (0 Constant Epochs, 1 Same Centroids)");
				} else {
					System.out.println("Invalid number, 0 or 1 expected");
				}
			} catch (NumberFormatException e) {
				System.out.println("Invalid number");
			}
		}

		if (stopMethod == 0) {

			/**
			 * Epochs (10):");
			 */
			if (args.length == 4) {
				System.out.println("Epochs (10)");
			}
			if (args.length == 5) {

				try {
					stopEpochs = Integer.parseInt(args[4]);
					System.out.println("Epochs (" + stopEpochs + ")");
				} catch (NumberFormatException e) {
					System.out.println("Invalid number");
				}

			}
		}

	}

	/**
	 * Run the NACluster algorithm, with the configured settings
	 */
	private void run() {
		BDataReader reader = new BDataReader(fileName);

		if (attributesUsed != null) {
			reader.setAttributesUsed(attributesUsed);
		}

		// read data from the file
		d = reader.read();
		reader = null;
		System.out.println("\n\n\nResults");
		System.out.println("Total number of catalogs: "
				+ (d.getCatalogs().size()));
		System.out.println("Total number of records: "
				+ (d.getRecords().size()));

		Collection<Cluster> clusters = runClustering(d);
		// System.out.println("Total number of centroids: " + clusters.size()
		// + "\n");

		long tempoSemSalvar = System.currentTimeMillis();
		System.out.println("Tempo sem salvar " + (tempoSemSalvar - initialTime) / 60000.0 + "minutos");



		/** To save the clusters in a file */
		saveAllClusters(clusters);

		finalTime = System.currentTimeMillis();

		System.out.println("Total time: " + (finalTime - initialTime) / 60000.0
				+ " minutes");
	}

	/**
	 * Run the cluster algorithm using the specific records
	 * 
	 * @param d
	 *            data
	 * @return
	 * @return the clusters
	 */
	private Collection<Cluster> runClustering(Data d) {
		NACluster clustering = new NACluster(d);

		// set the stopping criteria
		if (stopMethod == NACluster.STOP_EPOCHS) {
			clustering.setThreshold(new ConstantEpochsThreshold(stopEpochs));
		} else if (stopMethod == NACluster.STOP_SAME_CENTROIDS) {
			clustering.setThreshold(new SameCentroidThreshold());
		}

		// set the distance metric
		if (distanceAlgorithm == DISTANCE_EUCLIDEAN) {
			clustering.setDistanceAlgorithm(new EuclideanDistance());
		} else if (distanceAlgorithm == DISTANCE_EUCLIDEAN_SQUARED) {
			clustering.setDistanceAlgorithm(new EuclideanSquaredDistance());
		} else if (distanceAlgorithm == DISTANCE_MANHATTAN) {
			clustering.setDistanceAlgorithm(new ManhattanDistance());
		}

		// initialize the algorithm
		clustering.init();

		// run the algorithm
		Collection<Cluster> clusters = clustering.run();
		return clusters;
	}

	/**
	 * Save all clusters produced in a txt file
	 * 
	 * @param clusters
	 */
	private void saveAllClusters(Collection<Cluster> clusters) {
		try {
			File f = new File("clusters_"
					+ fileName.substring(fileName.length() - 10,
							fileName.length()));
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));

			int j = 1;
			for (Cluster cluster : clusters) {

				bw.write("cluster " + j);
				bw.newLine();
				bw.write("Centroid,");
				j++;

				double[] centroid = cluster.getCentroid();
				int attributesSize = centroid.length;

				for (int i = 0; i < attributesSize; i++) {
					bw.write(String.valueOf(centroid[i]));
					if (i != centroid.length - 1) {
						bw.write(",");
					}
				}

				bw.newLine();
				for (Record r : cluster.getRecords()) {
					HashMap<Cluster, Double> centroidsList = r
							.getCentroidsCandidatesDistance();
					if (centroidsList.size() > 1) {
						bw.write(r.getId() + "***,(" + r.getData()[0] + ","
								+ r.getData()[1] + "),"
								+ r.getCorrectClassifier());
						bw.newLine();
						centroidsList.remove(cluster);
						Set<Cluster> centroids = centroidsList.keySet();
						bw.write("\t ***Poss�veis matchings com os objetos do Cluster de Centroide "
								+ Arrays.toString(cluster.getCentroid()) + ":");
						bw.newLine();

						for (Cluster clusterCandidate : centroids) {
							ArrayList<Record> records = clusterCandidate
									.getRecords();
							for (Record record : records) {
								bw.write("\t * " + record.getId() + ",("
										+ record.getData()[0] + ","
										+ record.getData()[1] + "),"
										+ record.getCorrectClassifier());
								bw.newLine();
							}
						}

					} else {
						bw.write(r.getId() + ",(" + r.getData()[0] + ","
								+ r.getData()[1] + "),"
								+ r.getCorrectClassifier());
						bw.newLine();
					}
				}

			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println("error writing file " + e);
		}
	}
}
