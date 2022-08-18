package endorh.simpleconfig.yaml;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.utils.TransformingMap;
import endorh.simpleconfig.core.SimpleConfigImpl;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.electronwill.nightconfig.core.NullObject.NULL_OBJECT;

public class SimpleConfigCommentedYamlParser implements ConfigParser<CommentedConfig> {
	private final Yaml yaml;
	private final ConfigFormat<CommentedConfig> configFormat;
	private final SimpleConfigImpl config;
	private boolean parseComments = true;
	
	public SimpleConfigCommentedYamlParser(SimpleConfigCommentedYamlFormat format) {
		this.yaml = format.getYaml();
		this.configFormat = format;
		this.config = format.getSimpleConfig();
	}
	
	@Override public ConfigFormat<CommentedConfig> getFormat() {
		return configFormat;
	}
	
	@Override public CommentedConfig parse(Reader reader) {
		CommentedConfig config = configFormat.createConfig();
		parse(reader, config, ParsingMode.MERGE);
		return config;
	}
	
	@Override public void parse(
	  Reader reader, Config destination, ParsingMode parsingMode
	) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(reader, bos, StandardCharsets.UTF_8);
			byte[] bytes = bos.toByteArray();
			BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
			
			//noinspection unchecked
			Map<String, Object> wrappedMap = wrap(yaml.loadAs(r, Map.class));
			parsingMode.prepareParsing(destination);
			
			// Add entries
			if (parsingMode == ParsingMode.ADD) {
				for (Map.Entry<String, Object> entry : wrappedMap.entrySet())
					destination.valueMap().putIfAbsent(entry.getKey(), entry.getValue());
			} else destination.valueMap().putAll(wrappedMap);
			
			// Load comments
			if (config != null && isParseComments()) {
				r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
				Map<String, NodeComments> comments = parseYamlComments(yaml.compose(r));
				config.loadComments(comments);
			}
		} catch (RuntimeException | IOException e) {
			throw new ParsingException("YAML parsing failed", e);
		}
	}
	
	public Map<String, NodeComments> parseYamlComments(Node node) {
		Map<String, NodeComments> map = new HashMap<>();
		if (node != null) {
			NodeComments rootComments = NodeComments.from(node);
			if (rootComments.isNotEmpty()) map.put("", rootComments);
			if (node instanceof MappingNode) {
				MappingNode mNode = (MappingNode) node;
				for (NodeTuple tuple : mNode.getValue()) {
					Node keyNode = tuple.getKeyNode();
					if (keyNode instanceof ScalarNode) {
						String key = ((ScalarNode) keyNode).getValue();
						readChildYamlComments(key, tuple, map);
					}
				}
			}
		}
		return map;
	}
	
	public void readChildYamlComments(
	  String path, NodeTuple tuple, Map<String, NodeComments> destination
	) {
		NodeComments comments = NodeComments.from(tuple);
		if (comments.isNotEmpty()) destination.put(path, comments);
		Node valueNode = tuple.getValueNode();
		if (valueNode instanceof MappingNode) {
			MappingNode mNode = (MappingNode) valueNode;
			for (NodeTuple childTuple : mNode.getValue()) {
				Node childKeyNode = childTuple.getKeyNode();
				if (childKeyNode instanceof ScalarNode) {
					String childKey = ((ScalarNode) childKeyNode).getValue();
					readChildYamlComments(path + "." + childKey, childTuple, destination);
				}
			}
		}
	}
	
	private Map<String, Object> wrap(Map<String, Object> map) {
		return new TransformingMap<>(
		  map != null? map : new LinkedHashMap<>(),
		  this::wrap, v -> v,
		  v -> v);
	}
	
	private Object wrap(Object value) {
		if (value instanceof Map && !(value instanceof NonConfigMap)) {
			//noinspection unchecked
			Map<String, Object> map = wrap((Map<String, Object>) value);
			return CommentedConfig.wrap(map, configFormat);
		}
		return value == null ? NULL_OBJECT : value;
	}
	public boolean isParseComments() {
		return parseComments;
	}
	public void setParseComments(boolean parseComments) {
		this.parseComments = parseComments;
	}
}
