# Sunshine-port of SPT

This repository contains a verions of the Spoofax Testing Language (SPT)
that is in the process of being ported to use Sunshine (Spoofax without Eclipse)
instead of the Eclipse Plugin related libraries.

As it is still a work in progress, it breaks a lot of the SPT plugin's features,
but as a result you do get to run your testcases from the command line.
This branch may also introduce errors and does not yet support all kinds of testcases.

> What kind of testcases *are* supported?  
> I have no idea.
The `parse succeeds`, `parse fails`, `resolve #x` and `resolve #x to #y` should be supported.

Note that this README is branch specific.

## Setup

To start using SPT in combination with Sunshine
you will need the [appropriate Sunshine version](https://github.com/VolkerL/spoofax-sunshine).
Open the `org.spoofax.sunshine` project in Eclipse.
If the libraries are not on the Github repo, please make a Github issue there to remind me.
As some dependencies would otherwise have to be obtained from an Eclipse instance's plugin folder
if it has the appropriate Spoofax Plugin.
To save you that effort, and as the jar files should be platform independent,
I see no harm in having them online (maybe the issue is License related???).

To 'build' Sunshine, you can right-click the project and export is as a runnable jar file.

Now open this repository's `org.strategoxt.imp.testing` project in Eclipse.

To build this version of SPT you will need to alter [the build file's](org.stratego.imp.testing/build.main.xml)
line 22 and 23 to point to where these files can be found.

The lazy way to get those files:

```
curl -O https://raw.github.com/metaborg/stratego/master/org.strategoxt.imp.editors.stratego/syntax/Stratego-Sugar.def
curl -O https://raw.github.com/metaborg/mb-rep/master/org.strategoxt.imp.editors.aterm/syntax/ATerm.def
```

Or you could clone [the stratego repo](https://github.com/metaborg/stratego)
and [the mb-rep repository](https://github.com/metaborg/mb-rep).
You can find both files in their respective `syntax` folders.

Now the final step to make it build is to set up the classpath for SPT's `build.main.xml` file.
Right click that file in Eclipse and go to `Properties`.
Edit the `Spoofax-Testing build.main.xml` run configuration and navigate to the `classpath` tab.
Here add the `sunshine.jar` file you created by building Sunshine.

Now `Ctrl+Alt+b` should start building the project.

> Fingers crossed!

If you encounter any problems that you cannot figure out,
I refer you to [this repository's issues](https://github.com/VolkerL/spt/issues).
Don't hesitate to ask your questions there.

## Usage

Assuming you have the Sunshine jar, an spt testcase you want to run and a grammar to run it on,
here is how to do so:

```Shell
java -cp sunshine.jar org.spoofax.sunshine.drivers.Main
  --auto-lang path-to-repo/org.strategoxt.imp.testing
  --project path-to-directory-with-test-cases/
  --builder test-runner-file
  --build-on testcase.spt
  --no-analysis
```

Sunshine options explained:

- *auto-lang* - Sunshine loads all languages (you probably want SPT and your own language under test)
  that are located somewhere in the specified folder.
  A language is identified by the `include/languagename.packed.esv` file.
  
  *Note:* This means that the example above only works if you make a symlink to your language
  in the `repo/org.strategoxt.imp.testing` folder.
  Alternatively, you could just change the argument to `auto-lang` so both the SPT project
  and your own language are in a subdirectory of that folder.
- *project* - This option specifies the project path.
  In this case it is the path to the project that hosts your SPT test cases.
- *builder* - Specifies which builder should be executed on the test cases.
  I picked `test-runner-file` as that is the only one I tested so far.
  This builder runs the testcases in the given file.
- *build-on* - This is how you tell Sunshine on what file the builder should be run.
  If your testcase is located in the root of your `project` and called `testcase.spt`
  then you don't have to change anything here.
  If you want to run multiple files at once, that might be possible with the `build-on-all` option,
  but I didn't test that yet.
- *no-analysis* - As Sunshine expects the observer strategy of your language to be able to handle a list of files
  and SPT does not yet handle that, analysis would fail.
  The hardcore SPT developer might be interested to know that the current use of Dynamic Rules
  in the `editor-analyze` might make it hard for such an approach to work.
  Unless these rules are properly scoped, but I wouldn't know anything about that.

The output should start appearing on your screen.
At the moment it should just be a bunch of logging from Sunshine,
mixed with a lot of debug stuff from `debug` and `println` statements in SPT.

We are still looking to add proper test report generation.
Either by making a separate builder for it, or by leveraging the testlistener structure
that is currently broken due to this port.

## Disclaimer

This is still very much a work in progress.
Feel free to contribute and leave your questions or remarks in [the issues](https://github.com/VolkerL/spt/issues).
