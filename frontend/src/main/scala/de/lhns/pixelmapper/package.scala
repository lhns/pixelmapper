package de.lhns

import cats.effect.Async
import cats.syntax.all._
import fs2.Stream
import org.scalajs.dom.ReadableStream

import scala.scalajs.js.typedarray.Uint8Array

package object pixelmapper {
  def fromReadableStream[F[_]](rs: ReadableStream[Uint8Array])(implicit F: Async[F]): Stream[F, Byte] =
    Stream.bracket(F.delay(rs.getReader()))(r => F.delay(r.releaseLock())).flatMap { reader =>
      Stream.unfoldChunkEval(reader) { reader =>
        F.fromPromise(F.delay(reader.read())).map { chunk =>
          if (chunk.done)
            None
          else
            Some((fs2.Chunk.uint8Array(chunk.value), reader))
        }
      }
    }
}
