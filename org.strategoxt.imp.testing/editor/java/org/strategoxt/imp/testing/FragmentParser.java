package org.strategoxt.imp.testing;

import static java.lang.Math.max;
import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermString;
import static org.spoofax.interpreter.core.Tools.listAt;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.sunshine.Environment;
import org.spoofax.sunshine.parser.model.IParserConfig;
import org.spoofax.sunshine.parser.model.ParserConfig;
import org.spoofax.sunshine.services.language.ALanguage;
import org.spoofax.sunshine.services.parser.JSGLRI;
import org.spoofax.sunshine.services.parser.SourceAttachment;
import org.spoofax.sunshine.util.StrategoImpUtil;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermVisitor;
import org.strategoxt.lang.WeakValueHashMap;

/** 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class FragmentParser {

	private static final boolean ALLOW_CACHING = false; // currently useless; plus it breaks setup sections at end of file
	
	private static final int FRAGMENT_PARSE_TIMEOUT = 3000;
	
	private static final IStrategoConstructor FAILS_PARSING_0 =
		Environment.INSTANCE().termFactory.makeConstructor("FailsParsing", 0);
	
	private static final IStrategoConstructor SETUP_3 =
		Environment.INSTANCE().termFactory.makeConstructor("Setup", 3);

	private static final IStrategoConstructor TARGET_SETUP_3 =
		Environment.INSTANCE().termFactory.makeConstructor("TargetSetup", 3);

	private static final IStrategoConstructor OUTPUT_4 =
		Environment.INSTANCE().termFactory.makeConstructor("Output", 4);
	
	private static final IStrategoConstructor QUOTEPART_1 =
		Environment.INSTANCE().termFactory.makeConstructor("QuotePart", 1);
	
	private static final int EXCLUSIVE = 1;

	private final IStrategoConstructor setup_3;

	private final IStrategoConstructor topsort_1;

	// used to check if the FragmentParser was already configured for this ALanguage
	private ALanguage parseCacheLanguage;
	
	private final WeakValueHashMap<String, IStrategoTerm> failParseCache =
		new WeakValueHashMap<String, IStrategoTerm>();
	
	private final WeakValueHashMap<String, IStrategoTerm> successParseCache =
		new WeakValueHashMap<String, IStrategoTerm>();
	
	private JSGLRI parser;

	private List<OffsetRegion> setupRegions;
	
	private boolean isLastSyntaxCorrect;

	public FragmentParser(IStrategoConstructor setup_3, IStrategoConstructor topsort_1) {
		assert setup_3.getArity() == 3;
		assert topsort_1.getArity() == 1;
		this.setup_3 = setup_3;
		this.topsort_1 = topsort_1;
		parseCacheLanguage = null;
	}

	public void configure(ALanguage lang, File sptFile, IStrategoTerm ast) {
		if (parseCacheLanguage != lang) {
			parseCacheLanguage = lang;
			parser = getParser(lang, sptFile, ast);
			failParseCache.clear();
			successParseCache.clear();
		}
		setupRegions = getSetupRegions(ast);
	}
	
	public boolean isInitialized() {
		return parser != null;
	}

	/**
	 * Creates a JSGLRI parser for the given language and file.
	 * @param lang the language which we should parse.
	 * @param file the file that this parser should parse.
	 * @param ast TODO I don't know what it should represent, but it is used to determine the start symbol for the parser.
	 * @return the parser, or null if lang was null.
	 */
	private JSGLRI getParser(ALanguage lang, File file, IStrategoTerm ast) {
		if (lang == null)
			return null;
		IStrategoTerm start = StrategoImpUtil.findTerm(ast, topsort_1.getName());
		// start symbol creation is copied from the previous configure method
		// TODO: can't we just use lang.getStartSymbol?
		String startSymbol = start == null ? null : asJavaString(start.getSubterm(0));
		IParserConfig config = new ParserConfig(startSymbol, lang.getParseTableProvider(), FRAGMENT_PARSE_TIMEOUT);
		JSGLRI result = new JSGLRI(config, file);
		result.setUseRecovery(true);
		return result;
	}

	public IStrategoTerm parse(ITokenizer oldTokenizer, IStrategoTerm fragment, boolean ignoreSetup)
			throws TokenExpectedException, BadTokenException, SGLRException, IOException, InterruptedException {
		
		// TODO: use context-independent caching key
		//       (requires offset adjustments for reuse...)
		// String fragmentInputCompact = createTestFragmentString(oldTokenizer, fragment, ignoreSetup, true);
		String fragmentInput = createTestFragmentString(oldTokenizer, fragment, ignoreSetup, false);
		boolean successExpected = isSuccessExpected(fragment);
		IStrategoTerm parsed = getCache(successExpected).get(fragmentInput/*Compact*/);
		if (parsed != null) {
			isLastSyntaxCorrect = successExpected;
		}
		if (parsed == null || !ALLOW_CACHING) {
			//String fragmentInput = createTestFragmentString(oldTokenizer, fragment, false);
			parsed = parser.actuallyParse(fragmentInput, oldTokenizer.getFilename());
			isLastSyntaxCorrect = getTokenizer(parsed).isSyntaxCorrect();
			SourceAttachment.putSource(parsed, SourceAttachment.getResource(fragment), parser.getConfig());
			if (!successExpected)
				clearTokenErrors(getTokenizer(parsed));
			if (isLastSyntaxCorrect == successExpected)
				getCache(isLastSyntaxCorrect).put(fragmentInput/*Compact*/, parsed);
		}
		return parsed;
	}

	private WeakValueHashMap<String, IStrategoTerm> getCache(boolean parseSuccess) {
		return parseSuccess ? successParseCache : failParseCache;
	}

	private String createTestFragmentString(ITokenizer tokenizer, IStrategoTerm term,
			boolean ignoreSetup, boolean compactWhitespace) {
		
		IStrategoTerm fragmentHead = term.getSubterm(1);
		IStrategoTerm fragmentTail = term.getSubterm(2);
		int fragmentStart = getLeftToken(fragmentHead).getStartOffset();
		int fragmentEnd = getRightToken(fragmentTail).getEndOffset();
		String input = tokenizer.getInput();
		StringBuilder result = new StringBuilder(
			compactWhitespace ? input.length() + 16 : input.length());
		
		boolean addedFragment = false;
		int index = 0;
		
		if (!ignoreSetup) {
			for (OffsetRegion setupRegion : setupRegions) {
				int setupStart = setupRegion.startOffset;
				int setupEnd = setupRegion.endOffset;
				if (!addedFragment && setupStart >= fragmentStart) {
					addWhitespace(input, index, fragmentStart - 1, result);
					appendFragment(fragmentHead, input, result);
					appendFragment(fragmentTail, input, result);
					index = fragmentEnd + 1;
					addedFragment = true;
				}
				if (fragmentStart != setupStart) { // only if fragment != setup region
					addWhitespace(input, index, setupStart - 1, result);
					if (setupEnd >= index) {
						result.append(input, max(setupStart, index), setupEnd + EXCLUSIVE);
						index = setupEnd + 1;
					}
				}
			}
		}
		
		if (!addedFragment) {
			addWhitespace(input, index, fragmentStart - 1, result);
			appendFragment(fragmentHead, input, result);
			appendFragment(fragmentTail, input, result);
			index = fragmentEnd + 1;
		}
		
		addWhitespace(input, index, input.length() - 1, result);
		
		assert result.length() == input.length();
		return result.toString(); 
	}

	private void appendFragment(IStrategoTerm term, String input, StringBuilder output) {
		IToken left = getLeftToken(term);
		IToken right = getRightToken(term);
		if (tryGetConstructor(term) == QUOTEPART_1) {
			output.append(input, left.getStartOffset(), right.getEndOffset() + EXCLUSIVE);
		} else if (isTermString(term)) {
			// Brackets: treat as whitespace
			assert asJavaString(term).length() <= 4 : "Bracket expected: " + term;
			addWhitespace(input, left.getStartOffset(), right.getEndOffset(), output);
		} else {
			// Other: recurse
			for (int i = 0; i < term.getSubtermCount(); i++) {
				appendFragment(term.getSubterm(i), input, output);
			}
		}
	}

	private static void addWhitespace(String input, int startOffset, int endOffset, StringBuilder output) {
		for (int i = startOffset; i <= endOffset; i++)
			output.append(input.charAt(i) == '\n' ? '\n' : ' ');
	}
	
	private List<OffsetRegion> getSetupRegions(IStrategoTerm ast) {
		final List<OffsetRegion> results = new ArrayList<OffsetRegion>();
		new TermVisitor() {
			public void preVisit(IStrategoTerm term) {
				if (tryGetConstructor(term) == setup_3) {
					new TermVisitor() {
						public final void preVisit(IStrategoTerm term) {
							if (tryGetConstructor(term) == QUOTEPART_1) {
								term = term.getSubterm(0);
								results.add(new OffsetRegion(
									getLeftToken(term).getStartOffset(),
									getRightToken(term).getEndOffset()));
							}
						}
					}.visit(term);
				}
			}
		}.visit(ast);
		return results;
	}
	
	/*
	private boolean isSetupToken(IToken token) {
		// if (token.getKind() != IToken.TK_STRING) return false;
		assert token.getKind() == IToken.TK_STRING;
		IStrategoTerm node = (IStrategoTerm) token.getAstNode();
		if (node != null && "Input".equals(getSort(node))) {
			IStrategoTerm parent = getParent(node);
			if (parent != null && isTermAppl(parent) && "Setup".equals(((IStrategoAppl) parent).getName()))
				return true;
		}
		return false;
	}
	*/
	
	private boolean isSuccessExpected(IStrategoTerm fragment) {
		if (tryGetConstructor(fragment) == OUTPUT_4)
			return true;
		IStrategoAppl test = (IStrategoAppl) getParent(fragment);
		if (test.getConstructor() == SETUP_3 || test.getConstructor() == TARGET_SETUP_3)
			return true;
		IStrategoList expectations = listAt(test, test.getSubtermCount() - 1);
		for (IStrategoTerm expectation : StrategoListIterator.iterable(expectations)) {
			IStrategoConstructor cons = tryGetConstructor(expectation);
			if (/*cons == FAILS_0 ||*/ cons == FAILS_PARSING_0)
				return false;
		}
		return true;
	}
	
	public boolean isLastSyntaxCorrect() {
		return isLastSyntaxCorrect;
	}
	
	private void clearTokenErrors(ITokenizer tokenizer) {
		for (IToken token : tokenizer) {
			((Token) token).setError(null);
		}
	}
	
	/**
	 * An (inclusive) offset tuple.
	 * 
	 * @author Lennart Kats <lennart add lclnet.nl>
	 */
	static class OffsetRegion {
		int startOffset, endOffset;
		OffsetRegion(int startOffset, int endOffset) {
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
		@Override
		public String toString() {
			return "(" + startOffset + "," + endOffset + ")";
		}
	}
}
