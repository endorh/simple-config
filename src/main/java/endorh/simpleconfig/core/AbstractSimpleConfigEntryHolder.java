package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ISimpleConfig.InvalidConfigValueException;
import endorh.simpleconfig.api.ISimpleConfig.InvalidConfigValueTypeException;
import endorh.simpleconfig.api.ISimpleConfig.NoSuchConfigEntryError;
import endorh.simpleconfig.api.ISimpleConfig.NoSuchConfigGroupError;
import endorh.simpleconfig.api.ISimpleConfigEntryHolder;
import endorh.simpleconfig.core.entry.GUIOnlyEntry;
import endorh.simpleconfig.core.entry.TextEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.yaml.NodeComments;
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

import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.commentLine;

public abstract class AbstractSimpleConfigEntryHolder implements ISimpleConfigEntryHolder {
	protected static final Pattern DOT = Pattern.compile("\\.");
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	private static final Logger LOGGER = LogManager.getLogger();
	protected SimpleConfig root;
	protected Map<String, AbstractConfigEntry<?, ?, ?>> entries;
	protected Map<String, ? extends AbstractSimpleConfigEntryHolder> children;
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
		if (configComment.endsWith("\n"))
			configComment = configComment.substring(0, configComment.length() - 1);
		if (blockComments == null) blockComments = Lists.newArrayList();
		blockComments.removeIf(l -> l.getValue().startsWith("#"));
		if (!configComment.isEmpty()) Arrays.stream(LINE_BREAK.split(configComment))
		  .map(line -> commentLine("# " + line))
		  .forEach(blockComments::add);
		// FIXME: Remove once snakeyaml is updated to 1.31 (see bitbucket.org/snakeyaml/snakeyaml/issues/518)
		if (blockComments.size() > 90) {
			LOGGER.warn("Group " + getGlobalPath() + " has too many comments [BUG]. Trimmed to the last 90");
			blockComments.subList(0, blockComments.size() - 90).clear();
		}
		if (blockComments.isEmpty()) blockComments = null;
		previous.setBlockComments(blockComments);
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
	
	protected void removeGUI() {
		entries.values().forEach(AbstractConfigEntry::removeGUI);
		children.values().forEach(AbstractSimpleConfigEntryHolder::removeGUI);
	}
	
	protected void saveSnapshot(
	  CommentedConfig config, boolean fromGUI, boolean fromRemote, @Nullable Set<String> selectedPaths
	) {
		for (Entry<String, ? extends AbstractSimpleConfigEntryHolder> e : children.entrySet()) {
			final CommentedConfig subConfig = config.createSubConfig();
			e.getValue().saveSnapshot(subConfig, fromGUI, fromRemote, selectedPaths);
			if (!subConfig.isEmpty())
				config.set(e.getKey(), subConfig);
		}
		for (Entry<String, AbstractConfigEntry<?, ?, ?>> e : entries.entrySet()) {
			//noinspection unchecked
			final AbstractConfigEntry<?, Object, ?> entry =
			  (AbstractConfigEntry<?, Object, ?>) e.getValue();
			if ((selectedPaths == null || selectedPaths.contains(entry.getPath())) && !entry.nonPersistent)
				entry.put(
				  config, fromGUI? entry.apply(ee -> ee.forConfig(ee.fromGuiOrDefault(ee.getGUI())))
				                 : entry.apply(ee -> ee.forConfig(ee.get())));
		}
	}
	
	protected void loadSnapshot(
	  CommentedConfig config, boolean intoGUI, boolean forRemote, @Nullable Set<String> selectedPaths
	) {
		for (Entry<String, ? extends AbstractSimpleConfigEntryHolder> e : children.entrySet()) {
			if (config.contains(e.getKey())) {
				final Object sub = config.get(e.getKey());
				if (sub instanceof CommentedConfig)
					e.getValue().loadSnapshot((CommentedConfig) sub, intoGUI, forRemote, selectedPaths);
			}
		}
		for (Entry<String, AbstractConfigEntry<?, ?, ?>> e : entries.entrySet()) {
			//noinspection unchecked
			AbstractConfigEntry<?, Object, ?> entry = (AbstractConfigEntry<?, Object, ?>) e.getValue();
			if ((selectedPaths == null || selectedPaths.contains(entry.getPath()))
			    && config.contains(e.getKey()) && !entry.nonPersistent) {
				try {
					if (intoGUI) {
						entry.accept(ee -> ee.setGUI(ee.forGui(ee.fromConfigOrDefault(ee.get(config))), forRemote));
					} else entry.accept(ee -> ee.set(ee.fromConfig(ee.get(config))));
				} catch (InvalidConfigValueTypeException ignored) {
					LOGGER.error("Error loading config snapshot. Invalid value type for entry " + entry.getGlobalPath());
				} catch (InvalidConfigValueException ignored) {
					LOGGER.error("Error loading config snapshot. Invalid value for entry " + entry.getGlobalPath());
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	protected void loadGUIExternalChanges() {
		children.values().forEach(AbstractSimpleConfigEntryHolder::loadGUIExternalChanges);
		entries.values().forEach(entry -> entry.accept(e -> e.setGUIAsExternal(e.forGui(e.get()), false)));
	}
	
	protected void loadGUIRemoteExternalChanges(CommentedConfig config) {
		children.forEach((k, child) -> {
			Object sub = config.get(k);
			if (sub instanceof CommentedConfig) child.loadGUIRemoteExternalChanges((CommentedConfig) sub);
		});
		entries.forEach((k, entry) -> {
			if (config.contains(k) && !entry.nonPersistent) {
				Object v = config.get(k);
				entry.accept(e -> e.setGUIAsExternal(e.forGui(e.fromConfigOrDefault(e.fromActualConfig(v))), true));
			}
		});
	}
	
	protected void updateComments(Map<String, NodeComments> comments) {
		String type = root.getType().getAlias();
		boolean first = true;
		for (AbstractConfigEntry<?, ?, ?> entry : entries.values()) {
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
			} else {
				if (nodeComments == null) nodeComments = new NodeComments();
				nodeComments.addSeparatorLine();
			}
			if (nodeComments != null && nodeComments.isNotEmpty())
				comments.put(path, nodeComments);
			else comments.remove(path);
			child.updateComments(comments);
		}
	}
	
	protected void buildConfigSpec(ConfigSpec spec, String path) {
		final String thisPath = path + getName() + '.';
		for (AbstractConfigEntry<?, ?, ?> e : entries.values())
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
	@Internal @Override public void markDirty(boolean dirty) {
		this.dirty = dirty;
		if (!dirty) {
			children.values().forEach(c -> c.markDirty(false));
			entries.values().forEach(e -> e.dirty(false));
		}
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
	
	@Override public @NotNull AbstractSimpleConfigEntryHolder getChild(String path) {
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
	@Internal public <T, C, Gui> @Nullable AbstractConfigEntry<T, C, Gui> getEntryOrNull(String path) {
		AbstractConfigEntry<?, ?, ?> entry = entries.get(path);
		if (entry == null) entry = getSubEntry(path);
		//noinspection unchecked
		return (AbstractConfigEntry<T, C, Gui>) entry;
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
	@Internal public <T, C, Gui> @NotNull AbstractConfigEntry<T, C, Gui> getEntry(String path) {
		AbstractConfigEntry<T, C, Gui> entry = getEntryOrNull(path);
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
	protected <T> AbstractConfigEntry<T, ?, ?> getSubEntry(String path) {
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
		AbstractConfigEntry<?, ?, ?> entry = getEntryOrNull(path);
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
			AbstractConfigEntry<V, ?, Object> entry = this.getEntry(path);
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
			getEntry(path).accept(e -> e.setGUI(e.forGui(value)));
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	@Override public <V> void setForGUI(String path, V value) {
		if (value instanceof Number) {
			try {
				setForGUI(path, (Number) value);
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSetForGUI(path, value);
	}
	
	@Override public void setForGUI(String path, Number number) {
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
			return this.<V, Object, Object>getEntry(path).apply(e -> e.fromGui(e.getGUI()));
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	@Override public void reset() {
		for (AbstractConfigEntry<?, ?, ?> entry : this.entries.values()) {
			//noinspection unchecked
			AbstractConfigEntry<Object, ?, ?> e = (AbstractConfigEntry<Object, ?, ?>) entry;
			e.set(e.defValue);
		}
		for (AbstractSimpleConfigEntryHolder child: children.values())
			child.reset();
	}
	
	@Override public void reset(String path) {
		AbstractSimpleConfigEntryHolder child = getChildOrNull(path);
		if (child != null) {
			child.reset();
		} else {
			AbstractConfigEntry<Object, Object, Object> entry = getEntry(path);
			entry.set(entry.defValue);
		}
	}
	
	@Override @OnlyIn(Dist.CLIENT) public boolean resetInGUI(String path) {
		AbstractConfigEntry<Object, Object, Object> entry = getEntry(path);
		if (entry.hasGUI()) {
			AbstractConfigListEntry<Object> guiEntry = entry.getGuiEntry();
			if (guiEntry != null) guiEntry.resetValue();
			return true;
		} else return false;
	}
	
	@Override @OnlyIn(Dist.CLIENT) public boolean restoreInGUI(String path) {
		AbstractConfigEntry<Object, Object, Object> entry = getEntry(path);
		if (entry.hasGUI()) {
			AbstractConfigListEntry<Object> guiEntry = entry.getGuiEntry();
			if (guiEntry != null) guiEntry.restoreValue();
			return true;
		} else return false;
	}
}
