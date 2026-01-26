package cn.myjdemo.autotips.javadoc

import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag

/**
 * @tips 标签的 KDoc 标签信息
 * 
 * 为 Kotlin 的 KDoc 注释提供 @tips 标签支持
 */
object TipsKDocTag {
    /**
     * @tips 标签名称
     */
    const val TIPS_TAG_NAME = "tips"
    
    /**
     * 检查标签名是否为 @tips
     */
    fun isTipsTag(tagName: String): Boolean {
        return tagName.equals(TIPS_TAG_NAME, ignoreCase = true)
    }
}
