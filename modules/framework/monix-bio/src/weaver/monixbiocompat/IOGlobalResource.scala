package weaver
package monixbiocompat

import monix.bio.Task

trait IOGlobalResource extends GlobalResource[Task]
