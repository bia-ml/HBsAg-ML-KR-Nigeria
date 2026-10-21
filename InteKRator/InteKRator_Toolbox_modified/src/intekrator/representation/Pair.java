package intekrator.representation;


/**
 * A simple lightweight pair class.
 * 
 * @author Daan Apeldoorn
 */
public class Pair<T1,T2>
{
	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}


	/** The pair's first element. */
	T1 first;

	/** The pair's second element. */
	T2 second;
	

	/**
	 * Creates a new pair object. 
	 * 
	 * @param first   the first element of pair
	 * @param second  the second element of pair
	 */
	public Pair( T1 first, T2 second )
	{
		this.first = first;
		this.second = second;
	}
	
	
	/**
	 * Returns the first element of the pair.
	 * 
	 * @return  the first element of the pair 
	 */
	public T1 getFirst()
	{
		return first;
	}


	/**
	 * Returns the second element of the pair.
	 * 
	 * @return  the second element of the pair
	 */
	public T2 getSecond()
	{
		return second;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}
}
