package activation_counts_annotator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import intekrator.representation.MultiAbstractionLevelKB;
import intekrator.representation.Pair;
import intekrator.representation.Rule;


public class ActivationCountsAnnotator 
{
	/**
	 * Gets paths and filenames to a knowledge base and a data file as strings and
	 * writes a knowledge base file in which every rule is annotated with the
	 * activation counts according to the data file.
	 * 
	 * @param args
	 */
	public static void main( String[] args ) 
	{
		// Collect parameters
		String knowledgeBaseFilename = args[ 0 ];
		String dataFilename = args[ 1 ];
		
		// Read the knowledge base
		MultiAbstractionLevelKB kb = new MultiAbstractionLevelKB( knowledgeBaseFilename );
		
		// INFO
		int rulesCount = 0;
		for( int i = 0; i < kb.getNoOfLevels(); i++ )
		{
			rulesCount += kb.getLevel( i ).size();
		}
		System.out.println( "Number of rules: " + rulesCount );

		// Read the data file into a list
		// Create state-action sequence
		List<Pair<List<String>, String>> stateActionPairs = new ArrayList<Pair<List<String>, String>>();
		
		// Open and read file line by line into the state-action sequence
		BufferedReader file = null;
		try 
		{
			file = new BufferedReader( new FileReader( dataFilename ) );
			
			// Read first line
			String line = file.readLine();

			// Read all other lines
			while( line != null )
			{
				// Skip blank and comment lines
				if( !line.replace( " ", ""  ).replace( "\t", "" ).isEmpty() && !line.startsWith( "%" ) )
				{
					// Extract sensor values and action from line
					List<String> state = new ArrayList<String>();
					String[] stateActionPairRaw = line.split( " " );
					for( int i = 0; i < stateActionPairRaw.length - 1; i++ )
					{
						state.add( stateActionPairRaw[ i ] );
					}
					String action = stateActionPairRaw[ stateActionPairRaw.length - 1 ];
					
					// Create and add state-action pair
					Pair<List<String>, String> stateActionPair = new Pair<List<String>, String>( state, action );
					stateActionPairs.add( stateActionPair );
				}

				// Read next line
				line = file.readLine();
			}
		} 
		catch( FileNotFoundException e ) 
		{
			System.out.println( "File not found: " + dataFilename );
		}
		catch( IOException e ) 
		{
			System.out.println( "Error reading line of file: " + dataFilename );
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
				System.out.println( "Error closing file: " + dataFilename );
			}
		}

		// INFO
		System.out.println( "Size of data set: " + stateActionPairs.size() );
		
		// Iterate over the list and count rule activations according to the rules/premises
		int multiActivationsCount = 0;
		Map<String, Integer> activationCounts = new HashMap<String, Integer>();
		for( Pair<List<String>, String> stateActionPair: stateActionPairs )
		{
			// Get activated rules
			Set<Rule> activateRules = new HashSet<Rule>();
			kb.reasoning( new HashSet<String>( stateActionPair.getFirst() ), activateRules );
			
			// Count multi-activations
			if( activateRules.size() > 1 )
			{
				multiActivationsCount += (activateRules.size() - 1);
			}
			
			// Iterate over each rule and count
			for( Rule rule : activateRules )
			{
				String ruleAsString = rule.toString();
				if( !activationCounts.containsKey( ruleAsString ) )
				{
					activationCounts.put( ruleAsString, 0 );
				}
				activationCounts.put( ruleAsString, activationCounts.get( ruleAsString ) + 1 );
			}
		}

		// INFO
		System.out.println( "Multi activations count: " + multiActivationsCount );		
		
		// Write the knowledge base with activation counts annotated
		String outputFilename = "annotated_" + knowledgeBaseFilename;
		int totalActivationsCount = 0;
		writeToFile( "", outputFilename, false );
		for( int i = 0; i < kb.getNoOfLevels(); i++ )
		{
			for( Rule rule : kb.getLevel( i ) )
			{
				String ruleAsString = rule.toString();
				totalActivationsCount += activationCounts.get( ruleAsString );
				writeToFile( ruleAsString + " " + activationCounts.get( ruleAsString ) + "\n", outputFilename, true );
			}
			writeToFile( "\n", outputFilename, true );
		}
		
		// INFO
		System.out.println( "Total activations count: " + totalActivationsCount );
	}
	
	
	/**
	 * Writes a text to the file with the provided name in the provided mode.
	 * 
	 * @param rules     the rules to be written to the file
	 * @param filename  the name of the file to be written to
	 * @param append    whether or not the written rules should be appended
	 */
	private static void writeToFile( String text, String filename, boolean append )
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
}
