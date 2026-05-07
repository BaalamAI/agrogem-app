import asyncio

from domain.elevation.provider import ElevationProvider
from domain.environment.schema import EnvironmentDynamic, EnvironmentResponse, EnvironmentStatic
from domain.geocoding.provider import GeocodingProvider
from domain.soil.provider import SoilProvider
from domain.weather.provider import WeatherProvider
from providers.redis.environment_cache import RedisEnvironmentCache


async def fetch_environment(
    elevation_provider: ElevationProvider,
    weather_provider: WeatherProvider,
    soil_provider: SoilProvider,
    geocoding_provider: GeocodingProvider,
    cache: RedisEnvironmentCache,
    lat: float,
    lon: float,
) -> EnvironmentResponse:
    static_data, dynamic_data = await asyncio.gather(
        cache.get_static(lat, lon),
        cache.get_dynamic(lat, lon),
    )

    if static_data is None:
        elevation, soil = await asyncio.gather(
            elevation_provider.get(lat, lon),
            soil_provider.get_profile(lat, lon),
        )
        static_data = EnvironmentStatic(elevation=elevation, soil=soil)
        await cache.set_static(lat, lon, static_data)

    if dynamic_data is None:
        weather, location = await asyncio.gather(
            weather_provider.get_forecast(lat, lon),
            geocoding_provider.reverse(lat, lon),
        )
        dynamic_data = EnvironmentDynamic(weather=weather, location=location)
        await cache.set_dynamic(lat, lon, dynamic_data)

    return EnvironmentResponse(
        lat=lat,
        lon=lon,
        elevation=static_data.elevation,
        soil=static_data.soil,
        weather=dynamic_data.weather,
        location=dynamic_data.location,
    )
