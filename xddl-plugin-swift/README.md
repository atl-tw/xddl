swift
=====

Purpose
-------

The Swift plugin generates a net.kebernet.xddl.swift.SwiftPM library that contains all the same targets as the Java module. They will be in
the format of "Specification Name"-> "SpecificationNameV1_1". It should be suitable to tagging and publishing to a 
git repository.

Extensions
----------

 * Specification
   * ``libraryName`` -- the name of the Swift Library (default: Specification Title as UpperCamelCase)
   * ``dependencies`` -- an array of possible dependencies for the library
    * ``url`` -- the url to the git repository
    * ``from`` -- the lowest acceptable version
    * ``to`` -- the highest acceptable version
    * ``exclusive`` -- if the highest acceptable version should be excluded ("..<")
    * Understand that if there are multiple versions of a spec being built into a library, the dependencies delcared
      in the highest version, win.
 * Type (any type)
   * ``imports`` -- a string list of imports to support the type.
 * Property
   * ``type`` -- the native type of the field (must be Codable)
   * ``fieldName`` -- the encoded (JSON) name of the field.
   
Output
------

This plugin should build a Swift Library directory with a Package.swift file containing targets that include all 
packaged versions of the specification.
