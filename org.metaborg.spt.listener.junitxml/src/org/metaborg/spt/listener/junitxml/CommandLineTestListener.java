package org.metaborg.spt.listener.junitxml;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.metaborg.spt.listener.ITestReporter;

public class CommandLineTestListener implements ITestReporter {

	// map testsuite filenames to the suites
	private Map<String, TestSuite> testsuites;

	@Override
	public void reset() throws Exception {
		testsuites = new HashMap<String, TestSuite>();
	}

	/**
	 * @param testsuite the filename of the test suite to which this test case belongs
	 * @param description the description of the test case (i.e. the name)
	 */
	@Override
	public void addTestcase(String testsuite, String description)
			throws Exception {
		if (!testsuites.containsKey(testsuite)) {
			throw new IllegalArgumentException("No such testsuite");
		}
		TestSuite suite = testsuites.get(testsuite);
		suite.addTestCase(new TestCase(description, 0));
	}

	@Override
	public void addTestsuite(String name, String filename) throws Exception {
		if (testsuites.containsKey(filename))
			throw new IllegalArgumentException("Testsuite already present.");
		testsuites.put(filename, new TestSuite(name, filename));
	}

	@Override
	public void startTestcase(String testsuite, String description)
			throws Exception {
		testsuites.get(testsuite).getTestCase(description).start();
	}

	@Override
	public void finishTestcase(String testsuite, String description,
			boolean succeeded, Collection<String> messages) throws Exception {
		TestSuite suite = testsuites.get(testsuite);
		TestCase tcase = suite.getTestCase(description);
		tcase.finish(succeeded, messages);
		printChange();
	}

	private void printChange() {
		for (TestSuite s : testsuites.values()) {
			File output = new File(FilenameUtils.removeExtension(s.filename)
					+ ".sptreport.xml");
			try {
//				ObjectMapper mapper = new ObjectMapper();
//				mapper.writeValue(output, s);
				FileWriter out = new FileWriter(output, false);
				out.write(toJUnitXml(testsuites.values()));
				out.close();
			} catch (Exception | Error e) {
				e.printStackTrace();
			}
		}
	}

	private String toJUnitXml(Collection<TestSuite> ss) {
		StringBuilder b = new StringBuilder();
		b.append("<testsuites>\n");
		for (TestSuite s : ss) {
			b.append(s.toJUnitXml(1)).append('\n');
		}
		b.append("</testsuites>");
		
		return b.toString();
	}
	
	private class TestSuite {
		public String name;
		public String filename;
		private Map<String, TestCase> testcases;

		public TestSuite(String name, String file) {
			this.name = name;
			this.filename = file;
			this.testcases = new HashMap<String, TestCase>();
		}

		public void addTestCase(TestCase test) throws Exception {
			if (testcases.containsKey(test.name))
				throw new IllegalArgumentException("Test was already present.");
			testcases.put(test.name, test);
		}

		public TestCase getTestCase(String name) {
			return testcases.get(name);
		}

		public Collection<TestCase> getTestCases() {
			return testcases.values();
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append(name).append('\n');
			for (TestCase test : testcases.values()) {
				b.append('\t').append(test.name);
				if (test.finished)
					b.append("\t:\t").append(test.result);
				b.append('\n');
			}
			return b.toString();
		}
		
		public String toJUnitXml(int indent) {
			StringBuilder b = new StringBuilder();
			
			int failures = 0;
			for (TestCase c : testcases.values()) {
				if (c.finished && !c.result) failures++;
			}
			
			// no errors, just failures
			indent(b, indent).append("<testsuite name=\"").append(name)
			.append("\" tests=\"").append(testcases.size())
			.append("\" failures=\"").append(failures).append("\">\n");
			
			for (TestCase c : testcases.values()) {
				b.append(c.toJUnitXml(indent+1)).append('\n');
			}
			
			indent(b, indent).append("</testsuite>");
			
			return b.toString();
		}
	}

	private class TestCase {
		public String name;
		private int offset;
		public boolean result;
		public boolean finished;
		public long start;
		public long end;
		public Collection<String> messages;

		public TestCase(String name, int offset) {
			this.name = name;
			this.offset = offset;
		}

		public void start() {
			start = System.currentTimeMillis();
			finished = false;
		}

		public void finish(boolean result, Collection<String> messages) {
			if (!finished) {
				end = System.currentTimeMillis();
				this.messages = messages;
				finished = true;
				this.result = result;
			} else {
				throw new IllegalStateException("Already finished.");
			}
		}
		
		public String toJUnitXml(int indent) {
			StringBuilder b = new StringBuilder();
			indent(b, indent).append("<testcase name=\"").append(name).append("\"");
			// TODO figure out if time should be the runtime and if so in microseconds or not
			if (finished) b.append(" time=\"").append(end-start).append("\"");
			b.append(">\n");
			if (!finished) indent(b, indent + 1).append("<skipped type=\"Unknown\" />\n");
			// we don't do errors, just failures
			// TODO add the type and message of the failure
			if (finished && !result) {
				if (messages == null || messages.isEmpty()) {
					indent(b, indent + 1).append("<failure type=\"UnknownFailure\" message=\"unknown cause\"/>\n");
				} else {
					for (String message : messages) {
						indent(b, indent + 1).append("<failure type=\"UnknownFailure\" message=\"")
						.append(StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml(message)))
						.append("\"/>\n");
					}
				}
			}
			indent(b, indent).append("</testcase>");
			
			return b.toString();
		}
	}
	
	private StringBuilder indent(StringBuilder b, int i) {
		for (int j = 0; j < i; j++) {
			b.append('\t');
		}
		return b;
	}
}
