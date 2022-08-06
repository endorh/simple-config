package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simpleconfig.core.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simpleconfig.core.SimpleConfig.NoSuchConfigEntryError;
import endorh.simpleconfig.core.SimpleConfig.NoSuchConfigGroupError;
import endorh.simpleconfig.core.entry.GUIOnlyEntry;
import endorh.simpleconfig.core.entry.TextEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.yaml.NodeComments;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.comments.CommentLine;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.blankLine;

public abstract class AbstractSimpleConfigEntryHolder implements ISimpleConfigEntryHolder {
	protected static final Pattern DOT = Pattern.compile("\\.");
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	private static final Logger LOGGER = LogManager.getLogger();
	protected Map<String, AbstractConfigEntry<?, ?, ?, ?>> entries;
	protected Map<String, ? extends AbstractSimpleConfigEntryHolder> children;
	protected SimpleConfig root;
	protected boolean dirty = false;
	
	/**
	 * Get the root config of this entry holder
	 */
	@Override public SimpleConfig getRoot() {
		return root;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public @Nullable AbstractConfigScreen getGUI() {
		return root.gui;
	}
	
	protected abstract String getPath();
	
	protected String getPathPart() {
		return getPath() + ".";
	}
	
	protected String getGlobalPath() {
		return getRoot().getName() + "." + getPath();
	}
	
	protected abstract String getName();
	
	protected String getConfigComment() {
		return "";
	}
	
	protected @Nullable NodeComments getNodeComments(@Nullable NodeComments previous) {
		if (previous == null) previous = new NodeComments();
		List<CommentLine> blockComments = previous.getBlockComments();
		String configComment = getConfigComment();
		if (configComment.endsWith("\n")) configComment = configComment.substring(0, configComment.length() - 1);
		if (!configComment.isEmpty()) {
			if (blockComments == null) blockComments = Lists.newArrayList();
			blockComments.clear();
			Arrays.stream(LINE_BREAK.split(configComment))
			  .map(line -> SimpleConfigCommentedYamlWriter.commentLine(" " + line))
			  .forEach(blockComments::add);
			previous.setBlockComments(blockComments);
		}
		return previous.isNotEmpty()? previous : null;
	}
	
	/**
	 * Mark this entry holder as dirty<br>
	 * When the config screen is saved, only config files containing dirty entries are updated
	 */
	@Override public AbstractSimpleConfigEntryHolder markDirty() {
		markDirty(true);
		return this;
	}
	
	@Override public boolean isDirty() {
		return dirty;
	}
	
	protected void saveSnapshot(
	  CommentedConfig config, boolean fromGUI, @Nullable Set<String> selectedPaths
	) {
		for (Entry<String, ? extends AbstractSimpleConfigEntryHolder> e : children.entrySet()) {
			final CommentedConfig subConfig = config.createSubConfig();
			e.getValue().saveSnapshot(subConfig, fromGUI, selectedPaths);
			if (!subConfig.isEmpty())
				config.set(e.getKey(), subConfig);
		}
		for (Entry<String, AbstractConfigEntry<?, ?, ?, ?>> e : entries.entrySet()) {
			//noinspection unchecked
			final AbstractConfigEntry<?, Object, ?, ?> entry =
			  (AbstractConfigEntry<?, Object, ?, ?>) e.getValue();
			if ((selectedPaths == null || selectedPaths.contains(entry.getPath()))
			    && !entry.nonPersistent)
				entry.put(config, fromGUI? entry.guiForConfig() : entry.getForConfig());
		}
	}
	
	protected void loadSnapshot(
	  CommentedConfig config, boolean intoGUI, @Nullable Set<String> selectedPaths
	) {
		for (Entry<String, ? extends AbstractSimpleConfigEntryHolder> e : children.entrySet()) {
			if (config.contains(e.getKey())) {
				final Object sub = config.get(e.getKey());
				if (sub instanceof CommentedConfig)
					e.getValue().loadSnapshot(((CommentedConfig) sub), intoGUI, selectedPaths);
			}
		}
		for (Entry<String, AbstractConfigEntry<?, ?, ?, ?>> e : entries.entrySet()) {
			//noinspection unchecked
			AbstractConfigEntry<?, Object, ?, ?> entry = (AbstractConfigEntry<?, Object, ?, ?>) e.getValue();
			if ((selectedPaths == null || selectedPaths.contains(entry.getPath()))
			    && config.contains(e.getKey())) {
				try {
					if (intoGUI) {
						entry.setFromConfigForGUI(entry.get(config));
					} else entry.setFromConfig(entry.get(config));
				} catch (InvalidConfigValueTypeException ignored) {
					LOGGER.warn("Error loading config snapshot. Invalid value type for entry " + entry.getGlobalPath());
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	protected void saveGUISnapshot(CommentedConfig config, @Nullable Set<String> selectedPaths) {
		for (Entry<String, ? extends AbstractSimpleConfigEntryHolder> e : children.entrySet()) {
			final CommentedConfig subConfig = config.createSubConfig();
			e.getValue().saveGUISnapshot(subConfig, selectedPaths);
			if (!subConfig.isEmpty())
				config.set(e.getKey(), subConfig);
		}
		for (Entry<String, AbstractConfigEntry<?, ?, ?, ?>> e : entries.entrySet()) {
			//noinspection unchecked
			final AbstractConfigEntry<?, Object, ?, ?> entry =
			  (AbstractConfigEntry<?, Object, ?, ?>) e.getValue();
			if ((selectedPaths == null || selectedPaths.contains(entry.getPath()))
			    && !entry.nonPersistent)
				entry.put(config, entry.guiForConfig());
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	protected void loadGUIExternalChanges() {
		children.values().forEach(AbstractSimpleConfigEntryHolder::loadGUIExternalChanges);
		entries.values().forEach(AbstractSimpleConfigEntryHolder::setForGUIAsExternal);
	}
	
	private static <V> void setForGUIAsExternal(AbstractConfigEntry<V, ?, ?, ?> entry) {
		entry.setForGUIAsExternal(entry.get());
	}
	
	protected void updateComments(Map<String, NodeComments> comments) {
		String type = root.getType().extension();
		boolean first = true;
		for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values()) {
			String path = entry.getPath();
			if (path.startsWith(type + ".")) path = path.substring(type.length() + 1);
			NodeComments nodeComments = entry.getNodeComments(comments.get(path));
			if (nodeComments != null) {
				first = false;
				comments.put(path, nodeComments);
			} else comments.remove(path);
		}
		for (AbstractSimpleConfigEntryHolder child : children.values()) {
			String path = child.getPath();
			if (path.startsWith(type + ".")) path = path.substring(type.length() + 1);
			NodeComments nodeComments = child.getNodeComments(comments.get(path));
			if (first) {
				first = false;
			} else nodeComments = NodeComments.prefix(blankLine()).appendAsPrefix(nodeComments, false);
			if (nodeComments != null && nodeComments.isNotEmpty())
				comments.put(path, nodeComments);
			else comments.remove(path);
			child.updateComments(comments);
		}
	}
	
	protected void buildConfigSpec(ConfigSpec spec, String path) {
		final String thisPath = path + getName() + '.';
		for (AbstractConfigEntry<?, ?, ?, ?> e : entries.values())
			e.buildSpec(spec, thisPath);
		for (AbstractSimpleConfigEntryHolder child : children.values())
			child.buildConfigSpec(spec, thisPath);
	}
	
	protected abstract void bake();
	
	/**
	 * Mark this entry holder as dirty or clean<br>
	 * The clean state is propagated to all of the children<br>
	 * Subclasses should propagate the dirty state to their parents
	 */
	@Override public void markDirty(boolean dirty) {
		this.dirty = dirty;
		if (!dirty) {
			children.values().forEach(c -> c.markDirty(false));
			entries.values().forEach(e -> e.dirty(false));
		}
	}
	
	/**
	 * Remove GUI bindings after saving a GUI
	 */
	@OnlyIn(Dist.CLIENT) protected void removeGUI() {
		entries.values().forEach(e -> e.guiEntry = null);
		children.values().forEach(AbstractSimpleConfigEntryHolder::removeGUI);
	}
	
	/**
	 * Check if any dirty entry requires a restart
	 */
	public boolean anyDirtyRequiresRestart() {
		return entries.values().stream().anyMatch(e -> e.dirty && e.requireRestart)
		       || children.values().stream().anyMatch(
		         AbstractSimpleConfigEntryHolder::anyDirtyRequiresRestart);
	}
	
	/**
	 * Get a child entry holder, if it exists<br>
	 * @param path Name or dot-separated path to the child
	 *             If null or empty, {@code this} will be returned
	 * @return A child entry holder, or null if it doesn't exist
	 */
	public @Nullable AbstractSimpleConfigEntryHolder getChildOrNull(String path) {
		if (path == null || path.isEmpty()) return this;
		final String[] split = DOT.split(path, 2);
		if (split.length < 2) {
			return children.get(path);
		} else if (children.containsKey(split[0]))
			return children.get(split[0]).getChildOrNull(split[1]);
		return null;
	}
	
	/**
	 * Get a child entry holder<br>
	 * @param path Name or dot-separated path to the child
	 *             If null or empty, {@code this} will be returned
	 * @throws NoSuchConfigGroupError if the child is not found
	 * @return A child {@link AbstractSimpleConfigEntryHolder},
	 *         or {@code this} if path is null or empty.
	 */
	public @NotNull AbstractSimpleConfigEntryHolder getChild(String path) {
		AbstractSimpleConfigEntryHolder child = getChildOrNull(path);
		if (child == null) throw new NoSuchConfigGroupError(getPath() + "." + path);
		return child;
	}
	
	/**
	 * Get a config entry by name or dot-separated path<br>
	 * Doesn't check for the types to be correct<br>
	 * @param path Name or dot-separated path to the entry
	 * @return The entry, or null if not found
	 * @see #get(String)
	 * @see #set(String, Object)
	 * @see #getEntry(String)
	 */
	@Internal public <T, C, Gui> @Nullable AbstractConfigEntry<T, C, Gui, ?> getEntryOrNull(String path) {
		AbstractConfigEntry<?, ?, ?, ?> entry = entries.get(path);
		if (entry == null) entry = getSubEntry(path);
		//noinspection unchecked
		return (AbstractConfigEntry<T, C, Gui, ?>) entry;
	}
	
	/**
	 * Get a config entry by name or dot-separated path
	 * @param path Name or dot-separated path to the entry
	 * @param <T> Expected type of the entry
	 * @param <Gui> Expected GUI type of the entry
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @see #get(String)
	 * @see #set(String, Object)
	 * @see #getEntryOrNull(String)
	 */
	@Internal public <T, C, Gui> @NotNull AbstractConfigEntry<T, C, Gui, ?> getEntry(String path) {
		AbstractConfigEntry<T, C, Gui, ?> entry = getEntryOrNull(path);
		if (entry == null) throw new NoSuchConfigEntryError(getPath() + "." + path);
		return entry;
	}
	
	/**
	 * Get a config entry by path<br>
	 * The path must at least contain one level<br>
	 * This method is not intended to be called directly, instead use
	 * the more general {@link AbstractSimpleConfigEntryHolder#getEntry(String)}
	 * @param path Dot-separated path, containing at least one dot
	 * @param <T> Expected type of the entry
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	protected <T> AbstractConfigEntry<T, ?, ?, ?> getSubEntry(String path) {
		final String[] split = DOT.split(path, 2);
		if (split.length < 2) return null;
		AbstractSimpleConfigEntryHolder child = getChildOrNull(split[0]);
		if (child == null) return null;
		return child.getEntryOrNull(split[1]);
	}
	
	/**
	 * Check if a certain path exists.<br>
	 * @param path Name or dot-separated path to the entry
	 * @return {@code true} if the entry exists, {@code false} otherwise
	 */
	@Internal public boolean hasEntry(String path) {
		AbstractConfigEntry<?, ?, ?, ?> entry = getEntryOrNull(path);
		return entry != null && !(entry instanceof TextEntry);
	}
	
	@Internal public boolean hasChild(String path) {
		return getChildOrNull(path) != null;
	}
	
	/**
	 * Build a collection with all the paths below this<br>
	 */
	@Internal public Collection<String> getPaths(boolean includeGroups) {
		List<String> list = new ArrayList<>();
		gatherPaths(list, "", includeGroups);
		return list;
	}
	
	protected void gatherPaths(List<String> paths, String prefix, boolean includeGroups) {
		entries.forEach((k, e) -> {
			if (!(e instanceof TextEntry) && !(e instanceof GUIOnlyEntry))
				paths.add(prefix + k);
		});
		children.forEach((key, child) -> {
			if (includeGroups) paths.add(prefix + key);
			child.gatherPaths(paths, prefix + key + '.', includeGroups);
		});
	}
	
	protected abstract void commitFields();
	
	protected void commitFields(String path) {
		if (path == null || path.isEmpty())
			commitFields();
		getChild(path).commitFields();
	}
	
	/**
	 * Get a config value<br>
	 * To retrieve a numeric primitive value use instead the variant methods,
	 * or pass {@link Number} as the type parameter to prevent illegal casts.
	 * @param path Name or dot-separated path to the value
	 * @param <T> Expected type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type does not match the expected
	 * @see #getBoolean(String)
	 * @see #getChar(String)
	 * @see #getByte(String)
	 * @see #getShort(String)
	 * @see #getInt(String)
	 * @see #getLong(String)
	 * @see #getFloat(String)
	 * @see #getDouble(String)
	 */
	@Override public <T> T get(String path) {
		try {
			return this.<T, Object, Object>getEntry(path).get();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Set a config value<br>
	 * Use {@link AbstractSimpleConfigEntryHolder#set(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 * @deprecated Use {@link AbstractSimpleConfigEntryHolder#set(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@Internal @Deprecated @Override public <V> void doSet(String path, V value) {
		try {
			AbstractConfigEntry<V, ?, Object, ?> entry = this.getEntry(path);
			if (!entry.typeClass.isInstance(value))
				throw new InvalidConfigValueTypeException(getPath() + "." + path);
			entry.set(value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Set a config value in the GUI<br>
	 * Use {@link AbstractSimpleConfigEntryHolder#setGUI(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <G> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 * @deprecated Use {@link AbstractSimpleConfigEntryHolder#setGUI(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@Internal @Deprecated @Override public <G> void doSetGUI(String path, G value) {
		try {
			this.<Object, Object, G>getEntry(path).setGUI(value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	protected <V> void doSetForGUI(String path, V value) {
		try {
			getEntry(path).setForGUI(value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Set a config value in the GUI, translating it from its actual type<br>
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	public <V> void setForGUI(String path, V value) {
		if (value instanceof Number) {
			try {
				setForGUI(path, (Number) value);
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSetForGUI(path, value);
	}
	
	/**
	 * Set a config value in the GUI, translating it from its actual type<br>
	 * Numeric types are upcast as needed
	 * @param path Name or dot-separated path to the value
	 * @param number Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	public void setForGUI(String path, Number number) {
		boolean pre;
		//noinspection AssignmentUsedAsCondition
		if (pre = number instanceof Byte) {
			try {
				doSetForGUI(path, number.byteValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Short) {
			try {
				doSetForGUI(path, number.shortValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Integer) {
			try {
				doSetForGUI(path, number.intValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre || number instanceof Long) {
			try {
				doSetForGUI(path, number.longValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (number instanceof Float) {
			try {
				doSetForGUI(path, number.floatValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSetForGUI(path, number.doubleValue());
	}
	
	@Override public boolean hasGUI(String path) {
		return this.getEntry(path).hasGUI();
	}
	
	@Override public <G> G getGUI(String path) {
		try {
			return this.<Object, Object, G>getEntry(path).getGUI();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	@Override public <V> V getFromGUI(String path) {
		try {
			return this.<V, Object, Object>getEntry(path).getFromGUI();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Reset all entries to their default values.
	 */
	public void reset() {
		for (AbstractConfigEntry<?, ?, ?, ?> entry : this.entries.values()) {
			//noinspection unchecked
			AbstractConfigEntry<Object, ?, ?, ?> e = (AbstractConfigEntry<Object, ?, ?, ?>) entry;
			e.set(e.defValue);
		}
		for (AbstractSimpleConfigEntryHolder child: children.values())
			child.reset();
	}
	
	/**
	 * Reset an entry or group of entries to its default value.
	 * @param path Name or dot-separated path to the entry or group of entries
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	public void reset(String path) {
		AbstractSimpleConfigEntryHolder child = getChildOrNull(path);
		if (child != null) {
			child.reset();
		} else {
			AbstractConfigEntry<Object, Object, Object, ?> entry = getEntry(path);
			entry.set(entry.defValue);
		}
	}
	
	/**
	 * Reset an entry within the GUI, if it has one.
	 * @param path Name or dot-separated path to the value
	 * @return {@code true} if the entry has a GUI and was reset, {@code false} otherwise
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	@OnlyIn(Dist.CLIENT) public boolean resetInGUI(String path) {
		AbstractConfigEntry<Object, Object, Object, ?> entry = getEntry(path);
		if (entry.hasGUI()) {
			AbstractConfigListEntry<Object> guiEntry = entry.guiEntry;
			if (guiEntry != null) guiEntry.resetValue();
			return true;
		} else return false;
	}
	
	/**
	 * Restore an entry within the GUI, if it has one.
	 * @param path Name or dot-separated path to the value
	 * @return {@code true} if the entry has a GUI and was restored, {@code false} otherwise
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	@OnlyIn(Dist.CLIENT) public boolean restoreInGUI(String path) {
		AbstractConfigEntry<Object, Object, Object, ?> entry = getEntry(path);
		if (entry.hasGUI()) {
			AbstractConfigListEntry<Object> guiEntry = entry.guiEntry;
			if (guiEntry != null) guiEntry.restoreValue();
			return true;
		} else return false;
	}
}
