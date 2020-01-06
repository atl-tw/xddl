The xDDL Specification
======================

Before We Start
---------------

In this document, we are using the command line version of xddl, [available from JCenter](
https://jcenter.bintray.com/net/kebernet/xddl/xddl/0.9.0/xddl-0.9.0-distribution.zip).


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

Important Conventions
---------------------

While it is not *required* that you do so, you are encouraged to follow these naming conventions within your xDDL 
specification. Doing so will give you the best possible results when generating artifacts from the various plugins.

 * Structures should have UpperCamelCase names.
 * Property names should be lowerCameCase.
 * Specification-level types should be lower_snake_case.
 
 
 