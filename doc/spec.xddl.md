spec.xddl.json
==============

Purpose
--------

The intent of this file is to (eventually) define the "xDDL Specification" in the terms
of an xDDL specification. I would like the spec.xddl.son to result in a JSON schema that can 
the be used to validate an xddl specification file.
 
I would then like to REPLACE the net.kebernet.xddl.model package
in the xddl-runner project with the generated Java code from this file, or more to the point,
declare a dependency on the previously generated model code.
