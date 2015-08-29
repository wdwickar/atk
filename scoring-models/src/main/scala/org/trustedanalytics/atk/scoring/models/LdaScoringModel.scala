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
package org.trustedanalytics.atk.scoring.models

import org.trustedanalytics.atk.scoring.interfaces.Model
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import spray.json._
import ScoringJsonReaderWriters._

/**
 * Scoring model for Latent Dirichlet Allocation
 */
class LdaScoringModel(ldaModel: LdaModel) extends LdaModel(ldaModel.numTopics, ldaModel.topicWordMap) with Model {

  override def score(data: Seq[Array[String]]): Future[Seq[Any]] = future {
    var score = Seq[Any]()
    data.foreach { row =>
      {
        val document: Array[String] = row

        row.zipWithIndex.foreach {
          case (value: Any, index: Int) => document(index) = value
        }

        val predictReturn = predict(document.toList)
        score = score :+ predictReturn.toJson
      }
    }
    score
  }
}
