import psycopg2
from typing import Dict
import sys

def make_db( conn_info: Dict[str, str]) -> None:
  psql_connection_string = f"user={conn_info['user']} password={conn_info['password']}"
  conn = psycopg2.connect(psql_connection_string)
  cur = conn.cursor()

  # "CREATE DATABASE" requires automatic commits
  conn.autocommit = True
  sql_query = f"CREATE DATABASE public"

  try:
    cur.execute(sql_query)
  except Exception as e:
    print(f"{type(e).__name__}: {e}")
    print(f"Query: {cur.query}")
    cur.close()
  else:
    # Revert autocommit settings
    conn.autocommit = False

if __name__ == "__main__":
    make_db()
