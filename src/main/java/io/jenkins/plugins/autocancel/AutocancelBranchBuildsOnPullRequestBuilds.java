package io.jenkins.plugins.autocancel;

import java.io.PrintStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import hudson.console.ModelHyperlinkNote;
import hudson.model.*;
import io.jenkins.plugins.autocancel.interruption.SupersededInterruption;
import jenkins.model.Jenkins;
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

            PrintStream logger = task.getLogger();
            String changeBranch = currentBuild.getEnvironment(task).get("CHANGE_BRANCH", null);

            if (changeBranch == null) {
                return null;
            }
            logger.println(String.format("Current change branch: %s", changeBranch));

            URI jenkinsUri = new URI(Jenkins.get().getRootUrl());
            int currentBuildNumber = currentBuild.getNumber();
            Job pullRequestJob = currentBuild.getParent();
            Collection<Job> jobs = pullRequestJob.getParent().getAllItems(Job.class);

            for (Job job : jobs) {
                if (changeBranch.equals(job.getDisplayName())) {
                    final String jobUrl = jenkinsUri.resolve(job.getUrl()).toString();

                    logger.println(String.format("Found matching job: %s", ModelHyperlinkNote.encodeTo(jobUrl, job.getDisplayName())));

                    Collection<Run> builds = job.getBuildsAsMap().values();

                    for (Run build : builds) {
                        if (build.isBuilding()) {
                            final String message = String.format(MESSAGE_TEMPLATE, build.number, pullRequestJob.getName(), currentBuildNumber);
                            final String buildUrl = jenkinsUri.resolve(build.getUrl()).toString();

                            logger.println(String.format("Stopping branch job %s", ModelHyperlinkNote.encodeTo(buildUrl, build.getDisplayName())));

                            try {
                                build.getExecutor().interrupt(Result.ABORTED, new SupersededInterruption(message));
                            } catch (Exception e) {
                                logger.println(String.format("Failed to stop job %s", ModelHyperlinkNote.encodeTo(buildUrl, build.getDisplayName())));
                                logger.println(e.getMessage());
                            }
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
