package crud.tests

import crud.layers.CrudPersistence
import crud.models.{Entity, ModelConverter, Row}
import crud.DatabaseOperations
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play._
import slick.jdbc.PostgresProfile.api._
import uk.co.jemos.podam.api.PodamFactoryImpl
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

trait CrudPersistenceTestTrait[S<:Row, T<:Row, K<:Entity[T]] extends PlaySpec with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures with CrudTest{

  private implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val factory = new PodamFactoryImpl
  val persistence : CrudPersistence[S, T, K]
  var seedCollection:Seq[T]

  implicit def Model2Persistence:ModelConverter[S,T]

  implicit def Persistence2Model:ModelConverter[T,S]

  implicit def S2T (s : S)(implicit converter : ModelConverter[S,T]) : T = converter.convert(s)

  implicit def T2S (t : T)(implicit converter : ModelConverter[T,S]) : S = converter.convert(t)

  override def beforeAll(): Unit = {
    DatabaseOperations.createIfNotExist[T,K](persistence.db, persistence.table)
  }

  override def beforeEach(){
    seedCollection = Seq()
    DatabaseOperations.DropCreate[T,K](persistence.db, persistence.table)
    Thread.sleep(1000)
    for(_ <- 0 to 19){
      val pojo = generatePojo
      seedCollection = seedCollection :+ pojo
    }
    Await.result(persistence.db.run(persistence.table ++= seedCollection), 10.second)
    Thread.sleep(1000)
  }

  override def afterAll():Unit = {
    DatabaseOperations.Drop[T,K](persistence.db, persistence.table)
  }

  def generatePojo:T

  def assertByProperties(e1:T, e2:T):Unit = {
    assert(e1.name == e2.name, "El nombre deberia ser el mismo")
  }

  def createTest:Unit={
    "Al insertar un nuevo objeto" must {
      "Los campos del objeto persistido deben ser iguales que los del objeto a crear" in {
        val newObject = generatePojo
        whenReady(persistence.runAction(persistence.createAction(newObject))) {element =>
            assert(element.id == seedCollection.length + 1, "El id debe incrementar en 1")
            assertByProperties(newObject, element)
        }
      }

      "El objeto creado debe existir en la base de datos" in {
        val newObject = generatePojo
        whenReady(persistence.runAction(persistence.createAction(newObject))) {element =>
          whenReady(persistence.db.run(persistence.table.filter(_.id === element.id).result.headOption)){
            case None => fail()
            case Some(queried) =>
              assert(element.id == seedCollection.length + 1, "El id debe incrementar en 1")
              assertByProperties(newObject, queried)
          }
        }
      }
    }
  }

  def getTest:Unit={
    "Al buscar un objeto" must {
      "Busqueda por id: Los campos del objeto obtenido deben ser iguales a los del objeto ya existentes en la base de datos" in {
        val id = Random.nextInt(20)
        val existingObject = seedCollection(id)
        whenReady(persistence.runAction(persistence.getAction(persistence.table.filter(_.id === id + 1)))){
          case None => fail("El objeto buscado deberia encontrarse")
          case Some(element) => assertByProperties(element, existingObject)
        }
      }

      "Busqueda por consulta: El campo de filtrado debe ser igual al del objeto ya existente" in {
        val id = Random.nextInt(20)
        val existingObject = seedCollection(id)
        val query = persistence.table.filter(_.name === existingObject.name)
        whenReady(persistence.runAction(persistence.getAction(query))){
          case None => fail("El objeto buscado deberia encontrarse")
          case Some(element) => assert(element.name == existingObject.name, "El nombre deberia ser el mismo")
        }
      }
    }
  }

  def getAllTest:Unit={
    "Al solicitar multiples objetos" must {
      "Solicitar todos: La longitud de la lista de objetos recibida debe ser la misma que la de objetos semilla" in {
        whenReady(persistence.runAction(persistence.getAllAction(persistence.table, 0, 100))){
          case Nil => fail("La coleccion no deberia ser vacia")
          case elements => assert(elements.length == seedCollection.length, "La cantidad de objetos recibida debe ser la misma que los objetos semilla")
        }
      }

      "Solicitar por consulta: Se debe recibir una lista de longitud 1 cuando se filtra por una consulta" in {
        val id = Random.nextInt(20)
        val existingObject = seedCollection(id)
        val query = persistence.table.filter(_.name === existingObject.name)
        whenReady(persistence.runAction(persistence.getAllAction(query))){
          case Nil => fail("La coleccion no deberia ser vacia")
          case elements => assert(elements.length == 1, "La cantidad de objetos recibidos debe ser 1")
        }
      }

      "La longitud de la lista de objetos recibida debe ser menor o igual a la especificada por los parametros de paginacion" in {
        assert(true)
      }
    }
  }

  def updateTest:Unit = {
    "Al actualizar un objeto" must {
      "Se debe retornar el objeto actualizado" in {
        val toUpdate = generatePojo
        val id = Random.nextInt(20)
        whenReady(persistence.runAction(persistence.updateAction(id + 1, toUpdate))) {
          case None => fail("Se deberia obtener el objeto actualizado")
          case Some(element) =>
            assert(element.id == id+1, "El id deberia ser el mismo")
            assertByProperties(element, toUpdate)
        }
      }

      "No se deberia actualizar ningún registro en la base de datos si el objeto no existe" in {
        val toUpdate = generatePojo
        val id = 21
        whenReady(persistence.runAction(persistence.updateAction(id, toUpdate))) {
          case Some(_) => fail("No se deberia obtener ningun objeto")
          case None => succeed
        }
      }

      "Los campos deben ser actualizados en la base de datos" in {
        val toUpdate = generatePojo
        val id = Random.nextInt(20)
        whenReady(persistence.runAction(persistence.updateAction(id + 1, toUpdate))) {
          case None => fail("El objeto deberia actualizarse")
          case Some(_) =>
            whenReady(persistence.db.run(persistence.table.filter(_.id === (id + 1)).result.headOption)){
              case Some(queried) => assertByProperties(toUpdate, queried)
              case None => fail("El elemento no fue encontrado en la base de datos")
            }
        }
      }
    }
  }

  def deleteTest: Unit ={
    "Al eliminar un objeto" must {
      "Se debe retornar el objeto eliminado" in {
        val id = Random.nextInt(20)
        val toDelete = seedCollection(id)
        whenReady(persistence.runAction(persistence.deleteAction(id + 1))){
          case None => fail("Se deberia obtener el objeto eliminado")
          case Some(element) =>
            assert(element.id == id + 1, "El id deberia ser el mismo")
            assertByProperties(toDelete, element)
        }
      }

      "No se deberia eliminar el objeto si este no existe en la base de datos" in {
        val id = 21
        whenReady(persistence.runAction(persistence.deleteAction(id))) {
          case Some(_) => fail("No se deberia obtener ningun objeto")
          case None => succeed
        }
      }

      "El objeto no deberia existir en la base de datos despues de eliminado" in {
        val id = Random.nextInt(20)
        whenReady(persistence.runAction(persistence.deleteAction(id + 1))) {
          case None => fail("Se deberia obtener el objeto eliminado")
          case Some(_) =>
            whenReady(persistence.db.run(persistence.table.filter(_.id === (id + 1)).result.headOption)){
              case Some(_) => fail("No se deberia encontrar el elemento eliminado")
              case None => succeed
            }
        }
      }
    }
  }
}