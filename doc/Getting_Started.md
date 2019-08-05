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
      "@type": "Type",
      "core": "BIG_DECIMAL",
      "name": "money",
      "description": "This is a currency value of an accurate floating point value",
      "ext": {
        "elasticsearch": {
          "type": "scaled_float",
          "scaling_factor": "100"
        }
      }
    }   
  ],
  "structures": [
    {
      "@type": "Structure",
      "name": "Book",
      "description": "A book for sale",
      "properties": [
        {
          "@type": "Type",
          "core": "STRING",
          "name": "title"
        },
        {
          "@type": "Reference",
          "ref": "money",
          "name": "price"
        } ,
        {
            "@type": "Reference",
            "ref": "money",
            "name": "tax"
          } 
      ]   
    }   
  ]
}
```

Here we have a definition for a Book specification. Lets work through it point by point.

*  ``title`` is the title of the specification.
*  ``version`` is the version of the spec.
*  ``description`` is a Text/Markdown description of the specification.
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
    