package endorh.simpleconfig.ui.api;

import com.google.common.collect.Lists;
import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.INavigableTarget;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.entries.BaseListEntry;
import endorh.simpleconfig.ui.gui.widget.DynamicElementListWidget;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractConfigEntry<T> extends DynamicElementListWidget.ElementEntry {
	private static final Logger LOGGER = LogManager.getLogger();
	
	private @Nullable AbstractConfigScreen screen = null;
	private @Nullable DynamicEntryListWidget<?> entryList = null;
	private @Nullable AbstractConfigEntry<?> parentEntry = null;
	
	private T value;
	@Nullable private T original = null;
	@Nullable private T external = null;
	
	@NotNull private Supplier<T> defaultSupplier = () -> null;
	@Nullable private Supplier<Optional<ITextComponent>> errorSupplier = null;
	@Nullable private Consumer<T> saveConsumer = null;
	
	private boolean isSubEntry = false;
	private boolean isChildSubEntry = false;
	private boolean ignoreEdits = false;
	private boolean editable = true;
	private boolean previewingExternal = false;
	private boolean requiresRestart;
	private Supplier<Boolean> editableSupplier;
	
	private String category = null;
	private String name = "";
	private ITextComponent title;
	
	private final NavigableSet<EntryFlag> entryFlags = new TreeSet<>();
	
	protected String matchedText = null;
	protected String matchedValueText = null;
	private boolean focusedMatch = false;
	private boolean isFocused = false;
	private boolean isSelected = false;
	
	protected long lastFocusHighlightTime;
	protected int focusHighlightLength;
	protected int focusHighlightColor;
	
	protected int matchColor = 0x42FFFF42;
	protected int focusedMatchColor = 0x80ffBD42;
	protected int selectionColor = 0x408090F0;
	
	protected ResetButton resetButton;
	
	private @Nullable INavigableTarget navigableParent = null;
	
	public AbstractConfigEntry(ITextComponent title) {
		this.title = title;
		this.resetButton = new ResetButton(this);
	}
	
	public String getPath() {
		if (parentEntry != null) return parentEntry.providePath(this);
		return getCategory() + "." + getName();
	}
	
	public String providePath(AbstractConfigEntry<?> child) {
		return getPath() + "." + child.getName();
	}
	
	public String getCategory() {
		if (category != null) return category;
		if (parentEntry != null) return parentEntry.getCategory();
		return "";
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setCategory(ConfigCategory category) {
		this.category = category.getName();
	}
	
	/**
	 * A sub-entry is used by another entry as a GUI widget, so its non-gui state
	 * (value/original value/external value/entry flags/path/save consumer/...) may not matter.
	 */
	public boolean isSubEntry() {
		return isSubEntry || parentEntry != null && parentEntry.isSubEntry();
	}
	public void setSubEntry(boolean isSubEntry) {
		this.isSubEntry = isSubEntry;
		if (!isSubEntry) setChildSubEntry(false);
	}
	
	public boolean isChildSubEntry() {
		return isChildSubEntry;
	}
	
	public void setChildSubEntry(boolean childSubEntry) {
		isChildSubEntry = childSubEntry;
		if (childSubEntry) setSubEntry(true);
	}
	
	public boolean isRequiresRestart() {
		return requiresRestart;
	}
	public void setRequiresRestart(boolean requiresRestart) {
		this.requiresRestart = requiresRestart;
	}
	
	public NavigableSet<EntryFlag> getEntryFlags() {
		return entryFlags;
	}
	public void addEntryFlag(EntryFlag flag) {
		getEntryFlags().add(flag);
	}
	public void removeEntryFlag(EntryFlag flag) {
		getEntryFlags().remove(flag);
	}
	
	public boolean isFocused() {
		return isFocused;
	}
	public void setFocused(boolean focused) {
		this.isFocused = focused;
	}
	
	public boolean isSelectable() {
		return getScreen().canSelectEntries() && !isSubEntry();
	}
	
	public boolean isSelected() {
		return isSelectable() && isSelected;
	}
	
	public void setSelected(boolean isSelected) {
		if (!isSelectable()) return;
		this.isSelected = isSelected;
		getScreen().updateSelection();
	}
	
	public boolean shouldShowChildren() {
		AbstractConfigEntry<?> parent = getParentEntry();
		return !isSubEntry() && matchesSearch() || parent != null && parent.shouldShowChildren();
	}
	
	public @Nullable AbstractConfigEntry<?> getParentEntry() {
		return parentEntry;
	}
	
	public void setParentEntry(@Nullable AbstractConfigEntry<?> parentEntry) {
		this.parentEntry = parentEntry;
	}
	
	public @Nullable BaseListEntry<?, ?, ?> getListParent() {
		return parentEntry instanceof BaseListEntry<?, ?, ?>
		       ? ((BaseListEntry<?, ?, ?>) parentEntry)
		       : parentEntry != null? parentEntry.getListParent() : null;
	}
	
	public @Nullable IExpandable getExpandableParent() {
		return parentEntry instanceof IExpandable ? ((IExpandable) parentEntry) :
		       parentEntry != null? parentEntry.getExpandableParent() : null;
	}
	
	@Internal protected void doExpandParents(AbstractConfigEntry<?> entry) {
		final IExpandable parent = getExpandableParent();
		if (this instanceof IExpandable)
			((IExpandable) this).setExpanded(true);
		if (parent instanceof AbstractConfigEntry) {
			((AbstractConfigEntry<?>) parent).doExpandParents(entry);
		} else if (parent != null) parent.setExpanded(true);
	}
	
	public void expandParents() {
		final IExpandable parent = getExpandableParent();
		if (parent != null) {
			if (parent instanceof AbstractConfigEntry)
				((AbstractConfigEntry<?>) parent).doExpandParents(this);
			else parent.setExpanded(true);
		}
	}
	
	public ITextComponent getTitle() {
		return title;
	}
	public void setTitle(ITextComponent title) {
		this.title = title;
	}
	
	public ITextComponent getDisplayedTitle() {
		boolean hasError = this.hasError();
		boolean isEdited = this.isEdited();
		IFormattableTextComponent text = this.getTitle().deepCopy();
		if (matchedText != null && !matchedText.isEmpty()) {
			final String title = getUnformattedString(getTitle());
			final int i = title.indexOf(matchedText);
			if (i != -1) {
				text = new StringTextComponent(title.substring(0, i))
				  .append(new StringTextComponent(title.substring(i, i + matchedText.length()))
				            .mergeStyle(isFocusedMatch()? TextFormatting.GOLD : TextFormatting.YELLOW)
				            // .mergeStyle(TextFormatting.BOLD)
				            .mergeStyle(TextFormatting.UNDERLINE))
				  .appendString(title.substring(i + matchedText.length()));
			}
		}
		if (hasError)
			text = text.mergeStyle(TextFormatting.RED);
		if (isEdited)
			text = text.mergeStyle(TextFormatting.ITALIC);
		if (!hasError && !isEdited)
			text = text.mergeStyle(TextFormatting.GRAY);
		return text;
	}
	
	public T getValue() {
		return this.value;
	}
	public void setValue(T value) {
		this.value = value;
	}
	public void setValueTransparently(T value) {
		runTransparentAction(() -> {
			setValue(value);
			if (!isPreviewingExternal())
				setDisplayedValue(value);
		});
	}
	
	public T getDisplayedValue() {
		return getValue();
	}
	public void setDisplayedValue(T value) {}
	
	public @Nullable T getOriginal() {
		return original;
	}
	public void setOriginal(@Nullable T original) {
		this.original = original;
	}
	
	public @Nullable T getExternalValue() {
		return external;
	}
	public void setExternalValue(@Nullable T value) {
		this.external = value;
	}
	
	public void resetValue() {
		setValueTransparently(getDefaultValue());
	}
	
	public void restoreValue() {
		setValueTransparently(getOriginal());
	}
	
	public void acceptExternalValue() {
		if (hasExternalDiff() && !hasAcceptedExternalDiff()) {
			if (isPreviewingExternal()) setPreviewingExternal(false);
			setValueTransparently(getExternalValue());
		}
	}
	
	public void restoreHistoryValue(Object storedValue) {
		try {
			//noinspection unchecked
			setValue((T) storedValue);
			if (!isPreviewingExternal()) //noinspection unchecked
				setDisplayedValue((T) storedValue);
			applyHistoryHighlight();
		} catch (RuntimeException e) {
			applyErrorHighlight();
			LOGGER.warn("Could not revert value of config entry with path " + getPath() + ": " + e.getMessage());
		}
	}
	
	public boolean isPreviewingExternal() {
		return previewingExternal;
	}
	
	public void setPreviewingExternal(boolean previewing) {
		if (!hasExternalDiff()) previewing = false;
		if (previewing == previewingExternal) return;
		final T value = getValue();
		final T external = getExternalValue();
		if (previewing) {
			previewingExternal = true;
			setDisplayedValue(external);
			if (this instanceof IExpandable) ((IExpandable) this).setExpanded(true, true);
			setListener(null);
		} else {
			setDisplayedValue(value);
			previewingExternal = false;
		}
	}
	
	/**
	 * Override {@link AbstractConfigEntry#onMouseClicked} instead.
	 */
	@Override public final boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isShown()) return false;
		if (handleModalClicks(mouseX, mouseY, button)) return true;
		return onMouseClicked(mouseX, mouseY, button);
	}
	
	public boolean handleModalClicks(double mouseX, double mouseY, int button) {
		return false;
	}
	
	public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	/**
	 * Override {@link AbstractConfigEntry#onKeyPressed} instead.
	 */
	@Override public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!isShown()) return false;
		if (handleModalKeyPress(keyCode, scanCode, modifiers)) return true;
		return onKeyPressed(keyCode, scanCode, modifiers);
	}
	
	public boolean handleModalKeyPress(int keyCode, int scanCode, int modifiers) {
		return false;
	}
	
	public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	public void updateValue() {
		if (isPreviewingExternal()) {
			setDisplayedValue(getExternalValue());
		} else if (isEditable()) {
			setValue(getDisplayedValue());
		}
	}
	
	public void runTransparentAction(Runnable action) {
		preserveState();
		action.run();
		// getScreen().commitHistory();
	}
	
	public @Nullable ResetButton getResetButton() {
		return resetButton;
	}
	
	@Override public void applyFocusHighlight(int color, int length) {
		lastFocusHighlightTime = System.currentTimeMillis();
		focusHighlightColor = color;
		focusHighlightLength = length;
	}
	
	@Internal public Optional<ITextComponent> getErrorMessage() {
		return Optional.empty();
	}
	
	/**
	 * Subclasses should instead override {@link AbstractConfigEntry#getErrorMessage()}
	 * for entry specific errors. This method wraps that with the error supplier.
	 */
	public List<EntryError> getErrors() {
		List<EntryError> errors = new ArrayList<>();
		if (this.errorSupplier != null)
			errorSupplier.get().ifPresent(e -> errors.add(EntryError.of(e, this)));
		getErrorMessage().ifPresent(e -> errors.add(EntryError.of(e, this)));
		return errors;
	}
	
	public boolean hasError() {
		return !getErrors().isEmpty()
		       || this instanceof IEntryHolder
		          && ((IEntryHolder) this).getHeldEntries().stream()
		            .anyMatch(AbstractConfigEntry::hasError);
	}
	
	public void setErrorSupplier(@Nullable Supplier<Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
	}
	public @Nullable Supplier<Optional<ITextComponent>> getErrorSupplier() {
		return errorSupplier;
	}
	
	public @Nullable T getDefaultValue() {
		return defaultSupplier.get();
	}
	public void setDefaultValue(Supplier<T> defaultSupplier) {
		this.defaultSupplier = defaultSupplier;
	}
	
	public void setSaveConsumer(@Nullable Consumer<T> saveConsumer) {
		this.saveConsumer = saveConsumer;
	}
	public @Nullable Consumer<T> getSaveConsumer() {
		return saveConsumer;
	}
	
	public boolean isIgnoreEdits() {
		return ignoreEdits;
	}
	public void setIgnoreEdits(boolean ignoreEdits) {
		this.ignoreEdits = ignoreEdits;
	}
	
	@NotNull public final AbstractConfigScreen getScreen() {
		AbstractConfigScreen screen = this.screen;
		if (screen != null) return screen;
		AbstractConfigEntry<?> parent = getParentEntry();
		if (parent != null) screen = parent.getScreen();
		if (screen == null) throw new IllegalStateException(
		  "Cannot get config screen so early!");
		return screen;
	}
	
	protected final void addTooltip(@NotNull Tooltip tooltip) {
		getScreen().addTooltip(tooltip);
	}
	
	@Internal public boolean preserveState() {
		updateValue();
		getScreen().getHistory().preserveState(this);
		return true;
	}
	
	public void updateFocused(boolean isFocused) {
		if (isFocused && !isFocused()) {
			//noinspection SuspiciousMethodCalls
			if (!(this instanceof IEntryHolder)
			    || !((IEntryHolder) this).getHeldEntries().contains(getListener()))
				preserveState();
		}
		setFocused(isFocused);
		if (!isFocused) {
			final IGuiEventListener listener = getListener();
			if (listener instanceof Widget && ((Widget) listener).isFocused())
				WidgetUtils.forceUnFocus(listener);
			setListener(null);
		}
	}
	
	@Internal public void setScreen(@Nullable AbstractConfigScreen screen) {
		this.screen = screen;
	}
	
	public boolean isEditable() {
		return getScreen().isEditable() && editable
		       && (parentEntry == null || parentEntry.isEditable())
		       && !isPreviewingExternal();
	}
	
	public boolean shouldRenderEditable() {
		return isEditable() || isPreviewingExternal()
		       || (parentEntry != null && parentEntry.shouldRenderEditable());
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public void setEditableSupplier(Supplier<Boolean> editableSupplier) {
		this.editableSupplier = editableSupplier;
	}
	
	public @Nullable Supplier<Boolean> getEditableSupplier() {
		return editableSupplier;
	}
	
	public void save() {
		setPreviewingExternal(false);
		updateValue();
		final Consumer<T> saveConsumer = getSaveConsumer();
		if (!isIgnoreEdits() && !isSubEntry() && saveConsumer != null && isEditable())
			saveConsumer.accept(getValue());
	}
	
	public boolean isEdited() {
		return !isIgnoreEdits() && !isSubEntry() && isEditable()
		       && (hasError() || !areEqual(getValue(), getOriginal()));
	}
	
	public boolean isResettable() {
		if (!isEditable() || isSubEntry()) return false;
		return hasError() || !areEqual(getValue(), getDefaultValue());
	}
	
	public boolean isRestorable() {
		if (!isEditable() || isSubEntry()) return false;
		return hasError() || !areEqual(getValue(), getOriginal());
	}
	
	public boolean isGroup() {
		return this instanceof IEntryHolder;
	}
	public boolean canResetGroup() {
		return isGroup() && isResettable();
	}
	public boolean canRestoreGroup() {
		return isGroup() && isRestorable();
	}
	
	public boolean hasExternalDiff() {
		if (isSubEntry()) return false;
		final T external = getExternalValue();
		return external != null && !areEqual(getOriginal(), external);
	}
	
	public boolean hasAcceptedExternalDiff() {
		if (isSubEntry()) return false;
		final T external = getExternalValue();
		return external != null && areEqual(getValue(), external);
	}
	
	public boolean hasConflictingExternalDiff() {
		return hasExternalDiff() && !hasAcceptedExternalDiff();
	}
	
	public boolean areEqual(T value, T other) {
		return Objects.equals(value, other);
	}
	
	@Override public int getItemHeight() {
		return 24;
	}
	
	@Override public void tick() {
		super.tick();
		if (!isShown() && isFocused()) updateFocused(false);
		updateValue();
		final Supplier<Boolean> editableSupplier = getEditableSupplier();
		if (editableSupplier != null) {
			final Boolean editable = editableSupplier.get();
			if (editable != null) setEditable(editable);
		}
		if (this instanceof IEntryHolder)
			((IEntryHolder) this).getHeldEntries().forEach(AbstractConfigEntry::tick);
	}
	
	@Override public void render(
	  MatrixStack mStack, int index, int x, int y, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		renderBg(mStack, index, x, y, w, h, mouseX, mouseY, isHovered, delta);
		renderEntry(mStack, index, x, y, w, h, mouseX, mouseY, isHovered, delta);
		renderEntryOverlay(mStack, index, x, y, w, h, mouseX, mouseY, isHovered, delta);
	}
	
	@Override public DynamicEntryListWidget<?> getEntryList() {
		DynamicEntryListWidget<?> entryList = this.entryList;
		if (entryList != null) return entryList;
		AbstractConfigEntry<?> parent = getParentEntry();
		try {
			if (parent != null) entryList = parent.getEntryList();
		} catch (IllegalStateException ignored) {}
		if (entryList == null) throw new IllegalStateException(
		  "Tried to get parent of orphan config entry of type " + getClass().getSimpleName() +
		  "\nThis entry hasn't been properly initialized");
		return entryList;
	}
	
	@Override public void setEntryList(@Nullable DynamicEntryListWidget<?> parent) {
		this.entryList = parent;
	}
	
	public void renderBg(
	  MatrixStack mStack, int index, int x, int y, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		AbstractConfigScreen screen = getScreen();
		if (matchesSearch() && !screen.getSearchBar().isFilter())
			fill(mStack, 0, y, screen.width, y + getCaptionHeight(),
			     isFocusedMatch()? focusedMatchColor : matchColor);
		final long t = System.currentTimeMillis() - lastFocusHighlightTime - focusHighlightLength;
		if (t < 1000) {
			int color = focusHighlightColor;
			fill(mStack, 0, y, screen.width, y + getCaptionHeight(),
			     color & 0xFFFFFF | (int) ((color >> 24 & 0xFF) * (min(1000, 1000 - t) / 1000D)) << 24);
		}
	}
	
	@Internal public void renderEntry(
	  MatrixStack mStack, int index, int x, int y, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {}
	
	public void renderEntryOverlay(
	  MatrixStack mStack, int index, int x, int y, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		if (isSelected())
			renderSelectionOverlay(mStack, index, y, x, w, h, mouseX, mouseY, isHovered, delta);
	}
	
	protected void renderSelectionOverlay(
	  MatrixStack mStack, int index, int y, int x, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		fill(mStack, 2, y - 2, x + w, y + h - 2, selectionColor);
	}
	
	@Override public void claimFocus() {
		getScreen().setSelectedCategory(getCategory());
		AbstractConfigEntry<?> parent = getParentEntry();
		List<AbstractConfigEntry<?>> parents = Lists.newArrayList();
		parents.add(this);
		while (parent != null) {
			parents.add(parent);
			parent = parent.getParentEntry();
		}
		
		AbstractConfigEntry<?> p = parents.get(parents.size() - 1);
		getEntryList().setListener(p);
		for (int i = parents.size() - 2; i >= 0; i--) {
			AbstractConfigEntry<?> n = parents.get(i);
			p.setListener(n);
			p = n;
		}
		getEntryList().scrollTo(this);
		acquireFocus();
		getEntryList().setSelectedTarget(this);
	}
	
	protected static void playFeedbackTap(float volume) {
		Minecraft.getInstance().getSoundHandler().play(
		  SimpleSound.master(SimpleConfigMod.UI_TAP, volume));
	}
	
	protected static void playFeedbackClick(float volume) {
		Minecraft.getInstance().getSoundHandler().play(
		  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, volume));
	}
	
	// Search
	protected boolean matchesSearch() {
		return matchedText != null && !matchedText.isEmpty();
	}
	
	@Override public List<ISeekableComponent> search(Pattern query) {
		final boolean matchesSelf = searchSelf(query);
		final List<Pair<ISeekableComponent, List<ISeekableComponent>>> children =
		  seekableChildren().stream().map(c -> Pair.of(c, c.search(query)))
			 .filter(p -> !p.second.isEmpty()).collect(Collectors.toList());
		final List<ISeekableComponent> result =
		  children.stream().flatMap(p -> p.second.stream()).collect(Collectors.toList());
		if (matchesSelf)
			result.add(0, this);
		return result;
	}
	
	protected boolean searchSelf(Pattern query) {
		boolean matches = false;
		final String text = seekableText();
		matchedText = matchedValueText = null;
		if (!text.isEmpty()) {
			final Matcher m = query.matcher(text);
			while (m.find()) {
				if (!m.group().isEmpty()) {
					matches = true;
					matchedText = m.group();
					break;
				}
			}
		}
		final String valueText = seekableValueText();
		if (!valueText.isEmpty()) {
			final Matcher m = query.matcher(valueText);
			while (m.find()) {
				if (!m.group().isEmpty()) {
					matches = true;
					matchedValueText = m.group();
					break;
				}
			}
		}
		return matches;
	}
	
	@Internal public String seekableText() {
		if (isSubEntry())
			return "";
		return getUnformattedString(getDisplayedTitle());
	}
	
	@Internal public String seekableValueText() {
		final T value = getValue();
		if (value != null)
			return value.toString();
		return "";
	}
	
	protected List<ISeekableComponent> seekableChildren() {
		return Collections.emptyList();
	}
	
	@Override public boolean isFocusedMatch() {
		return focusedMatch;
	}
	
	@Override public void setFocusedMatch(boolean isFocusedMatch) {
		focusedMatch = isFocusedMatch;
		expandParents();
		claimFocus();
	}
	
	@Override public @Nullable INavigableTarget getNavigableParent() {
		return navigableParent != null? navigableParent : getParentEntry();
	}
	
	@Override public boolean isNavigable() {
		return isShown();
	}
	
	@Override public boolean isNavigableSubTarget() {
		return isSubEntry;
	}
	
	public void setNavigableParent(@Nullable INavigableTarget parent) {
		this.navigableParent = parent;
	}
	
	@Override public void navigate() {
		expandParents();
		claimFocus();
	}
	
	protected void acquireFocus() {
		final List<? extends IGuiEventListener> listeners = getEventListeners();
		if (!listeners.isEmpty()) {
			final IGuiEventListener listener = listeners.get(0);
			setListener(listener);
			WidgetUtils.forceFocus(listener);
		}
	}
	
	private static final Pattern STYLE_ESCAPE = Pattern.compile("§[\\da-f]");
	protected static String getUnformattedString(ITextComponent component) {
		return STYLE_ESCAPE.matcher(component.getString()).replaceAll("");
	}
	
	protected static void drawBorder(
	  MatrixStack mStack, int x, int y, int w, int h, int borderWidth, int color
	) {
		int maxX = x + w;
		int maxY = y + h;
		int bw = min(min(borderWidth, w), h);
		fill(mStack, x, y, maxX, y + bw, color);
		fill(mStack, x, y + bw, x + bw, maxY - bw, color);
		fill(mStack, maxX - bw, y + bw, maxX, maxY - bw, color);
		fill(mStack, x, maxY - bw, maxX, maxY, color);
	}
	
	public static class EntryError {
		protected ITextComponent error;
		protected INavigableTarget source;
		protected AbstractConfigEntry<?> entry;
		
		protected EntryError(
		  ITextComponent error, INavigableTarget source
		) {
			this.error = error;
			this.source = source;
			this.entry = source.findParentEntry();
		}
		
		public static EntryError of(ITextComponent error, INavigableTarget source) {
			return new EntryError(error, source);
		}
		
		public static EntryError wrap(ITextComponent error, EntryError cause) {
			return new EntryError(error, cause.source);
		}
		
		public ITextComponent getError() {
			return error;
		}
		
		public INavigableTarget getSource() {
			return source;
		}
		
		public AbstractConfigEntry<?> getEntry() {
			return entry;
		}
	}
}
