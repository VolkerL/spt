package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.sunshine.parser.model.IParserConfig;
import org.spoofax.sunshine.parser.model.ParserConfig;
import org.spoofax.sunshine.services.language.ALanguage;
import org.spoofax.sunshine.services.language.LanguageService;
import org.spoofax.sunshine.services.parser.JSGLRI;
import org.strategoxt.imp.testing.SpoofaxTestingJSGLRI;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * parse-spt-string strategy to get AST of Spoofax-Testing testsuite, where the
 * input fragments have been annotated with the AST of the input.
 * 
 * The current term is the string to parse and the sole term argument is an
 * absolute path to the file this string is coming from.
 */
public class parse_spt_file_0_0 extends Strategy {

	public static parse_spt_file_0_0 instance = new parse_spt_file_0_0();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		if (!isTermString(current))
			return null;
		String filename = ((IStrategoString) current).stringValue();
		File file = new File(filename);

		System.out.println("\n\n\nPARSE_FILE START\n");
		
		ALanguage l = LanguageService.INSTANCE().getLanguageByName(
				"Spoofax-Testing");
		
		System.out.println("Obtained language: "+l.getName());
		
		IParserConfig c = new ParserConfig(l.getStartSymbol(),
				l.getParseTableProvider(), 24 * 1000);
		JSGLRI p = new JSGLRI(c, file);
		SpoofaxTestingJSGLRI parser = new SpoofaxTestingJSGLRI(p);
		parser.setUseRecovery(false);
		
		System.out.println("Created parser.");
		
		try {
			IStrategoTerm res = parser.actuallyParse(FileUtils.readFileToString(file), filename);
			System.out.println(res);
			System.out.println("\nPARSE_FILE END\n\n");
			return res;
		} catch (SGLRException | InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
