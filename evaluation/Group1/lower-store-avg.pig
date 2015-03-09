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

-- Use the  PigStorage function to load the dataset as an array of records.
-- Input: (id,name,year,rating,duration) 
movies = LOAD 'dataset' USING PigStorage(',') AS (id,name,year,rating,duration);

-- Use the  FOREACH-GENERATE command to concatenate the name and the year
name_lower = FOREACH movies GENERATE LOWER(name) as lower_name, rating;

-- Use AVG
name_group = GROUP name_lower BY lower_name;
name_avg = FOREACH name_group GENERATE name_lower.lower_name, AVG(name_lower.rating);

-- Use the  PigStorage function to store the results.
STORE name_avg INTO 'lower-store-avg-results' USING PigStorage();
