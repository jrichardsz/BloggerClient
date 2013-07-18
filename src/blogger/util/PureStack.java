package blogger.util;

import java.util.LinkedList;

/**
 * A simple stack implementation.<br/>
 * It is NOT thread-safe.
 */
public final class PureStack<E> {

	private final LinkedList<E> linkedList = new LinkedList<>();

	public void push(E e) {
		linkedList.addLast(e);
	}

	/**
	 * @return the element, or null if stack is empty
	 */
	public E pop() {
		E e = peek();
		if (e != null)
			return linkedList.removeLast();
		else
			return null;
	}

	/**
	 * @return the element, or null if stack is empty
	 */
	public E peek() {
		return linkedList.peekLast();
	}

	public int size() {
		return linkedList.size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

}
