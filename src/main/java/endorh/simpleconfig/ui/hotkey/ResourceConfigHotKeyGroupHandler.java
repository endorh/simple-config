package endorh.simpleconfig.ui.hotkey;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import endorh.simpleconfig.ui.hotkey.ResourceConfigHotKeyGroupHandler.Loader;
import endorh.simpleconfig.ui.hotkey.SavedHotKeyGroupPickerWidget.ResourceSavedHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.SavedHotKeyGroupPickerWidget.SavedHotKeyGroup;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ResourceConfigHotKeyGroupHandler extends ReloadListener<Loader> {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder()
	  .registerTypeAdapter(ResourceHotKeyGroupsDescriptor.class, new ResourceHotKeyGroupsDescriptor.Serializer())
	  .create();
	private static final TypeToken<ResourceHotKeyGroupsDescriptor> TYPE =
	  new TypeToken<ResourceHotKeyGroupsDescriptor>() {};
	public static final ResourceConfigHotKeyGroupHandler INSTANCE = new ResourceConfigHotKeyGroupHandler();
	private final Map<String, ResourceSavedHotKeyGroup> groupRegistry = Maps.newLinkedHashMap();
	
	public List<ResourceSavedHotKeyGroup> getResourceHotKeyGroups() {
		return new ArrayList<>(groupRegistry.values());
	}
	
	@Override protected @NotNull Loader prepare(@NotNull IResourceManager manager, @NotNull IProfiler profiler) {
		Loader l = new Loader();
		profiler.startTick();
		for (String namespace: manager.getResourceNamespaces()) {
			profiler.startSection(namespace);
			try {
				for (IResource index: manager.getAllResources(new ResourceLocation(namespace, "config-hotkeys.json"))) {
					profiler.startSection(index.getPackName());
					try (
					  InputStream is = index.getInputStream();
					  Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)
					) {
						profiler.startSection("parse");
						ResourceHotKeyGroupsDescriptor desc = JSONUtils.fromJSONUnlenient(GSON, r, TYPE);
						profiler.endStartSection("register");
						if (desc != null) l.registerGroups(namespace, desc);
						profiler.endSection();
					} catch (RuntimeException e) {
						LOGGER.warn("Invalid config-hotkeys.json in resourcepack: '{}'", index.getPackName(), e);
					}
					profiler.endSection();
				}
			} catch (IOException ignored) {}
			profiler.endSection();
		}
		profiler.endTick();
		return l;
	}
	
	@Override protected void apply(@NotNull Loader l, @NotNull IResourceManager manager, @NotNull IProfiler profiler) {
		groupRegistry.clear();
		l.getGroups().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
		  e -> groupRegistry.put(e.getKey(), e.getValue()));
	}
	
	public static class Loader {
		private final Map<String, ResourceSavedHotKeyGroup> groupMap = Maps.newHashMap();
		public void registerGroups(String namespace, ResourceHotKeyGroupsDescriptor descriptor) {
			for (String name: descriptor.getGroups()) {
				String groupName = namespace + ":" + name;
				groupMap.put(groupName, SavedHotKeyGroup.resource(
				  groupName, new ResourceLocation(namespace, "config-hotkeys/" + name + ".yaml")));
			}
			descriptor.getDefaultGroups().stream().sorted().forEach(name -> {
				String groupName = namespace + ":" + name;
				ResourceSavedHotKeyGroup group = SavedHotKeyGroup.resource(
				  groupName, new ResourceLocation(namespace, "config-hotkeys/" + name + ".yaml"));
				groupMap.put(groupName, group);
				try {
					ConfigHotKeyManager.INSTANCE.addDefaultGroup(group.load().get());
				} catch (InterruptedException | ExecutionException ignored) {}
			});
		}
		
		public Map<String, ResourceSavedHotKeyGroup> getGroups() {
			return groupMap;
		}
	}
	
	public static class ResourceHotKeyGroupsDescriptor {
		private final Set<String> groups;
		private final Set<String> defaultGroups;
		
		public ResourceHotKeyGroupsDescriptor(Set<String> groups, Set<String> defaultGroups) {
			this.groups = groups;
			this.defaultGroups = defaultGroups;
		}
		
		public Set<String> getGroups() {
			return groups;
		}
		
		public Set<String> getDefaultGroups() {
			return defaultGroups;
		}
		
		public static class Serializer implements JsonDeserializer<ResourceHotKeyGroupsDescriptor>, JsonSerializer<ResourceHotKeyGroupsDescriptor> {
			@Override public ResourceHotKeyGroupsDescriptor deserialize(
			  JsonElement json, Type type, JsonDeserializationContext ctx
			) throws JsonParseException {
				JsonObject obj = JSONUtils.getJsonObject(json, "config hotkeys");
				JsonArray arr = JSONUtils.getJsonArray(obj, "groups", new JsonArray());
				Set<String> set = new HashSet<>();
				if (arr != null) for (JsonElement item: arr)
					set.add(JSONUtils.getString(item, "group name"));
				arr = JSONUtils.getJsonArray(obj, "default_groups", new JsonArray());
				Set<String> def = new HashSet<>();
				if (arr != null) for (JsonElement item: arr)
					def.add(JSONUtils.getString(item, "group name"));
				return new ResourceHotKeyGroupsDescriptor(set, def);
			}
			
			@Override public JsonElement serialize(
			  ResourceHotKeyGroupsDescriptor src, Type typeOfSrc, JsonSerializationContext ctx
			) {
				JsonObject obj = new JsonObject();
				obj.add("groups", ctx.serialize(new ArrayList<>(src.groups)));
				obj.add("default_groups", ctx.serialize(new ArrayList<>(src.defaultGroups)));
				return obj;
			}
		}
	}
}
