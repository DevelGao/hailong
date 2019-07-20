# Running Hailong

You can build and run Hailong with default options via:

```
./gradlew run
```

By default this stores all persistent data in `build/hailong`.

If you want to set custom CLI arguments for the Hailong execution, you can use the property `hailong.run.args` like e.g.:

```sh
./gradlew run -Phailong.run.args="--discovery=false --home=/tmp/hailongtmp"
```

which will pass `--discovery=false` and `--home=/tmp/hailongtmp` to the invocation.
