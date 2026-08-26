package dev.rodrigosambade.sockets;
import org.junit.jupiter.api.Test;import java.util.concurrent.*;import static org.junit.jupiter.api.Assertions.*;
class EchoServerTest{@Test void localEchoWorksWithoutInternetGate()throws Exception{try(var s=new EchoServer(0);var ex=Executors.newSingleThreadExecutor()){ex.submit(()->{try{s.serve();}catch(Exception ignored){}});assertEquals("ECHO hola",EchoClient.echo("localhost",s.port(),"hola"));}}}
