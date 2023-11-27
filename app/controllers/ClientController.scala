package controllers

import models.{Client, Clients}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
//import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientController @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider,
 cc:ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile]
{
  import profile.api._
  implicit val clientFormat: Writes[Client] = Json.writes[Client]
  implicit val clientFormatR: Reads[Client] = Json.reads[Client]
  def getAllClients: Action[AnyContent] = Action.async {
    db.run(Clients.table.result).map { clientsList =>
      Ok(Json.toJson(clientsList))
    }
  }

  def getClientByCode(code:Int): Action[AnyContent] = Action.async{ implicit request=>
    val query = Clients.table.filter(_.codigo===code)
    val result:Future[Option[Client]] = db.run(query.result.headOption)
    result.map {
      case Some(cliente) => Ok(Json.toJson(cliente))
      case None => NotFound(s"No se encontró el cliente con el código $code")
    }
  }

  /*def createClient(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val clientResult = request.body.validate[Client]

    clientResult.fold(
      errors => {
        println(s"Error de validación JSON: $errors")
        Future.successful(BadRequest(Json.obj("message" -> s"Error en el formato JSON: $errors")))

      },
      client => {
        val insertQuery = Clients.table returning Clients.table.map(_.codigo) into ((_, codigo) => codigo) += client
        val result: Future[Int] = db.run(insertQuery)

        result.map { id =>
          Ok(Json.obj("codigo" -> id, "message" -> "Cliente creado con éxito"))
        }
      }
    )
  }*/
  def createClient(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val clientResult = request.body.validate[Client]

    clientResult.fold(
      errors => {
        println(s"Error de validación JSON: $errors")
        Future.successful(BadRequest(Json.obj("message" -> "Error en el formato JSON")))
      },
      client => {
        val insertQuery = Clients.table += client
        val result: Future[Int] = db.run(insertQuery)

        result.map { _ =>
          Ok(Json.obj("message" -> "Cliente creado con éxito"))
        }
      }
    )
  }
  def updateClient(code: Int): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val clientResult = request.body.validate[Client]

    clientResult.fold(
      errors => {
        println(s"Error de validación JSON: $errors")
        Future.successful(BadRequest(Json.obj("message" -> "Error en el formato JSON")))
      },
      updatedClient => {
        val updateQuery = Clients.table.filter(_.codigo === code).update(updatedClient)
        val result: Future[Int] = db.run(updateQuery)

        result.map {
          case 0 =>
            NotFound(Json.obj("message" -> s"No se encontró un cliente con código $code"))
          case _ =>
            Ok(Json.obj("codigo" -> code, "message" -> "Cliente actualizado con éxito"))
        }
      }
    )
  }

  def deleteClient(code: Int): Action[AnyContent] = Action.async { implicit request =>
    val deleteQuery = Clients.table.filter(_.codigo === code).delete
    val result: Future[Int] = db.run(deleteQuery)

    result.map { _ =>
      Ok(Json.obj("message" -> "Cliente eliminado con éxito"))
    }
  }
}
