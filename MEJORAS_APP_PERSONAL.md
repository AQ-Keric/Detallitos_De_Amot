# Mejoras para uso personal — versión 2.2

Rama de trabajo: `mejoras-app-personal`, basada en `etapa-1-modelos` (`bbba3d2`). Incluye la Etapa 1 pendiente de fusionar. No se ha modificado `main`.

## Qué cambia

- La app conserva su logo, colores y navegación. Inicio muestra las ventas de hoy, acceso al inventario y respaldo/exportación.
- Productos, ventas y meta se guardan en una instantánea XML versionada, con escritura temporal, sincronización del archivo y reemplazo mediante rename en el mismo directorio. La versión anterior se conserva. No requiere una base de datos adicional ni una cuenta.
- La caché se actualiza después de confirmar el guardado. Registrar/anular una venta y modificar stock se guardan juntos. Las escrituras principales se ejecutan fuera del hilo de interfaz y se evita enviarlas dos veces mientras están en curso.
- Se mantienen los datos antiguos de SharedPreferences sin borrarlos. Si hay registros ilegibles o IDs de productos duplicados, la migración se detiene para evitar pérdida silenciosa. Las fechas legadas mantienen el texto original; el año inferido se indica como estimado.
- Las fotos seleccionadas se copian a almacenamiento privado con UUID, límite de 20 MB y error visible. Las miniaturas Android se cargan en segundo plano y se reduce el lado mayor. Las fotos históricas se conservan.
- Respaldo ZIP portable: datos, meta y fotos referenciadas. La importación valida versión, IDs, cantidades, rutas, entradas duplicadas, fotos y tamaños antes de mostrar la confirmación. Límites: 200 MB totales, 20 MB por foto, 10000 fotos y 100000 registros de cada tipo.
- Antes de restaurar se conservan los archivos de datos anteriores. Cancelar una importación elimina solo su carpeta provisional. Las imágenes restauradas no se eliminan al salir de la pantalla.
- Exportación CSV UTF-8 con BOM y separador punto y coma para planillas, incluyendo las ventas históricas y fechas originales. Escapa comillas y protege celdas de texto contra interpretación como fórmulas. CSV es un informe, no el respaldo restaurable.
- Dashboard: totales Long, ticket promedio corregido, períodos diferenciados por año, exclusión de fechas desconocidas en filtros de día/semana/mes, inventario bajo/agotado y aclaración de ganancia estimada. Los promedios usan períodos con ventas, no todos los días calendario.
- Formularios con validación de importes y stock, desplazamiento con teclado, búsqueda al elegir un producto y claves estables en listas. Historial agrupado por fecha completa.
- Se corrigen el BackHandler exclusivo de Android y el constructor inválido de ControladorImagen en Desktop. Desktop tiene persistencia en `$XDG_DATA_HOME/detallitos-de-amor`, o `~/.local/share/detallitos-de-amor`.
- Desugaring para usar java.time en Android 7/8 compatible con minSdk 24. R8 continúa desactivado.

## Verificación realizada

El compilador Kotlin 2.2.21 compiló el dominio y sus dos suites de pruebas con target JVM 11. JUnit 4.13.2 ejecutó **25 pruebas, todas correctas**, cubriendo migración, IDs, ventas/stock, fallos de escritura sin cambio de caché, recuperación de corrupción, restauración de imágenes, ZIP incompleto y rutas inválidas, CSV y cálculos históricos.

`git diff --check` no detectó problemas. La compilación completa de Compose/Android no pudo ejecutarse: el wrapper de Gradle no puede descargar su distribución desde Java en este entorno (`Network is unreachable`). No se ha generado ni probado un APK nuevo en un celular y no se afirma que las pantallas hayan compilado.

Se prepara `.github/workflows/verificar.yml`: pruebas JVM y Android, compilación debug y APK descargable como artefacto. La publicación fue bloqueada por revisión automática de autorización; todavía no se ha ejecutado en GitHub.

## Prueba al publicar

Desde la carpeta del proyecto, con cambios locales guardados:

```fish
git fetch origin
git switch --track origin/mejoras-app-personal
./gradlew :composeApp:jvmTest :composeApp:testDebugUnitTest :composeApp:assembleDebug
```

Si la rama ya existe localmente, usar `git switch mejoras-app-personal` y `git pull --ff-only origin mejoras-app-personal`.

Después de compilar, instalar por USB con `adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`. Para la primera prueba de teléfono conviene compilar localmente: un APK generado en CI tiene otra clave debug y no se puede instalar encima de uno firmado localmente sin usar la misma clave.

1. Crear dos productos (incluso con nombres iguales), una foto y una venta; cerrar y reabrir.
2. Renombrar/cambiar precios y comprobar el historial; anular y revisar stock.
3. Exportar ZIP y CSV desde Inicio > Respaldo y exportación; revisar el CSV con una planilla.
4. Añadir un producto extra y restaurar el ZIP: la vista previa debe mostrar cantidades originales y pedir confirmación; tras restaurar deben volver los datos y fotos del respaldo.
5. Verificar Inicio, filtros y dashboard; probar teclado, cámara, galería y navegación en la pantalla del teléfono.

## Límites conocidos

Se preserva la idea de app local y personal. No se añaden cuentas, sincronización en la nube, telemetría ni servicios comerciales. La persistencia reescribe la instantánea completa por operación; es adecuada para uso personal, no para millones de registros. Se conservan fotos y copias previas para proteger el historial; no se añade todavía borrado automático de archivos. Si una foto antigua ya falta en el dispositivo, no puede recuperarse por software: la exportación completa informa el problema en lugar de omitirla silenciosamente.

## Corrección de metas por período

La meta antigua se conserva exclusivamente en Total. Cada día, semana (lunes a domingo) y mes tiene su propia meta, asociada también al año; no se inventan montos para períodos nuevos. El dashboard indica el período, los pesos faltantes o excedidos y el porcentaje real, incluso sobre 100 %. Las metas se guardan fuera del hilo de interfaz, se restauran con el respaldo ZIP y conservan compatibilidad de lectura con la versión anterior del archivo.

### Ciclo de la meta global

Cambiar monto conserva el avance. Reiniciar pide confirmación y permite cambiar el monto; guarda una fecha de inicio y solo cuenta ventas posteriores. Eliminar quita la meta sin modificar ventas, fotos ni stock; crear otra después comienza desde ese momento. El avance de la meta es independiente de las métricas generales del dashboard, que conservan todo el historial. Cumplir una meta no la reinicia automáticamente. También se pueden quitar metas de períodos específicos. Estado activo, inicio y montos se guardan y viajan en el respaldo.

La compilación completa y pruebas JVM/Android del primer commit publicado pasaron en GitHub Actions (ejecución 34074295423). La ampliación del ciclo de metas requiere una nueva ejecución.
