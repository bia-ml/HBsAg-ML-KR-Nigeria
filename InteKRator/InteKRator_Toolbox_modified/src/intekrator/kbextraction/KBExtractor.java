package intekrator.kbextraction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import intekrator.representation.MultiAbstractionLevelKB;
import intekrator.representation.Pair;
import intekrator.representation.Rule;
import intekrator.revision.KBRevision;


/**
 * A knowledge extractor to create compact knowledge representations from state-action sequences.
 * 
 * @author Daan Apeldoorn
 */
public class KBExtractor 
{
	/** Determines that the extracted KB will be written to standard out during learning. */
	public static boolean silentMode = true;

	/** The name of the output file to which the resulting knowledge base is written; if no filename is provided, the output is not written to a file. */
	public static String outputFilename = null;
	
	/**
	 * The available modes for keeping rules that would be otherwise deleted during extraction 
	 * for more compact representations.
	 * TOP:   Ensures that the resulting knowledge base will have a top most rule
	 * ALL:   Ensures that no superfluous rules are deleted according to the given data
	 * NONE:  The original algorithm: Leads to more compact representations, since only rules are kept that are needed according to the provided state-action sequence
	 */
	public enum RuleKeepingMode { TOP, ALL, NONE }
	
	/** Determines the rule keeping mode. */
	public static RuleKeepingMode ruleKeepingMode = RuleKeepingMode.NONE;
	
	/** Determines whether or not the algorithm runs in discretization mode (i. e., sensors that contain only numeric data are automatically discretized). */
	public static boolean discretizationMode = false;

	/** The maximum number of discretization clusters (the map's values) for each provided column (the map's keys). */
	public static Map<Integer, Integer> maxClusters = null;

	/** The names of the discretization clusters (the map's values) for each provided column (the map's keys). */
	public static Map<Integer, List<String>> clusterNames = null;
	
	/** The columns for which additional infos should be shown. */
	public static Set<Integer> clusterInfos = null;

	/** Determines whether or not the algorithm runs in preselection mode (i. e., sensors are preselected at the beginning); the original algorithm runs without preselection. */
	public static boolean preselectionMode = false;

	/** The number of sensors to be preselected (if -1, the number will be automatically inferred from data). */
	public static int preselectionSensorsCount = -1;

	/** Determines whether or not the algorithm runs ins sampling mode (i. e., state-action pairs are randomly selected at the beginning); the original algorithm runs without sampling. */
	public static boolean samplingMode = false;

	/** The sampling value: either providing the number of state-action pairs to be selected or the percentage of state-action pairs to be selected. */
	public static double samplingValue = 0;
	
	/** Determines whether the provided sampling value is considered as percentage. */
	public static boolean samplingValueIsPercentage = false;
	
	/** Determines whether the progress will be calculated and shown. */
	public static boolean showProgress = false;

	/** Determines whether the details of the progress will be calculated and shown (i. e., the individual progress of each action). */
	public static boolean showProgressDetails = false;

	/** Determines whether the progress should be reported in the given file (if null, progress will be printed to standard out). */
	public static String progressFilename = null;

	/** The indicator (suffix) for values to be avoided. */
	public static String avoidValueIndicator = null;
	
	/**
	 * The available modes for selecting the most important rules.
	 * MARK:      The most important rules will be marked
	 * ONLY:      Only the most important rules will be kept
	 * ONLY_TOP:  Only the most important rules and the top rule will be kept
	 * NONE:      Importance of rules is not considered
	 */
	public enum ImportantRulesMode { MARK, ONLY, ONLY_TOP, NONE }
	
	/** Determines the rule keeping mode. */
	public static ImportantRulesMode importantRulesMode = ImportantRulesMode.NONE;

	/** The random generator to be used for sampling (the same random generator will be used by Preprocessing). */
	public static Random random = new Random();
	
	/** The possible actions (the action space). */
	private static HashSet<String> possibleActions = null;
	
	/** Stores relative frequencies P( action ^ state ) mapped to the corresponding state-action pairs to prevent them from being recalculated multiple times. */
	private static HashMap<Pair<Set<String>, String>, Double> cachedPs = new HashMap<Pair<Set<String>, String>, Double>();

	/** Stores conditional relative frequencies P( action | state ) mapped to the corresponding state-action pairs to prevent them from being recalculated multiple times. */
	private static HashMap<Pair<Set<String>, String>, Double> cachedConditionalPs = new HashMap<Pair<Set<String>, String>, Double>();
	
	/** 
	 * Stores rules whose weights from a converted deterministic state-action were already replaced by the weights from the original non-deterministic state-action sequence by the method "replaceWeightsNonDeterministic()".
	 * (This is needed since the knowledge base is written on the fly by relying on the weights of the deterministic sequence; thus the rule weights need to be replaced after every iteration.)
	 */
	private static HashMap<Rule, Double> replacedWeightsNonDeterministic = new HashMap<Rule, Double>();

	
	/**
	 * Returns the relative frequency of the given action in conjunction with the given state
	 * occurring in the given state-action sequence. 
	 * (The calculated values can later be cached for more performance)
	 * 
	 * @param stateActionSeq  the given state-action sequence
	 * @param action          the given action
	 * @param state           the given state
	 * @return                the relative frequency P( action ^ state )
	 */
	public static double P( List<Pair<List<String>,String>> stateActionSeq, String action, Set<String> state )
	{
		// Perform caching
		Pair<Set<String>, String> cachedStateActionPair = new Pair<Set<String>, String>( state, action ); 
		if( cachedPs.containsKey( cachedStateActionPair ) )
		{
			return( cachedPs.get( cachedStateActionPair ) );
		}
					
		// Count the occurrences of action ^ state
		double countState = 0;
		double countStateAndAction = 0;
		for( Pair<List<String>,String> stateActionPair : stateActionSeq )
		{
			// Count the state
			if( stateActionPair.getFirst().containsAll( state ) )
			{
				countState++;
				
				// Count the action, given the state
				if( stateActionPair.getSecond().equals( action ) )
				{
					countStateAndAction++;
				}
			}
		}

		// Perform caching
		cachedPs.put( cachedStateActionPair, countStateAndAction / stateActionSeq.size() );
		cachedConditionalPs.put( cachedStateActionPair, countStateAndAction / countState );
		
		return( countStateAndAction / stateActionSeq.size() );
	}
	

	/**
	 * Returns the conditional relative frequency of the given action assuming the given state
	 * occurring in the given state-action sequence. 
	 * (The calculated values can later be cached for more performance)
	 * 
	 * @param stateActionSeq  the given state-action sequence
	 * @param action          the given action
	 * @param state           the given state (condition part)
	 * @return                the relative frequency P( action | state )
	 */
	public static double conditionalP( List<Pair<List<String>,String>> stateActionSeq, String action, Set<String> state )
	{
		// Perform caching
		Pair<Set<String>, String> cachedStateActionPair = new Pair<Set<String>, String>( state, action ); 
		if( cachedConditionalPs.containsKey( cachedStateActionPair ) )
		{
			return( cachedConditionalPs.get( cachedStateActionPair ) );
		}
		
		// Count the occurrences of the state and the action given the state
		double countState = 0;
		double countActionGivenState = 0;
		for( Pair<List<String>,String> stateActionPair : stateActionSeq )
		{
			// Count the state
			if( stateActionPair.getFirst().containsAll( state ) )
			{
				countState++;
				
				// Count the action, given the state
				if( stateActionPair.getSecond().equals( action ) )
				{
					countActionGivenState++;
				}
			}
		}

		// Perform caching
		cachedPs.put( cachedStateActionPair, countActionGivenState / stateActionSeq.size() );
		cachedConditionalPs.put( cachedStateActionPair, countActionGivenState / countState );
				
		return( countActionGivenState / countState );
	}
	
	
	/**
	 * Returns all state-action pairs from the given state-action sequence that are not correctly
	 * represented by the given HKB. The rules involved in the correctly represented state-action pairs
	 * are returned in the additional parameter "usedRules".
	 * 
	 * @param stateActionSeq  the state-action pairs from which the not correctly represented pairs are selected 
	 * @param kb              the HKB through which it is calculated which state-action pairs are not correctly represented by invoking the reasoning algorithm
	 * @param usedRules       the rules used for the correctly represented state-action pairs
	 * @return                those state-action pairs that remain not correctly represented
	 */
	private static List<Pair<List<String>,String>> getRemainingStateActionPairs( List<Pair<List<String>,String>> stateActionSeq, MultiAbstractionLevelKB kb, List<Rule> usedRules )
	{
		// Invoke for every state-action pair the reasoning algorithm
		List<Pair<List<String>,String>> remainingStateActionPairs = new LinkedList<Pair<List<String>,String>>();
		for( Pair<List<String>,String> stateActionPair : stateActionSeq )
		{
			// Get the reasoning result(s) and the firing rule(s) for the current state-action pair
			Set<Rule> firingRules = new HashSet<Rule>();
			LinkedList<String> reasoningResult = kb.reasoning( new HashSet<String>( stateActionPair.getFirst() ), firingRules );
			
			// If the right conclusion could not be uniquely inferred,
			// add the current state-action pair as not correctly represented remaining pair (i. e., more exceptions are needed)
			if( !reasoningResult.contains( stateActionPair.getSecond() ) || (new HashSet<String>( reasoningResult )).size() > 1 )
			{
				remainingStateActionPairs.add( stateActionPair );
			}
			
			// ...if the right conclusion was found
			else
			{
				// Create list of all rules on the bottom most level with the correct conclusion
				// and add used rules from the upper levels if any
				List<Rule> correctBottomMostRules = new LinkedList<Rule>();
				for( Rule rule : firingRules )
				{
					if( (rule.getPremise().size() == kb.getNoOfLevels() - 1) && rule.getConclusion().equals( stateActionPair.getSecond() ) )
					{
						correctBottomMostRules.add( rule );
					}
					else if( rule.getConclusion().equals( stateActionPair.getSecond() ) && !usedRules.contains( rule ) )
					{
						usedRules.add( rule );
					}
				}

				// Choose a random rule among the rules with correct conclusion
				if( !correctBottomMostRules.isEmpty() )
				{	
					// Determine whether one of the equivalent bottom most rules is already used;
					// if so, nothing else has to be done here
					boolean isRuleAlreadyUsed = false;
					for( Rule rule : correctBottomMostRules )
					{
						if( usedRules.contains( rule ) )
						{
							isRuleAlreadyUsed = true;
							break;
						}
					}
					
					// If not one of the equivalent rules is already used, select and add random rule among 
					// the correct bottom most rules
					if( !isRuleAlreadyUsed )
					{
						// Note that this case can later lead to remaining unused rules since from a couple of unused rules here one could 
						// be selected which will be disadvantageous regarding some other rule that will be selected later. 
						// A solution like "always shuffling in the same way" will not help!
						// Example: Let's assume two equivalent unused rules r1, r2 for an sa-pair sa1, and later on two equivalent rules r2, r3 for another
						// sa-pair sa2. If for sa1 the rule r1 will be selected at random, then for sa2 we must select a rule which potentially leads to a 
						// less compact HKB). 
						// A solution is to again delete all unused rules from a level after a level is finished (see below). 

						
						// Determine the highest conditional probability
						double highestCondP = Double.NEGATIVE_INFINITY;
						for( Rule rule : correctBottomMostRules )
						{
							double condP = conditionalP( stateActionSeq, rule.getConclusion(), rule.getPremise() );
							if( condP > highestCondP )
							{
								highestCondP = condP;
							}
						}
						
						// Collect all rules among the correct bottom most rules with the highest conditional probability
						List<Rule> bestRules = new LinkedList<Rule>();
						for( Rule rule : correctBottomMostRules )
						{
							double condP = conditionalP( stateActionSeq, rule.getConclusion(), rule.getPremise() );
							if( condP == highestCondP )
							{
								bestRules.add( rule );
							}
						}

						// TODO Offer alternative deterministic selection strategies here!
						java.util.Collections.shuffle( bestRules );						
						Rule randomRule = bestRules.get( 0 );
						usedRules.add( randomRule );
					}
				}
			}
		}
		
		return( remainingStateActionPairs ); 
	}
	
	
	/**
	 * Returns all subsets of length k of the given set.
	 * The method is used to calculate all indices of k subsets from a given set. 
	 * These indices can be cached which is useful to efficiently calculate 
	 * the k subsets for different sets of the same length.
	 * 
	 * @param set  a set of subsequent indices (starting with index 0 and ending with index set length minus 1) whose subsets will be returned
	 * @param k    the length of the returned subsets
	 * @return     the index subsets of length k
	 */
	private static List<Set<Integer>> kSubsetsIndices( List<Integer> set, int k )
	{
		// The non-trivial case...
		if( k > 1 )
		{
			// Create every k - 1 subset (setsMinus1) and combine it with one element of the current 
			// considered (ordered) set, starting with the first element up to the last element of the 
			// current considered set that allows for creating a k - 1 subset with the remaining elements
			List<Set<Integer>> result = new ArrayList<Set<Integer>>();
			List<Set<Integer>> setsMinus1New = null;
			for( int i = 0; i < set.size() - (k - 1); i++ )
			{
				// Store the element of the ordered set to be combined with the sub sets of the remaining list
				Integer firstElement = set.get( i );
				
				// Create a sublist of the remaining elements up to the end of the current considered list
				List<Integer> listMinus1 = set.subList( i + 1, set.size() );
				
				// Create all k - 1 subsets of the remaining elements
				List<Set<Integer>> setsMinus1 = null; 
				if( setsMinus1New == null )
					setsMinus1 = kSubsetsIndices( listMinus1, k - 1 );
				else
					setsMinus1 = setsMinus1New;
				
				// Combine the previously stored element with all created k - 1 subsets to create 
				// the k subsets
				setsMinus1New = new ArrayList<Set<Integer>>();
				for( Set<Integer> setMinus1 : setsMinus1 )
				{
					// Keep all sets that can be reused for combinations in the next iteration over the given set
					// (This prevents the calculation of these combinations multiple times)
					if( !setMinus1.contains( set.get( i + 1 ) ) )
					{
						setsMinus1New.add( new HashSet<Integer>( setMinus1 ) );
					}
					
					// Do combination
					setMinus1.add( firstElement );
					result.add( setMinus1 );
				}
			}
			
			return( result );
		}
		
		// ...the trivial case k equals 1
		else if( k == 1 )
		{
			// Simply enumerate all sets of size one
			List<Set<Integer>> result = new ArrayList<Set<Integer>>();
			for( Integer element : set )
			{
				Set<Integer> oneElementSet = new HashSet<Integer>();
				oneElementSet.add( element );
				result.add( oneElementSet );
			}

			return( result );
		}
		
		// ...else k must be 0, resulting set will only contain the empty set
		else	
		{
			List<Set<Integer>> result = new ArrayList<Set<Integer>>();
			result.add( new HashSet<Integer>() );

			return( result );
		}
	}
		
	/** The cache storing the indices subsets from which all k subsets for a given k can be easily calculated. */
	private static Map<Integer, List<Set<Integer>>> cache = new HashMap<Integer, List<Set<Integer>>>();
	
	/** The size of the last set of which the k subsets were computed. */
	private static int lastSetSize = -1;
	
	/**
	 * Returns all subsets of length k of the given set.
	 * The subset indices are cached for the last k; if subsets for the same k and a (different) set
	 * of the same size are calculated subsequently, then the calculation runs much faster by using
	 * the cached subset indices.
	 * It's also possible to allow caches for more than only the last k (see TODO in this method).
	 * 
	 * @param set  the set whose subsets will be returned
	 * @param k    the length of the returned subsets
	 * @return     the subsets of length k
	 */
	private static List<Set<String>> kSubsets( List<String> set, int k )
	{
		// If no indices for k are cached yet or if the set size changed, 
		// calculate indices from scratch and cache them for the next time
		if( !cache.containsKey( k ) || lastSetSize != set.size() )
		{
			// Create indices set
			List<Integer> indices = new ArrayList<Integer>();
			for( int i = 0; i < set.size(); i++ )
			{
				indices.add( i );
			}
			
			// TODO Implement a more advanced strategy when to clear the cache!
			cache.clear();
			
			// Calculate indices k subsets
			List<Set<Integer>> indicesSets = kSubsetsIndices( indices, k );

			// Do caching, store last set size
			cache.put( k, indicesSets );
			lastSetSize = set.size();
		}
		
		// Create k subsets by accessing the cached k subset indices
		List<Set<String>> subsets = new ArrayList<Set<String>>();
		List<Set<Integer>> indicesSets = cache.get( k );
		for( Set<Integer> indicesSet : indicesSets )
		{
			Set<String> subset = new HashSet<String>();
			boolean isContainingAvoidValue = false;
			for( int i : indicesSet )
			{
				// If rules based on values to be avoided are not allowed, only consider the set as potential rule premise in case there is no avoid value contained
				// (otherwise omit the whole subset)
				if( (avoidValueIndicator == null) || (!set.get( i ).endsWith( avoidValueIndicator )) )
				{
					subset.add( set.get( i ) );
				}
				else
				{
					isContainingAvoidValue = true;
					break;
				}
			}
			if( !isContainingAvoidValue )
			{
				subsets.add( subset );
			}
		}
		
		return subsets;
	}
	
	
	/**
	 * Writes the given rules set (e. g., a layer in the knowledge base) to the file with the given name.
	 * 
	 * @param rules     the rules to be written to the file
	 * @param filename  the name of the file to be written to
	 * @param append    whether or not the written rules should be appended
	 */
	public static void writeToFile( String text, String filename, boolean append )
	{
		BufferedWriter file = null;
		try 
		{
			file = new BufferedWriter( new FileWriter( filename, append ) );

			// Write text to a new line of the file
			file.write( text );
		} 
		catch( IOException e ) 
		{
			System.out.println( "Error writing to file: " + filename );
		} 
		
		// Close the file
		if( file != null )
		{
			try 
			{
				file.close();
			} 
			catch( IOException e )
			{
				System.out.println( "Error closing file: " + filename );
			}
		}
	}
	
	
	/**
	 * Calculates and outputs the current progress
	 * 
	 * @param stateActionSeq             the state-action sequence from which the knowledge base is currently extracted
	 * @param remainingStateActionPairs  the state-action pairs that are currently remaining according to the so far calculated knowledge base 
	 * @param append                     whether or not the progress will be appended to the previous progress value
	 */
	private static void showProgress( List<Pair<List<String>,String>> stateActionSeq, List<Pair<List<String>,String>> remainingStateActionPairs, boolean append )
	{
		// Calculate progress
		long progress = Math.round( (((double) stateActionSeq.size() - remainingStateActionPairs.size()) / stateActionSeq.size()) * 100 );
		String progressString = progress + "%";
		
		// Calculate detailed progress, if desired
		if( showProgressDetails )
		{
			progressString += "   (";
			int actionCount = 0;
			for( String action : possibleActions )
			{
				long progressDetail = 0;
				for( Pair<List<String>, String> remainingPair : remainingStateActionPairs )
				{
					if( remainingPair.getSecond().equals( action ) )
					{
						progressDetail++;
					}
				}
				progressDetail = Math.round( (1.0 - (((double) progressDetail) / (stateActionSeq.size() * P( stateActionSeq, action, new HashSet<String>() )))) * 100 );
				
				// Update the progress string
				progressString += action + ": " + progressDetail + "%";
				actionCount++;
				if( actionCount < possibleActions.size() )
				{
					progressString += ", ";
				}
			}
			progressString += ")";
		}
		
		// Write to provided file or to standard out (if no filename was provided)
		if( progressFilename != null )
		{
			BufferedWriter file = null;
			try 
			{
				file = new BufferedWriter( new FileWriter( progressFilename, append ) );

				// Add a blank line at the beginning if in append mode
				if( append )
				{
					file.write( "\n" );
				}

				// Write progress
				file.write( progressString );
			} 
			catch( IOException e ) 
			{
				System.out.println( "Error writing to file: " + progressFilename );
			} 
			
			// Close the file
			if( file != null )
			{
				try 
				{
					file.close();
				} 
				catch( IOException e )
				{
					System.out.println( "Error closing file: " + progressFilename );
				}
			}
		}
		else
		{
			System.out.println( progressString );
		}
	}
	
	
	/**
	 * Performs a random sampling on the given state-action sequence. 
	 * 
	 * @param stateActionSeq  the state-action sequence to be sampled
	 * @param samplingValue   the sampling value (either a fixed number or percentage, depending on the next parameter)
	 * @param isPercentage    whether or not the the sampling value is considered a percentage value  
	 * @return                the sampled state-action sequence
	 */
	public static List<Pair<List<String>,String>> sample( List<Pair<List<String>,String>> stateActionSeq, double samplingValue, boolean isPercentage )
	{
		// Determine how many pairs to sample
		long pairsToSample = Math.round( samplingValue );
		if( isPercentage )
		{
			pairsToSample = Math.round( (stateActionSeq.size() * samplingValue) / 100 );
		}
		if( pairsToSample == 0 )
		{
			pairsToSample = 1;
		}
		
		// Do sampling
		List<Pair<List<String>,String>> stateActionSeqCopy = new ArrayList<Pair<List<String>,String>>( stateActionSeq );
		List<Pair<List<String>,String>> sampledStateActionSeq = new ArrayList<Pair<List<String>,String>>();
		while( (sampledStateActionSeq.size() < pairsToSample) && (stateActionSeqCopy.size() > 0) )
		{
			int randomIndex = random.nextInt( stateActionSeqCopy.size() );
			sampledStateActionSeq.add( stateActionSeqCopy.get( randomIndex ) );
			stateActionSeqCopy.remove( randomIndex );
		}
		
		return sampledStateActionSeq;
	}
	
	
	/**
	 * Replaces the rule weights of the provided knowledge base that stem from a converted deterministic 
	 * state-action sequence by the ones from the original non-deterministic state-action sequence
	 * (the weights are cached in case the method is called once after each iteration).  
	 * 
	 * @param kb                           the knowledge base whose rule weights will be replaced
	 * @param stateActionSeq               the converted deterministic state-action sequence
	 * @param absFreqOfActionsGivenAState  the absolute frequencies of actions given a state from the original non-deterministic state-action sequence
	 * @param isFinalIteration             whether or not this is the final iteration (i.e., the iteration of the most-specific or another bottommost level after which the remaining state-action pairs are empty)
	 * @return                             a new knowledge base where the rule weights are replaced
	 */
	public static MultiAbstractionLevelKB replaceWeightsNonDeterministic( MultiAbstractionLevelKB kb, List<Pair<List<String>, String>> stateActionSeq, Map<List<String>,Map<String, Integer>> absFreqOfActionsGivenAState, boolean isFinalIteration )
	{
		// Create a copy of the provided original knowledge base 
		MultiAbstractionLevelKB newKb = new MultiAbstractionLevelKB( kb );
		
		// Replace weights with the ones of the original non-deterministic state-action sequence:
		// Iterate over each level up to and including the most specific level (if it exists) 
		for( int i = 0; i <= stateActionSeq.get( 0 ).getFirst().size(); i++ )
		{	
			// Stop iterating if the knowledge base has less levels then the maximum number possible
			if( i >= newKb.getNoOfLevels() )
			{
				break;
			}
			
			// Iterate over every rule
			for( Rule rule : newKb.getLevel( i ) )
			{
				// If the weight was not already replaced in a previous iteration...
				if( !replacedWeightsNonDeterministic.containsKey( rule ) )
				{
					// Calculate the conditional relative frequency P(a|s) from the original
					// non-deterministic data as the new weight of the rule
					int stateCount = 0;
					int actionCount = 0;
					for( List<String> state : absFreqOfActionsGivenAState.keySet() )
					{
						if( (new HashSet<String>( state )).containsAll( rule.getPremise() ) )
						{
							for( String action : absFreqOfActionsGivenAState.get( state ).keySet() )
							{
								stateCount += absFreqOfActionsGivenAState.get( state ).get( action );
							}
							
							if( absFreqOfActionsGivenAState.get( state ).containsKey( rule.getConclusion() ) )
							{
								actionCount += absFreqOfActionsGivenAState.get( state ).get( rule.getConclusion() );
							}
						}
					}
					double newWeight = ((double) actionCount) / stateCount;
					
					// Do caching
					replacedWeightsNonDeterministic.put( new Rule( new HashSet<String>( rule.getPremise() ), rule.getConclusion(), rule.getWeight() ), newWeight );
					
					// Replace the rule's weight
					rule.setWeight( newWeight );
				}
				
				// ...else (if the weight was not already replaced in a previous iteration), get it from cache!
				else
				{
					rule.setWeight( replacedWeightsNonDeterministic.get( rule ) );
				}
			}
		}
		
		// In case of the final iteration: 
		// Do a revision with all state-action pairs from the deterministic state-action sequence
		// (to ensure the inference safety at least against the deterministic state-action sequence after the weights were changed)
		if( isFinalIteration )
		{
			// Create a copy of the current most specific level:
			// If the bottommost level is the most specific one possible
			// (then take these rules - otherwise most specific level before revision will be empty)
			Set<Rule> mostSpecificLevelBeforeRevision = new HashSet<Rule>();
			if( (!newKb.getLevel( newKb.getNoOfLevels() - 1 ).isEmpty()) && (newKb.getLevel( newKb.getNoOfLevels() - 1 ).iterator().next().getPremise().size() == stateActionSeq.get( 0 ).getFirst().size()) )
			{
				for( Rule rule : newKb.getLevel( newKb.getNoOfLevels() - 1 ) )
				{
					mostSpecificLevelBeforeRevision.add( new Rule( new HashSet<String>( rule.getPremise() ), rule.getConclusion(), rule.getWeight() ) );
				}
			}
			
			// Revise with the original deterministic version of the input state-action sequence (to ensure inference safety)
			KBRevision.revisionMulti( newKb , stateActionSeq );
		
			// If the bottommost level is the most specific one possible
			// (then revision took place or these rules have been there before)
			if( (!newKb.getLevel( newKb.getNoOfLevels() - 1 ).isEmpty()) && (newKb.getLevel( newKb.getNoOfLevels() - 1 ).iterator().next().getPremise().size() == stateActionSeq.get( 0 ).getFirst().size()) )
			{			
				// Replace weights of rules on the bottommost level
				for( Rule rule : newKb.getLevel( newKb.getNoOfLevels() - 1 ) )
				{
					// Only the rules that where added by the revision are of interest here
					if( !mostSpecificLevelBeforeRevision.contains( rule ) )
					{
						// Calculate the conditional relative frequency P(a|s) from the original
						// non-deterministic data as the new weight of the rule
						int stateCount = 0;
						int actionCount = 0;
						for( List<String> state : absFreqOfActionsGivenAState.keySet() )
						{
							if( (new HashSet<String>( state )).containsAll( rule.getPremise() ) )
							{
								for( String action : absFreqOfActionsGivenAState.get( state ).keySet() )
								{
									stateCount += absFreqOfActionsGivenAState.get( state ).get( action );
								}
								
								if( absFreqOfActionsGivenAState.get( state ).containsKey( rule.getConclusion() ) )
								{
									actionCount += absFreqOfActionsGivenAState.get( state ).get( rule.getConclusion() );
								}
							}
						}
						double newWeight = ((double) actionCount) / stateCount;
						
						// Replace the rule's weight
						rule.setWeight( newWeight );
					}
				}
			}
		}
		
		return newKb;
	}
	
	
	/**
	 * Extracts a knowledge base with multiple abstraction levels which compactly represents the knowledge
	 * contained in the given deterministic state-action sequence.
	 * The possible actions are inferred from data. 
	 * 
	 * @param stateActionSeq   the state-action sequence from which the knowledge base is created
	 * @return                 a knowledge base compactly representing the knowledge contained in the given state-action sequence
	 */
	public static MultiAbstractionLevelKB extractKB( List<Pair<List<String>,String>> stateActionSeq )
	{
		return extractKB( stateActionSeq, null );
	}
	
	
	/**
	 * Extracts a knowledge base with multiple abstraction levels which compactly represents the knowledge
	 * contained in the given deterministic state-action sequence. 
	 * 
	 * @param stateActionSeq   the state-action sequence from which the knowledge base is created
	 * @param possibleActions  the action space (i. e., the conclusions of the rules that will be contained in the returned knowledge base); if null, the possible actions will be inferred from data
	 * @return                 a knowledge base compactly representing the knowledge contained in the given state-action sequence
	 */
	public static MultiAbstractionLevelKB extractKB( List<Pair<List<String>,String>> stateActionSeq, HashSet<String> possibleActions )
	{
		// Do discretization, if desired
		if( discretizationMode )
		{
			stateActionSeq = Preprocessing.discretize( stateActionSeq, maxClusters, clusterNames, clusterInfos );
		
			// Write discretized version of the state-action sequence to file
			// (important, e. g. to perform check on state-action sequences with continuous data)
			String stateActionSeqDiscretizedFilename = outputFilename.substring( 0, outputFilename.lastIndexOf( "." ) ) + "_discretized.sa";
			writeToFile( "", stateActionSeqDiscretizedFilename, false );
			for( Pair<List<String>, String> pair : stateActionSeq )
			{
				String stateActionSeqAsString = "";
				for( String s : pair.getFirst() )
				{
					stateActionSeqAsString += s +  " ";
				}
				stateActionSeqAsString += pair.getSecond() + "\n";
				writeToFile( stateActionSeqAsString, stateActionSeqDiscretizedFilename, true );
			}
		}

		// Do preselection, if desired
		if( preselectionMode )
		{
			stateActionSeq = Preprocessing.preselectSensors( stateActionSeq, preselectionSensorsCount );
		}
		
		// Infer possible actions from data, if not provided
		if( possibleActions == null )
		{
			possibleActions = new HashSet<String>();
			for( Pair<List<String>,String> stateActionPair : stateActionSeq )
			{
				possibleActions.add( stateActionPair.getSecond() );
			}
		}
		
		// Store the possible actions (for being used by progress calculation)
		KBExtractor.possibleActions = possibleActions;
		
		
		//
		// Handle non-deterministic state-action sequences
		//
		
		// Check for non-determinism while counting frequencies of actions given a state
		boolean isNonDeterministic = false;
		Map<List<String>,Map<String, Integer>> absFreqOfActionsGivenAState = new HashMap<List<String>,Map<String, Integer>>();
		for( Pair<List<String>, String> stateActionPair : stateActionSeq )
		{
			// Count absolute frequencies for every action in a provided state
			if( !absFreqOfActionsGivenAState.containsKey( stateActionPair.getFirst() ) )
			{
				absFreqOfActionsGivenAState.put( stateActionPair.getFirst(), new HashMap<String, Integer>() );
			}
			if( !absFreqOfActionsGivenAState.get( stateActionPair.getFirst() ).containsKey( stateActionPair.getSecond() ) )
			{
				absFreqOfActionsGivenAState.get( stateActionPair.getFirst() ).put( stateActionPair.getSecond(), 0 );
			}
			absFreqOfActionsGivenAState.get( stateActionPair.getFirst() ).put( stateActionPair.getSecond(), absFreqOfActionsGivenAState.get( stateActionPair.getFirst() ).get( stateActionPair.getSecond() ) + 1 );
			
			// If there are more than one actions in the map for a state, sequence is non-deterministic
			if( !isNonDeterministic && absFreqOfActionsGivenAState.get( stateActionPair.getFirst() ).size() > 1 )
			{
				isNonDeterministic = true;
				
				System.out.println( "(Warning: State-action sequence is non-deterministic and will be transformed to a deterministic one - no inference safety!)\n" );
				System.out.println();
			}
		}
		
		// If sequence is non-deterministic, transform it
		if( isNonDeterministic )
		{
			List<Pair<List<String>,String>> deterministicStateActionSeq = new ArrayList<Pair<List<String>,String>>();
			for( List<String> state : absFreqOfActionsGivenAState.keySet() )
			{
				// Get the highest absolute action frequency for the current state
				int maxFreq = 0;
				for( String action : absFreqOfActionsGivenAState.get( state ).keySet() )
				{
					if( absFreqOfActionsGivenAState.get( state ).get( action ) > maxFreq )
					{
						maxFreq = absFreqOfActionsGivenAState.get( state ).get( action );
					}
				}

				// Get all actions having the highest frequency
				List<String> bestActions = new ArrayList<String>();
				for( String action : absFreqOfActionsGivenAState.get( state ).keySet() )
				{
					if( absFreqOfActionsGivenAState.get( state ).get( action ) == maxFreq )
					{
						bestActions.add( action );
					}
				}
				
				// Choose one randomly
				String bestAction = bestActions.get( random.nextInt( bestActions.size() ) );
				
				// Add best state-action pair to deterministic state-action sequence
				// as often as it was contained in the original sequence (to preserve the relations)
				for( int i = 0; i < absFreqOfActionsGivenAState.get( state ).get( bestAction ); i++ )
				{
					deterministicStateActionSeq.add( new Pair<List<String>, String>( state, bestAction ) );
				}
			}
			
			// From now on work on the deterministic state-action sequence
			stateActionSeq = deterministicStateActionSeq;
		}
		

		// Do sampling, if desired
		if( samplingMode )
		{
			stateActionSeq = sample( stateActionSeq, samplingValue, samplingValueIsPercentage );
		}
		
		
		//
		// Create and add the most general rule
		//
		
		// Calculate overall best action
		double maxP = 0;
		for( String action : possibleActions )
		{
			double p = P( stateActionSeq, action, new HashSet<String>() ); 
			if( p > maxP )
			{
				maxP = p;
			}
		}
		List<String> bestActions = new ArrayList<String>();
		for( String action : possibleActions )
		{
			double p = P( stateActionSeq, action, new HashSet<String>() ); 
			if( p == maxP )
			{
				bestActions.add( action );
			}
		}
		String bestAction = bestActions.get( random.nextInt( bestActions.size() ) );
		
		// Store corresponding weight
		double weight = maxP;
		
		// Create HKB top-level with rule
		MultiAbstractionLevelKB kb = new MultiAbstractionLevelKB();
		kb.addRule( new Rule( new HashSet<String>(), bestAction, weight ) );

		
		// BASIC IDEA HERE IS:
		// - Get the k-subsets of all remaining state-action pairs that are not yet covered by the extracted KB
		// - Compute all used rules
		// - Adapt the KB level(s) (i. e., retain/delete rules)
		// - Compute the new remaining pairs that are not yet covered according to the adapted KB...  (and so on)
		
		
		List<Rule> usedRules = new LinkedList<Rule>();
		List<Pair<List<String>, String>> remainingStateActionPairs = getRemainingStateActionPairs( stateActionSeq, kb, usedRules );
		int currentLevelIndex = 0;

		// Output the KB "on the fly" while extracting it, if desired:
		// Output the first computed level
		MultiAbstractionLevelKB outputKb = kb;					
		if( isNonDeterministic )
		{
			outputKb = replaceWeightsNonDeterministic( kb, stateActionSeq, absFreqOfActionsGivenAState, remainingStateActionPairs.isEmpty() );
		}
		if( !silentMode )
		{
			System.out.println( "(Iteration " + currentLevelIndex + ")" );
			System.out.println();
			System.out.println( outputKb );
		}
		if( outputFilename != null )
		{
			writeToFile( "" + outputKb, outputFilename, false );
		}

		// Output progress, if desired
		if( showProgress )
		{
			System.out.println();
			showProgress( stateActionSeq, remainingStateActionPairs, false );
		}
		
		// Repeat until there are no more state-action pairs left or until the last level of the KB has been considered
		// Note that completeness is only guaranteed if the algorithms terminates according to the first condition
		while( (!remainingStateActionPairs.isEmpty()) && (currentLevelIndex < stateActionSeq.get( 0 ).getFirst().size()) )
		{
			for( Pair<List<String>, String> remainingStateActionPair : remainingStateActionPairs )
			{
				// Iterate over all k-subsets of the current state, where k is equal to the number of the current level (+1)
				List<Set<String>> kSubsets = kSubsets( remainingStateActionPair.getFirst(), currentLevelIndex + 1 );

				for( Set<String> kSubset : kSubsets )
				{
					//
					// Create new rule
					//
					
					// TODO This casting of the set as HashSet is superfluous and should be improved later (e. g. by changing the HashSet in a rule's constructor to Set) 
					HashSet<String> state = (HashSet<String>) kSubset;
					
					String action = remainingStateActionPair.getSecond();
	
					// Calculate weight for rule
					// Note that the weight must be adapted later by dividing it by P( s_1 ^ ... ^ s_j ) 
					double weight2 = P( stateActionSeq, action, kSubset );
	
					Rule rule = new Rule( state, action, weight2 );
					
					// Add new rule to the HKB
					if( (kb.getNoOfLevels() <= currentLevelIndex + 1) || (!kb.getLevel( currentLevelIndex + 1 ).contains( rule )) )
						kb.addRule( rule );
				}
			}
			
			// Remove all rules from the bottom most level that have not been used
			usedRules = new LinkedList<Rule>();
			getRemainingStateActionPairs( stateActionSeq, kb, usedRules );
			if( kb.getNoOfLevels() > currentLevelIndex + 1 )
			{
				kb.getLevel( currentLevelIndex + 1 ).retainAll( usedRules );

				// Adapt rule weights from probabilities P( s_1 ^ ... ^ s_n ^ a ) to P( a | s_1 ^ ... ^ s_n )
				for( Rule rule : kb.getLevel( currentLevelIndex + 1 ) )
				{
					rule.setWeight( conditionalP( stateActionSeq, rule.getConclusion(), rule.getPremise() ) );
				}
			}	
			
			// Calculate all remaining state action pairs that are not correctly covered now with the rules now
			// having the correct conditional probabilities
			// While this, also calculate all used rules, since it may occur, that unused rules are remaining here
			// in the HKB (see the case where (random) rule selection is done in the method getRemainingStateActionPairs())
			usedRules = new LinkedList<Rule>();
			remainingStateActionPairs = getRemainingStateActionPairs( stateActionSeq, kb, usedRules );

			// Depending on rule keeping mode...
			if( ruleKeepingMode == RuleKeepingMode.NONE )  // This is the "normal" case of the original algorithm
			{
				// Remove again all rules from all levels that have not been used (now with the correct
				// conditional probabilities and regarding the rule selection from the previous run - to see why this is
				// necessary have a look at the second part of the previous comment)
				for( int i = 0; i < kb.getNoOfLevels(); i++ )
				{
					kb.getLevel( i ).retainAll( usedRules );
				}
			}
			else if( ruleKeepingMode == RuleKeepingMode.TOP )
			{
				// Remove all rules, except the top most rule
				for( int i = 1; i < kb.getNoOfLevels(); i++ )
				{
					kb.getLevel( i ).retainAll( usedRules );
				}
			}
			else  // (Case rule keeping mode ALL)			
			{
				// Delete only the unused rules on the bottom most level:
				// this leads to less compact representations but the "real" knowledge 
				// implied by the data is shown (but partly not needed given the data)
				if( kb.getNoOfLevels() > currentLevelIndex + 1 )
				{
					kb.getLevel( currentLevelIndex + 1 ).retainAll( usedRules );
				}
			}

			// Output the KB after every iteration "on the fly" while extracting it, if desired:
			outputKb = kb;					
			if( isNonDeterministic )
			{
				outputKb = replaceWeightsNonDeterministic( kb, stateActionSeq, absFreqOfActionsGivenAState, remainingStateActionPairs.isEmpty() );
			}
			if( !silentMode )
			{
				System.out.println();
				System.out.println();
				System.out.println( "(Iteration " + (currentLevelIndex + 1) + ")" );
				System.out.println();
				System.out.println( outputKb );
			}
			if( outputFilename != null )
			{
				writeToFile( "" + outputKb, outputFilename, false );
			}
			
			// Go to next level
			currentLevelIndex++;

			// Output progress, if desired
			if( showProgress )
			{
				System.out.println();
				showProgress( stateActionSeq, remainingStateActionPairs, true );
			}
		}
		
		// Mark important rules, if intended
		boolean isMarkImportantRules = true;
		if( isMarkImportantRules )
		{
			// Create map with the premises of all learned rules in KB
			Map<Set<String>, Integer> correctlyActivatedRulesPremiseCounts = new HashMap<Set<String>, Integer>();
			for( int levelNo = 0; levelNo < kb.getNoOfLevels(); levelNo++ )
			{
				for( Rule rule : kb.getLevel( levelNo ) )
				{
					// Note that rules with same premises and different conclusions should not be
					// included in the knowledge base
					if( !correctlyActivatedRulesPremiseCounts.containsKey( rule.getPremise() ) )
					{
						correctlyActivatedRulesPremiseCounts.put( rule.getPremise(), 0 );
					}
				}
			}
			
			// Count activated rules that provide a correct conclusion according to their premises
			for( Pair<List<String>, String> stateActionPair : stateActionSeq )
			{
				// Do reasoning ang get activated rules
				Set<Rule> activatedRules = new HashSet<Rule>();
				LinkedList<String> reasoningResult = kb.reasoning( new HashSet<String>( stateActionPair.getFirst() ), activatedRules );
				
				// Count only, if the conclusion is correct 
				if( (reasoningResult.size() == 1) && reasoningResult.contains( stateActionPair.getSecond() ) )
				{
					for( Rule rule : activatedRules )
					{
						correctlyActivatedRulesPremiseCounts.put( rule.getPremise(), correctlyActivatedRulesPremiseCounts.get( rule.getPremise() ) + 1 );
					}
				}
			}
			
			// Calculate the average number of activations
			double avgCorrectActivationCount = 0;
			for( Set<String> premise : correctlyActivatedRulesPremiseCounts.keySet() )
			{
				avgCorrectActivationCount += correctlyActivatedRulesPremiseCounts.get( premise );
			}
			avgCorrectActivationCount /= correctlyActivatedRulesPremiseCounts.size();
			
			// Calculate premise counts deviations from the average premise count
			Map<Set<String>, Double> correctlyActivatedRulesPremiseCountDeviations = new HashMap<Set<String>, Double>();
			for( Set<String> premise : correctlyActivatedRulesPremiseCounts.keySet() )
			{
				correctlyActivatedRulesPremiseCountDeviations.put( premise, correctlyActivatedRulesPremiseCounts.get( premise ) - avgCorrectActivationCount );
			}
			
			// Calculate the mean deviation
			double avgDeviation = 0;
			for( Set<String> premise : correctlyActivatedRulesPremiseCountDeviations.keySet() )
			{
				avgDeviation += Math.abs( correctlyActivatedRulesPremiseCountDeviations.get( premise ) );
			}
			avgDeviation /= correctlyActivatedRulesPremiseCountDeviations.size();
			
			// Calculate important rules
			Set<Set<String>> importantCorrectlyActivatedPremises = new HashSet<Set<String>>();
			for( Set<String> premise : correctlyActivatedRulesPremiseCountDeviations.keySet() )
			{
				if( correctlyActivatedRulesPremiseCountDeviations.get( premise ) > avgDeviation )
				{
					importantCorrectlyActivatedPremises.add( premise );
				}
			}
			
			// Output KB
			outputKb = kb;
			if( isNonDeterministic )
			{
				outputKb = replaceWeightsNonDeterministic( kb, stateActionSeq, absFreqOfActionsGivenAState, remainingStateActionPairs.isEmpty() );
			}
			String outputKbString = outputKb.toString();
			for( int levelNo = 0; levelNo < outputKb.getNoOfLevels(); levelNo++ )
			{
				for( Rule rule : outputKb.getLevel( levelNo ) )
				{
					if( importantCorrectlyActivatedPremises.contains( rule.getPremise() ) )
					{
						outputKbString = outputKbString.replace( rule.toString() + "\n", rule.toString() + " *\n" );
					}
				}
			}
			if( !silentMode )
			{
				System.out.println();
				System.out.println();
				System.out.println( "(Important rules)" );
				System.out.println();
				System.out.println( outputKbString );
			}
			if( outputFilename != null )
			{
				writeToFile( "" + outputKbString, outputFilename, false );
			}
		}
		
		// Provide a warning if the knowledge base cannot completely explain all state-action pairs
		if( !remainingStateActionPairs.isEmpty() )
		{
			System.out.println( "\n\n(Warning: Not all state-action pairs have been covered.)" );
		}
		
		return( kb );
	}
}
