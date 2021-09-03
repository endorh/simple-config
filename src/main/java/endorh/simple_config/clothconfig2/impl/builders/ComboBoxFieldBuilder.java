package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.ComboBoxListEntry;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.*;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class ComboBoxFieldBuilder<T> extends FieldBuilder<T, ComboBoxListEntry<T>, ComboBoxFieldBuilder<T>> {
	
	protected final ITypeWrapper<T> typeWrapper;
	protected List<T> suggestions = null;
	protected ISortedSuggestionProvider<T> suggestionProvider;
	protected boolean suggestionMode = true;
	protected int maxLength = Integer.MAX_VALUE;
	
	public ComboBoxFieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, T value, ITypeWrapper<T> typeWrapper
	) {
		super(builder, name, value);
		this.typeWrapper = typeWrapper;
	}
	
	/**
	 * Set a list of suggestions to be offered.<br>
	 * These are ignored if a suggestion provider is set.
	 */
	public ComboBoxFieldBuilder<T> setSuggestions(List<T> suggestions) {
		this.suggestions = suggestions;
		return self();
	}
	
	/**
	 * Set a suggestion provider, which may provide suggestions
	 * more efficiently.<br>
	 * Setting a suggestion provider effectively ignores suggestions set
	 * with {@link ComboBoxFieldBuilder#setSuggestions}
	 */
	public ComboBoxFieldBuilder<T> setSuggestionProvider(
	  ISortedSuggestionProvider<T> suggestionProvider
	) {
		this.suggestionProvider = suggestionProvider;
		return self();
	}
	
	public ComboBoxFieldBuilder<T> setSuggestionMode(boolean suggestionMode) {
		this.suggestionMode = suggestionMode;
		return self();
	}
	
	public ComboBoxFieldBuilder<T> setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		return self();
	}
	
	@Override protected ComboBoxListEntry<T> buildEntry() {
		final ComboBoxListEntry<T> entry =
		  new ComboBoxListEntry<>(fieldNameKey, value, typeWrapper, suggestions);
		entry.setSuggestionMode(suggestionMode);
		entry.setMaxLength(maxLength);
		if (suggestionProvider != null)
			entry.setSuggestionProvider(suggestionProvider);
		return entry;
	}
	
	public static ITypeWrapper<String> ofString() {
		return new StringTypeWrapper();
	}
	
	public static ITypeWrapper<ResourceLocation> ofResourceLocation() {
		return new ResourceLocationTypeWrapper();
	}
	
	public static ITypeWrapper<Item> ofItem() {
		return new ItemTypeWrapper();
	}
	
	public static ITypeWrapper<ResourceLocation> ofItemName() {
		return new ItemNameTypeWrapper();
	}
	
	public static ITypeWrapper<Block> ofBlock() {
		return new BlockTypeWrapper();
	}
	
	public static ITypeWrapper<ResourceLocation> ofBlockName() {
		return new BlockNameTypeWrapper();
	}
	
	public static ITypeWrapper<Fluid> ofFluid() {
		return new FluidTypeWrapper();
	}
	
	public static ITypeWrapper<ResourceLocation> ofFluidName() {
		return new FluidNameTypeWrapper();
	}
}
