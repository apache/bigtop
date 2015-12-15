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
        LOGGER.info("Number of tests to execute: " + description.testCount());
    }

    public void testRunFinished(Result result) throws Exception {
        LOGGER.info("Number of tests executed: " + result.getRunCount());
    }

    public void testStarted(Description description) throws Exception {
        LOGGER.info("Starting ${description.getTestClass().getSimpleName()} # ${description.getMethodName()}");
    }

    public void testFinished(Description description) throws Exception {
        LOGGER.info("Ending: ${description.getTestClass().getSimpleName()} # ${description.getMethodName()}");
    }

    public void testFailure(Failure failure) throws Exception {
        LOGGER.error("Failed: ${failure.getDescription().getClass().getSimpleName()} # ${failure.getDescription().getMethodName()}");
    }

    public void testAssumptionFailure(Failure failure) {
        LOGGER.error("Assumption Failed: ${failure.getDescription().getClass().getSimpleName()} # ${failure.getDescription().getMethodName()}");
    }

    public void testIgnored(Description description) throws Exception {
        LOGGER.info("Ignored ${description.getTestClass().getSimpleName()} # ${description.getMethodName()}");
    }
}
