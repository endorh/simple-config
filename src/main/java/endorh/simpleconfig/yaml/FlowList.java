package endorh.simpleconfig.yaml;

import com.google.common.collect.ForwardingList;

import java.util.ArrayList;
import java.util.List;

/**
 * Forwarding list used for marking purposes.
 */
public class FlowList<E> extends ForwardingList<E> {
	private final List<E> delegate;
	
	public static <E> FlowList<E> wrap(List<E> list) {
		return new FlowList<>(list);
	}
	
	public static <E> FlowList<E> ofArrayList(int initialCapacity) {
		return wrap(new ArrayList<>(initialCapacity));
	}
	
	private FlowList(List<E> delegate) {
		this.delegate = delegate;
	}
	
	@Override protected List<E> delegate() {
		return delegate;
	}
}
