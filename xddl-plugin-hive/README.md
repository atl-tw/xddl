hive
====

Purpose
-------

This plugin will create a table definition for Apache Hive or Amazon Athena based on
the EntryRef type for you specification.

Extensions
----------

### Specification

 * ``table-name`` the table name to be created
 * ``location`` the location of the table data.,
 * ``partitioned-by`` a comma delimited list of "name[space]type" that will be used to 
    create the partitioned by block.
    
### Type level
 * ``type`` the datatype to use. eg: ``DECIMAL(10,2)`` or ``VARCHAR(50)``