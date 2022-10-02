package endorh.simpleconfig.core.entry;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public interface BeanProxy<B> {
	default B create() {
		return create(null);
	}
	B create(@Nullable Map<String, Object> properties);
	default B createFrom(B def) {
		return createFrom(def, null);
	}
	B createFrom(B def, @Nullable Map<String, Object> properties);
	B createFromGUI(B def, @Nullable Map<String, Object> properties);
	
	Object get(B bean, String name);
	Object getGUI(B bean, String name);
	
	String getTypeName();
	String getPropertyName(String name);
	String getTypeTranslation();
	String getTranslation(String property);
	
	interface IBeanGuiAdapter {
		static <T> IBeanGuiAdapter of(Function<Object, T> forGui, Function<T, Object> fromGui) {
			return new IBeanGuiAdapter() {
				@Override public @Nullable Object forGui(@Nullable Object value) {
					return forGui.apply(value);
				}
				
				@SuppressWarnings("unchecked") @Override
				public @Nullable Object fromGui(@Nullable Object value) {
					try {
						return fromGui.apply((T) value);
					} catch (ClassCastException e) {
						return null;
					}
				}
			};
		}
		
		@Nullable Object forGui(@Nullable Object value);
		@Nullable Object fromGui(@Nullable Object value);
	}
}
