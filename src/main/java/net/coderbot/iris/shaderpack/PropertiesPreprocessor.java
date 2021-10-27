package net.coderbot.iris.shaderpack;

import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.shader.StandardMacros;
import org.anarres.cpp.DefaultPreprocessorListener;
import org.anarres.cpp.Feature;
import org.anarres.cpp.Preprocessor;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;

import java.nio.file.Path;

public class PropertiesPreprocessor {
	public static String process(Path rootPath, Path shaderPath, String source) {
		source = "#define MC_VERSION " + StandardMacros.getMcVersion() + "\n" + source;

		return preprocessSource(source);
	}

	// Derived from ShaderProcessor.glslPreprocessSource, which is derived from GlShader from Canvas, licenced under LGPL
	public static String preprocessSource(String source) {
		@SuppressWarnings("resource")
		final Preprocessor pp = new Preprocessor();
		pp.setListener(new DefaultPreprocessorListener());
		pp.addInput(new StringLexerSource(source, true));
		pp.addFeature(Feature.KEEPCOMMENTS);

		final StringBuilder builder = new StringBuilder();

		try {
			for (;;) {
				final Token tok = pp.token();
				if (tok == null) break;
				if (tok.getType() == Token.EOF) break;
				builder.append(tok.getText());
			}
		} catch (final Exception e) {
			Iris.logger.error("Properties pre-processing failed", e);
		}

		builder.append("\n");

		source = builder.toString();

		// strip leading whitespace before newline, makes next change more reliable
		source = source.replaceAll("[ \t]*[\r\n]", "\n");
		// consolidate newlines
		source = source.replaceAll("\n{2,}", "\n\n");

		return source;
	}
}
