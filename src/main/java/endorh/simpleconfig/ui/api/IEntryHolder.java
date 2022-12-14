package endorh.simpleconfig.ui.api;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IEntryHolder {
	List<AbstractConfigField<?>> getHeldEntries();
	
	Pattern DOT = Pattern.compile("\\.");
	default @Nullable AbstractConfigField<?> getEntry(String path) {
		final String[] sp = DOT.split(path, 2);
		final List<AbstractConfigField<?>> entries = getHeldEntries();
		
		AbstractConfigField<?> entry = entries.stream()
		  .filter(e -> e.getName().equals(sp[0]))
		  .findFirst().orElse(null);
		if (sp.length < 2) return entry;
		if (entry instanceof IEntryHolder)
			return ((IEntryHolder) entry).getEntry(sp[1]);
		return entry;
	}
	
	/**
	 * Get all the entries recursively
	 */
	default List<AbstractConfigField<?>> getAllMainEntries() {
		return getAllEntries(e -> !e.isSubEntry());
	}
	
	/**
	 * Get all the entries recursively
	 */
	default List<AbstractConfigField<?>> getAllEntries(Predicate<AbstractConfigField<?>> filter) {
		final List<AbstractConfigField<?>> list = Lists.newArrayList();
		getHeldEntries().stream()
		  .filter(filter)
		  .flatMap(
			 e -> e instanceof IEntryHolder
			      ? Stream.concat(Stream.of(e), ((IEntryHolder) e).getAllEntries(filter).stream())
			      : Stream.of(e)
		  ).forEach(list::add);
		return list;
	}
	
	default List<EntryError> getErrors() {
		return getHeldEntries().stream()
		  .flatMap(e -> e.getErrors().stream())
		  .collect(Collectors.toList());
	}
	
	default boolean hasErrors() {
		return !getErrors().isEmpty();
	}
	
	default List<AbstractConfigField<?>> getAllExternalConflicts() {
		return getAllMainEntries().stream()
		  .filter(AbstractConfigField::hasConflictingExternalDiff)
		  .collect(Collectors.toList());
	}
	
	default boolean hasExternalConflicts() {
		return getAllMainEntries().stream()
		  .anyMatch(AbstractConfigField::hasConflictingExternalDiff);
	}
	
	/**
	 * Implementers should also implement {@link IEntryHolder#resetSingleEntry}
	 */
	default @Nullable AbstractConfigField<?> getSingleResettableEntry() {
		return null;
	}
	
	/**
	 * Implementers should also implement {@link IEntryHolder#restoreSingleEntry}
	 */
	default @Nullable AbstractConfigField<?> getSingleRestorableEntry() {
		return null;
	}
	
	/**
	 * Implement if you return non-null values from {@link IEntryHolder#getSingleResettableEntry()}
	 */
	default void resetSingleEntry(AbstractConfigField<?> entry) {}
	/**
	 * Implement if you return non-null values from {@link IEntryHolder#getSingleRestorableEntry()}
	 */
	default void restoreSingleEntry(AbstractConfigField<?> entry) {}
}
