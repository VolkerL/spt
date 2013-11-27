package org.strategoxt.imp.testing.listener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CommandLineTestListener implements ITestListener {

	// map testsuite filenames to the suites
	private Map<String, TestSuite> testsuites;

	@Override
	public void reset() throws Exception {
		testsuites = new HashMap<String, TestSuite>();
	}

	@Override
	public void addTestcase(String testsuite, String description, int offset)
			throws Exception {
		if (!testsuites.containsKey(testsuite)) {
			throw new IllegalArgumentException("No such testsuite");
		}
		TestSuite suite = testsuites.get(testsuite);
		suite.addTestCase(new TestCase(description, offset));
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
			boolean succeeded) throws Exception {
		testsuites.get(testsuite).getTestCase(description).finish(succeeded);
		printChange();
	}

	@Override
	public void disableRefresh() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void enableRefresh() throws Exception {
		// TODO Auto-generated method stub
	}

	private void printChange() {
		for (TestSuite s : testsuites.values()) {
			File output = new File(FilenameUtils.removeExtension(s.filename)
					+ ".testreport");
			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.writeValue(output, s);
			} catch (Exception | Error e) {
				e.printStackTrace();
			}
		}
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

		@JsonGetter
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
	}

	private class TestCase {
		public String name;
		private int offset;
		public boolean result;
		public boolean finished;
		public long start;
		public long end;

		public TestCase(String name, int offset) {
			this.name = name;
			this.offset = offset;
		}

		public void start() {
			start = System.currentTimeMillis();
			finished = false;
		}

		public void finish(boolean result) {
			if (!finished) {
				end = System.currentTimeMillis();
				finished = true;
				this.result = result;
			} else {
				throw new IllegalStateException("Already finished.");
			}
		}
	}
}
