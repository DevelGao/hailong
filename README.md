# BeaconChain

 [![Build Status](https://jenkins.devgao.tech/job/Artemis/job/master/badge/icon)](https://jenkins.devgao.tech/job/Artemis/job/master/)
 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/DevgaoEng/artemis/blob/master/LICENSE)
 [ ![Download](https://api.bintray.com/packages/develgao/devgao-repo/artemis/images/download.svg) ](https://bintray.com/develgao/devgao-repo/artemis/_latestVersion)
 [![Gitter chat](https://badges.gitter.im/devgaoEng/artemis.png)](https://gitter.im/devgaoEng/artemis)

Implementation of the Ethereum 2.0 Beacon Chain.

Based on the (evolving) [specification](https://notes.ethereum.org/SCIg8AH5SA-O4C1G1LYZHQ?view).

## Build Instructions

To build, clone this repo and run with `gradle` like so:

```
git clone --recursive https://github.com/DevelGao/BeaconChain
cd beaconchain
gradle
```

After a successful build, distribution packages will be available in `build/distributions`.

## Code Style

We use Google's Java coding conventions for the project. To reformat code, run: 

```
gradle spotlessApply
```

Code style will be checked automatically during a build.

## Testing

All the unit tests are run as part of the build, but can be explicitly triggered with:
```
gradle test
```
The integration tests can be triggered with:
```
gradle integrationTest
```
