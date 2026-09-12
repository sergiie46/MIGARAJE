# Ayuda para Data Safety de Google Play

Revisar esta hoja contra la configuración final de Firebase/AdMob antes de enviar el formulario.

- **Datos de cuenta:** nombre, correo e identificador de usuario, usados para autenticación y sincronización.
- **Contenido generado por el usuario:** información y notas de vehículos/mantenimiento/reparaciones/gastos; finalidad principal: funcionalidad de la app y sincronización.
- **Fotos:** solo si el usuario selecciona/toma una foto. Locales por defecto; pueden sincronizarse si Firebase Storage está habilitado.
- **Compras:** el estado/derecho de Mi Garaje PRO se consulta mediante Google Play Billing; el procesamiento del pago lo realiza Google Play.
- **Publicidad:** la versión FREE integra AdMob y UMP. La declaración exacta de identificadores/dispositivos depende de la configuración final de anuncios y consentimiento de Google Mobile Ads SDK.
- **Analítica:** esta entrega no incluye Firebase Analytics.
- **Cifrado en tránsito:** Firebase y servicios Google usan transporte HTTPS/TLS.
- **Eliminación:** existe eliminación de cuenta dentro de la app; borra datos Firestore y solicita borrar la identidad Firebase. Storage se elimina si esa función está habilitada.
- **Compartición:** comprobar en el formulario la definición de Google sobre “compartidos” para los proveedores de servicio Google/Firebase/AdMob de la configuración final.

No copies respuestas automáticamente sin revisar las categorías que muestre Play Console en la fecha de publicación.
