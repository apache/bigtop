package org.apache.bigtop.bigpetstore

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import scala.collection.mutable.Stack

@RunWith(classOf[JUnitRunner])
class ScalaTestSample extends FlatSpec with Matchers {
	"This test" should "show an example of what we can do with the scala-test library" in {
		val stack = new Stack[Int]
		stack.push(1)
		stack.push(2)
		stack.pop() should be(2)
		stack.pop() should be(1)
	}
}
