-- Register the error JAR file so that the included UDFs can be called in the script.
REGISTER ./error.jar;

-- Try to clean the result set if exists
fs -rmr *results

-- Use the  PigStorage function to load the dataset as an array of records.
-- Input: (id,name,year,rating,duration) 
A = LOAD 'dataset-clean' USING PigStorage(',') AS (a_id,a_name,a_year,a_rating,a_duration);  

-- Use the  PigStorage function to load the dataset as an array of records.
-- Input: (id,name,year,rating,duration) 
B = LOAD 'dataset-clean' USING PigStorage(',') AS (b_id,b_name,b_year,b_rating,b_duration); 

-- Set the year of B to be null
B_null = FOREACH B GENERATE b_id,b_name,org.apache.pig.error.SetIntNull(b_year) as b_year,b_rating,b_duration;

-- Join two datasets by id
C_null = JOIN A BY a_id, B_null BY b_id;

-- CheckNameYear fails if both a_name and b_year are null
C_check = FOREACH C_null GENERATE a_id, org.apache.pig.error.CheckNameYear((chararray)a_name,(int)b_year);

-- Use the  PigStorage function to store the results.
STORE C_check INTO 'results' USING PigStorage();

