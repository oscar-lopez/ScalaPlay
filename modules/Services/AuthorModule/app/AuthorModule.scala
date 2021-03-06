/*
 * Desarrollado por: Sebastián Flórez
 * Universidad de los Andes
 * Ingeniería de Sistemas y Computación
 * Pregrado
 */
package author.settings

import author.logic.AuthorLogic
import author.persistence.{AuthorPersistence, AuthorPersistenceTrait}
import author.traits.AuthorLogicTrait
import book.logic.AuthorBookLogic
import book.persistence.{BookPersistence, BookPersistenceTrait}
import book.traits.AuthorBookLogicTrait
import review.persistence.{ReviewPersistence, ReviewPersistenceTrait}
import play.api.{Configuration, Environment}
import play.api.inject.Module

class AuthorModule extends Module {
  def bindings(env: Environment, conf: Configuration) = Seq(
    bind[AuthorLogicTrait].to[AuthorLogic],
    bind[AuthorBookLogicTrait].to[AuthorBookLogic],
    bind[AuthorPersistenceTrait].to[AuthorPersistence],
    bind[BookPersistenceTrait].to[BookPersistence],
    bind[ReviewPersistenceTrait].to[ReviewPersistence]
  )
}
