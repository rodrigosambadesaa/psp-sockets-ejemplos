# PSP · Ejemplos de sockets

Suite moderna de ejemplos TCP cliente/servidor para PSP. Los sockets se cierran con `try-with-resources`, el servidor limita concurrencia mediante `ExecutorService`, el protocolo es UTF-8 y los clientes usan timeout.

Para un host remoto se aplica la comprobación de conectividad adaptada del Gist MIT de Rodrigo Sambade: https://gist.github.com/rodrigosambadesaa/6f8e895de21c76b946bf70a1deffe05b. **`localhost` y loopback no requieren Internet**, porque una práctica local debe funcionar incluso sin WAN.

```bash
mvn verify
# Terminal 1
java -cp target/classes dev.rodrigosambade.sockets.EchoServer 9000
# Terminal 2
java -cp target/classes dev.rodrigosambade.sockets.EchoClient localhost 9000 hola
```
