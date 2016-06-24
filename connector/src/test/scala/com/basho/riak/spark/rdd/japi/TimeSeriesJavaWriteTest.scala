/**
  * *****************************************************************************
  * Copyright (c) 2016 IBM Corp.
  *
  * Created by Basho Technologies for IBM
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
  * *****************************************************************************
  */
package com.basho.riak.spark.rdd.japi

import com.basho.riak.spark.japi.SparkJavaUtil._
import com.basho.riak.spark.japi.rdd.RiakTSJavaRDD
import com.basho.riak.spark.rdd.{AbstractRDDTest, RiakTSTests}
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.sql.{DataFrame, SQLContext, Row}
import org.junit.Test
import org.junit.experimental.categories.Category

import scala.collection.JavaConversions._

@Category(Array(classOf[RiakTSTests]))
class TimeSeriesJavaWriteTest extends AbstractJavaTimeSeriesTest(false) with AbstractRDDTest {

  @Test
  def saveSqlRowsToRiakJava: Unit = {
    val sqlRowsRdd: JavaRDD[Row] = jsc.parallelize(List(
      Row(1, "f", 111111L, "bryce", 305.37),
      Row(1, "f", 111222L, "bryce", 300.12),
      Row(1, "f", 111333L, "bryce", 295.95),
      Row(1, "f", 111444L, "ratman", 362.121),
      Row(1, "f", 111555L, "ratman", 3502.212)))

    javaFunctions(sqlRowsRdd).saveToRiakTS(bucketName)
    val newRdd: RiakTSJavaRDD[Row] = jsc.riakTSTable[Row](bucketName, classOf[Row])
      .sql(s"SELECT user_id, temperature_k FROM $bucketName $sqlWhereClause")

    val data = newRdd.collect().map(_.toSeq)

    assertEqualsUsingJSONIgnoreOrder(
      """
        |[
        |   ['bryce',305.37],
        |   ['bryce',300.12],
        |   ['bryce',295.95],
        |   ['ratman',362.121],
        |   ['ratman',3502.212]
        |]
      """.stripMargin, data)
  }

  @Test
  def saveDataFrameWithSchemaToRiakJava: Unit = {
    val sqlContext: SQLContext = new SQLContext(jsc)
    val jsonRdd: JavaRDD[String] = jsc.parallelize(List(
      "{\"surrogate_key\": 1, \"family\": \"f\", \"time\": 111111, \"user_id\": \"bryce\", \"temperature_k\": 305.37}",
      "{\"surrogate_key\": 1, \"family\": \"f\", \"time\": 111222, \"user_id\": \"bryce\", \"temperature_k\": 300.12}",
      "{\"surrogate_key\": 1, \"family\": \"f\", \"time\": 111333, \"user_id\": \"bryce\", \"temperature_k\": 295.95}",
      "{\"surrogate_key\": 1, \"family\": \"f\", \"time\": 111444, \"user_id\": \"ratman\", \"temperature_k\": 362.121}",
      "{\"surrogate_key\": 1, \"family\": \"f\", \"time\": 111555, \"user_id\": \"ratman\", \"temperature_k\": 3502.212}"))

    val df: DataFrame = sqlContext.read.schema(schema).json(jsonRdd)
    javaFunctions(df.javaRDD).saveToRiakTS(bucketName)
    val newRdd: RiakTSJavaRDD[Row] = jsc.riakTSTable[org.apache.spark.sql.Row](bucketName, classOf[Row]).sql(
      s"SELECT user_id, temperature_k FROM $bucketName $sqlWhereClause")

    val data = newRdd.collect().map(_.toSeq)

    assertEqualsUsingJSONIgnoreOrder(
      """
        |[
        |   ['bryce',305.37],
        |   ['bryce',300.12],
        |   ['bryce',295.95],
        |   ['ratman',362.121],
        |   ['ratman',3502.212]
        |]
      """.stripMargin, data)
  }
}
