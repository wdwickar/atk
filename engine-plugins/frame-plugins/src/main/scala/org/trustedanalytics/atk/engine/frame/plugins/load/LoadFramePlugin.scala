/*
// Copyright (c) 2015 Intel Corporation 
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/

package org.trustedanalytics.atk.engine.frame.plugins.load

import org.trustedanalytics.atk.domain.UserPrincipal
import org.trustedanalytics.atk.domain.frame.{ FrameReference, FrameEntity }
import org.trustedanalytics.atk.domain.frame.load.LoadFrameArgs
import org.trustedanalytics.atk.engine.frame.SparkFrame
import org.trustedanalytics.atk.engine.plugin.{ PluginDoc, Invocation }
import org.trustedanalytics.atk.engine.plugin.SparkCommandPlugin
import org.apache.spark.frame.FrameRdd

import spray.json._
import org.trustedanalytics.atk.domain.DomainJsonProtocol._

/**
 * Parsing data to load and append to data frames
 */
@PluginDoc(oneLine = "<TBD>",
  extended = "<TBD>",
  returns = "<TBD>")
class LoadFramePlugin extends SparkCommandPlugin[LoadFrameArgs, FrameEntity] {

  /**
   * The name of the command, e.g. graph/ml/loopy_belief_propagation
   *
   * The format of the name determines how the plugin gets "installed" in the client layer
   * e.g Python client via code generation.
   */
  override def name: String = "frame:/load"

  /**
   * Number of Spark jobs that get created by running this command
   * (this configuration is used to prevent multiple progress bars in Python client)
   */
  override def numberOfJobs(load: LoadFrameArgs)(implicit invocation: Invocation) = 8

  /**
   * Parsing data to load and append to data frames
   *
   * @param invocation information about the user and the circumstances at the time of the call,
   *                   as well as a function that can be called to produce a SparkContext that
   *                   can be used during this invocation.
   * @param arguments the arguments supplied by the caller
   * @return a value of type declared as the Return type.
   */
  override def execute(arguments: LoadFrameArgs)(implicit invocation: Invocation): FrameEntity = {
    val sparkAutoPartitioner = engine.sparkAutoPartitioner
    def getAbsolutePath(s: String): String = engine.frames.frameFileStorage.hdfs.absolutePath(s).toString

    val destinationFrame: SparkFrame = arguments.destination

    // run the operation
    if (arguments.source.isFrame) {
      // load data from an existing frame and add its data onto the target frame
      val additionalData = (FrameReference(arguments.source.uri.toInt): SparkFrame).rdd
      unionAndSave(destinationFrame, additionalData)
    }
    else if (arguments.source.isFile || arguments.source.isMultilineFile) {
      val filePath = getAbsolutePath(arguments.source.uri)
      val partitions = sparkAutoPartitioner.partitionsForFile(filePath)
      val parseResult = LoadRddFunctions.loadAndParseLines(sc, filePath,
        null, partitions, arguments.source.startTag, arguments.source.endTag, arguments.source.sourceType.contains("xml"))
      unionAndSave(destinationFrame, parseResult.parsedLines)

    }
    else if (arguments.source.isHiveDb) {
      val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
      val rdd = sqlContext.sql(arguments.source.uri) //use URI
      unionAndSave(destinationFrame, LoadRddFunctions.convertHiveRddToFrameRdd(rdd))
    }
    else if (arguments.source.isFieldDelimited || arguments.source.isClientData) {
      val parser = arguments.source.parser.get
      val filePath = getAbsolutePath(arguments.source.uri)
      val parseResult = if (arguments.source.isFieldDelimited) {
        val partitions = sparkAutoPartitioner.partitionsForFile(filePath)
        LoadRddFunctions.loadAndParseLines(sc, filePath, parser, partitions)
      }
      else {
        val data = arguments.source.data.get
        LoadRddFunctions.loadAndParseData(sc, data, parser)
      }
      // parse failures go to their own data frame
      if (parseResult.errorLines.count() > 0) {
        val errorFrame = engine.frames.lookupOrCreateErrorFrame(destinationFrame)
        unionAndSave(errorFrame, parseResult.errorLines)
      }

      // successfully parsed lines get added to the destination frame
      unionAndSave(destinationFrame, parseResult.parsedLines.dropIgnoreColumns())
    }

    else {
      throw new IllegalArgumentException("Unsupported load source: " + arguments.source.sourceType)
    }
  }

  /**
   * Union the additionalData onto the end of the existingFrame
   * @param existingFrame the target DataFrame that may or may not already have data
   * @param additionalData the data to add to the existingFrame
   * @return the frame with updated schema
   */
  private def unionAndSave(existingFrame: SparkFrame, additionalData: FrameRdd)(implicit invocation: Invocation): SparkFrame = {
    val unionedRdd = existingFrame.rdd.union(additionalData)
    existingFrame.save(unionedRdd)
  }

}
