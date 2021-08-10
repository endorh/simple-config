package endorh.simple_config.clothconfig2.impl;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.IEntryHolder;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.entries.BaseListEntry;
import endorh.simple_config.clothconfig2.gui.entries.DecoratedListEntry;
import endorh.simple_config.clothconfig2.gui.entries.EntryPairListListEntry;
import endorh.simple_config.clothconfig2.gui.entries.NestedListListEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.Util;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.minecraft.util.math.MathHelper.clamp;

public class EditHistory {
	private static final Logger LOGGER = LogManager.getLogger();
	protected int maxSize = 100;
	protected List<EditRecord> records;
	protected @Nullable EditRecord preservedState;
	protected @Nullable EditRecord collector;
	protected int cursor = 0;
	protected Runnable onHistory;
	protected Supplier<Boolean> peek;
	
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
	}
	
	public void startBatch(AbstractConfigScreen holder, @Nullable AbstractConfigEntry<?> focus) {
		if (preservedState != null) saveState(holder);
		if (collector != null) saveBatch(holder);
		collector = new EditRecord(focus != null? focus.getPath() : null, new HashMap<>(), null);
	}
	
	public void saveBatch(AbstractConfigScreen holder) {
		saveState(holder);
		if (collector != null) {
			final EditRecord c = this.collector;
			collector = null;
			c.flatten(holder);
			addRecord(c);
		}
	}
	
	public void preserveState(AbstractConfigEntry<?> entry) {
		if (preservedState != null)
			saveState(entry.getConfigScreen());
		preservedState = EditRecord.of(entry);
	}
	
	public void discardPreservedState() {
		preservedState = null;
	}
	
	public void saveState(AbstractConfigScreen screen) {
		if (preservedState != null) {
			preservedState.flatten(screen);
			if (preservedState.size() > 0)
				addRecord(preservedState);
			preservedState = null;
		}
	}
	
	public void addRecord(EditRecord record) {
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
	
	public void apply(AbstractConfigScreen holder, boolean forward) {
		if (preservedState != null) saveState(holder);
		if (onHistory != null) onHistory.run();
		if (collector != null) saveBatch(holder);
		if (forward && cursor >= records.size() || !forward && cursor <= 0) return;
		if (!forward) cursor--;
		final EditRecord record = records.get(cursor);
		final EditRecord replacement = record.apply(holder);
		records.set(cursor, replacement);
		if (forward) cursor++;
	}
	
	public boolean canUndo(AbstractConfigScreen screen) {
		return cursor > 0 || preservedState != null && preservedState.peek(screen)
		       || peek != null && peek.get();
	}
	
	public boolean canRedo(AbstractConfigScreen screen) {
		return cursor < records.size() && (preservedState == null || !preservedState.peek(screen))
		       && (peek == null || !peek.get());
	}
	
	public int getCursor() {
		cursor = clamp(cursor, 0, records.size());
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
			  holder instanceof AbstractConfigEntry ? ((AbstractConfigEntry<?>) holder).getPath() : null,
			  holder.getAllEntries().stream().collect(Collectors.toMap(AbstractConfigEntry::getPath, e -> e, (a, b) -> b)),
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
					entry.restoreValue(v);
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
		
		public void flatten(AbstractConfigScreen holder) {
			final Set<String> removed = new HashSet<>();
			for (Entry<String, Object> e : values.entrySet()) {
				final AbstractConfigEntry<?> entry = holder.getEntry(e.getKey());
				if (entry == null || Objects.equals(entry.getValue(), e.getValue()))
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
				if (entry != null && !Objects.equals(entry.getValue(), e.getValue()))
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
			  null, null, null, null, null, null, null, null);
			protected String listEntry;
			protected List<Object> value;
			protected List<Integer> remove;
			protected Map<Integer, Object> modify;
			protected Map<Integer, Object> add;
			protected Map<Integer, Integer> move;
			protected Map<Integer, ListEditRecord> sub;
			protected Object caption;
			
			protected ListEditRecord(
			  String listEntry, List<Object> value, List<Integer> remove,
			  Map<Integer, Object> modify, Map<Integer, Object> add,
			  Map<Integer, Integer> move, Map<Integer, ListEditRecord> sub,
			  Object caption
			) {
				super(null, Collections.emptyMap(), null);
				this.listEntry = listEntry;
				this.value = value;
				this.remove = remove;
				this.modify = modify;
				this.add = add;
				this.move = move;
				this.sub = sub;
				this.caption = caption;
			}
			
			public ListEditRecord apply(IEntryHolder holder) {
				return apply(holder, true);
			}
			
			public ListEditRecord apply(IEntryHolder holder, boolean claimFocus) {
				if (listEntry == null) return EMPTY;
				AbstractConfigEntry<?> entry = holder.getEntry(listEntry);
				if (entry instanceof DecoratedListEntry)
					entry = ((DecoratedListEntry<?, ?, ?, ?>) entry).getListEntry();
				if (!(entry instanceof BaseListEntry)) {
					LOGGER.warn("Expected a list entry at path " + listEntry);
					return EMPTY;
				}
				if (claimFocus) entry.claimFocus();
				return apply((BaseListEntry<?, ?, ?>) entry);
			}
			
			public ListEditRecord apply(BaseListEntry<?, ?, ?> list) {
				if (value != null) {
					final ListEditRecord r = full(list);
					list.restoreValue(value);
					return r;
				}
				if (remove != null) {
					//noinspection unchecked
					final List<Object> value = (List<Object>) list.getValue();
					Map<Integer, Object> add = new Int2ObjectArrayMap<>(remove.size());
					list.setSuppressRecords(true);
					for (int idx : remove) {
						add.put(idx, value.get(idx));
						list.remove(idx);
						list.markRestoredCell(idx, true);
						value.remove(idx);
					}
					list.setSuppressRecords(false);
					return add(list, add);
				}
				if (add != null) {
					List<Integer> remove = new IntArrayList(add.size());
					//noinspection unchecked
					final BaseListEntry<Object, ?, ?> l = (BaseListEntry<Object, ?, ?>) list;
					list.setSuppressRecords(true);
					add.forEach((i, v) -> {
						try {
							l.add(i, v);
							l.markRestoredCell(i, false);
							remove.add(i);
						} catch (ClassCastException e) {
							LOGGER.warn("Error restoring removed list entry: " + e.getMessage());
						}
					});
					list.setSuppressRecords(false);
					return remove(list, remove);
				}
				if (modify != null) {
					final List<?> value = list.getValue();
					Map<Integer, Object> mod = new Int2ObjectArrayMap<>(modify.size());
					//noinspection unchecked
					final BaseListEntry<Object, ?, ?> l = (BaseListEntry<Object, ?, ?>) list;
					list.setSuppressRecords(true);
					modify.forEach((i, v) -> {
						try {
							final Object p = value.get(i);
							l.set(i, v);
							l.markRestoredCell(i, false);
							mod.put(i, p);
						} catch (ClassCastException e) {
							LOGGER.warn("Error restoring modified list entry: " + e.getMessage());
						}
					});
					list.setSuppressRecords(false);
					return modify(list, mod);
				}
				if (move != null) {
					Map<Integer, Integer> m = new Int2ObjectArrayMap<>(move.size());
					List<Pair<Integer, Integer>> l = new ArrayList<>();
					list.setSuppressRecords(true);
					move.forEach((i, j) -> {
						list.move(i, j);
						list.markRestoredCell(j, false);
						l.add(Pair.of(j, i));
					});
					l.forEach(p -> m.put(p.getKey(), p.getValue()));
					list.setSuppressRecords(false);
					return move(list, m);
				}
				if (sub != null) {
					List<AbstractConfigEntry<?>> subLists = null;
					try {
						if (list instanceof NestedListListEntry) {
							//noinspection unchecked
							subLists = (List<AbstractConfigEntry<?>>) ((NestedListListEntry<?, ?>) list).getEntries();
						} else if (list instanceof EntryPairListListEntry) {
							subLists =
							  ((EntryPairListListEntry<?, ?, ?, ?>) list).getEntries().stream().map(
								 p -> ((AbstractConfigEntry<?>) p.getValue())).collect(Collectors.toList());
						}
					} catch (ClassCastException e) {
						LOGGER.warn("Could not get sub list entries: " + e.getMessage());
						return EMPTY;
					}
					assert subLists != null;
					final List<BaseListEntry<?, ?, ?>> s = subLists.stream().map(
					  e -> e instanceof DecoratedListEntry
					       ? ((DecoratedListEntry<?, ?, ?, ?>) e).getListEntry()
					       : e instanceof BaseListEntry ? (BaseListEntry<?, ?, ?>) e : null
					).collect(Collectors.toList());
					Map<Integer, ListEditRecord> ret = new Int2ObjectArrayMap<>(sub.size());
					sub.forEach((i, r) -> {
						final BaseListEntry<?, ?, ?> sl = s.get(i);
						if (sl != null) ret.put(i, r.apply(sl));
						else LOGGER.warn("Expected list sub entry");
					});
					return sub(list, ret);
				}
				final AbstractConfigListEntry<?> entry = list.getHeldEntry();
				if (entry != null) {
					final Object prev = entry.getValue();
					entry.restoreValue(caption);
					return caption(list, prev);
				} else LOGGER.warn("Missing list caption entry");
				return EMPTY;
			}
			
			@Override public void flatten(AbstractConfigScreen holder) {
				if (value != null) {
					final AbstractConfigEntry<?> entry = holder.getEntry(listEntry);
					if (entry == null || Objects.equals(entry.getValue(), value))
						value = null;
				}
			}
			
			@Override protected void merge(EditRecord record) {
				throw new UnsupportedOperationException("Use a regular EditRecord for merging");
			}
			
			@Override public int size() {
				return value != null || remove != null || modify != null || add != null
				       || move != null || sub != null || caption != null? 1 : 0;
			}
			
			public static ListEditRecord full(BaseListEntry<?, ?, ?> list) {
				//noinspection unchecked
				return new ListEditRecord(list.getPath(), (List<Object>) list.getValue(), null, null, null, null,
				                          null, null);
			}
			
			public static ListEditRecord remove(BaseListEntry<?, ?, ?> list, List<Integer> indexes) {
				return new ListEditRecord(list.getPath(), null, indexes, null, null, null, null, null);
			}
			
			public static ListEditRecord modify(BaseListEntry<?, ?, ?> list, Map<Integer, Object> values) {
				return new ListEditRecord(list.getPath(), null, null, values, null, null, null, null);
			}
			
			public static ListEditRecord add(BaseListEntry<?, ?, ?> list, Map<Integer, Object> add) {
				return new ListEditRecord(list.getPath(), null, null, null, add, null, null, null);
			}
			
			public static ListEditRecord move(BaseListEntry<?, ?, ?> list, Map<Integer, Integer> moves) {
				return new ListEditRecord(list.getPath(), null, null, null, null, moves, null, null);
			}
			
			public static ListEditRecord sub(BaseListEntry<?, ?, ?> list, Map<Integer, ListEditRecord> records) {
				return new ListEditRecord(list.getPath(), null, null, null, null, null, records, null);
			}
			
			public static ListEditRecord caption(BaseListEntry<?, ?, ?> list, Object caption) {
				return new ListEditRecord(list.getPath(), null, null, null, null, null, null, caption);
			}
		}
	}
}
