package io.jenkins.plugins.autocancel;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import hudson.model.*;
import io.jenkins.plugins.autocancel.interruption.SupersededInterruption;
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

            int currentBuildNumber = currentBuild.getNumber();
            Job currentJob = currentBuild.getParent();
            Collection<Run> builds = currentJob.getBuildsAsMap().values();

            for (Run build : builds) {
                if (build.isBuilding() && build.number < currentBuildNumber) {
                    final String message = String.format(MESSAGE_TEMPLATE, build.number, currentBuildNumber);

                    build.getExecutor().interrupt(Result.ABORTED, new SupersededInterruption(message));
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
