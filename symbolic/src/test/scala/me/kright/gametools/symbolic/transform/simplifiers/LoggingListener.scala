package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.SymbolicStr
import me.kright.gametools.symbolic.transform.{PartialTransform, PartialTransformListener}

import scala.util.{Failure, Success, Try}

class LoggingListener extends PartialTransformListener[SymbolicStr]:
  val log = StringBuilder()

  override def apply(partialTransform: PartialTransform[SymbolicStr],
                     argument: SymbolicStr,
                     result: Try[Option[SymbolicStr]]): Unit =
    def append(msg: String): Unit =
      log.append(s"${partialTransform.getClass.getSimpleName}: $msg\n")

    result match
      case Failure(exception) => append(s"ERROR ${exception}")
      case Success(Some(resultExpr)) => append(s"${argument} => ${resultExpr}")
      case Success(None) =>
