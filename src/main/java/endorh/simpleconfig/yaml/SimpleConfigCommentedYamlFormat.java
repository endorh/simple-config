package endorh.simpleconfig.yaml;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import endorh.simpleconfig.core.SimpleConfigImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class SimpleConfigCommentedYamlFormat implements ConfigFormat<CommentedConfig> {
	public static final ThreadLocal<DumperOptions> DEFAULT_DUMPER_OPTIONS = ThreadLocal.withInitial(() -> {
		DumperOptions dump = new DumperOptions();
		dump.setProcessComments(true);
		dump.setIndent(2);
		dump.setDefaultFlowStyle(FlowStyle.BLOCK);
		return dump;
	});
	public static final ThreadLocal<LoaderOptions> DEFAULT_LOADER_OPTIONS = ThreadLocal.withInitial(() -> {
		LoaderOptions load = new LoaderOptions();
		load.setProcessComments(true);
		load.setAllowDuplicateKeys(false);
		return load;
	});
	public static final ThreadLocal<Constructor> DEFAULT_CONSTRUCTOR = ThreadLocal.withInitial(
	  () -> new SimpleConfigYamlConstructor(DEFAULT_LOADER_OPTIONS.get()));
	public static final ThreadLocal<Representer> DEFAULT_REPRESENTER = ThreadLocal.withInitial(
	  () -> new SimpleConfigYamlRepresenter(DEFAULT_DUMPER_OPTIONS.get()));
	public static final ThreadLocal<Yaml> DEFAULT_YAML = ThreadLocal.withInitial(() -> new Yaml(
	  DEFAULT_CONSTRUCTOR.get(),
	  DEFAULT_REPRESENTER.get(),
	  DEFAULT_DUMPER_OPTIONS.get(),
	  DEFAULT_LOADER_OPTIONS.get()
	));
	public static final ThreadLocal<SimpleConfigCommentedYamlFormat> WITHOUT_COMMENTS = ThreadLocal.withInitial(
	  () -> new SimpleConfigCommentedYamlFormat(DEFAULT_YAML.get(), null));
	
	public static SimpleConfigCommentedYamlFormat forConfig(SimpleConfigImpl config) {
		return new SimpleConfigCommentedYamlFormat(DEFAULT_YAML.get(), config);
	}
	
	public static SimpleConfigCommentedYamlFormat withoutComments() {
		return WITHOUT_COMMENTS.get();
	}
	
	public static Yaml getDefaultYaml() {
		return DEFAULT_YAML.get();
	}
	
	public static void registerExtension() {
		// This shouldn't be the default YAML parser
		// FormatDetector.registerExtension("yaml", SimpleConfigCommentedYamlFormat::forConfig);
		// FormatDetector.registerExtension("yml", SimpleConfigCommentedYamlFormat::forConfig);
	}
	
	private final Yaml yaml;
	private final SimpleConfigImpl config;
	
	private SimpleConfigCommentedYamlFormat(Yaml yaml, SimpleConfigImpl config) {
		this.yaml = yaml;
		this.config = config;
	}
	
	public SimpleConfigImpl getSimpleConfig() {
		return config;
	}
	
	@Override
	public SimpleConfigCommentedYamlWriter createWriter() {
		return new SimpleConfigCommentedYamlWriter(this);
	}
	
	public SimpleConfigCommentedYamlWriter createWriter(boolean generateComments) {
		SimpleConfigCommentedYamlWriter writer = createWriter();
		writer.setGenerateComments(generateComments);
		return writer;
	}
	
	@Override
	public SimpleConfigCommentedYamlParser createParser() {
		return new SimpleConfigCommentedYamlParser(this);
	}
	
	public SimpleConfigCommentedYamlParser createParser(boolean parseComments) {
		SimpleConfigCommentedYamlParser parser = createParser();
		parser.setParseComments(parseComments);
		return parser;
	}
	
	@Override public CommentedConfig createConfig(Supplier<Map<String, Object>> mapCreator) {
		return CommentedConfig.of(mapCreator, this);
	}
	
	@Override public boolean supportsComments() {
		return false;
	}
	
	@Override public boolean supportsType(Class<?> type) {
		return type == null
		       || type.isEnum()
		       || type == Boolean.class
		       || type == String.class
		       || type == java.util.Date.class
		       || type == java.sql.Date.class
		       || type == java.sql.Timestamp.class
		       || type == byte[].class
		       || type == Object[].class
		       || Number.class.isAssignableFrom(type)
		       || Set.class.isAssignableFrom(type)
		       || List.class.isAssignableFrom(type)
		       || Map.class.isAssignableFrom(type)
		       || Pair.class.isAssignableFrom(type)
		       || Triple.class.isAssignableFrom(type)
		       || Config.class.isAssignableFrom(type);
	}
	
	public Yaml getYaml() {
		return yaml;
	}
}
