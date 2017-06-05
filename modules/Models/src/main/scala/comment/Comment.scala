/*
 * Desarrollado por: Sebastián Flórez
 * Universidad de los Andes
 * Ingeniería de Sistemas y Computación
 * Pregrado
 */
package comment.model

import book.model.Book
import crud.models.Row

case class Comment(id:Int, name:String, content:String, book:Book) extends Row

