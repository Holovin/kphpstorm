package com.vk.kphpstorm.kphptags

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.Function
import com.vk.kphpstorm.inspections.quickfixes.RemoveWarnPerformanceItemQuickFix
import com.vk.kphpstorm.kphptags.psi.KphpDocElementTypes
import com.vk.kphpstorm.kphptags.psi.KphpDocTagElementType
import com.vk.kphpstorm.kphptags.psi.KphpDocWarnPerformanceItemPsiImpl

object KphpWarnPerformanceDocTag : KphpDocTag("@kphp-warn-performance") {
    val AVAILABLE_ITEMS = listOf(
            // also "all" is available, but it's treated as a special value and not listed here
            "implicit-array-cast",
            "constant-execution-in-loop",
            "array-merge-into",
            "array-reserve",
    )

    override val description: String
        get() = "[KPHP] ame as @kphp-analyze-performance, but it doesn't generate a report: instead, it gives a compilation error if potential optimizations are available."

    override val elementType: KphpDocTagElementType
        get() = KphpDocElementTypes.kphpDocTagWarnPerformance

    override fun isApplicableFor(owner: PsiElement): Boolean {
        return owner is Function
    }

    override fun needsAutoCompleteOnTyping(docComment: PhpDocComment, owner: PsiElement?): Boolean {
        return owner is Function && !KphpAnalyzePerformanceDocTag.existsInDocComment(docComment)
    }

    override fun annotate(docTag: PhpDocTag, rhs: PsiElement?, holder: AnnotationHolder) {
        if (rhs == null)
            return holder.errTag(docTag, "[KPHP] Provide arguments: 'all' or detailed inspections")

        val items = mutableListOf<String>()
        var allMentioned = false

        var item = rhs
        while (item != null) {
            if (item is KphpDocWarnPerformanceItemPsiImpl) {
                val name = item.name

                if (name == "all") {
                    allMentioned = true
                    if (item.isNegation())
                        holder.newAnnotation(HighlightSeverity.ERROR, "[KPHP] All can't be negated").range(item).create()
                    if (items.isNotEmpty())
                        holder.newAnnotation(HighlightSeverity.ERROR, "[KPHP] Use 'all' at the beginning").range(item).create()
                }
                else if (!AVAILABLE_ITEMS.contains(name)) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "[KPHP] Unknown item").range(item).withFix(RemoveWarnPerformanceItemQuickFix(item, "[KPHP] Remove unknown $name")).create()
                }
                else {
                    if (allMentioned && !item.isNegation())
                        holder.newAnnotation(HighlightSeverity.WARNING, "[KPHP] 'all' exists, this item has no effect").range(item).withFix(RemoveWarnPerformanceItemQuickFix(item, "[KPHP] Remove excessive $name")).create()
                    if (!allMentioned && item.isNegation())
                        holder.newAnnotation(HighlightSeverity.WARNING, "[KPHP] Using negation without 'all' is confusing").range(item).create()
                    if (items.contains(name))
                        holder.newAnnotation(HighlightSeverity.WARNING, "[KPHP] Duplicate item").range(item).withFix(RemoveWarnPerformanceItemQuickFix(item, "[KPHP] Remove duplicated $name")).create()
                    items.add(name)
                }
            }
            item = item.nextSibling
        }
    }

    override fun onAutoCompleted(docComment: PhpDocComment): String? {
        return "all"
    }
}
