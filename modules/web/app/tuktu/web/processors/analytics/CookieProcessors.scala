package tuktu.web.processors.analytics

import tuktu.api.BaseProcessor
import tuktu.api.DataPacket
import play.api.libs.json.JsObject
import play.api.libs.iteratee.Enumeratee
import scala.concurrent.Future
import tuktu.api.WebJsObject
import tuktu.api.utils
import scala.concurrent.ExecutionContext.Implicits.global
import tuktu.api.WebJsCodeObject
import tuktu.api.WebJsFunctionObject

/**
 * Sets a cookie
 */
class SetCookieProcessor(resultName: String) extends BaseProcessor(resultName) {
    var value: String = _
    var expires: Option[String] = _
    var path: Option[String] = _
    
    override def initialize(config: JsObject) {
        value = (config \ "value").as[String]
        expires = (config \ "expires").asOpt[String]
        path = (config \ "path").asOpt[String]
    }
    
    override def processor(): Enumeratee[DataPacket, DataPacket] = Enumeratee.mapM((data: DataPacket) => Future {
        for (datum <- data) yield {
            // Evaluate
            val cValue = utils.evaluateTuktuString(value, datum)
            val cExpires = expires match {
                case Some(e) => utils.evaluateTuktuString(e, datum)
                case None => None
            }
            val cPath = path match {
                case Some(p) => utils.evaluateTuktuString(p, datum)
                case None => None
            }
            
            datum + (resultName -> new WebJsCodeObject(
                    "document.cookie=\"" +
                    resultName + "=" + cValue + {
                        cExpires match {
                            case Some(e) => "; expires=" + e
                            case None => ""
                        }
                    } + {
                        cPath match {
                            case Some(p) => "; path=" + p
                            case None => ""
                        }
                    } + "\""
            ))
        }
    })
}

/**
 * Gets a single cookie by name
 */
class GetCookieProcessor(resultName: String) extends BaseProcessor(resultName) {
    var name: String = _
    
    override def initialize(config: JsObject) {
        name = (config \ "name").as[String]
    }
    
    override def processor(): Enumeratee[DataPacket, DataPacket] = Enumeratee.mapM((data: DataPacket) => Future {
        for (datum <- data) yield {
            // Evaluate
            val cName = utils.evaluateTuktuString(name, datum)
            
            datum + ((resultName + "_fnc") -> new WebJsFunctionObject(
                    "getCookie" + resultName,
                    List(""),
                    """var name = """ + cName + """ + "=";
                        var ca = document.cookie.split(';');
                        for(var i=0; i<ca.length; i++) {
                            var c = ca[i];
                            while (c.charAt(0)==' ') c = c.substring(1);
                            if (c.indexOf(name) == 0) return c.substring(name.length,c.length);
                        }
                        return "";"""
            )) + ((resultName + "_call") -> new WebJsObject(
                    "getCookie" + resultName
            ))
        }
    })
}

/**
 * Gets all cookies
 */
class GetAllCookiesProcessor(resultName: String) extends BaseProcessor(resultName) {
    override def processor(): Enumeratee[DataPacket, DataPacket] = Enumeratee.mapM((data: DataPacket) => Future {
        for (datum <- data) yield {
            datum + (resultName -> new WebJsObject(
                    "document.cookie.split(';').map(function(elem) {" +
                    "var res={};var splt=elem.split('=');" +
                    "res[splt[0]]=splt.slice(1,array.length).join('=');" +
                    "return res;})"
            ))
        }
    })
    
}