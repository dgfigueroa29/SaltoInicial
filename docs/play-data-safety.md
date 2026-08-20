# Seguridad de datos en Google Play — SaltoInicial

Documento de referencia para completar la sección **Seguridad de datos** y la **declaración de ID
de publicidad** en Play Console.

> **Importante:** esto **no se resuelve solo con código**. Los cambios en el manifest dejan la
> recolección explícita y consistente, pero el flag se levanta recién cuando se actualiza el
> formulario en Play Console con lo que está más abajo.

> **Repositorio público.** Este documento describe *qué categorías* de datos recolecta cada SDK.
> No agregar acá claves de API, tokens, DSNs, IDs de cuenta de Play Console, credenciales ni
> identificadores de usuarios reales.

---

## 1. El reporte

> Código de versión 6: Declaración de política - Sección de seguridad de datos: Tipo de datos de ID
> de dispositivo u otros: ID de dispositivo u otros (algunos ejemplos comunes pueden incluir ID de
> publicidad, ID de Android, IMEI, BSSID, dirección MAC)

**Traducción:** el escaneo de Play detectó que la app accede a identificadores de dispositivo, pero
la declaración de Seguridad de datos del version code 6 **no declara** el tipo de dato *ID de
dispositivo u otros*.

## 2. Causa raíz

La app integra ocho SDKs de analítica, atribución, monitoreo y ads. Varios de ellos leen el
Advertising ID (GAID) y/o generan identificadores persistentes de instalación, y varios inyectan
`com.google.android.gms.permission.AD_ID` en el manifest final vía *manifest merge* — o sea, el
permiso ya estaba en el APK publicado aunque no figurara en
`app/src/main/AndroidManifest.xml`. Play lo detecta y lo contrasta contra el formulario.

No es un falso positivo: la app **sí** recolecta IDs de dispositivo.

## 3. Cambios de código ya aplicados

Ninguno altera el comportamiento en runtime. Son de trazabilidad y consistencia:

1. **`AD_ID` declarado explícitamente** en `app/src/main/AndroidManifest.xml`.
   Play exige este permiso en apps que targetean Android 13+ y acceden al ID de publicidad (sin él,
   el GAID se devuelve en ceros). Ya venía por merge desde `firebase-analytics` y `af-android-sdk`;
   ahora está a la vista, comentado, y es auditable contra este documento.

2. **Eliminado el meta-data `com.facebook.sdk.AdvertisingIdCollectionEnabled`.**
   Esa clave **no existe** en el SDK de Meta — las válidas son `AutoInitEnabled`,
   `AutoLogAppEventsEnabled`, `AdvertiserIDCollectionEnabled`, `CodelessDebugLogEnabled` y
   `MonitorEnabled` (constantes de `FacebookSdk`). Era un no-op en `true` que contradecía
   visualmente a `AdvertiserIDCollectionEnabled` en `false`, y hacía leer el manifest como si Meta
   recolectara el ad ID cuando no lo hace.

**Comportamiento efectivo tras el cambio:** el SDK de Meta sigue **sin** recolectar el Advertising
ID. El GAID lo siguen usando Firebase Analytics y AppsFlyer, como antes.

---

## 4. Qué completar en Play Console

### 4.1 Contenido de la app → ID de publicidad

| Campo | Respuesta |
|---|---|
| ¿Tu app usa el ID de publicidad? | **Sí** |
| Motivo de uso | **Analíticas** y **Publicidad o marketing** (atribución de instalaciones) |

Esta declaración es independiente de la de Seguridad de datos y también es obligatoria al targetear
Android 13+. Si falta, Play bloquea la publicación por separado.

### 4.2 Seguridad de datos → tipos de datos a declarar

El reporte nombra un solo tipo, pero si *ID de dispositivo* estaba sin declarar es muy probable que
falten los otros tres. Conviene revisarlos todos en la misma pasada para no volver a rebotar.

#### a) ID de dispositivo u otros ID — **el que motivó el reporte**

| Pregunta del formulario | Respuesta |
|---|---|
| ¿Se recopilan estos datos? | **Sí** |
| ¿Se comparten con terceros? | **Sí** (ver nota abajo) |
| ¿Se procesan de forma efímera? | **No** |
| ¿La recopilación es obligatoria? | **Obligatoria** — la app no ofrece opt-in ni opt-out |
| Propósitos | Estadísticas · Publicidad o marketing · Funciones de la app · Prevención de fraudes, seguridad y cumplimiento |

Qué cae acá concretamente: GAID, Android ID (fallback de AppsFlyer), AppsFlyer ID, Firebase App
Instance ID, Firebase Installation ID, distinct_id de Mixpanel, device ID de Amplitude, UUID de
New Relic e installation ID de Sentry.

> **Nota sobre "compartido":** Play no considera *compartir* el envío a un proveedor que procesa los
> datos por cuenta tuya. Los SDKs puramente analíticos (Firebase, Amplitude, Mixpanel, Sentry, New
> Relic) suelen encuadrar ahí. Meta, en cambio, usa los datos para fines propios de publicidad, y
> AppsFlyer distribuye datos de atribución a redes de medios. Por eso la respuesta segura es
> **Sí**. Confirmalo contra los DPAs que tengas firmados con cada proveedor.

#### b) Información y rendimiento de la app → Registros de fallos

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan? | **Sí** — Firebase Crashlytics, Sentry, New Relic |
| ¿Se comparten? | **Sí** |
| ¿Obligatoria? | **Obligatoria** |
| Propósitos | Estadísticas · Funciones de la app |

#### c) Información y rendimiento de la app → Diagnósticos

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan? | **Sí** — Firebase Performance Monitoring, New Relic, Sentry (`tracesSampleRate = 1.0`) |
| ¿Se comparten? | **Sí** |
| ¿Obligatoria? | **Obligatoria** |
| Propósitos | Estadísticas · Funciones de la app |

#### d) Actividad en la app → Interacciones con la app

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan? | **Sí** — eventos de `AnalyticsEvents` (`app_open`, `screen_view`, `webview_load_*`, `navigation_back`, `webview_error`) |
| ¿Se comparten? | **Sí** |
| ¿Obligatoria? | **Obligatoria** |
| Propósitos | Estadísticas · Publicidad o marketing |

#### e) Ubicación → Ubicación aproximada — *evaluar*

La app **no** pide permisos de ubicación. Pero Firebase Analytics y otros SDKs derivan una
geolocalización aproximada a partir de la IP del lado del servidor. Google documenta la ubicación
aproximada derivada de IP dentro de lo que reporta Google Analytics.

Criterio sugerido: **declararla como recopilada** con propósito *Estadísticas*. Es la opción
conservadora y no tiene costo; sub-declarar es justamente lo que generó este reporte.

---

## 5. Inventario de SDKs

Fuente: `gradle/libs.versions.toml` y `app/build.gradle.kts`.

| SDK | Dónde se inicializa | Identificadores que toca | Otros datos |
|---|---|---|---|
| Firebase Analytics | `MainActivity.onCreate()` | App Instance ID, **GAID** | Interacciones, ubicación aprox. por IP, info de dispositivo |
| Firebase Crashlytics | Auto (plugin) + `MainActivity` | Crashlytics Installation UUID | Stack traces, estado del dispositivo |
| Firebase Performance | `MainViewModel` (traces `webview_page_load`) | Installation ID, IP | Métricas de carga, atributo `url` |
| AppsFlyer | `MainActivity.setupTracking()` | **GAID**, Android ID (fallback), AppsFlyer ID, IP | Eventos, referrer de instalación |
| Install Referrer | Transitivo de AppsFlyer | — | Referrer de Play Store |
| Meta SDK + Audience Network | `MainActivity.getFacebookLogger()` | Ad ID **desactivado** por manifest | App events manuales |
| Amplitude | `MainActivity.setupTracking()` | Device ID (UUID) | Eventos (`offline = true`, `useBatch = true`) |
| Mixpanel | `MainActivity.setupTracking()` | `distinct_id`, info de dispositivo | Eventos (`trackAutomaticEvents = false`) |
| New Relic | `MainActivity.onCreate()` | UUID de dispositivo | Crashes, red, performance |
| Sentry | `SaltoInicialApp.onCreate()` | Installation ID | Errores, breadcrumbs, sesiones, traces |

---

## 6. Puntos abiertos a revisar

Ninguno es parte del reporte actual, pero los tres pueden generar el próximo:

1. **URLs enviadas a analítica.** `MainViewModel.onPageStarted/onPageFinished/onError` mandan la URL
   de cada página visitada a los cinco proveedores de analítica, y `FirebasePerformance` la guarda
   como atributo del trace. Son todas URLs dentro de `saltoinicial.com.ar`, así que encuadran mejor
   como *Interacciones con la app* que como *Historial de navegación web* — esa categoría apunta a
   navegadores de propósito general. Si en algún momento el WebView permite navegar fuera del
   dominio propio, hay que revisar la clasificación.

2. **No hay mecanismo de consentimiento.** Todos los SDKs arrancan en `onCreate()` sin pedir nada al
   usuario. Por eso la recolección se declara como *Obligatoria*. Si la app apunta a usuarios de la
   UE/EEA, esto además necesita un CMP compatible con el Consent Management Platform requirement de
   Play, que es un tema aparte de este reporte.

3. **Audience Network se inicializa sin mostrar ads.** `AudienceNetworkAds.initialize()` corre en
   `MainActivity.kt`, pero no hay ningún `AdView`, `NativeAd` ni `InterstitialAd` en el proyecto.
   Se está cargando un SDK de publicidad, con su superficie de datos asociada, sin usarlo. Si no hay
   plan de monetizar con Audience Network, sacarlo reduce lo que hay que declarar y el tamaño del
   APK.

---

## 7. Mantenimiento

**Al agregar, quitar o reconfigurar cualquier SDK de terceros, actualizar este documento y revisar
la declaración en Play Console antes de publicar.** El desfase entre lo que la app hace y lo que el
formulario declara es exactamente lo que Play penaliza.

---

## 8. Fuentes

- [Provide information for Google Play's Data safety section — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Advertising ID — Play Console Help](https://support.google.com/googleplay/android-developer/answer/6048248)
- [Behavior changes: Apps targeting Android 13 or higher — Android Developers](https://developer.android.com/about/versions/13/behavior-changes-13)
- [Firebase: Prepare for Google Play's Data safety section](https://firebase.google.com/docs/android/play-data-disclosure)
- [AppsFlyer: About device identifiers](https://support.appsflyer.com/hc/en-us/articles/4408847686161-About-device-identifiers)
- [AppsFlyer: Bulletin — The Android SDK adds the AD_ID permission](https://support.appsflyer.com/hc/en-us/articles/7569900844689-Bulletin-The-Android-SDK-adds-the-AD-ID-permission)
- [Meta: Getting Started with App Events for Android](https://developers.facebook.com/docs/app-events/getting-started-app-events-android/)
