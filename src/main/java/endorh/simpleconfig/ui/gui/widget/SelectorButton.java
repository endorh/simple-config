package endorh.simpleconfig.ui.gui.widget;

import endorh.simpleconfig.api.SimpleConfigTextUtil;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simpleconfig.ui.icon.Icon;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class SelectorButton<T> extends MultiFunctionIconButton {
	private Consumer<T> action;
	private List<T> values;
	private int selectedIndex = 0;
	private Function<T, ITextComponent> formatter;
	private @Nullable Function<T, Icon> iconProvider;
	
	public static <T extends Enum<?>> SelectorButton<T> of(
	  Class<T> enumClass, Function<T, ITextComponent> formatter, Consumer<T> action
	) {
		return of(enumClass, formatter, null, action);
	}
	
	public static <T extends Enum<?>> SelectorButton<T> of(
	  Class<T> enumClass, Function<T, ITextComponent> formatter,
	  @Nullable Function<T, Icon> iconProvider, Consumer<T> action
	) {
		return of(Arrays.asList(enumClass.getEnumConstants()), formatter, iconProvider, action);
	}
	
	public static <T> SelectorButton<T> of(
	  Collection<T> values, Function<T, ITextComponent> formatter, Consumer<T> action
	) {
		return of(values, formatter, null, action);
	}
	
	public static <T> SelectorButton<T> of(
	  Collection<T> values, Function<T, ITextComponent> formatter,
	  @Nullable Function<T, Icon> iconProvider, Consumer<T> action
	) {
		return new SelectorButton<>(new ArrayList<>(values), formatter, iconProvider, action);
	}
	
	public SelectorButton(
	  List<T> values, Function<T, ITextComponent> formatter,
	  @Nullable Function<T, Icon> iconProvider, Consumer<T> action
	) {
		super(0, 0, 80, 80, Icon.EMPTY, ButtonAction.of(() -> {}));
		this.action = action;
		if (values.isEmpty()) throw new IllegalArgumentException("List of selectable values cannot be empty");
		this.values = values;
		this.formatter = formatter;
		this.iconProvider = iconProvider;
		actions.clear();
		on(Modifier.NONE, ButtonAction.of(this::selectNext, this::selectPrev));
		on(Modifier.SHIFT, ButtonAction.of(this::selectPrev, this::selectNext));
	}
	
	public void selectPrev() {
		selectNext(false);
	}
	public void selectNext() {
		selectNext(true);
	}
	public void selectNext(boolean forwards) {
		int size = values.size();
		selectedIndex = (selectedIndex + size + (forwards ? 1 : -1)) % size;
		action.accept(getValue());
	}
	
	@Override public Icon getDefaultIcon() {
		return iconProvider != null? iconProvider.apply(getValue()) : Icon.EMPTY;
	}
	
	@Override public ITextComponent getTitle() {
		return formatter.apply(getValue());
	}
	
	public T getValue() {
		return values.get(selectedIndex);
	}
	public void setValue(T value) {
		int i = values.indexOf(value);
		if (i < 0) throw new IllegalArgumentException("Set value is not selectable");
		selectedIndex = i;
		action.accept(getValue());
	}
	
	public int getSelectedIndex() {
		return selectedIndex;
	}
	public void setSelectedIndex(int idx) {
		if (idx < 0 || idx >= values.size()) throw new IndexOutOfBoundsException(
		  "Selected index is out of bounds");
		selectedIndex = idx;
		action.accept(getValue());
	}
	
	public List<T> getSelectableValues() {
		return values;
	}
	public void setSelectableValues(List<T> values) {
		if (values.isEmpty()) throw new IllegalArgumentException(
		  "List of selectable values cannot be empty");
		this.values = values;
		if (selectedIndex >= values.size()) selectedIndex = 0;
	}
	
	public Consumer<T> getAction() {
		return action;
	}
	public void setAction(Consumer<T> action) {
		this.action = action;
	}
	
	public Function<T, ITextComponent> getFormatter() {
		return formatter;
	}
	public void setFormatter(Function<T, ITextComponent> formatter) {
		this.formatter = formatter;
	}
	public @Nullable Function<T, Icon> getIconProvider() {
		return iconProvider;
	}
	public void setIconProvider(@Nullable Function<T, Icon> iconProvider) {
		this.iconProvider = iconProvider;
	}
	
	public static class BooleanButton extends SelectorButton<Boolean> {
		private static final List<Boolean> VALUES = Arrays.asList(false, true);
		
		public static BooleanButton of(BooleanConsumer action) {
			return new BooleanButton(b -> new TranslationTextComponent(
			  "simpleconfig.format.bool.yes_no." + b.toString()
			), null, action);
		}
		
		public static BooleanButton of(
		  Icon trueIcon, Icon falseIcon, BooleanConsumer action
		) {
			return new BooleanButton(b -> new TranslationTextComponent(
			  "simpleconfig.format.bool.yes_no." + b.toString()
			), b -> b? trueIcon : falseIcon, action);
		}
		
		public static BooleanButton of(
		  TextFormatting trueStyle, TextFormatting falseStyle, Icon trueIcon, Icon falseIcon,
		  BooleanConsumer action
		) {
			return of(
			  Style.EMPTY.applyFormat(trueStyle), Style.EMPTY.applyFormat(falseStyle),
			  trueIcon, falseIcon, action);
		}
		
		public static BooleanButton of(
		  Style trueStyle, Style falseStyle, Icon trueIcon, Icon falseIcon, BooleanConsumer action
		) {
			return of(
			  b -> new StringTextComponent(SimpleConfigTextUtil.stripFormattingCodes(I18n.get(
				 "simpleconfig.format.bool.yes_no." + b.toString()
			  ))).withStyle(b? trueStyle : falseStyle),
			  trueIcon, falseIcon, action);
		}
		
		public static BooleanButton of(
		  Function<Boolean, ITextComponent> formatter, Icon trueIcon, Icon falseIcon, BooleanConsumer action
		) {
			return new BooleanButton(formatter, b -> b? trueIcon : falseIcon, action);
		}
		
		public BooleanButton(
		  Function<Boolean, ITextComponent> formatter, @Nullable Function<Boolean, Icon> iconProvider,
		  BooleanConsumer action
		) {
			super(VALUES, formatter, iconProvider, b -> action.accept((boolean) b));
		}
		
		public boolean getToggle() {
			return getValue();
		}
		public void setToggle(boolean toggle) {
			setValue(toggle);
		}
	}
}
