/*
 * Desarrollado por: Sebastián Flórez
 * Universidad de los Andes
 * Ingeniería de Sistemas y Computación
 * Pregrado
 */
package controllers.traits

import auth.controllers.AuthUserHandler
import book.model.BookMin
import review.model.ReviewMin
import editorial.model._
import layers.controllers.CrudController
import play.api.libs.json.Json

trait EditorialControllerTrait extends CrudController[EditorialDetail, Editorial, EditorialPersistenceModel, EditorialTable] with AuthUserHandler {

  implicit val formatReviewMin = Json.format[ReviewMin]

  implicit val formatBookMin = Json.format[BookMin]

  implicit val formatDetail = Json.format[EditorialDetail]

  implicit val Model2Detail = EditorialDetailConverter
}