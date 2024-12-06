package org.jetbrains.compose.reload.analysis

@JvmInline
value class ClassId(val value: String) : Comparable<ClassId> {
    override fun compareTo(other: ClassId): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return value
    }
}