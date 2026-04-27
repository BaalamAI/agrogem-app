# Agrogem Backend API Guide for Kotlin / Android

> Documento pensado para dos públicos: 
> 1. la app Android en Kotlin
> 2. un agente que necesite entender el backend completo y consumirlo sin adivinar nada

---

## 1. Resumen ejecutivo

- Backend: **FastAPI async**
- Persistencia: **MongoDB**
- Cache y sesiones: **Redis**
- Storage de imágenes: **Google Cloud Storage**
- Embeddings para plagas: **Gemini + Atlas Vector Search**
- Auth general: **NO hay JWT ni Bearer auth**
- Auth real disponible: **registro/login por teléfono + password**
- Chat: usa **`session_id`** guardado en Redis con TTL de **24 horas**

### Convenciones globales

- Fechas: ISO 8601 en UTC
- IDs: UUID string
- Errores: formato FastAPI estándar
  ```json
  { "detail": "mensaje" }
  ```
- Muchos campos opcionales pueden venir en `null`
- El backend suele ignorar campos extra en el body

### Base URL local

```text
http://127.0.0.1:8000
```

### Docs automáticas FastAPI

```text
/docs
```

---

## 2. Autenticación y sesiones

### Lo importante

- **No existe autenticación global** para weather, soil, geocode, climate, GBIF, elevation, disease-risk, pest-risk ni pest endpoints.
- El único flujo de identidad real es:
  1. `POST /users/register`
  2. `POST /users/login`
  3. guardar `session_id`
  4. usar ese `session_id` en `POST /chat/messages`

### Cómo funciona la sesión

- Se guarda en Redis
- TTL: **24 horas**
- No usa header Authorization
- No usa cookies
- No usa JWT
- El cliente debe enviar `session_id` en el **body** de `POST /chat/messages`

### Estrategia recomendada en Kotlin

1. Hacer login
2. Guardar `session_id` en `DataStore` o `SharedPreferences`
3. Reutilizarlo en mensajes de chat
4. Si `/chat/messages` responde 404, asumir que la sesión expiró y volver a loguear

---

## 3. Tabla completa de endpoints reales

| Método | Path | Requiere auth | Cache/TTL | Propósito |
|---|---|---:|---|---|
| POST | `/users/register` | No | — | Registrar usuario |
| POST | `/users/login` | No | — | Login y creación de sesión |
| POST | `/sessions` | No | 24h | Crear sesión manual |
| GET | `/sessions/{session_id}` | No | 24h | Obtener sesión |
| PATCH | `/sessions/{session_id}/state` | No | 24h | Fusionar estado de sesión |
| DELETE | `/sessions/{session_id}` | No | — | Cerrar sesión |
| POST | `/chat/messages` | Sí, sesión activa | — | Guardar mensaje en conversación |
| GET | `/chat/conversations` | No | — | Listar conversaciones |
| GET | `/weather` | No | 15 min | Clima actual + forecast |
| GET | `/gbif/species` | No | 24h | Ocurrencias de especie por país |
| GET | `/geocode` | No | 30 días | Texto → coordenadas |
| GET | `/geocode/reverse` | No | 30 días | Coordenadas → lugar |
| GET | `/soil` | No | 90 días | Perfil de suelo |
| POST | `/pest/upload-url` | No | — | Signed URL para subir imagen |
| POST | `/pest/identify` | No | — | Identificar plaga por embedding |
| GET | `/elevation` | No | 365 días | Altitud |
| GET | `/climate/history` | No | 7 días | Histórico climático |
| GET | `/disease-risk` | No | reutiliza weather | Riesgo de enfermedad |
| GET | `/pest-risk` | No | reutiliza weather | Riesgo de plaga |

---

## 4. Endpoints detallados

## 4.1 Usuarios

### `POST /users/register`

**Archivo:** `domain/user/router.py`  
**Handler:** `sign_up_new_user`

### Qué hace

Registra un usuario nuevo con teléfono y password.

### Request

```json
{
  "phone": "+529991234567",
  "password": "supersecret"
}
```

### Validaciones

- `phone`: patrón `^\+?[0-9]{7,15}$`
- `password`: mínimo 8, máximo 128 caracteres

### Response `201`

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "phone": "+529991234567",
  "created_at": "2026-04-24T19:00:00Z"
}
```

### Errores

- `409` → teléfono ya registrado
- `422` → validación fallida

### Lógica interna

1. Busca por teléfono en Mongo
2. Si ya existe, devuelve 409
3. Hashea password con bcrypt
4. Guarda usuario en colección `users`

### Notas Kotlin

- `id` es UUID string, no ObjectId
- `created_at` parsearlo como `Instant`

---

### `POST /users/login`

**Archivo:** `domain/user/router.py`  
**Handler:** `log_in_user`

### Qué hace

Autentica por teléfono/password y además crea una sesión Redis.

### Request

```json
{
  "phone": "+529991234567",
  "password": "supersecret"
}
```

### Response `200`

```json
{
  "session_id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "user": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "phone": "+529991234567",
    "created_at": "2026-04-24T19:00:00Z"
  }
}
```

### Errores

- `401` → credenciales inválidas
- `422` → body inválido

### Lógica interna

1. Busca usuario en Mongo
2. Valida password con bcrypt
3. Crea sesión Redis con TTL 24h
4. Devuelve `session_id` + usuario público

### Notas Kotlin

- Guardar `session_id`, ese es el token real para chat
- No existe refresh token
- Timeout recomendado: **10s**

---

## 4.2 Sesiones

### `POST /sessions`

**Archivo:** `domain/session/router.py`  
**Handler:** `open_chat_session`

### Qué hace

Crea una sesión manual, útil si querés abrir flujo conversacional sin pasar por login.

### Request

```json
{
  "user_id": "+529991234567",
  "state": {}
}
```

### Response `201`

```json
{
  "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "user_id": "+529991234567",
  "state": {},
  "created_at": "2026-04-24T19:30:00Z",
  "updated_at": "2026-04-24T19:30:00Z"
}
```

### Lógica interna

- Genera UUID
- Guarda JSON serializado en Redis
- TTL 24 horas

---

### `GET /sessions/{session_id}`

**Archivo:** `domain/session/router.py`  
**Handler:** `get_chat_session`

### Qué hace

Obtiene la sesión activa desde Redis.

### Response `200`

Mismo schema que `POST /sessions`.

### Errores

- `404` → sesión inexistente o expirada

---

### `PATCH /sessions/{session_id}/state`

**Archivo:** `domain/session/router.py`  
**Handler:** `patch_chat_session_state`

### Qué hace

Fusiona claves en `state`.

### Request

```json
{
  "state": {
    "current_step": "geocoding",
    "crop": "corn",
    "location_found": true
  }
}
```

### Response `200`

Devuelve la sesión completa actualizada.

### Lógica interna

- Lee sesión actual
- Hace **merge superficial** del estado
- Actualiza `updated_at`
- Renueva TTL 24h

### Notas Kotlin

- `state` es libre, modelalo como `Map<String, JsonElement>` si querés máxima flexibilidad

---

### `DELETE /sessions/{session_id}`

**Archivo:** `domain/session/router.py`  
**Handler:** `close_chat_session`

### Qué hace

Elimina la sesión.

### Response

- `204 No Content`

---

## 4.3 Chat

### `POST /chat/messages`

**Archivo:** `domain/chat/router.py`  
**Handler:** `send_message_to_conversation`

### Qué hace

Persiste un mensaje dentro de la conversación del usuario, pero SOLO si la sesión Redis sigue activa.

### Request

```json
{
  "session_id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "message": {
    "role": "user",
    "content": "Hola, necesito ayuda con mi cultivo de maíz"
  }
}
```

### Reglas

- `role` debe ser uno de: `user`, `assistant`, `system`
- `content` no puede venir vacío

### Response `201`

```json
{
  "id": "+529991234567",
  "messages": [
    {
      "role": "user",
      "content": "Hola, necesito ayuda con mi cultivo de maíz",
      "created_at": "2026-04-24T19:35:00Z"
    }
  ],
  "created_at": "2026-04-24T19:30:00Z",
  "updated_at": "2026-04-24T19:35:00Z"
}
```

### Errores

- `404` → sesión inexistente o expirada
- `422` → payload inválido

### Lógica interna

1. Busca sesión en Redis
2. Toma `user_id` de esa sesión
3. Hace upsert en Mongo en colección `conversations`
4. La conversación queda identificada por el **teléfono** del usuario

### Notas Kotlin

- Si falla con 404, logueá de nuevo o recreá sesión
- El id de conversación NO es UUID: es el teléfono del usuario

---

### `GET /chat/conversations`

**Archivo:** `domain/chat/router.py`  
**Handler:** `list_conversations_for_user`

### Qué hace

Lista conversaciones.

### Query params

- `user_phone` opcional

### Lógica interna

- Si `user_phone` viene, filtra por ese usuario
- Si no viene, devuelve todas las conversaciones

### Response `200`

Array de conversaciones ordenadas por `updated_at` descendente.

---

## 4.4 Weather

### `GET /weather`

**Archivo:** `domain/weather/router.py`  
**Handler:** `get_weather`

### Qué hace

Trae clima actual + hourly + daily desde Open-Meteo.

### Query params

- `lat`: float `[-90, 90]`
- `lon`: float `[-180, 180]`

### Response `200`

```json
{
  "latitude": 14.5586,
  "longitude": -90.7295,
  "timezone": "America/Guatemala",
  "current": {
    "time": "2026-04-24T19:00",
    "temperature_2m": 22.5,
    "relative_humidity_2m": 65,
    "precipitation": 0.0,
    "weather_code": 2,
    "wind_speed_10m": 8.3
  },
  "hourly": {
    "time": ["2026-04-24T00:00"],
    "temperature_2m": [18.2],
    "relative_humidity_2m": [72],
    "precipitation_probability": [5]
  },
  "daily": {
    "time": ["2026-04-24"],
    "temperature_2m_max": [26.5],
    "temperature_2m_min": [15.2],
    "precipitation_sum": [0.0],
    "et0_fao_evapotranspiration": [3.2],
    "uv_index_max": [7.1]
  }
}
```

### Errores

- `422` → lat/lon inválidos
- `502` → falla upstream

### Cache

- Redis
- Key: `weather:{lat}:{lon}`
- TTL: **15 minutos**

### Notas Kotlin

- En arrays hourly/daily muchos valores pueden venir `null`
- Modelar como `List<Double?>` o `List<Float?>`
- Timeout recomendado: **10s**

---

## 4.5 GBIF

### `GET /gbif/species`

**Archivo:** `domain/gbif/router.py`  
**Handler:** `get_species_occurrence`

### Qué hace

Busca una especie en GBIF y resume ocurrencias en un país.

### Query params

- `scientific_name`: string, mínimo 2
- `country`: ISO alpha-2, default `GT`
- `limit`: int `1..300`, default `300`

### Response `200`

```json
{
  "found": true,
  "scientific_name": "Spodoptera frugiperda",
  "kingdom": "Animalia",
  "family": "Noctuidae",
  "common_names": [
    { "name": "Fall armyworm", "lang": "en" },
    { "name": "Gusano cogollero", "lang": "es" }
  ],
  "country": "GT",
  "total_records_in_country": 1423,
  "records_in_sample": 300,
  "top_regions": [["Jutiapa", 85]],
  "recent_years": { "2023": 45 },
  "interpretation": "Presencia abundante: muchos registros documentados."
}
```

### Particularidades

- `found: false` también puede devolver `200`
- Hace varias llamadas a GBIF por detrás

### Cache

- TTL: **24 horas**

### Notas Kotlin

- `top_regions` llega como lista de pares
- `recent_years` es mapa dinámico
- Timeout recomendado: **20s**

---

## 4.6 Geocoding

### `GET /geocode`

**Archivo:** `domain/geocoding/router.py`  
**Handler:** `forward_geocode`

### Qué hace

Convierte texto libre en coordenadas.

### Query params

- `q`: requerido
- `country`: opcional, ISO alpha-2

### Response `200`

```json
{
  "lat": 14.5586,
  "lon": -90.7295,
  "display_name": "Antigua Guatemala, Sacatepéquez, Guatemala",
  "country_code": "gt",
  "state": "Sacatepéquez",
  "municipality": "Antigua Guatemala",
  "type": "city"
}
```

### Errores

- `404` → sin match
- `422` → parámetros inválidos
- `502` → falla Nominatim

### Cache

- TTL: **30 días**

### Notas Kotlin

- `country_code`, `state`, `municipality`, `type` pueden ser `null`

---

### `GET /geocode/reverse`

**Archivo:** `domain/geocoding/router.py`  
**Handler:** `reverse_geocode_endpoint`

### Qué hace

Convierte coordenadas en información de lugar.

### Query params

- `lat`
- `lon`

### Response

Misma estructura que `/geocode`.

### Cache

- TTL: **30 días**

---

## 4.7 Soil

### `GET /soil`

**Archivo:** `domain/soil/router.py`  
**Handler:** `get_soil`

### Qué hace

Trae perfil de suelo por capas desde SoilGrids.

### Query params

- `lat`
- `lon`

### Response `200`

```json
{
  "lat": 14.5586,
  "lon": -90.7295,
  "horizons": [
    {
      "depth": "0-5cm",
      "ph": 6.2,
      "soc_g_per_kg": 12.4,
      "nitrogen_g_per_kg": 1.1,
      "clay_pct": 35.0,
      "sand_pct": 30.0,
      "silt_pct": 35.0,
      "cec_mmol_per_kg": 15.5,
      "texture_class": "clay loam"
    }
  ],
  "dominant_texture": "clay loam",
  "interpretation": "Horizonte superficial (0-5 cm): ligeramente ácido..."
}
```

### Errores

- `404` → sin cobertura
- `422` → lat/lon inválidos
- `502` → upstream error

### Cache

- TTL: **90 días**

### Notas Kotlin

- Todos los campos numéricos de `horizons` pueden ser `null`
- `interpretation` está pensada para mostrarse tal cual al agente

---

## 4.8 Pest image flow

### `POST /pest/upload-url`

**Archivo:** `domain/pest/router.py`  
**Handler:** `create_pest_upload_url`

### Qué hace

Devuelve una signed URL para subir imagen a GCS.

### Request

Sin body.

### Response `200`

```json
{
  "object_path": "queries/a1b2c3d4e5f6a7b8c9d0e1f2.jpg",
  "signed_url": "https://storage.googleapis.com/...",
  "content_type": "image/jpeg",
  "expires_in_seconds": 900
}
```

### Flujo correcto

1. Pedir upload URL
2. Hacer `PUT` binario a `signed_url`
3. Llamar a `/pest/identify` con `object_path`

---

### `POST /pest/identify`

**Archivo:** `domain/pest/router.py`  
**Handler:** `identify_pest_from_path`

### Qué hace

Descarga la imagen subida, genera embedding, busca vecinos más cercanos y decide la plaga más probable.

### Request

```json
{
  "object_path": "queries/a1b2c3d4e5f6a7b8c9d0e1f2.jpg"
}
```

### Response `200`

```json
{
  "top_match": {
    "pest_name": "Spodoptera_litura",
    "similarity": 0.87,
    "weighted_score": 3.2,
    "confidence": "high"
  },
  "alternatives": [
    { "pest_name": "Spodoptera_litura", "similarity": 0.87, "image_id": "pest_00123" }
  ],
  "votes": {
    "Spodoptera_litura": 3.2
  }
}
```

### Particularidades

- `top_match` puede ser `null`
- `alternatives` puede ayudar al agente a razonar
- Usa GCS + Gemini + Mongo Atlas Vector Search

### Notas Kotlin

- Timeout recomendado: **30s**
- `image_id` puede venir `null`

---

## 4.9 Elevation

### `GET /elevation`

**Archivo:** `domain/elevation/router.py`  
**Handler:** `get_elevation`

### Qué hace

Devuelve altitud en metros.

### Query params

- `lat`
- `lon`

### Response `200`

```json
{
  "lat": 14.5586,
  "lon": -90.7295,
  "elevation_m": 1530.2
}
```

### Cache

- TTL: **365 días**

---

## 4.10 Climate history

### `GET /climate/history`

**Archivo:** `domain/climate/router.py`  
**Handler:** `get_climate_history`

### Qué hace

Trae histórico climático desde NASA POWER.

### Query params

- `lat`
- `lon`
- `start` en formato `YYYY-MM-DD`
- `end` en formato `YYYY-MM-DD`
- `granularity`: `daily` o `monthly`

### Reglas

- `end >= start`
- si `granularity=daily`, máximo **366 días**

### Response `200`

```json
{
  "lat": 14.5586,
  "lon": -90.7295,
  "granularity": "monthly",
  "start": "2020-01-01",
  "end": "2023-12-31",
  "series": [
    {
      "date": "2020-01",
      "t2m": 18.5,
      "t2m_max": 25.2,
      "t2m_min": 12.1,
      "precipitation_mm": 2.5,
      "rh_pct": 72.0,
      "solar_mj_m2": 18.3
    }
  ]
}
```

### Cache

- TTL: **7 días**

### Notas Kotlin

- Todos los valores de `series` pueden venir `null`
- Para rangos largos usar `monthly`
- Timeout recomendado: **20s**

---

## 4.11 Disease risk

### `GET /disease-risk`

**Archivo:** `domain/disease_risk/router.py`  
**Handler:** `get_disease_risk`

### Qué hace

Calcula un score de riesgo de enfermedad usando clima de los próximos 7 días.

### Query params

- `lat`
- `lon`
- `disease`

### Response `200`

```json
{
  "disease": "coffee_rust",
  "lat": 14.5586,
  "lon": -90.7295,
  "risk_score": 0.73,
  "risk_level": "high",
  "factors": {
    "window_days": 7,
    "avg_temp_c": 22.4,
    "avg_humidity_pct": 84,
    "rainy_days": 4,
    "rule_notes": [
      "T° media 22.4°C en rango óptimo [21-25°C]"
    ]
  },
  "interpretation": "Riesgo alto de roya del café..."
}
```

### Enfermedades soportadas

`coffee_rust`, `late_blight`, `corn_rust`, `wheat_leaf_rust`, `wheat_yellow_rust`, `wheat_stem_rust`, `sugarbeet_cercospora`, `sugarbeet_rust`, `barley_rust`, `rice_blast`, `rice_brown_spot`, `rice_sheath_blight`, `rice_bacterial_leaf_blight`, `tomato_early_blight`, `tomato_late_blight`, `tomato_fusarium_wilt`, `potato_late_blight`, `potato_early_blight`, `bean_rust`, `bean_angular_leaf_spot`, `bean_anthracnose`, `banana_black_sigatoka`, `banana_fusarium_wilt`, `cardamom_rot`, `sugarcane_rust`, `sugarcane_smut`, `sugarcane_red_rot`, `rose_botrytis`, `rose_powdery_mildew`, `rose_downy_mildew`, `rose_black_spot`, `cacao_monilia`, `cacao_black_pod`, `cacao_witches_broom`, `cacao_frosty_pod`, `banana_moko`, `banana_cordana_leaf_spot`, `potato_bacterial_wilt`, `potato_blackleg`, `oca_downy_mildew`, `broccoli_downy_mildew`, `broccoli_black_rot`, `broccoli_alternaria`, `oil_palm_bud_rot`, `oil_palm_spear_rot`, `oil_palm_ganoderma`, `corn_gray_leaf_spot`, `corn_northern_leaf_blight`, `corn_stalk_rot`, `coffee_cercospora`

### Lógica detrás

- Reusa `/weather`
- Calcula promedio de temperatura y humedad
- Cuenta días lluviosos
- Aplica reglas por enfermedad

---

## 4.12 Pest risk

### `GET /pest-risk`

**Archivo:** `domain/pest_risk/router.py`  
**Handler:** `get_pest_risk`

### Qué hace

Calcula el riesgo climático de plagas para los próximos 7 días.

### OJO

Este endpoint existe en código real pero **no estaba documentado en README**.

### Query params

- `lat`
- `lon`
- `pest`

### Response `200`

```json
{
  "pest": "spider_mite",
  "pest_type": "mite",
  "life_stage_risk": "both",
  "affected_crops": ["corn", "bean", "tomato"],
  "lat": 14.5586,
  "lon": -90.7295,
  "risk_score": 0.82,
  "risk_level": "very_high",
  "factors": {
    "window_days": 7,
    "avg_temp_c": 30.5,
    "avg_humidity_pct": 45,
    "rainy_days": 1,
    "rule_notes": ["T° alta 30.5°C favorece reproducción"]
  },
  "virus_coalert": null,
  "interpretation": "Riesgo muy alto de Araña roja..."
}
```

### Plagas soportadas

- `spider_mite`
- `whitefly`
- `broad_mite`
- `white_grub`
- `thrips`
- `leafminer`
- `fall_armyworm`
- `root_knot_nematode`
- `coffee_berry_borer`

### Notas Kotlin

- `virus_coalert` puede ser `null`
- `pest_type` clasifica: `mite`, `insect`, `nematode`

---

## 5. Modelos y convenciones importantes para Kotlin

## 5.1 Nullables

Modelá como nullable todo esto:

- Weather hourly/daily arrays
- Soil horizons
- Climate series
- Campos opcionales de geocoding
- `top_match` en pest identify
- `virus_coalert` en pest risk

### Ejemplo Kotlin

```kotlin
@Serializable
data class PestIdentifyResponse(
    val top_match: PestTopMatch? = null,
    val alternatives: List<PestAlternative> = emptyList(),
    val votes: Map<String, Double> = emptyMap()
)
```

## 5.2 Timeouts sugeridos

| Endpoint | Timeout sugerido |
|---|---:|
| Login / Chat / Weather / Geocode | 10s |
| Soil / Climate / GBIF | 15-20s |
| Pest identify | 30s |

## 5.3 Manejo de errores sugerido

- `422`: revisar parámetros o body
- `404`: no encontrado o sesión expirada
- `409`: usuario ya registrado
- `502`: proveedor externo caído → conviene reintentar con backoff

---

## 6. Inconsistencias detectadas entre README y código

### 1. `GET /pest-risk` no estaba documentado

- Existe en `main.py`
- Router real: `domain/pest_risk/router.py`

### 2. `disease-risk` soporta muchas más enfermedades de las documentadas

- README menciona 3
- código soporta decenas

### 3. Ejemplos de chat rotos

Los ejemplos `.http` de chat no coinciden con las rutas reales:

- ejemplo roto: `POST /chat/sessions`
- ruta real: `POST /sessions`

- ejemplo roto: `POST /chat/conversations/{id}/messages`
- ruta real: `POST /chat/messages`

### 4. Posible bug en labels de confianza de `/pest/identify`

Hay un comportamiento raro en los umbrales de `high` y `medium`. Conviene revisarlo si esa etiqueta se usa en UX.

---

## 7. Recomendación para consumo desde app Kotlin

### Retrofit

- Usar Retrofit + OkHttp + kotlinx.serialization o Moshi
- Separar servicios por dominio:
  - `AuthApi`
  - `SessionApi`
  - `ChatApi`
  - `WeatherApi`
  - `GeoApi`
  - `SoilApi`
  - `PestApi`
  - `RiskApi`

### Reintentos

- Reintentar solo en `502`, timeouts y networking errors
- No reintentar automático en `422`, `401`, `409`

### Persistencia local mínima

- `session_id`
- `phone`
- última ubicación resuelta
- últimas consultas agroclimáticas cacheables si querés UX offline parcial

---

## 8. Mapa mental rápido para un agente

Si un agente tiene que operar sobre este backend, esta es la secuencia lógica:

1. Si necesita chat personalizado → login y guardar `session_id`
2. Si necesita entender una ubicación escrita → `/geocode`
3. Con coordenadas puede consultar:
   - `/weather`
   - `/soil`
   - `/elevation`
   - `/climate/history`
   - `/disease-risk`
   - `/pest-risk`
4. Si necesita biodiversidad/ocurrencias → `/gbif/species`
5. Si necesita identificar plaga por foto:
   - `/pest/upload-url`
   - subir imagen a GCS
   - `/pest/identify`
6. Si necesita persistir conversación → `/chat/messages`

---

## 9. Archivos fuente clave en el backend

- `main.py`
- `domain/user/router.py`
- `domain/session/router.py`
- `domain/chat/router.py`
- `domain/weather/router.py`
- `domain/gbif/router.py`
- `domain/geocoding/router.py`
- `domain/soil/router.py`
- `domain/pest/router.py`
- `domain/pest_risk/router.py`
- `domain/elevation/router.py`
- `domain/climate/router.py`
- `domain/disease_risk/router.py`

---

## 10. Conclusión

Este backend está bastante bien separado por dominios y para Kotlin se puede consumir sin drama, PERO hay que entender una cosa clave: **no hay auth global**, solo sesiones para chat. Todo lo demás son herramientas públicas especializadas.

Si querés, el siguiente paso te lo dejo todavía más en bandeja de plata y te puedo armar:

1. **interfaces Retrofit completas**
2. **data classes Kotlin listas para copiar/pegar**
3. **casos de uso recomendados para el agente en Android**
