package com.jonavon.grails.build.config

import grails.util.BuildSettingsHolder

/**
 * Controller for the sample build configuration settings.
 */
class BuildConfigurationController {

	def index() {
		Map settings = BuildSettingsHolder.getSettings()?.getConfig()?.flatten();
		[htmlTitle:"Build Settings", buildSettings: settings]
	}
}
