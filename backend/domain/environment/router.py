from fastapi import APIRouter, Depends, HTTPException, Query, status
from httpx import HTTPError
from redis.asyncio import Redis

from domain.environment.schema import EnvironmentResponse
from domain.environment.service import fetch_environment
from providers.nominatim.geocoding_provider import NominatimGeocodingProvider
from providers.openmeteo.elevation_provider import OpenMeteoElevationProvider
from providers.openmeteo.weather_provider import OpenMeteoWeatherProvider
from providers.redis.config import get_redis
from providers.redis.environment_cache import RedisEnvironmentCache
from providers.soilgrids.soil_provider import SoilGridsSoilProvider


router = APIRouter(prefix="/environment", tags=["environment"])


@router.get("", response_model=EnvironmentResponse)
async def get_environment(
    lat: float = Query(..., ge=-90, le=90, description="Latitud"),
    lon: float = Query(..., ge=-180, le=180, description="Longitud"),
    redis: Redis = Depends(get_redis),
):
    """Devuelve elevación, suelo, clima y ubicación en una sola llamada.
    Caché two-tier: datos estáticos (elevation + soil) 90 días, datos dinámicos (weather + location) 15 min."""
    cache = RedisEnvironmentCache(redis)
    try:
        return await fetch_environment(
            elevation_provider=OpenMeteoElevationProvider(),
            weather_provider=OpenMeteoWeatherProvider(),
            soil_provider=SoilGridsSoilProvider(),
            geocoding_provider=NominatimGeocodingProvider(),
            cache=cache,
            lat=lat,
            lon=lon,
        )
    except HTTPError as e:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Environment provider error: {e}",
        )
