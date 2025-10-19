import os

import mariadb


def create_db_connection() -> mariadb.Connection:
    return mariadb.connect(host="database", user='root', password='root', database='blogs_database')

def release_db_connection( connection: mariadb.Connection ):
    connection.close()

def init_db( connection: mariadb.Connection ):
    create_tables( connection )

def create_tables( connection: mariadb.Connection ):
    scripts_run_seq = [
        "create_users_table.sql" ,
        "create_blogs_table.sql"
    ]
    for script_filename in scripts_run_seq:
        create_tables_script = read_sql_script( script_filename )
        connection.cursor().execute( create_tables_script )
        connection.commit()
        print( f"Executed { script_filename }..." )

def generate_uid() -> str:
    return os.urandom( 10 ).hex()

def read_sql_script( script_file_name ) -> str:
    return open(os.path.join( "database/sql_scripts" , script_file_name) , "r" ).read()