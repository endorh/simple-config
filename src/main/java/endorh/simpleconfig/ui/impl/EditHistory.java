package endorh.simpleconfig.ui.impl;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.IEntryHolder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.entries.BaseListCell;
import endorh.simpleconfig.ui.gui.entries.BaseListEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EditHistory {
	private static final Logger LOGGER = LogManager.getLogger();
	protected AbstractConfigScreen owner;
	protected int maxSize = 100;
	protected List<EditRecord> records;
	protected @Nullable EditRecord preservedState;
	protected @Nullable EditRecord collector;
	protected int cursor = 0;
	protected Runnable onHistory;
	protected Supplier<Boolean> peek;
	protected boolean insideTransparentAction = false;
	
	public EditHistory() {
		this.records = new ArrayList<>();
	}
	
	public EditHistory(EditHistory previous) {
		records = new ArrayList<>(previous.records);
		maxSize = previous.maxSize;
		cursor = records.size();
		preservedState = null;
		collector = null;
		onHistory = null;
		peek = null;
		owner = null;
		insideTransparentAction = false;
	}
	
	public void setOwner(@Nullable AbstractConfigScreen owner) {
		this.owner = owner;
	}
	
	protected AbstractConfigScreen getOwner() {
		if (owner == null) throw new IllegalStateException("Cannot use unowned EditHistory");
		return owner;
	}
	
	public void runAtomicTransparentAction(Runnable action) {
		runAtomicTransparentAction(null, action);
	}
	
	public void runAtomicTransparentAction(@Nullable AbstractConfigEntry<?> focus, Runnable action) {
		final boolean insideAction = insideTransparentAction;
		if (!insideAction) {
			startBatch(focus);
			insideTransparentAction = true;
		}
		action.run();
		if (!insideAction) {
			saveBatch();
			insideTransparentAction = false;
		}
		// getOwner().commitHistory();
	}
	
	/**
	 * Consider using {@link EditHistory#runAtomicTransparentAction} instead,
	 * which supports nested batches.
	 */
	public void startBatch(@Nullable AbstractConfigEntry<?> focus) {
		if (preservedState != null) saveState();
		if (collector != null) saveBatch();
		collector = new EditRecord(focus != null? focus.getPath() : null, new HashMap<>(), null);
	}
	
	/**
	 * Consider using {@link EditHistory#runAtomicTransparentAction} instead, which supports
	 * nested batches.
	 */
	public void saveBatch() {
		saveState();
		if (collector != null) {
			final EditRecord c = this.collector;
			collector = null;
			c.flatten(getOwner());
			addRecord(c);
		}
	}
	
	public void preserveState(EditRecord record) {
		if (preservedState != null) saveState();
		preservedState = record;
	}
	
	public void preserveState(AbstractConfigEntry<?> entry) {
		if (preservedState != null) saveState();
		preservedState = EditRecord.of(entry);
	}
	
	public void discardPreservedState() {
		preservedState = null;
	}
	
	/**
	 * It's usually not necessary to call saveState after modifications.
	 * Calling {@link EditHistory#preserveState} before the changes
	 * is enough.
	 */
	public void saveState() {
		if (preservedState != null) {
			preservedState.flatten(getOwner());
			if (preservedState.size() > 0)
				addRecord(preservedState);
			preservedState = null;
		}
	}
	
	public void add(EditRecord record) {
		if (preservedState != null) saveState();
		addRecord(record);
	}
	
	protected void addRecord(EditRecord record) {
		if (collector != null) {
			collector.merge(record);
		} else {
			if (cursor < records.size())
				records.subList(cursor, records.size()).clear();
			records.add(record);
			if (records.size() > maxSize)
				records.remove(0);
			cursor = records.size();
		}
	}
	
	public void apply(boolean forward) {
		if (insideTransparentAction) throw new IllegalStateException(
		  "Cannot apply history inside transparent history action");
		if (preservedState != null) saveState();
		if (onHistory != null) onHistory.run();
		if (collector != null) saveBatch();
		if (forward && cursor >= records.size() || !forward && cursor <= 0) return;
		if (!forward) cursor--;
		final EditRecord record = records.get(cursor);
		final EditRecord replacement = record.apply(getOwner());
		records.set(cursor, replacement);
		if (forward) cursor++;
	}
	
	public boolean canUndo() {
		return cursor > 0 || preservedState != null && preservedState.peek(getOwner())
		       || peek != null && peek.get();
	}
	
	public boolean canRedo() {
		return cursor < records.size() && (preservedState == null || !preservedState.peek(getOwner()))
		       && (peek == null || !peek.get());
	}
	
	public int getCursor() {
		cursor = MathHelper.clamp(cursor, 0, records.size());
		return cursor;
	}
	
	public int size() {
		return records.size();
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		if (records.size() > this.maxSize)
			records.subList(0, records.size() - this.maxSize).clear();
	}
	
	public void setOnHistory(Runnable onHistory) {
		this.onHistory = onHistory;
	}
	
	public void setPeek(Supplier<Boolean> peek) {
		this.peek = peek;
	}
	
	public static class EditRecord {
		protected String focusEntry;
		protected Map<String, Object> values;
		@Nullable protected List<ListEditRecord> listRecords;
		
		protected EditRecord(
		  String focus, Map<String, Object> changes, List<ListEditRecord> listEditRecords
		) {
			values = changes;
			focusEntry = focus;
		}
		
		public static EditRecord of(AbstractConfigEntry<?> entry) {
			return new EditRecord(entry.getPath(), Util.make(
			  new HashMap<>(), m -> m.put(entry.getPath(), entry.getValue())), null);
		}
		
		public static EditRecord of(AbstractConfigEntry<?> focus, Iterable<AbstractConfigEntry<?>> entries) {
			return new EditRecord(focus.getPath(), Util.make(
			  new HashMap<>(), m -> entries.forEach(e -> m.put(e.getPath(), e.getValue()))), null);
		}
		
		public static EditRecord of(IEntryHolder holder) {
			return new EditRecord(
			  holder instanceof AbstractConfigEntry? ((AbstractConfigEntry<?>) holder).getPath() : null,
			  holder.getAllMainEntries().stream().collect(Collectors.toMap(AbstractConfigEntry::getPath, e -> e, (a, b) -> b)),
			  null);
		}
		
		public EditRecord apply(IEntryHolder holder) {
			final HashMap<String, Object> map = new HashMap<>();
			List<ListEditRecord> lr = null;
			for (Entry<String, Object> e : values.entrySet()) {
				final AbstractConfigEntry<?> entry = holder.getEntry(e.getKey());
				if (entry != null) {
					final Object v = e.getValue();
					map.put(e.getKey(), entry.getValue());
					entry.restoreHistoryValue(v);
				} else LOGGER.warn("Could not find entry with path " + e.getKey());
			}
			if (listRecords != null) {
				lr = new ArrayList<>();
				for (ListEditRecord record : listRecords) {
					final ListEditRecord r = record.apply(holder, false);
					if (!r.isEmpty()) lr.add(r);
				}
			}
			if (focusEntry != null) {
				final AbstractConfigEntry<?> focus = holder.getEntry(focusEntry);
				if (focus != null) {
					focus.claimFocus();
					focus.preserveState();
				}
			}
			return new EditRecord(focusEntry, map, lr);
		}
		
		private static <E> boolean tryCheckEqualValue(AbstractConfigEntry<E> entry, Object value) {
			try {
				//noinspection unchecked
				return entry.areEqual(entry.getValue(), (E) value);
			} catch (ClassCastException ignored) {
				return false;
			}
		}
		
		public void flatten(IEntryHolder holder) {
			final Set<String> removed = new HashSet<>();
			for (Entry<String, Object> e : values.entrySet()) {
				final AbstractConfigEntry<?> entry = holder.getEntry(e.getKey());
				if (entry == null || tryCheckEqualValue(entry, e.getValue()))
					removed.add(e.getKey());
			}
			values.keySet().removeAll(removed);
			if (listRecords != null) {
				List<ListEditRecord> removedLR = Lists.newArrayList();
				for (ListEditRecord record : listRecords) {
					record.flatten(holder);
					if (record.isEmpty()) removedLR.add(record);
				}
				listRecords.removeAll(removedLR);
				if (listRecords.isEmpty()) listRecords = null;
			}
		}
		
		protected void merge(EditRecord record) {
			if (record instanceof ListEditRecord) {
				if (listRecords == null) listRecords = new ArrayList<>();
				listRecords.add(((ListEditRecord) record));
			} else {
				values.putAll(record.values);
				// if (focusEntry == null) focusEntry = record.focusEntry;
			}
		}
		
		public boolean peek(AbstractConfigScreen holder) {
			for (Entry<String, Object> e : values.entrySet()) {
				final AbstractConfigEntry<?> entry = holder.getEntry(e.getKey());
				if (entry != null && !tryCheckEqualValue(entry, e.getValue()))
					return true;
			}
			return false;
		}
		
		public int size() {
			return values.size();
		}
		
		public boolean isEmpty() {
			return size() == 0;
		}
		
		@Internal
		public static class ListEditRecord extends EditRecord {
			public static ListEditRecord EMPTY = new ListEditRecord(
			  null, null, null, null, null);
			protected String listEntry;
			protected List<Integer> remove;
			protected Map<Integer, Object> modify;
			protected Map<Integer, Object> add;
			protected Map<Integer, Integer> move;
			
			protected ListEditRecord(
			  String listEntry, List<Integer> remove,
			  Map<Integer, Object> modify, Map<Integer, Object> add,
			  Map<Integer, Integer> move
			) {
				super(null, Collections.emptyMap(), null);
				this.listEntry = listEntry;
				this.remove = remove;
				this.modify = modify;
				this.add = add;
				this.move = move;
			}
			
			@Override public ListEditRecord apply(IEntryHolder holder) {
				return apply(holder, true);
			}
			
			public ListEditRecord apply(IEntryHolder holder, boolean claimFocus) {
				if (listEntry == null) return EMPTY;
				AbstractConfigEntry<?> entry = holder.getEntry(listEntry);
				if (!(entry instanceof BaseListEntry)) {
					LOGGER.warn("Expected a list entry at path " + listEntry);
					return EMPTY;
				}
				ListEditRecord applied = apply((BaseListEntry<?, ?, ?>) entry);
				if (claimFocus) {
					entry.claimFocus();
					entry.preserveState();
				}
				return applied;
			}
			
			public ListEditRecord apply(BaseListEntry<?, ?, ?> list) {
				if (remove != null) {
					//noinspection unchecked
					final List<Object> value = (List<Object>) list.getValue();
					Map<Integer, Object> add = new Int2ObjectArrayMap<>(remove.size());
					for (int idx : remove) {
						add.put(idx, value.get(idx));
						list.remove(idx);
						list.markRestoredCell(idx, false, true);
						value.remove(idx);
					}
					return add(list, add);
				}
				if (add != null) {
					List<Integer> remove = new IntArrayList(add.size());
					//noinspection unchecked
					final BaseListEntry<Object, ?, ?> l = (BaseListEntry<Object, ?, ?>) list;
					add.forEach((i, v) -> {
						try {
							l.add(i, v);
							l.markRestoredCell(i, true, false);
							remove.add(i);
						} catch (ClassCastException e) {
							LOGGER.warn("Error restoring removed list entry: " + e.getMessage());
						}
					});
					return remove(list, remove);
				}
				if (modify != null) {
					final List<?> value = list.getValue();
					Map<Integer, Object> mod = new Int2ObjectArrayMap<>(modify.size());
					//noinspection unchecked
					final BaseListEntry<Object, ?, ?> l = (BaseListEntry<Object, ?, ?>) list;
					modify.forEach((i, v) -> {
						try {
							final Object p = value.get(i);
							l.set(i, v);
							l.markRestoredCell(i, false, false);
							mod.put(i, p);
						} catch (ClassCastException e) {
							LOGGER.warn("Error restoring modified list entry: " + e.getMessage());
						}
					});
					return modify(list, mod);
				}
				if (move != null) {
					Map<Integer, Integer> m = new Int2ObjectArrayMap<>(move.size());
					move.forEach((i, j) -> {
						list.move(i, j);
						list.markRestoredCell(j, false, false);
						m.put(j, i);
					});
					return move(list, m);
				}
				return EMPTY;
			}
			
			@Override public void flatten(IEntryHolder holder) {
				AbstractConfigEntry<?> entry = holder.getEntry(listEntry);
				if (entry instanceof BaseListEntry<?, ?, ?>) {
					//noinspection unchecked
					BaseListEntry<Object, ?, ?> list = (BaseListEntry<Object, ?, ?>) entry;
					if (modify != null) {
						final List<BaseListCell<Object>> cells = list.getCells();
						final List<?> value = list.getValue();
						List<Integer> removed = new ArrayList<>();
						modify.forEach((i, v) -> {
							if (i < 0 || i >= cells.size()
							    || cells.get(i).areEqual(value.get(i), v)
							) removed.add(i);
						});
						removed.forEach(modify::remove);
						if (modify.isEmpty()) modify = null;
					}
				} else LOGGER.warn("Expected list entry at path \"" + listEntry + "\"");
			}
			
			@Override public boolean peek(AbstractConfigScreen holder) {
				AbstractConfigEntry<?> entry = holder.getEntry(listEntry);
				if (entry instanceof BaseListEntry<?, ?, ?>) {
					//noinspection unchecked
					BaseListEntry<Object, ?, ?> list = (BaseListEntry<Object, ?, ?>) entry;
					if (modify != null) {
						final List<BaseListCell<Object>> cells = list.getCells();
						final List<?> value = list.getValue();
						for (Entry<Integer, Object> e : modify.entrySet()) {
							int i = e.getKey();
							if (i < 0 || i >= cells.size()) continue;
							if (!cells.get(i).areEqual(value.get(i), e.getValue()))
								return true;
						}
					}
				} else LOGGER.warn("Expected list entry at path \"" + listEntry + "\"");
				return false;
			}
			
			@Override protected void merge(EditRecord record) {
				throw new UnsupportedOperationException("Use a regular EditRecord for merging");
			}
			
			@Override public int size() {
				return (remove != null? remove.size() : 0)
				  + (modify != null? modify.size() : 0)
				  + (add != null? add.size() : 0)
				  + (move != null? move.size() : 0);
			}
			
			public static ListEditRecord remove(BaseListEntry<?, ?, ?> list, List<Integer> indexes) {
				return new ListEditRecord(list.getPath(), indexes, null, null, null);
			}
			
			public static ListEditRecord modify(BaseListEntry<?, ?, ?> list, Map<Integer, Object> values) {
				return new ListEditRecord(list.getPath(), null, values, null, null);
			}
			
			public static ListEditRecord add(BaseListEntry<?, ?, ?> list, Map<Integer, Object> add) {
				return new ListEditRecord(list.getPath(), null, null, add, null);
			}
			
			public static ListEditRecord move(BaseListEntry<?, ?, ?> list, Map<Integer, Integer> moves) {
				return new ListEditRecord(list.getPath(), null, null, null, moves);
			}
		}
	}
}
