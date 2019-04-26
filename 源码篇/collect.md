		public interface Collection<E> extends Iterable<E> {
		    // Query Operations
		
		    /**
		     * Returns the number of elements in this collection.  If this collection
		     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
		     * <tt>Integer.MAX_VALUE</tt>.
		     * @return the number of elements in this collection
		     */
		    int size();
		
		    /**
		     * Returns <tt>true</tt> if this collection contains no elements.
		     * @return <tt>true</tt> if this collection contains no elements
		     */
		    boolean isEmpty();
		
		    /**
		     * Returns <tt>true</tt> if this collection contains the specified element.
		     * More formally, returns <tt>true</tt> if and only if this collection
		     * contains at least one element <tt>e</tt> such that
		     */
		    boolean contains(Object o);
		
		    /**
		     * Returns an iterator over the elements in this collection.  There are no
		     * guarantees concerning the order in which the elements are returned
		     * (unless this collection is an instance of some class that provides a
		     * guarantee).
		     *
		     * @return an <tt>Iterator</tt> over the elements in this collection
		     */
		    Iterator<E> iterator();
		
		    /**
		     *
		     * @return an array containing all of the elements in this collection
		     */
		    Object[] toArray();
		
		    /**
		     * @throws NullPointerException if the specified array is null
		     */
		    <T> T[] toArray(T[] a);
		
		    // Modification Operations
		
		    /**
		     */
		    boolean add(E e);
		
		    /**
		     */
		    boolean remove(Object o);
		
	
		
		    /**
		     */
		    boolean containsAll(Collection<?> c);
		
		    /**
		     * @see #add(Object)
		     */
		    boolean addAll(Collection<? extends E> c);
		
		    /**
		     */
		    boolean removeAll(Collection<?> c);
		
		   
		
		  
		    boolean retainAll(Collection<?> c);
		
		   
		    void clear();
		
		
		    // Comparison and hashing
		
		    /**
		     * 
		     */
		    boolean equals(Object o);
		
		    /**
		     *
		     */
		    int hashCode();
		
		  