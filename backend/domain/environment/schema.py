from pydantic import BaseModel

from domain.elevation.schema import ElevationResponse
from domain.geocoding.schema import ReverseGeocodeResult
from domain.soil.schema import SoilResponse
from domain.weather.schema import WeatherResponse


class EnvironmentStatic(BaseModel):
    elevation: ElevationResponse | None = None
    soil: SoilResponse | None = None


class EnvironmentDynamic(BaseModel):
    weather: WeatherResponse | None = None
    location: ReverseGeocodeResult | None = None


class EnvironmentResponse(BaseModel):
    lat: float
    lon: float
    elevation: ElevationResponse | None = None
    soil: SoilResponse | None = None
    weather: WeatherResponse | None = None
    location: ReverseGeocodeResult | None = None
