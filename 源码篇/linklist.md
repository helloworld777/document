LinkedList

	public class LinkedList<E>
	    extends AbstractSequentialList<E>
	    implements List<E>, Deque<E>, Cloneable, java.io.Serializable

	//添加到末尾
	 public boolean add(E e) {
        linkLast(e);
        return true;
    }
	//添加到指定位置
	 public E set(int index, E element) {
	        checkElementIndex(index);
	        Node<E> x = node(index);
	        E oldVal = x.item;
	        x.item = element;
	        return oldVal;
	   }
		
	//根据索引查找到该数据
	  Node<E> node(int index) {
        // assert isElementIndex(index);
		//如果索引值在长度/2内，从头开始找，否则从尾开始找
        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
	//添加到末尾
	public boolean offer(E e) {
        return add(e);
    }
	//添加到头
	public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }
	//添加到末尾
	public boolean offerLast(E e) {
	        addLast(e);
	        return true;
	  }
	
	//添加到头
	public void push(E e) {
        addFirst(e);
    }