package endorh.simpleconfig.yaml;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.List;
import java.util.Map;

public class SimpleConfigYamlConstructor extends Constructor {
	public static final Tag PAIR = new Tag("!pair");
	public static final Tag TRIPLE = new Tag("!triple");
	
	public SimpleConfigYamlConstructor() {
		init();
	}
	
	public SimpleConfigYamlConstructor(LoaderOptions loadingConfig) {
		super(loadingConfig);
		init();
	}
	
	private void init() {
		// It's not really used, since pairs and triples are serialized as sequences for
		// presentation purposes
		yamlConstructors.put(PAIR, new ConstructPair());
		yamlConstructors.put(TRIPLE, new ConstructTriple());
	}
	
	@Override protected Map<Object, Object> constructMapping(MappingNode node) {
		Map<Object, Object> map = super.constructMapping(node);
		if (map.keySet().stream().anyMatch(k -> !(k instanceof String)))
			return NonConfigMap.wrap(map);
		return map;
	}
	
	@Override protected List<?> constructSequence(SequenceNode node) {
		List<?> list = super.constructSequence(node);
		if (node.getFlowStyle() == FlowStyle.FLOW)
			return FlowList.wrap(list);
		return list;
	}
	
	public class ConstructPair extends AbstractConstruct {
		@Override public Object construct(Node node) {
			if (!(node instanceof SequenceNode)) {
				throw new ConstructorException(
				  "while constructing a pair",
				  node.getStartMark(), "expected a sequence, but found " + node.getNodeId(),
				  node.getStartMark());
			}
			SequenceNode snode = (SequenceNode) node;
			List<Node> values = snode.getValue();
			if (values.size() != 2) {
				throw new ConstructorException(
				  "while constructing a pair",
				  node.getStartMark(), "expected a sequence of length 2, but length was " + values.size(),
				  node.getStartMark());
			}
			return Pair.of(constructObject(values.get(0)), constructObject(values.get(1)));
		}
	}
	
	public class ConstructTriple extends AbstractConstruct {
		@Override public Object construct(Node node) {
			if (!(node instanceof SequenceNode)) {
				throw new ConstructorException(
				  "while constructing a triple",
				  node.getStartMark(), "expected a sequence, but found " + node.getNodeId(),
				  node.getStartMark());
			}
			SequenceNode snode = (SequenceNode) node;
			List<Node> values = snode.getValue();
			if (values.size() != 3) {
				throw new ConstructorException(
				  "while constructing a triple",
				  node.getStartMark(), "expected a sequence of length 3, but length was " + values.size(),
				  node.getStartMark());
			}
			return Triple.of(
			  constructObject(values.get(0)),
			  constructObject(values.get(1)),
			  constructObject(values.get(2)));
		}
	}
	
	public static class ConstructorException extends MarkedYAMLException {
		public ConstructorException(
		  String context, Mark contextMark, String problem, Mark problemMark, Throwable cause
		) {
			super(context, contextMark, problem, problemMark, cause);
		}
		
		public ConstructorException(
		  String context, Mark contextMark, String problem, Mark problemMark
		) {
			this(context, contextMark, problem, problemMark, null);
		}
	}
	
}
