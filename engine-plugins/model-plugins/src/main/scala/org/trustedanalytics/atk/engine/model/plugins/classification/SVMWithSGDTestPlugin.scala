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

package org.trustedanalytics.atk.engine.model.plugins.classification

import org.apache.spark.mllib.atk.plugins.MLLibJsonProtocol
import org.trustedanalytics.atk.domain.frame.ClassificationMetricValue
import org.trustedanalytics.atk.engine.frame.plugins.ClassificationMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.sql.Row
import org.trustedanalytics.atk.engine.model.Model
import org.trustedanalytics.atk.engine.model.plugins.FrameRddImplicits
import org.trustedanalytics.atk.engine.plugin.{ ApiMaturityTag, Invocation, PluginDoc }
import org.trustedanalytics.atk.engine.frame.SparkFrame
import org.trustedanalytics.atk.engine.plugin.SparkCommandPlugin
import org.apache.spark.mllib.regression.LabeledPoint
import FrameRddImplicits._
import org.apache.spark.rdd.RDD

//Implicits needed for JSON conversion
import spray.json._
import org.trustedanalytics.atk.domain.DomainJsonProtocol._
import MLLibJsonProtocol._

/* Run the SVMWithSGD model on the test frame*/

@PluginDoc(oneLine = "Predict test frame labels and return metrics.",
  extended = """Predict the labels for a test frame and run classification metrics on predicted
and target labels.""",
  returns = """object
    An object with binary classification metrics.
    The data returned is composed of multiple components:
  <object>.accuracy : double
  <object>.confusion_matrix : table
  <object>.f_measure : double
  <object>.precision : double
  <object>.recall : double""")
class SVMWithSGDTestPlugin extends SparkCommandPlugin[ClassificationWithSGDTestArgs, ClassificationMetricValue] {
  /**
   * The name of the command.
   *
   * The format of the name determines how the plugin gets "installed" in the client layer
   * e.g Python client via code generation.
   */
  override def name: String = "model:svm/test"

  override def apiMaturityTag = Some(ApiMaturityTag.Alpha)

  /**
   * Number of Spark jobs that get created by running this command
   * (this configuration is used to prevent multiple progress bars in Python client)
   */

  override def numberOfJobs(arguments: ClassificationWithSGDTestArgs)(implicit invocation: Invocation) = 9
  /**
   * Get the predictions for observations in a test frame
   *
   * @param invocation information about the user and the circumstances at the time of the call,
   *                   as well as a function that can be called to produce a SparkContext that
   *                   can be used during this invocation.
   * @param arguments user supplied arguments to running this plugin
   * @return a value of type declared as the Return type.
   */
  override def execute(arguments: ClassificationWithSGDTestArgs)(implicit invocation: Invocation): ClassificationMetricValue = {
    val model: Model = arguments.model
    val frame: SparkFrame = arguments.frame

    //Extracting the model and data to run on
    val svmData = model.data.convertTo[SVMData]
    val svmModel = svmData.svmModel
    if (arguments.observationColumns.isDefined) {
      require(svmData.observationColumns.length == arguments.observationColumns.get.length, "Number of columns for train and test should be same")
    }
    val svmColumns = arguments.observationColumns.getOrElse(svmData.observationColumns)

    val labeledTestRDD: RDD[LabeledPoint] = frame.rdd.toLabeledPointRDD(arguments.labelColumn, svmColumns)

    //predicting and testing
    val scoreAndLabelRDD: RDD[Row] = labeledTestRDD.map { point =>
      val prediction = svmModel.predict(point.features)
      Row(point.label, prediction)
    }

    //Run Binary classification metrics
    val posLabel: String = "1.0"
    ClassificationMetrics.binaryClassificationMetrics(scoreAndLabelRDD, 0, 1, posLabel, 1)

  }
}
