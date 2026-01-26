package testData;

/**
 * 测试类，包含各种无效和不完整的方法调用
 * 用于测试无效调用过滤功能
 */
public class InvalidCallTestClass {
    
    /**
     * 有效方法，用于对比测试
     * @tips 这是一个有效的方法
     */
    public void validMethod() {
        System.out.println("Valid method");
    }
    
    /**
     * 带参数的有效方法
     * @tips 这个方法接受参数
     */
    public String validMethodWithParams(String param) {
        return param;
    }
    
    /**
     * 测试方法，包含各种有效和无效的调用场景
     */
    public void testInvalidCalls() {
        // 有效调用 - 用于对比
        validMethod();
        
        // 有效调用 - 带参数
        validMethodWithParams("test");
        
        // 不完整的调用 - 缺少右括号（这在实际代码中会导致编译错误）
        // validMethod(
        
        // 不完整的调用 - 只有方法名（这在实际代码中会导致编译错误）
        // validMethod
        
        // 不完整的调用 - 缺少参数（这在实际代码中会导致编译错误）
        // validMethodWithParams()
        
        // 语法错误的调用（这在实际代码中会导致编译错误）
        // validMethod)(
        
        // 空的方法调用表达式（这在实际代码中会导致编译错误）
        // ()
    }
    
    /**
     * 测试方法，包含部分完成的代码
     * 模拟用户正在输入但尚未完成的场景
     */
    public void testPartialCode() {
        // 完整的调用
        validMethod();
        
        // 用户可能正在输入的代码...
        // valid
        // validM
        // validMethod
        // validMethod(
    }
}
