/*
 * Derived from https://github.com/spray/spray/blob/v1.1-M7/spray-http/src/main/scala/spray/http/parser/HttpParser.scala
 *
 * Copyright (C) 2011-2012 spray.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.http4s
package parser

import org.http4s.util.CaseInsensitiveString

import scalaz.{Failure, Validation, Success}
import org.http4s.util.string._


private[http4s] object HttpParser extends HttpParser

private[parser] trait HttpParser extends SimpleHeaders
                    with AcceptHeader
                    with AcceptLanguageHeader
                    with CacheControlHeader
                    with ContentTypeHeader
                    with CookieHeader
                    with AcceptCharsetHeader
                    with AcceptEncodingHeader
                    with AuthorizationHeader
                    with RangeParser
                    with LocationHeader
                    with ProxyAuthenticateHeader
                    with WwwAuthenticateHeader {

  type HeaderParser = String => ParseResult[Header]

  val rules: Map[CaseInsensitiveString, HeaderParser] =
    this
      .getClass
      .getMethods
      .filter(_.getName.forall(!_.isLower)) // only the header rules have no lower-case letter in their name
      .map { method =>
        method.getName.replace('_', '-').ci -> { value: String =>
          method.invoke(this, value)
        }.asInstanceOf[HeaderParser]
      }.toMap

  def parseHeader(header: Header.Raw): ParseResult[Header] = {
    rules.get(header.name) match {
      case Some(parser) => parser(header.value)
      case None => ParseResult.success(header) // if we don't have a rule for the header we leave it unparsed
    }
  }

  /**
   * Warm up the header parsers by triggering the loading of most classes in this package,
   * so as to increase the speed of the first usage.
   */
  def warmUp() {
    val results = List(
      Header("Accept", "*/*,text/plain,custom/custom"),
      Header("Accept-Charset", "*,UTF-8"),
      Header("Accept-Encoding", "gzip,custom"),
      Header("Accept-Language", "*,nl-be,custom"),
      Header("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="),
      Header("Cache-Control", "no-cache"),
      Header("Connection", "close"),
      Header("Content-Disposition", "form-data"),
      Header("Content-Encoding", "deflate"),
      Header("Content-Length", "42"),
      Header("Content-Type", "application/json"),
      Header("Cookie", "http4s=cool"),
      Header("Host", "http4s.org"),
      Header("X-Forwarded-For", "1.2.3.4"),
      Header("Fancy-Custom-Header", "yeah")
    ) map parseHeader
    assert(results.forall(_.isRight))
  }
}
