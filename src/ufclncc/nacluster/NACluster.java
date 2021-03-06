package ufclncc.nacluster;

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

import ch.ethz.globis.pht.BitTools;
import ch.ethz.globis.pht.PhTreeVD;
import ch.ethz.globis.pht.PhTreeVD.PVDEntry;
import ch.ethz.globis.pht.PhTreeVD.PVDIterator;

/**
 * Implementation of the NACluster algorithm. 
 * 
 * The idea of the algorithm is to compare each object of one catalog  
 * to all computed cluster centroids, by computing the distance 
 * d(O_{i},C_{a}) of an object O_{i}  to a centroid C_{a}. 
 * When d(O_{i},C_{a}) < \epsilon, then the object O_{i} is 
 * mapped to  cluster C_{a}. This mapping, however, can only be 
 * applied when there not exists another object O_{j} in cluster 
 * C_{a} that has been mapped to the same catalog of O_{i}.
 * 
 * In case an object of $C_{a}$ already exists in the cluster, 
 * two scenarios must be evaluated: 
 * (1) if d(O_{j},C_{a}) > d(O_{i},C_{a}) 
 * 		then we should remove the object O_{j} from the cluster C_{a}, 
 * 			insert O_{i} in this cluster, and search another 
 * 			cluster for O_{j}; 
 * (2) if d(O_{j},C_{a}) < d(O_{i},C_{a})
 * 		then the algorithm performs a recursive search on the candidate list,
 *      using the procedure searchCentroid
 * 		for allocating O_{i}.
 * In case, no cluster is found at distance \epsilon then a new cluster C_{b} 
 * is created to the point O_{i} and it will be the centroid.
 * 
 */
/**
 * @author Vinicius Pires de Moura Freire
 * Neste caso a classe threshold � com a phtree para limitar o numero de itera��es.
 */
public class NACluster {
	/** Constants for NACLuster Algorithm */
	public static final int MAXDISTANCE = 10000;

	/** Constants for stopping criteria */
	public static final int STOP_EPOCHS = 0;
	public static final int STOP_SAME_CENTROIDS = 1;

	/* This class is used to calculate the distance between two data points */
	private Distance distanceAlgorithm = new EuclideanDistance();
	/* This class is used to calculate the stopping criteria */
	private Threshold threshold = new ConstantEpochsThreshold(20);
	/* Centroids as PHTree Format */
	private PhTreeVD<Cluster> centroidsTree;
	
	private int qtdCentroideNovo = 0;
	/* Represent the actual records read from the file */
	private ArrayList<Record> records;
	/*
	 * Records from the largest catalog for using in initial cluster centroid
	 * position
	 */
	private ArrayList<Record> largestCatalogRecords;
	/* boolean value to control if the catalog has visited yet */
	private HashMap<Catalog, Boolean> visitedCatalogs = new HashMap<Catalog, Boolean>();
	/* Dataset */
	private Data d;
	/* cluster retrieved */
	private Cluster currentCluster;

	/* size of attributes list */
	private int attributesLenght;
	/* index of the largest catalog in catalogs ArrayList */
	private int largestCatalogIndex;
	/* time variables */
	//private long initialTime, finalTime;
	/* catalogs in this dataset */
	ArrayList<Catalog> catalogs;
	/* collection of clusters generated by NACluster Algorithm */
	/* number of catacatalogs: size of catalogs list */
	private int catalogsQuantity;
	private int quantidadeSearchCent = 0;
	private int primeiroRecordId;
	private int ultimoRecordId;
	private int grauAmbiguidadeSoma = 0;
	private double epsilon = 0.01;

	private Collection<Cluster> clustersCollection;

	/**
	 * 
	 * @param d
	 *            Data loaded from the file
	 * 
	 */
	public NACluster(Data d) {
		this.d = d;
		/* all dataset records */
		records = d.getRecords();
		primeiroRecordId = records.get(0).getId();
		ultimoRecordId = records.get(records.size()-1).getId();
		catalogs = d.getCatalogs();
		/* size of the largest catalog */
		long largestCatalogSize = 0;
		largestCatalogIndex = 0;
		/* number of attributes */
		attributesLenght = records.get(0).getData().length;
		/* number of catalogs */
		catalogsQuantity = catalogs.size();
		/* map records to their respective catalog and find the largest catalog */
		for (int i = 0; i < catalogsQuantity; i++) {
			System.out.println("Catalog " + catalogs.get(i).getId()
					+ " has size equals " + catalogs.get(i).getRecords().size());
			if (catalogs.get(i).getSize() > largestCatalogSize) {
				largestCatalogIndex = i;
				largestCatalogSize = catalogs.get(i).getSize();
			}
		}

        //#TODO aqui muda o maior catalogo
		largestCatalogIndex = 5;
		System.out.println("The largest catalog is "
				+ catalogs.get(largestCatalogIndex).getId() + ". It has "
				+ catalogs.get(largestCatalogIndex).getSize() + " records.");

		largestCatalogRecords = catalogs.get(
				largestCatalogIndex).getRecords();
		d = null;
	}

	/**
	 * Set threshold method
	 * 
	 * @param threshold
	 */
	public void setThreshold(Threshold threshold) {
		this.threshold = threshold;
	}

	/**
	 * Set Distance Algorithm (Euclidian, Euclidian Squared or Manhattan
	 * Distance
	 * 
	 * @param distanceAlgorithm
	 */
	public void setDistanceAlgorithm(Distance distanceAlgorithm) {
		this.distanceAlgorithm = distanceAlgorithm;
	}

	/**
	 * Initialize clusters and centroids
	 */
	public void init() {

		catalogPointsCentroids();
	}

	/**
	 * Retorna o resultado final do algoritmo, ou seja os clusters finais com os
	 * elementos
	 * @return 
	 * @throws IOException 
	 */
	public Collection<Cluster> run() {



		double currentDistance = 0;
		double distance = 0;
		PVDEntry<Cluster> currentCentroid = null;
		/* records of current catalog */
		ArrayList<Record> catalogRecords;
		boolean existCentroid;
		

		
		// Condi��o de parada: n� de itera��es ou quantidade de clusters n�o se
		// modificam
		

		
		ArrayList<Integer> cont = new ArrayList<>();
		
		for (int i = 0; i < 100; i++) {
			cont.add(0);
		}
		
		while (!threshold.isDone(centroidsTree)) {
			grauAmbiguidadeSoma = 0;
			int currentIteration = threshold.getCurrentIteration();
			System.out.println(cont);
			if (currentIteration > 1) {
				// limpa a lista de cat�logos visitados
				visitedCatalogs.clear();

				System.out.println();
				System.out.println();
				System.out.println("pr�xima rodada, resetando clusters...");
				 PhTreeVD.PVDIterator<Cluster> iter = centroidsTree.queryExtent();
			        while (iter.hasNext()) iter.next().getValue().resetRecords();;
			
			}

			for (Catalog catalog : catalogs) {
				// Adding all the catalogs in the map
				visitedCatalogs.put(catalog, false);
			}
			/**
			 * put the largest catalog in visitedCatalogs HashMap in the first
			 * iteration
			 */
			if (threshold.getCurrentIteration() == 1)
				visitedCatalogs.put(d.getCatalogs().get(largestCatalogIndex),
						true);

			/** Itera��o nos catalogs */
			for (Catalog catalog : catalogs) {
				/**
				 * Garantindo que os objetos do cat�logo n�o foram percorridos
				 * anteriormente
				 */
				if (!visitedCatalogs.get(catalog)) {
					visitedCatalogs.put(catalog, true);

					System.out.println("catalog.getId() = " + catalog.getId());
					catalogRecords = catalog.getRecords();
					/** Percorrendo os objetos do cat�logo **/
					for (Record record : catalogRecords) {
						currentDistance = MAXDISTANCE;

						/**
						 * find the closest centroid for this record
						 **/
						existCentroid = false;
						/**
						 * search for centroids in radius 0.003 from the
						 * position of the record. centroidsTree ia a PHTree
						 */

						Iterator<PVDEntry<Cluster>> results = rangeQuery(
								centroidsTree, record.getData(), epsilon);
						/**
						 * Why the hashmap is <String,Cluster> not
						 * <Long[],Cluster>? Because each time we call
						 * rangeQuery function, the centroidLong return has a
						 * pointer to an different java object, even if they
						 * have the same value in another function call.
						 */
						HashMap<Cluster, Double> centroidsCandidatesDistance = new HashMap<Cluster, Double>();
						PVDEntry<Cluster> clusterCandidate;

						for (Iterator<PVDEntry<Cluster>> iterator = results; iterator
								.hasNext();) {
							existCentroid = true;
							clusterCandidate = iterator.next();
							

							distance = distanceAlgorithm.calculate(
									record.getData(), clusterCandidate.getKey());
							/**
							 * search for the shortest distance between the
							 * current record and the current centroid
							 */

							if (currentDistance >= distance) {
								/**
								 * store the centroid with the shortest distance
								 * from the record
								 */
								currentCentroid = clusterCandidate;

								currentDistance = distance;
							}
							/**
							 * each centroid and its distance returned from
							 * range query are inserted in
							 * centroidsCandidatesDistance hashmap
							 */
							centroidsCandidatesDistance.put(
									clusterCandidate.getValue(), distance);
						}

						results = null;

						int quantidadeCandidatos = centroidsCandidatesDistance.size();
						record.setCentroidsCandidatesDistance(centroidsCandidatesDistance);
						
						grauAmbiguidadeSoma += quantidadeCandidatos -1;
						if (currentIteration == 2){
							//System.out.println(quantidadeCandidatos);
							cont.set(quantidadeCandidatos, cont.get(quantidadeCandidatos)+1);
						}
						//System.out.println(record.getId() + " " + quantidadeCandidatos );
						if (existCentroid) {

							currentCluster = currentCentroid.getValue();

							/**
							 * Verifica se n�o existe registro do mesmo cat�logo
							 * associado � esse cluster
							 */
							if (!currentCluster.containsCatalogPoint(catalog
									.getId())) {
								/*
								 * add this record if not exist a record from
								 * the same catalog in this cluster
								 */

								

								currentCluster.addRecord(record);
								currentCluster.calculateCentroid();
								

								/*
								 * once a record was inserted in the cluster,
								 * and the centroid position changed (when we
								 * call the calculateCentroid function, we need
								 * of the newCentroid value
								 */

								/*
								 * When we have a new centroid value, we must to
								 * delete the previous centroid from the phtree
								 * and the clustersMap and add a new centroid
								 * value in them
								 */
								
								
								if ((centroidsTree.contains(currentCluster.getCentroid())) && (!centroidsTree.get(currentCluster.getCentroid()).equals(currentCluster)) ){
									Cluster oldCluster =  centroidsTree.get(currentCluster.getCentroid());
									
									//System.out.println("o centroide novo ja existe 0");
									qtdCentroideNovo++;
									
									//System.out.println("centroides antes " + Arrays.toString(currentCluster.getCentroid()) + "e " +  Arrays.toString(oldCluster.getCentroid()));
									//System.out.println("Records old cluster1 : ");
									ArrayList<Record> recordsOld = oldCluster.getRecords();
						     //		for (int i = 0; i < recordsOld.size(); i++) {
							//			System.out.print(recordsOld.get(i).getId() + " ");
							//		}
									
							//		System.out.println("Records current cluster : ");
									ArrayList<Record> recordsCurrent = currentCluster.getRecords();
							//		for (int i = 0; i < recordsCurrent.size(); i++) {
							//			System.out.print(recordsCurrent.get(i).getId() + " ");
							//		}
							//		System.out.println("CentroidesTree antes do antes" + centroidsTree.size());
									centroidsTree.remove(oldCluster.getCentroid());
									Record oldRecord = oldCluster.getCatalogPoint(catalog.getId());
									oldCluster.removeRecord(oldRecord);
									oldCluster.addRecord(record);
									currentCluster.removeRecord(record);
									currentCluster.addRecord(oldRecord);
									currentCluster.calculateCentroid();
									
									//System.out.println(Math.(36.991940+36.991684, 2));
									oldCluster.calculateCentroid();
								//	System.out.println("CentroidesTree antes " + centroidsTree.size());
									centroidsTree.put(currentCluster.getCentroid(), currentCluster);
									centroidsTree.put(oldCluster.getCentroid(), oldCluster);
									//System.out.println("novos centroids " + Arrays.toString(currentCluster.getCentroid()) + "e " +  Arrays.toString(oldCluster.getCentroid()));
									//System.out.println("records oldC " + oldCluster.getRecords().get(0).getId() + " e " + oldCluster.getRecords().get(1).getId()+ " e records currentC " + "records oldC " +currentCluster.getRecords().get(0).getId() + " e " +currentCluster.getRecords().get(1).getId());
									//System.out.println("CentroidesTree depois " + centroidsTree.size());
								}else{
								

								int tamanhoantes = centroidsTree.size();
								//if ((centroidsTree.contains(currentCluster.getCentroid())) && (!centroidsTree.get(currentCluster.getCentroid()).equals(currentCluster)) ){
									//System.out.println("o centroide novo ja existe 1");
								//	qtdCentroideNovo++;
							//	};
								
								//System.out.println("Removendo " + Arrays.toString(currentCentroid.getKey()));
								//System.out.println("Inserindo " + Arrays.toString(currentCluster.getCentroid()));

								centroidsTree.remove(currentCentroid.getKey());
								// clustersMap.remove(currentCentroid.getValue());
								centroidsTree.put(currentCluster.getCentroid(),
										currentCluster);
								
								int tamanhodepois = centroidsTree.size();
							//	if (tamanhoantes != tamanhodepois) {
								//	System.out.println(" Centroide " + Arrays.toString(currentCluster.getCentroid()) + " existe novo? " + centroidsTree.contains(currentCluster.getCentroid())+ " e o velho? " + Arrays.toString(currentCentroid.getKey()) + " " + centroidsTree.contains(currentCentroid.getKey()));
								//	System.out.println(currentCluster.getRecords().get(0).getId() );
							//	}
								}
							} else {
								/**
								 * Se existe registro do mesmo cat�logo
								 * associado � esse cluster
								 */

								searchCentroid(record,
										centroidsCandidatesDistance,
										record.getCorrectClassifier());
								centroidsCandidatesDistance = null;
							}
						} else {

							/**
							 * If not exist centroid in the range query from the
							 * current record create a new cluster and add this
							 * record in that
							 */
							
							int tamanhoantes = centroidsTree.size();
							
							currentCluster = new Cluster(attributesLenght);
							currentCluster.addRecord(record);
							currentCluster.calculateCentroid();
							

							
						
							//System.out.println("Inserindo " + Arrays.toString(currentCluster.getCentroid()));

							centroidsTree.put(currentCluster.getCentroid(),
									currentCluster);
							
							int tamanhodepois = centroidsTree.size();
							//if (tamanhoantes == tamanhodepois) {
							//	System.out.println(" Centroide primeiro" + Arrays.toString(currentCluster.getCentroid()) + " existe? " + centroidsTree.contains(currentCluster.getCentroid()));
						//	}
							//clustersMap.put(newCentroid, currentCluster);

						}
					}
				}
			}
		
			System.out.println(" tree size " + centroidsTree.size());

			//clustersCollection = clustersMap.values();

			 clustersCollection = new ArrayList<>(centroidsTree.size());
			
			 PhTreeVD.PVDIterator<Cluster> iter = centroidsTree.queryExtent();
		        while (iter.hasNext()) clustersCollection.add(iter.next().getValue());
			
			
			System.out
					.println("iteration " + (threshold.getCurrentIteration()));

			computeFmeasure3(clustersCollection);

		}
		System.out.println("finished at the iteration "
				+ (threshold.getCurrentIteration() - 1));
		System.out.println(quantidadeSearchCent + " vezes no search Centroide");
		

		System.out.println("É o NACluster2 mesmo!!!");

		//clustersCollection = clustersMap.values();

		//for (int i = 0; i < cont.size(); i++) {
		//	System.out.println(i + " " + cont.get(i));
	//	}
	
		
		return clustersCollection;
	}

	private void computeFmeasure2(Collection<Cluster> clusters,
			int numberOfCatalogs) {
		ArrayList<Record> recordsCluster = new ArrayList<Record>();
		Integer clustersCorretos = 0;
		int clustersErrados = 0;
		int clusterId1 = 0, clusterId2 = 0, clusterId3 = 0;

		if (numberOfCatalogs == 2) {
			for (Cluster cluster : clusters) {

				recordsCluster = cluster.getRecords();

				if (recordsCluster.size() == 2) {
					clusterId1 = recordsCluster.get(0).getId();
					clusterId2 = recordsCluster.get(1).getId();
					if ((clusterId1 == clusterId2)) {
						clustersCorretos++;
					} else {
						clustersErrados++;
					}
				} else {
					clustersErrados++;
				}
			}

		}
		
		if (numberOfCatalogs == 3) {
			for (Cluster cluster : clusters) {
				recordsCluster = cluster.getRecords();
				if (recordsCluster.size() == 3) {
					clusterId1 = recordsCluster.get(0).getId();
					clusterId2 = recordsCluster.get(1).getId();
					clusterId3 = recordsCluster.get(2).getId();
					if ((clusterId1 == clusterId2) 
							&& (clusterId2 == clusterId3)) {
						clustersCorretos++;
					} else {
						clustersErrados++;
					}
				} else {
					clustersErrados++;
				}
			}

		}
		
		StringBuilder imprimir = new StringBuilder(680);
		double clustersCorretosDouble = clustersCorretos;
		double numeroOriginalCatalog = records.size() / catalogsQuantity;
		double precision = clustersCorretosDouble / clusters.size();
		double recall = clustersCorretosDouble / numeroOriginalCatalog;
		double fmeasure = (2 * precision * recall) / (precision + recall);

		// "Obs: Essa classifica��o de quantidade de clusters s� funciona para os cat�logos chacoalhados gerados pela classe catalogShake.java e utilizando no m�ximo 6 cat�logos. A porcentagem � qtdClustersCorretos/qtdClustersOriginais, considerando o catalogo original como id = 1");

		imprimir.append("\n" + clustersCorretos + " clusters corretos\n"
				+ clustersErrados + " clusters errados\n" + "Precision "
				+ precision + "\nRecall " + recall + "\n" + "F-measure "
				+ fmeasure + "\n" + threshold.getCurrentIteration() + " "
				+ clustersCorretos + " " + centroidsTree.size() + " "
				+ clustersCorretosDouble / numeroOriginalCatalog * 100 + "% "
				+ //((System.currentTimeMillis() - initialTime) / 1000.0) +
				 "s" + "\n centroidsIguais = "+ qtdCentroideNovo);

		System.out.println(imprimir);
		System.out.println(quantidadeSearchCent + " vezes no search Centroide");
		
	}

	/**
	 * Compute the number of correct clusters and F-Measure ATTENTION: We must
	 * to modify this implementation: we must having sorted objects
	 * 
	 * @param clusters
	 * @param numberOfCatalogs
	 */
	private void computeFmeasure(Collection<Cluster> clusters,
			int numberOfCatalogs) {
		ArrayList<Record> recordsCluster = new ArrayList<Record>();
		Integer clustersCorretos = 0;
		int clustersErrados = 0;
		int clusterId1 = 0, clusterId2 = 0, clusterId3 = 0;

		if (numberOfCatalogs == 2) {
			for (Cluster cluster : clusters) {

				recordsCluster = cluster.getRecords();

				if (recordsCluster.size() == 2) {
					clusterId1 = recordsCluster.get(0).getId();
					clusterId2 = recordsCluster.get(1).getId();
					if ((clusterId1 == clusterId2 + 1)) {
						clustersCorretos++;
					} else {
						clustersErrados++;
					}
				} else {
					clustersErrados++;
				}
			}

		}

		if (numberOfCatalogs == 3) {
			for (Cluster cluster : clusters) {
				recordsCluster = cluster.getRecords();
				if (recordsCluster.size() == 3) {
					clusterId1 = recordsCluster.get(0).getId();
					clusterId2 = recordsCluster.get(1).getId();
					clusterId3 = recordsCluster.get(2).getId();
					if ((clusterId1 == clusterId2 - 1)
							&& (clusterId2 == clusterId3 - 1)) {
						clustersCorretos++;
					} else {
						clustersErrados++;
					}
				} else {
					clustersErrados++;
				}
			}

		}

		if (numberOfCatalogs == 4) {
			for (Cluster cluster : clusters) {
				recordsCluster = cluster.getRecords();
				if (recordsCluster.size() == 4) {
					clusterId1 = recordsCluster.get(0).getId();
					clusterId2 = recordsCluster.get(1).getId();
					clusterId3 = recordsCluster.get(2).getId();
					int clusterId4 = recordsCluster.get(3).getId();
					if ((clusterId1 == clusterId2 - 1)
							&& (clusterId2 == clusterId3 - 1)
							&& (clusterId3 == clusterId4 - 1)) {
						clustersCorretos++;
					} else {
						clustersErrados++;
					}
				} else {
					clustersErrados++;
				}
			}
		}

		if (numberOfCatalogs == 5) {
			for (Cluster cluster : clusters) {
				recordsCluster = cluster.getRecords();
				if (recordsCluster.size() == 5) {
					clusterId1 = recordsCluster.get(0).getId();
					clusterId2 = recordsCluster.get(1).getId();
					clusterId3 = recordsCluster.get(2).getId();
					int clusterId4 = recordsCluster.get(3).getId();
					int clusterId5 = recordsCluster.get(4).getId();
					if ((clusterId1 == clusterId2 - 1)
							&& (clusterId2 == clusterId3 - 1)
							&& (clusterId3 == clusterId4 - 1)
							&& (clusterId4 == clusterId5 - 1)) {
						clustersCorretos++;
					} else {
						clustersErrados++;
					}
				} else {
					clustersErrados++;
				}
			}

		}

		if (numberOfCatalogs == 6) {
			for (Cluster cluster : clusters) {
				recordsCluster = cluster.getRecords();
				if (recordsCluster.size() == 6) {
					clusterId1 = recordsCluster.get(0).getId();
					clusterId2 = recordsCluster.get(1).getId();
					clusterId3 = recordsCluster.get(2).getId();
					int clusterId4 = recordsCluster.get(3).getId();
					int clusterId5 = recordsCluster.get(4).getId();
					int clusterId6 = recordsCluster.get(5).getId();
					if ((clusterId1 == clusterId2 - 1)
							&& (clusterId2 == clusterId3 - 1)
							&& (clusterId3 == clusterId4 - 1)
							&& (clusterId4 == clusterId5 - 1)
							&& (clusterId5 == clusterId6 - 1)) {
						clustersCorretos++;
					} else {
						clustersErrados++;
					}
				} else {
					clustersErrados++;
				}
			}

		}

		StringBuilder imprimir = new StringBuilder(680);
		double clustersCorretosDouble = clustersCorretos;
		double numeroOriginalCatalog = records.size() / catalogsQuantity;
		double precision = clustersCorretosDouble / clusters.size();
		double recall = clustersCorretosDouble / numeroOriginalCatalog;
		double fmeasure = (2 * precision * recall) / (precision + recall);

		// "Obs: Essa classifica��o de quantidade de clusters s� funciona para os cat�logos chacoalhados gerados pela classe catalogShake.java e utilizando no m�ximo 6 cat�logos. A porcentagem � qtdClustersCorretos/qtdClustersOriginais, considerando o catalogo original como id = 1");

		imprimir.append("\n" + clustersCorretos + " clusters corretos\n"
				+ clustersErrados + " clusters errados\n" + "Precision "
				+ precision + "\nRecall " + recall + "\n" + "F-measure "
				+ fmeasure + "\n" + threshold.getCurrentIteration() + " "
				+ clustersCorretos + " " + centroidsTree.size() + " "
				+ clustersCorretosDouble / numeroOriginalCatalog * 100 + "% "
				+ //((System.currentTimeMillis() - initialTime) / 1000.0) +
				 "s" + "\n centroidsIguais = "+ qtdCentroideNovo);

		System.out.println(imprimir);
		System.out.println(quantidadeSearchCent + " vezes no search Centroide");
		
		
	}
	
	
	
	private void computeFmeasure3(Collection<Cluster> clusters) {
        ArrayList<Record> recordsCluster;
        Integer clustersCorretos = 0;
        int clustersErrados = 0;

        int sizeCluster;

		Iterator<Cluster> itr = clusters.iterator();
		while(itr.hasNext()){
			Cluster cluster = itr.next();
			recordsCluster = cluster.getRecords();
			if (recordsCluster.size() >0) {
				sizeCluster = recordsCluster.size();
				boolean certo = true;
				// int id1 = recordsCluster.get(0).getId();
				Record record1 = recordsCluster.get(0);

				if (sizeCluster == 1 && record1.getQtdClusterReal() == 1) {
					clustersCorretos++;
				} else {
					for (int i = 1; i < sizeCluster; i++) {
						if (record1.getId() != recordsCluster.get(i).getId()) certo = false;
					}
					if (certo) {
						if (sizeCluster == record1.getQtdClusterReal())
							clustersCorretos++;
						else {
							clustersErrados++;
						}
					} else clustersErrados++;
				}


			}
			//clusters.remove(cluster);
            //TODO não posso remover de clusters, mas posso remover de centroidsTree. Pode dar bug
			centroidsTree.remove(cluster.getCentroid());
			//else clustersErrados++;

		}




		double sizeClusters = clustersCorretos + clustersErrados;

        StringBuilder imprimir = new StringBuilder(680);
        double clustersCorretosDouble = clustersCorretos;

        System.out.println("primeiroRecordId = " + primeiroRecordId);
        System.out.println("ultimoRecordId = " + ultimoRecordId);
        double precision = clustersCorretosDouble / sizeClusters;
        double recall = clustersCorretosDouble /(ultimoRecordId - primeiroRecordId + 1) ;

        System.out.println(" clustersCorretosDouble = " + clustersCorretosDouble);
        double fmeasure = (2 * precision * recall) / (precision + recall);

        // "Obs: Essa classifica??o de quantidade de clusters s? funciona para os cat?logos chacoalhados gerados pela classe catalogShake.java e utilizando no m?ximo 6 cat?logos. A porcentagem ? qtdClustersCorretos/qtdClustersOriginais, considerando o catalogo original como id = 1");

        imprimir.append("\n").append(clustersCorretos).append(" clusters corretos\n").append(clustersErrados).append(" clusters errados\n").append("Precision ").append(precision).append("\nRecall ").append(recall).append("\n").append("F-measure ").append(fmeasure).append("\n").append(+clustersCorretos).append(" ").append(clusters.size()).append(" ").append(clustersCorretosDouble / 3 * 100).append("% ");

        System.out.println(imprimir);
        
        double grau = grauAmbiguidadeSoma;
       
        //double sizeClusters = centroidsTree.size();
        //sizeClusters = sizeClusters -1.0;

        double sizeRecords = this.records.size();
        
      
        double maxAmb = sizeClusters*sizeRecords;
     
        
        System.out.println(quantidadeSearchCent + " vezes no search Centroide");
        System.out.println("Grau de Ambiguidade " + grauAmbiguidadeSoma);
        System.out.printf( "Grau M�dio de Ambiguidade = %.8f\n", grau/sizeRecords);
        System.out.printf( "M�trica de Ambiguidade = %.8f\n", grau/maxAmb);
        

    }

	
	

	/**
	 * Recursive search on the candidate list (centroidsCandidatesDistance)
	 * 
	 *
	 */
	private void searchCentroid(Record record,
			HashMap<Cluster, Double> centroidsCandidatesDistance, Integer catalog) {
		quantidadeSearchCent++;
		switch (centroidsCandidatesDistance.size()) {
		case 0:
			/* criando cluster, pois n�o tem centroide candidato */

			currentCluster = new Cluster(attributesLenght);

			currentCluster.addRecord(record);
			currentCluster.calculateCentroid();
			int tamanhoantes = centroidsTree.size();

		
			
			//System.out.println("Inserindo " + Arrays.toString(currentCluster.getCentroid()));

			centroidsTree.put(currentCluster.getCentroid(),
					currentCluster);
			int tamanhodepois = centroidsTree.size();
			if (tamanhoantes == tamanhodepois) { 
				//System.out.println(" Centroide segundo" + Arrays.toString(currentCluster.getCentroid()) + " existe? " + centroidsTree.contains(currentCluster.getCentroid()));
			}

			break;

		default:
			Set<Cluster> centroidsIds = centroidsCandidatesDistance.keySet();
			double currentDistance = 10000;
			Cluster currentCentroid = null;;

			/* performs a seach in the candidates list */
			for (Cluster centroid : centroidsIds) {

				if (centroidsCandidatesDistance.get(centroid) < currentDistance) {
					/* find the closest centroid */
					currentDistance = centroidsCandidatesDistance.get(centroid);

					currentCentroid = centroid;
				}
			}
			currentCluster = currentCentroid;

			if (!currentCluster.containsCatalogPoint(catalog)) {

				double[] currentCentroidDouble = convertToDoubleArray(currentCentroid.getCentroidLongBits());

				currentCluster.addRecord(record);

				currentCluster.calculateCentroid();
				//long[] newCentroid = currentCluster.getCentroidLongBits();
				if ((centroidsTree.contains(currentCluster.getCentroid())) && (!centroidsTree.get(currentCluster.getCentroid()).equals(currentCluster)) ){
				//	System.out.println("o centroide novo "+ Arrays.toString(currentCluster.getCentroid())+" ja existe 2");
					qtdCentroideNovo++;
					Cluster oldCluster =  centroidsTree.get(currentCluster.getCentroid());
				//	System.out.println("tamanho phtree antes de remover " + centroidsTree.size());
					centroidsTree.remove(oldCluster.getCentroid());
					//System.out.println("tamanho phtree depois de remover " + centroidsTree.size());
					Record oldRecord = oldCluster.getCatalogPoint(catalog);
					oldCluster.removeRecord(oldRecord);
					oldCluster.addRecord(record);
					currentCluster.removeRecord(record);
					currentCluster.addRecord(oldRecord);
					currentCluster.calculateCentroid();
					
					//System.out.println(Math.(36.991940+36.991684, 2));
					oldCluster.calculateCentroid();
					centroidsTree.put(currentCluster.getCentroid(), currentCluster);
					//System.out.println("tamanho phtree depois de add 1 " + centroidsTree.size());
				//	clustersCollection.add(currentCluster); //faltava adicionar essa linha ########################
					
					centroidsTree.put(oldCluster.getCentroid(), oldCluster);
					//System.out.println("tamanho phtree depois de add 2 " + centroidsTree.size());
				//	System.out.println("novos centroids 2 " + Arrays.toString(currentCluster.getCentroid()) + "e " +  Arrays.toString(oldCluster.getCentroid()));
					//System.out.println("records oldC " + oldCluster.getRecords().get(0).getId() + " e " + oldCluster.getRecords().get(1).getId()+ " e records currentC " + "records oldC " +currentCluster.getRecords().get(0).getId() + " e " +currentCluster.getRecords().get(1).getId());
				}else{
				 tamanhoantes = centroidsTree.size();
				 //System.out.println("Removendo " + Arrays.toString(currentCentroidDouble));
			     //System.out.println("Inserindo " + Arrays.toString(currentCluster.getCentroid()));

				 centroidsTree.remove(currentCentroidDouble); //n�o entendi pq 

				centroidsTree.put(currentCluster.getCentroid(),
						currentCluster);
				 tamanhodepois = centroidsTree.size();
				//if (tamanhoantes != tamanhodepois) {
				//	System.out.println(" Centroide " + Arrays.toString(currentCluster.getCentroid()) + " existe novo? " + centroidsTree.contains(currentCluster.getCentroid())+ " e o velho? " + Arrays.toString(currentCentroidDouble) + " " + centroidsTree.contains(currentCentroidDouble));
				//	System.out.println(currentCluster.getRecords().get(0).getId() );
				//}
				}
			} else {
				/**
				 * remove the object oldRecord from the cluster currentCluster,
				 * insert record in this cluster, and search another cluster for
				 * oldRecord
				 */

				double[] currentCentroidDouble = convertToDoubleArray(currentCentroid.getCentroidLongBits());

				Record oldRecord = currentCluster.getCatalogPoint(catalog);

				double oldDistance = distanceAlgorithm.calculate(
						oldRecord.getData(), currentCluster.getCentroid());

				if (currentDistance < oldDistance) {
					currentCluster.removeRecord(oldRecord);
					currentCluster.addRecord(record);
					currentCluster.calculateCentroid();
					if (centroidsTree.contains(currentCluster.getCentroid())){
						//System.out.println("o centroide novo ja existe 3");
						
						currentCluster.removeRecord(record);
						currentCluster.addRecord(oldRecord);
						currentCluster.calculateCentroid();
						centroidsCandidatesDistance.remove(currentCluster);
						searchCentroid(record, centroidsCandidatesDistance,
								catalog);
		
						
						
						
						//System.out.println(Math.(36.991940+36.991684, 2));
				
						//System.out.println("novos centroids " + Arrays.toString(currentCluster.getCentroid()) + "e " +  Arrays.toString(oldCluster.getCentroid()));
						//System.out.println("records oldC " + oldCluster.getRecords().get(0).getId() + " e " + oldCluster.getRecords().get(1).getId()+ " e records currentC " + "records oldC " +currentCluster.getRecords().get(0).getId() + " e " +currentCluster.getRecords().get(1).getId());
					}else{
					
					//System.out.println("Removendo " + Arrays.toString(currentCentroidDouble));
					//System.out.println("Inserindo " + Arrays.toString(currentCluster.getCentroid()));

				
					tamanhoantes = centroidsTree.size();
					centroidsTree.remove(currentCentroidDouble);

					centroidsTree
							.put(currentCluster.getCentroid(), currentCluster);
			
					 tamanhodepois = centroidsTree.size();
					//if (tamanhoantes != tamanhodepois) {
					//	System.out.println(" Centroide quartp " + Arrays.toString(currentCluster.getCentroid()) + " existe novo? " + centroidsTree.contains(currentCluster.getCentroid())+ " e o velho? " + Arrays.toString(currentCentroidDouble) + " " + centroidsTree.contains(currentCentroidDouble));
					//	System.out.println(currentCluster.getRecords().get(0).getId() );
					//}

					centroidsCandidatesDistance.clear();
					currentDistance = 10000;
				//	double[] centroid = new double[record.getData().length];

					/* search another cluster for oldRecord */
					Iterator<PVDEntry<Cluster>> results = rangeQuery(
							centroidsTree, oldRecord.getData(), epsilon);


					Cluster otherCluster = null;

					for (Iterator<PVDEntry<Cluster>> iterator = results; iterator
							.hasNext();) {

						otherCluster = iterator.next().getValue();

						

						double distance = distanceAlgorithm.calculate(
								oldRecord.getData(), otherCluster.getCentroid());

						if (!currentCluster.equals(otherCluster)) {

							centroidsCandidatesDistance.put(otherCluster,
									distance);

						}

					}
					searchCentroid(oldRecord, centroidsCandidatesDistance,
							catalog);
				}
				}else {

					centroidsCandidatesDistance.remove(currentCentroid);
					searchCentroid(record, centroidsCandidatesDistance, catalog);
				}
			}

			break;
		}

	}

	/**
	 * Pick all records from the largest catalog and assign them as initial
	 * clusters.
	 */
	private void catalogPointsCentroids() {
		System.out.println("Inicializando centroides");

		/**
		 * initialize the centroids pick k data points as centroids since they
		 * belongs to the same catalog (correctClassifier)
		 */

		/* index of centroids (phtree) */
		centroidsTree = new PhTreeVD<Cluster>(attributesLenght);
		// create a cluster and add a record from the largest catalog to each
		// cluster
		
		// colection of clusters
				
				boolean passou = false;
				int k = largestCatalogRecords.size();
		for (int i = 0; i < k; i++) {
			

			Cluster cluster = new Cluster(attributesLenght);
			cluster.addRecord(largestCatalogRecords.get(i));
			cluster.calculateCentroid();
		//	long[] centroidLong = cluster.getCentroidLongBits();

		
		
			//System.out.println("Inserindo " + Arrays.toString(cluster.getCentroid()));

			centroidsTree.put(cluster.getCentroid(), cluster);
			
			cluster = null;
			//centroidLong = null;

		}

		System.out.println("Centroides inicializados");
		System.out.println("quantidade na arvore " + centroidsTree.size());
		//System.out.println("clusterCollection " + clustersCollection.size());
		largestCatalogRecords = null;
	}

	/**
	 * Range Query, testei apenas para duas dimens�es
	 * 
	 * @param point
	 * @param epsilon
	 * @return
	 */
	public PVDIterator<Cluster> rangeQuery(PhTreeVD<Cluster> centroidsTree2,
			double[] point, double epsilon) {
		double[] min = new double[point.length];
		double[] max = new double[point.length];

		for (int i = 0; i < point.length; i++) {
			min[i] = (point[i] - epsilon);
			max[i] = (point[i] + epsilon);
		}

		return centroidsTree2.query(min, max);
	}

	public double[] convertToDoubleArray(long[] longArray) {
		double[] doubleArray = new double[attributesLenght];
		for (int i = 0; i < attributesLenght; i++) {
			doubleArray[i] = BitTools.toDouble(longArray[i]);
		}
		return doubleArray;

	}

}
