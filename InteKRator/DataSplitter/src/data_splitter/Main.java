package data_splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Main 
{
	public static String ORIGINAL_DATA_FILE = "NIMR_AllCases_Nigerian_RefRanges.sa";
	public static String INDICES_FILE = "NIMR_Traincases_vec.csv";
	public static String TRAINING_DATA_FILE = "NIMR_AllCases_Nigerian_RefRanges_Training.sa";
	public static String EVALUATION_DATA_FILE = "NIMR_AllCases_Nigerian_RefRanges_Evaluation.sa";
	
	public static void main( String[] args ) 
	{
		// Set the command line parameters
		ORIGINAL_DATA_FILE = args[ 0 ];
		System.out.println( "ORIGINAL_DATA_FILE = " + ORIGINAL_DATA_FILE );
		INDICES_FILE = args[ 1 ];
		System.out.println( "INDICES_FILE = " + INDICES_FILE );
		TRAINING_DATA_FILE = args[ 2 ];
		System.out.println( "TRAINING_DATA_FILE = " + TRAINING_DATA_FILE );
		EVALUATION_DATA_FILE = args[ 3 ];
		System.out.println( "EVALUATION_DATA_FILE = " + EVALUATION_DATA_FILE );
		
		// Read the (remaining) original data into a list
		List<String> originalData = new LinkedList<String>();
		try 
		{
			// Create reader
			BufferedReader reader = new BufferedReader( new FileReader( ORIGINAL_DATA_FILE ) );
		    			
			// Read the first line
			String line = reader.readLine();
		    
		    // Go through every line
		    while( line != null ) 
		    {		    	
		    	originalData.add( line );
		    	
		    	// Read next line
		    	line = reader.readLine();
		    }

		    // Close reader
		    reader.close();
		} 
		catch( FileNotFoundException e ) 
		{
			e.printStackTrace();
		} 
		catch( IOException e ) 
		{
			e.printStackTrace();
		}
		
		// Read in the indices file into a list
		List<String> indices = new LinkedList<String>();
		try 
		{
			// Create reader
			BufferedReader reader = new BufferedReader( new FileReader( INDICES_FILE ) );
		    			
			// Read the first line
			String line = reader.readLine();
		    
		    // Go through every line
		    while( line != null ) 
		    {		    	
		    	indices.add( line );
		    	
		    	// Read next line
		    	line = reader.readLine();
		    }

		    // Close reader
		    reader.close();
		} 
		catch( FileNotFoundException e ) 
		{
			e.printStackTrace();
		} 
		catch( IOException e ) 
		{
			e.printStackTrace();
		}
		
		// Create a list for the training data
		List<String> trainingData = new LinkedList<String>();
		
		// For each index
		for( String index : indices )
		{	
			// Store the corresponding row in the training data list
			trainingData.add( originalData.get( Integer.parseInt( index ) - 1 ) );
		}
		
		// Write training data file
		try 
		{
			// Create reader
			BufferedWriter writer = new BufferedWriter( new FileWriter( TRAINING_DATA_FILE ) );
		    
			for( String line : trainingData )
			{
				// Write KB to file
				writer.append( line + "\n" );
			}
			
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
		
		// Remove training data (note that the order of the remaining data is not necessarily kept)
		// (For a order keeping solution see comment below)
		for( String line: trainingData )
		{
			originalData.remove( line );
		}
		
//		List<String> remainingData = new LinkedList<String>();		
//		for( int i = 0; i < originalData.size(); i++ )
//		{
//          // 1 is assumed to be the index of the first line
//			if( !indices.contains( "" + (i + 1) ) )
//			{
//				remainingData.add( originalData.get( i ) );
//			}
//		}
		// NOTE THAT BELOW IT MUST THEN BE ITERATED OVER remainingData INSTEAD of originalData
		
		// Write remaining original data into evaluation data file
		try 
		{
			// Create reader
			BufferedWriter writer = new BufferedWriter( new FileWriter( EVALUATION_DATA_FILE ) );
		    
			for( String line : originalData )
			{
				// Write KB to file
				writer.append( line + "\n" );
			}
			
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
}
