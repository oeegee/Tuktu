package tuktu.nosql.generators

import tuktu.api._
import play.api.libs.json.JsValue
import play.api.libs.iteratee.Enumeratee
import tuktu.nosql.util.cassandra
import scala.collection.JavaConversions._

class CassandraGenerator(resultName: String, processors: List[Enumeratee[DataPacket, DataPacket]]) extends AsyncGenerator(resultName, processors) {
    override def receive() = {
        case config: JsValue => {
            // Get the hostname
            val host = (config \ "host").as[String]

            // Set up the connection
            val client = new cassandra.client(host)

            // Determine how this query should be executed
            val executionType = (config \ "type").asOpt[String].getOrElse("default")

            // Get the query
            val query = (config \ "query").as[String]

            // Run the query
            executionType match {
                case _ => {
                    val rows = client.runQuery(query)

                    // Go over the rows and push them
                    for (row <- rows)
                        channel.push(new DataPacket(List(Map(resultName -> row))))
                }
            }
            
            // We stop once the query is done
            client.close()
            self ! StopPacket
        }
        case sp: StopPacket => {
            cleanup()
        }
    }
}