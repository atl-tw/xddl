xDDL
====

(Extensible Data Definition Language)

Purpose
-------

This is a parser and generator plugin system that allow you to generate multiple source/DDL artifacts from a single, 
easily editable, easily auditable specification format. By keeping all your definitions in one place, you make all 
bugs shallow.

> There are two ways of constructing a software design: One way is to make it so simple that there are obviously no 
> deficiencies, and the other way is to make it so complicated that there are no obvious deficiencies. The first method
> is far more difficult.
>
> -- Tony Hoare

We want to make it easy to build something where there are obviously no deficiencies.

Principles
----------

xDDL is based on on couple of simple ideas:

  1. Provide a simple, universal way to define an object graph.
     
     This borrows from JSON Schema and any of a million other things.
  1. Provide a simple, language independent way to provide implementation details for specific outputs.
     
     We don't need to cover every possible possible output format, if it is easy to add custom values to a field or 
     structure.
     
  1. Provide a code execution path that is suitable for automated dependency generation.
  
     We want you to turn this format into DDLs for whatever format you need: JSON, ElasticSearch, SQL, etc.

Getting Started
---------------

Please see the [Getting Started Guide](./doc/Getting_Started.md).

     
Simple Example
--------------

```json
{
  "types": [
    {
      "@type": "Type",
      "name": "optional_text",
      "description": "An optional string value of any size that is searchable as text, not as a token value",
      "core": "STRING",
      "ext": {
        "sql": {
          "datatype": "CLOB default ''"
        },
        "elasticsearch": {
          "type": "text"
        }
      }
    },
    {
      "@type": "Type",
      "name": "exact_short_string",
      "description": "A string value searchable on exact value",
      "core": "STRING",
      "ext": {
        "sql": {
          "datatype": "VARCHAR(255) default null"
        },
        "elasticsearch": {
          "type": "keyword"
        }
      }
    }
  ],
  "structures": [
    {
      "@type": "Structure",
      "name": "currency",
      "description": "An expression of a currency value.",
      "properties": [
        {
          "@type": "Type",
          "name": "value",
          "description": "The value in American decimal notation",
          "core": "BIG_DECIMAL",
          "required": true,
          "ext": {
            "java": {
              "annotations": "@Column(precision = 12, scale = 3)"
            },
            "sql": {
              "type": "NUMERIC(12,3)"
            },
            "elasticsearch": {
              "type": {
                "type": "scaled_float",
                "scaling_factor": 1000
              }
            }
          }
        },
        {
          "@type": "Reference",
          "name": "formatted",
          "description": "The value formatted to the correct locale.",
          "ref": "optional_text"
        },
        {
          "@type": "Reference",
          "name": "code",
          "description": "The ISO currency code represented by the value",
          "ref": "exact_short_string",
          "required": true
        }
      ]
    }
  ]
}
```

A specification is defined as such: 

```javascript
{
  "description" : "A description of the general spec",
  "types": [], // A list of low level types. Basically these are references
               // you can use as primitives.
  "structures": [] // A list of structures that define the composite types of
                   // your specification. You can @Reference the types, or other
                   // Structures within this list.
}

```

Above we define a couple of simple types: an ``optional_text`` type that is just a free text field, an 
``exact_short_string`` field that is a short string value that is queryable by exact value in both SQL and 
ElasticSearch. Finally, we construct a ``Structure`` to represent a currency value. The currency value has an 
arbitrary precision floating point value (``BIG_DECIMAL``), a currency code (``exact_short_string``) and a locale 
specific formatted value (``optional_text``).

The ``ext`` (extension type) structure at each level allows us to provide code literals for particular languages and platforms to be
used in generating an artifact from the xDDL specification. These are unbounded and can be used for any generation 
plugin.

Hopefully the value of this specification becomes obvious when you get to the ``value`` property: There are multiple 
definitions of a particular value, but they are closely clustered.
 * It should be obvious if there is a disagreement between...
    * A JPA annotation in Java.
    * A SQL column definition.
    * An ElasticSearch field definition.
 * We aren't fixing any problems between these platforms, but hopefully we are making the bugs more shallow. If you see
   that your SQL type is (12,3) it should be obvious to you if your scaling_factor isn't 1000.
   
Code Generation
---------------

Once you have defined you data sufficiently, you need to get it to a usable form. This is done through plugins defined
by [JAR Services](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) that implement the
``net.kebernet.xddl.plugins.Plugin`` class. Plugins receive a ``net.kebernet.xddl.plugins.Context`` object that they 
can use to generate their specific code.

The ``json-schema`` module is a basic example of a code generator based on the ``json`` extension type.

Download
--------

Distributions are available at [bintray](https://bintray.com/kebernet/maven/xddl). The simplest
version is the "xddl" distribution zip that contains an executable packaged with the plugins.


Execution
---------

You can run ``xddl.bat generate`` or ``xddl generate`` depending on your OS.

```
Usage: generate [options]
  Options:
 * --format, -f
      The output plugin to generate
      Default: [markdown, json, elasticsearch]
    --help
      Show this help text
    --include-dir, -d
      Directory(ies) to scan for *.xddl.json files to include.
  * --input-file, -i
      The specification file.
  * --output-directory, -o
      The directory to output generated artifacts to.
      Default: .
    --stacktrace
      Show the stacktrace of an error
      Default: false


```

 
 

Build
-----

``gradlew build``