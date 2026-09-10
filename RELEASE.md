# Versión de uso diario
La versión 2.5 conserva las funciones de 2.4. Release no permite depuración.
La minificación sigue desactivada para mantener el comportamiento ya probado.

## Compilar
Desde la raíz del proyecto:
```fish
bash gradlew :composeApp:jvmTest :composeApp:testDebugUnitTest :composeApp:assembleRelease
```
GitHub Actions también genera el artefacto Detallitos-Android-release-sin-firma.
Ese APK sin firma NO es el instalador final.

## Firmar
Conserva el respaldo privado fuera del repositorio. Contiene:
- detallitos-release.p12: clave privada y certificado.
- clave.txt: contraseña de la clave.
- certificado.pem: certificado público.
No compartas el respaldo con usuarios ni lo subas a GitHub. Guarda una copia
adicional en un lugar privado. Usa SIEMPRE esta misma clave para actualizar.

Ejemplo en Linux, ajustando las rutas a tu SDK y al respaldo:
```fish
python3 tools/firmar_release.py composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk "$HOME/.local/share/detallitos-firma" "$HOME/Descargas/Detallitos_2.5.apk" --build-tools "$HOME/Android/Sdk/build-tools/35.0.0"
```
El script alinea a 16 KiB, firma y verifica; no imprime la contraseña.
En cada nueva versión incrementa versionCode y conserva applicationId y clave.
La clave no se almacena en GitHub ni se genera de nuevo en CI.

## Entregar y actualizar
Entrega solo el APK firmado. Una instalación nueva empieza vacía.
Las actualizaciones firmadas con esta clave se instalan encima conservando datos.
El paso inicial desde un APK debug antiguo puede requerir reinstalar: exporta
antes el respaldo de datos a Descargas u otro lugar fuera de la app, verifica
que existe, instala release y restaura. Nunca desinstales con datos sin respaldo.
La firma estable permite actualizaciones; no sustituye los respaldos periódicos.
