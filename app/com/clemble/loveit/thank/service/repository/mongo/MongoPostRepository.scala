package com.clemble.loveit.thank.service.repository.mongo

import javax.inject.{Inject, Named, Singleton}
import akka.stream.Materializer
import com.clemble.loveit.common.model.{Post, PostID, Project, ProjectID, Resource, Tag, UserID}
import com.clemble.loveit.common.mongo.MongoSafeUtils
import com.clemble.loveit.common.model.Project
import com.clemble.loveit.thank.service.repository.PostRepository
import play.api.libs.json.{JsArray, JsObject, Json}
import play.modules.reactivemongo.json._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class MongoPostRepository @Inject()(
                                          @Named("post") collection: JSONCollection,
                                          implicit val ec: ExecutionContext,
                                          implicit val mat: Materializer
                                        ) extends PostRepository {

  MongoPostRepository.ensureMeta(collection)

  override def isSupportedBy(supporter: UserID, url: Resource): Future[Option[Boolean]] = {
    val query = Json.obj(
      "url" -> url,
      "thank.supporters" -> Json.obj("$exists" -> supporter)
    )
    val projection = Json.obj("thank.supporters" -> 1)
    collection.find(query, projection).
      one[JsObject].
      map(_.flatMap(json => (json \ "thank" \ "supporters").asOpt[JsArray].map(_.value.nonEmpty)))
  }

  override def save(post: Post): Future[Boolean] = {
    post.validate()
    MongoSafeUtils.safeSingleUpdate(collection.insert(post))
  }

  override def delete(id: PostID): Future[Option[Post]] = {
    val selector = Json.obj("_id" -> id)
    collection.findAndRemove(selector).map(_.result[Post])
  }

  override def findById(id: String): Future[Option[Post]] = {
    val selector = Json.obj("_id" -> id)
    collection.find(selector).one[Post]
  }

  override def update(post: Post): Future[Option[Post]] = {
    val selector = Json.obj("url" -> post.url)
    val update = Json.toJsObject(post)
    collection.findAndUpdate(selector, update, fetchNewObject = true).map(_.result[Post])
  }

  override def deleteAll(user: UserID, url: Resource): Future[Boolean] = {
    val selector = Json.obj("project.user" -> user, "url" -> Json.obj("$regex" -> s"${url}.*"))
    collection.remove(selector)
      .map(res => {
        res.ok
      })
  }

  override def findByResource(url: Resource): Future[Option[Post]] = {
    val fSearchResult = collection.find(Json.obj("url" -> url)).one[Post]
    MongoSafeUtils.safe(fSearchResult)
  }

  override def findByTags(tags: Set[String]): Future[List[Post]] = {
    val selector = Json.obj("ogObj.tags" -> Json.obj("$in" -> tags))
    MongoSafeUtils.collectAll[Post](collection, selector, sort = Json.obj("ogObj.pubDate" -> -1))
  }

  override def findByProject(project: ProjectID): Future[List[Post]] = {
    val selector = Json.obj("project._id" -> project)
    MongoSafeUtils.collectAll[Post](collection, selector, sort = Json.obj("ogObj.pubDate" -> -1))
  }

  override def findLastByProject(project: ProjectID): Future[Option[Post]] = {
    val selector = Json.obj("project._id" -> project)
    val byPubDate = Json.obj("ogObj.pubDate" -> -1)
    collection.find(selector).sort(byPubDate).one[Post]
  }

  override def findByAuthor(author: UserID): Future[List[Post]] = {
    val selector = Json.obj("project.user" -> author)
    MongoSafeUtils.collectAll[Post](collection, selector, sort = Json.obj("ogObj.pubDate" -> -1))
  }

  override def markSupported(user: UserID, url: Resource): Future[Boolean] = {
    val query = Json.obj(
      "url" -> url
    )
    val update = Json.obj(
      "$inc" -> Json.obj("thank.given" -> 1),
      "$addToSet" -> Json.obj("thank.supporters" -> user)
    )
    MongoSafeUtils.safeSingleUpdate(collection.update(query, update, multi = false))
  }

  override def updateProject(project: Project): Future[Boolean] = {
    val query = Json.obj(
      "$or" -> Json.arr(
        Json.obj("url" -> project.url),
        Json.obj("url" -> Json.obj("$regex" -> s"^${project.url}/.*"))
      )
    )
    val update = Json.obj("$set" -> Json.obj("project" -> project))
    collection.update(query, update, multi = true).map(res => res.ok)
  }

}

object MongoPostRepository {

  def ensureMeta(collection: JSONCollection)(implicit ec: ExecutionContext) = {
    ensureIndexes(collection)
  }

  private def ensureIndexes(collection: JSONCollection)(implicit ec: ExecutionContext) = {
    MongoSafeUtils.ensureIndexes(
      collection,
      Index(
        key = Seq("url" -> IndexType.Ascending),
        name = Some("recourse_is_unique"),
        unique = true
      )
    )
  }

}