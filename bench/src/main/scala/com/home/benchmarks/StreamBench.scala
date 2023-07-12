package com.home.benchmarks

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import cats.effect.std.Semaphore
import cats.effect.unsafe.IORuntime
import cats.effect.{Concurrent, IO}
import cats.implicits.{catsSyntaxParallelTraverse1, toFlatMapOps}
import cats.{Parallel, Traverse}
import fs2.Stream
import org.openjdk.jmh.annotations.{Scope, _}
import zio.stream._
import zio.{IO => _, Semaphore => _, _}

import java.util.concurrent.TimeUnit
import scala.collection.BuildFrom
import scala.concurrent.{Await, ExecutionContext, Future}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Measurement(iterations = 10, timeUnit = TimeUnit.SECONDS, time = 10)
@Warmup(iterations = 5, timeUnit = TimeUnit.SECONDS, time = 10)
@Threads(1)
class StreamBench {

  private implicit val actorSystem: ActorSystem = ActorSystem("benchmark")
  private implicit val ec: ExecutionContext = actorSystem.dispatcher

  private val data = 1 to (128 * 1024)
  private val parallelism = 8
  private val chunkSize = 1024

  private val zioRuntime = Runtime.default
  private val catsRuntime = IORuntime.global

  @Benchmark
  def CollectionBench =
    data
      .filter(_ => true)
      .map(_ + 1)
      .map(_ + 2)
      .flatMap(i => Seq(i, i, i))
      .toVector

  @Benchmark
  def ZStreamBench = runZio {
    ZStream.fromIterable(data, chunkSize)
      .filter(_ => true)
      .mapZIOPar(parallelism)(i => ZIO.succeed(i + 1))
      .map(_ + 2)
      .flatMap(i => ZStream(i, i, i))
      .runCollect
  }

  @Benchmark
  def ZStreamChunksBench = runZio {
    ZStream.fromIterable(data, chunkSize)
      .filter(_ => true)
      .mapChunksZIO(chunk => ZIO.withParallelism(parallelism)(chunk.mapZIOPar(i => ZIO.succeed(i + 1))))
      .map(_ + 2)
      .flatMap(i => ZStream(i, i, i))
      .runCollect
  }

  @Benchmark
  def AkkaStreamBench = runFuture {
    Source(data)
      .filter(_ => true)
      .mapAsync(parallelism)(i => Future(i + 1))
      .map(_ + 2)
      .flatMapConcat(i => Source(List(i, i, i)))
      .runWith(Sink.seq)
  }

  @Benchmark
  def AkkaStreamChunksBench = runFuture {
    Source(data)
      .filter(_ => true)
      .grouped(chunkSize)
      .mapAsync(parallelism)(chunk => parTraverseNFuture(parallelism, chunk)(i => Future(i + 1)))
      .flatMapConcat(i => Source(i))
      .map(_ + 2)
      .flatMapConcat(i => Source(List(i, i, i)))
      .runWith(Sink.seq)
  }

  @Benchmark
  def FS2Bench = runIO {
    Stream.fromIterator[IO](data.iterator, chunkSize)
      .filter(_ => true)
      .mapAsync(parallelism)(i => IO(i + 1))
      .map(_ + 2)
      .flatMap(i => Stream(i, i, i))
      .compile
      .toVector
  }

  @Benchmark
  def FS2ChunksBench = runIO {
    Stream.fromIterator[IO](data.iterator, chunkSize)
      .filter(_ => true)
      .chunks
      .evalMap(chunk => parTraverseN(parallelism, chunk)(i => IO(i + 1)))
      .flatMap(Stream.chunk)
      .map(_ + 2)
      .flatMap(i => Stream(i, i, i))
      .compile
      .toVector
  }

  @TearDown
  def close(): Unit = actorSystem.terminate()

  private def runZio[A](zio: Task[A]): A = Unsafe.unsafe(implicit unsafe => zioRuntime.unsafe.run(zio).getOrThrow())

  private def runFuture[A](future: Future[A]): A = Await.result(future, scala.concurrent.duration.Duration.Inf)

  private def runIO[A](io: IO[A]): A = io.unsafeRunSync()(catsRuntime)

  private def parTraverseNFuture[A, B, M[X] <: IterableOnce[X]](n: Int, ga: M[A])(f: A => Future[B])(implicit bf: BuildFrom[M[A], B, M[B]]): Future[M[B]] = {
    val sem = new java.util.concurrent.Semaphore(n)
    Future.traverse(ga)(a => Future(sem.acquire()).flatMap(_ => f(a)).andThen(_ => sem.release()))
  }

  private def parTraverseN[F[_] : Concurrent : Parallel, G[_] : Traverse, A, B](n: Int, ga: G[A])(f: A => F[B]) =
    Semaphore[F](n).flatMap { s =>
      ga.parTraverse(a => s.permit.use(_ => f(a)))
    }

}
