
migrate
=======

Purpose
-------

This is a plugin that generates Java code that will use Jackson to migrate an object
instance of one version to an object instance of another version.

Usage
-----

You can generate classes with the plugin, but you need to be sure to include the
``net.kebernet.xddl:xddl-plugin-migrate-lib:[version]`` dependency in your project to use the generated classes.


Concepts
--------

### migration

The migration extension has the following properties:
1. op - ``REPLACE`` or ``MIXIN`` (default: REPLACE) whether the results of the stages should replace the current
   value, or be mixed into it. For arrays, if the result of the stages is an array, they will be concatenated, otherwise
   appended. For objects, the property values of the result of the stages will be set on the original value.
1. ``defaultMixinValue`` - if the original value of the property is "nullish" (missing or ``null``), then this will
   be the value the mixins are added to (this is usually like ``{}`` or ``[]``). If you do not provide this value
   and the current value is nullish, then the mixins will be ignored. If the mixin result value is nullish, it will be ignored.
1. ``stages`` -- an array of serial operations to be performed to synthesize a new value. They can be:   
    1. *jsonp* - has ``"steps":[]`` with Jayway JSON-Path queries starting from ``"start": "[ROOT|LOCAL|CURRENT]"``
    1. *regex* - has ``"search"`` and ``"replace"`` based the Java Regular expression replacement.
    1. *map* - has ``"values"`` where each is in the format ``{"from": any, "to": any}`` that maps from one literal json 
       value to another. Any value that doesn't match a ``from`` value will be passed through unmodified.
    1. *literal* - has ``"value": somevalue``
    1. *rename* - has ``"from": "aPropertyName", "to":"otherPropertyName"`` which renames a field on the CHILD properties 
      of the current working value.
    1. *case* - has ``"from": "[a format], "to":"[a format]"`` converts from one casing format to another where casing formats
       are one of:
       1. ``LOWER_WORDS`` "whitespace separated words" starting with all lowercase characters.
       1. ``UPPER_WORDS`` "Whitespace Separated Words" starting with uppercase characters.
       1. ``UPPER_CAMEL`` "CamelCaseWords" where each word starts with an uppercase.
       1. ``LOWER_CAMEL`` "camelCaseWords" where each word after the first starts with an uppercase.
       1. ``LOWER_SNAKE`` "snake_case_words" where each word is lowercase and separated by an underscore.
       1. ``UPPER_SNAKE`` "SNAKE_CASE_WORDS" where each word is uppercase and separated by an underscore. 
   1. *template* - has ``insertInto`` which is a graph that contains an empty object reference somewhere that where the 
      current value will be injected into the tree.
   1. *java* - has ``className`` which is a fully-qualified Java class name that implements 
      ``net.kebernet.xddl.migrate.JavaMigration``. The class needs to be available at plugin-execution time, 
      and needs to have a default no-args constructor. The ``migrate()`` method should return a new "current"
      value that will be passed to the subsequent stages. This is intended to be a catch-all for anything you
      simply cannot 


### Patch Files

xDDL supports expressing "patch operations" as part of a specification. These are meant
to express what you need to migrate a document/object from one version of the specification
to the next. A patch file is simply another xDDL document that includes directives that 
guide this process.

Migration operates on Jackson JsonNode objects, and performs steps in a particular order
that is important to understand:

1. Apply migrations on current objects
1. Apply migrations on child objects
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
                    "$.value"         //<-- we select the value peer property using a Json-Path expression
                  ]
                },
                { "@type": "regex",   //<-- The next state is a regular expression
                  "search": "^(.*), .*$","replace": "$1" } //<-- where we select the last name to group1
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
                {"@type": "jsonp", "start": "LOCAL", "steps": ["$.value"]},
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

And you end up with a class file that looks (roughly like):

```java
package xddl.v2_0.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.Override;
import java.util.Optional;
import net.kebernet.xddl.migrate.MigrationVisitor;

public class Name implements MigrationVisitor {
  public JsonNode migrate_lastName_0(ObjectNode root, ObjectNode local, JsonNode current) {
    Optional<JsonNode> result = Optional.ofNullable(local);
    result = result.map(n-> MigrationVisitor.evaluateJsonPath(n, "$.value"));
    return result.orElse(null);
  }

  public void migrate_lastName(ObjectNode root, ObjectNode local) {
    JsonNode current = local.has("lastName") ? local.get("lastName") : null;
    current = migrate_lastName_0(root, local, current);
    if(current != null) {
      current = MigrationVisitor.evaluateRegexReplace(current, "^(.*), .*$", "$1");
    }
    local.set("lastName", current);
  }

  public JsonNode migrate_firstName_0(ObjectNode root, ObjectNode local, JsonNode current) {
    Optional<JsonNode> result = Optional.ofNullable(local);
    result = result.map(n-> MigrationVisitor.evaluateJsonPath(n, "$.value"));
    return result.orElse(null);
  }

  public void migrate_firstName(ObjectNode root, ObjectNode local) {
    JsonNode current = local.has("firstName") ? local.get("firstName") : null;
    current = migrate_firstName_0(root, local, current);
    if(current != null) {
      current = MigrationVisitor.evaluateRegexReplace(current, "^.*, (.*)$", "$1");
    }
    local.set("firstName", current);
  }

  @Override
  public void apply(ObjectNode root, ObjectNode local) {
    migrate_lastName(root, local);
    migrate_firstName(root, local);
    if(local.has("value")) local.remove("value");
  }
}
```

Notice that we ``import net.kebernet.xddl.migrate.MigrationVisitor`` this means your resultant
xDDL project need to have dependency on ``net.kebernet.xddl:xddl-plugin-migrate-lib:[version]``

Hopefully it is obvious why the order of operations is important:

1. If you are referencing values from the previous specification in your "migration stages" and they
   are at your peer level or above, they should be in-tact, and in the original state when you read
   them.
2. Migrations down-tree are executed before value migrations at the top level.
3. Deletes, from leaf nodes in are executed.


FAQ
---

Q: Why does the ``jsonp`` stage have multiple steps, can't I use multiple stages?

A: Because if you do limited selection from an array, you always get an array. Mostly this
   gives you the ability to punch out of it. You can select 
    ``$.something.other.array[(foo =="bar")]``,
    ``$[0]``
   to get the first thing in an array where the foo property equals "bar" WITHOUT ending up 
   with a single element array, since the array is dereferenced in the second step.
   You could do this with multiple json path stages, but it would just be noisy.
   
Q: I want to expand a list from simple values to composite!

A: If your "contains" element is an object, but your original list value is a simple value 
   like a String or Number value, you can reference the original value as an underscore ``_``:
   
```json
{
  "@type": "List",
  "name": "list",
  "contains": {
    "@type": "Structure",
    "properties": [
      {
        "@type": "Type",
        "core": "STRING",
        "name": "originalValue",
        "ext": {
          "migration": {
            "stages": [
              {
                "@type": "jsonp",
                "start": "LOCAL", //<-- LOCAL inside a list structure means the list instance value.
                "steps": [
                  "$._" // <-- "Underscore" means the current list iterator value if the list
                        // doesn't contain a structure.
                ]
              }
            ]
          }
        }
      }
    ]
  }
}
```   

But let's look at our Name example, where we want to extract values:

