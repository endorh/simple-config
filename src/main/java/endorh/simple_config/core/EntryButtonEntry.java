package endorh.simple_config.core;

import endorh.simple_config.core.entry.GUIOnlyEntry;
import endorh.simple_config.gui.EntryButtonListEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class EntryButtonEntry<V, Gui, Inner extends AbstractConfigEntry<V, ?, Gui, Inner>>
  extends GUIOnlyEntry<V, Gui, EntryButtonEntry<V, Gui, Inner>> {
	
	protected Inner inner;
	protected BiConsumer<V, ISimpleConfigEntryHolder> action;
	protected Supplier<ITextComponent> buttonLabelSupplier;
	
	public EntryButtonEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  AbstractConfigEntryBuilder<V, ?, Gui, Inner, ?> innerBuilder,
	  V value, BiConsumer<V, ISimpleConfigEntryHolder> action,
	  Class<?> typeClass
	) {
		super(parent, name, value, false, typeClass);
		this.inner = innerBuilder.build(new DummyEntryHolder<>(parent.getRoot(), innerBuilder), "");
		this.action = action;
	}
	
	public static class Builder<V, Gui, Inner extends AbstractConfigEntry<V, ?, Gui, Inner>>
	  extends GUIOnlyEntry.Builder<V, Gui, EntryButtonEntry<V, Gui, Inner>, Builder<V, Gui, Inner>> {
		
		protected AbstractConfigEntryBuilder<V, ?, Gui, Inner, ?> inner;
		protected BiConsumer<V, ISimpleConfigEntryHolder> action;
		protected Supplier<ITextComponent> buttonLabelSupplier = () -> new StringTextComponent("✓");
		
		public Builder(
		  AbstractConfigEntryBuilder<V, ?, Gui, Inner, ?> inner,
		  BiConsumer<V, ISimpleConfigEntryHolder> action
		) {
			super(inner.value, inner.typeClass);
			this.inner = inner;
			this.action = action;
		}
		
		public Builder<V, Gui, Inner> label(String translation) {
			final TranslationTextComponent ttc = new TranslationTextComponent(translation);
			this.buttonLabelSupplier = () -> ttc;
			return self();
		}
		
		public Builder<V, Gui, Inner> label(ITextComponent label) {
			this.buttonLabelSupplier = () -> label;
			return self();
		}
		
		public Builder<V, Gui, Inner> label(Supplier<ITextComponent> label) {
			this.buttonLabelSupplier = label;
			return self();
		}
		
		@Override protected final EntryButtonEntry<V, Gui, Inner> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final EntryButtonEntry<V, Gui, Inner> entry = new EntryButtonEntry<>(
			  parent, name, inner, value, action, typeClass);
			entry.buttonLabelSupplier = buttonLabelSupplier;
			return entry;
		}
	}
	
	@Override protected Gui forGui(V value) {
		return inner.forGui(value);
	}
	
	@Nullable @Override protected V fromGui(@Nullable Gui value) {
		return inner.fromGui(value);
	}
	
	@Override public Optional<AbstractConfigListEntry<Gui>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final ITextComponent prevKey = builder.getResetButtonKey();
		builder.setResetButtonKey(sameOrLessWidthBlank(prevKey));
		final Optional<AbstractConfigListEntry<Gui>> in = inner.buildGUIEntry(builder);
		builder.setResetButtonKey(prevKey);
		if (!in.isPresent())
			return Optional.empty();
		final EntryButtonListEntry<Gui, AbstractConfigListEntry<Gui>> entry = new EntryButtonListEntry<>(
		  getDisplayName(), in.get(), g -> action.accept(fromGuiOrDefault(g), parent), buttonLabelSupplier,
		  null, () -> this.supplyTooltip(getGUI()), builder.getResetButtonKey());
		return Optional.of(entry);
	}
	
	public static ITextComponent sameOrLessWidthBlank(ITextComponent src) {
		final FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
		final int target = fontRenderer.getStringPropertyWidth(src);
		String str = "";
		int cur = 0;
		while (cur <= target) {
			str += " ";
			cur = fontRenderer.getStringPropertyWidth(new StringTextComponent(str));
		}
		return new StringTextComponent(str.substring(1));
	}
}
