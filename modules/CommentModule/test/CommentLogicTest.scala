import comment.logic.CommentLogic
import comment.persistence.CommentPersistence
import crud.models.ModelConverter
import crud.tests.CrudLogicTestTrait
import models.comment._
import org.mockito.Mockito._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by sfrsebastian on 4/12/17.
  */
class CommentLogicTest extends CrudLogicTestTrait[Comment, CommentPersistenceModel, CommentTable, CommentLogic, CommentPersistence]{

  var persistenceMock = mock[CommentPersistence]

  var logic = new CommentLogic(persistenceMock)

  override implicit def Model2Persistence: ModelConverter[Comment, CommentPersistenceModel] = CommentPersistenceConverter

  override implicit def Persistence2Model: ModelConverter[CommentPersistenceModel, Comment] = PersistenceCommentConverter

  override def beforeEach(){
    persistenceMock = mock[CommentPersistence]
    when(persistenceMock.table) thenReturn mock[TableQuery[CommentTable]]
    logic = new CommentLogic(persistenceMock)
  }

  override def generatePojo: CommentPersistenceModel = factory.manufacturePojo(classOf[CommentPersistenceModel])
}
