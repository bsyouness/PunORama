import com.google.cloud.dataflow.sdk.values.PCollectionView
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.cloud.dataflow.sdk.values.PCollection
import com.google.api.services.bigquery.model.TableSchema
import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.io.TextIO
import com.google.cloud.dataflow.sdk.options.PipelineOptions
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory
import com.google.cloud.dataflow.sdk.transforms.Count
import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.transforms.ParDo
import com.google.cloud.dataflow.sdk.values.KV
import com.google.cloud.dataflow.sdk.transforms.Filter
import com.google.cloud.dataflow.sdk.transforms.Sample
import com.google.cloud.dataflow.sdk.transforms.View
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.sys.process._
import com.google.cloud.dataflow.sdk.transforms.SerializableComparator
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.cloud.dataflow.sdk.io.BigQueryIO
import scala.pickling._
import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.pickling.static._
import scala.collection.immutable.TreeSet
import com.google.cloud.dataflow.sdk.transforms.SerializableFunction

object Transforms {
  /* -----------------------------------------
  								Types  
     -----------------------------------------*/
  // WordAndPronunciation: a key-valye object of a word and its pronunciation.
  type WAP = KV[String, String]
  // A scored pun: a key-value object, where the keys are the scores, and each
  // object is tuple of  the word and its pronunciation
  type ScoredPun = KV[java.lang.Long, KV[String, String]]
  type SerializedSet = Array[Byte]
  
  /* -----------------------------------------
  								Scoring  
     -----------------------------------------*/  
  def scorePuns = new DoFn[KV[WAP, WAP], ScoredPun]() {
    override def processElement(c: DoFn[KV[WAP, WAP], ScoredPun]#ProcessContext) {
      val word1 = c.element().getKey().getKey()
      val pron1 = c.element().getKey().getValue()
      val word2 = c.element().getValue().getKey()
      val pron2 = c.element().getValue().getValue()
      c.output(KV.of(Pun.punScore(pron1, pron2, word1, word2),
        KV.of(word1, word2)))
    }
  }
  
  /* -----------------------------------------
  								Formatting
     -----------------------------------------*/  
  val formatSortedPuns = new DoFn[java.util.List[ScoredPun], String]() {
    override def processElement(c: DoFn[java.util.List[ScoredPun], String]#ProcessContext) {
      val puns = c.element
      for (p <- puns) {
        val tuple = (p.getKey, p.getValue.getKey, p.getValue.getValue)
        c.output(tuple.toString)
      }
    }
  }

  def formatPair = new DoFn[KV[WAP, WAP], String]() {
    override def processElement(c: DoFn[KV[WAP, WAP], String]#ProcessContext) {
      c.output(c.element().toString)
    }
  }

  val formatScoredPun = new DoFn[ScoredPun, String]() {
    override def processElement(c: DoFn[ScoredPun, String]#ProcessContext) {
      val tuple = (c.element.getKey, c.element.getValue.getKey, c.element.getValue.getValue)
      c.output(tuple.toString)
    }
  }

  /* -----------------------------------------
  								Filtering
     -----------------------------------------*/  
  val filterPuns = new DoFn[ScoredPun, ScoredPun]() {
    override def processElement(c: DoFn[ScoredPun, ScoredPun]#ProcessContext) {
      val threshold = 2
      val pun = c.element()
      if (pun.getKey >= threshold) {
        c.output(pun)
      }
    }
  }
  
  // Filter out all the words that are not alphanumeric (more filters to come)
  val filterWords = new DoFn[String, String]() {
    override def processElement(c: DoFn[String, String]#ProcessContext) {
      val word = c.element()
      // We might invent more filters later.
      val isAlphanumeric = word.forall(_.isLetterOrDigit)
      if (isAlphanumeric) {
        c.output(word)
      }
    }
  }

  def filterChar(text: String): String = {
    val forbiddenList = List(''', ',', '-','2')
    text.toList.filterNot(forbiddenList.contains(_)).mkString("").toUpperCase()
  }
    
  /* -----------------------------------------
  								Cartesian Product  
     -----------------------------------------*/  
  def cartesianProduct(pca: PCollection[WAP], pcb: PCollection[WAP]): PCollection[KV[WAP, WAP]] = {
    val view: PCollectionView[java.lang.Iterable[WAP]] = pca.apply(View.asIterable[WAP])
    val doProduct = new DoFn[WAP, KV[WAP, WAP]]() {
      override def processElement(c: DoFn[WAP, KV[WAP, WAP]]#ProcessContext) {
        val collectionA: Iterable[WAP] = c.sideInput(view)
        for (a <- collectionA) {
          c.output(KV.of(a, c.element()))
        }
      }
    }
    pcb.apply(ParDo.withSideInputs(view).of(doProduct))
  }
  
  val emitPunsOld = new DoFn[java.util.List[ScoredPun], ScoredPun]() {
    override def processElement(c: DoFn[java.util.List[ScoredPun], ScoredPun]#ProcessContext) {
      val puns = c.element
      for (p <- puns) {
        c.output(p)
      }
    }
  }

  /* -----------------------------------------
  								BigQuery Schema  
     -----------------------------------------*/
  val tableSchema = new TableSchema().setFields(List(
    new TableFieldSchema().setName("score").setType("INTEGER"),
    new TableFieldSchema().setName("word_0").setType("STRING"),
    new TableFieldSchema().setName("word_1").setType("STRING")))

   val scoredPunToWordConverter = new DoFn[ScoredPun, TableRow]() {
    override def processElement(c: DoFn[ScoredPun, TableRow]#ProcessContext) {
      val score: Long = c.element().getKey
      val word0: String = c.element().getValue.getKey
      val word1: String = c.element().getValue.getValue
      c.output(new TableRow().set("score", score).set("word_0", word0).set("word_1", word1))
    }
  }
  
  /* -----------------------------------------
  								Trie Methods  
     -----------------------------------------*/  
  val trieSeed = new DoFn[WAP, SerializedSet]() {
    override def processElement(c: DoFn[WAP, SerializedSet]#ProcessContext) {
      val empty = new TreeSet[(String, String)]()
      val oneItem = empty + ((c.element.getValue, c.element.getKey))
      assert(oneItem.size == 1)
      c.output(oneItem.toSet.pickle.value)
    }
  }

  val trieCombine = new SerializableFunction[java.lang.Iterable[SerializedSet], SerializedSet]() {
    override def apply(input: java.lang.Iterable[SerializedSet]): SerializedSet = {
      val sets: List[Set[(String, String)]] = input.toList.map({ ss: SerializedSet =>
        BinaryPickle(ss).unpickle[Set[(String, String)]]
      })

      val set = sets.foldLeft(new TreeSet[(String, String)])(_ ++ _)
      assert(set.size >= sets.size)

      set.toSet.pickle.value
    }
  }

  val visualizeTrie = new DoFn[SerializedSet, String]() {
    override def processElement(c: DoFn[SerializedSet, String]#ProcessContext) {
      val s = c.element.unpickle[Set[(String, String)]]

      // TODO(ericmc): Inefficient.
      val ts = s.foldLeft(new TreeSet[(String, String)]())(_ + _)

      c.output(ts.toString)
    }
  }
    
  def emitPuns(trieView: PCollectionView[SerializedSet], waps: PCollection[WAP]): PCollection[ScoredPun] = {
    val doEmit = new DoFn[WAP, ScoredPun]() {
      override def processElement(c: DoFn[WAP, ScoredPun]#ProcessContext) {
        val s = c.sideInput(trieView).unpickle[Set[(String, String)]]

        // TODO(ericmc): Inefficient.
        val ts = s.foldLeft(new TreeSet[(String, String)]())(_ + _)

        val query = (c.element.getValue, c.element.getKey)
        val possiblePuns = Pun.findPuns(ts, query)

        for (p <- possiblePuns) {
          val (overlap, (matchingPronunciation, matchingWord)) = p
          assert(overlap > 0)

          val score = Pun.punScore(query._1, matchingPronunciation, query._2, matchingWord)
          c.output(KV.of(score, KV.of(query._2, matchingWord)))
        }
      }
    }

    waps.apply(ParDo.withSideInputs(trieView).named("EmitPuns").of(doEmit))
  }
}
