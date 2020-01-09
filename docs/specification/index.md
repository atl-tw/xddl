Writing an xDDL Specification
=============================

Before We Start
---------------

In this document, we are using the command line version of xddl, [available from JCenter](
https://jcenter.bintray.com/net/kebernet/xddl/xddl/0.9.0/xddl-0.9.0-distribution.zip). You might need to change the
version numbers.

This document was created based on the 0.9.0 version of the project.

Table of Contents
-----------------

 1. [Core Concepts](#core)
 1. [Conventions and Practices](#conv)
 1. [Referencing Other Data](#data)
 1. [Patching Between Versions](#patch)
 1. [Using Command Line Tools](#cli)
 
 
<a name="core"></a>

Core Concepts
-------------

An xDDL Specification is composed of a set of ``Structures``. ``Structures`` have ``Properties``, which can be ``Types``, 
``Lists``, or other ``Structures``. Each of these can have "Extensions" which contain extended data specific to a particular runtime,
language, environment, data store, etc.

Let's start by looking at a minimal example:

```json
{
  "structures": [
    { "@type": "Structure", "name": "Person",
      "properties": [
          {"@type": "Type", "core": "STRING", "name": "firstName"},
          {"@type": "Type", "core": "STRING", "name": "lastName"}
      ]
    } 
  ]
}
```

Here we have created an xDDL specification with a single Structure definition, called "Person", that has two Properties,
``firstName`` and ``lastName``. These are ``Types``, or basic values with a "core" type of ``STRING``. The core type 
carries with it some implicit settings for the various xDDL plugins, so picking the right core type can be important.

The ``core`` values for a type should be one of:

 * ``STRING``: A short string value, however you choose to define it.  
 * ``TEXT``: A long text value, however you choose to define it.
 * ``DATE``: A calendar date.
 * ``TIME``: A time of day.
 * ``DATETIME``: A date and time.
 * ``INTEGER``: A 32 bit integer.
 * ``LONG``: A 64 bit integer.
 * ``BOOLEAN``: A flag
 * ``FLOAT``: A 32 bit floating point value.
 * ``DOUBLE``: A 64 bit floating point value.
 * ``BIG_INTEGER``: An exact and arbitrary integer value.
 * ``BIG_DECIMAL``: An exact and arbitrary decimal value.
 * ``BINARY``: A collection of bytes representing a binary value.
 
Next, lets add another Structure to our specification:

```json
{
  "structures": [
    { "@type": "Structure", "name": "Person",
      "properties": [
          {"@type": "Type", "core": "STRING", "name": "firstName"},
          {"@type": "Type", "core": "STRING", "name": "lastName"}
      ]
    },
    { "@type": "Structure", "name": "OrganizationalUnit", 
      "properties": [
        {"@type": "Type", "core": "STRING", "name": "name"},
        {"@type": "List", "name": "members", 
         "contains": { "@type": "Reference", "ref": "Person"}}     
      ]
    }
  ]
}
```

Now we have an ``OrganizationalUnit`` that has a name, and a ``List`` called ``members``. The List type has an attribute
called ``contains`` that specifies what it is a list "of". Here we use ``"@type": "Reference"`` to say we are going to 
reference something else in the specification and ``"ref": "Person"`` to refer to the structure we created before.

 * Lists should "contain", Structures, Types, or References to Structures and Types.
 * Lists may contain nested Lists, but this is discouraged as it is problematic to support in all places where you might
   want to use your specification.
 * References may refer to Structures or Types at the root level of the specification. (More on this below)
 
The final core concept is the idea of an "Extension". Extensions, in the ``ext`` attribute allows you to pass extended
information about any level of the specification to plugins to be used for generating artifacts.

Now we can use the "generate" command to create a JSON Schema equivalent for our specification:

```bash
xddl generate --input-file ./step2.xddl.json --format json --output-directory .
```

Which gives us:

```json
{
  "definitions" : {
    "OrganizationalUnit" : {
      "title" : "OrganizationalUnit",
      "type" : "object",
      "properties" : {
        "members" : {
          "title" : "members",
          "type" : "array",
          "items" : {
            "$ref" : "#/definitions/Person"
          }
        },
        "name" : { "title" : "name", "type" : "string"}
      }
    },
    "Person" : {
      "title" : "Person",
      "type" : "object",
      "properties" : {
        "firstName" : { "title" : "firstName", "type" : "string" },
        "lastName" : {"title" : "lastName", "type" : "string"}
      }
    }
  },
  "$schema" : "http://json-schema.org/draft-07/schema#",
  "$ref" : "#/definitions/null"
}
```

This looks remarkably close to our original specification file, but things are renamed a bit. Also we ended up with 
``"$ref" : "#/definitions/null"``. Let's iterate on our specification again...

```json
{
  "entryRef": "OrganizationalUnit",
  "types": [
    {"@type": "Type", "name": "human_name", "core": "STRING",
      "ext": {
        "json": {
          "minLength": 1, "maxLength": 255, "pattern": "[A-z-']*"
        }
      }
    }
  ],
  "structures": [
    { "@type": "Structure", "name": "Person",
      "properties": [
        {"@type": "Reference", "ref": "human_name", "name": "firstName", "required": true},
        {"@type": "Reference", "ref": "human_name", "name": "lastName", "required": true}
      ]
    },
    { "@type": "Structure", "name": "OrganizationalUnit",
      "properties": [
        {"@type": "Type", "core": "STRING", "name": "name","required": true},
        {"@type": "List", "name": "members",
          "contains": { "@type": "Reference", "ref": "Person"}}
      ]
    }
  ]
}
```

Here we have added an ``entryRef`` which is the name of the Structure that represents the top level of a document. We
have marked a few properties as ``required``, and we have create in the ``types`` list a new type called ``human_name``,
which we use as a ``Reference`` for firstName and lastName.


```bash
xddl generate --input-file ./step3.xddl.json --format json --output-directory .
```

This gives us:

```json
{
  "definitions": {
    "OrganizationalUnit": {
      "title": "OrganizationalUnit",
      "type": "object",
      "properties": {
        "members": {
          "title": "members",
          "type": "array",
          "items": {
            "$ref": "#/definitions/Person"
          }
        },
        "name": {
          "title": "name", "type": "string"
        }
      },
      "required": ["name"]
    },
    "Person": {
      "title": "Person", "type": "object",
      "properties": {
        "firstName": {
          "title": "firstName", "type": "string", "minLength": 1, "maxLength": 255, "pattern": "[A-z-']*"
        },
        "lastName": {
          "title": "lastName", "type": "string", "minLength": 1, "maxLength": 255, "pattern": "[A-z-']*"
        }
      },
      "required": ["firstName", "lastName"]
    }
  },
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$ref": "#/definitions/OrganizationalUnit"
}
```

Now we see the values from our ``ext: { json:`` tree copied into the JSON schema for the firstName and lastName. Since
each of these is referenced as ``human_name``, we don't need to duplicate the extended configuration multiple places. 
You also now have a JSON Schema document with which you can used to validate OrganizationalUnit documents.


<a name="conv"></a>

Conventions and Practices
-------------------------

While it is not *required* that you do so, you are encouraged to follow these naming conventions within your xDDL 
specification. Doing so will give you the best possible results when generating artifacts from the various plugins.

 * Structures should have UpperCamelCase names.
 * Property names should be lowerCameCase.
 * Specification-level types should be lower_snake_case.
 
As you can imagine, keeping all your definitions in a single file can become overwhelming if your definition is large.
You can break this up by using the `` --include-dir`` options on the command line. This will scan a directory of files
named *.xddl.json and place them into the types or structures groups where appropriate. You can then break your specification
down to:

```json
{
  "title": "My Specification", 
  "version": "1.0",
  "entryRef": "OrganizationalUnit"
}
```
 
And separate your files into, for example...

``human_name.xddl.json``
```json
{"@type": "Type", "name": "human_name", "core": "STRING",
  "ext": {
    "json": {
      "minLength": 1, "maxLength": 255, "pattern": "[A-z-']*"
    }
  }
}
```
 
This makes developing and updating you specifications in an IDE much easier since you might not have to search withing
a hundreds (or thousands) of lines specification to locate what you need to edit.

Looking at the help text for the ``generate`` command, we see:
```bash
xddl generate --help
```
```
Usage: generate [options]
  Options:
  * --format, -f
      The output plugin to generate
    --help
      Show this help text
    --include-dir, -d
      Directory(ies) to scan for *.xddl.json files to include.
  * --input-file, -i
      The specification file.
  * --output-directory, -o
      The directory to output generated artifacts to.
    --stacktrace
      Show the stacktrace of an error
      Default: false
    --vals-file, -v
      JSON file of values
```
 
 
So if we run

```bash
xddl generate -i step4.xddl.json -d step4includes -f json -o . 
``` 

Then because we have provided a title and version to our specification document, we now get 
``My_Speciifcation_1.0.schema.json``. This naming convention is common among plugins. If we wanted to generate a 
tabled definition for [Apache Hive](https://hive.apache.org/) we could run...

```bash
xddl generate -i step4.xddl.json -d step4includes -f hive -o . 
``` 

...and we will generate a file called ``My_Specification_1.0.hive`` with our Hive table definition.

```hiveql
CREATE EXTERNAL TABLE IF NOT EXISTS My_Specification_1.0 (
  name varchar(255),
  members ARRAY<STRUCT<firstName:varchar(255), lastName:varchar(255)>>
)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES ('serialization.format' = '1',  'ignore.malformed.json' = 'true')
LOCATION ''

TBLPROPERTIES ('has_encrypted_data'='false');
```

<a name="data"></a>

Referencing Other Data
----------------------

Field values in xDDL can be interpreted as [OGNL](https://commons.apache.org/proper/commons-ognl/) expressions, which
allows you to reference values from within and without of the specification. Lets look at an example.

In our previous specification file, we hard coded the ``version`` to be "1.0", but this might not be specific enough.
Surely our project is being built on a server somewhere and we might want to be more specific. In this case we can say:

```json
{
  "title": "My Specification",
  "version": "1.0.${vals.buildNumber}",
  "entryRef": "OrganizationalUnit"
}
```

Now we can create a ``values.json`` file that looks like:

```json
{"buildNumber": "1234" }
```

And use the "unify" command to generate a version of our spec that will have the external value escaped it it...

```bash
xddl unify -i step5.xddl.json -d step4includes -v values.json -o current.xddl.json
```

... giving us:

```json
{
  "title" : "My Specification",
  "version" : "1.0.1234",
  "entryRef" : "OrganizationalUnit",
  "types" : [ {
    "@type" : "Type", "name" : "human_name", "core" : "STRING"
```
.. and so forth.

Let's look at a VERY common case:

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

Here we are copying the version from the specification file to the the initializer of the Java variable. You can see 
now the two major context object you have access to from OGNL:

 1. ``specification`` -- the actual specification itself
 1. ``vals`` -- the external values object, this can be read from a JSON file as we are doing here, or can be defined 
    in the build.gradle file if you are using the Gradle plugin. 
    

<a name="patch"></a>

Patching Between Versions
-------------------------

With the ``unify`` command, in addition to our "includes" directory, we have the option to specify a "patches" directory.
This is a directory that contains *.patch.json or *.xddl.json files that we will use to modify an existing specification.

These are simply more individual structure files that contain **only the changes** between versions. For example, if
we wanted to rename the "firstName" field on our "Person" structure, we could create ``Person.patch.json``

```json
{ "@type": "Structure", "name": "Person",
  "properties": [
    {"@type": "PATCH_DELETE",  "name": "firstName"},
    {"@type": "Reference", "ref": "human_name", "name": "givenName", "required": true}
  ]
}
```

Now we can specify a patches directory and a new version on the command line:    

```bash
xddl unify -i step5.xddl.json -d step4includes  -o current.xddl.json  -p ./step6patches --new-version 2.0
```

Giving us:

```json
{
  "title" : "My Specification",
  "version" : "2.0",
  "entryRef" : "OrganizationalUnit",
  // ...
  "structures" : [ 
  // ...
 {
    "@type" : "Structure",
    "name" : "Person",
    "properties" : [ {
      "@type" : "Reference",
      "name" : "lastName",
      "required" : true,
      "ref" : "human_name"
    }, {
      "@type" : "Reference",
      "name" : "givenName",
      "required" : true,
      "ref" : "human_name"
    } ]
  } ]
}
```    
 
With our new version from the command line populated, and the "givenName" field added to the Person structure. This is 
part of a larger conversation about data migration between versions. For that, you should consult the section on 
ElasticSearch migrations.

<a name="cli"></a>

Using Command Line Tools
------------------------

While most people use the Gradle plugins, the xDDL Command Line is fully featured and can be used to integrated the
functionality with whatever build chain you might have. Here we have mostly used the ``generate`` command to create
artifacts using the xDDL plugins. The other commands are:

 * ``unify`` -- Takes a collection of includes and/or patches, and generate a single xddl file that contains all the 
   defined structures.
   
```bash
xddl unify --help
```
Output:
```
Usage: unify [options]
  Options:
    --no-evaluate-ognl, -no-eval
      Disables OGNL evaluation.
    --help
      Show this help text
    --include-dir, -d
      Directory(ies) to scan for *.xddl.json files to include.
  * --input-file, -i
      The specification file.
    --new-version, -nb
      The version string of the unified file
  * --output-file, -o
      The file to output generated artifacts to.
    --patches-dir, -p
      Directory(ies) to scan for *.patch.json files to include.
    --scrub-patch, -s
      scrubs patch-delete operations from the original
      Default: false
    --stacktrace
      Show the stacktrace of an error
      Default: false
    --vals-file, -v
      JSON file of values

```   
   
 * ``glide`` -- Takes a specification and a directory of versioned patches and generates each of the interim xddl files
 for each version. (You can learn more about this in the models or elasticsearch documentation).
 
```bash
xddl glide --help
```
Output:
```
Usage: glide [options]
  Options:
    --glide-patches, -g
      Directory(ies) to scan for 'vXXXX' directories containing *.patch.json 
      files to include.
    --help
      Show this help text
    --include-dir, -d
      Directory(ies) to scan for *.xddl.json files to include.
  * --input-file, -i
      The specification file.
  * --output-directory, -o
      The file to output generated artifacts to.
    --stacktrace
      Show the stacktrace of an error
      Default: false
    --vals-file, -v
      JSON file of values

``` 
   
 * ``diff`` -- Outputs the diff between different xddl specs.   
 
 ```bash
 xddl diff --help
```
Output:
```
Usage: diff [options]
  Options:
    --comparision
      Show comparision rather than just missing fields
      Default: false
    --help
      Show this help text
  * --left-file, -l
      The left hand file.
    --left-include-dir, -ld
      Directory(ies) to scan for *.xddl.json files to include.
  * --right-file, -r
      The left hand file.
    --right-include-dir, -rd
      Directory(ies) to scan for *.xddl.json files to include.
    --stacktrace
      Show the stacktrace of an error
      Default: false
```
 
 