package controllers.book

import auth.controllers.AuthUserHandler
import author.AuthorDetail
import author.model._
import book.model.{Book, BookMin, BookPersistenceModel, BookTable}
import comment.model.CommentMin
import editorial.model.EditorialMin
import layers.controllers.ManyToManyController
import play.api.libs.json.Json

/**
  * Created by sfrsebastian on 5/29/17.
  */
trait BookAuthorControllerTrait extends ManyToManyController[Book, BookPersistenceModel, BookTable, AuthorDetail, Author, AuthorPersistenceModel, AuthorTable] with AuthUserHandler{

  implicit val commentMinFormat = Json.format[CommentMin]

  implicit val editorialMin = Json.format[EditorialMin]

  implicit val authorMin = Json.format[AuthorMin]

  implicit val bookMin = Json.format[BookMin]

  implicit val formatDetail = Json.format[AuthorDetail]

  implicit def Detail2Model = AuthorDetailConverter

  def relationMapper(book:Book):Seq[Author] = book.authors

  def inverseRelationMapper(author:Author):Seq[Book] = author.books
}