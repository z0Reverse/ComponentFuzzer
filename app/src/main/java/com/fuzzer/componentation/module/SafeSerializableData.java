// 文件路径: app/src/main/java/com/fuzzer/componentation/model/SafeSerializableData.java
package com.fuzzer.componentation.module;

import java.io.Serializable;

public class SafeSerializableData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String testString = "SAFE_TEST_STRING";
    private int testInt = 2023;

    // 可选：添加Getter/Setter方法
    public String getTestString() { return testString; }
    public int getTestInt() { return testInt; }
}