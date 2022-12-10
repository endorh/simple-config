package endorh.simpleconfig.core.reflection;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.annotation.Bean;
import endorh.simpleconfig.api.annotation.Entry;
import endorh.simpleconfig.api.annotation.Group.Caption;
import endorh.simpleconfig.api.entry.BeanEntryBuilder;
import endorh.simpleconfig.core.entry.BeanEntry.Builder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static java.util.Collections.synchronizedMap;

public class BeanClassParser {
	private static final Map<Class<?>, BeanEntryBuilder<?>> BEAN_MAP = synchronizedMap(new HashMap<>());
	private static Stack<Class<?>> foundBeans = null;
	private static Class<?> rootClass = null;
	
	@SuppressWarnings("unchecked") public static <T> BeanEntryBuilder<T> create(T bean) {
		synchronized (BEAN_MAP) {
			Class<?> beanClass = bean.getClass();
			BeanEntryBuilder<?> builder = BEAN_MAP.get(beanClass);
			if (builder != null) return ((BeanEntryBuilder<T>) builder).withValue(bean);
			BeanEntryBuilder<T> b = new Builder<>(bean);
			if (beanClass.isAnnotationPresent(Bean.class)) {
				boolean root = foundBeans == null;
				if (root) {
					foundBeans = new Stack<>();
					rootClass = beanClass;
				}
				b = decorateBean(b, bean);
				if (root) {
					foundBeans = null;
					rootClass = null;
				}
			}
			BEAN_MAP.put(beanClass, b);
			return b;
		}
	}
	
	private static <T> BeanEntryBuilder<T> decorateBean(BeanEntryBuilder<T> b, T bean) {
		Class<?> beanClass = bean.getClass();
		if (foundBeans.contains(beanClass)) throw new IllegalArgumentException(
		  "Recursion detected in config bean class: " + rootClass.getCanonicalName()
		  + ", bean " + beanClass.getCanonicalName() + " contains a transitive reference to itself.");
		foundBeans.push(beanClass);
		BindingContext ctx = new BindingContext(beanClass, null);
		Field[] fields = beanClass.getFields();
		boolean captionFound = false;
		for (Field field: fields) {
			field.setAccessible(true);
			if (field.isAnnotationPresent(Entry.class)
			    || field.isAnnotationPresent(Caption.class)) {
				ConfigEntryBuilder<?, ?, ?, ?> entry = FieldParser.parseInstanceField(ctx, field, bean);
				if (entry != null) {
					if (field.isAnnotationPresent(Caption.class)) {
						if (captionFound) throw new IllegalArgumentException(
						  "Multiple @Caption entries found in Bean class");
						captionFound = true;
						b = b.caption(field.getName(), castCaption(entry));
					} else b = b.add(field.getName(), entry);
				}
			}
		}
		foundBeans.pop();
		return b;
	}
	
	@SuppressWarnings("unchecked") private static <B extends ConfigEntryBuilder<?, ?, ?, ?> & AtomicEntryBuilder> B castCaption(
	  ConfigEntryBuilder<?, ?, ?, ?> b
	) {
		if (!(b instanceof AtomicEntryBuilder)) throw new IllegalArgumentException(
		  "Caption entry does not implement AtomicEntryBUilder");
		return (B) b;
	}
}
