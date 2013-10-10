package org.strategoxt.imp.testing.strategies;

import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.sunshine.services.language.ALanguage;
import org.spoofax.sunshine.services.language.LanguageService;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * Retrieves the names of the reference resolver strategies of the given language.
 * These functions can be invoked with the plugin-strategy-involve strategy.
 * 
 * @author Volker Lanting
 *
 */
public class get_reference_resolvers_0_1 extends Strategy { 

	public static get_reference_resolvers_0_1 instance = new get_reference_resolvers_0_1();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm unused,
			IStrategoTerm language) {
		ALanguage lang = LanguageService.INSTANCE().getLanguageByName(Tools.asJavaString(language));
		String[] resolverStrings = lang.getResolverFunctions();
		ITermFactory f = context.getFactory();
		IStrategoTerm[] resolvers = new IStrategoTerm[resolverStrings.length];
		for (int i = 0; i < resolverStrings.length; i++) {
			resolvers[i] = f.makeString(resolverStrings[i]);
		}
		return f.makeList(resolvers);
	}
}
