package intekrator.kbextraction;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import intekrator.representation.Pair;


/**
 * Preprocessing (e. g., preselection of sensors, discretization, etc.) for the knowledge base extractor.
 * 
 * @author Daan Apeldoorn
 */
public class Preprocessing 
{
	/** The "column index" representing all columns (wildcard). */
	public static final int ALL_COLUMNS = -1;
	
	
	/**
	 * Discretizes the values of sensors that only contain numeric data using clustering.
	 * If neither the maximum number of clusters nor any name mappings are provided for a column, it is tried to 
	 * infer the adequate number of clusters from data (by successively increasing it until empty clusters appear).
	 * Parameters maxClusters, names and infos maybe null, if no such information are provided.
	 * Note: For performance reasons, this method changes and returns the provided state-action sequence parameter (instead of internally copying it)!
	 * Also the parameter maxClusters can be changed by running this method.
	 * 
	 * @param stateActionSeq  the state-action sequence, whose parameters will be discretized (will be modified by the method!)
	 * @param maxClusters     the maximum number of clusters (keys are column indices starting from 0, where -1 stands for * ("all columns"); values are the maximum number of clusters for the respective columns); this parameter maybe changed inside the method
	 * @param names           the names for the clusters (keys are column indices starting from 0, where -1 stands for * ("all columns"); values are the namnes of clusters for the respective columns)
	 * @param infos           the column indices of the columns for whose clusters additional information should be provided within the names of symbols
	 * @return                the modified input state-action sequence, where purely numeric sensors are discretized 
	 */
	public static List<Pair<List<String>,String>> discretize( List<Pair<List<String>,String>> stateActionSeq, Map<Integer, Integer> maxClusters, Map<Integer, List<String>> names, Set<Integer> infos )
	{
		// Provide some feedback on what is currently done
		System.out.println( "Discretizing..." );
		
		
		//
		// Prepare optional parameters
		//
		
		// For reasons of unification, create empty version of all optional parameter not being provided
		if( maxClusters == null )
		{
			maxClusters = new HashMap<Integer, Integer>();
		}
		if( names == null )
		{
			names = new HashMap<Integer, List<String>>();
		}
		if( infos == null )
		{
			infos = new HashSet<Integer>();
		}
		
		
		//
		// Ensure that in case of the same columns are provided both for max. clusters and names,
		// the numbers of names are dominant (since they are "more concrete") and that if a * is provided
		// for names, this constraints the max. number of clusters dominantly for all clusters (despite those
		// being dominated by individually set name mappings)
		//
		
		// If a star is provided for column names, ensure that no max. number of clusters can differ from the provided
		// number of names by removing all provided max. number of clusters (the default max. number of all clusters
		// will be set in the next for loop together with all individual max. number of clusters provided by individual
		// name mappings!) 
		if( names.keySet().contains( ALL_COLUMNS ) )
		{
			maxClusters.clear();
		}
		
		// Ensure that if there are individual name sets being provided, max. number of clusters is correctly adapted
		for( int columnNo: names.keySet() )
		{
			maxClusters.put( columnNo, names.get( columnNo ).size() );
		}
				
		
		// Create a list for each sensor and the action column to store the numeric values
		// (lists will be deleted later if the sensor does not only provide numeric values)
		HashMap<Integer, List<Double>> columnsWithNumData = new HashMap<Integer, List<Double>>();
		if( stateActionSeq.size() > 0 )
		{
			// For each sensor
			for( int i = 0; i < stateActionSeq.get( 0 ).getFirst().size(); i++ )
			{
				columnsWithNumData.put( i, new ArrayList<Double>() );
			}
			
			// For the action column
			columnsWithNumData.put( stateActionSeq.get( 0 ).getFirst().size(), new ArrayList<Double>() );
		}
		
		// Fill the lists with numeric data and remove non-numeric columns
		for( Pair<List<String>,String> pair : stateActionSeq )
		{
			// If there are no numeric sensors (anymore), stop it!
			if( columnsWithNumData.isEmpty() )
			{
				break;
			}

			// Check for each value of the sensor data row, if it is numeric;
			// if so, add it to the respective list, otherwise, remove the sensor
			int i = 0;
			for( String value : pair.getFirst() )
			{
				try
				{
					if( columnsWithNumData.containsKey( i ) )
					{
						Double valueAsDouble = Double.parseDouble( value );
						columnsWithNumData.get( i ).add( valueAsDouble );
					}
				}
				catch( NumberFormatException e )
				{
					columnsWithNumData.remove( i );
				}
				
				i++;
			}
			
			// Check for action, if it is numeric;
			// if so, add it to the respective list, otherwise, remove the action column
			try
			{
				if( columnsWithNumData.containsKey( pair.getFirst().size() ) )
				{
					Double valueAsDouble = Double.parseDouble( pair.getSecond() );
					columnsWithNumData.get( pair.getFirst().size() ).add( valueAsDouble );
				}
			}
			catch( NumberFormatException e )
			{
				columnsWithNumData.remove( pair.getFirst().size() );
			}
		}

		// Perform clustering on each column
		HashMap<Integer, List<List<Double>>> columnClusters = new HashMap<Integer, List<List<Double>>>();
		HashMap<Integer, double[]> centroids = new HashMap<Integer, double[]>();
		for( Integer columnNo : columnsWithNumData.keySet() )
		{
			// Search for the min./max. values and store them
			double minNumValue = Double.POSITIVE_INFINITY;
			double maxNumValue = Double.NEGATIVE_INFINITY;
			for( double numValue : columnsWithNumData.get( columnNo ) )
			{
				if( numValue < minNumValue )
				{
					minNumValue = numValue;
				}
				if( numValue > maxNumValue )
				{
					maxNumValue = numValue;
				}
			}
			
			
			//
			// Perform clustering with the min./max. values as initial centroids
			//
			
			// If min. is equal to max., there is only one value for the whole sensor,
			// thus there will be only one cluster (and therefore clustering can be skipped)
			double[] initCentroids = null; 
			List<List<Double>> clusters = null;
			if( minNumValue != maxNumValue )
			{
				// Start with two clusters (will be successively increased later)
				initCentroids = new double[]{ minNumValue, maxNumValue };
				clusters = kMeansClustering( columnsWithNumData.get( columnNo ), initCentroids ); 
			}
			else
			{
				clusters = new ArrayList<List<Double>>();
				clusters.add( columnsWithNumData.get( columnNo ) );
				
				// Since there will be only one value for the whole column here (minNumValue == maxNumValue), 
				// store the only value as centroid
				initCentroids = new double[]{ minNumValue };
			}
			
			// If there are any clusters, check if one of the clusters is empty
			boolean isOneOfTheClustersEmpty = false;
			for( int j = 0; j < clusters.size(); j++ )
			{
				if( clusters.get( j ).isEmpty() )
				{
					isOneOfTheClustersEmpty = true;
					break;
				}
			}
			
			// If there are at least two clusters and none of them is empty and the maximum of clusters is not yet reached
			// (Note that the maximum number of clusters cannot differ from the provided number of names, which is ensured at the beginning of the preprocessing!) 
			if( (clusters.size() > 1) && !isOneOfTheClustersEmpty && (    (!maxClusters.containsKey( columnNo ) && !maxClusters.containsKey( ALL_COLUMNS )) 
					                                                   || (maxClusters.containsKey( columnNo ) && (clusters.size() < maxClusters.get( columnNo ))) 
					                                                   || (!maxClusters.containsKey( columnNo ) && maxClusters.containsKey( ALL_COLUMNS ) && (clusters.size() < maxClusters.get( ALL_COLUMNS ))) ) )
			{
				// Successively increase the cluster size (continuing with three clusters)
				for( int i = 2; i < stateActionSeq.size(); i++ )
				{
					// Do clustering with i + 1 clusters
					double[] newInitCentroids = new double[ i + 1 ];
					newInitCentroids[ 0 ] = minNumValue;
					newInitCentroids[ newInitCentroids.length - 1 ] = maxNumValue;
					double step = (maxNumValue - minNumValue) / i;
					for( int j = 1; j < (newInitCentroids.length - 1); j++ )
					{
						newInitCentroids[ j ] = newInitCentroids[ j - 1 ] + step;
					}
					List<List<Double>> newClusters = kMeansClustering( columnsWithNumData.get( columnNo ), newInitCentroids ); 
					
					// Is one of the clusters empty?
					isOneOfTheClustersEmpty = false;
					for( int j = 0; j < newClusters.size(); j++ )
					{
						if( newClusters.get( j ).isEmpty() )
						{
							isOneOfTheClustersEmpty = true;
							break;
						}
					}
					
					// Break if one of the clusters is empty, if there are no names provided for the clusters
					// (otherwise the empty clusters are needed later to infer which of the names have to be used!)
					if( isOneOfTheClustersEmpty && (!names.containsKey( columnNo )) && (!names.containsKey( ALL_COLUMNS )) )
					{
						// (Take the previous clusters, without the empty one)
						break;
					}
					
					// ...else if the maximum number of clusters is provided for the current column (or for all columns)
					// (Note that the maximum number of clusters cannot differ from the provided number of names, which is ensured at the beginning of the preprocessing!) 
					else if(    (maxClusters.containsKey( columnNo ) && (newClusters.size() >= maxClusters.get( columnNo ))) 
							 || (!maxClusters.containsKey( columnNo ) && maxClusters.containsKey( ALL_COLUMNS ) && (newClusters.size() >= maxClusters.get( ALL_COLUMNS ))) )
					{
						initCentroids = newInitCentroids;
						clusters = newClusters;
						break;
					}
					
					// ...else (no empty clusters or empty cluster, but names are provided and number of clusters did not yet reach the number of names): Continue!
					else
					{
						initCentroids = newInitCentroids;
						clusters = newClusters;
					}
				}	
			}

			
			// Store clusters
			columnClusters.put( columnNo, clusters );
			
			// Store centroids
			centroids.put( columnNo, initCentroids );
		}

		// Replace all values in the original state-action sequence, that belong 
		// to the corresponding cluster with "Sx:sy", where x = sensorNo denotes the x-th sensor
		// and y = the number of the corresponding cluster (and respectively for the action column).
		// Also add additional information about the number of clusters and the cluster size (if desired)
		Map<Integer, Map<Double, String>> numbersToNames = new HashMap<Integer, Map<Double, String>>();  // Maps sensors to maps that map numeric sensor values to symbol names; will be stored for later use
		int i = 0;
		for( Pair<List<String>,String> pair : stateActionSeq )
		{
			for( int columnNo : columnClusters.keySet() )
			{
				// Calculate the number of clusters by only considering non-empty ones
				// (only if names are provided for the column, there can by empty clusters (in case too many names were provided)!)
				int clusterCount = columnClusters.get( columnNo ).size();
				if( names.containsKey( columnNo ) || names.containsKey( ALL_COLUMNS ) )
				{
					clusterCount = 0;
					for( List<Double> cluster : columnClusters.get( columnNo ) )
					{
						if( !cluster.isEmpty() )
						{
							clusterCount++;
						}
					}
				}
				
				// Iterate over all clusters representing the sensor values inferred by clustering
				for( int valueNo = 0; valueNo < columnClusters.get( columnNo ).size(); valueNo++ )
				{
					// Empty clusters are "automatically" skipped here, since the do not contain any values
					if( columnClusters.get( columnNo ).get( valueNo ).contains( columnsWithNumData.get( columnNo ).get( i ) ) )
					{
						// Get the name of the sensor value/action, if provided
						// and store if it's a "general" name for all columns or not
						boolean isGeneralName = false;
						String name = null;
						if( names.containsKey( columnNo ) )
						{
							name = names.get( columnNo ).get( valueNo ); 
						}
						else if( names.containsKey( ALL_COLUMNS ) )
						{
							name = names.get( ALL_COLUMNS ).get( valueNo ); 
							isGeneralName = true;
						}

						// Create info, if desired
						String info = "";
						if( infos.contains( columnNo ) || infos.contains( ALL_COLUMNS ) )
						{
							info = "(" + centroids.get( columnNo )[ valueNo ] +  ")/" + clusterCount + "(" + Math.round( (((double) columnClusters.get( columnNo ).get( valueNo ).size()) / stateActionSeq.size()) * 100.0 ) + "%)";
						}

						// Decide between sensor columns or the actino column
						if( columnNo < pair.getFirst().size() )
						{
							// Set default name for sensor value, if not provided
							if( name == null )
							{
								name = "S" + (columnNo + 1) + ":s" + (valueNo + 1);
							}
							else if( isGeneralName )
							{
								name = "S" + (columnNo + 1) + ":" + name;
							}
							
							// Add sensor and value to the mapping of numeric values to symbol names 
							if( !numbersToNames.containsKey( columnNo + 1 ) )
							{
								numbersToNames.put( columnNo + 1, new HashMap<Double, String>() );
							}
							numbersToNames.get( columnNo + 1 ).put( columnsWithNumData.get( columnNo ).get( i ) , name + info );
							
							pair.getFirst().set( columnNo, name + info );
						}
						else
						{
							// Set default name for action, if not provided
							if( name == null )
							{
								name = "a" + (valueNo + 1);
							}

							// TODO Maybe extend Pair class with setter methods to avoid recreating a new pair here
							String discretizedAction = name + info;
							Pair<List<String>, String> newPair = new Pair<List<String>, String>( pair.getFirst(), discretizedAction );
							stateActionSeq.set( i, newPair );
						}
					}
				}
			}
		
			i++;
		}

		// Write mapping of numeric sensor values to symbol names to file (for later use)
		String mappingFilename = KBExtractor.outputFilename.substring( 0, KBExtractor.outputFilename.lastIndexOf( "." ) ) + ".map";
		try 
		{
			ObjectOutputStream numbersToNamesFile = new ObjectOutputStream( new FileOutputStream( mappingFilename ) );
			numbersToNamesFile.writeObject( numbersToNames );
			numbersToNamesFile.close();
		} 
		catch( IOException e ) 
		{
			System.out.println( "Warning: Problem writing mapping file: " + mappingFilename );
		}
		
		return stateActionSeq;
	}

		
	/**
	 * Preselects the most relevant provided number of sensors.
	 * 
	 * @param stateActionSeq  the data based on which the sensors are selected
	 * @param count           the number of sensors selected (-1 for automatic detection)
	 * @return                a state-action sequence containing only the preselected data
	 */
	public static List<Pair<List<String>,String>> preselectSensors( List<Pair<List<String>,String>> stateActionSeq, int count )
	{
		// Provide some feedback on what is currently done
		System.out.print( "Preselecting sensors..." );
		
		// Limit the provided number of sensors to the number of sensors provided in the data
		if( stateActionSeq.size() > 0 )
		{
			if( count > stateActionSeq.get( 0 ).getFirst().size() )
			{
				count = stateActionSeq.get( 0 ).getFirst().size();
			}
		}
		else
		{
			count = 0;
		}
		
		
		//
		// Iterate over all data and calculate for each sensor s_i the probability avg_{s_i}( max_a( P( a ^ s_i ) ) )
		//
		
		// Create sets of distinct sensor values for each sensor s_i and actions
		List<Set<String>> distinctSensorValues = new ArrayList<Set<String>>();		
		Set<String> distinctActions = new HashSet<String>();
		for( Pair<List<String>,String> pair : stateActionSeq )
		{
			// For the sensors
			int sensorNo = 0;
			for( String state : pair.getFirst() )
			{
				// Create the set first, if it does not yet exist for the sensor with this number
				if( sensorNo > (distinctSensorValues.size() - 1) )
				{
					distinctSensorValues.add( new HashSet<String>() );
				}			
				distinctSensorValues.get( sensorNo ).add( state );
				
				sensorNo++;
			}
			
			// For the actions
			distinctActions.add( pair.getSecond() );
		}

		// Create the average probabilities and store the corresponding value in a map, 
		// where the keys are the indices referring to the respective sensor
		Map<Integer, Double> avgProbabilities = new HashMap<Integer, Double>();
		for( Set<String> sensorValues : distinctSensorValues )
		{
			// Create average over the max. probabilities of a state in conjunction with an action
			// times the max. conditional probability of an action given a state:
			// avg_{s_i}( max_a( P( a ^ s_i ) ) * max_a( P( a | s_i ) )
			avgProbabilities.put( avgProbabilities.size(), 0.0 );
			for( String sensorValue : sensorValues )
			{
				// Search for the max. probability of a state in conjunction with an action max_a( P( a ^ s_i ) )
				// and for the max. conditional probability of an action given a state max_a( P( a | s_i ) )
				double maxProbForAction = 0.0;
				double maxConditionalProbForAction = 0.0;
				for( String action : distinctActions )
				{
					HashSet<String> state = new HashSet<String>();
					state.add( sensorValue );
					double probForAction = KBExtractor.P( stateActionSeq, action, state );
					if( probForAction > maxProbForAction )
					{
						maxProbForAction = probForAction;
					}
					double conditionalProbForAction = KBExtractor.conditionalP( stateActionSeq, action, state );
					if( conditionalProbForAction > maxConditionalProbForAction )
					{
						maxConditionalProbForAction = conditionalProbForAction;
					}
				}

				// Sum them up (to create the average later through dividing by the number of distinct sensor values)
				avgProbabilities.put( avgProbabilities.size() - 1, avgProbabilities.get( avgProbabilities.size() - 1 ) + (maxProbForAction * maxConditionalProbForAction) );
			}
	
			// Divide by the number of distinct sensor values to create the average
			avgProbabilities.put( avgProbabilities.size() - 1, avgProbabilities.get( avgProbabilities.size() - 1 ) / sensorValues.size() );
		}
		
		
		//
		// Create a list of the indices of the best sensors
		//
		
		List<Integer> bestSensorIndices = new ArrayList<Integer>();

		// If a number of sensors to be taken is provided
		if( count > -1 )
		{
			// Take the n best sensor indices and put them in a list
			// starting with the best sensor
			for( int i = 0; i < count; i++ )
			{
				// Search for the max. strength
				double maxStrength = 0.0;
				for( int sensorNo : avgProbabilities.keySet() )
				{
					if( avgProbabilities.get( sensorNo ) > maxStrength )
					{
						maxStrength = avgProbabilities.get( sensorNo );
					}
				}
				
				// Get all sensors having this strength
				List<Integer> sensorsWithMaxStrength = new ArrayList<Integer>();
				for( int sensorNo : avgProbabilities.keySet() )
				{
					if( avgProbabilities.get( sensorNo ) == maxStrength )
					{
						sensorsWithMaxStrength.add( sensorNo );
					}
				}
				
				// Select one of the candidates randomly
				int maxSensor = sensorsWithMaxStrength.get( KBExtractor.random.nextInt( sensorsWithMaxStrength.size() ) );
				
				bestSensorIndices.add( maxSensor );
				avgProbabilities.remove( maxSensor );
			}
		}
		
		// ...else infer the number automatically
		else
		{
			// Search for the min. and the max. sensor and store its corresponding values
			double minAvgProb = 1.0;
			double maxAvgProb = 0.0;
			for( double avgProb : avgProbabilities.values() )
			{
				if( avgProb < minAvgProb )
				{
					minAvgProb = avgProb;
				}
				if( avgProb > maxAvgProb )
				{
					maxAvgProb = avgProb;
				}
			}
			
			// Perform a 2-means clustering on the sensors with the min. and the max. values as initial centroids
			double[] initCentroids = new double[]{ minAvgProb, maxAvgProb };
			List<List<Double>> clusters = kMeansClustering( avgProbabilities.values(), initCentroids );
			
			// Take the higher cluster, put the sensor indices in a list
			List<Double> higherCluster = null;
			if( clusters.size() > 1 )
			{
				higherCluster = clusters.get( 1 );
			}
			else
			{
				higherCluster = clusters.get( 0 );
			}
			
			// Take the sensor indices of the values in the higher cluster 
			// and put them in a list starting from the best sensor 
			Collections.sort( higherCluster );
			for( double avgProb : higherCluster )
			{
				// Get all sensors having the current strength of the higher cluster
				List<Integer> higherClusterSensors = new ArrayList<Integer>();
				for( int sensorNo : avgProbabilities.keySet() )
				{
					if( avgProbabilities.get( sensorNo ) == avgProb )
					{
						higherClusterSensors.add( sensorNo );
					}
				}
				
				// Shuffle to create a random order of equivalent sensors 
				Collections.shuffle( higherClusterSensors );
				
				bestSensorIndices.addAll( higherClusterSensors );
				for( int sensorNo : higherClusterSensors )
				{
					avgProbabilities.remove( sensorNo );
				}
			}		
		}
		
		
		// Create new modified state-action sequence
		List<Pair<List<String>,String>> preselectedStateActionSeq = new ArrayList<Pair<List<String>,String>>();
		for( Pair<List<String>,String> pair : stateActionSeq )
		{
			// Only select the sensor columns that have been determined before
			List<String> preselectedState = new ArrayList<String>();
			for( int index : bestSensorIndices )
			{
				preselectedState.add( pair.getFirst().get( index ) );
			}
			
			// Create a new reduced state-action pair and add to the new state-action sequence
			Pair<List<String>,String> preselectedPair = new Pair<List<String>,String>( preselectedState, pair.getSecond() );
			preselectedStateActionSeq.add( preselectedPair );
		}

		// Provide some feedback on the number of selected sensors
		int selectedSensors = 0;
		if( !preselectedStateActionSeq.isEmpty() )
		{
			selectedSensors = preselectedStateActionSeq.get( 0 ).getFirst().size();
		}
		System.out.println( " (" + selectedSensors + " selected)" );
		
		return preselectedStateActionSeq;
	}

	
	/**
	 * Preselects the most relevant provided number of sensors.
	 * The number of relevant sensors is automatically inferred from data.
	 * 
	 * @param stateActionSeq  the data based on which the sensors are selected
	 * @return                a state-action sequence containing only the preselected data
	 */
	public static List<Pair<List<String>,String>> preselectSensors( List<Pair<List<String>,String>> stateActionSeq )
	{
		return preselectSensors( stateActionSeq, -1 );		
	}

	
	/**
	 * Performs a k-means clustering with the provided initial centroids (where k is the number of centroids provided).
	 * Note that the order of centroids determines the order of resulting clusters.
	 * The final centroids are provided in the centroids parameter after returning.
	 * 
	 * @param values         the list of double values on which the clustering is performed
	 * @param centroids      the initial centroids (the order of centroids determines the order of resulting clusters); after returning, the final centroids are provided here
	 * @return               the clusters
	 */
	public static List<List<Double>> kMeansClustering( Collection<Double> values, double[] centroids )
	{
		// Create empty clusters
		@SuppressWarnings("unchecked")
		List<Double>[] clusters = new ArrayList[ centroids.length ];
		for( int i = 0; i < centroids.length; i++ )
		{
			clusters[ i ] = new ArrayList<Double>();
		}
		
		// Create empty old clusters (to observe changes)
		@SuppressWarnings("unchecked")
		List<Double>[] clustersOld = new ArrayList[ centroids.length ];
		for( int i = 0; i < centroids.length; i++ )
		{
			clusters[ i ] = new ArrayList<Double>();
		}
		
		boolean hasOneOfTheClusterChanged = false;  // Termination condition
		do
		{
			// Store old clusters, clear new ones
			for( int i = 0; i < centroids.length; i++ )
			{
				clustersOld[ i ] = new ArrayList<Double>();
				clustersOld[ i ].addAll( clusters[ i ] );
				clusters[ i ] = new ArrayList<Double>();
			}
			
			// Fill clusters
			for( double value : values )
			{
				// Search for the best fitting cluster
				int bestClusterNo = 0;
				for( int i = 0; i < centroids.length; i++ )
				{
					if( Math.abs( value - centroids[ i ] ) < Math.abs( value - centroids[ bestClusterNo ] ) )
					{
						bestClusterNo = i;
					}
				}

				// Add value to the best fitting cluster
				clusters[ bestClusterNo ].add( value );
			}
			
			// Recalculate centroids
			for( int i = 0; i < centroids.length; i++ )
			{
				centroids[ i ] = 0.0;
				for( double value : clusters[ i ] )
				{
					centroids[ i ] += value;
				}
				centroids[ i ] /= clusters[ i ].size();
			}
		
			// Determine whether one of the clusters has changed in this iteration
			hasOneOfTheClusterChanged = false;
			for( int i = 0; i < centroids.length; i++ )
			{
				if( !((clusters[ i ].size() == clustersOld[ i ].size()) && clusters[ i ].containsAll( clustersOld[ i ] )) )
				{
					hasOneOfTheClusterChanged = true;
					break;
				}
			}
		}
		
		// Do as long as one of the clusters changed
		while( hasOneOfTheClusterChanged );
	
		// Add the clusters to the results list
		List<List<Double>> result = new ArrayList<List<Double>>();
		for( int i = 0; i < centroids.length; i++ )
		{
			result.add( clusters[ i ] );
		}
		
		return result;
	}
}
