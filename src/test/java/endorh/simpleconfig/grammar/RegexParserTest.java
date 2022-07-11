package endorh.simpleconfig.grammar;

import endorh.simpleconfig.grammar.regex.RegexLexer;
import endorh.simpleconfig.grammar.regex.RegexParser;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import static java.util.Arrays.asList;

public class RegexParserTest {
	public static void main(String[] args) {
		// Parse
		CharStream stream = CharStreams.fromString("\\s*+(?<group>\\w+?)(?i)(?>a|b)");
		RegexLexer lexer  = new RegexLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		RegexParser parser = new RegexParser(tokenStream);
		ParseTree tree = parser.pattern();
		
		// Show AST in console
		System.out.println(tree.toStringTree(parser));
		
		// Show AST in GUI
		TreeViewer viewer = new TreeViewer(asList(parser.getRuleNames()), tree);
		viewer.open();
	}
}
