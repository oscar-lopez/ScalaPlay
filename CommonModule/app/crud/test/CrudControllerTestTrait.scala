package common.traits.test

import akka.stream.Materializer
import common.traits.layers.{CrudController, CrudLogic}
import common.traits.model.{Entity, Row}
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{Format, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.co.jemos.podam.api.PodamFactoryImpl
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import scala.concurrent.Future

/**
  * Created by sfrsebastian on 4/13/17.
  */
trait CrudControllerTestTrait[T<:Row, K<:Entity[T], C<:CrudController[T,K], L<:CrudLogic[T,K]] extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures with MockitoSugar with CrudTest{

  val factory = new PodamFactoryImpl
  var logicMock:L
  var controller: C

  implicit val format:Format[T]
  implicit lazy val materializer: Materializer = app.materializer

  def generatePojo:T

  override def createTest: Unit = {
    "Al crear un nuevo recurso" must {
      "Se deberia retornar el mensaje esperado cuando el formato del recurso no corresponde al esperado" in {
        val request = FakeRequest().withJsonBody(Json.parse("{}"))
        val result = call(controller.create(), request)
        val response = contentAsString(result)
        assert(response == "Error en formato de contenido", "El mensaje deberia ser Error en formato de recurso")
        assert(status(result) == BAD_REQUEST, "El codigo de respuesta debe ser 400")
      }

      "Se deberia retornar el mensaje esperado cuando el recurso no pudo ser creado" in {
        val newResource = generatePojo
        val request = FakeRequest().withJsonBody(Json.toJson(newResource))
        when(logicMock.create(any())) thenReturn Future(None)
        val result = call(controller.create(), request)
        val response = contentAsString(result)
        assert(response == "El recurso no pudo ser creado", "El mensaje deberia ser El recurso no pudo ser creado")
        assert(status(result) == BAD_REQUEST, "El codigo de respuesta debe ser 400")
      }

      "Se deberia retornar el recurso creado cuando si pudo ser creado" in {
        val newResource = generatePojo
        val jsonResource = Json.toJson(newResource)
        val request = FakeRequest().withJsonBody(jsonResource)
        when(logicMock.create(any())) thenReturn Future(Some(newResource))
        val result = call(controller.create(), request)
        val jsonResponse = contentAsJson(result)
        assert(jsonResponse == jsonResource, "El json recibido debe corresponder al recurso obtenido por la logica")
        assert(status(result) == CREATED, "El codigo de respuesta debe ser 201")
      }
    }
  }

  override def getTest: Unit = {
    "Al solicitar un recurso" must {
      "Se debe recibir el json del recurso solicitado" in {
        val id = Random.nextInt(20)
        val newResource = generatePojo
        val request = FakeRequest()
        val jsonResource = Json.toJson(newResource)
        when(logicMock.get(anyInt())) thenReturn Future(Some(newResource))
        val result = call(controller.get(id), request)
        val jsonResponse = contentAsJson(result)
        assert(jsonResponse == jsonResource, "El json recibido debe corresponder al recurso obtenido por la logica")
        assert(status(result) == OK, "El codigo de respuesta debe ser 200")
      }

      "Se debe recibir el mensaje esperado cuando no existe el recurso" in {
        val id = Random.nextInt(20)
        val request = FakeRequest()
        when(logicMock.get(anyInt())) thenReturn Future(None)
        val result = call(controller.get(id), request)
        val response = contentAsString(result)
        assert(response == "El recurso no existe", "El mensaje deberia ser El recurso no existe")
        assert(status(result) == BAD_REQUEST, "El codigo de respuesta debe ser 400")
      }
    }
  }

  override def getAllTest: Unit = {
    "Al solicitar todos los recursos" must {
      "Se debe recibir el json de la coleccion solicitada" in {
        val newResource = (0 to 20).map(_ => generatePojo)
        val jsonResource = Json.toJson(newResource)
        val request = FakeRequest()
        when(logicMock.getAll) thenReturn Future(newResource)
        val result = call(controller.getAll, request)
        val jsonResponse = contentAsJson(result)
        assert(jsonResponse == jsonResource, "El json recibido debe corresponder al recurso obtenido")
        assert(status(result) == OK, "El codigo de respuesta debe ser 200")
      }
    }
  }

  override def updateTest: Unit = {
    "Al actualizar un recurso" must {
      "Se deberia retornar el mensaje esperado cuando el formato del recurso no corresponde al esperado" in {
        val request = FakeRequest().withJsonBody(Json.parse("{}"))
        val id = Random.nextInt(20)
        val result = call(controller.update(id), request)
        val response = contentAsString(result)
        assert(response == "Error en formato de contenido", "El mensaje deberia ser Error en formato de contenido")
        assert(status(result) == BAD_REQUEST, "El codigo de respuesta debe ser 400")
      }

      "Se deberia retornar el mensaje esperado cuando el elemento no pudo ser actualizado" in {
        val toUpdate = generatePojo
        val id = Random.nextInt(20)
        val request = FakeRequest().withJsonBody(Json.toJson(toUpdate))
        when(logicMock.update(anyInt(), any())) thenReturn Future(None)
        val result = call(controller.update(id), request)
        val response = contentAsString(result)
        assert(response == "El recurso no pudo ser actualizado", "El mensaje deberia ser El recurso no pudo ser actualizado")
        assert(status(result) == BAD_REQUEST, "El codigo de respuesta debe ser 400")
      }

      "Se deberia retornar el elemento creado cuando si pudo ser actualizado" in {
        val toUpdate = generatePojo
        val jsonResource = Json.toJson(toUpdate)
        val id = Random.nextInt(20)
        val request = FakeRequest().withJsonBody(Json.toJson(toUpdate))
        when(logicMock.update(anyInt(), any())) thenReturn Future(Some(toUpdate))
        val result = call(controller.update(id), request)
        val jsonResponse = contentAsJson(result)
        assert(jsonResponse == jsonResource, "El json recibido debe corresponder al recurso obtenido")
        assert(status(result) == OK, "El codigo de respuesta debe ser 200")
      }
    }
  }

  override def deleteTest: Unit = {
    "Al eliminar un recurso" must {
      "Se deberia retornar el mensaje esperado cuando el recurso no pudo ser eliminado" in {
        val id = Random.nextInt(20)
        val request = FakeRequest()
        when(logicMock.delete(anyInt())) thenReturn Future(None)
        val result = call(controller.delete(id), request)
        val response = contentAsString(result)
        assert(response == "El recurso con id dado no existe", "El mensaje deberia ser El recurso con id dado no existe")
        assert(status(result) == BAD_REQUEST, "El codigo de respuesta debe ser 400")
      }

      "Se deberia retornar el recurso eliminado cuando la operacion es exitosa" in {
        val toDelete = generatePojo
        val jsonResource = Json.toJson(toDelete)
        val id = Random.nextInt(20)
        val request = FakeRequest()
        when(logicMock.delete(anyInt())) thenReturn Future(Some(toDelete))
        val result = call(controller.delete(id), request)
        val jsonResponse = contentAsJson(result)
        assert(jsonResponse == jsonResource, "El json recibido debe corresponder al recurso eliminado")
        assert(status(result) == OK, "El codigo de respuesta debe ser 200")
      }
    }
  }
}