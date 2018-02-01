package com.clemble.loveit.thank.service

import com.clemble.loveit.common.ServiceSpec
import com.clemble.loveit.common.error.UserException
import com.clemble.loveit.common.model.Resource
import com.clemble.loveit.thank.model.SupportedProject
import org.junit.runner.RunWith
import org.specs2.concurrent.ExecutionEnv
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ROServiceSpec(implicit val ee: ExecutionEnv) extends ServiceSpec {

  lazy val service = dependency[ROService]
  lazy val supPrjService = dependency[SupportedProjectService]

  def listResources(user: String): Set[SupportedProject] = {
    await(supPrjService.findProjectsByUser(user)).toSet
  }

  def assignOwnership(userAuth: Seq[(String, String)], resource: Resource) = {
    val user = userAuth.head._2
    await(service.validate(SupportedProject(resource, user)))
  }

  "POST" should {

    "assign ownership" in {
      val user = createUser()

      val resource = Resource from s"http://${someRandom[String]}.com"
      createProject(user, resource)

      eventually(listResources(user).size shouldEqual 1)

      val expectedResources = Set(resource)
      val actualResources = listResources(user)
      actualResources mustEqual expectedResources
    }

    "prohibit assigning same resource" in {
      val A = createUser()
      val B = createUser()

      val resource = someRandom[Resource]

      createProject(A, resource) mustEqual resource
      createProject(B, resource) must throwA[UserException]
    }

    "prohibit assigning sub resource" in {
      val A = createUser()
      val B = createUser()

      val child = someRandom[Resource]
      val parent = child.parent.get

      createProject(A, parent) mustEqual parent
      createProject(B, child) must throwA[UserException]
    }

    "allow assigning of sub resource to the owner" in {
      val A = createUser()

      val child = someRandom[Resource]
      val parent = child.parent.get

      createProject(A, parent) mustEqual parent
      createProject(A, child) mustEqual child
    }

  }

}
