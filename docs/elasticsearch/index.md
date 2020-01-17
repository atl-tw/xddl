Using Glide and PowerGlide to Migrate ElasticSearch Data
========================================================

Moving data from one ElasticSearch mappings structure to another is a common problem. Here we are going to look 
at a way to do this with xDDL, the "migrate" plugin, and PowerGlide.


Before We Start
---------------

We are not going to discuss the details of xDDL specifications here. While we like to think the code should be relatively
self evident, if you would like to know more, please see the [writing a specification](../specification) documentation 
for details.


Table of Contents
-----------------

 1. [Strategy](#strat)
 1. [Primary Components](#comp)
 1. [Creating Migration Patches](#patch)
    1. [See also the Migrate Plugin](https://github.com/atl-tw/xddl/tree/master/xddl-plugin-migrate)
 1. [Using the Glide Gradle Plugins](#gradle)
 1. [Setting Up a Project](#setup)
 1. [Migrating to a New Version](#migrate)
 1. [Why Code Generation? (or How to I build my own migration tool?)](#codegen)


<a name="strat"></a>

Strategy
--------

Unlike SQL databases, it is not possible to alter an ElasticSearch index once it has been created, and updating the 
structure of individual records cannot be done with the flexibility of SQL once created. To deal with this, xDDL's
ElasticSearch works with index aliases. The idea being that there are indexes based on versions, and then an alias
that is updated as the data is migrated. The flow goes something like this:

Initial State:
 * Alias "My_Data" points to "My_Data_1.0".
Steps:
 * Create "My_Data_2.0"
 * Extract records from "My_Data_1.0" and transform them into records usable with 2.0
 * Insert the records into "My_Data_2.0"
 * Re-point the "My_Data" alias to "My_Data_2.0"
 
Doing this by hand-coding your migration every time you have a new version is an imposing chore and a lot of set up,
so the Glide and PowerGlide Gradle plugins look to make this easier by letting you express your record changes as an
xDDL extension, then it will handle plumbing for you.

<a name="comp"></a>

Primary Components
------------------

Working with ElasticSearch in a continuous fashion depends on understanding three primary components of the xDDL system:

 1. The ``migrate`` plugin, which reads migration instructions from your specification and patches, and generates 
    Java code that will migrate from one version of a specification to another.
 1. The ``Glide`` Gradle tasks, which steps over each of the patch versions and generates an authoritative xDDL file 
    for each interim version
 1. The ``PowerGlide`` Gradle tasks, which actually apply your changes to an ElasticSearch instance.
 
We will look at each of these in turn.

<a name="patch"></a>

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
  ]
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

**You can learn more about
[the migrate plugin](https://github.com/atl-tw/xddl/tree/master/xddl-plugin-migrate) from the README.**

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

<a name="gradle"></a>

Using the Glide Gradle Plugins
------------------------------

The ``XDDLGlide`` task essentially crawls a directory of ``patches`` folders, and creates interim versions. A common
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

task migrationSources(type: XDDLGlideGenerate, dependsOn: glide){
    plugin "migrate"
    outputDirectory file("${project.buildDir}/xddl-java")
}

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

<a name="setup"></a>

Setting Up a Project
--------------------

Let's work a complete example now. We can start with our 1.0 version of the specification, creating a gradle project
with the following structure:

 * src/
   * main/
     * xddl/
       * Specification.xddl.json
       * includes/
         * Name.xddl.json
   * elasticsearch/
     * load/
        * a.json
        * b.json
        * ...
        
And our build script as follows:

```groovy
plugins {
    id 'net.kebernet.xddl' version '+'
}
repositories {
    jcenter() // the plugins dependencies are in jcenter
}

task glide(type: XDDLGlide){}

task elasticsearch(type: XDDLGlideGenerate, dependsOn: glide){
    plugin "elasticsearch"
}

task createIndex(type: XDDLElasticSearchIndex, dependsOn: elasticsearch){
    activeAlias "test_index"
    elasticSearchUrl URI.create("http://localhost:9200")
}

task loadData(type: XDDLElasticSearchLoad, dependsOn: createIndex){
    activeAlias "test_index"
    elasticSearchUrl URI.create("http://localhost:9200")
}
```

So we have four tasks defined here.

1. ``glide`` -- which will unify our xDDL specification by version
1. ``elasticsearch`` -- which will create out index definitions from mappings
1. ``createIndex`` -- which will find the version of our index that needs to be created, will create it, then make it 
   the active alias. So we end up with the alias "test_index" pointing to "test_index_1.0".
1. ``loadData`` -- which will go through the json documents in ``elasticsearch/load`` and put them into the index. 
    These are very simple Name types like: ``{ "id": "a", "value": "Cooper, Robert"}``

Now if we run ``gradle loadData`` we see:

```
> Task :elasticsearch
Generating elasticsearch for .../build/glide/baseline.xddl.json

> Task :createIndex
There isn't a currently active version.
Versions not deployed [1.0]
Creating index for 1.0
Reading schema from .../build/xddl/xddl_1.0.mappings.json
Since there is no active alias, setting it to 1.0

> Task :loadData
Inserted 4 records to test_index
```

Now we have our ES instance configured and containing data.

<a name="migrate"></a>

Migrating to a New Version
--------------------------

Now let's apply our changes as we described above. We start by creating a folder called ``1.0.1`` in 
``src/main/xddl/patches``, and putting the Name.patch.json file we described above in it. As you already know, the 
migration is handled by generating Java classes that are used to adapt from one version to the next, so we need to 
make our ``build.gradle`` a Java-is project...

```groovy
// ... stuff omitted
apply plugin: 'java'

sourceSets {
    main {
        java.srcDirs([
                file("${project.buildDir}/xddl-java"),
                file("src/main/java")
        ])
    }
}

dependencies {
    // We need this dependency to get the migration library, so our MigrationVisitors will actually compile.
    compile "net.kebernet.xddl:xddl-plugin-migrate-lib:+"
}

task glide(type: XDDLGlide){}

task migrationSources(type: XDDLGlideGenerate, dependsOn: glide){
    plugin "migrate"
    outputDirectory file("${project.buildDir}/xddl-java")
}
// ... stuff omitted

task migrate(type: XDDLPowerGlide, dependsOn: [createIndex, compileJava]){
    activeAlias "test_index"
    elasticSearchUrl URI.create("http://localhost:9200")
}



compileJava.dependsOn migrationSources

```

So first we need to add the directory where we are going to generate the sources (``xddl-java``)) to our project's ``main``
SourceSet. Then we add another ``XDDLGlideGenerate`` task, this time calling the "migrate" plugin, and outputting to our
generated sources directory. Finally, we add an ``XDDLPowerGlide`` task called "migrate" that will look for the latest
index and migrate our data into it.

Running ``gradle migrate`` we see

```
> Task :glide

> Task :migrationSources
Generating migrate for .../build/glide/baseline.xddl.json
Generating migrate for .../build/glide/1_0_1.xddl.json

> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes
> Task :jar
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :check UP-TO-DATE
> Task :build

> Task :elasticsearch
Generating elasticsearch for .../build/glide/baseline.xddl.json
Generating elasticsearch for .../build/glide/1_0_1.xddl.json

> Task :createIndex
Current active index: 1.0
Higher index versions already created: []
Higher index versions not already created:[1.0.1]
Creating index for 1.0.1
Reading schema from .../build/xddl/xddl_1.0.1.mappings.json

> Task :migrate
Completed migration run:
	MigrationState(scrollId=null, successfulRecords=4, failedRecords=0, exceptions=Histogram: {
}, visitorClassName=com.my.project.model.v1_0_1.migration.Name, itemName=xddl_1.0.1, currentIndex=test_index_1.0, nextIndex=test_index_1.0.1, batchSize=0, switchActiveOnCompletion=true, activeAlias=test_index)

BUILD SUCCESSFUL in 2s
```

So we create the index for 1.0.1, and we run ``migrate``. You can see we migrated 4 records successfully, and once the
migration finished, it moved the ``test_index`` alias to our new version.

So now if we run:

```text
GET /test_index/_search
{
    "query": {
        "match_all": {}
    }
}
```
We get:
```json
{
  "took" : 3,
  "timed_out" : false,
  "_shards" : {},
  "hits" : {
    "total" : 4,
    "max_score" : 1.0,
    "hits" : [
      {
        "_index" : "test_index_1.0.1",
        "_type" : "xddl_1.0.1",
        "_id" : "c",
        "_score" : 1.0,
        "_source" : {
          "id" : "c",
          "firstName" : "Connor",
          "lastName" : "Wylie"
        }
      },
```

...and so on.

You can find these completed project examples [here](https://github.com/atl-tw/xddl/tree/master/gradle-plugin/src/integration/projects)


<a name="codegen"></a>

Why Code Generation? (or How to I build my own migration tool?)
---------------------------------------------------------------

"This seems like a fairly significant bit of set up", you might be saying to your self, "to get my ``migrate`` task 
working in Gradle." This is a fair question, but we wanted the PowerGlide code to be as "remixable" (that's what the kids
are calling or, or were calling it 10 years ago) as possible. Performing your data migration from gradle might not
be the best solution. You might want to do it from AWS Lambda or as part of your actual application code. 

You can package your project as a Jar with all the migration classes in tact and call the PowerGlide runner directly:

```java
    PowerGlideRunner runner = new PowerGlideRunner(
        new ElasticSearchClient(
            new RestHighLevelClient(
                RestClient.builder(
                    new HttpHost("localhost", 9200)
                ) 
            ),
            new ObjectMapper()
        ),
        MigrationState.builder()
          .nextIndex("my_index_1.0.1")
          .currentIndex("my_index_1.0")
          .itemName("my_item")
          .visitorClassName("com.my.project.model.v1_0_1.migration.MyItem")
          .batchSize(1000)
          .build()
    );
    
    MigrationState nextState = runner.runSingleBatch();
```

Note that you will probably want to wire these up with your DI framework of choice, but you can see the outline here...

We create the ``PowerGlideRunner`` with an ``ElasticSearchClient`` instance (which needs a configured ``ObjectMapper`` 
instance) and an initial MigrationState. We can then call ``runSingleBatch()`` to migrate a batch of records to 
the new index. The ``MigrationState`` that is returned can then be serialized to JSON and passed to a next invocation,
or you can simply call ``runSingleBatch()`` again, or even ``run()`` to migrate all the records in a continuous loop.

