/**
 *  Copyright 2013 Robert Welin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooo.nilewapps.bokbytarappen.server.authentication

import scala.concurrent.{ExecutionContext, Future}
import spray._
import routing._
import authentication._

import com.mooo.nilewapps.bokbytarappen.server.data._

/**
 * Defines fuctions to extract a Token from an `Authorization` header with
 * the format `Nilewapp key="value",...` to a Map.
 */
class NilewappTokenAuthenticator[U](
    val realm: String,
    val authenticator: Option[Token] => Future[Option[U]])
    (implicit val executionContext: ExecutionContext)
  extends ContextAuthenticator[U] {

  def apply(ctx: RequestContext) = {
    authenticate(ctx) map {
      case Some(token) => Right(token)
      case None => Left {
        AuthenticationFailedRejection(realm)
      }
    }
  }

  def scheme = "Nilewapp "

  def params(ctx: RequestContext): Map[String, String] = Map.empty

  /**
   * Extracts the token from the Authorization header and passes it as
   * the argument to the authenticator.
   */
  def authenticate(ctx: RequestContext) = {
    authenticator {
      ctx.request.headers.find(_.name == "Authorization") match {
        case Some(head) =>
          if (head.value.startsWith(scheme)) {
            TokenFactory(asMap(head.value.replaceFirst(scheme, "")))
          } else None
        case _ => None
      }
    }
  }

  /**
   * Decodes the token in the Authorization header.
   */
  def asMap(s: String) = {
    lazy val de = new sun.misc.BASE64Decoder()
    s.split(",").map {
      a => a.split("=") match {
        case Array(key, value @ _*) =>
          (key, new String(de.decodeBuffer(value.mkString("=").replaceAll("\"", ""))))
      }
    }.toMap
  }
}
