package zuluaga.cluster
 
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.contrib.pattern.{ShardRegion, ClusterSharding, ClusterClient, ClusterReceptionistExtension}
import akka.persistence.{SnapshotOffer, EventsourcedProcessor}

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
       persistence.journal.leveldb.native = off
     }""")
 
    val system = ActorSystem("ClusterSystem", ConfigFactory.load(config))

    ClusterSharding(system).start(
      typeName = "ClusterMaster",
      entryProps = Some(Props[ ClusterMaster ]),
      idExtractor = ClusterMaster.idExtractor,
      shardResolver = ClusterMaster.shardResolver)

    ClusterSharding(system).start(
      typeName = "AnotherClusterMaster",
      entryProps = Some(Props[ ClusterMaster ]),
      idExtractor = ClusterMaster.idExtractor,
      shardResolver = ClusterMaster.shardResolver)

    val master = system.actorOf(Props[ClusterMasterProxy], "master")
    ClusterReceptionistExtension(system).registerService(master)
  }
  class ClusterMasterProxy extends Actor with ActorLogging {
    def receive = {

      case e: Int =>
        //extra pattern
        val originalSender = sender
        context.actorOf(Props(new Actor() {
          val region: ActorRef = ClusterSharding(context.system).shardRegion("ClusterMaster")

          override def receive = {
            case x => originalSender ! x
          }

          region ! e
        }))
    }
  }
  object ClusterMaster {
    val idExtractor: ShardRegion.IdExtractor = {
      case c: Int => (c.toString, c)
    }

    val shardResolver: ShardRegion.ShardResolver = msg => msg match {
      case c: Int => c.toString
    }
  }
  class ClusterMaster extends EventsourcedProcessor with ActorLogging {
    var count: Int = 0

    def update(e: Int) =
      count = count + 1


    override def receiveCommand = {
      case e :Int =>
        persist(e)(update)
        log.info(s"from master : $e : $count : $sender")
        sender ! s"master : how are you? + $count"
    }

    override def receiveRecover = {
      case e: Int =>
        update(e)
      case SnapshotOffer(_, snapshot: Int) =>
        count = snapshot
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
			c ! ClusterClient.Send("/user/master", i, localAffinity = true)
			c ! ClusterClient.Send("/user/member", s"hello - $i", localAffinity = true)
		  case e =>
			log.info(s"$e")
		}
	}
    
  }
}