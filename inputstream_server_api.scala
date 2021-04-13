import java.io.InputStream
import java.security.{ KeyStore, SecureRandom }

import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.{ ConnectionContext, Http, HttpsConnectionContext }
import com.github.ghik.silencer.silent
import com.typesafe.sslconfig.akka.AkkaSSLConfig
implicit val system = ActorSystem()
implicit val dispatcher = system.dispatcher

// Manual HTTPS configuration

// using httpsServer

val password: Array[Char] = "divers_bot".toCharArray // do not store passwords in code, read them from somewhere safe!



val ks: KeyStore = KeyStore.getInstance("PKCS12")
val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")

require(keystore != null, "Keystore required!")
ks.load(keystore, password)

val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
keyManagerFactory.init(ks, password)

val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
tmf.init(ks)

val sslContext: SSLContext = SSLContext.getInstance("TLS")
sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
val https: HttpsConnectionContext = ConnectionContext.httpsServer(sslContext)


//running server http and https


// you can run both HTTP and HTTPS in the same application as follows:
val commonRoutes: Route = get { complete("//") }
Http().newServerAt("127.0.0.1", 443).enableHttps(https).bind(commonRoutes)
Http().newServerAt("127.0.0.1", 8000).bind(commonRoutes)

// core server api


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl._

implicit val system = ActorSystem()
implicit val executionContext = system.dispatcher

val serverSource: Source[https.IncomingConnection, Future[https.ServerBinding]] =
  https().newServerAt("localhost", 8000).connectionSource()
val bindingFuture: Future[https.ServerBinding] =
  serverSource.to(Sink.foreach { connection => // foreach materializes the source
    println("Accepted new connection from " + connection.remoteAddress)
    // ... and then actually handle the connection
  }).run()
