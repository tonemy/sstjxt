import java.text.SimpleDateFormat
import java.util.Date

import org.apache.calcite.avatica.proto.Common
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.java.io.jdbc.{JDBCInputFormat, JDBCOutputFormat}
import org.apache.flink.shaded.jackson2.org.yaml.snakeyaml.tokens.TagTuple
import org.apache.flink.table.api.Table
import org.apache.flink.table.functions.{FunctionContext, TableFunction}

import scala.collection.immutable.Range
//import org.apache.flink.api.java.io.jdbc.{JDBCInputFormat, JDBCOutputFormat}
import org.apache.flink.api.java.typeutils.RowTypeInfo
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.table.api.scala._
import org.apache.flink.types.Row

import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
 * @Author: 张今天
 * @Date: 2020/4/28 20:27
 */

class Product {
    @BeanProperty
    var id : Int=_

    @BeanProperty
    var title : String=_

    @BeanProperty
    var imgUrl : String=_

    @BeanProperty
    var categories : String=_

    @BeanProperty
    var tags : String=_
  override def toString = s"Product($id, $title, $imgUrl, $categories, $tags)"
}

class Rating {
  @BeanProperty
  var userId : Int=_

  @BeanProperty
  var productId : Int=_

  @BeanProperty
  var score : Double=_

  @BeanProperty
  var time : Int=_
}

class Test {
  @BeanProperty
  var productId : Int=_


  override def toString = s"Test($productId)"
}
case  class test2( productId:Int, score:Double, timetest:Int)

case  class test3( productId:Int, score:Double, yearMonth:Int)

class changeDate  extends  TableFunction[Int] {
  override def open(context: FunctionContext): Unit = super.open(context)
  def changeDate (x: Int) {
    val simpleDateFormat = new SimpleDateFormat("yyyyMM")
    simpleDateFormat.format(new Date(x * 1000L)).toInt
  }
}
object LoadData {

  def main(args: Array[String]): Unit = {
    val JDBC_URL: String = "jdbc:mysql://localhost:3306/resy"
    val JDBC_USER: String = "root"
    val JDBC_PASSWORD: String = "123456"
    val JDBC_QUERY = "insert into shoplist(id, title, image, categories, tags) values(?,?,?,?,?)"

    val JDBC_QUERY2 = "insert into rating(userId, productId, score, time) values(?,?,?,?)"


    val env = ExecutionEnvironment.getExecutionEnvironment
    val tableEnv = BatchTableEnvironment.create(env)

//    val csvInput : DataSet[Product] = env.readCsvFile [Product]("E:\\java项目\\sstjxt\\recommend\\LoadData\\src\\main\\resources\\products.csv"
//      ,fieldDelimiter = "^" ,includedFields = Array(0, 1, 4, 5, 6),  pojoFields = Array("id", "title", "imgUrl", "categories", "tags"))
////
//    val csvTableSource = tableEnv.fromDataSet(csvInput)
//    tableEnv.registerTable("product", csvTableSource)
//
//    val groupedByCountry : Table =  tableEnv.sqlQuery("select id from product");
//
//    val data = tableEnv.toDataSet[Test](groupedByCountry).print();




//    val csvInput = env.readCsvFile [Product]("E:\\java项目\\sstjxt\\recommend\\LoadData\\src\\main\\resources\\products.csv"
//                      ,fieldDelimiter = "^" ,includedFields = Array(0, 1, 4, 5, 6),  pojoFields = Array("id", "title", "imgUrl", "categories", "tags"))
//                  .map(x =>{
//                    val row = new Row(5)
//                    row.setField(0, x.id)
//                    row.setField(1, x.title)
//                    row.setField(2, x.imgUrl)
//                    row.setField(3, x.categories)
//                    row.setField(4, x.tags)
//                    row
//                  })



//    writeMysql(env, csvInput, JDBC_URL, JDBC_USER, JDBC_PASSWORD, JDBC_QUERY)
//    val csvInput2 = env.readCsvFile[Rating]("E:\\java项目\\sstjxt\\recommend\\LoadData\\src\\main\\resources\\ratings.csv"
//                    ,fieldDelimiter = ",", includedFields = Array(0, 1, 2, 3), pojoFields = Array("userId", "productId", "score", "time"))
//                  .map(x => {
//                    val row = new Row(4)
//
//                    row.setField(0, x.userId)
//                    row.setField(1, x.productId)
//                    row.setField(2, x.score)
//                    row.setField(3, x.time)
//                    row
//                  })
//    writeMysql(env, csvInput2, JDBC_URL, JDBC_USER, JDBC_PASSWORD, JDBC_QUERY2)

    //1、统计历史热门商品
//    val JDBC_QUERY3 = "select productId, count(productId) as count from rating group by productId order by count desc"
//
//    val historypupolarProduct = readMysql(env, JDBC_URL, JDBC_USER, JDBC_PASSWORD, JDBC_QUERY3)
//    historypupolarProduct.map(x => {
//      val productId = x.getField(0)
//      val count = x.getField(1)
//      ("productId=" + productId, "cout="+count)
//    }).print()



    //2、近期热门商品数据
    val JDBC_QUERY4 = "select productId, score, time as yearMonth from rating"
    val originRating = readMysql(env, JDBC_URL, JDBC_USER, JDBC_PASSWORD, JDBC_QUERY4, new RowTypeInfo(
            BasicTypeInfo.INT_TYPE_INFO, BasicTypeInfo.DOUBLE_TYPE_INFO, BasicTypeInfo.INT_TYPE_INFO))
    val data : DataSet[test2] = originRating.map(x => {
      val productId = x.getField(0).toString.toInt
      val count = x.getField(1).toString.toDouble
      val simpleDateFormat = new SimpleDateFormat("yyyyMM")
      val tmp = x.getField(2)
      val timetest  = simpleDateFormat.format( tmp.toString.toInt *1000 ).toInt
      new test2(productId, count, timetest)
    })
    val originRatingTableSource = tableEnv.fromDataSet(data)
    tableEnv.registerTable("originRating", originRatingTableSource)
    val groupedByRating : Table =  tableEnv.sqlQuery("select productId, score, timetest as yearMonth from originRating");
    tableEnv.registerTable("yearMonthRatingTable", groupedByRating)
    val ratingRecently = tableEnv.sqlQuery("select productId, count(productId) as test, yearMonth from yearMonthRatingTable group by yearMonth,  productId order by yearMonth desc, test desc")
//    val data2 = tableEnv.toDataSet[(Int, Long, Int)](ratingRecently).print();

    //3. 根据商品的平均分 ->优质商品统计
    val averageProduct = tableEnv.sqlQuery("select productId, avg(score) as avgtmp from originRating group by productId order by avgtmp desc")
    val data2 = tableEnv.toDataSet[(Int, Double)](averageProduct).print();



  }

//   写mysql
  def writeMysql(env: ExecutionEnvironment, outputData: DataSet[Row], url: String, user: String, pwd: String, sql: String) = {
    outputData.output(JDBCOutputFormat.buildJDBCOutputFormat()
      .setDrivername("com.mysql.jdbc.Driver")
      .setDBUrl(url)
      .setUsername(user)
      .setPassword(pwd)
      .setQuery(sql)
      .finish())
    env.execute("insert data to mysql")
    print("data write successfully")
  }
//  //读取数据
  def readMysql(env: ExecutionEnvironment, url: String,   user: String, pwd: String, sql: String, rowTypeInfo: RowTypeInfo) = {
    val dataResult: DataSet[Row] = env.createInput(JDBCInputFormat.buildJDBCInputFormat()
      .setDrivername("com.mysql.jdbc.Driver")
      .setDBUrl(url)
      .setUsername(user)
      .setPassword(pwd)
      .setQuery(sql)
      .setRowTypeInfo( rowTypeInfo)
      .finish())
      dataResult
  }

}
