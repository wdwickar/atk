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

package org.apache.spark.mllib.atk.plugins

import org.trustedanalytics.atk.engine.model.plugins.classification.glm.LogisticRegressionData
import org.trustedanalytics.atk.engine.model.plugins.classification.SVMData
import org.trustedanalytics.atk.engine.model.plugins.clustering.KMeansData
import org.trustedanalytics.atk.engine.model.plugins.dimensionalityreduction.PrincipalComponentsData
import org.apache.spark.mllib.linalg.{ DenseVector, SparseVector, DenseMatrix }
import org.apache.spark.mllib.classification.{ LogisticRegressionModelWithFrequency, SVMModel }
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.regression.LinearRegressionModel
import org.trustedanalytics.atk.domain.DomainJsonProtocol._
import org.scalatest.WordSpec
import MLLibJsonProtocol._
import org.trustedanalytics.atk.engine.model.plugins.regression.LinearRegressionData

import spray.json._

class MLLibJsonProtocolTest extends WordSpec {
  "DenseVectorFormat" should {

    "be able to serialize" in {
      val dv = new DenseVector(Array(1.2, 3.4, 2.2))
      assert(dv.toJson.compactPrint == "{\"values\":[1.2,3.4,2.2]}")
    }

    "parse json" in {
      val string =
        """
          |{
          |   "values": [1.2,3.4,5.6,7.8]
          |
          |
          |}
        """.stripMargin
      val json = JsonParser(string).asJsObject
      val dv = json.convertTo[DenseVector]
      assert(dv.values.length == 4)
    }
  }

  "SparseVectorFormat" should {

    "be able to serialize" in {
      val sv = new SparseVector(2, Array(1, 2, 3), Array(1.5, 2.5, 3.5))
      assert(sv.toJson.compactPrint == "{\"size\":2,\"indices\":[1,2,3],\"values\":[1.5,2.5,3.5]}")
    }

    "parse json" in {
      val string =
        """
        |{
        |   "size": 3,
        |   "indices": [1,2,3,4],
        |   "values": [1.5,2.5,3.5,4.5]
        |
        |
        | }
      """.
          stripMargin
      val json = JsonParser(string).asJsObject
      val sv = json.convertTo[SparseVector]
      assert(sv.size == 3)
      assert(sv.indices.length == 4)
      assert(sv.values.length == 4)
    }
  }

  "KmeansModelFormat" should {

    "be able to serialize" in {
      val v = new KMeansModel(Array(new DenseVector(Array(1.2, 2.1)), new DenseVector(Array(3.4, 4.3))))
      assert(v.toJson.compactPrint == "{\"clusterCenters\":[{\"values\":[1.2,2.1]},{\"values\":[3.4,4.3]}]}")
    }

    "parse json" in {
      val string = "{\"clusterCenters\":[{\"values\":[1.2,2.1]},{\"values\":[3.4,4.3]}]}"
      val json = JsonParser(string).asJsObject
      val v = json.convertTo[KMeansModel]
      assert(v.clusterCenters.length == 2)
    }
  }

  "KMeansDataFormat" should {

    "be able to serialize" in {
      val d = new KMeansData(new KMeansModel(Array(new DenseVector(Array(1.2, 2.1)),
        new DenseVector(Array(3.4, 4.3)))), List("column1", "column2"), List(1.0, 2.0))
      assert(d.toJson.compactPrint == "{\"k_means_model\":{\"clusterCenters\":[{\"values\":[1.2,2.1]},{\"values\":[3.4,4.3]}]},\"observation_columns\":[\"column1\",\"column2\"],\"column_scalings\":[1.0,2.0]}")
    }

    "parse json" in {
      val string = "{\"k_means_model\":{\"clusterCenters\":[{\"values\":[1.2,2.1]},{\"values\":[3.4,4.3]}]},\"observation_columns\":[\"column1\",\"column2\"],\"column_scalings\":[1.0,2.0]}"
      val json = JsonParser(string).asJsObject
      val d = json.convertTo[KMeansData]
      assert(d.kMeansModel.clusterCenters.length == 2)
      assert(d.observationColumns.length == d.columnScalings.length)
      assert(d.observationColumns.length == 2)
    }
  }

  "LogisticRegressionDataFormat" should {

    "be able to serialize" in {
      val l = new LogisticRegressionData(new LogisticRegressionModelWithFrequency(new DenseVector(Array(1.3, 3.1)), 3.5), List("column1", "column2"))

      assert(l.toJson.compactPrint == "{\"log_reg_model\":{\"weights\":{\"values\":[1.3,3.1]},\"intercept\":3.5,\"numFeatures\":2,\"numClasses\":2},\"observation_columns\":[\"column1\",\"column2\"]}")

    }

    "parse json" in {
      val string = "{\"log_reg_model\":{\"weights\":{\"values\":[1.3,3.1,1.2]},\"intercept\":3.5, \"numFeatures\":3,\"numClasses\":2},\"observation_columns\":[\"column1\",\"column2\"]}"
      val json = JsonParser(string).asJsObject
      val l = json.convertTo[LogisticRegressionData]

      assert(l.logRegModel.weights.size == 3)
      assert(l.logRegModel.intercept == 3.5)
      assert(l.observationColumns.length == 2)
      assert(l.logRegModel.numFeatures == 3)
      assert(l.logRegModel.numClasses == 2)
    }
  }

  "SVMDataFormat" should {

    "be able to serialize" in {
      val s = new SVMData(new SVMModel(new DenseVector(Array(2.3, 3.4, 4.5)), 3.0), List("column1", "column2", "columns3", "column4"))
      assert(s.toJson.compactPrint == "{\"svm_model\":{\"weights\":{\"values\":[2.3,3.4,4.5]},\"intercept\":3.0},\"observation_columns\":[\"column1\",\"column2\",\"columns3\",\"column4\"]}")
    }

    "parse json" in {
      val string = "{\"svm_model\":{\"weights\":{\"values\":[2.3,3.4,4.5]},\"intercept\":3.0},\"observation_columns\":[\"column1\",\"column2\",\"columns3\",\"column4\"]}"
      val json = JsonParser(string).asJsObject
      val s = json.convertTo[SVMData]

      assert(s.svmModel.weights.size == 3)
      assert(s.svmModel.intercept == 3.0)
      assert(s.observationColumns.length == 4)
    }

  }

  "LinearRegressionDataFormat" should {

    "be able to serialize" in {
      val l = new LinearRegressionData(new LinearRegressionModel(new DenseVector(Array(1.3, 3.1)), 3.5), List("column1", "column2"))
      assert(l.toJson.compactPrint == "{\"lin_reg_model\":{\"weights\":{\"values\":[1.3,3.1]},\"intercept\":3.5},\"observation_columns\":[\"column1\",\"column2\"]}")
    }

    "parse json" in {
      val string = "{\"lin_reg_model\":{\"weights\":{\"values\":[1.3,3.1]},\"intercept\":3.5},\"observation_columns\":[\"column1\",\"column2\"]}"
      val json = JsonParser(string).asJsObject
      val l = json.convertTo[LinearRegressionData]

      assert(l.linRegModel.weights.size == 2)
      assert(l.linRegModel.intercept == 3.5)
      assert(l.observationColumns.length == 2)
    }
  }

  "DenseMatrixFormat" should {

    "be able to serialize" in {
      val dm = new DenseMatrix(2, 2, Array(1.0, 2.0, 3.0, 4.0), false)
      assert(dm.toJson.compactPrint == "{\"numRows\":2,\"numCols\":2,\"values\":[1.0,2.0,3.0,4.0],\"isTransposed\":false}")
    }

    "parse json" in {
      val string =
        """
        |{
        | "numRows": 2,
        | "numCols": 2,
        | "values": [1.0,2.0,3.0,4.0],
        | "isTransposed": false
        |}
      """.stripMargin
      val json = JsonParser(string).asJsObject
      val dm = json.convertTo[DenseMatrix]
      assert(dm.values.length == 4)
    }
  }

  "PrincipalComponentsModelFormat" should {

    "be able to serialize" in {
      val singularValuesVector = new DenseVector(Array(1.1, 2.2))
      val vFactorMatrix = new DenseMatrix(2, 2, Array(1.0, 2.0, 3.0, 4.0), false)
      val p = new PrincipalComponentsData(2, List("column1", "column2"), singularValuesVector, vFactorMatrix)
      assert(p.toJson.compactPrint == "{\"k\":2,\"observationColumns\":[\"column1\",\"column2\"]," +
        "\"singularValues\":{\"values\":[1.1,2.2]}," +
        "\"vFactor\":{\"numRows\":2,\"numCols\":2,\"values\":[1.0,2.0,3.0,4.0],\"isTransposed\":false}}")
    }

    "parse json" in {
      val string =
        """
          |{
          |"k":2,
          |"observationColumns": ["column1", "column2"],
          |"singularValues": {"values": [1.1,2.2]},
          |"vFactor": {"numRows":2, "numCols": 2, "values": [1.0,2.0,3.0,4.0], "isTransposed": false}
          |}
        """.stripMargin
      val json = JsonParser(string).asJsObject
      val p = json.convertTo[PrincipalComponentsData]

      assert(p.k == 2)
      assert(p.observationColumns.length == 2)
      assert(p.singularValues.size == 2)
      assert(p.vFactor.numRows == 2)
    }
  }

}
