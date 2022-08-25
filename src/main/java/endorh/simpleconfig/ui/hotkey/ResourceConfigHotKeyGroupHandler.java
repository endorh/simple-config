package endorh.simpleconfig.ui.hotkey;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import endorh.simpleconfig.ui.hotkey.ResourceConfigHotKeyGroupHandler.Loader;
import endorh.simpleconfig.ui.hotkey.SavedHotKeyGroupPickerWidget.ResourceSavedHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.SavedHotKeyGroupPickerWidget.SavedHotKeyGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
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

public class ResourceConfigHotKeyGroupHandler extends SimplePreparableReloadListener<Loader> {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder()
	  .registerTypeAdapter(ResourceHotKeyGroupsDescriptor.class, new ResourceHotKeyGroupsDescriptor.Serializer())
	  .create();
	private static final TypeToken<ResourceHotKeyGroupsDescriptor> TYPE =
	  new TypeToken<>() {};
	public static final ResourceConfigHotKeyGroupHandler INSTANCE = new ResourceConfigHotKeyGroupHandler();
	private final Map<String, ResourceSavedHotKeyGroup> groupRegistry = Maps.newLinkedHashMap();
	
	public List<ResourceSavedHotKeyGroup> getResourceHotKeyGroups() {
		return new ArrayList<>(groupRegistry.values());
	}
	
	@Override protected @NotNull Loader prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
		Loader l = new Loader();
		profiler.startTick();
		for (String namespace: manager.getNamespaces()) {
			profiler.push(namespace);
			try {
				for (Resource index: manager.getResourceStack(new ResourceLocation(namespace, "config-hotkeys.json"))) {
					profiler.push(index.sourcePackId());
					try (
					  InputStream is = index.open();
					  Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)
					) {
						profiler.push("parse");
						ResourceHotKeyGroupsDescriptor desc = GsonHelper.fromJson(GSON, r, TYPE);
						profiler.popPush("register");
						if (desc != null) l.registerGroups(namespace, desc);
						profiler.pop();
					} catch (RuntimeException e) {
						LOGGER.warn("Invalid config-hotkeys.json in resourcepack: '{}'", index.sourcePackId(), e);
					}
					profiler.pop();
				}
			} catch (IOException ignored) {}
			profiler.pop();
		}
		profiler.endTick();
		return l;
	}
	
	@Override protected void apply(@NotNull Loader l, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
		groupRegistry.clear();
		l.getGroups().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
		  e -> groupRegistry.put(e.getKey(), e.getValue()));
	}
	
	public static class Loader {
		private final Map<String, ResourceSavedHotKeyGroup> groupMap = Maps.newHashMap();
		public void registerGroups(String namespace, ResourceHotKeyGroupsDescriptor descriptor) {
			for (String name: descriptor.groups()) {
				String groupName = namespace + ":" + name;
				groupMap.put(groupName, SavedHotKeyGroup.resource(
				  groupName, new ResourceLocation(namespace, "config-hotkeys/" + name + ".yaml")));
			}
			descriptor.defaultGroups().stream().sorted().forEach(name -> {
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
	
	public record ResourceHotKeyGroupsDescriptor(
	  Set<String> groups, Set<String> defaultGroups
	) {
		public static class Serializer
		  implements JsonDeserializer<ResourceHotKeyGroupsDescriptor>,
		             JsonSerializer<ResourceHotKeyGroupsDescriptor> {
			@Override public ResourceHotKeyGroupsDescriptor deserialize(
			  JsonElement json, Type type, JsonDeserializationContext ctx
			) throws JsonParseException {
				JsonObject obj = GsonHelper.convertToJsonObject(json, "config hotkeys");
				JsonArray arr = GsonHelper.getAsJsonArray(obj, "groups", new JsonArray());
				Set<String> set = new HashSet<>();
				if (arr != null) for (JsonElement item: arr)
					set.add(GsonHelper.convertToString(item, "group name"));
				arr = GsonHelper.getAsJsonArray(obj, "default_groups", new JsonArray());
				Set<String> def = new HashSet<>();
				if (arr != null) for (JsonElement item: arr)
					def.add(GsonHelper.convertToString(item, "group name"));
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
