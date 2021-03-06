Externalizing Your Build Configuration In Grails
================================================

Grails has several configuration files. Two significant files are `grails-app/conf/Config.groovy` and `grails-app/conf/BuildConfig.groovy` that are for runtime and build time configurations respectively. Configuration variables can be set in each of these files, but sometimes it would be beneficial to externalize variables. Grails allows for this by convention with the grails.config.locations variable that is set in `Config.groovy`. However, there is no such facility in `BuildConfig.groovy`. Fortunately, externalizing your build configuration in grails can be achieved with a little modification to BuildConfig.groovy

Guide
-----

Let's build a proof of concept.

### Create an empty grails app

~~~~ {.bash}
$> grails create-app build-proof
$> cd build-proof
$> grails create-script print-build-settings
$> grails create-controller me.sample.grails.BuildConfigurationController
~~~~

### Copy and paste code

Grails has added code at the top of each `Config.groovy` file that allows for users to specify additional locations for configuration. We can add similar code to the the top of `BuildConfig.groovy` and get similar behavior. Variables set in external files are not available at the root level. Variables in BuildConfig are stored in `grailsSettings.config` we create a pointer, `buildConfig`, so it makes sense. For example, if you have a variable called db.username you would access the value with `grailsSettings.config.db.username` or by using the pointer `buildConfig.db.username`.

Note that none of this code is necessary if all you are going to use is `settings.groovy`. The variables in `settings.groovy` are imported automatically.

~~~~ {.groovy startFrom="1" data-fileName="buildConfig.groovy"}
//
// External build configuration
//
// Pointer to build settings.
ConfigObject buildConfig = grailsSettings.config

// Default build file locations.
List buildLocations = [
    // ${userHome}/.grails/settings.groovy is already imported
    "${userHome}/.grails/${appName}-buildConfig.properties"
    , "${userHome}/.grails/${appName}-buildConfig.groovy"
    , "./${appName}-buildConfig.properties"
    , "./${appName}-buildConfig.groovy"
]

// Read a system property for the build file location.
if (System.properties["${appName}.build.config.location"]) {
    buildLocations << System.properties["${appName}.build.config.location"]
}

// Merge configuration for every buildLocation.
buildLocations.each {
    def buildConfigLocation ->
        File bConfig = new File(buildConfigLocation.toString())
        if(bConfig.exists()) {
            buildConfig.merge(new ConfigSlurper(grailsSettings.grailsEnv).parse(bConfig.toURI().toURL()))
        }
}
//
// End build external configuration
~~~~

### Create a script file

~~~~ {.groovy startFrom="1" data-fileName="PrintBuildSettings.groovy"}

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder

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

class LinkedProperties extends Properties {
    private final List list = new ArrayList()

    @Override
    public synchronized Object put(Object key, Object value) {
        list.add(key)
        return super.put(key, value)
    }

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

        @Override
        boolean hasMoreElements() {
            this.iterateIndex()
            return this.index < this.list.size()
        }

        @Override
        Object nextElement() {
            this.iterateIndex()
            return list.get(this.index++)
        }

        private void iterateIndex() {
            while(this.index < this.list.size() && !this.properties.containsKey(this.list.get(this.index))) {
                this.index++
            }
        }
    }
}
setDefaultTarget(printBuildSettings)
~~~~

~~~~ {.bash}
>$ grails print-build-settings

---=== BEGIN Print build settings ===---
#
#Wed Aug 20 09:19:46 EDT 2014
grails.servlet.version=3.0
grails.project.class.dir=target/classes
grails.project.test.class.dir=target/test-classes
grails.project.test.reports.dir=target/test-reports
grails.project.work.dir=target/work
grails.project.dependency.resolver=maven
---=== END Print build settings ===---
~~~~

### Create a controller

~~~~ {.groovy startFrom="1" data-fileName="BuildConfigurationController.groovy"}
package me.sample.grails

import grails.util.BuildSettingsHolder

class BuildConfigurationController {

    def index() {
        Map settings = BuildSettingsHolder.getSettings().getConfig().flatten();
        [htmlTitle:"Build Settings", buildSettings: settings]
    }
}
~~~~

### Create a view

We can create a view for the above controller in grails-app/views/buildConfiguration/index.gsp. The html below creates a table with an entry for each key value pair in build settings.

~~~~ {.html startFrom="1" data-fileName="index.gsp"}
<!DOCTYPE html>
<html>
    <head>
        <title>${htmlTitle}</title>
        <style type="text/css">
            body {
                font: normal medium/1.4 sans-serif;
            }
            table {
                border-collapse: collapse;
                width: 100%;
            }
            th, td {
                padding: 0.25rem;
                text-align: left;
                border: 1px solid #ccc;
            }
            tbody tr:nth-child(odd) {
                background: #eee;
            }
        </style>
    </head>
    <body>
        <h1>${htmlTitle}</h1>
        <g:if test="${flash.message}">
            <div class="message" role="status">${flash.message}</div>
        </g:if>
        <table>
            <thead>
                <tr>
                        <td>Key</td>
                        <td>Value</td>
                </tr>
            </thead>
            <tbody>
                <g:each in="${buildSettings}" var="setting">
                    <tr>
                        <td>${setting.key}</td>
                        <td>${setting.value}</td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </body>
</html>
~~~~

Go to <http://localhost:8080/build-proof/buildConfiguration/> to see the results of your work.

### Test external files

To prove that it works we can manually test. After adding each file you may want to just run `grails print-build-settings` script.

#### \${user.home}/.grails/settings.groovy

Create a file \${user.home}/.grails/settings.groovy and append to the config my.test.one = "uno".

~~~~ {.bash}
>$ echo 'my.test.one = "uno"' >> ~/.grails/settings.groovy
>$ grails print-build-settings

---=== BEGIN Print build settings ===---
#
#Wed Aug 20 09:45:12 EDT 2014
my.test.one=uno
grails.servlet.version=3.0
grails.project.class.dir=target/classes
grails.project.test.class.dir=target/test-classes
grails.project.test.reports.dir=target/test-reports
grails.project.work.dir=target/work
grails.project.dependency.resolver=maven
---=== END Print build settings ===---
~~~~

#### \${user.home}/.grails/

Create a file \({user.home}/.grails/\){appName}-buildConfig.groovy and append to the config my.test.two = "dos".

~~~~ {.bash}
>$ echo 'my.test.two = "dos"' >> ~/.grails/build-proof-buildConfig.groovy
>$ grails print-build-settings

---=== BEGIN Print build settings ===---
#
#Wed Aug 20 09:45:12 EDT 2014
my.test.one=uno
my.test.two=dos
grails.servlet.version=3.0
grails.project.class.dir=target/classes
grails.project.test.class.dir=target/test-classes
grails.project.test.reports.dir=target/test-reports
grails.project.work.dir=target/work
grails.project.dependency.resolver=maven
---=== END Print build settings ===---
~~~~

#### ./

Create a file ./\${appName}-buildConfig.groovy and append to the config my.test.three = "tres".

~~~~ {.bash}
>$ echo 'my.test.three = "tres"' >> ./build-proof-buildConfig.groovy
>$ grails print-build-settings

---=== BEGIN Print build settings ===---
#
#Wed Aug 20 09:45:12 EDT 2014
my.test.one=uno
my.test.two=dos
my.test.three=tres
grails.servlet.version=3.0
grails.project.class.dir=target/classes
grails.project.test.class.dir=target/test-classes
grails.project.test.reports.dir=target/test-reports
grails.project.work.dir=target/work
grails.project.dependency.resolver=maven
---=== END Print build settings ===---
~~~~

#### CLI System Property

Create an override file in /tmp/config.groovy.

~~~~ {.groovy startFrom="1" data-fileName="/tmp/config.groovy"}
my {
    test {
        one = "one"
        two = "two"
        three = "three"
    }
}
~~~~

Run with system property.

~~~~ {.bash}
$> grails -Dbuild-proof.build.config.location=/tmp/config.groovy print-build-settings

---=== BEGIN Print build settings ===---
#
#Wed Aug 20 10:06:41 EDT 2014
my.test.one=one
my.test.two=two
my.test.three=three
grails.servlet.version=3.0
grails.project.class.dir=target/classes
grails.project.test.class.dir=target/test-classes
grails.project.test.reports.dir=target/test-reports
grails.project.work.dir=target/work
grails.project.dependency.resolver=maven
---=== END Print build settings ===---
~~~~

### Win!

Clone the completed project using git at http://github.com/jonavon/external-build-config
