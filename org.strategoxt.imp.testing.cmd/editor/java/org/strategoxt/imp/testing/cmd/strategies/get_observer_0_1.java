package org.strategoxt.imp.testing.cmd.strategies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.spoofax.core.service.stratego.StrategoFacet;
import org.metaborg.sunshine.environment.ServiceRegistry;
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
		ILanguage lang = ServiceRegistry.INSTANCE().getService(ILanguageService.class).get(Tools.asJavaString(language));
		return context.getFactory().makeString(lang.facet(StrategoFacet.class).analysisStrategy());
	}

}