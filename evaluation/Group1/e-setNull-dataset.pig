-- Register the error JAR file so that the included UDFs can be called in the script.
REGISTER ./error.jar;

-- Try to clean the result set if exists
fs -rmr *results

-- Use the  PigStorage function to load the dataset as an array of records.
-- Input: (id,name,year,rating,duration) 
A = LOAD 'dataset-clean' USING PigStorage(',') AS (a_id,a_name,a_year,a_rating,a_duration);  

-- Set the name of A to be null
A_null = FOREACH A GENERATE a_id,org.apache.pig.error.SetCharArrayNull_SEC(a_name) as a_name,a_year,a_rating,a_duration;

-- Use the  PigStorage function to load the dataset as an array of records.
-- Input: (id,name,year,rating,duration) 
B = LOAD 'dataset' USING PigStorage(',') AS (b_id,b_name,b_year,b_rating,b_duration); 

-- Join two datasets by id
C_null = JOIN A_null BY a_id, B BY b_id;

-- CheckNameYear fails if both a_name and b_year are null
C_check = FOREACH C_null GENERATE a_id, org.apache.pig.error.CheckNameYear((chararray)a_name,(int)b_year);

-- Use the  PigStorage function to store the results.
STORE C_check INTO 'results' USING PigStorage();

