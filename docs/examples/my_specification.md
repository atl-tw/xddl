My Specification
===========================================================
_Version: 1.0_

This is my specification, there are many like it but this one is mine.


Contents
--------

1. Structures
   1. [Book](#Book)
1. Types
   1. [money](#money)


Structures
----------

<a name="Book"></a>
### Book
A book for sale

#### Properties

  * title (STRING)
  * authors (List of...)
    * null (STRING)
  * price ([money](#money))
  * tax ([money](#money))


Types
-----

<a name="money"></a>
#### money (BIG_DECIMAL)
  * This is a currency value of an accurate floating point value
  * extensions
    * elasticsearch
      * ``type: "scaled_float"``
      * ``scaling_factor: "100"``

