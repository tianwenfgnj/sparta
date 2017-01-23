/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.sparta.plugin.output.elasticsearch

import java.io.{Serializable => JSerializable}

import com.stratio.sparta.sdk.pipeline.output.Output._
import com.stratio.sparta.sdk.pipeline.output.{Output, SaveModeEnum}
import com.stratio.sparta.sdk.pipeline.schema.SpartaSchema
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import org.apache.spark.sql._
import org.apache.spark.sql.types._

/**
  *
  * Check for possible Elasticsearch-dataframe settings
  *
  * org/elasticsearch/hadoop/cfg/Settings.java
  * org/elasticsearch/hadoop/cfg/ConfigurationOptions.java
  *
  */
class ElasticSearchOutput(keyName: String,
                          version: Option[Int],
                          properties: Map[String, JSerializable],
                          schemas: Seq[SpartaSchema])
  extends Output(keyName, version, properties, schemas) {

  val DefaultIndexType = "sparta"
  val DefaultNode = "localhost"
  val DefaultHttpPort = "9200"
  val NodeName = "node"
  val NodesName = "nodes"
  val HttpPortName = "httpPort"
  val DefaultCluster = "elasticsearch"
  val ElasticSearchClass = "org.elasticsearch.spark.sql"

  val idField = properties.getString("idField", None)
  val mappingType = properties.getString("indexMapping", DefaultIndexType)
  val clusterName = properties.getString("clusterName", DefaultCluster)
  val httpNodes = getHostPortConfs(NodesName, DefaultNode, DefaultHttpPort, NodeName, HttpPortName)

  lazy val mappingName = versionedTableName(mappingType).toLowerCase

  override def save(dataFrame: DataFrame, saveMode: SaveModeEnum.Value, options: Map[String, String]): Unit = {
    val tableName = getTableNameFromOptions(options)
    val dataFrameSchema = dataFrame.schema
    val timeDimension = dataFrameSchema.fields.filter(stField => stField.metadata.contains(Output.TimeDimensionKey))
      .map(_.name).headOption
    getTimeFromOptions(options)
    val sparkConfig = getSparkConfig(timeDimension, saveMode)


    //This dataFrame transformation is necessary because ES doesn't support java.sql.TimeStamp in the row values: use
    // dateType in the cube writer options and set to long, date or dateTime
    val newDataFrame = if (dataFrameSchema.fields.exists(stField => stField.dataType == TimestampType)) {
      val rdd = dataFrame.map(row => {
        val seqOfValues = row.toSeq.map { value =>
          value match {
            case value: java.sql.Timestamp => value.getTime
            case _ => value
          }
        }
        Row.fromSeq(seqOfValues)
      })
      val newSchema = StructType(dataFrameSchema.map(structField =>
        if (structField.dataType == TimestampType) structField.copy(dataType = LongType)
        else structField)
      )
      SQLContext.getOrCreate(dataFrame.rdd.sparkContext).createDataFrame(rdd, newSchema)
    }
    else dataFrame

    validateSaveMode(saveMode)

    dataFrame.write
      .format(ElasticSearchClass)
      .mode(getSparkSaveMode(saveMode))
      .options(sparkConfig ++ getCustomProperties)
      .save(indexNameType(tableName))
  }

  def indexNameType(tableName: String): String = s"${tableName.toLowerCase}/$mappingName"

  def getHostPortConfs(key: String,
                       defaultHost: String,
                       defaultPort: String,
                       nodeName: String,
                       portName: String): Seq[(String, Int)] = {
    properties.getMapFromJsoneyString(key).map(c =>
      (c.getOrElse(nodeName, defaultHost), c.getOrElse(portName, defaultPort).toInt))
  }

  def getSparkConfig(timeName: Option[String], saveMode: SaveModeEnum.Value): Map[String, String] = {
    saveMode match {
      case SaveModeEnum.Upsert => idField.fold(Map.empty[String, String]) { field => Map("es.mapping.id" -> field) }
      case _ => Map.empty[String, String]
    }
  } ++ {
    Map("es.nodes" -> httpNodes.head._1, "es.port" -> httpNodes.head._2.toString, "es.index.auto.create" -> "no")
  } ++ {
    timeName match {
      case Some(timeNameValue) if !timeNameValue.isEmpty => Map("es.mapping.timestamp" -> timeNameValue)
      case _ => Map.empty[String, String]
    }
  }
}
