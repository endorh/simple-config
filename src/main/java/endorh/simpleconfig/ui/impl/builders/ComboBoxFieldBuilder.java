package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.ComboBoxListEntry;
import endorh.simpleconfig.ui.gui.widget.combobox.IComboBoxModel;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ComboBoxFieldBuilder<T> extends FieldBuilder<T, ComboBoxListEntry<T>, ComboBoxFieldBuilder<T>> {
	
	protected final TypeWrapper<T> typeWrapper;
	protected List<T> suggestions = null;
	protected IComboBoxModel<T> suggestionProvider;
	protected boolean suggestionMode = true;
	protected int maxLength = Integer.MAX_VALUE;
	
	public ComboBoxFieldBuilder(
	  ConfigFieldBuilder builder, Component name, T value, TypeWrapper<T> typeWrapper
	) {
		super(ComboBoxListEntry.class, builder, name, value);
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
	  IComboBoxModel<T> suggestionProvider
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
	
	public static TypeWrapper<String> ofString() {
		return new StringTypeWrapper();
	}
	
	public static TypeWrapper<ResourceLocation> ofResourceLocation() {
		return new ResourceLocationTypeWrapper();
	}
	
	public static TypeWrapper<Item> ofItem() {
		return new ItemTypeWrapper();
	}
	
	public static TypeWrapper<ResourceLocation> ofItemName() {
		return new ItemNameTypeWrapper();
	}
	
	public static TypeWrapper<Block> ofBlock() {
		return new BlockTypeWrapper();
	}
	
	public static TypeWrapper<ResourceLocation> ofBlockName() {
		return new BlockNameTypeWrapper();
	}
	
	public static TypeWrapper<Fluid> ofFluid() {
		return new FluidTypeWrapper();
	}
	
	public static TypeWrapper<ResourceLocation> ofFluidName() {
		return new FluidNameTypeWrapper();
	}
}
