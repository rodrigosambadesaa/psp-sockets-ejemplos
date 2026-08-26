/*
* SPDX-License-Identifier: MIT
*
* Java SE connectivity helper adapted for these exercises from Rodrigo Sambade's
* ConnectivityAndInternetAccess gist:
* https://gist.github.com/rodrigosambadesaa/6f8e895de21c76b946bf70a1deffe05b
*
* The source Gist attributes the original connectivity logic to Emil, str4d and
* Rodrigo Sambade. This adaptation keeps its DNS-first philosophy and adds a
* conservative TCP fallback for networks that block UDP/53.
*/
package dev.rodrigosambade.net;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
public final class ConnectivityAndInternetAccess {
 private static final InetSocketAddress[] DNS_RESOLVERS={new InetSocketAddress("1.1.1.1",53),new InetSocketAddress("8.8.8.8",53)};private static final InetSocketAddress[] TCP_FALLBACKS={new InetSocketAddress("1.1.1.1",443),new InetSocketAddress("8.8.8.8",443)};private ConnectivityAndInternetAccess(){}
 public record Result(boolean networkInterfaceAvailable,boolean internetReachable,String method,String endpoint){}
 public static boolean isLocalHost(String host){if(host==null||host.isBlank())return false;if(host.equalsIgnoreCase("localhost"))return true;try{InetAddress address=InetAddress.getByName(host);return address.isLoopbackAddress()||address.isAnyLocalAddress();}catch(IOException ignored){return false;}}
 public static boolean hasUsableNetworkInterface()throws SocketException{for(NetworkInterface nif:Collections.list(NetworkInterface.getNetworkInterfaces())){if(!nif.isUp()||nif.isLoopback()||nif.isVirtual())continue;if(Collections.list(nif.getInetAddresses()).stream().anyMatch(a->!a.isLoopbackAddress()&&!a.isLinkLocalAddress()))return true;}return false;}
 public static Result check(Duration timeout){int timeoutMs=Math.toIntExact(Math.max(1,timeout.toMillis()));final boolean interfaceAvailable;try{interfaceAvailable=hasUsableNetworkInterface();}catch(SocketException e){return new Result(false,false,"interface","enumeration-failed");}if(!interfaceAvailable)return new Result(false,false,"interface","none");for(InetSocketAddress resolver:DNS_RESOLVERS)if(dnsProbe(resolver,timeoutMs))return new Result(true,true,"dns",resolver.toString());for(InetSocketAddress endpoint:TCP_FALLBACKS)if(tcpProbe(endpoint,timeoutMs))return new Result(true,true,"tcp",endpoint.toString());return new Result(true,false,"none","unreachable");}
 private static boolean dnsProbe(InetSocketAddress resolver,int timeoutMs){int id=ThreadLocalRandom.current().nextInt(0x10000);byte[]query;try{query=dnsQuery(id,"example.com");}catch(IOException impossible){return false;}byte[]buffer=new byte[512];try(DatagramSocket socket=new DatagramSocket()){socket.connect(resolver);socket.setSoTimeout(timeoutMs);socket.send(new DatagramPacket(query,query.length));DatagramPacket response=new DatagramPacket(buffer,buffer.length);socket.receive(response);if(response.getLength()<12)return false;int responseId=((buffer[0]&0xff)<<8)|(buffer[1]&0xff);boolean responseBit=(buffer[2]&0x80)!=0;int rcode=buffer[3]&0x0f;return responseId==id&&responseBit&&rcode==0;}catch(IOException ignored){return false;}}
 private static byte[] dnsQuery(int id,String hostname)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream(64);try(DataOutputStream out=new DataOutputStream(bytes)){out.writeShort(id);out.writeShort(0x0100);out.writeShort(1);out.writeShort(0);out.writeShort(0);out.writeShort(0);for(String label:hostname.split("\\.")){byte[]encoded=label.getBytes(StandardCharsets.US_ASCII);if(encoded.length==0||encoded.length>63)throw new IOException("Invalid DNS label");out.writeByte(encoded.length);out.write(encoded);}out.writeByte(0);out.writeShort(1);out.writeShort(1);}return bytes.toByteArray();}
 private static boolean tcpProbe(InetSocketAddress endpoint,int timeoutMs){try(Socket socket=new Socket()){socket.connect(endpoint,timeoutMs);return true;}catch(IOException ignored){return false;}}
 public static void requireInternetForRemoteHost(String host,Duration timeout)throws IOException{if(isLocalHost(host))return;Result result=check(timeout);if(!result.internetReachable())throw new IOException("No usable Internet path detected for remote host "+host+" (networkInterfaceAvailable="+result.networkInterfaceAvailable()+")");}
}
