package com.clemble.loveit.thank.service.repository

import com.clemble.loveit.common.RepositorySpec
import com.clemble.loveit.common.model.{OwnedProject, Project}
import com.clemble.loveit.thank.model.UserProjects
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UserProjectRepositorySpec extends RepositorySpec {

  lazy val repo = dependency[UserProjectsRepository]

  "Save" in {
    val projects = someRandom[UserProjects]

    await(repo.save(projects)) shouldEqual projects
    await(repo.save(projects)) should throwA[Throwable]
  }

  "Update Owned Project" in {
    val user = createUser()

    val ownedProject = someRandom[OwnedProject]
    await(repo.saveOwnedProject(user, Seq(ownedProject)))

    await(repo.findById(user)).get.owned should containAllOf(Seq(ownedProject))
  }

  "Update Installed Project" in {
    val user = createUser()

    val installedProject = someRandom[Project].copy(user = user)
    await(repo.saveProject(installedProject)) shouldEqual installedProject

    await(repo.findById(user)).get.installed should containAllOf(Seq(installedProject))
  }

}