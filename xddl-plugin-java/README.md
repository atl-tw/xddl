java
=====

Purpose
-------

This is a plugin that turn xDDL to Java types with the extension value "java".

Notes
-----

For Enum values that are not Java Name Friendly, it will use the Jackson "@JsonValue"
annotation.

Supported Extensions
--------------------

 * Specification:
    * ``package``: The base package name the types will be written into. Will be postfixed
      with the v[specification version with dots replaced with underscores] if there is a version defined.
 * Structure:
    * ``implements``: Array of fully qualified interface names that will be 
    applied to the generated class.
 * Property:
    * ``type``: A fully qualified Java type for the property. eg, use ``java.util.Date`` for 
      the DATETIME. 
    * ``initializer``: A string containing a Java snippet to initialize.
    * ``equalsHashcodeWrapper``: (List properties only) fully qualified class name for a Collection
        type to wrap lists in for doing equality.
    * ``"initializer": "new java.util.ArrayList<>()"`` to initialize a default value. For example
        an empty list