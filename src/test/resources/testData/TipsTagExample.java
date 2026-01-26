package testData;

/**
 * 演示 @tips 标签使用的示例类
 */
public class TipsTagExample {
    
    /**
     * 计算两个数的和
     * 
     * @tips 这个方法执行简单的加法运算。
     *       请确保传入的参数不会导致整数溢出。
     *       对于大数运算，建议使用 BigInteger。
     * 
     * @param a 第一个加数
     * @param b 第二个加数
     * @return 两数之和
     */
    public int add(int a, int b) {
        return a + b;
    }
    
    /**
     * 除法运算
     * 
     * @tips 注意：除数不能为零！
     *       这个方法不会自动处理除零异常，调用者需要自行检查。
     * @tips 返回值会向下取整（整数除法）。
     *       如果需要精确的小数结果，请使用 divideExact 方法。
     * 
     * @param dividend 被除数
     * @param divisor 除数
     * @return 商（整数部分）
     * @throws ArithmeticException 当除数为零时抛出
     */
    public int divide(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return dividend / divisor;
    }
    
    /**
     * 获取用户信息
     * 
     * @tips 这是一个耗时操作，建议在后台线程中调用。
     *       返回的用户对象可能为 null，请务必进行空值检查。
     * 
     * @param userId 用户ID
     * @return 用户信息对象，如果用户不存在则返回 null
     */
    public User getUserInfo(String userId) {
        // 模拟数据库查询
        return null;
    }
    
    /**
     * 内部类：用户信息
     */
    static class User {
        private String id;
        private String name;
        
        public String getId() { return id; }
        public String getName() { return name; }
    }
}
