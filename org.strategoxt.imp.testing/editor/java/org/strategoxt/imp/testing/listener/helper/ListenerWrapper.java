/**
 * 
 */
package org.strategoxt.imp.testing.listener.helper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.strategoxt.imp.testing.listener.CommandLineTestListener;
import org.strategoxt.imp.testing.listener.ITestListener;

/**
 * 
 * Provides wrapper methods for the client-end of the ITestListener extension point. The purpose of this class is to
 * abstract the implementation details of discovering the client (and using reflexive calls) from the Strategies.
 * 
 * This class is a singleton
 * 
 * @author vladvergu
 * 
 */
public final class ListenerWrapper implements ITestListener {

	private static ITestListener instance;
	
	public static ITestListener instance() {
		if (instance == null)
			instance = new ListenerWrapper();

		return instance;
	}

	private Object wrapped = null; 
	
	private ListenerWrapper() {
	}

	private Object getWrapped() {
//		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
//				ITestListener.EXTENSION_ID);

//		Object candidateListener = null;
//		String preferredView = PlatformUI.getPreferenceStore().getString(PreferenceConstants.P_LISTENER_ID);
//		if (preferredView.equals(""))
//			preferredView = PreferenceInitializer.DEFAULT_LISTENER_ID;

//		for (IConfigurationElement e : config) {
//			if (((RegistryContributor) e.getContributor()).getActualName().equals(preferredView)) {
//				candidateListener = e.createExecutableExtension("class");
//				break;
//			}
//		}

//		return candidateListener;
		
		if(wrapped == null) wrapped = new CommandLineTestListener();
		return wrapped;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.strategoxt.imp.testing.listener.ITestListener#reset()
	 */
	@Override
	public void reset() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {

		Object wrapped = getWrapped();
		// Using reflection, because if I use a cast, I get a ClassCastException
		// cannot cast type <x> to <x>. Probably because of some different classloader issue.
		Method m = wrapped.getClass().getMethod("reset", new Class[] {});
		if (!Modifier.isAbstract(m.getModifiers())) {
			m.invoke(wrapped);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.strategoxt.imp.testing.listener.ITestListener#addTestcase(java.lang.String, java.lang.String, int)
	 */
	@Override
	public void addTestcase(String testsuite, String description, int offset) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {

		Object wrapped = getWrapped();
		Method m = wrapped.getClass().getMethod("addTestcase", new Class[] { String.class, String.class, int.class });
		if (!Modifier.isAbstract(m.getModifiers())) {
			m.invoke(wrapped, testsuite, description, offset);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.strategoxt.imp.testing.listener.ITestListener#addTestsuite(java.lang.String, java.lang.String)
	 */
	@Override
	public void addTestsuite(String name, String filename) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, SecurityException, NoSuchMethodException {

		Object wrapped = getWrapped();
		Method m = wrapped.getClass().getMethod("addTestsuite", new Class[] { String.class, String.class });
		if (!Modifier.isAbstract(m.getModifiers())) {
			m.invoke(wrapped, name, filename);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.strategoxt.imp.testing.listener.ITestListener#startTestcase(java.lang.String, java.lang.String)
	 */
	@Override
	public void startTestcase(String testsuite, String description) throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Object wrapped = getWrapped();
		Method m = wrapped.getClass().getMethod("startTestcase", new Class[] { String.class, String.class });
		if (!Modifier.isAbstract(m.getModifiers())) {
			m.invoke(wrapped, testsuite, description);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.strategoxt.imp.testing.listener.ITestListener#finishTestcase(java.lang.String, java.lang.String,
	 * boolean)
	 */
	@Override
	public void finishTestcase(String testsuite, String description, boolean succeeded, Collection<String> messages)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException,
			NoSuchMethodException {

		Object wrapped = getWrapped();
		Method m = wrapped.getClass().getMethod("finishTestcase",
				new Class[] { String.class, String.class, boolean.class, Collection.class });
		if (!Modifier.isAbstract(m.getModifiers())) {
			m.invoke(wrapped, testsuite, description, succeeded, messages);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.strategoxt.imp.testing.listener.ITestListener#disableRefresh()
	 */
	@Override
	public void disableRefresh() {
		// the test provider doesn't use this hack
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.strategoxt.imp.testing.listener.ITestListener#enableRefresh()
	 */
	@Override
	public void enableRefresh() {
		// the test provider doesn't use this hack
	}

}
