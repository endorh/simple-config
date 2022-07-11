package endorh.simpleconfig.ui.gui.widget.combobox;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import net.minecraft.util.text.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.min;

public abstract class AbstractComboBoxModel<T> implements IComboBoxModel<T> {
	/**
	 * Splits word parts
	 */
	protected static Pattern TOKEN_SPLITTER = Pattern.compile("[\\s_]++|(?<=[a-z])(?=[A-Z])");
	
	/**
	 * Extract a formatted substring from an {@link ITextComponent}.<br>
	 * Should be called on the client side only, if the component may contain translations.<br>
	 * <br>
	 * See {@code endorh.util.text.TextUtil} from the {@code endorh-util} mod
	 *
	 * @param text  Component to slice
	 * @param start Start index of the substring.
	 *              Negative values are corrected counting from the end.
	 * @param end   End index of the substring.
	 *              Negative values are corrected counting from the end.
	 *              Defaults to the end of the component.
	 */
	protected static IFormattableTextComponent subText(ITextComponent text, int start, int end) {
		final int n = text.getString().length();
		if (start > n)
			throw new StringIndexOutOfBoundsException("Index: " + start + ", Length: " + n);
		if (start < 0) {
			if (n + start < 0)
				throw new StringIndexOutOfBoundsException("Index: " + start + ", Length: " + n);
			start = n + start;
		}
		if (end > n) throw new StringIndexOutOfBoundsException("Index: " + end + ", Length: " + n);
		if (end < 0) {
			if (n + end < 0)
				throw new StringIndexOutOfBoundsException("Index: " + end + ", Length: " + n);
			end = n + end;
		}
		if (end <= start) return new StringTextComponent("");
		boolean started = false;
		final List<ITextComponent> siblings = text.getSiblings();
		IFormattableTextComponent res = new StringTextComponent("");
		String str = text.getContents();
		if (start < str.length()) {
			started = true;
			res = res.append(new StringTextComponent(
			  str.substring(start, Math.min(str.length(), end))).setStyle(text.getStyle()));
			if (end < str.length()) return res;
		}
		int o = str.length();
		for (ITextComponent sibling : siblings) {
			str = sibling.getContents();
			if (started || start - o < str.length()) {
				res = res.append(new StringTextComponent(
				  str.substring(started ? 0 : start - o, Math.min(str.length(), end - o))
				).setStyle(sibling.getStyle()));
				started = true;
				if (end - o < str.length()) return res;
			}
			o += str.length();
		}
		return res;
	}
	
	/**
	 * Matches at word part starts
	 */
	protected static List<String> tokenMatches(String target, String query) {
		query = query.trim();
		target = target.trim();
		if (query.length() > target.length())
			return Collections.emptyList();
		final String[] q = TOKEN_SPLITTER.split(query);
		final String[] t = TOKEN_SPLITTER.split(target);
		if (t.length == 0)
			return Collections.emptyList();
		List<String> result = Lists.newArrayList();
		int r = -1;
		String rem;
		for (String qq : q) {
			qq = qq.toLowerCase();
			if (++r < t.length) {
				rem = t[r];
			} else return Collections.emptyList();
			while (!qq.isEmpty()) {
				int j = 0;
				int m = min(qq.length(), rem.length());
				while (j < m && qq.charAt(j) == Character.toLowerCase(rem.charAt(j))) j++;
				if (j == 0) {
					if (++r < t.length) {
						rem = t[r];
						continue;
					} else return Collections.emptyList();
				}
				result.add(rem.substring(0, j));
				qq = qq.substring(j);
				if (qq.isEmpty())
					break;
				if (++r < t.length) {
					rem = t[r];
				} else return Collections.emptyList();
			}
		}
		return result;
	}
	
	@Override public Pair<List<T>, List<ITextComponent>> pickAndDecorateSuggestions(
	  ITypeWrapper<T> typeWrapper, String query, List<T> suggestions
	) {
		if (query.isEmpty())
			return Pair.of(suggestions, suggestions.stream()
			  .map(typeWrapper::getDisplayName).collect(Collectors.toList()));
		if (suggestions.isEmpty()) return Pair.of(suggestions, new ArrayList<>());
		Set<T> set = new LinkedHashSet<>();
		List<ITextComponent> names = new ArrayList<>();
		suggestions.stream()
		  .map(e -> {
			  final String n = typeWrapper.getName(e);
			  return Triple.of(e, n, tokenMatches(n, query));
		  }).filter(t -> !t.getRight().isEmpty())
		  .sorted(Comparator.<Triple<T, String, List<String>>>comparingInt(
			 t -> t.getRight().stream().mapToInt(String::length).reduce(0, (a, b) -> a * b)
		  ).thenComparingInt(t -> t.getMiddle().length()))
		  .forEachOrdered(t -> {
			  final T value = t.getLeft();
			  if (set.add(value)) {
				  final String name = t.getMiddle();
				  String n = name;
				  final String[] sp = TOKEN_SPLITTER.split(name);
				  final List<String> matches = t.getRight();
				  int i = 0, o = 0;
				  IFormattableTextComponent stc = new StringTextComponent("");
				  for (final String frag : sp) {
					  if (i >= matches.size()) break;
					  final int s = n.indexOf(frag);
					  if (s > 0) {
						  stc = stc.append(getNonMatch(typeWrapper, value, name, o, n.substring(0, s)));
						  o += s;
						  n = n.substring(s);
					  }
					  final String tar = matches.get(i);
					  final int j = frag.indexOf(tar);
					  if (j == -1) {
						  stc = stc.append(getNonMatch(typeWrapper, value, name, o, frag));
					  } else {
						  stc = stc.append(getNonMatch(typeWrapper, value, name, o, frag.substring(0, j)))
							 .append(getMatch(typeWrapper, value, name, o, frag, o + j, tar))
							 .append(getNonMatch(typeWrapper, value, name, o + j + tar.length(),
							                     frag.substring(j + tar.length())));
						  i++;
					  }
					  o += frag.length();
					  n = n.substring(frag.length());
				  }
				  stc = stc.append(getNonMatch(typeWrapper, value, name, o, n));
				  names.add(stc);
			  }
		  });
		suggestions.stream()
		  .filter(e -> !set.contains(e))
		  .map(e -> Pair.of(e, typeWrapper.getName(e)))
		  .filter(p -> p.getRight().contains(query))
		  .sorted(Comparator
			         .<Pair<T, String>>comparingInt(p -> p.getRight().length())
			         .thenComparingInt(p -> p.getRight().indexOf(query))
			         .thenComparing(Pair::getRight))
		  .forEachOrdered(p -> {
			  final T value = p.getKey();
			  if (set.add(value)) {
				  final String name = p.getRight();
				  final int i = name.indexOf(query);
				  names.add(getNonMatch(typeWrapper, value, name, 0, name.substring(0, i)).copy()
					           .append(getMatch(typeWrapper, value, name, 0, name, i, query))
					           .append(getNonMatch(typeWrapper, value, name, i + query.length(),
					                               name.substring(i + query.length()))));
			  }
		  });
		return Pair.of(new ArrayList<>(set), names);
	}
	
	protected Style getMatchStyle() {
		return Style.EMPTY.applyFormats(TextFormatting.BLUE);
	}
	
	protected ITextComponent getMatch(
	  ITypeWrapper<T> typeWrapper, T item, String name, int fragmentPos,
	  String fragment, int matchPos, String match
	) {
		return new StringTextComponent(match).setStyle(getMatchStyle());
	}
	
	protected ITextComponent getNonMatch(
	  ITypeWrapper<T> typeWrapper, T item, String name, int fragmentPos, String fragment
	) {
		final ITextComponent title = typeWrapper.getDisplayName(item);
		if (!title.getString().equals(name))
			return new StringTextComponent(fragment);
		return subText(title, fragmentPos, fragmentPos + fragment.length());
	}
}
