/*
 * Copyright 2019, 2020 Robert Cooper, ThoughtWorks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kebernet.xddl.model;

/** An enum of the core data types. */
public enum CoreType {
  /** A short absolute value string, assume by default 255 or fewer characters */
  STRING,
  /** A long string value representing arbitrary text, assumed unbounded. */
  TEXT,
  /** A day month year value without a timezone. */
  DATE,
  /** A time of day */
  TIME,
  /** A full timestamp */
  DATETIME,
  /** A 32 bit integer value */
  INTEGER,
  /** A 64 bit integer value */
  LONG,
  /** A true false value */
  BOOLEAN,
  /** A 32 bit floating point. */
  FLOAT,
  /** A 64 bit floating point. */
  DOUBLE,
  /** And unbounded integer */
  BIG_INTEGER,
  /** and unbounded floating point value */
  BIG_DECIMAL,
  /** A binary value (byte array/blob/etc) */
  BINARY
}
