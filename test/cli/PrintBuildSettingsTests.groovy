import grails.test.AbstractCliTestCase

class PrintBuildSettingsTests extends AbstractCliTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testPrintBuildSettings() {

        execute(["print-build-settings"])

        assertEquals 0, waitForProcess()
        verifyHeader()
    }
}
