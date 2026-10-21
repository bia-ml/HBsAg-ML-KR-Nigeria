package intekrator.representation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The class implements a multi-abstraction-level knowledge base with sets as levels. 
 * 
 * @author Daan Apeldoorn
 */
public class MultiAbstractionLevelKB 
{
	/** The name of the input file from which the knowledge base is read; if no filename is provided, the knowledge base was not read from a file. */
	private String inputFilename = null;

	/** A mapping of sensor numbers to maps that map numeric sensor data to the corresponding sensor symbol names (from clustering). */
	private Map<Integer, Map<Double, String>> numbersToNames;
	
	/**	The list of rule sets where rule sets with lower indices contain more general rules. */
	LinkedList<Set<Rule>> ruleSets;
	

	/**
	 * Creates the knowledge base.
	 */
	public MultiAbstractionLevelKB()
	{
		numbersToNames = null;
		ruleSets = new LinkedList<Set<Rule>>();
	}
	

	/**
	 * Creates a copy of the given knowledge base.
	 * 
	 * @param kb  the knowledge base to be copied
	 */
	public MultiAbstractionLevelKB( MultiAbstractionLevelKB kb )
	{
		this( kb, false );
	}


	/**
	 * Creates a copy of the given knowledge base. Drops all weight information (by setting every weight to 1.0) if dropWeights is true.
	 * 
	 * @param kb           the knowledge base to be copied
	 * @param dropWeights  if true, all weight information is dropped (by setting every weight to 1.0)
	 */
	public MultiAbstractionLevelKB( MultiAbstractionLevelKB kb, boolean dropWeights )
	{
		this();
		
		for( int i = 0; i < kb.getNoOfLevels(); i++ )
		{
			for( Rule rule : kb.getLevel( i ) )
			{
				Rule newRule = new Rule( new HashSet<String>( rule.getPremise() ), rule.getConclusion(), (dropWeights ? 1.0 : rule.getWeight()) );
				addRule( newRule );
			}
		}
	}
	
	
	/**
	 * Creates the knowledge base by reading the file determined by the given path and filename.
	 * 
	 * @param filename  the path and filename of the file to be read
	 */
	public MultiAbstractionLevelKB( String filename )
	{
		// Do basic initialization
		this();
		
		try 
		{
			// Create reader
			BufferedReader reader = new BufferedReader( new FileReader( filename ) );
		    			
			// Read the first line
			String line = reader.readLine();
		    
		    // Go through every line
		    while( line != null ) 
		    {		    	
		    	// Skip empty lines and comments (comment is "%")
		    	if( !line.equals( "" ) && !line.startsWith( "%" ) )
		    	{
			    	// Split up rules (separated by ",")
			    	String[] rules = line.split( "," );
			    	for( String rule : rules )
			    	{
			    		// Split premise and conclusion (separated by arrow "->")
			    		String[] premiseConclusion = rule.split( "->" );
			    		
			    		// If there is no premise, just take the conclusion...
			    		HashSet<String> premise = new HashSet<String>();
			    		String[] values = null;
			    		String conclusionWithOrWithoutWeight = null;
			    		if( premiseConclusion.length == 1 )
			    			conclusionWithOrWithoutWeight = premiseConclusion[ 0 ].trim();
			    		
			    		// ...else split conjunction of the premise in the (sensor) values/partial states
			    		// and get the conclusion
			    		else
			    		{
			    			values = premiseConclusion[ 0 ].split( "\\^" );
				    		for( String value : values )
				    			premise.add( value.trim() );
			    			conclusionWithOrWithoutWeight = premiseConclusion[ 1 ].trim();
			    		}			    		

			    		// If a weight was annotated to the rule, extract it
			    		String[] conclusionWeight = conclusionWithOrWithoutWeight.split( "\\[" ); 
			    		String conclusion = conclusionWeight[ 0 ].trim(); 
			    		String weight = "1.0"; 
			    		if( conclusionWeight.length > 1 )
			    			weight = conclusionWeight[ 1 ].split( "\\]" )[ 0 ].trim();
			    		
			    		// Create and add the complete rule
			    		Rule newRule = new Rule( premise, conclusion, Double.parseDouble( weight ) );
			    		this.addRule( newRule );
			    	}
		    	}
		    	
		    	// Read next line
		    	line = reader.readLine();
		    }

		    // Close reader
		    reader.close();
		    
		    // Store input filename
		    inputFilename = filename;
		} 
		catch( FileNotFoundException e ) 
		{
			e.printStackTrace();
		} 
		catch( IOException e ) 
		{
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Writes the knowledge base to the given path and filename.
	 * 
	 * @param filename  the path and filename of the file to be written to
	 */
	public void writeToFile( String filename )
	{
		try 
		{
			// Create reader
			BufferedWriter writer = new BufferedWriter( new FileWriter( filename ) );
		    
			// Write KB to file
			writer.append( toString() );
			
		    // Close writer
			writer.close();
		} 
		catch( FileNotFoundException e ) 
		{
			e.printStackTrace();
		} 
		catch( IOException e ) 
		{
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Adds a rule to the knowledge base.
	 * The rule is automatically added to the right level of abstraction (depending on the rule's premise)
	 * and a the missing abstractions level are created if necessary.
	 * 
	 * @param rule  the rule to be added to the knowledge base
	 */
	public void addRule( Rule rule )
	{
		while( ruleSets.size() < rule.getPremise().size() + 1 )
			ruleSets.add( new HashSet<Rule>() );
		ruleSets.get( rule.getPremise().size() ).add( rule );
	}
		
	
	/**
	 * Returns the number of abstraction levels in the knowledge base.
	 * 
	 * @return  the number of abstraction levels
	 */
	public int getNoOfLevels()
	{
		return ruleSets.size();
	}
	
	
	/**
	 * Returns the rule set of the i-th abstraction level of the knowledge base.
	 * 
	 * @param i  the index of the abstraction level to be returned
	 * @return   the abstraction level
	 */
	public Set<Rule> getLevel( int i )
	{
		return ruleSets.get( i );
	}

	
	/**
	 * Performs the reasoning algorithm by returning a list of conclusions that fit best to the given
	 * knowledge. 
	 * Usually one conclusion is returned but in case more than one firing rules have the same weight
	 * all of their conclusions are returned.  
	 * 
	 * @param knowledge  the knowledge to be put into the knowledge base for reasoning
	 * @return           the reasoned conclusions
	 * @see              #reasoning(Set, Set)
	 */
	public LinkedList<String> reasoning( Set<String> knowledge )
	{
		return reasoning( knowledge, null );
	}
	

	/**
	 * Performs the reasoning algorithm by returning a list of conclusions that fit best to the given
	 * knowledge. 
	 * Usually one conclusion is returned but in case more than one firing rules have the same weight
	 * all of their conclusions are returned.  
	 * If a rule set is given as second parameter, the firing rules are added to this set. This may
	 * be used to trace which rules where responsible for providing the given conclusion(s). 
	 * 
	 * @param knowledge    the knowledge to be put into the knowledge base for reasoning
	 * @param firingRules  the rule set to which the firing rules leading to the conclusion(s) will be added
	 * @return             the reasoned conclusions (may be empty in case no conclusions could be returned; e.g. if the knowledge base is empty or has no rule on the top level)
	 */
	public LinkedList<String> reasoning( Set<String> knowledge, Set<Rule> firingRules )
	{
		// Replace numeric knowledge by the symbol names of the corresponding clusters
		knowledge = mapNumbersToNames( knowledge );
		
		LinkedList<Rule> potentialFiringRules = new LinkedList<Rule>(); 
		int i = getNoOfLevels() - 1;
		while( potentialFiringRules.isEmpty() && (i >= 0) )
		{
			for( Rule rule : getLevel( i ) )
				if( knowledge.containsAll( rule.getPremise() ) )
					potentialFiringRules.add( rule );
			i--;
		}
		
		// Keep only the firing rules with the highest weight
		double maxWeight = Double.NEGATIVE_INFINITY;
		for( Rule rule : potentialFiringRules )
			if( maxWeight < rule.getWeight() )
				maxWeight = rule.getWeight();
		LinkedList<String> conclusions = new LinkedList<String>(); 
		for( int j = 0; j < potentialFiringRules.size(); j++ )
			if( potentialFiringRules.get( j ).getWeight() == maxWeight )
			{
				conclusions.add( potentialFiringRules.get( j ).getConclusion() );
				
				// Store the premises of the firing rules
				if( firingRules != null )
					firingRules.add( potentialFiringRules.get( j ) );
			}
		
		return conclusions;
	}
	
	
	/**
	 * Returns a transformed knowledge where all numbers are replaced by the symbol names of the
	 * corresponding clusters according to the previously stored mapping.
	 * The output set might be of larger size than the input set in case numbers are provided that
	 * have an equal distance to multiple of the numbers that are associated with the symbol names
	 * in the mapping file (see comment below).
	 * 
	 * @param knowledge  the knowledge to be transformed
	 * @return           the new knowledge with numbers being replaced by the symbol names of the corresponding clusters
	 */
	private Set<String> mapNumbersToNames( Set<String> knowledge )
	{
		// Replace numeric knowledge by corresponding symbol names
		Set<String> nonNumericKnowledge = new HashSet<String>();
		for( String s : knowledge )
		{
			// Check if numeric knowledge
			// TODO Allow further number formats (see also method "check" in "InteKRator")
			if( s.matches( "S[0-9]+:-?([0-9]*\\.)?[0-9]+" ) )
			{
				// Get sensor number and numeric knowledge
				Double number = null;
				Integer sensorNo = null;
				try
				{
					String[] sSplitted = s.split( ":" );
					sensorNo = Integer.parseInt( sSplitted[ 0 ].substring( 1 ) );
					number = Double.parseDouble( sSplitted[ 1 ] );
				}
				catch( NumberFormatException e )
				{		
					// No problem can occur here: it's ensured by the regular expression above
				}
	
				// If mapping not loaded, try loading it
				if( numbersToNames == null )
				{
					String mappingFilename = null;
					ObjectInputStream numbersToNamesFile;
					try 
					{
						mappingFilename = inputFilename.substring( 0, inputFilename.lastIndexOf( "." ) ) + ".map";
						numbersToNamesFile = new ObjectInputStream( new FileInputStream( mappingFilename ) );
						numbersToNames = (Map<Integer, Map<Double, String>>) numbersToNamesFile.readObject();
						numbersToNamesFile.close();
					} 
					catch( ClassNotFoundException e ) 
					{
						System.out.println( "Error reading map from mapping file: " + mappingFilename );
					}
					catch( IOException e ) 
					{
						System.out.println( "Error reading mapping file: " + mappingFilename );
					}
				}
					
				// If there is a mapping available
				if( numbersToNames != null )
				{
					// If the mapping contains the current sensor
					if( numbersToNames.containsKey( sensorNo ) )
					{
						// Check if the numeric knowledge is exactly contained (this can be a common case, e. g., if a large data set is checked)
						if( numbersToNames.get( sensorNo ).containsKey( number ) )
						{
							nonNumericKnowledge.add( numbersToNames.get( sensorNo ).get( number ) );
						}
						else
						{
							//
							// Search for the most similar numbers in the key set and add them all!
							// THEY ARE ALL ADDED FOR THE FOLLOWING REASON: IN THE RARE CASE THAT
							// THERE ARE MORE THAN ONE MOST SIMILAR NUMBERS IN THE KEY SET, ALL RULES
							// OF THE KB SHOULD POTENTIALLY FIRE WHOSE PREMISE IS SATISFIED BY ONE OF 
							// THESE VALUES. THIS ENSURES THAT THERE IS NO NON-DETERMINISM IN THE 
							// REASONING AND THAT THE CONTROL WHICH OF THE CONCLUSIONS ARE IN FACT
							// TAKEN IS UP TO THE RULES' WEIGHTS AS IT IS THE CASE FOR NON-NUMERIC
							// KNOWLEDGE AS WELL.
							//
							
							// Search for the minimal distance to a key number
							double minDistanceToKeyNumber = Double.POSITIVE_INFINITY;
							for( Double keyNumber : numbersToNames.get( sensorNo ).keySet() )
							{
								if( Math.abs( keyNumber - number ) < minDistanceToKeyNumber )
								{
									minDistanceToKeyNumber = Math.abs( keyNumber - number );
								}
							}
							
							// Add all key numbers that have the minimal distance to the number
							for( Double keyNumber : numbersToNames.get( sensorNo ).keySet() )
							{
								if( Math.abs( keyNumber - number ) == minDistanceToKeyNumber )
								{
									nonNumericKnowledge.add( numbersToNames.get( sensorNo ).get( keyNumber ) );
								}
							}
						}
					}
					else
					{
						System.out.println( "Warning: Reasoning from numeric knowledge with missing sensor in mapping: S" + sensorNo );
					}
				}
				else
				{
					System.out.println( "Warning: Reasoning from numeric knowledge with missing mapping" );
				}
			}
			else
			{
				nonNumericKnowledge.add( s );
			}
		}

		return( nonNumericKnowledge );
	}
	
		
	/**
	 * Compares the knowledge base to the given knowledge base on a semantic level (i.e., the degree of equal conclusions given all combinations of the given sensor values).
	 * The measure is symmetrical. 
	 * 
	 * @param kb      the knowledge base to which this knowledge base will be compared
	 * @param states  the states based on which the semantical comparison is done
	 * @return        the similarity of the to knowledge bases between 0 and 1
	 */
	public double compareSemantical( MultiAbstractionLevelKB kb, Set<Set<String>> states )
	{
		int equalConcusionsCount = 0;
		for( Set<String> state : states )
		{
			if( this.reasoning( state ).equals( kb.reasoning( state ) ) )
				equalConcusionsCount++;
		}
		
		return ((double) equalConcusionsCount) / ((double) states.size());
	}
	
			
	/**
	 * Compares the knowledge base to the given knowledge base by counting how many rules are different. 
	 * The measure is asymmetrical. 
	 * 
	 * @param kb  the knowledge base to which this knowledge base will be compared
	 * @return    the ratio how many rules of this knowledge base are contained in the given knowledge base
	 */
	private double compareSyntacticalAsymmetric( MultiAbstractionLevelKB kb )
	{
		// Remove weight information
		MultiAbstractionLevelKB kb1 = new MultiAbstractionLevelKB( this, true );
		MultiAbstractionLevelKB kb2 = new MultiAbstractionLevelKB( kb, true );
		
		int rulesCount = 0;
		int equalRulesCount = 0;
		for( int i = 0; i < kb1.getNoOfLevels(); i++ )
		{
			for( Rule rule : kb1.ruleSets.get( i ) )
			{
				rulesCount++;
				if( i < kb2.getNoOfLevels() )
				{
					for( Rule rule2 : kb2.getLevel( i ) )
					{
						if( rule.equals( rule2 ) )
						{
							equalRulesCount++;
							break;
						}
					}
				}
			}
		}
		
		return ((double) equalRulesCount) / ((double) rulesCount);
	}
	

	/**
	 * Makes a syntactical comparison to the knowledge base based on different rules on the different levels of abstraction. 
	 * The measure is symmetrical. 
	 * 
	 * @param kb  the knowledge base to which this knowledge base will be compared
	 * @return    the similarity of the to knowledge bases between 0 and 1
	 */
	public double compareSyntactical( MultiAbstractionLevelKB kb )
	{
		return (compareSyntacticalAsymmetric( kb ) + kb.compareSyntacticalAsymmetric( this )) / 2;
	}	
	
	
	@Override
	public String toString() 
	{
		// Determine the last non-empty level of the knowledge base
		int lastNonEmptyLevelIndex = -1;
		for( int i = ruleSets.size() - 1; i >= 0; i-- )
		{
			if( !ruleSets.get( i ).isEmpty() )
			{
				lastNonEmptyLevelIndex = i;
				break;
			}
		}

		// Create a sublist of the list of levels of the knowledge base that does not include the trailing empty levels
		List<Set<Rule>> ruleSetsUntilEmpty = ruleSets.subList( 0, lastNonEmptyLevelIndex + 1 );

		String result = new String();
		for( Set<Rule> set : ruleSetsUntilEmpty )
		{	
			int serializedRulesCounter = 0;
			for( Rule rule : set )
			{
				result += rule.toString();
				
				// Count rules already serialized
				serializedRulesCounter++;

				// If it's not the last rule of the current level 
				// or if it's not on the last non-empty level, add newline at the end
				if( (serializedRulesCounter < set.size()) || (set != ruleSetsUntilEmpty.get( ruleSetsUntilEmpty.size() - 1 )) )
				{
					result += "\n";
				}
			}
			
			// Separate rule levels with an empty line:
			// Add newline, if this is not the last non-empty level and if it is not an empty intermediate level  
			if( (set != ruleSetsUntilEmpty.get( ruleSetsUntilEmpty.size() - 1 )) && (!set.isEmpty()) )
			{
				result += "\n";
			}
		}
			
		return result;
	}
}
