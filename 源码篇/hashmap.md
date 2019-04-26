集合之HashMap
		  /**
			默认数组大小
	     * The default initial capacity - MUST be a power of two.
	     */
	    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
	
	    /**
			最大容量
	     * The maximum capacity, used if a higher value is implicitly specified
	     * by either of the constructors with arguments.
	     * MUST be a power of two <= 1<<30.
	     */
	    static final int MAXIMUM_CAPACITY = 1 << 30;
	
	    /**
	     * The load factor used when none specified in constructor.
	     */
	    static final float DEFAULT_LOAD_FACTOR = 0.75f;

定义了初始值默认大小为16.如果调用构造器的时候没传入大小。则默认大小为16.还定义的超过大小的多少则扩容。  
我们都知道集合不用你自己管理它的大小。只管往里面添加数据就好。但是集合自己就要处理，只要是底层是数组实现的，则都要考虑扩容。这里是超过集合大小的0.75则就扩容。扩容是当前集合大小的2倍




先看放数据的put方法

	 public V put(K key, V value) {
	        return putVal(hash(key), key, value, false, true);
	    }

调用hash方法生成hash值。并且允许key为null.

	static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

调用内部的putVal方法

	final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
	                   boolean evict) {
	        Node<K,V>[] tab; Node<K,V> p; int n, i;
	        if ((tab = table) == null || (n = tab.length) == 0)
				//1 初始化，或者扩容
	            n = (tab = resize()).length;
			//2 找到位置，并且为null,直接放入
	        if ((p = tab[i = (n - 1) & hash]) == null)
	            tab[i] = newNode(hash, key, value, null);
			//3 找到位置，并且有值
	        else {
				
	            Node<K,V> e; K k;
				//4 key是一样的。直接替换旧的
	            if (p.hash == hash &&
	                ((k = p.key) == key || (key != null && key.equals(k))))
	                e = p;
				//5 发送了碰撞。如果是树节点。1.8之后如果链表长度超过7，则转换为树结构
	            else if (p instanceof TreeNode)
	                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
	            else {
					//6 放到链表后面
	                for (int binCount = 0; ; ++binCount) {
						//7 当前位置后面没有。则直接放在后面
	                    if ((e = p.next) == null) {
	                        p.next = newNode(hash, key, value, null);
							//8 如果链表长度超过7，则转换为数结构
	                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
	                            treeifyBin(tab, hash);
	                        break;
	                    }
						//9 同样，如果找个key一样的。替换旧的
	                    if (e.hash == hash &&
	                        ((k = e.key) == key || (key != null && key.equals(k))))
	                        break;
						//没找到继续找。直到末尾。
	                    p = e;
	                }
	            }
				//10 如果找到了替换的。则返回旧值，否则返回null
	            if (e != null) { // existing mapping for key
	                V oldValue = e.value;
	                if (!onlyIfAbsent || oldValue == null)
	                    e.value = value;
	                afterNodeAccess(e);
	                return oldValue;
	            }
	        }
	        ++modCount;
			//11 判断是否要扩容
	        if (++size > threshold)
	            resize();
	        afterNodeInsertion(evict);
	        return null;
	    }

注释1 如果table数组还没初始化。则调用resize方法初始化。resize方法有两作用，1是初始化table数组。2是判断是否要扩容。如果需要则扩容。注释 2 则根据hash找到该key所在table的位置。如果该位置为null.则说明还没有数据，直接放里面就行。注释3 如果有数据。注释 4 判断key是不是和要放入的key是不是一样的。如果是一样的则替换。注释5 如果不一样。则判断后面是不是树节点。如果是树节点，则插入树中。注释6 如果是链表。则去链表中找。注释 7 如果在链表中没有找到到了一样的key，则判断链接是否超过了7.如果没有，则放到链表后面，如果超过了7.则转换为数结构。注释9 如果找到了一样的key，同样的替换旧的值，返回旧的值。


		//初始化或者扩容
	    final Node<K,V>[] resize() {
	        Node<K,V>[] oldTab = table;
	        int oldCap = (oldTab == null) ? 0 : oldTab.length;
	        int oldThr = threshold;
	        int newCap, newThr = 0;
	       ...
	            newCap = DEFAULT_INITIAL_CAPACITY;
	            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
	       ...
	        threshold = newThr;
	        @SuppressWarnings({"rawtypes","unchecked"})
				//1 初始化一个大小为16的数组
	            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
	        table = newTab;
	        ...
	        return newTab;
	    }

查看获取的方法get		

	   public V get(Object key) {
	        Node<K,V> e;
	        return (e = getNode(hash(key), key)) == null ? null : e.value;
	    }
		
调用getNode方法

	    final Node<K,V> getNode(int hash, Object key) {
	        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
			//1 如果table不为Null,并且长度大于0，并且根据key能获取到对应的value
	        if ((tab = table) != null && (n = tab.length) > 0 &&
	            (first = tab[(n - 1) & hash]) != null) {
				//2 根据hash和key对象匹配
	            if (first.hash == hash && // always check first node
	                ((k = first.key) == key || (key != null && key.equals(k))))
	                return first;
				//3 去后面的链表或者树里面寻找
	            if ((e = first.next) != null) {
	                if (first instanceof TreeNode)
	                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
	                do {
	                    if (e.hash == hash &&
	                        ((k = e.key) == key || (key != null && key.equals(k))))
	                        return e;
	                } while ((e = e.next) != null);
	            }
	        }
	        return null;
	    }
注释1 判断table是否初始化了，并且长度大于0，并且根据hash拿到对应的数据，注释2根据hash和key匹配到数据，则返回。否则则去链表或者树结构中查找。


移除也是类似

	public V remove(Object key) {
	        Node<K,V> e;
	        return (e = removeNode(hash(key), key, null, false, true)) == null ?
	            null : e.value;
	    }

先从数组中匹配数据。如果找到则，则把数组中的数据指向next.删除数组中的数据不影响后面的链表或者树节点。如果在链表中或者树节点中。则去链表中或者树节点中删除。


		
	 final Node<K,V> removeNode(int hash, Object key, Object value,
	                               boolean matchValue, boolean movable) {
	        Node<K,V>[] tab; Node<K,V> p; int n, index;
	        if ((tab = table) != null && (n = tab.length) > 0 &&
	            (p = tab[index = (n - 1) & hash]) != null) {
	            Node<K,V> node = null, e; K k; V v;
				//根据hash和key对象匹配
	            if (p.hash == hash &&
	                ((k = p.key) == key || (key != null && key.equals(k))))
	                node = p;
	            else if ((e = p.next) != null) {
					//如果是树结构。去树结构找
	                if (p instanceof TreeNode)
	                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
	                else {
						//去链表找
	                    do {
	                        if (e.hash == hash &&
	                            ((k = e.key) == key ||
	                             (key != null && key.equals(k)))) {
	                            node = e;
	                            break;
	                        }
	                        p = e;
	                    } while ((e = e.next) != null);
	                }
	            }
	            if (node != null && (!matchValue || (v = node.value) == value ||
	                                 (value != null && value.equals(v)))) {
	                if (node instanceof TreeNode)
						//在树中移除
	                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
	                else if (node == p)
						//在数组中赋值
	                    tab[index] = node.next;
	                else
						//在链表中赋值
	                    p.next = node.next;
	                ++modCount;
	                --size;
	                afterNodeRemoval(node);
	                return node;
	            }
	        }
	        return null;
	    }


小结：通过源码分析，如果初始化hashMap的时候不传入大小。则默认会初始化一个16个大小的数组。
存入数据是hash函数根据key对象的hashCode值做一些处理，生成hash值，再与数组长度-1做异或处理生成索引值。
如果发送了碰撞，也就是产生的索引值在当前数组里面已经有数据了，则以链表形式放在该数据后面，JDK1.8之后，判断该链表是否>=7.则转换为树结构