/*
NOTE THAT THE VERSION OF THE INTEKRATOR TOOLBOX INCLUDED HERE IS (SLIGHTLY) MODIFIED!

THE (LATEST) ORIGINAL VERSION CAN BE FOUND AT:
https://gitlab.com/dapel1/intekrator_toolbox
*/


package intekrator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import intekrator.kbextraction.KBExtractor;
import intekrator.kbextraction.KBExtractor.RuleKeepingMode;
import intekrator.kbextraction.Preprocessing;
import intekrator.representation.MultiAbstractionLevelKB;
import intekrator.representation.Pair;
import intekrator.representation.Rule;
import intekrator.revision.KBRevision;

public class InteKRator
{
	static final String VERSION = "0.14.0b";
	
	static final String PARAM_HELP = "-help";
	static final String PARAM_LEARN = "-learn";
	static final String PARAM_LEARN_TOP = "top";
	static final String PARAM_LEARN_ALL = "all";
	static final String PARAM_LEARN_DISCRETIZE = "discretize";
	static final String PARAM_LEARN_PRESELECT = "preselect";
	static final String PARAM_LEARN_SAMPLE = "sample";
	static final String PARAM_LEARN_AVOID = "avoid";
	static final String PARAM_LEARN_IMPORTANT = "important";
	static final String PARAM_LEARN_IMPORTANT_ONLY = "importantOnly";
	static final String PARAM_LEARN_IMPORTANT_ONLY_TOP = "importantOnlyTop";
	static final String PARAM_INFER = "-infer";
	static final String PARAM_INFER_WHY = "why";
	static final String PARAM_REVISE = "-revise";
	static final String PARAM_INFERMULTI = "-inferMulti";
	static final String PARAM_REVISEMULTI = "-reviseMulti";
	static final String PARAM_PROGRESS = "-progress";
	static final String PARAM_PROGRESS_DETAILS = "details";
	static final String PARAM_CHECK = "-check";
	static final String PARAM_CHECK_DETAILS = "details";
	static final String PARAM_SILENT = "-silent";

	static boolean isShowProgress = false;
	static boolean isShowProgressDetails = false;
	static String progressFile = null;
	static boolean isSilentMode = false;
	
	
	public static void main( String[] args ) 
	{
		// Convert parameters to list for easier handling
		List<String> argsList = new ArrayList<String>();
		for( String arg : args )
		{
			argsList.add( arg );
		}
		
		while( !argsList.isEmpty() )
		{
			// -help
			if( argsList.contains( PARAM_HELP ) )
			{
				showHelp();
				argsList.remove( PARAM_HELP );
			}
			
			else if( argsList.contains( PARAM_SILENT ) )
			{
				isSilentMode = true;
				argsList.remove( PARAM_SILENT );
			}
			
			else if( argsList.contains( PARAM_PROGRESS ) )
			{
				// Create arguments sublist
				List<String> progressArgsList = new ArrayList<String>();
				int startIndex = argsList.indexOf( PARAM_PROGRESS );
				argsList.remove( startIndex );
				for( int i = startIndex; i < argsList.size();  )
				{
					if( argsList.get( i ).startsWith( "-" ) )
					{
						break;
					}
					progressArgsList.add( argsList.get( i ) );
					argsList.remove( i );
				}
	
				progress( progressArgsList );
			}
	
			else if( argsList.contains( PARAM_LEARN ) )
			{
				// Create arguments sublist
				List<String> learnArgsList = new ArrayList<String>();
				int startIndex = argsList.indexOf( PARAM_LEARN );
				argsList.remove( startIndex );
				for( int i = startIndex; i < argsList.size();  )
				{
					if( argsList.get( i ).startsWith( "-" ) )
					{
						break;
					}
					learnArgsList.add( argsList.get( i ) );
					argsList.remove( i );
				}
	
				learn( learnArgsList );
			}
	
			else if( argsList.contains( PARAM_INFER ) )
			{
				// Create arguments sublist
				List<String> inferArgsList = new ArrayList<String>();
				int startIndex = argsList.indexOf( PARAM_INFER );
				argsList.remove( startIndex );
				for( int i = startIndex; i < argsList.size();  )
				{
					if( argsList.get( i ).startsWith( "-" ) )
					{
						break;
					}
					inferArgsList.add( argsList.get( i ) );
					argsList.remove( i );
				}
	
				infer( inferArgsList );
			}
			
			else if( argsList.contains( PARAM_REVISE ) )
			{
				// Create arguments sublist
				List<String> reviseArgsList = new ArrayList<String>();
				int startIndex = argsList.indexOf( PARAM_REVISE );
				argsList.remove( startIndex );
				for( int i = startIndex; i < argsList.size();  )
				{
					if( argsList.get( i ).startsWith( "-" ) )
					{
						break;
					}
					reviseArgsList.add( argsList.get( i ) );
					argsList.remove( i );
				}
	
				revise( reviseArgsList );
			}
			
			else if( argsList.contains( PARAM_INFERMULTI ) )
			{
				// Create arguments sublist
				List<String> inferMultiArgsList = new ArrayList<String>();
				int startIndex = argsList.indexOf( PARAM_INFERMULTI );
				argsList.remove( startIndex );
				for( int i = startIndex; i < argsList.size();  )
				{
					if( argsList.get( i ).startsWith( "-" ) )
					{
						break;
					}
					inferMultiArgsList.add( argsList.get( i ) );
					argsList.remove( i );
				}
	
				inferMulti( inferMultiArgsList );
			}
			
			else if( argsList.contains( PARAM_REVISEMULTI ) )
			{
				// Create arguments sublist
				List<String> reviseMultiArgsList = new ArrayList<String>();
				int startIndex = argsList.indexOf( PARAM_REVISEMULTI );
				argsList.remove( startIndex );
				for( int i = startIndex; i < argsList.size();  )
				{
					if( argsList.get( i ).startsWith( "-" ) )
					{
						break;
					}
					reviseMultiArgsList.add( argsList.get( i ) );
					argsList.remove( i );
				}
	
				reviseMulti( reviseMultiArgsList );
			}

			else if( argsList.contains( PARAM_CHECK ) )
			{
				// Create arguments sublist
				List<String> checkArgsList = new ArrayList<String>();
				int startIndex = argsList.indexOf( PARAM_CHECK );
				argsList.remove( startIndex );
				for( int i = startIndex; i < argsList.size();  )
				{
					if( argsList.get( i ).startsWith( "-" ) )
					{
						break;
					}
					checkArgsList.add( argsList.get( i ) );
					argsList.remove( i );
				}
	
				check( checkArgsList );
			}
			
			// ...else unknown parameter
			else
			{
				System.out.println( "Unknown parameter in " + argsList );
				break;
			}
		}
	}
	
	
	/**
	 * Shows the help text.
	 */
	private static void showHelp()
	{
		System.out.println( "InteKRator v" + VERSION + " - Help" );
		System.out.println();
		System.out.println( "InteKRator is a toolbox that allows to integrate knowledge representation," );
		System.out.println( "machine learning and belief revision approaches in the context of agents." );
		System.out.println( "It can perform efficient reasoning on knowledge bases with different levels" );
		System.out.println( "of abstraction, learn such knowledge bases from data and revise such knowledge" );
		System.out.println( "bases with new evident information." );
		System.out.println();
		System.out.println( "Command: " );
		System.out.println( "java -jar InteKRator.jar PARAMETERS INFILE [OUTFILE] [...]" );
		System.out.println();
		System.out.println( "This command processes the INFILE according to the give PARAMETERS and" );
		System.out.println( "optionally provides the results in the OUTFILE (if provided)." );
		System.out.println( "If no OUTFILE is provided (and -silent is not used), the results are only");
		System.out.println( "written to standard out." );
		System.out.println( "Optionally, multiple PARAMETERS INFILE [OUTFILE] sequences can be used for" );
		System.out.println( "sequential processing.");
		System.out.println();
		System.out.println( "PARAMETERS:" );
		System.out.println( "-learn [OPTIONS]: Learns a knowledge base from data in form of state-action" );
		System.out.println( "                  pairs contained in INFILE. " );
		System.out.println( "                  Each line of the file must be of the form s1 ... sn a" );
		System.out.println( "                  (separated by space characters) where s1 ... sn describes" );
		System.out.println( "                  an agent's state in which the action a was performed." );
		System.out.println();
		System.out.println( "                  OPTIONS:" );
		System.out.println( "                  top: Ensures resulting knowledge base has a top level rule" );
		System.out.println( "                       (even if not needed for inference on the provided" );
		System.out.println( "                       data)." );
		System.out.println( "                  all: Ensures resulting knowledge base includes all rules" );
		System.out.println( "                       learned from data (even those that are not needed" );
		System.out.println( "                       for inference on the provided data)." );
		System.out.println( "                  discretize [C}N ...] [C}NAMES ...] [info C ...]:" );
		System.out.println( "                             Discretizes numeric data columns by clustering." );
		System.out.println( "                             If C}N ... is provided, each column C (where 1" );
		System.out.println( "                             denotes the first colum) will be discretized" );
		System.out.println( "                             to at most N symbols, each representing one" );
		System.out.println( "                             cluster." );
		System.out.println( "                             If C}NAMES ... is provided, NAMES must be a" );
		System.out.println( "                             comma-separated list of names (without space" );
		System.out.println( "                             characters). Each column C will be discretized to" );
		System.out.println( "                             a maximum number of clusters according to the" );
		System.out.println( "                             number of names provided, and the names will be" );
		System.out.println( "                             used for the resulting cluster symbols." );
		System.out.println( "                             If info-option is provided, additional" );
		System.out.println( "                             information about the number of clusters and the" );
		System.out.println( "                             percentage covered by a specific cluster symbol" );
		System.out.println( "                             will be provided for the each column C ... ." );
		System.out.println( "                             In either case, the any-keyword can be used for C" );
		System.out.println( "                             to denote all columns." );
		System.out.println( "                  preselect [N]: Only the most promising sensors are" );
		System.out.println( "                                 considered for learning (increases speed!). " );
		System.out.println( "                                 If N is provided, only the N most promising" );
		System.out.println( "                                 sensors are considered (otherwise N is" );
		System.out.println( "                                 inferred from data)." );
		System.out.println( "                  sample N[%]: Only N state-action pairs are randomly selected" );
		System.out.println( "                               for learning." );
		System.out.println( "                               If % is provided, N percent of the state-action" );
		System.out.println( "                               pairs are randomly selected instead." );
		System.out.println( "                               Sampling can extremely speed up the learning" );
		System.out.println( "                               process, but may result in incomplete " );
		System.out.println( "                               representations (not inference-safe!). " );
		System.out.println( "                               For keeping inference-safety, the resulting" );
		System.out.println( "                               knowledge base can be revised subsequently with" );
		System.out.println( "                               the INFILE (use -reviseMulti), resulting in a" );
		System.out.println( "                               less compact but complete representation." );
		System.out.println( "                  avoid SUFFIX: Avoids learning of rules with premises" );
		System.out.println( "                                containing values with the provided SUFFIX." );
		System.out.println( "                                Can be used, e.g., for excluding missing" );
		System.out.println( "                                values, but may result in incomplete" );
		System.out.println( "                                representations (not inference-safe!)." );
		System.out.println( "                  important [SD]: Highlights the most important rules based" );
		System.out.println( "                                  on standard deviation SD (if provided)." );
		System.out.println( "                                  If SD is not provided, 1 will be assumed." );
		System.out.println( "                  importantOnly [SD]: Same as important, but only the" );
		System.out.println( "                                      important rules will be shown (not" );
		System.out.println( "                                      inference-safe!). " );
		System.out.println( "-infer [why] STATE: Infers one (or more) action(s) for the given STATE from a" );
		System.out.println( "                    knowledge base contained in the INFILE. If more than one" );
		System.out.println( "                    action is inferred this means that the inferred actions" );
		System.out.println( "                    are equally good according to the knowledge contained in" );
		System.out.println( "                    the knowledge base. If why-option is provided, the rules" );
		System.out.println( "                    will be shown based on which the inference was performed." );
		System.out.println( "                    The STATE must be of the form s1 ... sn (separated by" );
		System.out.println( "                    space characters)." );
		System.out.println( "-revise PAIR: Revises the knowledge base contained in the INFILE with the" );
		System.out.println( "              provided state-action PAIR." );
		System.out.println( "              The state-action PAIR must be of the form s1 ... sn a" );
		System.out.println( "              (separated by space characters)." );
		System.out.println( "-inferMulti [why] FILE: Infers one (or more) action(s) for each state in FILE" );
		System.out.println( "                        from a knowledge base contained in the INFILE." );
		System.out.println( "                        Every line of the FILE represents one state of the" );
		System.out.println( "                        form s1 ... sn (separated by space characters)." );
		System.out.println( "                        The results for each state are separated by empty" );
		System.out.println( "                        lines." );
		System.out.println( "                        The why-option works in the same way as for -infer." );
		System.out.println( "-reviseMulti FILE: Revises the knowledge base contained in the INFILE with" );
		System.out.println( "                   the state-action pairs in the provided FILE.");
		System.out.println( "                   Every line of FILE represents one state-action pair of" );
		System.out.println( "                   the form s1 ... sn a (separated by space characters)." );
		System.out.println( "-check [details] FILE: Checks the knowledge base contained in the INFILE" );
		System.out.println( "                       against the state-action sequence contained in FILE" );
		System.out.println( "                       and returns the percentage of covered state-action" );
		System.out.println( "                       pairs for which the correctconclusion is returned." );
		System.out.println( "                       If details-option is provided, the result is" );
		System.out.println( "                       additionally shown individually for each action." );
		System.out.println( "-silent: Results will not be written to standard out." );
		System.out.println( "-progress [details] [FILE]: Writes the progress of the current operation" );
		System.out.println( "                            to FILE (if provided). If FILE is not provided," );
		System.out.println( "                            the progress will be written to standard out" );
		System.out.println( "                            out instead. If details-option is provided, the" );
		System.out.println( "                            progress is additionally shown individually for" );
		System.out.println( "                            each action." );
		System.out.println( "-verbose: Writes additional information to standard out (e.g., time needed)." );
		System.out.println( "-help: Shows this help text." );
		System.out.println();
	}
	
	
	private static void progress( List<String> progressArgs )
	{
		isShowProgress = true;

		while( progressArgs.size() != 0 )
		{	
			if( progressArgs.get( 0 ).equals( PARAM_PROGRESS_DETAILS ) )
			{
				isShowProgressDetails = true;
	
				progressArgs.remove( PARAM_PROGRESS_DETAILS );
			}
			else
			{
				progressFile = progressArgs.get( 0 );
				
				progressArgs.remove( progressFile );

				// If more than two argument provided, show error message
				if( progressArgs.size() != 0 )
				{
					System.out.println( "Too many arguments for " + PARAM_PROGRESS + "?" );
					break;
				}
			}
		}
	}
	
	
	private static void learn( List<String> learnArgs )
	{
		while( learnArgs.size() != 0 )
		{	
			if( learnArgs.get( 0 ).equals( PARAM_LEARN_TOP ) )
			{
				KBExtractor.ruleKeepingMode = RuleKeepingMode.TOP;
				
				learnArgs.remove( PARAM_LEARN_TOP );
			}
			else if( learnArgs.get( 0 ).equals( PARAM_LEARN_ALL ) )
			{
				KBExtractor.ruleKeepingMode = RuleKeepingMode.ALL;
				
				learnArgs.remove( PARAM_LEARN_ALL );
			}
			else if( learnArgs.get( 0 ).equals( PARAM_LEARN_DISCRETIZE ) )
			{
				KBExtractor.discretizationMode = true;
				learnArgs.remove( PARAM_LEARN_DISCRETIZE );
				
				// Process discretize options
				int firstNonDiscretizeOptionIndex = 0;
				for( String option : learnArgs )
				{
					// Break if... 
					// it is not a number (info-option parameter)
					// and not the info-option
					// and not a mapping parameter (containing "}")
					// and not the any-keyword of the info-option
					try
					{
						Integer.parseInt( option );
					}
					catch( NumberFormatException e )
					{
						if( !option.equals( "info" ) && !option.contains( "}" ) && !option.equals( "any" ) )
						{
							break;
						}
					}
					
					firstNonDiscretizeOptionIndex++;
				}
				
				// Get all parameters belonging to discretize options and remove them as being processed
				List<String> discretizeOptions = new ArrayList<String>( learnArgs.subList( 0, firstNonDiscretizeOptionIndex ) );
				learnArgs.removeAll( discretizeOptions );
				
				// Go through all the options
				HashMap<Integer, Integer> maxClusters = new HashMap<Integer, Integer>();
				HashMap<Integer, List<String>> names = new HashMap<Integer, List<String>>();
				Set<Integer> infos = new HashSet<Integer>();
				for( int i = 0; i < discretizeOptions.size(); i++ )
				{
					// Determine whether the current option argument is a mapping to names
					boolean isMappingToNames = false;
					if( discretizeOptions.get( i ).contains( "}" ) )
					{
						try
						{
							Integer.parseInt( discretizeOptions.get( i ).split( "\\}" )[ 1 ] );
						}
						catch( NumberFormatException e )
						{
							isMappingToNames = true;
						}
					}
					
					// If mapping to names starts...
					if( isMappingToNames )
					{
						// Get column number of the names mapping (-1 means "any", i. e., all columns)
						int columnNo = Preprocessing.ALL_COLUMNS;
						if( !discretizeOptions.get( i ).split( "\\}" )[ 0 ].equals( "any" ) )
						{
							columnNo = Integer.parseInt( discretizeOptions.get( i ).split( "\\}" )[ 0 ] );

							// Adapt column number, since they are counted from 1 in the UI
							columnNo--;
						}
						
						// Get all mapping names
						List<String> mappingNames = new ArrayList<String>();
						Collections.addAll( mappingNames, discretizeOptions.get( i ).split( "\\}" )[ 1 ].split( "\\," ) );
						names.put( columnNo, mappingNames );
					}
					
					// ...else if maximum number of clusters starts
					else if( discretizeOptions.get( i ).contains( "}" ) )
					{
						// Get column number of the names mapping (-1 means "any", i. e., all columns)
						int columnNo = Preprocessing.ALL_COLUMNS;
						if( !discretizeOptions.get( i ).split( "\\}" )[ 0 ].equals( "any" ) )
						{
							columnNo = Integer.parseInt( discretizeOptions.get( i ).split( "\\}" )[ 0 ] );

							// Adapt column number, since they are counted from 1 in the UI
							columnNo--;
						}
						
						// Get the maximum number of clusters
						int maxClustersCount = Integer.parseInt( discretizeOptions.get( i ).split( "\\}" )[ 1 ] );
						maxClusters.put( columnNo, maxClustersCount );
					}
					
					// ...else if info option starts
					else if( discretizeOptions.get( i ).equals( "info" ) )
					{
						do
						{
							i++;
							try
							{
								// Get column number of the names mapping (-1 means "any", i. e., all columns)
								if( !discretizeOptions.get( i ).equals( "any" ) )
								{
									int columnNo = Integer.parseInt( discretizeOptions.get( i ) );
									
									// Adapt column number, since they are counted from 1 in the UI
									columnNo--;
									
									infos.add( columnNo );
								}
								else
								{
									// Add -1 for any column
									infos.add( Preprocessing.ALL_COLUMNS );
								}
							}
							catch( NumberFormatException e )
							{
								// Decrease: if conversion did not work, it's one parameter too far
								i--;
								break;
							}
						}
						while( i < discretizeOptions.size() - 1 );
					}
				}

				// Set the options if not being empty
				if( !maxClusters.isEmpty() )
				{
					KBExtractor.maxClusters = maxClusters;
				}
				if( !names.isEmpty() )
				{
					KBExtractor.clusterNames = names;
				}
				if( !infos.isEmpty() )
				{
					KBExtractor.clusterInfos = infos;
				}
			}
			else if( learnArgs.get( 0 ).equals( PARAM_LEARN_PRESELECT ) )
			{
				KBExtractor.preselectionMode = true;
				learnArgs.remove( PARAM_LEARN_PRESELECT );

				// If the optional number of sensors is provided
				if( learnArgs.size() > 0 )
				{
					// Read the number of sensors to be preselected (will be removed below, if it could be properly processed)
					String learnPreselectArg = learnArgs.get( 0 );
					
					// Try to convert the number of sensors to be preselected to integer
					try
					{
						KBExtractor.preselectionSensorsCount = Integer.parseInt( learnPreselectArg );

						// Remove the number of sensors to be preselected, if conversion was successful
						// (otherwise could be the next learn parameter)
						learnArgs.remove( learnPreselectArg );
					}
					catch( NumberFormatException e )
					{
						// No error message here that the parameter should be an integer,
						// since it could be the next parameter as well (due to the number of
						// sensors being optional) and will therefore be delegated to the
						// processing of the next parameter
					}
				}
			}
			else if( learnArgs.get( 0 ).equals( PARAM_LEARN_SAMPLE ) )
			{
				KBExtractor.samplingMode = true;
				learnArgs.remove( PARAM_LEARN_SAMPLE );

				if( learnArgs.size() > 0 )
				{
					// Read and remove the sample value
					String learnSampleArg = learnArgs.get( 0 );
					learnArgs.remove( learnSampleArg );
					
					// Determine if its percentage
					KBExtractor.samplingValueIsPercentage = false;
					if( learnSampleArg.endsWith( "%" ) )
					{
						KBExtractor.samplingValueIsPercentage = true;
						learnSampleArg = learnSampleArg.substring( 0, learnSampleArg.length() - 1 ); 
					}
					
					// Try to convert sample value to double
					try
					{
						KBExtractor.samplingValue = Double.parseDouble( learnSampleArg );
					}
					catch( NumberFormatException e )
					{
						System.out.println( "Argument for " + PARAM_LEARN + " " + PARAM_LEARN_SAMPLE + " must be numeric." );
					}
				}
				else
				{
					System.out.println( "Missing argument for " + PARAM_LEARN + " " + PARAM_LEARN_SAMPLE );
				}
			}
			else if( learnArgs.get( 0 ).equals( PARAM_LEARN_AVOID ) )
			{
				learnArgs.remove( PARAM_LEARN_AVOID );
				
				if( learnArgs.size() > 0 )
				{
					// Read and remove the indicator for values to be avoided for rule premises
					String avoidValueIndicator = learnArgs.get( 0 );
					learnArgs.remove( avoidValueIndicator );
					
					// Pass it to the knowledge base extractor 
					KBExtractor.avoidValueIndicator = avoidValueIndicator;
				}
				else
				{
					System.out.println( "Missing argument for " + PARAM_LEARN + " " + PARAM_LEARN_AVOID );
				}
			}
			else if( learnArgs.get( 0 ).equals( PARAM_LEARN_IMPORTANT ) )
			{
				System.out.println( "Parameter \""+ PARAM_LEARN_IMPORTANT + "\" is not yet implemented!" );
				// TODO Implement "important" parameter here
				
				learnArgs.remove( PARAM_LEARN_IMPORTANT );
			}
			else if( learnArgs.get( 0 ).equals( PARAM_LEARN_IMPORTANT_ONLY ) )
			{
				System.out.println( "Parameter \""+ PARAM_LEARN_IMPORTANT_ONLY + "\" is not yet implemented!" );
				// TODO Implement "importantOnly" parameter here
				
				learnArgs.remove( PARAM_LEARN_IMPORTANT_ONLY );
			}
			
			// ...else must be the file name, read the state-action sequence!
			else
			{
				// Create state-action sequence
				List<Pair<List<String>, String>> stateActionPairs = new ArrayList<Pair<List<String>, String>>();

				// Open and read file line by line into the state-action sequence
				String filename = learnArgs.get( 0 );
				BufferedReader file = null;
				try 
				{
					file = new BufferedReader( new FileReader( filename ) );
					
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
					System.out.println( "File not found: " + filename );
				}
				catch( IOException e ) 
				{
					System.out.println( "Error reading line of file: " + filename );
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
				
				// Reading the file is finished now, remove the parameter from list
				learnArgs.remove( filename );

				// Is OUTFILE provided?
				if( learnArgs.size() == 1 )
				{
					String outputFilename = learnArgs.get( 0 );
					KBExtractor.outputFilename = outputFilename;

					// Output file is set, remove the parameter from list
					learnArgs.remove( outputFilename );
				}
				
				if( learnArgs.size() == 0 )
				{
					// Configure extractor
					KBExtractor.silentMode = isSilentMode;
					
					if( !isSilentMode )
					{
						System.out.println( "Resulting knowledge base:" );
						System.out.println();
						System.out.println();
					}
					
					// Perform extraction
					KBExtractor.showProgress = isShowProgress;
					KBExtractor.showProgressDetails = isShowProgressDetails;
					KBExtractor.progressFilename = progressFile;
					KBExtractor.extractKB( stateActionPairs );

					System.out.println();
					System.out.println();
					System.out.println( "Completed." );
				}
				else
				{
					System.out.println( "Too many arguments for " + PARAM_LEARN + "?" );
					break;
				}
			}
		}
	}

	
	private static void infer( List<String> inferArgs )
	{
		Set<String> state = new HashSet<String>();
		MultiAbstractionLevelKB kb = null;
		List<String> result = null;
		String resultString = "";
		boolean isShowRules = false;
		while( inferArgs.size() != 0 )
		{	
			// If parameter why is provided...
			if( inferArgs.get( 0 ).equals( PARAM_INFER_WHY ) )
			{
				isShowRules = true;
				inferArgs.remove( 0 );
			}
			
			// ...else if it's not the name of an existing file, take it as part of the state
			else if( !(new File( inferArgs.get( 0 ) )).exists() )
			{
				state.add( inferArgs.get( 0 ) );
				inferArgs.remove( 0 );
			}
			
			// ...else (if it is the name of an existing a file name), must be the INFILE
			else
			{
				// Read KB
				kb = new MultiAbstractionLevelKB( inferArgs.get( 0 ) );
				
				// Parameter use, remove it
				inferArgs.remove( 0 );
				
				// Do inference
				Set<Rule> usedRules = new HashSet<Rule>();
				result = kb.reasoning( state, usedRules );

				// Create a mapping from results to the rules providing them
				Map<String, List<Rule>> usedRulesForResult = new HashMap<String, List<Rule>>();
				for( String a : result )
				{
					// Collect all rules providing the result 
					ArrayList<Rule> usedRulesForR = new ArrayList<Rule>();
					for( Rule r : usedRules )
					{
						if( r.getConclusion().equals( a ) )
						{
							usedRulesForR.add( r );
						}
					}
					
					// If the result is not contained yet, add it!
					// (if it is contained already, it has been handled before)
					if( !usedRulesForResult.containsKey( a ) )
					{
						usedRulesForResult.put( a, usedRulesForR );
					}
				}
				
				// Write every inference result to a new line in a string
				int i = 0;
				for( String a : usedRulesForResult.keySet() )
				{
					// Attach corresponding weight to the result
					resultString = resultString + a;
					
					// If rule(s) should be shown, add them!
					if( isShowRules )
					{
						resultString = resultString + "   (based on: " + usedRulesForResult.get( a ).toString().substring( 1, usedRulesForResult.get( a ).toString().length() - 1 ) + ")";
					}
					
					// Attach newline its not the last line
					if( i < usedRulesForResult.keySet().size() - 1 )
					{
						resultString = resultString + "\n";
					}
					i++;
				}
								
				// Is OUTFILE provided?
				if( inferArgs.size() == 1 )
				{
					String outputFilename = inferArgs.get( 0 );

					// Output file is set, remove the parameter from list
					inferArgs.remove( outputFilename );
					
					// Open file
					BufferedWriter file = null;
					try 
					{
						file = new BufferedWriter( new FileWriter( outputFilename, false ) );

						// Write every inference result to a new line of the file
						file.write( resultString );
					} 
					catch( IOException e ) 
					{
						System.out.println( "Error writing to file: " + outputFilename );
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
							System.out.println( "Error closing file: " + outputFilename );
						}
					}
				}
				
				if( inferArgs.size() == 0 )
				{
					System.out.println( "Resulting inference:" );
					System.out.println();
					
					// Perform extraction
					System.out.println( resultString );
					
					System.out.println();
					System.out.println( "Completed." );
				}
				else
				{
					System.out.println( "Too many arguments for " + PARAM_INFER + "?" );
					break;
				}
			}
		}
	}
	
	
	private static void revise( List<String> reviseArgs )
	{
		Set<String> state = new HashSet<String>();
		String action = null;
		MultiAbstractionLevelKB kb = null;
		String resultString = "";
		while( reviseArgs.size() != 0 )
		{	
			// If the next argument is the name of an existing file
			if( (reviseArgs.size() > 1) && (new File( reviseArgs.get( 1 ) )).exists() )
			{
				action = reviseArgs.get( 0 );
				reviseArgs.remove( 0 );
			}

			// ...else if it's not the name of an existing file, take it as part of the state-action pair
			else if( !(new File( reviseArgs.get( 0 ) )).exists() )
			{
				state.add( reviseArgs.get( 0 ) );
				reviseArgs.remove( 0 );
			}
			
			// ...else (if it is the name of an existing a file name), must be the INFILE
			else
			{
				// Read KB
				kb = new MultiAbstractionLevelKB( reviseArgs.get( 0 ) );
				
				// Parameter used, remove it
				reviseArgs.remove( 0 );
				
				// Do revision
				Pair<Set<String>, String> stateActionPair = new Pair<Set<String>, String>( state, action );
				KBRevision.revision( kb, stateActionPair );

				resultString = kb.toString();
				
				// Is OUTFILE provided?
				if( reviseArgs.size() == 1 )
				{
					String outputFilename = reviseArgs.get( 0 );

					// Output file is set, remove the parameter from list
					reviseArgs.remove( outputFilename );
					
					// Open file
					BufferedWriter file = null;
					try 
					{
						file = new BufferedWriter( new FileWriter( outputFilename, false ) );

						// Write revised knowledge base to file
						file.write( resultString );
					} 
					catch( IOException e ) 
					{
						System.out.println( "Error writing to file: " + outputFilename );
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
							System.out.println( "Error closing file: " + outputFilename );
						}
					}
				}
				
				if( reviseArgs.size() == 0 )
				{
					System.out.println( "Resulting knowledge base:" );
					System.out.println();
					
					System.out.println( resultString );

					System.out.println();
					System.out.println( "Completed." );
				}
				else
				{
					System.out.println( "Too many arguments for " + PARAM_REVISE + "?" );
					break;
				}
			}
		}
	}
	

	private static void inferMulti( List<String> inferMultiArgs )
	{
		String resultString = "";
		boolean isShowRules = false;
		while( inferMultiArgs.size() != 0 )
		{	
			// If parameter why is provided...
			if( inferMultiArgs.get( 0 ).equals( PARAM_INFER_WHY ) )
			{
				isShowRules = true;
				inferMultiArgs.remove( 0 );
			}

			// ...else (if it is the name of an existing file name), must be the INFILE
			else
			{
				//
				// First parameter must be the state-action file to revise from
				//
				
				// Create state list
				List<List<String>> states = new ArrayList<List<String>>();
	
				// Open and read file line by line into the states
				String filename = inferMultiArgs.get( 0 );
				BufferedReader statesfile = null;
				try 
				{
					statesfile = new BufferedReader( new FileReader( filename ) );
					
					// Read first line
					String line = statesfile.readLine();
	
					// Read all other lines
					while( line != null )
					{
						// Skip blank and comment lines
						if( !line.replace( " ", ""  ).replace( "\t", "" ).isEmpty() && !line.startsWith( "%" ) )
						{
							// Extract sensor values and action from line
							List<String> state = new ArrayList<String>();
							String[] stateRaw = line.split( " " );
							for( int i = 0; i < stateRaw.length; i++ )
							{
								state.add( stateRaw[ i ] );
							}
							
							// Add state
							states.add( state );
						}
	
						// Read next line
						line = statesfile.readLine();
					}
				} 
				catch( FileNotFoundException e ) 
				{
					System.out.println( "File not found: " + filename );
				}
				catch( IOException e ) 
				{
					System.out.println( "Error reading line of file: " + filename );
				} 
				
				// Close the file
				if( statesfile != null )
				{
					try 
					{
						statesfile.close();
					} 
					catch( IOException e )
					{
						System.out.println( "Error closing file: " + filename );
					}
				}
				
				// Reading the file is finished now, remove the parameter from list
				inferMultiArgs.remove( filename );
			
	
				//
				// Second parameter must be INFILE (the knowledge base)
				//
				
				String kbFilename = null; 
				MultiAbstractionLevelKB kb = null;
				if( inferMultiArgs.size() > 0 )
				{
					kbFilename = inferMultiArgs.get( 0 );
					kb = new MultiAbstractionLevelKB( kbFilename );
				
					// KB file is read, remove the parameter from list
					inferMultiArgs.remove( kbFilename );
				}
				else
				{
					System.out.println( "Too few arguments for " + PARAM_INFERMULTI );
					break;
				}
	
				
				//
				// Now multiple inference can be performed
				//
				
				// Iterate through states and do inference for each state
				int i = 0;
				for( List<String> rawState : states )
				{
					// Add sensor info with number to numeric data
					// (see also method "check")
					int sensorNo = 1;
					Set<String> state = new HashSet<String>();
					for( String s : rawState )
					{
						// If it is numeric data, add sensor prefix
						// TODO Allow further number formats (see also method "mapNumbersToNames" in "MultiAbstractionLevelKB")
						if( s.matches( "-?([0-9]*\\.)?[0-9]+" ) )
						{
							state.add( "S" + sensorNo + ":" + s );
						}
						else
						{
							state.add( s );
						}
						
						sensorNo++;
					}

					// Do inference
					Set<Rule> usedRules = new HashSet<Rule>();
					List<String> result = kb.reasoning( state, usedRules );

					// Create a mapping from results to the rules providing them
					Map<String, List<Rule>> usedRulesForResult = new HashMap<String, List<Rule>>();
					for( String a : result )
					{
						// Collect all rules providing the result 
						ArrayList<Rule> usedRulesForR = new ArrayList<Rule>();
						for( Rule r : usedRules )
						{
							if( r.getConclusion().equals( a ) )
							{
								usedRulesForR.add( r );
							}
						}
						
						// If the result is not contained yet, add it!
						// (if it is contained already, it has been handled before)
						if( !usedRulesForResult.containsKey( a ) )
						{
							usedRulesForResult.put( a, usedRulesForR );
						}
					}
					
					// Write every inference result to a new line in a string
					int j = 0;
					for( String a : usedRulesForResult.keySet() )
					{
						// Attach corresponding weight to the result
						resultString = resultString + a;
						
						// If rule(s) should be shown, add them!
						if( isShowRules )
						{
							resultString = resultString + "   (based on: " + usedRulesForResult.get( a ).toString().substring( 1, usedRulesForResult.get( a ).toString().length() - 1 ) + ")";
						}
						
						// Attach newline its not the last line
						if( j < usedRulesForResult.keySet().size() - 1 )
						{
							resultString = resultString + "\n";
						}
						j++;
					}

					// Empty line between inference results
					if( i < states.size() - 1 )
					{
						resultString += "\n\n";
					}
					i++;
				}
				
				
				// Is OUTFILE provided?
				if( inferMultiArgs.size() == 1 )
				{
					String outputFilename = inferMultiArgs.get( 0 );

					// Output file is set, remove the parameter from list
					inferMultiArgs.remove( outputFilename );
					
					// Open file
					BufferedWriter file = null;
					try 
					{
						file = new BufferedWriter( new FileWriter( outputFilename, false ) );

						// Write every inference result to a new line of the file
						file.write( resultString );
					} 
					catch( IOException e ) 
					{
						System.out.println( "Error writing to file: " + outputFilename );
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
							System.out.println( "Error closing file: " + outputFilename );
						}
					}
				}
				
				if( inferMultiArgs.size() == 0 )
				{
					System.out.println( "Resulting inferences:" );
					System.out.println();
					
					// Perform extraction
					System.out.println( resultString );
					
					System.out.println();
					System.out.println( "Completed." );
				}
				else
				{
					System.out.println( "Too many arguments for " + PARAM_INFERMULTI + "?" );
					break;
				}
			}
		}
	}
	
	
	private static void reviseMulti( List<String> reviseMultiArgs )
	{
		String resultString = "";
		while( reviseMultiArgs.size() != 0 )
		{	
			// This is just to keep the structure along the lines with the other commands
			// (in case further parameters will be added in future)
			if( false )
			{
				// ...
			}

			// ...else (if it is the name of an existing file name), must be the INFILE
			else
			{
				//
				// First parameter must be the state-action file to revise from
				//
				
				// Create state-action sequence
				List<Pair<List<String>, String>> stateActionPairs = new ArrayList<Pair<List<String>, String>>();
	
				// Open and read file line by line into the state-action sequence
				String filename = reviseMultiArgs.get( 0 );
				BufferedReader stateActionfile = null;
				try 
				{
					stateActionfile = new BufferedReader( new FileReader( filename ) );
					
					// Read first line
					String line = stateActionfile.readLine();
	
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
						line = stateActionfile.readLine();
					}
				} 
				catch( FileNotFoundException e ) 
				{
					System.out.println( "File not found: " + filename );
				}
				catch( IOException e ) 
				{
					System.out.println( "Error reading line of file: " + filename );
				} 
				
				// Close the file
				if( stateActionfile != null )
				{
					try 
					{
						stateActionfile.close();
					} 
					catch( IOException e )
					{
						System.out.println( "Error closing file: " + filename );
					}
				}
				
				// Reading the file is finished now, remove the parameter from list
				reviseMultiArgs.remove( filename );
			
	
				//
				// Second parameter must be INFILE (the knowledge base)
				//
				
				String kbFilename = null; 
				MultiAbstractionLevelKB kb = null;
				if( reviseMultiArgs.size() > 0 )
				{
					kbFilename = reviseMultiArgs.get( 0 );
					kb = new MultiAbstractionLevelKB( kbFilename );
				
					// KB file is read, remove the parameter from list
					reviseMultiArgs.remove( kbFilename );
				}
				else
				{
					System.out.println( "Too few arguments for " + PARAM_REVISEMULTI );
					break;
				}
	
				
				//
				// Now revisions can be performed
				//
				
				// Iterate of state-action sequence and do revision for each state-action pair
				for( Pair<List<String>, String> stateActionpair : stateActionPairs )
				{
					// Add sensor info with number to numeric data
					int sensorNo = 1;
					Set<String> state = new HashSet<String>();
					for( String s : stateActionpair.getFirst() )
					{
						// If it is numeric data, add sensor prefix
						// TODO Allow further number formats (see also method "mapNumbersToNames" in "MultiAbstractionLevelKB")
						if( s.matches( "-?([0-9]*\\.)?[0-9]+" ) )
						{
							state.add( "S" + sensorNo + ":" + s );
						}
						else
						{
							state.add( s );
						}
						
						sensorNo++;
					}
					
					String action = stateActionpair.getSecond();
					
					// Transform action according to mapping
					// TODO Call transformation method here

					// Do revision
					Pair<Set<String>, String> stateActionPair = new Pair<Set<String>, String>( state, action );
					KBRevision.revision( kb, stateActionPair );
				}
				
				resultString = kb.toString();
				
				// Is OUTFILE provided?
				if( reviseMultiArgs.size() == 1 )
				{
					String outputFilename = reviseMultiArgs.get( 0 );

					// Output file is set, remove the parameter from list
					reviseMultiArgs.remove( outputFilename );
					
					// Open file
					BufferedWriter file = null;
					try 
					{
						file = new BufferedWriter( new FileWriter( outputFilename, false ) );

						// Write revised knowledge base to file
						file.write( resultString );
					} 
					catch( IOException e ) 
					{
						System.out.println( "Error writing to file: " + outputFilename );
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
							System.out.println( "Error closing file: " + outputFilename );
						}
					}
				}
				
				if( reviseMultiArgs.size() == 0 )
				{
					System.out.println( "Resulting knowledge base:" );
					System.out.println();
					
					System.out.println( resultString );

					System.out.println();
					System.out.println( "Completed." );
				}
				else
				{
					System.out.println( "Too many arguments for " + PARAM_REVISEMULTI + "?" );
					break;
				}
			}
		}
	}

	
	private static void check( List<String> checkArgs )
	{
		String resultString = "";
		boolean isShowCheckDetails = false;
		while( checkArgs.size() != 0 )
		{	
			if( checkArgs.get( 0 ).equals( PARAM_CHECK_DETAILS ) )
			{
				isShowCheckDetails = true;
				
				checkArgs.remove( PARAM_CHECK_DETAILS );
			}
			else
			{
				//
				// First parameter after optional "details" must be the state-action file to check against
				//
				
				// Create state-action sequence
				List<Pair<List<String>, String>> stateActionPairs = new ArrayList<Pair<List<String>, String>>();
	
				// Open and read file line by line into the state-action sequence
				String filename = checkArgs.get( 0 );
				BufferedReader stateActionfile = null;
				try 
				{
					stateActionfile = new BufferedReader( new FileReader( filename ) );
					
					// Read first line
					String line = stateActionfile.readLine();
	
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
						line = stateActionfile.readLine();
					}
				} 
				catch( FileNotFoundException e ) 
				{
					System.out.println( "File not found: " + filename );
				}
				catch( IOException e ) 
				{
					System.out.println( "Error reading line of file: " + filename );
				} 
				
				// Close the file
				if( stateActionfile != null )
				{
					try 
					{
						stateActionfile.close();
					} 
					catch( IOException e )
					{
						System.out.println( "Error closing file: " + filename );
					}
				}
				
				// Reading the file is finished now, remove the parameter from list
				checkArgs.remove( filename );
			
	
				//
				// Second parameter must be INFILE (the knowledge base)
				//
				
				String kbFilename = null; 
				MultiAbstractionLevelKB kb = null;
				if( checkArgs.size() > 0 )
				{
					kbFilename = checkArgs.get( 0 );
					kb = new MultiAbstractionLevelKB( kbFilename );
				
					// KB file is read, remove the parameter from list
					checkArgs.remove( kbFilename );
				}
				else
				{
					System.out.println( "Too few arguments for " + PARAM_CHECK );
					break;
				}
	
				
				//
				// Now check can be performed
				//
				
				// Iterate of state-action sequence and count number of wrong conclusions
				int noOfWrongConclusions = 0;
				HashMap<String, Integer> wrongConclusionsPerAction = new HashMap<String, Integer>();
				HashMap<String, Integer> actionCounts = new HashMap<String, Integer>();
				for( Pair<List<String>, String> stateActionpair : stateActionPairs )
				{
					// Add sensor info with number to numeric data
					// (see also method "inferMulti")
					int sensorNo = 1;
					Set<String> state = new HashSet<String>();
					for( String s : stateActionpair.getFirst() )
					{
						// If it is numeric data, add sensor prefix
						// TODO Allow further number formats (see also method "mapNumbersToNames" in "MultiAbstractionLevelKB")
						if( s.matches( "-?([0-9]*\\.)?[0-9]+" ) )
						{
							state.add( "S" + sensorNo + ":" + s );
						}
						else
						{
							state.add( s );
						}
						
						sensorNo++;
					}
					
					String action = stateActionpair.getSecond();
					Set<String> result = new HashSet<String>( kb.reasoning( state ) );
					if( (result.size() > 1) || !result.contains( action ) )
					{
						noOfWrongConclusions++;
						
						// If details are desired, count wrong conclusions per action
						if( isShowCheckDetails )
						{
							if( !wrongConclusionsPerAction.containsKey( action ) )
							{
								wrongConclusionsPerAction.put( action, 0 );
							}
							wrongConclusionsPerAction.put( action, wrongConclusionsPerAction.get( action ) + 1 );
						}
					}
					
					// If details are desired, count action occurrences
					if( isShowCheckDetails )
					{
						if( !actionCounts.containsKey( action ) )
						{
							actionCounts.put( action, 0 );
						}
						actionCounts.put( action, actionCounts.get( action ) + 1 );
					}
				}
				
				resultString = "" + (1 - (((double) noOfWrongConclusions ) / stateActionPairs.size()));
				
				// If wrong conclusions per action are desired as details, complete the result string
				if( isShowCheckDetails )
				{
					resultString += "   (";
					int actionCounter = 0;
					for( String action : actionCounts.keySet() )
					{
						double checkDetail = 1.0;
						if( wrongConclusionsPerAction.containsKey( action ) )
						{
							checkDetail = 1.0 - (((double) wrongConclusionsPerAction.get( action )) / actionCounts.get( action ));
						}
						
						// Update the check string
						resultString += action + ": " + checkDetail;
						actionCounter++;
						if( actionCounter < actionCounts.size() )
						{
							resultString += ", ";
						}
					}
					resultString += ")";
				}
				
				
				// Is OUTFILE provided?
				if( checkArgs.size() == 1 )
				{
					String outFilename = checkArgs.get( 0 );
	
					// Output file is set, remove the parameter from list
					checkArgs.remove( outFilename );
					
					// Open file
					BufferedWriter outFile = null;
					try 
					{
						outFile = new BufferedWriter( new FileWriter( outFilename, false ) );
	
						// Write the result to the file
						outFile.write( resultString );
					} 
					catch( IOException e ) 
					{
						System.out.println( "Error writing to file: " + outFilename );
					} 
					
					// Close the file
					if( outFile != null )
					{
						try 
						{
							outFile.close();
						} 
						catch( IOException e )
						{
							System.out.println( "Error closing file: " + outFilename );
						}
					}
				}
	
				// Write results if not too many arguments provided
				if( checkArgs.size() == 0 )
				{
					System.out.println( "Resulting certainty:" );
					System.out.println();
					
					System.out.println( resultString );
	
					System.out.println();
					System.out.println( "Completed." );
				}
				else
				{
					System.out.println( "Too many arguments for " + PARAM_CHECK + "?" );
					break;
				}
			}
		}
	}
}
