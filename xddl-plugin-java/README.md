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
      the ``DATETIME``. For List types, they should be a non-parameterized collection name.
    * ``initializer``: A string containing a Java snippet to initialize.
    * ``equalsHashCodeWrapper``: (List properties only) fully qualified class name for a Collection
        type to wrap lists in for doing equality. This will default to ArrayList -- that is, it will
        copy all list types into an array list to ensure deep equals works irrespective of the j.u.List
        implementation on the class. This can also be ``none`` to disable this entirely and attempt to
        use the ``equals`` method on the collection directly.
    * ``"initializer": "new java.util.ArrayList<>()"`` to initialize a default value. For example
        an empty list
        
Complex List Example
--------------------

With the following property def:
```json

{
  "@type": "List",
  "name": "listOfStrings",
  "contains": {
    "@type": "Type",
    "core": "STRING"
  },
  "ext": {
    "java": {
      "type": "java.util.Set",
      "initializer": "new java.util.LinkedHashSet<>()",
      "equalsHashCodeWrapper": "java.util.HashSet"
    }
  }
}
```

We have a base property type of ``Set<String>``, with an initializer of ``LinkedHashSet``, meaning the
strings will be kept in order if you just call add, but then an equalsHashCodeWrapper of ``HashSet``,
meaning the order of the strings will NOT be considered when evaluating ``equals()`` and ``hashCode()``.