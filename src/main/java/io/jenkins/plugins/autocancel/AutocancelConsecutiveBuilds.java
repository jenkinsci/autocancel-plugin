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
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

public class AutocancelConsecutiveBuilds extends Step {

    @DataBoundConstructor
    public AutocancelConsecutiveBuilds() {}

    @Override
    public StepExecution start(StepContext stepContext) {
        return new Execution(stepContext);
    }

    public static class Execution extends SynchronousStepExecution<Void> {

        private static String MESSAGE_TEMPLATE = "#%s (superseded by #%s)";

        Execution(StepContext context) {
            super(context);
        }

        @Override
        protected Void run() throws Exception {
            Run currentBuild = getContext().get(Run.class);
            TaskListener task = getContext().get(TaskListener.class);

            PrintStream logger = task.getLogger();
            URI jenkinsUri = new URI(Jenkins.get().getRootUrl());
            int currentBuildNumber = currentBuild.getNumber();
            Job currentJob = currentBuild.getParent();
            Collection<WorkflowRun> builds = currentJob.getBuildsAsMap().values();

            for (WorkflowRun build : builds) {
                if (build.isBuilding() && build.number < currentBuildNumber) {
                    final String message = String.format(MESSAGE_TEMPLATE, build.number, currentBuildNumber);
                    final String buildUrl = jenkinsUri.resolve(build.getUrl()).toString();

                    logger.println(String.format("Stopping job %s", ModelHyperlinkNote.encodeTo(buildUrl, build.getDisplayName())));

                    try {
                        Executor executor = build.getExecutor();
                        if (executor != null) {
                            executor.interrupt(Result.ABORTED, new SupersededInterruption(message));
                        }
                        build.doKill();
                    } catch (Exception e) {
                        logger.println(String.format("Failed to stop job %s", ModelHyperlinkNote.encodeTo(buildUrl, build.getDisplayName())));
                        logger.println(e.getMessage());
                    }
                    build.setDisplayName(message);
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
            return "autocancelConsecutiveBuilds";
        }

        @Override
        public String getDisplayName() {
            return "Autocancel consecutive builds";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }
}
