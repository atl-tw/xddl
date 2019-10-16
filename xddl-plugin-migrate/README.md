migrate
=====++

Purpose
-------

This is a plugin that generates Java code that will use Jackson to migrate an object
instance of one version to an object instance of another version.

Concepts
--------

### Patch Files

xDDL supports expressing "patch operations" as part of a specification. These are meant
to express what you need to migrate a document/object from one version of the specification
to the next. A patch file is simply another xDDL document that includes directives that 
guide this process.

