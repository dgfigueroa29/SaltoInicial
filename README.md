# SaltoInicial

App Android que envuelve el sitio [saltoinicial.com.ar](https://www.saltoinicial.com.ar/) en un
WebView nativo con Jetpack Compose.

## Stack

- Kotlin 2.4.0 · AGP 9.2.1 · JVM 17
- Jetpack Compose (BOM 2026.05.01) + Material 3
- minSdk 24 · compileSdk / targetSdk 37
- Clean Architecture (domain / data / presentation) con patrón MVI e inyección manual de
  dependencias vía `ViewModelProvider.Factory`

## Compilar

```bash
./gradlew assembleDebug     # APK de debug
./gradlew assembleRelease   # APK de release (R8 + shrinkResources)
./gradlew test              # Tests unitarios
./gradlew detekt            # Análisis estático
```

## Configuración

La app integra varios SDKs de analítica, atribución y monitoreo. Sus claves se inyectan en
`BuildConfig` en tiempo de compilación desde `local.properties` (ignorado por git) o como
propiedades de Gradle:

```properties
appsFlyerDevKey=
amplitudeApiKey=
sentryDsn=
newRelicAppToken=
mixpanelProjectToken=
facebookAppId=
facebookClientToken=
```

Todas son opcionales: si una clave está vacía, el SDK correspondiente no se inicializa y se registra
una advertencia. El proyecto compila y corre sin ninguna configurada.

> Este repositorio es público. **No commitear claves, tokens, DSNs ni keystores.**

## Privacidad

La app recolecta identificadores de dispositivo y datos de uso a través de sus SDKs de terceros.
El inventario completo por SDK y la declaración correspondiente para la sección de Seguridad de
datos de Google Play están documentados en [`docs/play-data-safety.md`](docs/play-data-safety.md).

## Documentación

| Documento | Contenido |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Guía de arquitectura, convenciones y tareas comunes |
| [`CLAUDE.md`](CLAUDE.md) | Guía para Claude Code |
| [`docs/play-data-safety.md`](docs/play-data-safety.md) | Recolección de datos y declaración en Play Console |

## Licencia

[MIT](LICENSE)
