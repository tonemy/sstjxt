/**
 * @Author: 张今天
 * @Date: 2020/5/4 10:19
 */

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.api.common.operators.Order
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat
import org.apache.flink.api.java.typeutils.RowTypeInfo
import org.apache.flink.api.scala._
import org.apache.flink.ml.common.ParameterMap
import org.apache.flink.ml.recommendation.ALS
import org.apache.flink.types.Row
case class Recommendation(productId: Int, score: Double)
case class UserRecs(userId: Int, recs: Seq[Recommendation])


object Xtglv {
  val JDBC_URL: String = "jdbc:mysql://localhost:3306/resy"
  val JDBC_USER: String = "root"
  val JDBC_PASSWORD: String = "123456"

  def main(args: Array[String]): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    //1.读取rating（productId, userId, score）的数据
    val JDBC_QUERY4 = "select userId, productId, score as yearMonth from rating"
    val originRating = readMysql(env, JDBC_URL, JDBC_USER, JDBC_PASSWORD, JDBC_QUERY4, new RowTypeInfo(
      BasicTypeInfo.INT_TYPE_INFO,  BasicTypeInfo.INT_TYPE_INFO, BasicTypeInfo.DOUBLE_TYPE_INFO))
      .map(x =>{
        val userId = x.getField(0).toString.toInt
        val productId = x.getField(1).toString.toInt
        val score = x.getField(2).toString.toDouble
        (userId, productId, score)
      })
    val userDataset = originRating.map(_._1).distinct();
    val productDataset = originRating.map(_._2).distinct()

    //2.训练隐语义模型
    //设置参数
    val als = ALS().setIterations(10).setNumFactors(5).setLambda(0.01)
    //计算因式分解
    als.fit(originRating)

    val userProducts = userDataset.cross(productDataset)
    // 通过一个map参数设置其他参数

     val preRating  = als.predict(userProducts)
    //提取用户推荐列表(筛选出大于0 的数据)
    val userRecs = preRating.filter(_._3 > 0).map(
      x => {
        val userId = x._1
        val productId = x._2
        val score = x._3
        (userId, (productId, score))
      }
    ).groupBy(0).sortGroup(0, Order.DESCENDING).reduce( (userId, resc)=> (userId._1, userId._2)).print()



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
