import grails.util.BuildSettings
import grails.util.BuildSettingsHolder

// These constants were copied from org.jetbrains.plugins.grails.config.PrintGrailsSettingsConstants
final String SETTINGS_START_MARKER = "---=== BEGIN Print Build Settings ===---"
final String SETTINGS_END_MARKER = "---=== END Print Build Settings ===---"

includeTargets << grailsScript("_GrailsInit")

target(printBuildSettings: "Print the current build settings.") {
	depends(resolveDependencies)

	BuildSettings settings = BuildSettingsHolder.settings
	Properties properties = new LinkedProperties()
	Map flattenedSettings = settings.getConfig().flatten()

	for(Iterator itr = flattenedSettings.entrySet().iterator(); itr.hasNext(); ) {
		Map.Entry entry = (Map.Entry)itr.next()
		Object value = entry.getValue()
		if(value instanceof String || value instanceof GString || value instanceof File) {
			String key = (String)entry.getKey()
			properties.setProperty(key, value.toString())
		}
	}

	println()
	println(SETTINGS_START_MARKER)
	properties.store(System.out, "Settings printed")
	println(SETTINGS_END_MARKER)
}

/**
 * Popular reimplementation of Properties to remember order.
 */
class LinkedProperties extends Properties {
	private final List list = new ArrayList()

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Object put(Object key, Object value) {
		list.add(key)
		return super.put(key, value)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Enumeration keys() {
		return new ListEnumeration(list, this)
	}

	class ListEnumeration implements Enumeration {

		private final List list
		private final Properties properties
		private int index = 0

		/**
		 * Constructor initializes the class.
		 */
		ListEnumeration(List list, Properties properties) {
			this.list = list
			this.properties = properties
		}

		/**
		 * Tests if this enumeration contains more elements.
		 *
		 * @return <code>true</code> if and only if this enumeration object
		 *           contains at least one more element to provide;
		 *          <code>false</code> otherwise.
		 */
		@Override
		boolean hasMoreElements() {
			this.iterateIndex()
			return this.index < this.list.size()
		}

		/**
		 * Returns the next element of this enumeration if this enumeration
		 * object has at least one more element to provide.
		 *
		 * @return the next element of this enumeration.
		 * @exception NoSuchElementException  if no more elements exist.
		 */
		@Override
		Object nextElement() {
			this.iterateIndex()
			return list.get(this.index++)
		}

		/**
		 * Used in Enumeration methods.
		 */
		private void iterateIndex() {
			while(this.index < this.list.size() && !this.properties.containsKey(this.list.get(this.index))) {
				this.index++
			}
		}
	}
}
setDefaultTarget(printBuildSettings)
