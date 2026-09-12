# Política de privacidad — Mi Garaje

Última actualización: 11/09/2026

Mi Garaje permite al usuario guardar información sobre sus vehículos. Esta política describe los datos tratados por la aplicación y los servicios externos utilizados.

## Datos de cuenta
El inicio de sesión utiliza Google Authentication mediante Firebase Authentication. La aplicación recibe los datos básicos necesarios para identificar la cuenta, como identificador Firebase (UID), nombre, correo electrónico y, cuando esté disponible, imagen de perfil. No recibe ni almacena la contraseña de Google.

## Datos del vehículo
El usuario puede introducir datos de vehículos, kilometraje, mantenimiento, reparaciones, repostajes, gastos, neumáticos, inspecciones, seguros, impuestos, piezas, modificaciones, recordatorios y notas. Estos datos se almacenan en Cloud Firestore bajo el UID autenticado. Las reglas de seguridad están diseñadas para que cada cuenta acceda únicamente a su propio espacio de datos.

## Fotografías
Las fotografías de vehículos se conservan localmente por defecto. Si el desarrollador habilita expresamente Firebase Storage, la aplicación puede sincronizarlas en una ruta asociada al UID del usuario.

## Publicidad y consentimiento
La versión gratuita puede utilizar Google AdMob. Google User Messaging Platform (UMP) se utiliza para solicitar o gestionar el consentimiento cuando corresponda, incluyendo regiones del EEE. El usuario puede volver a abrir las opciones de privacidad desde Ajustes. Los usuarios PRO no reciben banners de la aplicación.

## Compras
La compra opcional Mi Garaje PRO se procesa mediante Google Play Billing. La aplicación consulta Google Play para determinar el derecho de uso PRO. Mi Garaje no recibe los datos completos del medio de pago.

## Datos locales y funcionamiento offline
La aplicación utiliza Firestore con caché offline y DataStore para preferencias como tema, unidades, vehículo seleccionado y notificaciones. WorkManager mantiene los recordatorios locales programados.

## Analítica
La versión entregada no integra Firebase Analytics ni un sistema propio de analítica de comportamiento.

## Eliminación de cuenta y datos
El usuario puede solicitar la eliminación desde Ajustes > Eliminar cuenta. La aplicación elimina los vehículos y sus subregistros en Firestore, las imágenes sincronizadas cuando Firebase Storage está habilitado y solicita a Firebase Authentication eliminar la identidad. Firebase puede exigir una autenticación reciente para completar el último paso.

## Terceros
Los servicios de Google/Firebase empleados pueden tratar datos conforme a sus propias políticas: Firebase Authentication, Cloud Firestore, Firebase App Check, Firebase Storage cuando se habilite, Google AdMob/UMP y Google Play Billing.

## Contacto
Contacto del desarrollador: `DEVELOPER_CONTACT` (sustituir por el correo real antes de publicar).
