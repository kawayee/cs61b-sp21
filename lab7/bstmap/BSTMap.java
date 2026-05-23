package bstmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.NoSuchElementException;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {

    private BSTNode root;

    private class BSTNode {
        private K key;
        private V val;
        private BSTNode left;
        private BSTNode right;
        private int size;

        private BSTNode(K key, V val, int size) {
            this.key = key;
            this.val = val;
            this.size = size;
        }
    }

    public BSTMap() {
        root = null;
    }

    @Override
    public void clear() {
        root = null;
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("calls containsKey() with a null key");
        }
        return containsKey(root, key);
    }

    private boolean containsKey(BSTNode node, K key){
        if (node == null) return false;

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            return containsKey(node.left, key);
        } else if (cmp > 0) {
            return containsKey(node.right, key);
        } else {
            return true;
        }
    }

    @Override
    public V get(K key) {
        if (key == null) throw new IllegalArgumentException("calls get() with a null key");
        return get(root, key);
    }

    private V get(BSTNode node, K key) {
        if (node == null) return null;

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            return get(node.left, key);
        } else if (cmp > 0) {
            return get(node.right, key);
        } else {
            return node.val;
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return size(root);
    }

    private int size(BSTNode node) {
        if (node == null) {
            return 0;
        }
        return node.size;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("calls put() with a null key");
        }
        root = put(root, key, value);
    }

    private BSTNode put(BSTNode node, K key, V value) {
        if (node == null) {
            return new BSTNode(key, value, 1);
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = put(node.left, key, value);
        } else if (cmp > 0) {
            node.right = put(node.right, key, value);
        } else {
            node.val = value;
        }

        node.size = 1 + size(node.left) + size(node.right);
        return node;
    }

    public void printInOrder() {
        printInOrder(root);
    }

    private void printInOrder(BSTNode node) {
        if (node == null) {
            return;
        }

        printInOrder(node.left);
        System.out.println(node.key + " " + node.val);
        printInOrder(node.right);
    }

    @Override
    public Set<K> keySet() {
        HashSet<K> keys = new HashSet<>();
        if (isEmpty()) return keys;
        keySet(root, keys);
        return keys;
    }

    private void keySet(BSTNode node, Set<K> keys){
        if (node == null) return;

        keySet(node.left, keys);
        keys.add(node.key);
        keySet(node.right, keys);
    }

    @Override
    public V remove(K key) {
        if (key == null) throw new IllegalArgumentException("calls remove(K) with a null key");
        V removedValue = get(key);
        if (removedValue == null) return null;
        root = remove(root, key);
        return removedValue;
    }

    private BSTNode remove(BSTNode node, K key) {
        if (node == null) return null;

        int cmp = key.compareTo(node.key);
        if      (cmp < 0) node.left  = remove(node.left,  key);
        else if (cmp > 0) node.right = remove(node.right, key);
        else {
            if (node.right == null) return node.left;
            if (node.left  == null) return node.right;
            BSTNode temp = node;
            node = min(temp.right);
            node.right = deleteMin(temp.right);
            node.left = temp.left;
        }
        node.size = size(node.left) + size(node.right) + 1;
        return node;
    }

    @Override
    public V remove(K key, V value){
        if (key == null) throw new IllegalArgumentException("calls remove(K, V) with a null key");
        V removedValue = get(key);
        if (removedValue == null || !removedValue.equals(value)) return null;
        root = remove(root, key);
        return removedValue;
    }

    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }

    /**
     * Removes the smallest key and associated value from the symbol table.
     *
     * @throws NoSuchElementException if the symbol table is empty
     */
    private BSTNode deleteMin(BSTNode node) {
        if (node.left == null) return node.right;
        node.left = deleteMin(node.left);
        node.size = size(node.left) + size(node.right) + 1;
        return node;
    }

    /**
     * Returns the smallest key in the symbol table.
     *
     * @return the smallest key in the symbol table
     * @throws NoSuchElementException if the symbol table is empty
     */
    private BSTNode min(BSTNode node) {
        if (node.left == null) return node;
        else                   return min(node.left);
    }
}
