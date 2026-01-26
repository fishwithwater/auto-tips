package testData;

/**
 * 测试类，用于测试方法调用检测功能
 */
public class CallDetectionTestClass {
    
    /**
     * 简单方法，用于测试基本调用检测
     * @tips 这是一个简单的方法调用
     */
    public void simpleMethod() {
        System.out.println("Simple method");
    }
    
    /**
     * 带参数的方法
     * @tips 这个方法接受多个参数
     * @param name 名称
     * @param age 年龄
     * @return 格式化的字符串
     */
    public String methodWithParams(String name, int age) {
        return name + " is " + age + " years old";
    }
    
    /**
     * 返回对象的方法，用于测试链式调用
     * @tips 这个方法返回一个对象，可以进行链式调用
     * @return 当前对象
     */
    public CallDetectionTestClass chainableMethod() {
        return this;
    }
    
    /**
     * 另一个可链式调用的方法
     * @tips 链式调用的第二个方法
     * @return 当前对象
     */
    public CallDetectionTestClass anotherChainableMethod() {
        return this;
    }
    
    /**
     * 静态方法
     * @tips 这是一个静态方法
     */
    public static void staticMethod() {
        System.out.println("Static method");
    }
    
    /**
     * 测试方法，包含各种方法调用
     */
    public void testMethodCalls() {
        // 简单调用
        simpleMethod();
        
        // 带参数的调用
        String result = methodWithParams("John", 25);
        
        // 链式调用
        this.chainableMethod().anotherChainableMethod();
        
        // 静态方法调用
        CallDetectionTestClass.staticMethod();
        
        // 字符串方法调用
        String text = "hello";
        text.toUpperCase();
        
        // 嵌套调用
        System.out.println(methodWithParams("Jane", 30));
    }
}
