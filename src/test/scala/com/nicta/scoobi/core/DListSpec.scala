package com.nicta.scoobi
package core

import org.apache.hadoop.io.Text
import testing.NictaSimpleJobs

class DListSpec extends NictaSimpleJobs {

  tag("issue 99")
  "a DList can be created and persisted with some Text" >> { implicit sc: SC =>
    val list = DList((new Text("key1"), new Text("value1")), (new Text("key2"), new Text("value2")))
    run(list).map(_.toString).sorted must_== Seq("(key1,value1)", "(key2,value2)")
  }

  tag("issue 104")
  "Summing up an empty list should do something graceful" >> { implicit sc: SC =>
    run(DList[Int]().sum) must throwAn[Exception](message = "the reduce operation is called on an empty list")
  }

  tag("issue 117")
  "A groupByKey with barrier followed by a groupByKey must be ok" >> { implicit sc: SC =>
    val list = DList.tabulate(5)((n: Int) => ("hello" -> "world")).groupByKey.groupBarrier.groupByKey
    run(list).toString.split(", ").filter { case w => w contains "world" } must have size(5)
  }

  tag("issue 117")
  "A complex graph example must not throw an exception" >> { implicit sc: SC =>


    def simpleJoin[T: Manifest: WireFormat, V: Manifest: WireFormat](a: DList[(Int, T)], b: DList[(Int, V)]) =
      (a.map(x => (x._1, x._1)) ++ b.map(x => (x._1, x._1))).groupByKey.groupBarrier

    val data = DList((12 -> 13), (14 -> 15), (13 -> 55))
    val (a, b, c, d, e) = (data, data, data, data, data)

    val q = simpleJoin(simpleJoin(a, b), simpleJoin(c, d))
    val res = simpleJoin(q, simpleJoin(q, e).groupByKey)

    run(res) must not(throwAn[Exception])
  }

  tag("issue 127")
  "There must be no cyclic execution" >> { implicit sc: SC =>
    val input = fromKeyValues("k1,1", "k2,2")

    val inputGrouped = input.groupBy(_._1)
    val inputGroupedDifferently = input.groupBy(_._2)
    val inputGroupedAsDObject = inputGrouped.materialize

    val dObjectJoinedToInputGroupedDiff = (inputGroupedAsDObject join inputGroupedDifferently)

    run(dObjectJoinedToInputGroupedDiff).toString === "Something"
  }.pendingUntilFixed("fix the Mscr creation")

}