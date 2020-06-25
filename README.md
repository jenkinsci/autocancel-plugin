![Build](https://github.com/adidas/jenkins-autocancel-plugin/workflows/Build/badge.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# jenkins-autocancel-plugin

Autocancel builds with ease

## Steps

### autocancelConsecutiveBuilds()

Autocancel consecutive builds will ensure running builds are aborted when a new build starts, leaving always the latest build running.

```
autocancelConsecutiveBuilds()

node {
    // Steps
}
```


### autocancelBranchBuildsOnPullRequestBuilds()

Autocancel branch builds on pull request builds will ensure running source branch builds are aborted when a new pull request build starts.

This could happen when a branch is pushed and immediately filed as a pull request, where branch build becomes redundant.

To match pull requests, the plugin leverages `env.CHANGE_BRANCH`, which should be set to the matching source branch name.
Then, the plugin looks for branch jobs named as the source branch name under multibranch pipeline parent job to abort any running builds.

If `env.CHANGE_BRANCH` is not defined, the step will do nothing.

```
autocancelBranchBuildsOnPullRequestBuilds()

node {
    // Steps
}
```

## Reference links

- https://www.jenkins.io/doc/developer/tutorial/create/
- https://javadoc.jenkins-ci.org/hudson/model/Job.html
- https://javadoc.jenkins-ci.org/hudson/model/Run.html
- https://javadoc.jenkins-ci.org/hudson/model/Executor.html
- https://github.com/jenkinsci/workflow-step-api-plugin/blob/master/README.md
- https://github.com/jenkinsci/workflow-basic-steps-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/steps/EchoStep.java
- https://github.com/jenkinsci/workflow-basic-steps-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/steps/UnstableStep.java
- https://github.com/jenkinsci/bitbucket-branch-source-plugin

## License and Software Information

© adidas AG

adidas AG publishes this software and accompanied documentation (if any) subject to the terms of the MIT license with the aim of helping the community with our tools and libraries which we think can be also useful for other people. You will find a copy of the MIT license in the root folder of this package. All rights not explicitly granted to you under the MIT license remain the sole and exclusive property of adidas AG.

NOTICE: The software has been designed solely for the purpose of analyzing the code quality by checking the coding guidelines. The software is NOT designed, tested or verified for productive use whatsoever, nor or for any use related to high risk environments, such as health care, highly or fully autonomous driving, power plants, or other critical infrastructures or services.

If you want to contact adidas regarding the software, you can mail us at _software.engineering@adidas.com_.

For further information open the [adidas terms and conditions](https://github.com/adidas/adidas-contribution-guidelines/wiki/Terms-and-conditions) page.

### License

[MIT](LICENSE)