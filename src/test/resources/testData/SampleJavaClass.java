package testData;

/**
 * 示例Java类，用于测试@tips注释解析
 */
public class SampleJavaClass {
    
    /**
     * 示例方法，包含@tips注释
     * @tips 这是一个示例方法，用于演示Auto-Tips插件的功能
     * @param name 用户名称
     * @return 问候语
     */
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
    
    /**
     * 多行@tips注释示例
     * @tips 这个方法执行复杂的计算操作
     * 请确保输入参数在有效范围内
     * 返回值可能为null，请注意检查
     * @param a 第一个参数
     * @param b 第二个参数
     * @return 计算结果
     */
    public Integer calculate(int a, int b) {
        if (a < 0 || b < 0) {
            return null;
        }
        return a + b;
    }
    
    /**
     * 没有@tips注释的方法
     * @param value 输入值
     * @return 处理后的值
     */
    public String process(String value) {
        return value.toUpperCase();
    }
    
    /**
     * 多个@tips标记的方法
     * @tips 第一个提示：这个方法很重要
     * @tips 第二个提示：请谨慎使用
     * @return 状态码
     */
    public int getStatus() {
        return 200;
    }
}