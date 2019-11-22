gradle-plugin
=============

Purpose
-------

This is a gradle plugin for xDDL support.

Introduction
------------

The Gradle plugin assumes a certain project structure by default, though you
can customize the tasks how ever you like.

 * src/
    * main/
        * xddl/
            * Specification.xddl.json <- the main xDDL file
            * includes/ <- includes directory (recursive)
                MyStruct.xddl.json <- example
            * patches/ <- the top level folder for Glide patch versions.
                * 1.0.1/ <- folder for a particular patch version
                    MyStruct.patch.json <- example

Simple Tasks
------------


You can include the xDDL tasks in your Gradle file like:

```groovy
plugins {
    id 'net.kebernet.xddl'
}

```

#### XDDLGenerate

This allows you to generate artifacts from your xDDL sources. Commonly, you might
want to generate Java classes. This would be done as below:

```groovy
task xddlJava(type: XDDLGenerate){
    plugin = "java"
    outputDirectory = file("${project.buildDir.getAbsolutePath()}/xddl-java")
}
``` 
Next, you need to make a SourceSet for your generated Java classes, and add the 
generated classes to your classpaths.

```groovy
sourceSets {
    xddl {
        java.srcDir file("${project.buildDir.getAbsolutePath()}/xddl-java")
    }

    main {
        runtimeClasspath += xddl.output
    }
    test {
        runtimeClasspath += xddl.output
    }
}
```

Finally, add the task to your dependency tree.

```groovy
compileJava.dependsOn xddlJava
```

Other common examples:

```groovy
task xddlGraphvis(type: XDDLGenerate){
    plugin = "graphvis"
    outputDirectory = file(project.buildDir.getAbsolutePath()+ "/reports/xddl")
}

task xddlMarkdown(type: XDDLGenerate){
    plugin = "markdown"
    outputDirectory = file(project.buildDir.getAbsolutePath()+ "/reports/xddl")
}
```

##### Configuration Parameters

 * ``sourceFile`` File (default: file('src/main/xddl/Specification.xddl.json'))
 * ``includeDirectories`` List of files (default: [file('src/main/xddl/includes')])
 * ``outputDirectory`` File (default: file('build/xddl'))
 * ``plugin`` String REQUIRED
 
 #### XDDLUnify
 
 This task takes a specification, includes folders, and a collections of patches
 and generates a unified *.xddl.json document with the merged versions.
 
 ##### Configuration Parameters
 
  * ``sourceFile`` File (default: file('src/main/xddl/Specification.xddl.json'))
  * ``includeDirectories`` List of files (default: [file('src/main/xddl/includes')])
  * ``patchDirectory`` File REQUIRED
  * ``outputFile`` File (default file('build/xddl/Unified.xddl.json))
  * ``newVersion`` String (optional) the new version identifier to put in the unified file.


Glide Tasks
-----------

The 'Glide' tasks are xDDL's version of [Flyway](https://flywaydb.org/).

You can include patch sets in named, semver-style directories, and Glide will `Unify` each version,
the apply the next versions patches, the give you an xDDL with the patch directives for each subsequent
version. See [the simple example here](./src/functional/projects/xddl-glide)

The basic config looks something like this:

```groovy
task glide(type: XDDLGlide){ // This just runs the Glide task with defaults
}

task glideGenerate(type: XDDLGlideGenerate){ // This generates interim Java classes for each version
    plugin "java"
    outputDirectory file("${project.buildDir}/xddl-java")
}
```

For our sample project, we will end up with three unified files in the ``build/glide`` directory:

 * baseline.xddl.json This is just the Specification.xddl.json file and all the includes unified.
 * 1_0_1.xddl.json This contains the xddl and patch directives to migrate from the baseline to v1.0.1
 * 1_0_2.xddl.json This contains the xddl and patch directives to migrate from 1.0.1 to 1.0.2
 
 *IMPORTANT*
 
 Additionally, after running XDDLGlide task, it will update the project.version attribute in your
 Gradle project to be the highest semver value it found.
 
 When an XDDLGlideGenerate task runs, it will run for ALL the generated, unified versions. So when
 we run the ``java`` plugin, it will generate:
 
  * build/xddl-java
    * xddl/ <- the default base package.
        * v1_0 <- Package containing the 1.0 version classes
        * v1_0_1 <- Package containing the 1.0.1 version classes
        * v1_0_2 <- Package containing the 1.0.2 version classes
        
For more about patches, see the [migrate-plugin](../xddl-plugin-migrate/README.md)
        
 ##### Configuration Parameters
 
 XDDLGlide has the following:
 
 * ``sourceFile`` File (default: file('src/main/xddl/Specification.xddl.json'))
 * ``includeDirectories`` List of files (default: [file('src/main/xddl/includes')])
 * ``patchesDirectory`` File (default file('src/main/xddl/patches))
 * ``outputDirectory`` File (default file('build/glide'))
 
It also exposes the output value:

 * ``outputFiles`` List of File sorted from baseline to the highest SemVer value.
 
XDDLGlideGenerateTask has the following:
 * ``outputDirectory`` File (default file('build/xddl'))
 * ``plugin`` String REQUIRED
 * ``glideDirectory`` File (default file('build/glide'))
                