package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {

    private Comparator<T> comparator;

    /** Creates a MaxArrayDeque with the given Comparator. */
    public MaxArrayDeque(Comparator<T> c) {
        super();
        this.comparator = c;
    }

    /** returns the maximum element in the deque as governed by the previously given Comparator.
     *  If the MaxArrayDeque is empty, simply return null. */
    public T max() {
        return max(comparator);
    }

    /** returns the maximum element in the deque as governed by the parameter Comparator c.
     *  If the MaxArrayDeque is empty, simply return null. */
    public T max(Comparator<T> c) {
        if (isEmpty()) {
            return null;
        }
        T maxItem = get(0);
        for (int i = 1; i < size(); i++) {
            T current = get(i);
            if (c.compare(current, maxItem) > 0) {
                maxItem = current;
            }
        }
        return maxItem;
    }
}


