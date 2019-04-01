package uk.ac.bris.cs.scotlandyard.model;

import java.util.Collections;
import java.util.List;

public class ListCycler<T> {
	private List<T> list;
	private int currentIndex = 0;

	ListCycler(List<T> list) {
		this.list = list;
	}

	public T current() {
		return this.list.get(this.currentIndex);
	}

	@SuppressWarnings("UnusedReturnValue")
	public T next() {
		this.currentIndex = (this.currentIndex + 1) % this.list.size();
		return this.current();
	}

	public List<T> list() {
		return Collections.unmodifiableList(this.list);
	}
}