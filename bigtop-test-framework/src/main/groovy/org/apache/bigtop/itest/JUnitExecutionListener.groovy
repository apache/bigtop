package org.apache.bigtop.itest;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tools.ant.taskdefs.optional.junit.FailureRecorder;
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

public class JUnitExecutionListener extends RunListener {

    private Log LOGGER = LogFactory.getLog(JUnitExecutionListener.class);

    public void testRunStarted(Description description) throws Exception {
        LOGGER.info("NUMBER OF TESTS TO EXECUTE : " + description.testCount());
    }

    public void testRunFinished(Result result) throws Exception {
        LOGGER.info("NUMBER OF TESTS EXECUTED : " + result.getRunCount());
    }

    public void testStarted(Description description) throws Exception {
        LOGGER.info("STARTING TEST ${description.getTestClass().getSimpleName()} # ${description.getMethodName()}");
    }

    public void testFinished(Description description) throws Exception {
        LOGGER.info("ENDING TEST ${description.getTestClass().getSimpleName()} # ${description.getMethodName()}");
    }

    public void testFailure(Failure failure) throws Exception {
        LOGGER.error("FAILED TEST ${failure.getDescription().getClass().getSimpleName()} # ${failure.getDescription().getMethodName()}");
    }

    public void testAssumptionFailure(Failure failure) {
        LOGGER.error("ASSUMPTION FAILED TEST ${failure.getDescription().getClass().getSimpleName()} # ${failure.getDescription().getMethodName()}");
    }

    public void testIgnored(Description description) throws Exception {
        LOGGER.info("IGNORED TEST ${description.getTestClass().getSimpleName()} # ${description.getMethodName()}");
    }
}
