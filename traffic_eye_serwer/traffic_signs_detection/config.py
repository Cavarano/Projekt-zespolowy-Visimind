"""A module providing configuration variables."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class BaseConfig(BaseSettings):
    """A class containing base settings configuration."""
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


class AppConfig(BaseConfig):
    DB_HOST: str
    DB_PORT: int
    DB_NAME: str
    DB_USER: str
    DB_PASSWORD: str

    class Config:
        env_file = ".env"


config = AppConfig()

