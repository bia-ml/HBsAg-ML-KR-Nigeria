package intekrator.revision;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import intekrator.representation.MultiAbstractionLevelKB;
import intekrator.representation.Pair;
import intekrator.representation.Rule;


/**
 * This class allows for performing a simple revision mechanism on a provided knowledge base.  
 * 
 * @author DaanApeldoorn
 */
public class KBRevision 
{
	/**
	 * Revises the provided knowledge base with the provided state-action pair, such that performing
	 * reasoning on the knowledge base with the state-action pair's state results in the state-action
	 * pair's action. (The state-action pair must be complete!)
	 * 
	 * @param kb               the knowledge base to be revised
	 * @param stateActionPair  a (complete) state-action pair reflecting the new information that is considered to be evident 
	 */
	public static void revision( MultiAbstractionLevelKB kb, Pair<Set<String>, String> stateActionPair )
	{
		// Perform inference
		Set<Rule> firingRules = new HashSet<Rule>();
		List<String> inferenceResult = kb.reasoning( stateActionPair.getFirst(), firingRules );
		
		// Determine if conclusion fits exactly to the provided state action pair
		// (Conclusion only fits exactly the provided state action pair if only the action of the provided 
		// state-action pair is contained in the inference result)
		boolean correctConclusion = true;
		for( String result : inferenceResult  )
		{
			if( !result.equals( stateActionPair.getSecond() ) )
			{
				correctConclusion = false;
				break;
			}
		}
		
		// If conclusion does not fit, do revision
		if( !correctConclusion )
		{
			// Determine the most specific level of the knowledge base that is not empty
			// (which must not necessarily be the bottommost level)
			int mostSpecificLevelIndex = 0;
			for( int i = (kb.getNoOfLevels() - 1); i > 0; i-- )
			{
				if( !kb.getLevel( i ).isEmpty() )
				{
					mostSpecificLevelIndex = i;
					break;
				}
			}
			
			// NOTE THAT IT CANNOT BE CHECKED HERE IF THIS IS THE REALLY THE BOTTOMMOST LEVEL OF THE KB
			// ACCORDING TO THE ORIGINAL NUMBER OF AGENT SENSORS
			if( stateActionPair.getFirst().size() < mostSpecificLevelIndex )
			{
				System.out.println( "(Warning: Revision not done on the most specific level can affect inference safety.)" );
				System.out.println();
			}
			
			// Determine whether the responsible rule is on the most specific level
			boolean isResponsibleRuleOnMostSpecificLevel = false;
			for( Rule firingRule : firingRules )
			{
				if( firingRule.getPremise().size() == stateActionPair.getFirst().size()  )
				{
					isResponsibleRuleOnMostSpecificLevel = true;
				}
				
				// It's sufficient to check only one of the firing rules, since multiple rules
				// can only fire on the same level
				break;
			}
			
			// If not on most specific level, simply add new exception
			if( !isResponsibleRuleOnMostSpecificLevel )
			{
				Rule newRule = new Rule( new HashSet<String>( stateActionPair.getFirst() ), new String( stateActionPair.getSecond() ), 1.0 );
				kb.addRule( newRule );
			}
			
			// ...else (must be on the most specific level), remove or exchange exception
			else
			{
				// Remove or Exchange every firing rule that is responsible for the wrong conclusion
				for( Rule firingRule : firingRules )
				{
					// If the firing rule is the one providing the wrong conclusion
					if( !firingRule.getConclusion().equals( stateActionPair.getSecond() ) )
					{
						// Remove it
						kb.getLevel( firingRule.getPremise().size() ).remove( firingRule );
						
						// Perform inference
						inferenceResult = kb.reasoning( stateActionPair.getFirst() );
						
						// Determine if conclusion fits exactly to the provided state action pair
						// (Conclusion only fits exactly the provided state action pair if only the action of the provided 
						// state-action pair is contained in the inference result)
						correctConclusion = true;
						for( String result : inferenceResult  )
						{
							if( !result.equals( stateActionPair.getSecond() ) )
							{
								correctConclusion = false;
								break;
							}
						}
						
						// If the correct conclusion still cannot be provided, add new correct rule
						if( !correctConclusion )
						{
							// Add new correct rule
							Rule newRule = new Rule( new HashSet<String>( stateActionPair.getFirst() ), new String( stateActionPair.getSecond() ), 1.0 );
							kb.addRule( newRule );
						}
						
						// ...else (if conclusion is correct, no need for further iterating the original firing rules)
						else
						{
							break;
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Revises the provided knowledge base with the provided list of state-action pairs by calling the revision-method for each state action pair of the list (in the order of their occurrences in the list).
	 * 
	 * @param kb                the knowledge base to be revised
	 * @param stateActionPairs  a list of (complete) state-action pairs, where every pair reflects a new information that is considered to be evident 
	 */
	public static void revisionMulti( MultiAbstractionLevelKB kb, List<Pair<List<String>, String>> stateActionPairs )
	{
		for( Pair<List<String>, String> stateActionPair : stateActionPairs )
		{
			Pair<Set<String>, String> pairAsSet = new Pair<Set<String>, String>( new HashSet<String>( stateActionPair.getFirst() ), stateActionPair.getSecond() );
			revision( kb, pairAsSet );
		}
	}
}
