package endorh.simpleconfig.yaml;

import endorh.simpleconfig.core.PairList;
import endorh.simpleconfig.ui.hotkey.HotKeyAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypeManager;
import endorh.simpleconfig.ui.hotkey.HotKeyActionWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
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
		yamlConstructors.put(Tag.PAIRS, new ConstructPairList());
		yamlConstructors.put(PAIR, new ConstructPair());
		yamlConstructors.put(TRIPLE, new ConstructTriple());
	}
	
	@Override protected Construct getConstructor(Node node) {
		if (!node.useClassConstructor()) {
			HotKeyActionType<?, ?> type = HotKeyActionTypeManager.INSTANCE.getType(node.getTag());
			if (type != null) return new ConstructHotKeyActionWrapper<>(type);
		}
		return super.getConstructor(node);
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
			if (!(node instanceof SequenceNode)) throw new ConstructorException(
			  "while constructing a pair",
			  node.getStartMark(), "expected a sequence, but found " + node.getNodeId(),
			  node.getStartMark());
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
			if (!(node instanceof SequenceNode)) throw new ConstructorException(
			  "while constructing a triple",
			  node.getStartMark(), "expected a sequence, but found " + node.getNodeId(),
			  node.getStartMark());
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
	
	public class ConstructPairList extends AbstractConstruct {
		@Override
		public Object construct(Node node) {
			if (!(node instanceof SequenceNode)) {
				throw new ConstructorException(
				  "while constructing pairs", node.getStartMark(),
				  "expected a sequence, but found " + node.getNodeId(), node.getStartMark());
			}
			SequenceNode snode = (SequenceNode) node;
			PairList<Object, Object> pairs = new PairList<>(snode.getValue().size());
			for (Node subNode : snode.getValue()) {
				if (!(subNode instanceof MappingNode)) {
					throw new ConstructorException(
					  "while constructing pairs", node.getStartMark(),
					  "expected a mapping of length 1, but found " + subNode.getNodeId(),
					  subNode.getStartMark());
				}
				MappingNode mNode = (MappingNode) subNode;
				if (mNode.getValue().size() != 1) {
					throw new ConstructorException(
					  "while constructing pairs", node.getStartMark(),
					  "expected a single mapping item, but found " + mNode.getValue().size()
					  + " items", mNode.getStartMark());
				}
				Node keyNode = mNode.getValue().get(0).getKeyNode();
				Node valueNode = mNode.getValue().get(0).getValueNode();
				Object key = constructObject(keyNode);
				Object value = constructObject(valueNode);
				pairs.add(Pair.of(key, value));
			}
			return pairs;
		}
	}
	
	public class ConstructHotKeyActionWrapper<T, A extends HotKeyAction<T>> extends AbstractConstruct {
		private final HotKeyActionType<T, A> type;
		public ConstructHotKeyActionWrapper(HotKeyActionType<T, A> type) {
			this.type = type;
		}
		
		@Override public Object construct(Node node) {
			if (!(node instanceof SequenceNode)) {
				throw new ConstructorException(
				  "while constructing hotkey wrapper", node.getStartMark(),
				  "expected a sequence, but found " + node.getNodeId(), node.getStartMark());
			}
			SequenceNode seq = (SequenceNode) node;
			List<Node> nodes = seq.getValue();
			if (nodes.size() > 1) {
				throw new ConstructorException(
				  "while constructing hotkey wrapper", node.getStartMark(),
				  "expected a sequence empty or of length 1, but found size" + nodes.size(),
				  node.getStartMark());
			}
			Object value = nodes.isEmpty()? null :
			               SimpleConfigYamlConstructor.this.constructObject(nodes.get(0));
			return new HotKeyActionWrapper<>(type, value);
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
