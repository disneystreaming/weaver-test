package org.junit.runner
import org.typelevel.scalaccompat.annotation.unused

/**
 * Stub used for cross-compilation
 */
class RunWith[T](@unused cls: Class[T])
    extends scala.annotation.StaticAnnotation
