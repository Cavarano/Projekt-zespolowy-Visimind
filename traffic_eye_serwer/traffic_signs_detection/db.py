""""A module providing database access."""

import asyncio
import databases
import sqlalchemy
from sqlalchemy.exc import OperationalError, DatabaseError
from asyncpg.exceptions import CannotConnectNowError, ConnectionDoesNotExistError

from config import config


metadata = sqlalchemy.MetaData()

road_sign_table = sqlalchemy.Table(
    "road_signs",
    metadata,
    sqlalchemy.Column("id", sqlalchemy.Integer, primary_key=True),
    sqlalchemy.Column("name", sqlalchemy.String),
    sqlalchemy.Column("description", sqlalchemy.String),
    sqlalchemy.Column("photo_url", sqlalchemy.String),
)

db_uri = (
    f"postgresql+asyncpg://{config.DB_USER}:{config.DB_PASSWORD}"
    f"@{config.DB_HOST}:{config.DB_PORT}/{config.DB_NAME}?sslmode=require"
)

database = databases.Database(db_uri)

async def init_db(retries: int = 5, delay: int = 5) -> None:
    """Function to initialize DB connection with retry logic."""
    for attempt in range(retries):
        try:
            print(f"Attempting to connect to the database... (Attempt {attempt + 1})")
            await database.connect()
            await database.fetch_all(road_sign_table.select().limit(1))
            print("Connected successfully to the database.")
            return
        except (OperationalError, DatabaseError, CannotConnectNowError, ConnectionDoesNotExistError) as e:
            print(f"Attempt {attempt + 1} failed: {e}")
            await asyncio.sleep(delay)
        finally:
            if database.is_connected:
                await database.disconnect()
    raise ConnectionError("Could not connect to the database after several retries.")
