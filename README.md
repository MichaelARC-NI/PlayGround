# Volt Trigger

Gatillos L/R para Android usando los botones de volumen. Vol+ se convierte en el gatillo
izquierdo y Vol− en el derecho, sin root y sin Internet.

La app está pensada para jugar en horizontal y está inspirada en los gatillos de teléfonos como
el Redmi K40/K50 Gaming. No está afiliada a Xiaomi ni a Redmi.

<!-- omgithub:readme:start -->
## 🚀 Build, play, and remix with OMGithub

**Created using [OMGithub.com](https://omgithub.com).**

[![OMGithub](https://img.shields.io/badge/OMGithub-Open%20project-orange?style=for-the-badge)](https://omgithub.com/MichaelARC-NI/PlayGround)
[![GitHub](https://img.shields.io/badge/GitHub-Source-181717?logo=github&style=for-the-badge)](https://github.com/MichaelARC-NI/PlayGround)

- 🎮 [Open the project](https://omgithub.com/MichaelARC-NI/PlayGround).
- ✨ [Remix this project](https://omgithub.com/?remix=MichaelARC-NI%2FPlayGround).
- 💻 [Explore the source](https://github.com/MichaelARC-NI/PlayGround).
- 🛠️ [Check build runs](https://github.com/MichaelARC-NI/PlayGround/actions).
- 🐛 [Report an issue](https://github.com/MichaelARC-NI/PlayGround/issues).
- 👤 [Explore the creator's projects](https://omgithub.com/MichaelARC-NI).
- 🌍 [Create with OMGithub](https://omgithub.com).
<!-- omgithub:readme:end -->

## Características

- Vol+ → L y Vol− → R, con coordenadas independientes.
- Modo **Toque**: un tap corto configurable (60 ms por defecto, de 20 a 250 ms).
- Modo **Mantener**: el dedo virtual permanece pulsado mientras mantienes la tecla.
- L y R pueden mantenerse pulsados a la vez, incluso pulsando una tecla después de la otra.
- Editor flotante a pantalla completa para arrastrar L y R sobre el botón real del juego.
- Burbuja flotante **V** para abrir el editor sin salir del juego; se puede arrastrar.
- Posiciones guardadas como porcentaje de la pantalla, así que se adaptan a rotación y resolución.
- Destello visual en el punto de toque y vibración opcional.
- Interfaz en español, detección del estado de accesibilidad y atajo a los ajustes.
- Sin root, sin permisos de Internet y sin lectura del contenido de otras apps.

## Requisitos

- Android 8.0 o superior (`minSdk 26`).
- Servicio de accesibilidad activado manualmente.
- Para compilar: JDK 17, Android SDK 35 y las herramientas de Gradle incluidas.

## Instalación

El APK de depuración se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

También se deja una copia en `dist/VoltTrigger-1.0.0-debug.apk`.

Instalación por ADB:

```bash
adb install -r dist/VoltTrigger-1.0.0-debug.apk
```

Abre **Volt Trigger**, pulsa **Activar** en el bloque de accesibilidad y habilita el servicio en
*Ajustes → Accesibilidad → Aplicaciones instaladas → Volt Trigger*.

## Uso rápido

1. Activa la accesibilidad y luego enciende **Activar mapeo**.
2. En **Tus gatillos**, elige **Toque** o **Mantener** para L y R.
3. Pulsa **Mover en pantalla** para abrir el editor y arrastra L/R al botón del juego.
4. Pulsa **Guardar**. Las posiciones quedan guardadas como porcentaje.
5. Abre el juego. Mientras el mapeo esté activo, Vol+ y Vol− disparan L y R.

Atajos:

- Toca la burbuja **V** para abrir el editor mientras juegas.
- Mantén y arrastra la burbuja si tapa un control.
- **Probar** envía un toque de prueba al punto configurado.
- **Restablecer posiciones** vuelve a L al 10 % · 30 % y R al 90 % · 30 %.

## Compilar

```bash
./gradlew assembleDebug
./gradlew lintDebug
```

APK resultante:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Cómo funciona

`TriggerAccessibilityService` filtra los eventos de Vol+ y Vol− con
`FLAG_REQUEST_FILTER_KEY_EVENTS`. Según el modo configurado, pide un tap o un hold a
`GestureInjector`, que inyecta los toques con `AccessibilityService.dispatchGesture`.

Los holds se mantienen con `StrokeDescription` y `continueStroke`. Para poder añadir un segundo
puntero sin cancelar el primero, el nuevo puntero empieza unos milisegundos después dentro del
mismo gesto; así Android permite continuar el dedo existente y registrar un
`ACTION_POINTER_DOWN` adicional. Al soltar una tecla, su puntero termina con
`willContinue=false` mientras el otro puede seguir pulsado.

`OverlayController` dibuja la burbuja y el editor sobre el juego. `EditorOverlayView` permite
arrastrar los puntos y guarda coordenadas normalizadas en `SharedPreferences`.

## Estructura

```text
app/src/main/java/com/michaelarc/volttrigger/
├── MainActivity.java                 Interfaz principal y permisos
├── TriggerAccessibilityService.java  Intercepción de teclas y ciclo de vida
├── GestureInjector.java              Taps, holds y multi-touch por accesibilidad
├── OverlayController.java            Burbuja, editor y destellos
├── EditorOverlayView.java            Editor arrastrable L/R
├── AppPrefs.java                     Preferencias y coordenadas
├── Trigger.java                      Modelo L/R
├── BubbleView.java                   Burbuja flotante V
├── FlashView.java                    Anillo de confirmación
└── HeroArtView.java                  Ilustración de la portada
```

## Limitaciones conocidas

- **Las teclas de volumen se consumen** mientras el mapeo está activo. Para cambiar el volumen,
  apaga el mapeo o usa el panel de volumen del sistema.
- Algunos juegos o apps pueden bloquear, ignorar o cancelar los gestos inyectados por
  accesibilidad; pruébalo en cada juego.
- Android cancela los gestos inyectados si detecta un toque real simultáneo en la pantalla. Si eso
  ocurre mientras mantienes un gatillo, suelta y vuelve a pulsar la tecla.
- Los fabricantes con gestores de batería agresivos pueden detener el servicio. Excluye la app de
  la optimización de batería si deja de responder en segundo plano.
- La app se verificó en un emulador Android 15. No se probó en un K40/K50 físico.
- Respeta las normas de uso de cada juego.

## Verificación

Se comprobó en un emulador Android 15:

- Tap de L y R en el punto configurado.
- Hold de L, hold simultáneo de L+R y liberación de R manteniendo L.
- Re-pulsación rápida de R sin cancelar L.
- Editor, guardado, reset y rotación a horizontal.
- Capturas locales en `screenshots/`.
