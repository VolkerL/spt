package org.strategoxt.imp.testing.strategies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.metaborg.sunshine.services.language.ALanguage;
import org.metaborg.sunshine.services.language.LanguageService;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;


/**
 * Retrieves the name of the Observer strategy of the given language.
 * This can then be used by the plugin-strategy-invoke strategy.
 * 
 * Assumptions:
 * - the Sunshine analysis function is assumed to be the observer strategy
 * 
 * @author Volker Lanting
 *
 */
public class get_observer_0_1 extends Strategy {

	public static get_observer_0_1 instance = new get_observer_0_1();

	private static Logger LOG = LogManager.getLogger(get_observer_0_1.class); 
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm unused, IStrategoTerm language) {
		LOG.info("Getting observer for language {}", language);
		ALanguage lang = LanguageService.INSTANCE().getLanguageByName(Tools.asJavaString(language));
		return context.getFactory().makeString(lang.getAnalysisFunction());
	}

}
