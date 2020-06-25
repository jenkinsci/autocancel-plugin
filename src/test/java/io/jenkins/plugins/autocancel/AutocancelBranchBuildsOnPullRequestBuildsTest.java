package io.jenkins.plugins.autocancel;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource;
import com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait;
import com.cloudbees.jenkins.plugins.bitbucket.OriginPullRequestDiscoveryTrait;
import com.cloudbees.jenkins.plugins.bitbucket.api.*;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import hudson.model.Cause;
import hudson.model.Result;
import io.jenkins.plugins.bitbucket.BitbucketMockApiFactory;
import jenkins.branch.*;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.Returns;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutocancelBranchBuildsOnPullRequestBuildsTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public GitSampleRepoRule sampleRepo = new GitSampleRepoRule();

    @Test
    public void testPreviousBranchBuildsAreAborted() throws Exception {
        jenkins.createOnlineSlave();
        jenkins.createOnlineSlave();

        String pipelineScript = "autocancelBranchBuildsOnPullRequestBuilds()\n"
                + "\n"
                + "node {\n"
                + "  sleep 30\n"
                + "}\n";

        sampleRepo.init();
        sampleRepo.write("Jenkinsfile", pipelineScript);
        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--all", "--message='INIT repository'");

        BitbucketApi api = mock(BitbucketApi.class);

        BitbucketRepository repository = Mockito.mock(BitbucketRepository.class);
        when(repository.getOwnerName()).thenReturn("adidas");
        when(repository.getRepositoryName()).thenReturn("test");
        when(repository.getScm()).thenReturn("git");
        when(repository.getFullName()).thenReturn("adidas/test");
        when(api.getRepository()).thenReturn(repository);

        BitbucketBranch branch = mock(BitbucketBranch.class);
        List<? extends BitbucketBranch> branches = Collections.singletonList(branch);
        when(branch.getName()).thenReturn("feature/test");
        when(branch.getRawNode()).thenReturn(sampleRepo.head());
        when(api.getBranches()).thenAnswer(new Returns(branches));

        BitbucketPullRequestSource pullRequestSource = mock(BitbucketPullRequestSource.class);
        BitbucketPullRequestDestination pullRequestTarget = mock(BitbucketPullRequestDestination.class);
        BitbucketPullRequest pullRequest = mock(BitbucketPullRequest.class);
        List<? extends BitbucketPullRequest> pullRequests = Collections.singletonList(pullRequest);
        when(pullRequestSource.getBranch()).thenReturn(branch);
        when(pullRequestSource.getRepository()).thenReturn(repository);
        when(pullRequestTarget.getBranch()).thenReturn(branch);
        when(pullRequest.getSource()).thenReturn(pullRequestSource);
        when(pullRequest.getDestination()).thenReturn(pullRequestTarget);
        when(pullRequest.getId()).thenReturn("1");
        when(api.getPullRequests()).thenAnswer(new Returns(pullRequests));

        BitbucketCommit commit = Mockito.mock(BitbucketCommit.class);
        when(api.resolveCommit(sampleRepo.head())).thenReturn(commit);
        when(commit.getDateMillis()).thenReturn(System.currentTimeMillis());

        when(api.checkPathExists(Mockito.anyString(), eq("Jenkinsfile"))).thenReturn(true);

        when(api.getRepositoryUri(eq(BitbucketRepositoryType.GIT),
                any(BitbucketRepositoryProtocol.class),
                anyString(),
                eq("adidas"),
                eq("test")))
                .thenReturn(sampleRepo.fileUrl());

        BitbucketMockApiFactory.add(BitbucketCloudEndpoint.SERVER_URL, api);
        WorkflowMultiBranchProject job = jenkins.createProject(WorkflowMultiBranchProject.class, "test-autocancel-consecutive-builds");

        BitbucketSCMSource source = new BitbucketSCMSource("adidas", "test");
        source.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, true),
                new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD))
        ));

        DefaultBranchPropertyStrategy strategy = new DefaultBranchPropertyStrategy(ArrayUtils.toArray(
                new NoTriggerBranchProperty()
        ));

        BranchSource bitbucketBranchSource = new BranchSource(source);

        bitbucketBranchSource.setStrategy(strategy);

        job.getSourcesList().add(bitbucketBranchSource);

        // when
        job.scheduleBuild();

        jenkins.waitUntilNoActivity();

        assertThat(job.getIndexing().getResult(), is(Result.SUCCESS));

        WorkflowJob featureTest = job.getItem("feature/test");
        WorkflowJob pr1 = job.getItem("PR-1");

        featureTest.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        pr1.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        featureTest.scheduleBuild(new Cause.UserIdCause());

        Thread.sleep(5 * 1000);

        pr1.scheduleBuild(new Cause.UserIdCause());

        Thread.sleep(10 * 1000);

        WorkflowRun build1 = featureTest.getBuildByNumber(1);

        // then
        jenkins.assertBuildStatus(Result.ABORTED, build1);
    }
}
