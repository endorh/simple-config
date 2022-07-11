package endorh.simpleconfig.core;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.EntryFlag;
import endorh.simpleconfig.ui.impl.builders.EntryListFieldBuilder;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import endorh.simpleconfig.core.entry.AbstractListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Special config entry containing a list of values managed
 * by another entry<br>
 * @param <V> The type of the elements of the list
 * @param <C> The type of the elements of the list facing the config
 * @param <G> The type of the elements of the list facing the GUI
 * @param <E> The type of the entry nested within the list
 */
public class EntryListEntry
  <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
    B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
  extends AbstractListEntry<V, C, G, EntryListEntry<V, C, G, E, B>> {
	protected static final String TOOLTIP_KEY_SUFFIX = ":help";
	protected static final String SUB_ELEMENTS_KEY_SUFFIX = ":sub";
	
	protected final AbstractConfigEntry<V, C, G, E> entry;
	protected final B entryBuilder;
	protected FakeEntryHolder holder;
	
	@Internal public EntryListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<V> value, B entryBuilder) {
		super(parent, name, value);
		holder = new FakeEntryHolder(parent.getRoot());
		this.entryBuilder = entryBuilder;
		entry = entryBuilder.build(holder, name).withSaver((g, c) -> {});
		if (!entry.canBeNested())
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a list entry");
		if (translation != null)
			setTranslation(translation);
		if (tooltip != null)
			setTooltip(tooltip);
	}
	
	public static class Builder<V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
	  extends AbstractListEntry.Builder<V, C, G, EntryListEntry<V, C, G, E, B>, Builder<V, C, G, E, B>> {
		protected B builder;
		
		public Builder(List<V> value, B builder) {
			super(new ArrayList<>(value), builder.typeClass);
			this.builder = builder.copy();
		}
		@Override protected EntryListEntry<V, C, G, E, B> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			return new EntryListEntry<>(parent, name, value, builder);
		}
		
		@Override protected Builder<V, C, G, E, B> createCopy() {
			return new Builder<>(value, builder);
		}
	}
	
	@Override
	protected void setTranslation(String translation) {
		super.setTranslation(translation);
		if (translation != null)
			entry.setTranslation(translation + SUB_ELEMENTS_KEY_SUFFIX);
		self();
	}
	
	@Override
	protected void setTooltip(String translation) {
		super.setTooltip(translation);
		if (tooltip != null)
			if (tooltip.endsWith(TOOLTIP_KEY_SUFFIX))
				entry.setTooltip(tooltip.substring(0, tooltip.length() - TOOLTIP_KEY_SUFFIX.length())
				                 + SUB_ELEMENTS_KEY_SUFFIX + TOOLTIP_KEY_SUFFIX);
			else entry.setTooltip(tooltip + SUB_ELEMENTS_KEY_SUFFIX);
		self();
	}
	
	@Override
	protected C elemForConfig(V value) {
		return entry.forConfig(value);
	}
	
	@Override
	protected V elemFromConfig(C value) {
		return entry.fromConfig(value);
	}
	
	@Override
	protected G elemForGui(V value) {
		return entry.forGui(value);
	}
	
	@Override
	protected V elemFromGui(G value) {
		return entry.fromGui(value);
	}
	
	@Override public List<ITextComponent> getElementErrors(G value) {
		List<ITextComponent> errors = super.getElementErrors(value);
		errors.addAll(entry.getErrors(value));
		return errors;
	}
	
	@Override protected boolean validateElement(Object o) {
		try {
			//noinspection unchecked
			return !entry.getError(elemForGui(elemFromConfig((C) o))).isPresent();
		} catch (ClassCastException e) {
			return false;
		}
	}
	
	@OnlyIn(Dist.CLIENT) protected AbstractConfigListEntry<G> buildCell(
	  ConfigEntryBuilder builder
	) {
		final E e = entryBuilder.build(holder, holder.nextName())
		  .withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent("•"));
		e.nonPersistent = true;
		e.actualValue = e.value;
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder)
		  .orElseThrow(() -> new IllegalStateException(
		    "List config entry's sub-entry did not produce a GUI entry"));
		g.removeEntryFlag(EntryFlag.NON_PERSISTENT);
		e.guiEntry = g;
		return g;
	}
	
	@Override protected Consumer<List<G>> createSaveConsumer() {
		return super.createSaveConsumer().andThen(l -> holder.clear());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<G>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		holder.clear();
		final EntryListFieldBuilder<G, AbstractConfigListEntry<G>> valBuilder =
		  builder.startEntryList(getDisplayName(), forGui(get()), en -> buildCell(builder));
		return Optional.of(decorate(valBuilder).build());
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, entry.getExpectedType());
	}
}