/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import com.google.inject.Inject
import io.swagger.annotations._
import it.gov.daf.server.dataset.{DatasetService, Query}
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import it.gov.daf.server.dataset.json._

@Api(value = "dataset-manager")
class DatasetController @Inject() (
  configuration: Configuration,
  playSessionStore: PlaySessionStore,
  ws: WSClient,
  implicit val ec: ExecutionContext
) extends Controller with Common {

  val datasetService = new DatasetService(configuration.underlying, ws)(ec)

  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    httpMethod = "GET",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  def getSchema(
    @ApiParam(value = "the uri to access the dataset", required = true) uri: String
  ): Action[AnyContent] = Action.async { implicit request =>
    withAuthentication(request) { auth =>
      datasetService.schema(auth, uri)
        .map(st => Ok(st.prettyJson))
        .recover {
          case ex: Throwable =>
            BadRequest(ex.getMessage).as(JSON)
        }
    }
  }

  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    httpMethod = "GET",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  def getDataset(
    @ApiParam(value = "the uri to access the dataset", required = true) uri: String
  ): Action[AnyContent] = Action.async { implicit request =>
    withAuthentication(request) { auth =>
      datasetService.data(auth, uri)
        .map { df =>
          val records = s"[${df.toJSON.collect().mkString(",")}]"
          Ok(records)
        }
        .recover {
          case ex: Throwable =>
            BadRequest(ex.getMessage).as(JSON)
        }
    }
  }

  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    consumes = "application/json",
    httpMethod = "POST",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "query",
      value = "A valid query",
      required = true,
      dataType = "it.gov.daf.server.dataset.Query",
      paramType = "body"
    )
  ))
  def queryDataset(
    @ApiParam(value = "the uri to access the dataset", required = true) uri: String
  ): Action[AnyContent] = Action.async { request =>
    withAuthentication(request){ auth =>
      val query = request.body.asJson.map(_.as[Query])
      query match {
        case Some(q) =>
          datasetService.query(auth, uri, q)
            .map(df => Ok(s"[${df.toJSON.collect().mkString(",")}]"))

        case None =>
          Future.successful(BadRequest("Missing Query Body").as(JSON))
      }
    }
  }

}
