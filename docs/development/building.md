# Checkout code and build it
## Checkout source code

```
git clone --recursive git@github.com:DevelGao/hailong.git
```
OR
```
git clone --recursive https://github.com/DevelGao/hailong
```

## See what tasks are available
To see all of the gradle tasks that are available:
```
cd hailong
./gradlew tasks  
```


## Build from source
After you have checked out the code, this will build the distribution binaries.
```
cd hailong
./gradlew build  
```

## Run tests
All the unit tests are run as part of the build, but can be explicitly triggered with:
```
./gradlew test
```

### Ethereum reference tests

TBD

Please see the comment on the `test` target in the top level `build.gradle`
file for more details.
