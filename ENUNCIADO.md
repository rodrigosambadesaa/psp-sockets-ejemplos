# Material de partida — Ejemplos de sockets TCP

Colección del tema **Sockets** de Programación de Servicios y Procesos destinada a practicar la comunicación TCP cliente/servidor.

El material original de Drive contiene varios ejemplos progresivos y ejercicios. Entre los ejercicios documentados se encuentran:

- un servidor no concurrente que recibe dos números, calcula suma y resta y devuelve ambos resultados;
- la versión concurrente del servidor anterior;
- un servidor concurrente de alarma para varios edificios, cuyos clientes envían periódicamente temperatura y concentración de CO₂ y reciben `Todo OK` o `ALARMA` según los umbrales definidos.

Este repositorio consolida el patrón fundamental de esos ejemplos en una implementación TCP moderna tipo eco, con cierre correcto de recursos, UTF-8, timeouts y concurrencia limitada.

No utiliza una base de datos.
