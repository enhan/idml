package io.idml.server

import cats.effect.IO
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

/** Runs recursive work on user input (IDML parsing, JSON to IdmlValue conversion, JSON rendering)
  * on dedicated threads with an explicit stack size, so that hostile deeply nested input ends in a
  * StackOverflowError on one of these threads, which we turn into an error, instead of a fatal
  * error on a cats-effect compute thread, which would shut down the whole JVM.
  */
object BoundedStack {
  private val log = LoggerFactory.getLogger(getClass)

  /** Stack size of the worker threads: 16x the usual 1 MiB JVM default, enough to run a mapping
    * over a document at jawn's maximum JSON depth (4096), which already overflows 1 and 4 MiB.
    */
  val StackSizeBytes: Long = 16L * 1024 * 1024

  /** Largest websocket message or request body accepted, in bytes. */
  val MaxInputBytes: Int = 1024 * 1024

  /** Deepest `(`/`[`/`{` nesting accepted in a JSON message, an IDML mapping or a path. The repo's
    * own mappings, tests and docs never go past 12; 1000 leaves plenty of room for generated input
    * while staying far below what overflows a StackSizeBytes thread.
    */
  val MaxNestingDepth: Int = 1000

  /** Longest time a request may take before its caller gets an error. */
  val RequestTimeout: FiniteDuration = 10.seconds

  val TooDeep: List[String]  = List("Input is nested too deeply to be processed")
  val TooLarge: List[String] = List(s"Input is larger than the $MaxInputBytes bytes limit")
  val TimedOut: List[String] = List(s"Request took longer than the $RequestTimeout limit")
  val Internal: List[String] = List("Internal error while processing the request")

  private val threadCount = math.max(2, Runtime.getRuntime.availableProcessors)

  private val threadFactory: ThreadFactory = {
    val counter = new AtomicInteger
    runnable => {
      val thread = new Thread(
        null,
        runnable,
        s"idmld-bounded-stack-${counter.incrementAndGet()}",
        StackSizeBytes)
      thread.setDaemon(true)
      thread
    }
  }

  // bounded pool: each thread may reserve StackSizeBytes, so an unbounded one could exhaust memory
  private val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(threadCount, threadFactory))

  /** Runs `work` on the JSON message `input` on a bounded-stack thread, rejecting oversized or too
    * deeply nested input up front. Never fails: every error, fatal ones included, becomes a Left.
    */
  def run[A](input: String, timeout: FiniteDuration = RequestTimeout)(
      work: String => Either[List[String], A]): IO[Either[List[String], A]] =
    if (exceedsMaxBytes(input)) IO.pure(Left(TooLarge))
    else if (jsonNestingDepth(input) > MaxNestingDepth) IO.pure(Left(TooDeep))
    else
      // shortcut: uncancellable work keeps the worker busy after the timeout until it finishes,
      // switch to a cancellable design if this is abused. Only the join is timed out, not the
      // fiber, so the caller does not wait for the work to finish.
      IO(guarded(work(input)))
        .evalOn(executionContext)
        .start
        .flatMap(_.joinWithNever.timeoutTo(timeout, IO.pure(Left(TimedOut))))

  /** Whether `input` is more than MaxInputBytes once encoded in UTF-8, without encoding it. */
  def exceedsMaxBytes(input: String): Boolean =
    input.length > MaxInputBytes || utf8Length(input) > MaxInputBytes

  // a surrogate pair takes 4 bytes; a lone surrogate is counted 2 instead of 1, overestimating
  private def utf8Length(input: String): Long =
    input.foldLeft(0L)((n, c) =>
      n + (if (c < 0x80) 1 else if (c < 0x800 || Character.isSurrogate(c)) 2 else 3))

  /** Deepest bracket nesting in `json`, ignoring brackets inside JSON string literals. */
  def jsonNestingDepth(json: String): Int = maxDepth(json, skipStrings = true)

  /** Deepest bracket nesting in IDML `code`. IDML has several kinds of strings, comments and
    * backtick labels, so brackets inside them are counted too: this can only overestimate.
    */
  def idmlNestingDepth(code: String): Int = maxDepth(code, skipStrings = false)

  private final case class Scan(depth: Int, max: Int, inString: Boolean, escaped: Boolean)

  private def maxDepth(text: String, skipStrings: Boolean): Int =
    text
      .foldLeft(Scan(0, 0, inString = false, escaped = false)) { (s, c) =>
        if (s.inString)
          if (s.escaped) s.copy(escaped = false)
          else if (c == '\\') s.copy(escaped = true)
          else if (c == '"') s.copy(inString = false)
          else s
        else
          c match {
            case '"' if skipStrings => s.copy(inString = true)
            case '(' | '[' | '{'    => s.copy(depth = s.depth + 1, max = s.max.max(s.depth + 1))
            case ')' | ']' | '}'    => s.copy(depth = (s.depth - 1).max(0))
            case _                  => s
          }
      }
      .max

  // unsafe: catches every Throwable, fatal ones included, before cats-effect sees it. A
  // StackOverflowError comes from our own recursion on this thread, unwound by now. A LinkageError
  // (e.g. NoClassDefFoundError for a class whose initialiser overflowed earlier) only breaks the
  // feature using that class, so it fails this request instead of shutting the JVM down. Payloads
  // are never logged.
  private def guarded[A](work: => Either[List[String], A]): Either[List[String], A] =
    try work
    catch {
      case e @ (_: StackOverflowError | _: ExceptionInInitializerError) =>
        log.warn(s"Rejected input nested too deeply (${e.getClass.getName})")
        Left(TooDeep)
      case e: LinkageError                                              =>
        // no stack trace: it carries the original overflow's, and repeats on every request
        log.error(s"Broken class while processing a request: $e")
        Left(Internal)
      case t: Throwable                                                 =>
        log.error("Unexpected error while processing a request", t)
        Left(Internal)
    }
}
