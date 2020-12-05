package weaver
package monixcompat

import monix.eval.Task

trait TaskGlobalResource extends GlobalResource[Task]
