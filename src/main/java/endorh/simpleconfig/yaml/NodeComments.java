package endorh.simpleconfig.yaml;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.blankLine;
import static java.util.Arrays.asList;

public class NodeComments {
	private @Nullable List<CommentLine> blockComments;
	private @Nullable List<CommentLine> keyInLineComments;
	private @Nullable List<CommentLine> valueBlockComments;
	private @Nullable List<CommentLine> inLineComments;
	private @Nullable List<CommentLine> endComments;
	
	public NodeComments() {
		this(null, null, null, null, null);
	}
	
	public NodeComments(
	  @Nullable List<CommentLine> blockComments, @Nullable List<CommentLine> keyInLineComments,
	  @Nullable List<CommentLine> valueBlockComments, @Nullable List<CommentLine> inLineComments,
	  @Nullable List<CommentLine> endComments
	) {
		this.blockComments = blockComments == null || blockComments.isEmpty()? null : blockComments;
		this.keyInLineComments =
		  keyInLineComments == null || keyInLineComments.isEmpty()? null : keyInLineComments;
		this.valueBlockComments =
		  valueBlockComments == null || valueBlockComments.isEmpty()? null : valueBlockComments;
		this.inLineComments =
		  inLineComments == null || inLineComments.isEmpty()? null : inLineComments;
		this.endComments = endComments == null || endComments.isEmpty()? null : endComments;
	}
	
	public @Nullable List<CommentLine> getBlockComments() {
		return blockComments;
	}
	
	public void setBlockComments(@Nullable List<CommentLine> blockComments) {
		this.blockComments = blockComments;
	}
	
	public @Nullable List<CommentLine> getKeyInLineComments() {
		return keyInLineComments;
	}
	
	public void setKeyInLineComments(@Nullable List<CommentLine> keyInLineComments) {
		this.keyInLineComments = keyInLineComments;
	}
	
	public @Nullable List<CommentLine> getValueBlockComments() {
		return valueBlockComments;
	}
	
	public void setValueBlockComments(@Nullable List<CommentLine> valueBlockComments) {
		this.valueBlockComments = valueBlockComments;
	}
	
	public @Nullable List<CommentLine> getInLineComments() {
		return inLineComments;
	}
	
	public void setInLineComments(@Nullable List<CommentLine> inLineComments) {
		this.inLineComments = inLineComments;
	}
	
	public @Nullable List<CommentLine> getEndComments() {
		return endComments;
	}
	
	public void setEndComments(@Nullable List<CommentLine> endComments) {
		this.endComments = endComments;
	}
	
	public List<CommentLine> getAllLines() {
		return Stream.of(
		  getBlockComments(), getKeyInLineComments(), getValueBlockComments(),
		  getInLineComments(), getEndComments()
		).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
	}
	
	public void addSeparatorLine() {
		List<CommentLine> comments = getBlockComments();
		if (comments == null) comments = new ArrayList<>();
		if (comments.isEmpty() || comments.get(0).getCommentType() != CommentType.BLANK_LINE)
			comments.add(0, blankLine());
		setBlockComments(comments);
	}
	
	public NodeComments appendAsPrefix(@Nullable NodeComments comments) {
		return appendAsPrefix(comments, true);
	}
	
	public NodeComments appendAsPrefix(@Nullable NodeComments comments, boolean addBlankLine) {
		if (!isNotEmpty()) return comments;
		if (comments == null) comments = new NodeComments();
		List<CommentLine> blockComments = comments.getBlockComments();
		if (blockComments == null) blockComments = new ArrayList<>();
		List<CommentLine> list = getAllLines();
		if (!list.isEmpty()) {
			if (addBlankLine) list.add(blankLine());
			blockComments.addAll(0, list);
		}
		comments.setBlockComments(blockComments);
		return comments;
	}
	
	public NodeComments appendAtEnd(@Nullable NodeComments comments) {
		return appendAtEnd(comments, true);
	}
	
	public NodeComments appendAtEnd(@Nullable NodeComments comments, boolean addBlankLine) {
		if (!isNotEmpty()) return comments;
		if (comments == null) comments = new NodeComments();
		List<CommentLine> endComments = comments.getEndComments();
		if (endComments == null) endComments = new ArrayList<>();
		List<CommentLine> list = getAllLines();
		if (!list.isEmpty()) {
			if (addBlankLine) endComments.add(blankLine());
			endComments.addAll(list);
		}
		comments.setEndComments(endComments);
		return comments;
	}
	
	public static NodeComments prefix(CommentLine... lines) {
		return new NodeComments(asList(lines), null, null, null, null);
	}
	
	public static NodeComments suffix(CommentLine... lines) {
		return new NodeComments(null, null, null, null, asList(lines));
	}
	
	public static NodeComments from(Node node) {
		List<CommentLine> blockComments = node.getBlockComments();
		List<CommentLine> inLineComments = node.getInLineComments();
		List<CommentLine> endComments = node.getEndComments();
		return new NodeComments(
		  blockComments, null, null,
		  inLineComments, endComments);
	}
	
	public static NodeComments from(NodeTuple tuple) {
		Node keyNode = tuple.getKeyNode();
		Node valueNode = tuple.getValueNode();
		List<CommentLine> blockComments = keyNode.getBlockComments();
		List<CommentLine> keyInLineComments = keyNode.getInLineComments();
		List<CommentLine> valueBlockComments = valueNode.getBlockComments();
		List<CommentLine> inLineComments = valueNode.getInLineComments();
		List<CommentLine> endComments = valueNode.getEndComments();
		return new NodeComments(
		  blockComments, keyInLineComments,
		  valueBlockComments, inLineComments, endComments);
	}
	
	public void apply(NodeTuple tuple) {
		Node keyNode = tuple.getKeyNode();
		Node valueNode = tuple.getValueNode();
		keyNode.setBlockComments(getBlockComments());
		keyNode.setInLineComments(getKeyInLineComments());
		valueNode.setBlockComments(getValueBlockComments());
		valueNode.setInLineComments(getInLineComments());
		valueNode.setEndComments(getEndComments());
	}
	
	public void apply(Node node) {
		node.setBlockComments(getBlockComments());
		node.setInLineComments(getInLineComments());
		node.setEndComments(getEndComments());
	}
	
	public boolean isNotEmpty() {
		return blockComments != null || keyInLineComments != null
		       || valueBlockComments != null || inLineComments != null
		       || endComments != null;
	}
}
