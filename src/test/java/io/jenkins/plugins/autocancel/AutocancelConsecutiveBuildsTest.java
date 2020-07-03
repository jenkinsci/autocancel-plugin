package io.jenkins.plugins.autocancel;

import hudson.model.Cause;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AutocancelConsecutiveBuildsTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testPreviousBuildsAreAborted() throws Exception {
        jenkins.createOnlineSlave();
        jenkins.createOnlineSlave();
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-autocancel-consecutive-builds");

        String pipelineScript = "autocancelConsecutiveBuilds()\n"
                + "\n"
                + "node {\n"
                + "  sleep 30\n"
                + "}\n";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        job.setConcurrentBuild(true);
        job.setQuietPeriod(0);

        job.scheduleBuild(new Cause.UserIdCause());

        Thread.sleep(5 * 1000);

        job.scheduleBuild(new Cause.UserIdCause());

        Thread.sleep(10 * 1000);

        WorkflowRun build1 = job.getBuildByNumber(1);

        jenkins.assertBuildStatus(Result.ABORTED, build1);
    }
}
