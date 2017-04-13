package logic.bookModule

import common.traits.app.CrudLogic
import models.bookModule.{Book, Books}
import persistence.bookModule.BookPersistenceTrait

trait BookLogicTrait extends CrudLogic[Book, Books] {
  val persistence : BookPersistenceTrait
}
