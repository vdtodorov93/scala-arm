// -----------------------------------------------------------------------------
//
//  scala.arm - The Scala Incubator Project
//  Copyright (c) 2009 The Scala Incubator Project. All rights reserved.
//
//  The primary distribution site is http://jsuereth.github.com/scala-arm
//
//  This software is released under the terms of the Revised BSD License.
//  There is NO WARRANTY.  See the file LICENSE for the full text.
//
// -----------------------------------------------------------------------------

package resource

import scala.collection.Traversable

/** 
 * This trait provides a means to ensure traversable access to items inside a resource, while ensuring that the
 * resource is opened/closed appropriately before/after the traversal.  This class can be dangerous because it
 * always tries to open and close the resource for every call to foreach.   This is practically ever method
 * call on a Traversable besides the "view" method.
 * 
 * @tparam A the resource type
 */
trait ManagedTraversable[+B, A] extends Traversable[B] {
  /**
   * The resource we plan to traverse through.
   */
  val resource: ManagedResource[A]

  /**
   * This method determines if the error can be ignored 
   * and traversable continues.
   */
  @com.github.ghik.silencer.silent
  def ignoreError(error: Exception): Boolean = false

  /**
   * This method is called if an exception happens during traversal of the collection.   We allow subclasses to
   * override this to change the default behavior of resource related errors on traversal.
   */
  def handleErrorsDuringTraversal(ex: List[Throwable]): Unit =
    ex.headOption.foreach(throw _)

  protected val traverse: A => TraversableOnce[B]

  /**
   * This method gives us an iterator over items in a resource.
   */
  protected def internalForeach[U](inner: A, f: B => U): Unit =
    traverse(inner).foreach(f)

  /**
   * Executes a given function against all items in the resource.
   * The resource is opened/closed during the call to this method.
   */
  override def foreach[U](f: B => U): Unit = {
    val result = resource.acquireFor { r =>
      try internalForeach(r, f)
      catch {
        case e : Exception if ignoreError(e) =>  //Ignore...
      }
    }
    //If there are errors -> Handle them appropriately
    result.left foreach handleErrorsDuringTraversal
  }

  // Prevent toString from doing work.
  override def toString = s"ManagedTraversable($resource)"
}
