# Administración de servidores remotos IPTV

Se agregó una opción **Servidores remotos** al menú de la app.

## Funciones
- Agregar servidores por nombre y URL/endpoint.
- Elegir parser AUTO, M3U o JSON.
- Activar/desactivar servidores guardados.
- Probar conexión: muestra código HTTP, Content-Type, canales detectados y una vista previa del cuerpo.
- Importar: descarga y parsea canales compatibles, los guarda en Room y asocia `serverId`.
- Sincronización inicial: si la base local no tiene canales, intenta sincronizar el primer servidor activo guardado.
- Parser M3U/M3U8 y JSON con arrays raíz o campos `channels`, `data` o `results`.
- Player Media3 existente con mensajes básicos de carga/listo/error.
- Room migrado de versión 1 a 2: nueva tabla `servers` y columna nullable `channels.serverId`.

## JSON reconocido
Cada item acepta nombre desde `name`, `title` o `channel`; URL desde `streamUrl`, `stream_url`, `url` o `link`; grupo desde `group` o `category`; logo desde `logo` o `image`.

## Nota
Los dominios que se agreguen manualmente deben ser fuentes públicas/autorizadas y entregar una lista o JSON compatible. Esta función no implementa acceso a APIs privadas, autenticación de terceros ni bypass de protecciones. Un endpoint que responda HTML, un JSON de estructura desconocida o un API sin documentación puede aparecer en diagnóstico, pero no importará canales automáticamente.
