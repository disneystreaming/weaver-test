package weaver

import monix.eval.Task

package object monixcompat {
  type TaskSuite       = MutableTaskSuite
  type SimpleTaskSuite = SimpleMutableTaskSuite
  type FunSuite        = FunTaskSuite
  type GlobalResource  = TaskGlobalResource
  type GlobalRead      = GlobalResourceF.Read[Task]
  type GlobalWrite     = GlobalResourceF.Write[Task]
}
