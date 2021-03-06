/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- Register the error JAR file so that the included UDFs can be called in the script.
REGISTER ./error.jar;

-- Try to clean the result set if exists
fs -rmr *results

-- Use the  PigStorage function to load the dataset as an array of records.
-- Input: (id,name,year,rating,duration) 
movies = LOAD 'dataset-clean' USING PigStorage(',') AS (id,name,year,rating,duration);

-- Use the  FOREACH-GENERATE command to concatenate the name and the year
name_lastIndex = FOREACH movies GENERATE org.apache.pig.error.LAST_INDEX_OF(name,'a');

-- Use the  PigStorage function to store the results.
STORE name_lastIndex INTO 'results' USING PigStorage();
