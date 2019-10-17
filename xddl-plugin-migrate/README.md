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

Here is a 

Migration operates on Jackson JsonNode objects, and performs steps in a particular order
that is important to understand:

1. Apply migrations on child objects
1. Apply migrations on current objects
1. Delete deleted fields.

Let's imagine you have a xDDL specification for Structure called "Name" and 
it has a single field called "value" that contains an Anglo style name in 
"Last, First" format, but you want to break that up into firstName and lastName
fields for your "2.0" spec. Lets look at an example:

```json
{
  "version": "2.0",
  "entryRef": "Name",
  "structures": [
    {
      "@type": "Structure",
      "name": "Name",
      "properties": [
        {
          "@type": "PATCH_DELETE", //<-- We are going to delete this
                                   //    BUT it will be the last thing we do to the JsonNode, so it will be
                                   //    readable for our property synthesizers below.
          "name": "value"
        },
        {
          "@type": "Type",
          "core": "STRING",
          "name": "lastName",
          "ext": {
            "migration": {
              "stages": [             //<-- stages are lists of operation for creating a new value from 
                                      //    the old specification
                {
                  "@type": "jsonp",   //<-- This is going to be a series of JsonPath expressions
                  "start": "LOCAL",   //<-- We are going to start from the current structure. 
                                      //    you can also start from ROOT, at the top of the document, or
                                      //    omit to star from the current property value.
                  "steps": [
                    "$.value"         //<-- we select the "value" peer property
                  ]
                },
                { "@type": "regex",   //<-- The next state is a regular expression
                  "search": "^(.*), .*$","replace": "$1" } //<-- where we select the last name to group 1
                                                           //    and do a replacement
              ]
            }
          }
        },
        {
          "@type": "Type",
          "core": "STRING",
          "name": "firstName",
          "ext": {
            "migration": {
              "stages": [
                {
                  "@type": "jsonp",
                  "start": "LOCAL",
                  "steps": [
                    "$.value"
                  ]
                },
                { "@type": "regex", 
                  "search": "^.*, (.*)$","replace": "$1" } //<-- Select the first name as group 1 and replace
              ]
            }
          }
        }
      ]
    }
  ]
}
```

Now when we run the Migration plugin, we end up with a Java class called 
``xddl.v2_0.migration.Name`` with an ``apply()`` method we can call. Calling it
with the document...

```json
{
  "value": "Cooper, Robert"
}
```

... modifies the Json tree _in place_ to become...

```json
{
  "lastName" : "Cooper",
  "firstName" : "Robert"
}
```