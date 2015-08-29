/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.handling

import org.slf4j.Logger
import ratpack.http.client.ReceivedResponse
import ratpack.test.internal.RatpackGroovyDslSpec

class RequestIdSpec extends RatpackGroovyDslSpec {

  def "add request id"() {
    given:
    handlers {
      all {
        render get(RequestId)
      }
    }

    when:
    ReceivedResponse response = get()

    then:
    response.body.text.length() == 36 // not the best test ever but UUIDs should be 36 characters long including the dashes.
  }

  def "use custom request id generator"() {
    given:
    bindings {
      bindInstance RequestId.Generator, { RequestId.of('foo') } as RequestId.Generator
    }
    handlers {
      all {
        render get(RequestId)
      }
    }

    when:
    ReceivedResponse response = get()

    then:
    response.body.text == 'foo'
  }

  def "request log includes request id"() {
    given:
    def logger = Mock(Logger) {
      isInfoEnabled() >> true
    }

    int count = 0
    bindings {
      bindInstance RequestId.Generator, { RequestId.of("request-${count++}") } as RequestId.Generator
    }
    handlers {
      all RequestLogger.ncsa(logger)
      path("foo") {
        render get(RequestId)
      }
      path("bar") {
        render get(RequestId)
      }
    }

    when:
    get("foo")

    then:
    1 * logger.info({ it.contains("\"GET /foo HTTP/1.1\" 200 9 id=request-0") })

    when:
    post("bar")

    then:
    1 * logger.info({ it.contains("\"POST /bar HTTP/1.1\" 200 9 id=request-1") })
  }
}
