public class TipsTestClass {
    
    /**
     * A simple method without tips
     */
    public void simpleMethod() {
        System.out.println("Simple method");
    }
    
    /**
     * A method with @tips annotation
     * @tips This method demonstrates the tips feature. Use it carefully!
     */
    public void methodWithTips() {
        System.out.println("Method with tips");
    }
    
    /**
     * A method with multiple @tips annotations
     * @tips First tip: This is important
     * @tips Second tip: Remember to check the return value
     */
    public String methodWithMultipleTips() {
        return "result";
    }
    
    /**
     * A method with parameters
     * @tips Make sure to pass valid parameters
     * @param name The name parameter
     * @param age The age parameter
     */
    public void methodWithParams(String name, int age) {
        System.out.println("Name: " + name + ", Age: " + age);
    }
    
    /**
     * A chainable method
     * @tips This method returns this for chaining
     */
    public TipsTestClass chainableMethod() {
        return this;
    }
    
    /**
     * Another chainable method
     * @tips Chain this after chainableMethod()
     */
    public TipsTestClass anotherChainableMethod() {
        return this;
    }
    
    /**
     * A static method
     * @tips This is a static method, call it on the class
     */
    public static void staticMethod() {
        System.out.println("Static method");
    }
    
    /**
     * Test method that calls other methods
     */
    public void testMethodCalls() {
        // Simple method call
        simpleMethod();
        
        // Method call with tips
        methodWithTips();
        
        // Method call with parameters
        methodWithParams("John", 30);
        
        // Chained method calls
        this.chainableMethod().anotherChainableMethod();
        
        // Static method call
        TipsTestClass.staticMethod();
        
        // Method with multiple tips
        String result = methodWithMultipleTips();
    }
}
