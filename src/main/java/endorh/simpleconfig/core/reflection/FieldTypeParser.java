package endorh.simpleconfig.core.reflection;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface FieldTypeParser<V> {
	static <V> FieldTypeParser<V> of(FieldTypeFilter filter, FieldEntryBuilder<V> builder) {
		return new FieldTypeParserImpl<>(filter, builder);
	}
	
	FieldTypeFilter getFilter();
	ConfigEntryBuilder<?, ?, ?, ?> create(EntryTypeData data, AnnotatedType aType, Type type, Class<V> cls, @Nullable V value);
	
	class FieldTypeParserImpl<V> implements FieldTypeParser<V> {
		private final FieldTypeFilter filter;
		private final FieldEntryBuilder<V> builder;
		
		public FieldTypeParserImpl(FieldTypeFilter filter, FieldEntryBuilder<V> builder) {
			this.filter = filter;
			this.builder = builder;
		}
		
		@Override public FieldTypeFilter getFilter() {
			return filter;
		}
		
		@Override public ConfigEntryBuilder<?, ?, ?, ?> create(
		  EntryTypeData data, AnnotatedType aType, Type type, Class<V> c, @Nullable V value
		) {
			return builder.build(data.getBindingContext(), data, aType, type, c, Optional.ofNullable(value));
		}
	}
}
