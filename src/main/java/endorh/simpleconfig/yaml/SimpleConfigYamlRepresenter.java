package endorh.simpleconfig.yaml;

import endorh.simpleconfig.core.PairList;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class SimpleConfigYamlRepresenter extends Representer {
	private boolean representPairsAsSequences = true;
	
	public SimpleConfigYamlRepresenter() {
		init();
	}
	
	public SimpleConfigYamlRepresenter(DumperOptions options) {
		super(options);
		init();
	}
	
	private void init() {
		// representers.remove(null); // Remove Java Beans
		multiRepresenters.put(Pair.class, new RepresentPair());
		multiRepresenters.put(Triple.class, new RepresentTriple());
		Represent list = multiRepresenters.remove(List.class);
		multiRepresenters.put(PairList.class, new RepresentPairList());
		multiRepresenters.put(List.class, list);
		multiRepresenters.put(HotKeyActionWrapper.class, new RepresentHotKeyActionWrapper());
	}
	
	@Override protected Node representSequence(Tag tag, Iterable<?> sequence, FlowStyle flowStyle) {
		if (sequence instanceof FlowList) flowStyle = FlowStyle.FLOW;
		return super.representSequence(tag, sequence, flowStyle);
	}
	
	public class RepresentPair implements Represent {
		@Override public Node representData(Object data) {
			Pair<?, ?> pair = (Pair<?, ?>) data;
			return representSequence(
			  representPairsAsSequences? Tag.SEQ : SimpleConfigYamlConstructor.PAIR,
			  FlowList.wrap(asList(pair.getLeft(), pair.getRight())), FlowStyle.FLOW);
		}
	}
	
	public class RepresentTriple implements Represent {
		@Override public Node representData(Object data) {
			Triple<?, ?, ?> triple = (Triple<?, ?, ?>) data;
			return representSequence(
			  representPairsAsSequences? Tag.SEQ : SimpleConfigYamlConstructor.TRIPLE,
			  FlowList.wrap(asList(triple.getLeft(), triple.getMiddle(), triple.getRight())),
			  FlowStyle.FLOW);
		}
	}
	
	public class RepresentPairList implements Represent {
		@Override public Node representData(Object data) {
			PairList<?, ?> list = (PairList<?, ?>) data;
			return representSortedMapping(Tag.PAIRS, list, FlowStyle.BLOCK);
		}
	}
	
	public class RepresentHotKeyActionWrapper implements Represent {
		@Override public Node representData(Object data) {
			HotKeyActionWrapper<?, ?> wrapper = (HotKeyActionWrapper<?, ?>) data;
			HotKeyActionType<?, ?> type = wrapper.getType();
			Tag tag = type.getTag();
			Object value = wrapper.getValue();
			if (value == null) return new SequenceNode(tag, Collections.emptyList(), FlowStyle.FLOW);
			Node node = SimpleConfigYamlRepresenter.this.representData(value);
			FlowStyle style = FlowStyle.BLOCK;
			if (node instanceof ScalarNode scalar) {
				if (scalar.getScalarStyle() == ScalarStyle.PLAIN || scalar.getScalarStyle() == ScalarStyle.SINGLE_QUOTED)
					style = FlowStyle.FLOW;
			}
			return new SequenceNode(tag, Collections.singletonList(node), style);
		}
	}
	
	public SequenceNode representSortedMapping(
	  Tag tag, Iterable<? extends Map.Entry<?, ?>> map, FlowStyle flowStyle
	) {
		if (flowStyle == FlowStyle.AUTO) flowStyle = FlowStyle.BLOCK;
		int size = map instanceof List? ((List<?>) map).size() : 10;
		List<Node> nodes = new ArrayList<>(size);
		SequenceNode node = new SequenceNode(tag, nodes, flowStyle);
		for (Map.Entry<?, ?> entry: map) {
			Node keyNode = representData(entry.getKey());
			Node valueNode = representData(entry.getValue());
			NodeTuple tuple = new NodeTuple(keyNode, valueNode);
			MappingNode mapping = new MappingNode(
			  Tag.MAP, Collections.singletonList(tuple), flowStyle);
			nodes.add(mapping);
		}
		return node;
	}
	
	public boolean isRepresentPairsAsSequences() {
		return representPairsAsSequences;
	}
	
	public void setRepresentPairsAsSequences(boolean representPairsAsSequences) {
		this.representPairsAsSequences = representPairsAsSequences;
	}
}
