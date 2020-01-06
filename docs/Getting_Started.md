Getting Started
===============

Hi, do you have a problem of keeping a number of different specifications for
data structures in sync? You've got your JSON rest API; you've got Hibernate
code; you've got ElasticSearch; you need a way for everyone to agree on what 
EXACTLY your model is.

xDDL is here to help.

The Specification File
----------------------

The core of xDDL is the specification file. It looks something like this:

```json
{
  "title": "My Specification",
  "version": "1.0",
  "description": "This is my specification, there are many like it but this one is mine.",
  "entryRef": "Book",
  "types": [
    {
      "@type": "Type", "core": "BIG_DECIMAL",
      "name": "money","description": "This is a currency value of an accurate floating point value",
      "ext": {
        "elasticsearch": {"type": "scaled_float", "scaling_factor": "100"}
      }
    }   
  ],
  "structures": [
    {
      "@type": "Structure", "name": "Book","description": "A book for sale",
      "properties": [
        { "@type": "Type", "core": "STRING", "name": "title"},
        {
          "@type": "List", "name": "authors",
          "contains": { "@type": "Type","core": "STRING"}
        },
        {"@type": "Reference","ref": "money","name": "price"} ,
        {"@type": "Reference","ref": "money","name": "tax"} 
      ]   
    }   
  ]
}
```

Here we have a definition for a Book specification. Lets work through it point by point.

*  ``title`` is the title of the specification.
*  ``version`` is the version of the spec.
*  ``description`` is a Text/Markdown description of the specification.
*  ``comment`` is a developer comment on the type.
*  ``entryRef`` is a reference to a Structure name that is the stop level of the specification.
   This is optional for the spec, but required for some plugin generations 
   (like JSON-schema, or ElasticSearch)
* ``types`` is an array of ``Type`` values that you will "reference" in you structures. 
    * These all have the attribute ``"""@type":"Type"``
    * They have a ``name`` attribute used for reference.
    * They have a ``core`` attribute that is one of:
        * ``STRING``: A short string value, however you choose to define it.  
        * ``TEXT``: A long text value, however you choose to define it.
        * ``DATE``: A calendar date.
        * ``TIME``: A time of day.
        * ``DATETIME``: A date and time
        * ``INTEGER``: A 32 bit integer.
        * ``LONG``: A 64 bit integer.
        * ``BOOLEAN``: A flag
        * ``FLOAT``: A 32 bit floating point value.
        * ``DOUBLE``: A 64 bit floating point value.
        * ``BIG_INTEGER``: An exact and arbitrary integer value.
        * ``BIG_DECIMAL``: An exact and arbitrary decimal value.
        * ``BINARY``: A collection of bytes representing a binary value.
    * They can have a ``description`` value to describe the type.
    * They can have a ``comment`` value, that is a literal string value containing a comment on the type.
    * They can have an array of ``allowable``. This array contains list of objects in the format:
      ``{ "value": [Any Object of the type], "description": "A description of the object".``.
      
      For example: ``{ 
        "@type": "type",
        "core": "STRING",
        "name": "maritalStatus"
        "allowable": [
            {
                "value": "SINGLE",
                "description": "Never married."
            },
            {
                "value": "MARRIED",
                "description": "Currently married."
            },
            {
                "value": "DIVORCED",
                "description": "Legally divorced."
            },
            {
                "value": "WIDOWX",
                "description": "Widow/Widower"
            }   
        ]
      ``
    * They can also have an array of example values, that are like the allowable values, except the user should
      not infer that the list is enforced.
    * ``ext`` This is "Extensions". Extensions are really the core of xDDL. Anything under the ``ext`` structure
      is specific to an xDDL plugin. In the example we are providing an ElasticSearch definition for the 
      ``money`` type to say that it should be an arbitrary scaled floating point value with a precision of two
      decimal places. The particular list of values under any given node in the ``ext`` structure is defined by
      the plugin that handles it.
* ``structures`` is an array of complex objects in your spec. 
    * They have an array of ``properties`` that can be of any ``@type`` value:
        * ``Structure`` a nested structure.
        * ``Reference`` a reference to a type or structure in the top level lists
        * ``List`` a list of any type.
        * ``Type`` another simple type.    
        
        
Generating Artifacts
--------------------
        
What can we do with our xDDL file now? We can generate new artifacts from it. When we run:

```bash
xddl generate -f markdown -f json -f elasticsearch -o . -i MySpecification.xddl.json 
```

We are saying "Generate artifacts for the markdown, json, and elasticsearch plugins from
my specification file into the current directory. You can see the results in the
[examples](./examples) folder.

The Markdown plugin generated ``my_specification.md`` and ``my_specification.html`` with
reference documentation:

```markdown
My Specification
===========================================================
_Version: 1.0_

This is my specification, there are many like it but this one is mine.


Contents
--------

1. Structures
   1. [Book](#Book)
1. Types
   1. [money](#money)


Structures
----------

<a name="Book"></a>
### Book
A book for sale
```
...and so on.

The JSON plugin generated a JSON Schema document from out specification in ``schema.json``:

```json
{
  "$schema" : "http://json-schema.org/draft-07/schema#",
  "$ref" : "#/definitions/Book",
  "definitions" : {
    "Book" : {
      "title" : "Book", "description" : "A book for sale", "type" : "object",
      "properties" : {
        "price" : {
          "title" : "price",
          "description" : "This is a currency value of an accurate floating point value",
          "type" : "string",
          "pattern" : "^-?\\d*(\\.\\d*)?$"
        },
```
... and so on. Here you can see the BIG_DECIMAL money type was adapted to a "string" with
a validation pattern. Plugins should provide a reasonable default output for each of the
built in core types, but you can always override this behavior using the ``ext`` structures.

For the Elasticsearch plugin, we got ``my_specification.mappings.json``:

```json
{
  "mappings" : {
    "_default_" : { "dynamic" : "strict"},
    "my_specification-1.0" : {
      "properties" : {
        "title" : {"type" : "keyword"},
        "authors" : {"type" : "keyword"},
        "price" : {"type" : "scaled_float", "scaling_factor" : "100"},
        "tax" : { "type" : "scaled_float", "scaling_factor" : "100"}
      }
    }
  },
  "settings" : { }
}
```

Here you can see the mapping configuration for the ``price`` property was copied from the extension structure:
```json
{
    "ext": {
        "elasticsearch": {
          "type": "scaled_float",
          "scaling_factor": "100"
        }
      }
}
``` 

What exactly you can put into the extensions structure for a plugin will vary by plugin.

The core plugins are:
 * [``markdown``](../xddl-plugin-markdown/README.md): To generate ``.md`` and ``.html`` documentation.
 * [``graphvis``](../xddl-plugin-graphvis/README.md): To generate ``.dot`` and ``.png`` graphs that show the relationships 
    between your structure types.
 * [``plantuml``](../xddl-plugin-plantuml/README.md): To generate ``.puml`` diagram that show the relationships 
    between your structure types. 
 * [``json``](../xddl-plugin-json-schema/README.md): To generate a JSON-Schema file.
 * [``elasticsearch``](../xddl-plugin-elasticsearch/README.md): To generate an Elasticsearch Mappings structure for indexing your 
   document structure.
 * [``java``](../xddl-plugin-java/README.md): To generate a set of Java classes suitable for mapping to your specification.
 * [``swift``](../xddl-plugin-swift/README.md): To generate a SwiftPM library suitable for mapping your specification.
 * [``hive``](../xddl-plugin-hive/README.md): Generates table specs for Apache Hive/Amazon Athena
 * [``migrate``](../xddl-plugin-migrate/README.md): Generates a class to migrate data from one version of the spec to another.
 
Best Practices
-------------- 
 
Each of the plugins has different capabilities and limitations right now. However, there
are some good rules of thumb to get you started:

 * Lists of Lists (multidimensional arrays) can be problematic.
 * Be careful of graph recursions, such as ``Person.children[List(Person)]``. For things
   like ElasticSearch where the graph is unrolled completely, this will not work.
 * Naming Referable types:
    * Types should be in "lower_snake_case".
    * Structures should be in "UpperCamelCase".
 * Breaking up large schemas...
   You can use the ``--include-dir, -d`` option on the command line to automatically include
   ``*.xddl.json``. Any ``{ "@type": "Type"... }`` or ``{ "@type":"Structure"... }`` elements 
   parsed will be automatically added to your main xDDL specification as it is parsed.
``
          
          
Templating
----------

Any "Stringish" value that isn't a property name can be "templated" using
[OGNL](http://commons.apache.org/proper/commons-ognl/language-guide.html) syntax
enclosed in a ```${}``` block. If you need to include ``${`` as string, you can escape it
with ``\${``.

A common example is a ``version`` property. The specification has a version, and 
you might want to use this within your specification as a constant.

For example:

```json
{
  "@type": "Type",
  "core": "STRING",
  "name": "version",
  "description": "The version",
  "required": true,
  "ext": {
    "java": {
      "initializer": "\"${specification.version}\""
    }
  }
}
```

This will ensure that when you create a new Java bean generated from your schema,
the "version" attribute will have the version from your xDDL specification.

In most places you can also provide a set of ``vals`` that can be injected during
generation. These would be referencable using ``${vals.something.theOtherThing}``.
Using the command line you can provide a JSON file that can contain these values. 
When using the gradle plugin, you can provide a ``Map``, a file, or both.