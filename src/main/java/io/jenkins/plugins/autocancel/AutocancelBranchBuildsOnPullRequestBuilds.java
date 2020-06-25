package io.jenkins.plugins.autocancel;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import hudson.model.*;
import io.jenkins.plugins.autocancel.interruption.SupersededInterruption;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

public class AutocancelBranchBuildsOnPullRequestBuilds extends Step {

    @DataBoundConstructor
    public AutocancelBranchBuildsOnPullRequestBuilds() {}

    @Override
    public StepExecution start(StepContext stepContext) {
        return new Execution(stepContext);
    }

    public static class Execution extends SynchronousStepExecution<Void> {

        private static String MESSAGE_TEMPLATE = "#%s (superseded by %s #%s)";

        Execution(StepContext context) {
            super(context);
        }

        @Override
        protected Void run() throws Exception {
            Run currentBuild = getContext().get(Run.class);
            TaskListener task = getContext().get(TaskListener.class);
            String changeBranch = currentBuild.getEnvironment(task).get("CHANGE_BRANCH", null);

            if (changeBranch == null) {
                return null;
            }

            int currentBuildNumber = currentBuild.getNumber();
            Job pullRequestJob = currentBuild.getParent();
            Collection<Job> jobs = pullRequestJob.getParent().getAllItems(Job.class);

            for (Job job : jobs) {
                if (changeBranch == job.getDisplayName()) {
                    Collection<Run> builds = job.getBuildsAsMap().values();

                    for (Run build : builds) {
                        if (build.isBuilding()) {
                            final String message = String.format(MESSAGE_TEMPLATE, build.number, pullRequestJob.getName(), currentBuildNumber);

                            build.getExecutor().interrupt(Result.ABORTED, new SupersededInterruption(message));
                            build.setDisplayName(message);
                        }
                    }
                }
            }

            return null;
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "autocancelBranchBuildsOnPullRequestBuilds";
        }

        @Override
        public String getDisplayName() {
            return "Autocancel branch builds on pull request builds";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }
}
