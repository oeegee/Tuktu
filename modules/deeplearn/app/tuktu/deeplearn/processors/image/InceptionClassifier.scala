package tuktu.deeplearn.processors.image

import play.api.libs.iteratee.Enumeratee
import tuktu.api.DataPacket
import play.api.libs.json.JsObject
import tuktu.api.BaseProcessor
import java.net.URL
import scala.concurrent.Future
import tuktu.api.utils
import tuktu.deeplearn.models.image.InceptionV3
import scala.concurrent.ExecutionContext.Implicits.global

class InceptionClassifier(resultName: String) extends BaseProcessor(resultName) {
    var localRemote = "remote"
    var imageName: String = _
    var n: Int = _
    var flatten: Boolean = false
    var useCategories: Boolean = _
    var counter = 0
    
    override def initialize(config: JsObject) {
        (config \ "local_remote").asOpt[String] match {
            case Some(lr) if lr == "local" => localRemote = "local"
            case _ => 
        }
        imageName = (config \ "image_name").as[String]
        n = (config \ "top_n").asOpt[Int].getOrElse(3)
        flatten = (config \ "flatten").asOpt[Boolean].getOrElse(false)
        useCategories = (config \ "use_categories").asOpt[Boolean].getOrElse(false)
        
        // Load model
        InceptionV3.load
    }
    
    def getImageLabels(uri: String) = {
        val labels = InceptionV3.classifyFile(uri, if (flatten) 1 else n, useCategories)
        if (flatten) labels.head._1 else labels
    }
    
    def getImageLabels(uri: URL) = {
        val labels = InceptionV3.classifyFile(uri, if (flatten) 1 else n, useCategories)
        if (flatten) labels.head._1 else labels
    }
    
    override def processor(): Enumeratee[DataPacket, DataPacket] = Enumeratee.mapM((data: DataPacket) => Future {
        data.map{datum =>
            datum + (resultName -> {
                counter += 1
                if (counter % 25 == 0) {
                    System.gc
                    counter = 0
                }
                
                // Get image, check if it's a list of URLs or a hard coded URL
                datum.get(imageName) match {
                    case Some(value: Seq[String]) => {
                        // Get class for each image in the list
                        value.map{uri =>
                            if (localRemote == "remote") getImageLabels(new URL(uri)) else getImageLabels(uri)
                        }
                    }
                    case _ => {
                        val uri = utils.evaluateTuktuString(imageName, datum)
                        if (localRemote == "remote") getImageLabels(new URL(uri)) else getImageLabels(uri)
                    }
                }
            })
        }
    })
}