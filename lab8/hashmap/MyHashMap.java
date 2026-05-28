package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {
    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;
    // You should probably define some more!
    private static final int INIT_CAPACITY = 16;
    private static final double INIT_MAXLOAD = 0.75;
    private double loadFactor;
    private int n;  // number of elements
    private int m;  // number of buckets
    private HashSet<K> keys;

    /** Constructors */
    public MyHashMap() {
        this(INIT_CAPACITY, INIT_MAXLOAD);
    }

    public MyHashMap(int initialSize) {
        this(initialSize, INIT_MAXLOAD);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.m = initialSize;
        this.loadFactor = maxLoad;
        this.n = 0;
        this.keys = new HashSet<>();
        this.buckets = createTable(initialSize);
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        Collection<Node>[] table = (Collection<Node>[]) new Collection[tableSize];
        for (int i = 0; i < tableSize; i++){
            table[i] = createBucket();
        }
        return table;
    }

    // TODO: Implement the methods of the Map61B Interface below
    // Your code won't compile until you do so!
    @Override
    public void clear() {
        this.n = 0;
        this.buckets = createTable(m);
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) throw new IllegalArgumentException("argument to containsKey() is null");
        return get(key) != null;
    }

    @Override
    public V get(K key) {
        if (key == null) throw new IllegalArgumentException("argument to get() is null");
        int i = hash(key);
        for (Node node: buckets[i]){
            if (node.key.equals(key)){
                return node.value;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return n;
    }

    @Override
    public void put(K key, V val) {
        if (key == null) throw new IllegalArgumentException("first argument to put() is null");
        if (val == null) {
            remove(key);
            return;
        }

        // double table size if average length of list >= 10
        if (n >= loadFactor*m) resize(2*m);

        int i = hash(key);

        for (Node node : buckets[i]) {
            if (node.key.equals(key)) {
                node.value = val;
                return;
            }
        }

        buckets[i].add(createNode(key, val));
        keys.add(key);
        n += 1;
    }

    @Override
    public Set<K> keySet() {
        return keys;
    }

    @Override
    public V remove(K key) {
        if (key == null) throw new IllegalArgumentException("argument to delete() is null");
        V result = null;
        int i = hash(key);
        for (Node node : buckets[i]) {
            if (node.key.equals(key)) {
                result = node.value;
                node.value = null;
            }
        }
        return result;
    }

    @Override
    public V remove(K key, V val) {
        if (key == null) throw new IllegalArgumentException("argument to delete() is null");
        V result = null;
        int i = hash(key);
        for (Node node : buckets[i]) {
            if (node.key.equals(key)) {
                n--;
                buckets[i].remove(node);
                keys.remove(key);
                result = val;
            }
        }

        // halve table size if average length of list <= 2
        if (m > INIT_CAPACITY && 2*n <= loadFactor*m) resize(Math.max(m/2, INIT_CAPACITY));
        return result;
    }

    @Override
    public Iterator<K> iterator() {
        return keys.iterator();
    }

    // resize the hash table to have the given number of chains,
    // rehashing all of the keys
    private void resize(int chains) {
        MyHashMap<K, V> temp = new MyHashMap<>(chains);
        for (int i = 0; i < m; i++) {
            for (Node node : buckets[i]) {
                temp.put(node.key, node.value);
            }
        }
        this.m  = temp.m;
        this.n  = temp.n;
        this.buckets = temp.buckets;
    }

    // hash function for keys - returns value between 0 and m-1
    private int hash(K key) {
        return (key.hashCode() & 0x7fffffff) % m;
    }
}
