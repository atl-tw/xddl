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
    * ``compareToIncludeProperties``: IF implements includes ``java.lang.Comparable<SomeType>`` then a
      ``compareTo()`` method will be generated. This field includes an order of precedence list of property
      names that will determine the result of the comparison. They should all be primitive types or Comparables.
      You can invert the order of any given property by placing a ``!`` character at the beginning of the name. For
      example: ``"compareToIncludeProperties":["familyName", "surname", "!dateOfBirth"]`` will give your the YOUNGEST
      people with matching names first. All properties will for comparison will be accessed via getters.
      If this property is not included, ALL properties will be included in the order listed.
 * Property:
    * ``type``: A fully qualified Java type for the property. eg, use ``java.util.Date`` for 
      the ``DATETIME``. For List types, they should be a non-parameterized collection name.
    * ``equalsHashCodeWrapper``: (List properties only) fully qualified class name for a Collection
        type to wrap lists in for doing equality. This will default to ArrayList -- that is, it will
        copy all list types into an array list to ensure deep equals works irrespective of the j.u.List
        implementation on the class. This can also be ``none`` to disable this entirely and attempt to
        use the ``equals`` method on the collection directly.
    * ``initializer``:  Contains a Java statement that will be the initializer for the field
        (ex ``"intitializer": "new java.util.ArrayList<>()"`` for an empty list or ``"intitializer": "\"foo\"""``
        to initialize a default String value to ``foo``.
        
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