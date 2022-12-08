package endorh.simpleconfig.core.reflection;

import endorh.simpleconfig.api.ConfigEntryBuilder;

import java.lang.annotation.Annotation;
import java.util.function.BiFunction;

public interface FieldBuilderDecorator<B extends ConfigEntryBuilder<?, ?, ?, ? extends B>> {
	static <B extends ConfigEntryBuilder<?, ?, ?, ? extends B>> FieldBuilderDecorator<B> of(
	  Class<B> a, FieldDecorator<B> builder
	) {
		return new FieldBuilderDecoratorImpl<>(a, builder);
	}
	
	static <B extends ConfigEntryBuilder<?, ?, ?, ? extends B>> FieldBuilderDecorator<B> of(
	  Class<B> a, BiFunction<EntryTypeData, B, B> builder
	) {
		return new FieldBuilderDecoratorImpl<>(a, (c, aa, b) -> builder.apply(aa, b));
	}
	
	boolean isApplicable(ConfigEntryBuilder<?, ?, ?, ?> builder);
	<BB extends B> B decorate(EntryTypeData a, BB builder);
	
	class FieldBuilderDecoratorImpl<B extends ConfigEntryBuilder<?, ?, ?, ? extends B>> implements FieldBuilderDecorator<B> {
		private final Class<?> type;
		private final FieldDecorator<B> decorator;
		
		public FieldBuilderDecoratorImpl(Class<B> type, FieldDecorator<B> decorator) {
			this.type = type;
			this.decorator = decorator;
		}
		
		
		@Override public boolean isApplicable(ConfigEntryBuilder<?, ?, ?, ?> builder) {
			return type.isInstance(builder);
		}
		
		@Override public <BB extends B> BB decorate(EntryTypeData data, BB builder) {
			// noinspection unchecked
			return (BB) decorator.decorate(data.getMethodBindingContext(), data, builder);
		}
	}
	
	interface FieldDecorator<B extends ConfigEntryBuilder> {
		B decorate(MethodBindingContext ctx, EntryTypeData data, B builder);
	}
	
	interface AnnotationFieldDecorator<A extends Annotation, B extends ConfigEntryBuilder> {
		B decorate(MethodBindingContext ctx, EntryTypeData data, A a, B builder);
	}
}
