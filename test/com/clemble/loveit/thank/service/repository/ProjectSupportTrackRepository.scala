package com.clemble.loveit.thank.service.repository

import com.clemble.loveit.common.RepositorySpec
import com.clemble.loveit.common.model.{Project, Resource, UserID, Verification}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProjectSupportTrackRepositorySpec extends RepositorySpec {

  val trackRepo = dependency[ProjectSupportTrackRepository]

  def assignOwnership(user: UserID, url: Resource): Project = {
    await(prjRepo.saveProject(Project(url, user, someRandom[String], someRandom[String], someRandom[Verification])))
  }

  "Update repo" in {
    val giver = createUser()

    val project = someRandom[Project]

    await(trackRepo.markSupportedBy(giver, project)) shouldEqual true
    await(trackRepo.getSupported(giver)) shouldEqual List(project._id)
  }

}
