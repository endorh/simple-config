package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Maps;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfigResourcePresetHandler.Loader;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

public class SimpleConfigResourcePresetHandler extends SimplePreparableReloadListener<Loader> {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder()
	  .registerTypeAdapter(PresetsDescriptor.class, new PresetsDescriptor.Serializer())
	  .create();
	private static final TypeToken<Map<String, PresetsDescriptor>> TYPE =
	  new TypeToken<Map<String, PresetsDescriptor>>() {};
	public static final SimpleConfigResourcePresetHandler
	  INSTANCE = new SimpleConfigResourcePresetHandler();
	private final Map<String, Map<Preset, CommentedConfig>> presetRegistry = Maps.newHashMap();
	
	public List<Preset> getResourcePresets(String modId) {
		return new ArrayList<>(presetRegistry.getOrDefault(modId, Collections.emptyMap()).keySet());
	}
	
	public CommentedConfig getResourcePreset(String modId, Preset preset) {
		return presetRegistry.get(modId).get(preset);
	}
	
	@Override protected @NotNull Loader prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
		Loader l = new Loader();
		profiler.startTick();
		for (String namespace: manager.getNamespaces()) {
			profiler.push(namespace);
			try {
				for (Resource index: manager.getResources(new ResourceLocation(namespace, "config-presets.json"))) {
					profiler.push(index.getSourceName());
					try (
					  InputStream is = index.getInputStream();
					  Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)
					) {
						profiler.push("parse");
						Map<String, PresetsDescriptor> map = GsonHelper.fromJson(GSON, r, TYPE);
						profiler.popPush("register");
						map.forEach((k, v) -> l.registerPresets(namespace, k, v));
						profiler.pop();
					} catch (RuntimeException e) {
						LOGGER.warn("Invalid config-presets.json in resourcepack: '{}'", index.getSourceName(), e);
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
		presetRegistry.clear();
		l.getPresetMap().forEach((modId, m) -> {
			Map<Preset, CommentedConfig> mm = presetRegistry.computeIfAbsent(modId, i -> Maps.newHashMap());
			SimpleConfigCommentedYamlFormat format;
			if (SimpleConfigImpl.hasConfig(modId, SimpleConfig.Type.CLIENT))
				format = SimpleConfigCommentedYamlFormat.forConfig(
				  SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.CLIENT));
			else if (SimpleConfigImpl.hasConfig(modId, SimpleConfig.Type.SERVER))
				format = SimpleConfigCommentedYamlFormat.forConfig(
				  SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.SERVER));
			else return;
			m.forEach((preset, location) -> {
				try {
					CommentedConfig config = format.createParser(false).parse(
					  new InputStreamReader(manager.getResource(
						 new ResourceLocation(location.getNamespace(), "config-presets/" + location.getPath())
					  ).getInputStream()));
					mm.put(preset, config);
				} catch (YAMLException e) {
					LOGGER.warn("Invalid config preset: " + location, e);
				} catch (IOException ignored) {}
			});
			if (mm.isEmpty()) presetRegistry.remove(modId);
		});
	}
	
	public static class Loader {
		private final Map<String, Map<Preset, ResourceLocation>> presetMap = Maps.newHashMap();
		public void registerPresets(String namespace, String modId, PresetsDescriptor descriptor) {
			Map<Preset, ResourceLocation> map = presetMap.computeIfAbsent(modId, m -> Maps.newHashMap());
			for (String name: descriptor.clientPresets) {
				String fileName = modId + "-client-" + name + ".yaml";
				ResourceLocation r = new ResourceLocation(namespace, fileName);
				Preset preset = Preset.resource(name, SimpleConfig.Type.CLIENT);
				map.put(preset, r);
			}
			for (String name: descriptor.commonPresets) {
				String fileName = modId + "-common-" + name + ".yaml";
				ResourceLocation r = new ResourceLocation(namespace, fileName);
				Preset preset = Preset.resource(name, SimpleConfig.Type.COMMON);
				map.put(preset, r);
			}
			for (String name: descriptor.serverPresets) {
				String fileName = modId + "-server-" + name + ".yaml";
				ResourceLocation r = new ResourceLocation(namespace, fileName);
				Preset preset = Preset.resource(name, SimpleConfig.Type.SERVER);
				map.put(preset, r);
			}
			if (map.isEmpty()) presetMap.remove(modId);
		}
		
		public Map<String, Map<Preset, ResourceLocation>> getPresetMap() {
			return presetMap;
		}
	}
	
	public static class PresetsDescriptor {
		public final Set<String> clientPresets;
		public final Set<String> commonPresets;
		public final Set<String> serverPresets;
		
		public PresetsDescriptor(Set<String> clientPresets, Set<String> commonPresets, Set<String> serverPresets) {
			this.clientPresets = clientPresets;
			this.commonPresets = commonPresets;
			this.serverPresets = serverPresets;
		}
		
		public static class Serializer implements JsonDeserializer<PresetsDescriptor>, JsonSerializer<PresetsDescriptor> {
			@Override public PresetsDescriptor deserialize(
			  JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext ctx
			) throws JsonParseException {
				JsonObject obj = GsonHelper.convertToJsonObject(json, "mod config presets");
				Optional<String> first = obj.entrySet().stream().map(Entry::getKey)
				  .filter(k -> !"client".equals(k) && !"server".equals(k) && !"common".equals(k)).findFirst();
				if (first.isPresent())
					throw new JsonSyntaxException("Unknown preset type: " + first.get());
				JsonArray client = GsonHelper.getAsJsonArray(obj, "client", new JsonArray());
				JsonArray common = GsonHelper.getAsJsonArray(obj, "common", new JsonArray());
				JsonArray server = GsonHelper.getAsJsonArray(obj, "server", new JsonArray());
				Set<String> clientPresets = new HashSet<>();
				Set<String> commonPresets = new HashSet<>();
				Set<String> serverPresets = new HashSet<>();
				if (client != null) for (JsonElement item: client)
					clientPresets.add(GsonHelper.convertToString(item, "preset name"));
				if (common != null) for (JsonElement item: common)
					commonPresets.add(GsonHelper.convertToString(item, "preset name"));
				if (server != null) for (JsonElement item: server)
					serverPresets.add(GsonHelper.convertToString(item, "preset name"));
				return new PresetsDescriptor(clientPresets, commonPresets, serverPresets);
			}
			
			@Override public JsonElement serialize(
			  PresetsDescriptor src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext ctx
			) {
				JsonObject obj = new JsonObject();
				List<String> clientPresets = new ArrayList<>(src.clientPresets);
				List<String> commonPresets = new ArrayList<>(src.commonPresets);
				List<String> serverPresets = new ArrayList<>(src.serverPresets);
				obj.add("client", ctx.serialize(clientPresets));
				obj.add("common", ctx.serialize(commonPresets));
				obj.add("server", ctx.serialize(serverPresets));
				return obj;
			}
		}
	}
}
