package io.idml

/** Allows traversal of a IdmlValue using the visitor pattern */
abstract class IdmlValueVisitor {

  def visitObject(path: Seq[String], obj: IdmlObject): Unit = {
    obj.fields.foreach { case (key, value) =>
      visitValue(path :+ key, value)
    }
  }

  def visitArray(path: Seq[String], array: IdmlArray): Unit = {
    array.items.foreach { case value: Any =>
      visitValue(path, value)
    }
  }

  def visitNothing(path: Seq[String], n: IdmlNothing): Unit = {}

  def visitString(path: Seq[String], s: IdmlString): Unit = {}

  def visitDouble(path: Seq[String], d: IdmlDouble): Unit = {}

  def visitInt(path: Seq[String], i: IdmlInt): Unit = {}

  def visitBool(path: Seq[String], b: IdmlBool): Unit = {}

  def visitNull(path: Seq[String]): Unit = {}

  def visitPrimitive(path: Seq[String], value: IdmlValue): Unit = {
    value match {
      case v: IdmlString => visitString(path, v)
      case v: IdmlDouble => visitDouble(path, v)
      case v: IdmlInt    => visitInt(path, v)
      case v: IdmlBool   => visitBool(path, v)
      case IdmlNull      => visitNull(path)
    }
  }

  def visitIndexValue(path: Seq[String], index: Int, value: IdmlValue): Unit = {
    visitValue(path, value)
  }

  def visitValue(path: Seq[String], value: IdmlValue): Unit = {
    value match {
      case o: IdmlObject  => visitObject(path, o)
      case a: IdmlArray   => visitArray(path, a)
      case n: IdmlNothing => visitNothing(path, n)
      case v: Any         => visitPrimitive(path, v)
    }
  }

  def visitValue(value: IdmlValue): Unit = {
    visitValue(Vector(), value)
  }
}
