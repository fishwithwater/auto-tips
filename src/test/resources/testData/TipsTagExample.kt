package testData

/**
 * 演示 @tips 标签使用的 Kotlin 示例类
 */
class TipsTagExampleKt {
    
    /**
     * 计算两个数的和
     * 
     * @tips 这个方法执行简单的加法运算。
     *       Kotlin 的 Int 类型会自动处理溢出（环绕），
     *       如果需要检测溢出，请使用 addExact 或 BigInteger。
     * 
     * @param a 第一个加数
     * @param b 第二个加数
     * @return 两数之和
     */
    fun add(a: Int, b: Int): Int {
        return a + b
    }
    
    /**
     * 除法运算
     * 
     * @tips 注意：除数不能为零！
     *       Kotlin 会在除零时抛出 ArithmeticException。
     * @tips 返回值会向下取整（整数除法）。
     *       如果需要浮点数结果，请将参数转换为 Double。
     * 
     * @param dividend 被除数
     * @param divisor 除数
     * @return 商（整数部分）
     * @throws ArithmeticException 当除数为零时抛出
     */
    fun divide(dividend: Int, divisor: Int): Int {
        require(divisor != 0) { "Division by zero" }
        return dividend / divisor
    }
    
    /**
     * 获取用户信息（挂起函数）
     * 
     * @tips 这是一个挂起函数，只能在协程中调用。
     *       返回值使用了 Kotlin 的可空类型，编译器会强制进行空值检查。
     * @tips 建议使用 Dispatchers.IO 来执行这个函数，
     *       因为它涉及 I/O 操作。
     * 
     * @param userId 用户ID
     * @return 用户信息对象，如果用户不存在则返回 null
     */
    suspend fun getUserInfo(userId: String): User? {
        // 模拟异步数据库查询
        return null
    }
    
    /**
     * 数据类：用户信息
     */
    data class User(
        val id: String,
        val name: String
    )
}
