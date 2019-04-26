集合之ArrayList

 	
	/**
     * Default initial capacity.
     */
    private static final int DEFAULT_CAPACITY = 10;

默认初始数组大小为10


		/**
	     * Appends the specified element to the end of this list.
	     *
	     * @param e element to be appended to this list
	     * @return <tt>true</tt> (as specified by {@link Collection#add})
	     */
	    public boolean add(E e) {
	        ensureCapacityInternal(size + 1);  // Increments modCount!!
			//放到数组最后边
	        elementData[size++] = e;
	        return true;
	    }

	//判断如果是初始值，如果是则先创建一个默认10的数组。
	//如果不是。则判断是否超过现在的数组长度。如果超过，则扩容1.5倍
	 private void ensureCapacityInternal(int minCapacity) {
		//如果是初始空数组值
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
			//创建一个不大于10的数组
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        ensureExplicitCapacity(minCapacity);
    }
	
	//判断是否扩容
	 private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

        // overflow-conscious code
		//如果比数组还大了。扩容
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }
	
	//扩容
	private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;
		//扩容原来的1.5倍
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        elementData = Arrays.copyOf(elementData, newCapacity);
    }


		//添加元素，
	public void add(int index, E element) {
	        if (index > size || index < 0)
	            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
			//判断是否需要扩容
	        ensureCapacityInternal(size + 1);  // Increments modCount!!
			//在[index,leng]位置赋值到[index+1,leng+1],留出index位置
			//假设index = 2 , 原数组为12345 经过arraycopy后---> 120345 
	        System.arraycopy(elementData, index, elementData, index + 1,
	                         size - index);
	        elementData[index] = element;
	        size++;
	}

	public E get(int index) {
	        if (index >= size)
	            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
			//直接从数组里面获取
	        return (E) elementData[index];
	    }

	//移除元素
	public E remove(int index) {
	        if (index >= size)
	            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	
	        modCount++;
			//拿到要移除的值
	        E oldValue = (E) elementData[index];
	        int numMoved = size - index - 1;
	        if (numMoved > 0)
				//把[index+1,end] 赋值到[index,end]
				//假设index = 2 , 原数组为12345 经过arraycopy后---> 12455 
	            System.arraycopy(elementData, index+1, elementData, index,
	                             numMoved);
			//把最后一个置空
	        elementData[--size] = null; // clear to let GC do its work
	
	        return oldValue;
	    }

小结：ArrayList集合就简单了。底层用数组实现。构造的时候不传大小默认大小为10，每次超过数组大小和扩容为原来的1.5倍。