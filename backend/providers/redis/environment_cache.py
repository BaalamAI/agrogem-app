from typing import Awaitable, cast

from redis.asyncio import Redis

from domain.environment.schema import EnvironmentDynamic, EnvironmentStatic


STATIC_TTL_SECONDS = 90 * 24 * 60 * 60
DYNAMIC_TTL_SECONDS = 15 * 60

STATIC_PREFIX = "env:static:"
DYNAMIC_PREFIX = "env:dynamic:"


def _static_key(lat: float, lon: float) -> str:
    return f"{STATIC_PREFIX}{lat:.2f}:{lon:.2f}"


def _dynamic_key(lat: float, lon: float) -> str:
    return f"{DYNAMIC_PREFIX}{lat:.2f}:{lon:.2f}"


class RedisEnvironmentCache:
    def __init__(self, redis: Redis):
        self._redis = redis

    async def get_static(self, lat: float, lon: float) -> EnvironmentStatic | None:
        raw = await cast(Awaitable[str | None], self._redis.get(_static_key(lat, lon)))
        return EnvironmentStatic.model_validate_json(raw) if raw else None

    async def set_static(self, lat: float, lon: float, data: EnvironmentStatic) -> None:
        await cast(
            Awaitable[bool],
            self._redis.set(_static_key(lat, lon), data.model_dump_json(), ex=STATIC_TTL_SECONDS),
        )

    async def get_dynamic(self, lat: float, lon: float) -> EnvironmentDynamic | None:
        raw = await cast(Awaitable[str | None], self._redis.get(_dynamic_key(lat, lon)))
        return EnvironmentDynamic.model_validate_json(raw) if raw else None

    async def set_dynamic(self, lat: float, lon: float, data: EnvironmentDynamic) -> None:
        await cast(
            Awaitable[bool],
            self._redis.set(_dynamic_key(lat, lon), data.model_dump_json(), ex=DYNAMIC_TTL_SECONDS),
        )
