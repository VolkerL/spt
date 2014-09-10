package org.strategoxt.imp.testing;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermList;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.termAt;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.IOException;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.metaborg.sunshine.Environment;
import org.metaborg.sunshine.parser.model.ParserConfig;
import org.metaborg.sunshine.services.language.ALanguage;
import org.metaborg.sunshine.services.language.LanguageService;
import org.metaborg.sunshine.services.parser.JSGLRI;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermTransformer;
import org.spoofax.terms.TermVisitor;
import org.spoofax.terms.attachments.ParentAttachment;
import org.spoofax.terms.attachments.ParentTermFactory;

public class SpoofaxTestingJSGLRI extends JSGLRI {
	
	private static final int PARSE_TIMEOUT = 20 * 1000;
	
	private static final IStrategoConstructor INPUT_4 =
		Environment.INSTANCE().termFactory.makeConstructor("Input", 4);
	
	private static final IStrategoConstructor OUTPUT_4 =
		Environment.INSTANCE().termFactory.makeConstructor("Output", 4);
	
	private static final IStrategoConstructor ERROR_1 =
		Environment.INSTANCE().termFactory.makeConstructor("Error", 1);
	
	private static final IStrategoConstructor LANGUAGE_1 =
		Environment.INSTANCE().termFactory.makeConstructor("Language", 1);

	private static final IStrategoConstructor TARGET_LANGUAGE_1 =
		Environment.INSTANCE().termFactory.makeConstructor("TargetLanguage", 1);

	private static final IStrategoConstructor SETUP_3 =
		Environment.INSTANCE().termFactory.makeConstructor("Setup", 3);

	private static final IStrategoConstructor TARGET_SETUP_3 =
		Environment.INSTANCE().termFactory.makeConstructor("TargetSetup", 3);

	private static final IStrategoConstructor TOPSORT_1 =
		Environment.INSTANCE().termFactory.makeConstructor("TopSort", 1);

	private static final IStrategoConstructor TARGET_TOPSORT_1 =
		Environment.INSTANCE().termFactory.makeConstructor("TargetTopSort", 1);
	
	private static final Logger LOG = LogManager.getLogger(SpoofaxTestingJSGLRI.class);
	
	private final FragmentParser fragmentParser = new FragmentParser(SETUP_3, TOPSORT_1);
	
	private final FragmentParser outputFragmentParser = new FragmentParser(TARGET_SETUP_3, TARGET_TOPSORT_1);

	private final SelectionFetcher selections = new SelectionFetcher();

	public SpoofaxTestingJSGLRI(JSGLRI template) {
		super(new ParserConfig(
				template.getConfig().getStartSymbol(),
				template.getConfig().getParseTableProvider(),
				PARSE_TIMEOUT),
				template.getFile());
		setUseRecovery(true);
	}
	
	@Override
	public IStrategoTerm actuallyParse(String input, String filename) throws InterruptedException, SGLRException {
		IStrategoTerm ast = super.actuallyParse(input, filename);
		return parseTestedFragments(ast);
	}

	private IStrategoTerm parseTestedFragments(final IStrategoTerm root) {
		final Tokenizer oldTokenizer = (Tokenizer) getTokenizer(root);
		final Retokenizer retokenizer = new Retokenizer(oldTokenizer);
		final ITermFactory nonParentFactory = Environment.INSTANCE().termFactory;
		final ITermFactory factory = new ParentTermFactory(nonParentFactory);
		final FragmentParser testedParser = configureFragmentParser(root, getLanguage(root), fragmentParser);
		final FragmentParser outputParser = getTargetLanguage(root) == null
				? testedParser : configureFragmentParser(root, getTargetLanguage(root), outputFragmentParser);
		assert !(nonParentFactory instanceof ParentTermFactory);

		if (testedParser == null || !testedParser.isInitialized()
				|| outputParser == null || !outputParser.isInitialized()) {
			return root;
		}

		IStrategoTerm result = new TermTransformer(factory, true) {
			@Override
			public IStrategoTerm preTransform(IStrategoTerm term) {
				IStrategoConstructor cons = tryGetConstructor(term);
				FragmentParser parser = null;
				if (cons == INPUT_4) {
					parser = testedParser;
				}
				else if (cons == OUTPUT_4) {
					parser = outputParser;
				}
				if (parser != null) {
					IStrategoTerm fragmentHead = termAt(term, 1);
					IStrategoTerm fragmentTail = termAt(term, 2);
					retokenizer.copyTokensUpToIndex(getLeftToken(fragmentHead).getIndex() - 1);
					try {
						IStrategoTerm parsed = parser.parse(oldTokenizer, term, /*cons == OUTPUT_4*/ false);
						int oldFragmentEndIndex = getRightToken(fragmentTail).getIndex();
						retokenizer.copyTokensFromFragment(fragmentHead, fragmentTail, parsed,
								getLeftToken(fragmentHead).getStartOffset(), getRightToken(fragmentTail).getEndOffset());
						if (!parser.isLastSyntaxCorrect())
							parsed = nonParentFactory.makeAppl(ERROR_1, parsed);
						ImploderAttachment implodement = ImploderAttachment.get(term);
						IStrategoList selected = selections.fetch(parsed);
						term = factory.annotateTerm(term, nonParentFactory.makeListCons(parsed, selected));
						term.putAttachment(implodement.clone());
						retokenizer.skipTokensUpToIndex(oldFragmentEndIndex);
					} catch (IOException e) {
						e.printStackTrace();
						LOG.error("Could not parse tested code fragment (IOE)", e);
					} catch (SGLRException e) {
						// TODO: attach ErrorMessage(_) term with error?
						e.printStackTrace();
						LOG.error("Could not parse tested code fragment (SGLRE)", e);
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
						LOG.error("Could not parse tested code fragment(CNSE)", e);
					} catch (RuntimeException e) {
						e.printStackTrace();
						LOG.error("Could not parse tested code fragment(RE)", e);
					} catch (InterruptedException e) {
						// TODO: attach ErrorMessage(_) term with error?
						e.printStackTrace();
						LOG.error("Could not parse tested code fragment(IE)", e);
					}
				}
				return term;
			}
			
			@Override
			public IStrategoTerm postTransform(IStrategoTerm term) {
				Iterator<IStrategoTerm> iterator = TermVisitor.tryGetListIterator(term); 
				for (int i = 0, max = term.getSubtermCount(); i < max; i++) {
					IStrategoTerm kid = iterator == null ? term.getSubterm(i) : iterator.next();
					ParentAttachment.putParent(kid, term, null);
				}
				return term;
			}
		}.transform(root);
		retokenizer.copyTokensAfterFragments();
		retokenizer.getTokenizer().setAst(result);
		retokenizer.getTokenizer().initAstNodeBinding();
		return result;
	}

	private FragmentParser configureFragmentParser(IStrategoTerm root, ALanguage language, FragmentParser fragmentParser) {
		if (language == null) return null;
		fragmentParser.configure(language, super.getFile(), root);
		// FIXME: I have no clue how commenting this will affect SPT
		// it's probably only editor related, so no worries for the Sunshine version I think
//		attachToLanguage(language);
		return fragmentParser;
	}

//	/**
//	 * Add our language service to the descriptor of a fragment language,
//	 * so our service gets reinitialized once the fragment language changes.
//	 */
//	private void attachToLanguage(Language theirLanguage) {
//		SGLRParseController myController = getController();
//		EditorState myEditor = myController.getEditor();
//		if (myEditor == null)
//			return;
//		ILanguageService myWrapper = myEditor.getEditor().getParseController();
//		if (myWrapper instanceof IDynamicLanguageService) {
//			Descriptor theirDescriptor = Environment.getDescriptor(theirLanguage);
//			theirDescriptor.addActiveService((IDynamicLanguageService) myWrapper);
//		} else {
//			Environment.logException("SpoofaxTestingParseController wrapper is not IDynamicLanguageService");
//		}
//	}
	
	private String getLanguageName(IStrategoTerm root, IStrategoConstructor which) {
		if (root.getSubtermCount() < 1 || !isTermList(termAt(root, 0)))
			return null;
		IStrategoList headers = termAt(root, 0);
		for (IStrategoTerm header : StrategoListIterator.iterable(headers)) {
			if (tryGetConstructor(header) == which) {
				IStrategoString name = termAt(header, 0);
				return asJavaString(name);
			}
		}
		return null;
	}

	private ALanguage getLanguage(IStrategoTerm root) {
		final String languageName = getLanguageName(root, LANGUAGE_1);
		if (languageName == null) return null;
		return LanguageService.INSTANCE().getLanguageByName(languageName);
	}

	private ALanguage getTargetLanguage(IStrategoTerm root) {
		String languageName = getLanguageName(root, TARGET_LANGUAGE_1);
		if (languageName == null) return null;
		return LanguageService.INSTANCE().getLanguageByName(languageName);
	}
}
