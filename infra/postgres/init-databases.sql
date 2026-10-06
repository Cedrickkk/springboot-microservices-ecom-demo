SELECT format('CREATE DATABASE %I', database_name)
FROM (VALUES ('product'), ('order'), ('payment')) AS required(database_name)
WHERE NOT EXISTS (SELECT 1
                  FROM pg_database
                  WHERE datname = required.database_name)
\gexec
