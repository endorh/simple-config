package endorh.simple_config.clothconfig2.api;

import java.util.Objects;
import java.util.function.Supplier;

public final class LazyResettable<T>
  implements Supplier<T> {
	private final Supplier<T> supplier;
	private T value = null;
	private boolean supplied = false;
	
	public LazyResettable(Supplier<T> supplier) {
		this.supplier = supplier;
	}
	
	@Override
	public T get() {
		if (!this.supplied) {
			this.value = this.supplier.get();
			this.supplied = true;
		}
		return this.value;
	}
	
	public void reset() {
		this.supplied = false;
		this.value = null;
	}
	
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}
		LazyResettable<?> that = (LazyResettable<?>) o;
		return Objects.equals(this.get(), that.get());
	}
	
	public int hashCode() {
		T value = this.get();
		return value != null ? value.hashCode() : 0;
	}
}

