package zuluaga.cluster
 
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.contrib.pattern.{ClusterClient, ClusterReceptionistExtension}
 
object DemoMaster {
 
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.parseString("""
     akka {
       actor {
         provider = "akka.cluster.ClusterActorRefProvider"
       }
       remote {
         transport = "akka.remote.netty.NettyRemoteTransport"
         log-remote-lifecycle-events = off
         netty.tcp {
           hostname = "127.0.0.1"
           port = 2551
         }
       }
 
       cluster {
         seed-nodes = [
           "akka.tcp://ClusterSystem@127.0.0.1:2551"
           ]
 
         roles = [master]
 
         auto-down = on
       }
     }""")
 
    val system = ActorSystem("ClusterSystem", ConfigFactory.load(config))
    val master = system.actorOf(Props[ClusterMaster], "master")
    ClusterReceptionistExtension(system).registerService(master)
  }
 
  class ClusterMaster extends Actor with ActorLogging {
    def receive = {
      case e =>
        log.info(s"from master : $e : $sender")
        sender ! "master : how are you?"
    }
  }
}
 
object DemoMember {
 
  def main(args: Array[String]) {
    val config = ConfigFactory.parseString("""
     akka {
       actor {
         provider = "akka.cluster.ClusterActorRefProvider"
       }
 
       remote {
         transport = "akka.remote.netty.NettyRemoteTransport"
         log-remote-lifecycle-events = off
         netty.tcp {
          hostname = "127.0.0.1"
          port = 3000
         }
       }
 
       cluster {
         seed-nodes = [
           "akka.tcp://ClusterSystem@127.0.0.1:2551"
           ]
 
         auto-down = on
       }
     }""")
 
    val system = ActorSystem("ClusterSystem", ConfigFactory.load(config))
    val clusterMember = system.actorOf(Props[ClusterMember], "member")
    ClusterReceptionistExtension(system).registerService(clusterMember)
  }
 
  class ClusterMember extends Actor with ActorLogging {
    def receive = {
      case e =>
        log.info(s"from member : $e : $sender")
        sender ! "member : how are you?"
    }
  }
}
 
object DemoClient {
 
  def main(args : Array[String]) {
    val config = ConfigFactory.parseString("""
     akka {
       actor {
         provider = "akka.remote.RemoteActorRefProvider"
       }
 
       remote {
         transport = "akka.remote.netty.NettyRemoteTransport"
         log-remote-lifecycle-events = off
         netty.tcp {
          hostname = "127.0.0.1"
          port = 5000
         }
       }
     }""")
 
    val system = ActorSystem("OTHERSYSTEM", ConfigFactory.load(config))
	val clusterClient = system.actorOf(Props[ClusterClient], "client")
	clusterClient ! 1
	clusterClient ! 2
	clusterClient ! 3
	
	class ClusterClient extends Actor with ActorLogging {
		val initialContacts = Set(
		  context.system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2551/user/receptionist"),
		  context.system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:3000/user/receptionist"))
 
		val c = context.system.actorOf(ClusterClient.props(initialContacts), "os-client")
	
		def receive = {
		  case i : Int =>
			c ! ClusterClient.Send("/user/master", s"hello - $i", localAffinity = true)
			c ! ClusterClient.Send("/user/member", s"hello - $i", localAffinity = true)
		  case e =>
			log.info(s"$e")
		}
	}
    
  }
}