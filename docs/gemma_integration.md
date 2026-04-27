# Guía de Integración: Gemma 4 On-Device (AgroGem)

Esta documentación detalla la implementación del sistema de inferencia local utilizando **Gemma 4 (LiteRT-LM)** dentro de la aplicación AgroGem.

## 1. Arquitectura General

El sistema sigue un patrón de **Expected/Actual** de Kotlin Multiplatform (KMP) para permitir una interfaz común mientras se aprovechan las capacidades nativas de Android.

### Componentes Clave:
- **`GemmaManager` (Common)**: Interfaz que define las capacidades de inferencia (texto, imágenes, streaming).
- **`AndroidGemmaManager` (Android)**: Implementación nativa que orquesta el motor LiteRT-LM.
- **`GemmaModelDownloader`**: Encargado de la gestión de descarga y almacenamiento del modelo (2.5GB).

## 2. Proceso de Inicialización

La inicialización del motor es la parte más crítica debido al consumo de recursos. Se realiza en `AndroidGemmaManager.initialize()`:

1.  **Priorización de Backends**:
    - Se intenta inicializar con **GPU** (aceleración por hardware).
    - Si falla, se realiza un fallback automático a **CPU**.
2.  **Configuración del Motor**:
    - `visionBackend`: Forzado a GPU para procesamiento de imágenes eficiente.
    - `cacheDir`: Configurado en la caché de la app para mejorar el tiempo de carga en ejecuciones posteriores.

## 3. Manejo de Imágenes (Multimodal)

El motor nativo de Gemma **no puede leer URIs de Android** (`content://`). Por ello, implementamos un conversor de URIs:

- **Función `getLocalPath`**: Toma una URI del Photo Picker, la copia a un archivo temporal en el almacenamiento interno y devuelve una ruta absoluta (`/data/user/0/...`).
- **Orden de Contenido**: Para Gemma 4, las imágenes deben enviarse **antes** que el texto en el builder de contenidos para maximizar la precisión del diagnóstico.

4. Inferencia en Segundo Plano (GemmaWorker)

Para tareas que no requieren interacción directa con el usuario (ej: procesar todas las fotos de un cultivo tomadas durante el día), se debe implementar un `GemmaWorker` heredando de `CoroutineWorker`.

### Patrón Recomendado:
- **`doWork()`**:
    - Obtener la instancia de `GemmaManager`.
    - Inicializar el motor si no lo está.
    - Llamar a `sendMessage()` (versión suspendida, no-streaming) para procesar los datos.
    - Retornar `Result.success()` o `Result.retry()`.
- **Restricciones de Red**: No requiere red, pero sí un nivel de batería suficiente debido al alto consumo del NPU/GPU.

## 5. Proceso de Descarga (GemmaModelDownloader)

El modelo de 2.5GB se gestiona mediante `WorkManager` para asegurar que la descarga continúe incluso si la app se cierra o el dispositivo se reinicia.

### Flujo de Trabajo:
1.  **`GemmaModelDownloader.downloadModel(url)`**: Registra una tarea única en `WorkManager`.
2.  **`DownloadWorker`**:
    - Descarga el archivo `.litertlm` en fragmentos.
    - Publica el progreso mediante notificaciones del sistema.
    - Guarda el archivo final en el almacenamiento interno privado (`filesDir/models/`).
3.  **Verificación**: `modelDownloader.isModelDownloaded()` comprueba la existencia física y el tamaño del archivo antes de intentar cargarlo.

## 5. Secuencia de Llamadas Necesaria

Para evitar errores de "File not found" o bloqueos de UI, sigue este orden:

```kotlin
// 1. Verificar si el modelo existe
if (modelDownloader.isModelDownloaded()) {
    // 2. Inicializar el motor (proceso pesado, usar background thread)
    gemmaManager.initialize(modelDownloader.getModelPath())
} else {
    // 3. Si no existe, disparar descarga
    modelDownloader.downloadModel(MODEL_URL)
}

// 4. Una vez inicializado (isInitialized.value == true), enviar mensajes
gemmaManager.sendMessageStream(...)
```

## 6. Hoja de Ruta para Extensión a toda la App

Para llevar la potencia de Gemma 4 a otras áreas (como el chat principal o análisis automático), recomendamos:

1.  **Singleton Global**: Mover la instancia de `GemmaManager` a un nivel superior (ej. `Koin` o un Singleton en `AndroidMain`) para no reinicializar el motor en cada pantalla.
2.  **Estado de Descarga Centralizado**: Usar `DataStore` o una tabla en `Room` para rastrear el progreso de descarga globalmente, permitiendo mostrar una barra de progreso en el `Dashboard` principal.
3.  **Inferencia en Segundo Plano**: Implementar un `GemmaWorker` para tareas que no requieran streaming, como la categorización automática de fotos de la galería mientras el usuario no usa la app.
4.  **Gestión de Memoria**: Liberar el motor (`gemmaManager.close()`) cuando el usuario entre en secciones de la app que consuman mucha RAM (ej. edición de video o mapas complejos) para evitar cierres forzados por el sistema (OOM).

## 7. Inferencia y Streaming

### Añadir soporte para Audio:
1.  Actualizar la interfaz `GemmaManager` para aceptar `audioPath`.
2.  En `AndroidGemmaManager`, añadir `Content.AudioFile(path)` al builder de contenidos.
3.  Asegurar que el `audioBackend` en `EngineConfig` esté configurado (CPU es recomendado para audio).

### Cambiar de Modelo:
Solo es necesario actualizar la URL en `GemmaDemoViewModel`. El sistema de descarga y el `GemmaManager` detectarán el nuevo archivo `.litertlm` automáticamente.

---
**Nota de Rendimiento**: El modelo requiere aproximadamente 3GB de RAM libre para funcionar fluidamente en dispositivos Android. Se recomienda monitorear el Logcat con el tag `GemmaManager`.
