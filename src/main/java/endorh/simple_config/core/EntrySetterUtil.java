package endorh.simple_config.core;

import endorh.simple_config.gui.DoubleSliderEntry;
import endorh.simple_config.gui.FloatSliderEntry;
import endorh.simple_config.gui.NestedListEntry;
import endorh.simple_config.gui.StringPairListEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ModifierKeyCode;
import me.shedaniel.clothconfig2.gui.entries.*;
import me.shedaniel.clothconfig2.gui.entries.AbstractTextFieldListListEntry.AbstractTextFieldListCell;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static endorh.simple_config.core.ReflectionUtil.getFieldOrNull;
import static endorh.simple_config.core.ReflectionUtil.getFieldValueOrNull;

public class EntrySetterUtil {
	protected static final Field
	  BooleanListEntry$bool = getFieldOrNull(BooleanListEntry.class, "bool");
	protected static Field SelectionListEntry$index =
	  getFieldOrNull(SelectionListEntry.class, "index");
	protected static Field BaseListEntry$cells =
	  getFieldOrNull(BaseListEntry.class, "cells");
	protected static Field BaseListEntry$widgets =
	  getFieldOrNull(BooleanListEntry.class, "widgets");
	protected static Field BaseListEntry$createNewInstance =
	  getFieldOrNull(BaseListEntry.class, "createNewInstance");
	@SuppressWarnings("UnstableApiUsage") protected static Field AbstractTextFieldListCell$widget =
	  getFieldOrNull(AbstractTextFieldListCell.class, "widget");
	
	public static void setValue(AbstractConfigListEntry<?> entry, Object value) {
		setValue(entry, value, String::valueOf);
	}
	public static void setValue(
	  AbstractConfigListEntry<?> entry, Object value, Function<Object, String> serializer
	) {
		try {
			if (entry instanceof StringPairListEntry) {
				//noinspection unchecked
				((StringPairListEntry<Object, ?>) entry).setValue((List<Pair<String, Object>>) value);
			} else if (entry instanceof BaseListEntry) {
				//noinspection unchecked
				setListValue((BaseListEntry<Object, ?, ?>) entry, (List<Object>) value, serializer);
			} else {
				//noinspection unchecked
				setValueSingle(((AbstractConfigListEntry<Object>) entry), value, serializer);
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation") private static <T> void setValueSingle(
	  AbstractConfigListEntry<T> entry, T value, Function<T, String> serializer
	) {
		if (entry instanceof BooleanListEntry) {
			final AtomicBoolean v = getFieldValueOrNull(entry, BooleanListEntry$bool);
			if (v != null) v.set((Boolean) value);
		} else if (entry instanceof ColorEntry) {
			((ColorEntry) entry).setValue((Integer) value);
		} else if (entry instanceof TextFieldListEntry) {
			((TextFieldListEntry<T>) entry).setValue(serializer.apply(value));
		} else if (entry instanceof IntegerSliderEntry) {
			((IntegerSliderEntry) entry).setValue((Integer) value);
		} else if (entry instanceof LongSliderEntry) {
			((LongSliderEntry) entry).setValue((Long) value);
		} else if (entry instanceof FloatSliderEntry) {
			((FloatSliderEntry) entry).setValue((Float) value);
		} else if (entry instanceof DoubleSliderEntry) {
			((DoubleSliderEntry) entry).setValue((Double) value);
		} else if (entry instanceof SelectionListEntry && value instanceof Enum) {
			final AtomicInteger index = getFieldValueOrNull(entry, SelectionListEntry$index);
			index.set(((Enum<?>) value).ordinal());
		} else if (entry instanceof DropdownBoxEntry) {
			((DropdownBoxEntry<T>) entry).getSelectionElement().getTopRenderer().setValue(value);
		} else if (entry instanceof BaseListEntry) {
			throw new IllegalStateException("Cannot set non-list value to list entry.");
		} else if (entry instanceof KeyCodeEntry) {
			((KeyCodeEntry) entry).setValue((ModifierKeyCode) value);
		}
	}
	
	@SuppressWarnings("UnstableApiUsage") private static <T> void setListValue(
	  BaseListEntry<T, ?, ?> entry, List<T> value, Function<T, String> serializer
	) {
		if (entry instanceof NestedListEntry) {
			//noinspection unchecked
			((NestedListEntry<T, ?>) entry).setValue(value);
		} else if (entry instanceof StringPairListEntry) {
			throw new IllegalStateException("Cannot set non-pair-list value to pair list entry");
		} else if (entry instanceof AbstractTextFieldListListEntry) {
			final List<AbstractTextFieldListCell<T, ?, ?>> cells =
			  getFieldValueOrNull(entry, BaseListEntry$cells);
			final List<IGuiEventListener> widgets = getFieldValueOrNull(entry, BaseListEntry$widgets);
			for (int i = 0; i < cells.size() && i < value.size(); i++) {
				final TextFieldWidget w = getFieldValueOrNull(cells.get(i), AbstractTextFieldListCell$widget);
				if (w != null) w.setText(serializer.apply(value.get(i)));
			}
			while (cells.size() > value.size()) {
				cells.get(value.size()).onDelete();
				cells.remove(value.size());
				widgets.remove(value.size());
			}
			final Function<AbstractTextFieldListListEntry<T, ?, ?>, AbstractTextFieldListCell<T, ?, ?>> createCell =
			  getFieldValueOrNull(entry, BaseListEntry$createNewInstance);
			if (createCell == null)
				return;
			while (cells.size() < value.size()) {
				//noinspection unchecked
				AbstractTextFieldListCell<T, ?, ?> cell = createCell.apply(
				  ((AbstractTextFieldListListEntry<T, ?, ?>) entry).self());
				cells.add(cell);
				widgets.add(cell);
				cell.onAdd();
				final TextFieldWidget w = getFieldValueOrNull(cell, AbstractTextFieldListCell$widget);
				if (w != null) w.setText(serializer.apply(value.get(cells.size() - 1)));
			}
		}
	}
}
