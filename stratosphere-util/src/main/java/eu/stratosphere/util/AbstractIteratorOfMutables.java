package eu.stratosphere.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Base iterator implementation for a read-only iterator.<br>
 * This skeleton implementation eases the development of new iterators since only {@link #loadNext()} needs to be
 * overwritten.
 * 
 * @param <T>
 *        the type of the elements
 */
public abstract class AbstractIteratorOfMutables<T> implements Iterator<T> {
	private boolean initialized;

	private boolean hasNext = true;

	private T currentValue;

	@Override
	public boolean hasNext() {
		if (!this.initialized) {
			this.currentValue = this.loadNext();
			this.initialized = true;
		}
		return this.hasNext;
	}

	@Override
	public T next() {
		if (!this.hasNext)
			throw new NoSuchElementException();
		if (!this.initialized) {
			this.currentValue = this.loadNext();
			this.initialized = true;
		}

		final T value = this.copy(this.currentValue);
		this.currentValue = this.loadNext();
		return value;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected abstract T copy(T currentValue);

	/**
	 * Returns the next element or the result of {@link #noMoreElements()}.
	 * 
	 * @return the next element
	 */
	protected abstract T loadNext();

	/**
	 * Signal methods that should be invoked when no more elements are in the iterator.
	 * 
	 * @return a signal that no more elements are in this iterator
	 */
	protected T noMoreElements() {
		this.hasNext = false;
		return null;
	}
}