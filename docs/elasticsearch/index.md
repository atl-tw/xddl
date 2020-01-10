Using Glide and PowerGlide to Migrate ElasticSearch Data
========================================================

Moving data from one ElasticSearch mappings structure to another is a common problem. Here we are going to look 
at a way to do this with xDDL, the "migrate" plugin, and PowerGlide.


Before We Start
---------------

We are not going to discuss the details of xDDL specifications here. While we like to think the code should be relatively
self evident, if you would like to know more, please see the [writing a specification](../specification) documentation 
for details.


Primary Components
------------------

Working with ElasticSearch in a continuous fashion depends on understanding three primary components of the xDDL system:

 1. The ``migrate`` plugin, which reads migration instructions from your specification and patches, and generates 
    Java code that will migrate from one version of a specification to another.
 1. The ``Glide`` Gradle tasks, which steps over each of the patch versions and generates an authoritative xDDL file 
    for each interim version
 1. The ``PowerGlide`` Gradle tasks, which actually apply your changes to an ElasticSearch instance.
 
We will look at each of these in turn.

Creating Migration Patches
--------------------------

As you would have seen in the [writing a specification](../specification) document, you can use ``"@type":"PATCH_DELETE``
to remove a field from a subsequent version of an xDDL specification. However, you can use the migrate plugin to do much
more than just delete a file. 

Let's look at a simple example. Imagine we have a Structure called ``Name`` that represents the name of a person. We 
might start with...


```json
{
  "@type": "Structure",
  "name": "Name",
  "properties": [
    {"@type": "Type", "core": "STRING", "name": "id"},
    {"@type": "Type", "core": "TEXT", "name": "value"}
  ],
  "ext": {
    "java": {
      "implements": ["com.my.project.HasId"]
    }
  }
}
```
   
Here we are just keeping an ``id`` field, and a ``value`` field. But ``value`` is a bit nondescript. We might want to 
break names up into components.  Using the PATCH_DELETE we could create a ``patches/1.0.1/Name.patch.json`` file that removes value
and adds ``firstName`` and ``lastName``.

```json
{
  "@type": "Structure",
  "name": "Name",
  "properties": [
    {"@type": "PATCH_DELETE", "name": "value"},
    { "@type": "Type", "core": "STRING", "name": "firstName"},
    { "@type": "Type", "core": "STRING", "name": "lastName"}
  ]
}
```

This will allow us to generate a correct ElasticSearch mappings file for our new version, but we need to express HOW we
will change the existing data to match the new data. To do that, we need to put ``migration`` extensions in the patch
file.

```json
{
  "@type": "Structure",
  "name": "Name",
  "properties": [
    {"@type": "PATCH_DELETE", "name": "value"},
    {
      "@type": "Type", "core": "STRING", "name": "firstName",
      "ext": {
        "migration": {
          "stages": [
            {"@type": "jsonp", "start": "LOCAL", "steps": ["$.value"]},
            {"@type": "regex", "search": "^.*, (.*)$", "replace": "$1"}
          ]
        }
      }
    },
    {
      "@type": "Type", "core": "STRING", "name": "lastName",
      "ext": {
        "migration": {
          "stages": [
            {"@type": "jsonp", "start": "LOCAL", "steps": ["$.value"]},
            {"@type": "regex", "search": "^(.*), .*$", "replace": "$1"}
          ]
        }
      }
    }
  ]
}
```

Here we are expressing our ``migration`` in two ``stages``. The output from a preceding stage becomes the input for a 
subsequent stage, so you can kind of imagine this as a data transformation pipeline.

We start with a ``jsonp`` stage. This is a JSON Path expression (based on [Jayway](https://github.com/json-path/JsonPath))
that selects an initial value for the migration. It has a ``start`` property that is one of:

 * ``LOCAL`` the current object on which the property exists
 * ``ROOT`` the root object of the document
 * ``CURRENT`` the current value of the property itself
 
We begin with the ``$.value`` expression on ``LOCAL``, or the "value" property on our Name structure.

Once we have this, the next state is a ``regex`` stage, where we assume the values we want for ``firstName`` and 
``lastName`` are separated by a comma. We select Group 1 for which ever side of the comma we want and that will be the
value for our new field from an existing record.

> ``jsonp`` and ``regex`` are just two of a number of possible migrations stages you can use. For more information
> see the README on the xddl-plugin-migrate project.

Now that we have expressed our migration stages, we need to generate code that will actually perform the migration.

Using the Glide Gradle Plugins
------------------------------

The ``XDDLGlide`` task essentially craws a directory of ``patches`` folders, and creates interim versions. A common
build.gradle file might look like:

```groovy
sourceSets {
    main {
        java.srcDirs([
                file("${project.buildDir}/xddl-java"),
                file("src/main/java")
        ])
    }
}

dependencies {
    compile "net.kebernet.xddl:xddl-plugin-migrate-lib:+"
}

task glide(type: XDDLGlide){}

task migrationSources(type: XDDLGlideGenerate){
    plugin "migrate"
    outputDirectory file("${project.buildDir}/xddl-java")
}

javaSources.dependsOn glide
compileJava.dependsOn migrationSources
```

Here, we execute the ``glide`` task. This will step through the src/main/xddl/patches/* folders and create a file in 
``build/glide`` representing the Unified specification for each version.

Next we do ``migrationSources`` which iterates over each of those authoritative versions, and calls the ``migrate`` plugin
and creates source files in the ``xddl-java`` directory.

Inside there, you will end up with a ``MigrationVisitor`` for our ``Name`` type (this will be created in the "migration"
subpackage of your Java package, that looks like this:

```java
public class Name implements MigrationVisitor {
  public static final Name INSTANCE = new Name();

  public JsonNode migrate_firstName_0(ObjectNode root, JsonNode local, JsonNode current) {
    Optional<JsonNode> result = Optional.ofNullable(local);
    result = result.map(n-> MigrationVisitor.evaluateJsonPath(n, "$.value"));
    return result.orElse(null);
  }

  public void migrate_firstName(ObjectNode root, JsonNode local) {
    String fieldName = "firstName";
    JsonNode current = local.has("firstName") ? local.get("firstName") : null;
    current = migrate_firstName_0(root, local, current);
    current = MigrationVisitor.evaluateRegexReplace(current, "^.*, (.*)$", "$1");
    ((ObjectNode) local).set(fieldName, current);
  }

  public JsonNode migrate_lastName_0(ObjectNode root, JsonNode local, JsonNode current) {
    Optional<JsonNode> result = Optional.ofNullable(local);
    result = result.map(n-> MigrationVisitor.evaluateJsonPath(n, "$.value"));
    return result.orElse(null);
  }

  public void migrate_lastName(ObjectNode root, JsonNode local) {
    String fieldName = "lastName";
    JsonNode current = local.has("lastName") ? local.get("lastName") : null;
    current = migrate_lastName_0(root, local, current);
    current = MigrationVisitor.evaluateRegexReplace(current, "^(.*), .*$", "$1");
    ((ObjectNode) local).set(fieldName, current);
  }

  @Override
  public void apply(ObjectNode root, JsonNode local) {
    migrate_firstName(root, local);
    migrate_lastName(root, local);
    if(local.has("value")) ((ObjectNode)local).remove("value");
  }
}
```

The ``MigrationVisitor`` class is why we require a dependency on``net.kebernet.xddl:xddl-plugin-migrate-lib:+`` but this
plugin will generate a bunch of Java classes based on your package name that will migrate a node from the old version 
to the new version.

Creating Some Test Data
-----------------------



