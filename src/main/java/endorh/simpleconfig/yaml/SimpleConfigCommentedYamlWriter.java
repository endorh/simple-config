package endorh.simpleconfig.yaml;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.core.utils.TransformingMap;
import endorh.simpleconfig.core.SimpleConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.Writer;
import java.util.Map;

import static com.electronwill.nightconfig.core.NullObject.NULL_OBJECT;

public class SimpleConfigCommentedYamlWriter implements ConfigWriter {
	
	private final SimpleConfigCommentedYamlFormat format;
	private final Yaml yaml;
	private final SimpleConfig config;
	private boolean generateComments = true;
	
	public SimpleConfigCommentedYamlWriter(SimpleConfigCommentedYamlFormat format) {
		this.format = format;
		this.yaml = format.getYaml();
		this.config = format.getSimpleConfig();
	}
	
	@Override public void write(
	  UnmodifiableConfig config, Writer writer
	) {
		try {
			Map<String, Object> unwrappedMap = unwrap(config);
			Node node = yaml.represent(unwrappedMap);
			if (isGenerateComments())
				attachYamlComments(node, this.config.getComments());
			yaml.serialize(node, writer);
		} catch (RuntimeException e) {
			throw new WritingException("YAML writing failed", e);
		}
	}
	
	public void attachYamlComments(Node root, Map<String, NodeComments> comments) {
		NodeComments rootComments = comments.get("");
		if (rootComments != null)
			rootComments.apply(root);
		if (root instanceof MappingNode) {
			MappingNode mappingNode = (MappingNode) root;
			for (NodeTuple tuple : mappingNode.getValue()) {
				Node keyNode = tuple.getKeyNode();
				if (keyNode instanceof ScalarNode) {
					String key = ((ScalarNode) keyNode).getValue();
					attachChildYamlComments(key, tuple, comments);
				}
			}
		}
	}
	
	public void attachChildYamlComments(
		String path, NodeTuple tuple, Map<String, NodeComments> comments
	) {
		NodeComments nodeComments = comments.get(path);
		if (nodeComments != null) nodeComments.apply(tuple);
		Node value = tuple.getValueNode();
		if (value instanceof MappingNode) {
			MappingNode mappingNode = (MappingNode) value;
			for (NodeTuple childTuple : mappingNode.getValue()) {
				Node childKeyNode = childTuple.getKeyNode();
				if (childKeyNode instanceof ScalarNode) {
					String childKey = ((ScalarNode) childKeyNode).getValue();
					attachChildYamlComments(path + "." + childKey, childTuple, comments);
				}
			}
		}
	}
	
	private static Mark dummyMark() {
		return new Mark("generated", 0, 0, 0, new int[0], 0);
	}
	
	public static CommentLine commentLine(String line) {
		return new CommentLine(dummyMark(), dummyMark(), line, CommentType.BLOCK);
	}
	
	public static CommentLine blankLine() {
		return new CommentLine(dummyMark(), dummyMark(), "", CommentType.BLANK_LINE);
	}
	
	private static Map<String, Object> unwrap(UnmodifiableConfig config) {
		return new TransformingMap<>(config.valueMap(), SimpleConfigCommentedYamlWriter::unwrap, v -> v, v -> v);
	}
	
	private static Object unwrap(Object value) {
		if (value instanceof UnmodifiableConfig)
			return unwrap((UnmodifiableConfig) value);
		return value == NULL_OBJECT ? null : value;
	}
	public boolean isGenerateComments() {
		return generateComments;
	}
	public void setGenerateComments(boolean generateComments) {
		this.generateComments = generateComments;
	}
}
