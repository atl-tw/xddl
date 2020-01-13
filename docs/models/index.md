Generating Model Artifacts and Sharing Between Projects
=======================================================


Before We Start
---------------

We are not going to discuss the details of xDDL specifications here. While we like to think the code should be relatively
self evident, if you would like to know more, please see the [writing a specification](../specification) documentation 
for details.


Table of Contents
-----------------

 1. [Generation and Plugins](#gen)
 1. [Project Setup](#setup)
 1. [Java Code Generation](#java)
    a. [Complete List of Java Extension Properties](#alljava)
    a. [Enums and the Java Plugin](#javaenums)
 1. [Swift Code Generation](#swift)
    a.  [Complete List of Swift Extension Properties](#allswift)


<a name="gen"></a>

Generation and Plugins
----------------------

The primary way the xDDL utilities are used with with "generate" plugins. These generate new artifacts from your xDDL
specification file. 

The core plugins are:
 * [``markdown``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-markdown/README.md): To generate ``.md`` and ``.html`` documentation.
 * [``graphvis``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-graphvis/README.md): To generate ``.dot`` and ``.png`` graphs that show the relationships 
    between your structure types.
 * [``plantuml``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-plantuml/README.md): To generate ``.puml`` diagram that show the relationships 
    between your structure types. 
 * [``json``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-json-schema/README.md): To generate a JSON-Schema file.
 * [``elasticsearch``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-elasticsearch/README.md): To generate an Elasticsearch Mappings structure for indexing your 
   document structure.
 * [``java``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-java/README.md): To generate a set of Java classes suitable for mapping to your specification.
 * [``swift``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-swift/README.md): To generate a SwiftPM library suitable for mapping your specification.
 * [``hive``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-hive/README.md): Generates table specs for Apache Hive/Amazon Athena
 * [``migrate``](https://github.com/atl-tw/xddl/tree/master//xddl-plugin-migrate/README.md): Generates a class to migrate data from one version of the spec to another.
 
 
 But here we are going to focus on the two that generate source code for you: Java and Swift.
 
 
<a name="setup"></a>

Project Setup
-------------
 
 
 Basic code generation can be done on any xDDL model, and will generate a set of defaults based on the core types, but 
 you can highly customize the sources that are generated. First, though, let's set up a Gradle project:
 
``build.grade`` 
 ```groovy
plugins {
    id "net.kebernet.xddl" version "+"
}

apply plugin: 'java'

repositories { jcenter() }

sourceSets {
    main {
        java.srcDirs([
                file("${project.buildDir}/xddl-java"),
                file("src/main/java")
        ])
    }
}

task glide(type: XDDLGlide){}

task glideJava(type: XDDLGlideGenerate, dependsOn: glide){
    plugin "java"
    outputDirectory file("${project.buildDir}/xddl-java")
}

compileJava.dependsOn glideJava
```

This will give us a basic Java project where the files in default project layout...

 * src/
   * main/
     * xddl/
       * Specification.xddl.json (the primary specification file)
       * includes/ (a folder for included types and structures)
       
... will be generated into the ./build/xddl-java directory, and packaged into the JAR artifact for the project. You can
learn more about the Glide plugin in the [documentation for ElasticSearch](../elasticsearch).

If we create a empty ``Specification.xddl.json`` file (containing simply ``{}``), we have a start. We can then create
``xddl/includes/Item.xddl.json`` as below:

```json
{
  "@type": "Structure",
  "name": "Item",
  "properties": [
    {"@type": "Type", "core": "INTEGER", "name": "count"}
  ]
}
```

and we do ``gradle build``, we will see the following file generated into the ``./build/xddl``.

```java
package xddl;

public class Item {
  private Integer count;

  /**
   *
   * @return the value
   */
  public Integer getCount() {
    return this.count;
  }

  /**
   *
   * @param value the value
   */
  public void setCount(final Integer value) {
    this.count = value;
  }

  /**
   * @param value the value 
   * @return this
   */
  public Item count(final Integer value) {
    this.count = value;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if(!(o instanceof Item)) return false;
    Item that = (Item) o;
    return 
           java.util.Objects.equals(this.count,that.count) && 
              true;
  }

  @Override
  public int hashCode() {
    return   java.util.Objects.hash(
           this.count,
            0);
  }
}
```

<a name="java"></a>

Java Code Generation
--------------------

While in the previous section we generated Java sources, the project setup was generally useful for any kind of source
generation. Now we will look in detail at the Java plugin. You can see above our "INTEGER" property was created as a
``java.lang.Integer`` type on the generated Java code. Here is a list of the default mappings for each of the core
xDDL types:


 * ``STRING``: ``java.lang.String``
 * ``TEXT``: ``java.lang.String``
 * ``DATE``: ``java.time.LocalDate``
 * ``TIME``: ``java.time.OffsetTime``
 * ``DATETIME``: ``java.time.OffsetDateTime``
 * ``INTEGER``: ``int`` if the field is required, ``java.lang.Integer`` otherwise.
 * ``LONG``: ``long`` if the field is required, ``java.lang.Long`` otherwise.
 * ``BOOLEAN``: ``boolean`` if the field is required, ``java.lang.Boolean`` otherwise.
 * ``FLOAT``: ``float`` if the field is required, ``java.lang.Float`` otherwise.
 * ``DOUBLE``: ``double`` if the field is required, ``java.lang.Double`` otherwise.
 * ``BIG_INTEGER``: ``java.math.BigInteger``
 * ``BIG_DECIMAL``: ``java.math.BigDecimal``
 * ``BINARY``: ``byte[]``
 
 You can, override any of these defaults, however, with the Java extension:
 
 ```json
 {
   "@type": "Structure",
   "name": "Item",
   "properties": [
     {
        "@type": "Type", "core": "DATETIME", "name": "theDate",
        "ext": {
          "java": {
            "type": "java.util.Date"          
          }       
        }     
      
     }
   ]
 }
```

You can also use the Java extension on the type to import types to use. For example 


 ```json
 {
   "@type": "Structure",
   "name": "Item",
   "ext": {
      "java": {
        "imports": ["javax.persistence.Entity", "javax.persistence.Temporal", "javax.persistence.TemporalType"],
        "annotations": "@Entitiy"
      }   
    },
   "properties": [
     {
        "@type": "Type", "core": "DATETIME", "name": "theDate",
        "ext": {
          "java": {
            "type": "java.util.Date",      
            "annotations": "@Temporal(TemporalType.TIMESTAMP)"    
          }       
        }     
      
     }
   ]
 }
```
 
Resulting in:

```java
package xddl;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class Item {
  @Temporal(TemporalType.TIMESTAMP)
  private Date theDate;

  /**
   *
   * @return the value
   */
  public Date getTheDate() {
    return this.theDate;
  }
//...
 ```


<a name="alljava"></a>

### Complete List of Java Extension Properties
 
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
     * ``imports``: An array of fully qualified class names to "import" to use as simple names.
     * ``annotations``: A String containing annotations to put on the class. Can reference things
       from imports as simple names.
  * Property:
     * ``type``: A fully qualified Java type for the property. eg, use ``java.util.Date`` for 
       the ``DATETIME``. For List types, they should be a non-parameterized collection name.
     * ``equalsHashCodeWrapper``: (List properties only) fully qualified class name for a Collection
         type to wrap lists in for doing equality. This will default to ``java.util.ArrayList`` -- that is, it will
         copy all list types into an array list to ensure deep equals works irrespective of the j.u.List
         implementation on the class. This can also be ``none`` to disable this entirely and attempt to
         use the ``equals`` method on the collection directly.
     * ``initializer``:  Contains a Java statement that will be the initializer for the field
         (ex ``"intitializer": "new java.util.ArrayList<>()"`` for an empty list or ``"intitializer": "\"foo\"""``
         to initialize a default String value to ``foo``.
     * ``imports``: An array of fully qualified class names to "import" to use as simple names.
     * ``annotations``: A String containing annotations to put on the field. Can reference things
         from imports on either the property or the structure as simple names.
         
<a name="javaenums"></a>
         
### Enums and the Java Plugin
 
If you have "allowables" defined on a type, it will be treated as an ``enum``, and only STRING types will be treated as
enums. If any of the values are not usable as Java-ish names, they will be prefixed with ``val_`` and Jackson 
``@JsonCreator`` method. So for instance:

```json
{
  "@type": "Type",
  "core": "STRING",
  "name": "ordinal_enum",
  "description": "A referenced enum property value",
  "allowable": [
    {"value": "1ST", "description": "The 1st value"},
    {"value": "SECOND", "description": "THe 2nd value"},
    {"value": "THIRD", "comment": "Maybe add a 3rd value?"}
  ]
}
``` 

Results in
```java
 /**
 * A referenced enum property value
 */
public enum OrdinalEnum implements Serializable {
  /**
   * The 1st value
   */
  VAL_1ST("1st"),

  /**
   * THe 2nd value
   */
  SECOND("SECOND"),

  /**
   * Comment: Maybe add a 3rd value?
   */
  THIRD("THIRD");

  private final String value;

  OrdinalEnum(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return value;
  }

  /**
   * @return enum value
   * @param value string value */
  @JsonCreator
  public static OrdinalEnum forValue(String value) {
    for (OrdinalEnum check : values()) {
      if(check.value.equals(value)) {
        return check;
      }
    }
    throw new IllegalArgumentException(value);
  }
}
```

<a name="swift"></a>

Swift Code Generation
---------------------

Swift Code generation works largely like the Java generation, except we don't need to include the Swift code in our
project (unless we want to). The Swift Plugin outputs an entire directory suitable for publishing as a SwiftPM module.
The code generated by the Swift plugin is for use with Apple's "Codable" API.

We first need to add a new task to our Gradle project:

```groovy
task glideSwift(type: XDDLGlideGenerate, dependsOn: glide){
    plugin "swift"
    outputDirectory file("${project.buildDir}/swift")
}
```

This results in:

``Pacakge.swift``
```swift
// swift-tools-version:5.1
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
  name: "xddl",
  products: [
    .library(
      name: "xddl",
      targets: ["xddl"]
    )
  ],
  dependencies: [
  ],
  targets:[
    .target(
      name: "xddl",
      dependencies:[]),
  ]
)
```

and ``Sources\xddl\Item.swift``
```swift
import Foundation

public struct Item: Codable {
  var count: Int?
}
```

Like the Java plugin, if you have named versions of your specification generated by the Glide plugin, you will get 
multiple targets in your Package.swift file, each named like "library_vX_X".

<a name="allswift"></a>

### Complete List of Swift Plugin Extensions

 * Specification
   * ``libraryName`` -- the name of the Swift Library (default: Specification Title as UpperCamelCase)
   * ``dependencies`` -- an array of possible dependencies for the library
    * ``url`` -- the url to the git repository
    * ``from`` -- the lowest acceptable version
    * ``to`` -- the highest acceptable version
    * ``exclusive`` -- if the highest acceptable version should be excluded ("..<")
    * Understand that if there are multiple versions of a spec being built into a library, the dependencies declared
      in the highest version, win.
 * Type (any type)
   * ``imports`` -- a string list of imports to support the type.
 * Property
   * ``type`` -- the native type of the field (must be Codable)
   * ``fieldName`` -- the encoded (JSON) name of the field.