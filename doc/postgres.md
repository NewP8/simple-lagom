# Postgres

CREATE USER paolo WITH PASSWORD 'paolo';

CREATE DATABASE prova

GRANT ALL PRIVILEGES ON DATABASE prova to paolo;

## psql

psql -l  mostra tutti i db

psql -d prova  si collega a db prova

\q esce
\d lista tutte le tabelle

select * from nome_tabella; mostra righe

### Altri comandi

http://postgresguide.com/utilities/psql.html

## Backup 

pg_dump  utility per backup (vari formati)

pg_dump database_name_here > database.sql
pg_dump -Fc database_name_here > database.bak # compressed binary format
pg_dump -Ft database_name_here > database.tar # tarball

pg_restore -Fc -C database.bak # restore compressed binary format
pg_restore -Ft -C database.tar # restore tarball

copy per copiare dati di tabelle

## Indici

possibilità di creare indici per accesso veloce a dato

## Explain Execution plan

## HStore e Jsonb

oer chiave valore tipo json con funzionalità di ricerca. Per json più specifico Jsonb


