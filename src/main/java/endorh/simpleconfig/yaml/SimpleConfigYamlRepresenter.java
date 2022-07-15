package endorh.simpleconfig.yaml;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

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
		representers.remove(null); // Remove Java Beans
		multiRepresenters.put(Pair.class, new RepresentPair());
		multiRepresenters.put(Triple.class, new RepresentTriple());
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
	
	public boolean isRepresentPairsAsSequences() {
		return representPairsAsSequences;
	}
	
	public void setRepresentPairsAsSequences(boolean representPairsAsSequences) {
		this.representPairsAsSequences = representPairsAsSequences;
	}
}
